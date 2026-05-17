<template>
  <div class="phm-dashboard animate-fade-in" v-loading="loading">
    <el-alert
      title="全区公共卫生监控看板"
      type="success"
      description="公共卫生管理模式：实时监控全区居民健康档案、健康指标异常分布及预警趋势，辅助制定干预策略。"
      show-icon
      class="mb-20"
    />

    <!-- 核心统计指标 -->
    <el-row :gutter="20" class="mb-20">
      <el-col :span="6">
        <div class="stat-card">
          <div class="label">全区建档总数</div>
          <div class="value text-primary">{{ overview.residentCount }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="label">血压异常占比</div>
          <div class="value text-danger">{{ riskStats.bpAbnormalRate }}%</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="label">血糖异常占比</div>
          <div class="value text-warning">{{ riskStats.bsAbnormalRate }}%</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="label">血脂异常占比</div>
          <div class="value text-info">{{ riskStats.blAbnormalRate }}%</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="page-card mb-20">
          <template #header>
            <div class="card-header"><span class="title">全区居民性别分布</span></div>
          </template>
          <div ref="genderChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="page-card mb-20">
          <template #header>
            <div class="card-header"><span class="title">健康指标异常分布</span></div>
          </template>
          <div ref="indicatorChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 干预达标率简报 -->
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <span class="title">近期干预效果评估 (近6个月)</span>
          <el-button type="primary" link @click="$router.push('/intervention-effect')">详情</el-button>
        </div>
      </template>
      <div ref="effectChartRef" class="chart-box-sm"></div>
    </el-card>
  </div>
</template>

<script setup>
import * as echarts from "echarts";
import { onMounted, onBeforeUnmount, ref, reactive, nextTick } from "vue";
import { statsApi } from "../api/modules";

const loading = ref(false);
const overview = ref({ residentCount: 0, newAlertCount: 0 });
const riskStats = reactive({ bpAbnormalRate: 0, bsAbnormalRate: 0, blAbnormalRate: 0 });

const genderChartRef = ref();
const indicatorChartRef = ref();
const effectChartRef = ref();
let genderChart, indicatorChart, effectChart;

const loadData = async () => {
  loading.value = true;
  try {
    const [ovRes, riskRes, genRes, indRes, effRes] = await Promise.all([
      statsApi.overview(),
      statsApi.riskScreening(),
      statsApi.gender(),
      statsApi.indicatorDistribution(),
      statsApi.interventionEffect()
    ]);

    overview.value = ovRes.data;
    riskStats.bpAbnormalRate = (riskRes.data.bpAbnormalRate * 100).toFixed(1);
    riskStats.bsAbnormalRate = (riskRes.data.bsAbnormalRate * 100).toFixed(1);
    riskStats.blAbnormalRate = (riskRes.data.blAbnormalRate * 100).toFixed(1);

    await nextTick();
    renderCharts(genRes.data, indRes.data, effRes.data);
  } catch (error) {
    console.error("加载公共卫生统计失败", error);
  } finally {
    loading.value = false;
  }
};

const renderCharts = (genderData, indicatorData, effectData) => {
  // 性别饼图
  if (genderChartRef.value) {
    genderChart = echarts.init(genderChartRef.value);
    genderChart.setOption({
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: '60%', data: genderData }]
    });
  }
  // 健康指标异常对比饼图
  if (indicatorChartRef.value) {
    indicatorChart = echarts.init(indicatorChartRef.value);
    indicatorChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
      legend: { bottom: 0, left: 'center' },
      series: [
        {
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: false },
          emphasis: {
            label: { show: true, fontSize: 14, fontWeight: 'bold' }
          },
          labelLine: { show: false },
          data: [
            { name: '异常人数', value: indicatorData.abnormal || 0 },
            { name: '正常人数', value: indicatorData.normal || 0 }
          ],
          color: ['#ef4444', '#22c55e']
        }
      ]
    });
  }
  // 效果趋势
  if (effectChartRef.value) {
    effectChart = echarts.init(effectChartRef.value);
    effectChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { top: 30, bottom: 30, left: 50, right: 20 },
      xAxis: { type: 'category', data: (effectData || []).map(i => i.month) },
      yAxis: { type: 'value', axisLabel: { formatter: '{value}%' } },
      series: [
        { name: '血压达标率', type: 'line', smooth: true, data: (effectData || []).map(i => i.bpRate) },
        { name: '血糖达标率', type: 'line', smooth: true, data: (effectData || []).map(i => i.bsRate) }
      ]
    });
  }
};

onMounted(loadData);
onBeforeUnmount(() => {
  genderChart?.dispose();
  indicatorChart?.dispose();
  effectChart?.dispose();
});
</script>

<style scoped>
.phm-dashboard { max-width: 1200px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }
.chart-box { height: 300px; }
.chart-box-sm { height: 200px; }

.stat-card {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  text-align: center;
  border-top: 4px solid #1aa094;
}
.stat-card .label { color: #64748b; font-size: 14px; margin-bottom: 8px; }
.stat-card .value { font-size: 24px; font-weight: 700; }
.text-primary { color: #1aa094; }
.text-danger { color: #ef4444; }
.text-warning { color: #f59e0b; }
.text-info { color: #3b82f6; }

.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
