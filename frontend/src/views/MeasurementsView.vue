<template>
  <div class="measurements-container animate-fade-in" v-loading="loading">
    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Monitor /></el-icon>
            <span class="title">健康检测记录</span>
          </div>
          <div class="header-right">
            <el-select v-model="selectedResidentId" placeholder="选择居民查看" clearable style="width: 220px; margin-right: 12px" @change="onResidentChange">
              <el-option v-for="item in residents" :key="item.id" :label="`${item.name} (${item.archiveNo})`" :value="item.id" />
            </el-select>
            <el-button v-if="canEdit" type="primary" @click="openAddDialog">
              <el-icon><Plus /></el-icon> 新增检测
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="residentName" label="居民姓名" width="120" align="center" v-if="!selectedResidentId" />
        <el-table-column label="测量时间" width="170" align="center">
          <template #default="{ row }">
            {{ formatFriendlyDateTime(row.measuredAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="location" label="地点" width="120" show-overflow-tooltip />
        <el-table-column prop="systolicBp" label="收缩压" width="110" align="center">
          <template #default="{ row }">
            <span :class="{'text-danger font-bold': row.systolicBp > 140 || row.systolicBp < 90}">{{ row.systolicBp }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="diastolicBp" label="舒张压" width="110" align="center">
          <template #default="{ row }">
            <span :class="{'text-danger font-bold': row.diastolicBp > 90 || row.diastolicBp < 60}">{{ row.diastolicBp }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bloodSugar" label="血糖" width="110" align="center">
          <template #default="{ row }">
            <span :class="{'text-danger font-bold': row.bloodSugar > 6.1 || row.bloodSugar < 3.9}">{{ row.bloodSugar }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bloodLipid" label="血脂" width="110" align="center">
           <template #default="{ row }">
            <span :class="{'text-danger font-bold': row.bloodLipid > 5.2}">{{ row.bloodLipid }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="heartRate" label="心率" width="110" align="center">
          <template #default="{ row }">
            <span :class="{'text-danger font-bold': row.heartRate > 100 || row.heartRate < 60}">{{ row.heartRate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.sourceType === 'MANUAL' ? 'info' : 'success'">
              {{ row.sourceType === 'MANUAL' ? '手动' : '同步' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.alertFlag ? 'danger' : 'success'" effect="dark">
              {{ row.alertFlag ? "异常" : "正常" }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无检测记录" :image-size="100" />
        </template>
      </el-table>
    </el-card>

    <el-card class="page-card" v-if="selectedResidentId">
      <template #header>
        <div class="card-header">
          <span class="title">检测趋势图 (最近20次)</span>
        </div>
      </template>
      <div ref="chartRef" class="chart-box"></div>
    </el-card>

    <!-- 新增弹窗 -->
    <el-dialog v-model="showDialog" title="录入健康检测数据" width="580px" class="custom-dialog">
      <el-form :model="form" label-position="top" class="p-20">
        <el-form-item label="居民对象">
          <el-select v-model="form.residentId" style="width: 100%" filterable placeholder="请选择居民">
            <el-option v-for="item in residents" :key="item.id" :label="`${item.name} (${item.archiveNo})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="测量时间">
              <el-date-picker v-model="form.measuredAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="测量地点">
              <el-input v-model="form.location" placeholder="如：家庭、社区中心" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">体征数据</el-divider>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="收缩压 (mmHg)">
              <el-input-number v-model="form.systolicBp" :min="40" :max="250" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="舒张压 (mmHg)">
              <el-input-number v-model="form.diastolicBp" :min="30" :max="180" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="心率 (次/分)">
              <el-input-number v-model="form.heartRate" :min="30" :max="220" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="血糖 (mmol/L)">
              <el-input-number v-model="form.bloodSugar" :step="0.1" :precision="1" :min="1" :max="40" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="血脂 (mmol/L)">
              <el-input-number v-model="form.bloodLipid" :step="0.1" :precision="1" :min="1" :max="20" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="onSubmit">保存记录</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import * as echarts from "echarts";
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Monitor, Plus } from "@element-plus/icons-vue";
import { measurementApi, residentApi } from "../api/modules";
import { useAuthStore } from "../store/auth";
import { formatChartTime, formatFriendlyDateTime } from "../utils/datetime";

const auth = useAuthStore();
const canEdit = computed(() => auth.role === "ADMIN" || auth.role === "DOCTOR");

const loading = ref(false);
const submitting = ref(false);
const residents = ref([]);
const selectedResidentId = ref();
const list = ref([]);
const showDialog = ref(false);
const defaultForm = () => ({
  residentId: null,
  measuredAt: new Date().toISOString().slice(0, 19),
  location: "社区卫生站",
  systolicBp: 120,
  diastolicBp: 80,
  bloodSugar: 5.0,
  bloodLipid: 1.5,
  heartRate: 75,
  sourceType: "MANUAL"
});
const form = ref(defaultForm());

const openAddDialog = () => {
  form.value = defaultForm();
  showDialog.value = true;
};

const chartRef = ref();
let chart;

const loadResidents = async () => {
  try {
    const res = await residentApi.list({}); // 确保传入空对象获取全部
    residents.value = res.data || [];
  } catch (e) {
    console.error("加载居民列表失败", e);
  }
};

const load = async () => {
  if (!selectedResidentId.value) {
    list.value = [];
    return;
  }
  loading.value = true;
  try {
    const res = await measurementApi.list(selectedResidentId.value);
    list.value = res.data || [];
  } catch (e) {
    console.error("加载记录失败", e);
  } finally {
    loading.value = false;
  }
};

const renderTrend = async () => {
  if (!selectedResidentId.value) return;
  try {
    const res = await measurementApi.trend(selectedResidentId.value);
    const data = res.data || [];
    if (data.length === 0) return;

    await nextTick();
    if (!chartRef.value) return;
    chart?.dispose();
    chart = echarts.init(chartRef.value);
    chart.setOption({
      tooltip: { trigger: "axis" },
      legend: { data: ["收缩压", "舒张压", "血糖"], bottom: 0 },
      grid: { left: "3%", right: "4%", bottom: "10%", containLabel: true },
      xAxis: { type: "category", boundaryGap: false, data: data.map((i) => formatChartTime(i.measuredAt)) },
      yAxis: { type: "value" },
      color: ["#1aa094", "#2f4056", "#ef4444"],
      series: [
        { name: "收缩压", type: "line", smooth: true, data: data.map((i) => i.systolicBp) },
        { name: "舒张压", type: "line", smooth: true, data: data.map((i) => i.diastolicBp) },
        { name: "血糖", type: "line", smooth: true, data: data.map((i) => i.bloodSugar) }
      ]
    });
  } catch (e) {
    console.error("加载趋势图失败", e);
  }
};

const onResidentChange = async () => {
  await load();
  await renderTrend();
};

const onSubmit = async () => {
  if (!form.value.residentId) return ElMessage.warning("请选择居民");
  submitting.value = true;
  try {
    await measurementApi.create(form.value);
    ElMessage.success("记录录入成功");
    showDialog.value = false;
    form.value = defaultForm();
    await load();
    await renderTrend();
  } catch (error) {
    // 拦截器处理
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadResidents();
});

onBeforeUnmount(() => {
  chart?.dispose();
});
</script>

<style scoped>
.measurements-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.page-card:hover {
  transform: none !important;
}

.mb-20 { margin-bottom: 20px; }
.p-20 { padding: 20px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  font-size: 20px;
  color: #1aa094;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #2f4056;
}

.chart-box {
  height: 400px;
  width: 100%;
}

.text-danger {
  color: #ef4444;
  font-weight: bold;
}

.animate-fade-in {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 20px 20px;
}

.custom-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  margin-right: 0;
  padding: 20px 24px;
}

.custom-dialog :deep(.el-dialog__title) {
  font-weight: 700;
  color: #2f4056;
}
</style>
