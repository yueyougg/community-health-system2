<template>
  <div class="intervention-effect-container animate-fade-in" v-loading="loading">
    <el-alert
      title="健康干预效果记录与评估"
      type="success"
      description="记录干预前后指标变化，系统自动给出效果评估，并以图示方式比较变化趋势。"
      show-icon
      class="mb-20"
    />

    <el-card v-if="canEdit" class="page-card mb-20">
      <template #header><div class="card-header"><span class="title">新增干预效果记录</span></div></template>
      <el-form :model="form" label-position="top">
        <el-row :gutter="20">
          <el-col :span="8"><el-form-item label="干预类型"><el-input v-model="form.interventionType" /></el-form-item></el-col>
          <el-col :span="8">
            <el-form-item label="目标指标">
              <el-select v-model="form.targetMetric" placeholder="选择目标指标" style="width:100%">
                <el-option label="收缩压" value="收缩压" />
                <el-option label="舒张压" value="舒张压" />
                <el-option label="血糖" value="血糖" />
                <el-option label="血脂" value="血脂" />
                <el-option label="体重" value="体重" />
                <el-option label="BMI" value="BMI" />
                <el-option label="心率" value="心率" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="居民">
              <el-select v-model="form.residentId" filterable clearable placeholder="选择居民" style="width:100%">
                <el-option v-for="item in residents" :key="item.id" :label="`${item.name} (${item.archiveNo})`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="8"><el-form-item label="干预前"><el-input-number v-model="form.beforeValue" :step="0.1" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="干预后"><el-input-number v-model="form.afterValue" :step="0.1" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="说明"><el-input v-model="form.notes" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <div class="actions"><el-button type="primary" :loading="submitting" @click="saveRecord">保存记录</el-button></div>
    </el-card>

    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <span class="title">干预前后指标变化图</span>
          <el-select v-model="selectedResidentId" filterable clearable placeholder="选择居民筛选" style="width: 220px" @change="renderChart">
            <el-option v-for="item in residentsWithRecords" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </div>
      </template>
      <div ref="chartRef" class="chart-box"></div>
    </el-card>

    <el-card class="page-card">
      <template #header><div class="card-header"><span class="title">干预效果记录</span></div></template>
      <el-table :data="displayRecords" border stripe>
        <el-table-column label="记录时间" width="170"><template #default="{ row }">{{ formatFriendlyDateTime(row.interventionDate) }}</template></el-table-column>
        <el-table-column label="针对对象" width="120">
          <template #default="{ row }">{{ row.residentName || '未指定' }}</template>
        </el-table-column>
        <el-table-column prop="interventionType" label="干预类型" width="120" />
        <el-table-column prop="targetMetric" label="目标指标" width="120" />
        <el-table-column prop="beforeValue" label="干预前" width="90" />
        <el-table-column prop="afterValue" label="干预后" width="90" />
        <el-table-column label="说明" show-overflow-tooltip>
          <template #default="{ row }">{{ row.notes || '无' }}</template>
        </el-table-column>
        <el-table-column label="自动评估" show-overflow-tooltip>
          <template #default="{ row }">{{ row.autoEvaluation }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import * as echarts from "echarts";
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { interventionApi, residentApi } from "../api/modules";
import { useAuthStore } from "../store/auth";
import { formatFriendlyDateTime } from "../utils/datetime";

const auth = useAuthStore();
const canEdit = computed(() => ["DOCTOR", "ADMIN", "PUBLIC_HEALTH_MANAGER"].includes(auth.role));
const loading = ref(false);
const submitting = ref(false);
const chartRef = ref();
const records = ref([]);
const residents = ref([]);
const selectedResidentId = ref(null);
let chart;

const form = ref({
  residentId: null,
  interventionType: "随访管理",
  targetMetric: "收缩压",
  beforeValue: null,
  afterValue: null,
  notes: ""
});

const residentsWithRecords = computed(() => {
  const residentIds = new Set(records.value.filter(r => r.residentId).map(r => r.residentId));
  return residents.value.filter(r => residentIds.has(r.id));
});

const filteredRecords = computed(() => {
  if (!selectedResidentId.value) {
    return records.value;
  }
  return records.value.filter(r => r.residentId === selectedResidentId.value);
});

const displayRecords = computed(() => {
  if (!selectedResidentId.value) {
    return records.value;
  }
  return records.value.filter(r => r.residentId === selectedResidentId.value);
});

const renderChart = async () => {
  await nextTick();
  if (!chartRef.value) return;
  chart?.dispose();
  chart = echarts.init(chartRef.value);
  
  const chartData = filteredRecords.value;
  
  if (chartData.length === 0) {
    chart.setOption({
      title: {
        text: selectedResidentId.value ? '该居民暂无干预记录' : '暂无干预记录',
        left: 'center',
        top: 'center',
        textStyle: { color: '#909399', fontSize: 14 }
      }
    });
    return;
  }
  
  chart.setOption({
    tooltip: { trigger: "axis" },
    legend: { data: ["干预前", "干预后"], bottom: 0 },
    grid: { left: "3%", right: "4%", bottom: "10%", containLabel: true },
    xAxis: { type: "category", data: chartData.map((i) => `${i.targetMetric}#${i.id}`), boundaryGap: false },
    yAxis: { type: "value" },
    series: [
      { name: "干预前", type: "line", smooth: true, data: chartData.map((i) => i.beforeValue) },
      { name: "干预后", type: "line", smooth: true, data: chartData.map((i) => i.afterValue) }
    ]
  });
};

const loadData = async () => {
  loading.value = true;
  try {
    const [res, residentRes] = await Promise.all([
      interventionApi.list({}),
      residentApi.list({})
    ]);
    records.value = res.data || [];
    residents.value = residentRes.data || [];
    await renderChart();
  } finally {
    loading.value = false;
  }
};

const saveRecord = async () => {
  if (!form.value.residentId) {
    return ElMessage.warning("请选择针对对象（居民）");
  }
  if (!form.value.targetMetric || form.value.beforeValue == null || form.value.afterValue == null) {
    return ElMessage.warning("请填写完整的指标与干预前后值");
  }
  submitting.value = true;
  try {
    await interventionApi.create(form.value);
    ElMessage.success("干预记录已保存");
    form.value = { residentId: null, interventionType: "随访管理", targetMetric: "收缩压", beforeValue: null, afterValue: null, notes: "" };
    await loadData();
  } finally {
    submitting.value = false;
  }
};

onMounted(loadData);
onBeforeUnmount(() => chart?.dispose());
</script>

<style scoped>
.intervention-effect-container { max-width: 1200px; margin: 0 auto; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0,0,0,.05); }
.mb-20 { margin-bottom: 20px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }
.actions { display: flex; justify-content: flex-end; }
.chart-box { height: 360px; width: 100%; }
.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>