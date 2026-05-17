<template>
  <div class="doctor-dashboard animate-fade-in" v-loading="loading">
    <el-alert
      title="医生工作台"
      type="info"
      description="在此您可以全面掌握所辖居民的健康状态、预警情况以及待处理的随访任务。建议您优先处理高风险预警。"
      show-icon
      class="mb-20"
    />

    <!-- 统计卡片 -->
    <el-row :gutter="20" class="mb-20">
      <el-col :span="8">
        <div class="stat-card">
          <div class="icon-box bg-blue"><el-icon><UserFilled /></el-icon></div>
          <div class="content">
            <div class="label">管理居民数</div>
            <div class="value">{{ stats.residentCount }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="icon-box bg-red"><el-icon><Warning /></el-icon></div>
          <div class="content">
            <div class="label">待处理预警</div>
            <div class="value text-danger">{{ stats.newAlertCount }}</div>
          </div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="icon-box bg-orange"><el-icon><Calendar /></el-icon></div>
          <div class="content">
            <div class="label">随访计划数</div>
            <div class="value text-warning">{{ stats.followUpPlanCount }}</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <!-- 居民分布图 -->
      <el-col :span="12">
        <el-card class="page-card mb-20">
          <template #header>
            <div class="card-header"><span class="title">社区居民健康指标分布</span></div>
          </template>
          <div ref="diseaseChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <!-- 预警动态 -->
      <el-col :span="12">
        <el-card class="page-card mb-20">
          <template #header>
            <div class="card-header">
              <span class="title">待处理预警 (最新5条)</span>
              <el-button type="primary" link @click="$router.push('/alerts')">更多</el-button>
            </div>
          </template>
          <el-table :data="recentAlerts" stripe style="width: 100%" size="small">
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                {{ formatAlertType(row.alertType) }}
              </template>
            </el-table-column>
            <el-table-column prop="message" label="预警描述" show-overflow-tooltip />
            <el-table-column label="产生时间" width="160">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import * as echarts from "echarts";
import { onMounted, onBeforeUnmount, ref, reactive, nextTick } from "vue";
import { statsApi, alertApi } from "../api/modules";
import { UserFilled, Warning, Calendar } from "@element-plus/icons-vue";
import { formatFriendlyDateTime } from "../utils/datetime";

const loading = ref(false);
const stats = reactive({
  residentCount: 0,
  newAlertCount: 0,
  followUpPlanCount: 0
});
const recentAlerts = ref([]);
const diseaseChartRef = ref();
let diseaseChart;

const formatDateTime = (dateStr) => formatFriendlyDateTime(dateStr);

const formatAlertType = (type) => {
  const map = {
    HEALTH_METRIC: "健康指标预警",
    FOLLOW_UP: "随访提醒"
  };
  return map[type] || "其他预警";
};

const renderDiseaseChart = (data) => {
  if (!diseaseChartRef.value) return;
  diseaseChart?.dispose();
  diseaseChart = echarts.init(diseaseChartRef.value);
  diseaseChart.setOption({
    tooltip: { trigger: "item" },
    legend: { bottom: 0, left: "center" },
    series: [
      {
        name: "异常记录数",
        type: "pie",
        radius: ["45%", "70%"],
        center: ["50%", "45%"],
        avoidLabelOverlap: false,
        label: { show: true, formatter: "{b}\n{c}" },
        labelLine: { show: true },
        data
      }
    ]
  });
};

const loadData = async () => {
  loading.value = true;
  try {
    const [overviewRes, indicatorRes, alertRes] = await Promise.all([
      statsApi.overview(),
      statsApi.indicatorDistribution(),
      alertApi.list({ status: "NEW" })
    ]);

    Object.assign(stats, overviewRes.data);
    recentAlerts.value = (alertRes.data || []).slice(0, 5);

    await nextTick();
    renderDiseaseChart(indicatorRes.data || []);
  } catch (error) {
    console.error("加载工作台数据失败", error);
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
onBeforeUnmount(() => diseaseChart?.dispose());
</script>

<style scoped>
.doctor-dashboard { max-width: 1200px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }
.chart-box { height: 350px; width: 100%; }

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 15px;
}

.icon-box {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.bg-blue { background: #3b82f6; }
.bg-red { background: #ef4444; }
.bg-orange { background: #f59e0b; }
.bg-green { background: #10b981; }

.stat-card .label { color: #64748b; font-size: 14px; }
.stat-card .value { font-size: 24px; font-weight: 700; color: #1e293b; }
.text-danger { color: #ef4444 !important; }
.text-warning { color: #f59e0b !important; }
.text-success { color: #10b981 !important; }

.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
