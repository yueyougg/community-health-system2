<template>
  <div class="health-overview-container animate-fade-in" v-loading="loading">
    <el-alert
      title="健康概览说明"
      type="success"
      description="在此您可以快速查看您的健康统计信息，包括近期的体征测量趋势。建议您定期测量并录入体征数据。"
      show-icon
      class="mb-20"
    />

    <!-- 数据汇总卡片 -->
    <el-row :gutter="20" class="mb-20">
      <el-col :span="6">
        <div class="stat-card">
          <div class="label">测量记录总数</div>
          <div class="value">{{ stats.measurementCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="label">待处理提醒</div>
          <div class="value text-warning">{{ stats.newAlertCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="label">进行中随访</div>
          <div class="value text-primary">{{ stats.activePlanCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="label">就诊记录数</div>
          <div class="value">{{ stats.visitCount }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 趋势图表 -->
    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <span class="title">健康体征趋势 (最近20次)</span>
        </div>
      </template>
      <div ref="chartRef" class="chart-box"></div>
    </el-card>

    <!-- 最近预警 -->
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span class="title">近期健康提醒</span>
          <el-button type="primary" link @click="$router.push('/health-reminders')">查看更多</el-button>
        </div>
      </template>
      <el-table :data="recentAlerts" stripe style="width: 100%">
        <el-table-column label="类型" width="140">
          <template #default="{ row }">
            {{ formatAlertType(row.alertType) }}
          </template>
        </el-table-column>
        <el-table-column prop="message" label="内容" show-overflow-tooltip />
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="row.level === 'HIGH' ? 'danger' : row.level === 'MEDIUM' ? 'warning' : 'info'" size="small">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import * as echarts from "echarts";
import { onMounted, onBeforeUnmount, ref, nextTick, reactive } from "vue";
import { ElMessage } from "element-plus";
import { residentApi, measurementApi, alertApi, followUpApi, visitRecordApi } from "../api/modules";
import { formatChartTime, formatFriendlyDateTime } from "../utils/datetime";

const loading = ref(false);
const stats = reactive({
  measurementCount: 0,
  newAlertCount: 0,
  activePlanCount: 0,
  visitCount: 0
});
const recentAlerts = ref([]);
const chartRef = ref();
let chart;

const formatDateTime = (dateStr) => formatFriendlyDateTime(dateStr);

const formatAlertType = (type) => {
  const map = {
    HEALTH_METRIC: "健康指标预警",
    FOLLOW_UP: "随访提醒"
  };
  return map[type] || "其他预警";
};

const renderTrend = (data) => {
  if (!chartRef.value || data.length === 0) return;
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
};

const loadData = async () => {
  loading.value = true;
  try {
    // 1. 获取个人档案
    const profileRes = await residentApi.me();
    const profile = profileRes.data;
    if (!profile) return;

    const residentId = profile.id;

    // 2. 并行获取各项统计数据
    const [trendRes, alertRes, planRes, visitRes, measureRes] = await Promise.all([
      measurementApi.trendMe(),
      alertApi.list({ residentId, status: "NEW" }),
      followUpApi.plans(residentId),
      visitRecordApi.list(residentId),
      measurementApi.me()
    ]);

    // 3. 处理统计
    stats.measurementCount = measureRes.data?.length || 0;
    stats.newAlertCount = alertRes.data?.filter(a => a.status === 'NEW').length || 0;
    stats.activePlanCount = planRes.data?.filter(p => p.active).length || 0;
    stats.visitCount = visitRes.data?.length || 0;

    // 4. 最近预警
    recentAlerts.value = (alertRes.data || []).slice(0, 5);

    // 5. 渲染图表
    await nextTick();
    renderTrend(trendRes.data || []);

  } catch (error) {
    console.error("加载概览数据失败", error);
    if (error.response?.status !== 404) {
      ElMessage.error("获取健康概览失败");
    }
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
onBeforeUnmount(() => chart?.dispose());
</script>

<style scoped>
.health-overview-container { max-width: 1200px; margin: 0 auto; }
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
  text-align: center;
}
.stat-card .label { color: #64748b; font-size: 14px; margin-bottom: 8px; }
.stat-card .value { font-size: 28px; font-weight: 700; color: #1e293b; }
.text-warning { color: #f59e0b !important; }
.text-primary { color: #1aa094 !important; }

.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
