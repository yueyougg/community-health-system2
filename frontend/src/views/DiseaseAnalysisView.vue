<template>
  <div class="analysis-view animate-fade-in" v-loading="loading">
    <el-alert
      title="全区疾病分布分析"
      type="info"
      description="公共卫生管理模式：通过对全区居民健康档案的深度分析，识别高发疾病谱及高危人群分布，为制定公共卫生政策提供数据支撑。"
      show-icon
      class="mb-20"
    />

    <el-row :gutter="20" class="mb-20">
      <el-col :span="12">
        <el-card class="page-card shadow-hover">
          <template #header>
            <div class="card-header">
              <span class="title">社区疾病谱分布 (TOP 10)</span>
            </div>
          </template>
          <div ref="diseaseChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="page-card shadow-hover">
          <template #header>
            <div class="card-header">
              <span class="title">男女健康指标异常分布</span>
            </div>
          </template>
          <div ref="genderChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <span class="title">年龄段指标异常分布</span>
          <el-radio-group v-model="selectedIndicator" size="small" @change="updateAgeGroupChart">
            <el-radio-button label="bloodPressure">血压</el-radio-button>
            <el-radio-button label="bloodSugar">血糖</el-radio-button>
            <el-radio-button label="bloodLipid">血脂</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="ageGroupChartRef" class="chart-box-sm"></div>
    </el-card>

    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <span class="title">常见慢病统计明细</span>
        </div>
      </template>
      <el-table :data="diseaseData" border stripe style="width: 100%">
        <el-table-column prop="name" label="疾病名称" />
        <el-table-column prop="value" label="确诊人数" sortable align="center" />
        <el-table-column label="全区患病率参考" width="300">
          <template #default="{ row }">
            <el-progress :percentage="calculatePercentage(row.value)" :stroke-width="12" :color="getProgressColor(row.value)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, nextTick, onBeforeUnmount } from "vue";
import * as echarts from "echarts";
import { statsApi } from "../api/modules";

const loading = ref(false);
const diseaseChartRef = ref(null);
const genderChartRef = ref(null);
const ageGroupChartRef = ref(null);
const diseaseData = ref([]);
const totalResidents = ref(0);

let diseaseChart;
let genderChart;
let ageGroupChart;

const selectedIndicator = ref("bloodPressure");
const genderAbnormalData = ref({ male: [], female: [] });
const ageGroupAbnormalData = ref({ bloodPressure: [], bloodSugar: [], bloodLipid: [] });

const calculatePercentage = (val) => {
  if (totalResidents.value === 0) return 0;
  return Number(((val / totalResidents.value) * 100).toFixed(1));
};

const getProgressColor = (val) => {
  const rate = calculatePercentage(val);
  if (rate > 15) return '#f56c6c';
  if (rate > 8) return '#e6a23c';
  return '#67c23a';
};

const loadData = async () => {
  loading.value = true;
  try {
    const [diseaseRes, genderRes, ageGroupRes, riskRes] = await Promise.all([
      statsApi.disease(),
      statsApi.genderAbnormalStats(),
      statsApi.ageGroupAbnormalStats(),
      statsApi.riskScreening()
    ]);
    
    diseaseData.value = diseaseRes.data || [];
    genderAbnormalData.value = genderRes.data || { male: [], female: [] };
    ageGroupAbnormalData.value = ageGroupRes.data || { bloodPressure: [], bloodSugar: [], bloodLipid: [] };
    totalResidents.value = riskRes.data.totalResidents;

    await nextTick();
    renderCharts();
  } catch (e) {
    console.error("加载分析数据失败", e);
  } finally {
    loading.value = false;
  }
};

const renderCharts = () => {
  renderDiseaseChart();
  renderGenderChart();
  renderAgeGroupChart();
};

const renderDiseaseChart = () => {
  if (diseaseChartRef.value) {
    diseaseChart?.dispose();
    diseaseChart = echarts.init(diseaseChartRef.value);
    diseaseChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
      legend: { bottom: '0', left: 'center', icon: 'circle' },
      series: [{
        name: '疾病分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        data: diseaseData.value
      }]
    });
  }
};

const renderGenderChart = () => {
  if (genderChartRef.value) {
    genderChart?.dispose();
    genderChart = echarts.init(genderChartRef.value);
    
    const maleData = genderAbnormalData.value.male || [];
    const femaleData = genderAbnormalData.value.female || [];
    
    const maleAbnormal = maleData.find(d => d.name === '异常')?.value || 0;
    const maleNormal = maleData.find(d => d.name === '正常')?.value || 0;
    const femaleAbnormal = femaleData.find(d => d.name === '异常')?.value || 0;
    const femaleNormal = femaleData.find(d => d.name === '正常')?.value || 0;

    genderChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { data: ['异常', '正常'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: ['男性', '女性'] },
      yAxis: { type: 'value' },
      series: [
        {
          name: '异常',
          type: 'bar',
          stack: 'total',
          data: [maleAbnormal, femaleAbnormal],
          itemStyle: { color: '#ef4444' },
          label: { show: true, position: 'inside' }
        },
        {
          name: '正常',
          type: 'bar',
          stack: 'total',
          data: [maleNormal, femaleNormal],
          itemStyle: { color: '#22c55e' },
          label: { show: true, position: 'inside' }
        }
      ]
    });
  }
};

const renderAgeGroupChart = () => {
  if (ageGroupChartRef.value) {
    ageGroupChart?.dispose();
    ageGroupChart = echarts.init(ageGroupChartRef.value);
    updateAgeGroupChart();
  }
};

const updateAgeGroupChart = () => {
  if (!ageGroupChart) return;
  
  const data = ageGroupAbnormalData.value[selectedIndicator.value] || [];
  const ageGroups = data.map(d => d.ageGroup);
  const abnormalCounts = data.map(d => d.abnormal);

  ageGroupChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { 
      type: 'category', 
      data: ageGroups,
      axisLabel: { rotate: 20 }
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '异常人数',
        type: 'bar',
        data: abnormalCounts,
        itemStyle: { color: '#f59e0b' },
        label: { show: true, position: 'top' }
      }
    ]
  });
};

onMounted(loadData);
onBeforeUnmount(() => {
  diseaseChart?.dispose();
  genderChart?.dispose();
  ageGroupChart?.dispose();
});
</script>

<style scoped>
.analysis-view { max-width: 1200px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }
.chart-box { height: 350px; width: 100%; }
.chart-box-sm { height: 280px; width: 100%; }
.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
