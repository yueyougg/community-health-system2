<template>
  <div class="intervention-view">
    <h2>健康干预与评估</h2>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="高危人群自动筛查" name="screening">
        <div class="screening-panel">
          <div class="stat-card blue">
            <h3>居民总数</h3>
            <div class="number">{{ screeningData.totalResidents }}</div>
          </div>
          <div class="stat-card orange">
            <h3>血压异常人群</h3>
            <div class="number">{{ screeningData.highBloodPressure }}</div>
            <div class="rate">占比 {{ toPercent(screeningData.hbpRate) }}%</div>
          </div>
          <div class="stat-card purple">
            <h3>血糖异常人群</h3>
            <div class="number">{{ screeningData.diabetes }}</div>
            <div class="rate">占比 {{ toPercent(screeningData.diabetesRate) }}%</div>
          </div>
          <div class="stat-card green">
            <h3>血脂异常人群</h3>
            <div class="number">{{ screeningData.bloodLipid }}</div>
            <div class="rate">占比 {{ toPercent(screeningData.blRate) }}%</div>
          </div>
        </div>
        
        <el-row :gutter="20" style="margin-top: 20px">
          <el-col :span="12">
            <el-card class="chart-card">
              <template #header>
                <div class="card-header">
                  <span>男女健康指标异常分布</span>
                </div>
              </template>
              <div ref="genderChartRef" class="chart-container-sm"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="chart-card">
              <template #header>
                <div class="card-header">
                  <span>年龄段指标异常分布</span>
                  <el-radio-group v-model="selectedIndicator" size="small" @change="updateAgeGroupChart">
                    <el-radio-button label="bloodPressure">血压</el-radio-button>
                    <el-radio-button label="bloodSugar">血糖</el-radio-button>
                    <el-radio-button label="bloodLipid">血脂</el-radio-button>
                  </el-radio-group>
                </div>
              </template>
              <div ref="ageGroupChartRef" class="chart-container-sm"></div>
            </el-card>
          </el-col>
        </el-row>
        
        <el-alert
          title="筛查标准说明"
          type="info"
          :closable="false"
          style="margin-top: 20px"
        >
          <template #default>
            <ul>
              <li><b>血压异常：</b> 收缩压 ≥ 140 mmHg 或 舒张压 ≥ 90 mmHg</li>
              <li><b>血糖异常：</b> 空腹血糖 ≥ 7.0 mmol/L</li>
              <li><b>血脂异常：</b> 血脂 ≥ 5.2 mmol/L</li>
            </ul>
          </template>
        </el-alert>
      </el-tab-pane>

      <el-tab-pane label="干预效果评估" name="assessment">
        <div class="assessment-panel">
          <div class="control-rates">
            <el-progress type="dashboard" :percentage="Number((interventionData.hbpControlRate * 100).toFixed(1))" :color="colors">
              <template #default>
                <span class="percentage-value">{{ toPercent(interventionData.hbpControlRate) }}%</span>
                <span class="percentage-label">高血压控制率</span>
              </template>
            </el-progress>
            <el-progress type="dashboard" :percentage="Number((interventionData.diabetesControlRate * 100).toFixed(1))" :color="colors">
              <template #default>
                <span class="percentage-value">{{ toPercent(interventionData.diabetesControlRate) }}%</span>
                <span class="percentage-label">糖尿病控制率</span>
              </template>
            </el-progress>
          </div>

          <el-card class="chart-card">
            <template #header>
              <div class="card-header">
                <span>干预后高危人群数量变化趋势</span>
              </div>
            </template>
            <div ref="trendChartRef" class="chart-container"></div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch } from "vue";
import * as echarts from "echarts";
import { statsApi } from "../api/modules";

const activeTab = ref("screening");
const trendChartRef = ref(null);
const genderChartRef = ref(null);
const ageGroupChartRef = ref(null);
let trendChart = null;
let genderChart = null;
let ageGroupChart = null;

const selectedIndicator = ref("bloodPressure");

const screeningData = reactive({
  totalResidents: 0,
  highBloodPressure: 0,
  diabetes: 0,
  bloodLipid: 0,
  hbpRate: 0,
  diabetesRate: 0,
  blRate: 0
});

const interventionData = reactive({
  hbpControlRate: 0,
  diabetesControlRate: 0,
  trends: []
});

const genderAbnormalData = ref({ male: [], female: [] });
const ageGroupAbnormalData = ref({ bloodPressure: [], bloodSugar: [], bloodLipid: [] });

const colors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#6f7ad3', percentage: 100 }
];

const toPercent = (val) => (val * 100).toFixed(1);

const fetchScreeningData = async () => {
  try {
    const res = await statsApi.riskScreening();
    Object.assign(screeningData, {
      totalResidents: res.data.totalResidents,
      highBloodPressure: res.data.bpAbnormal,
      diabetes: res.data.bsAbnormal,
      bloodLipid: res.data.blAbnormal,
      hbpRate: res.data.bpAbnormalRate,
      diabetesRate: res.data.bsAbnormalRate,
      blRate: res.data.blAbnormalRate
    });
  } catch (error) {
    console.error(error);
  }
};

const fetchGenderAbnormalData = async () => {
  try {
    const res = await statsApi.genderAbnormalStats();
    genderAbnormalData.value = res.data;
    initGenderChart();
  } catch (error) {
    console.error(error);
  }
};

const fetchAgeGroupAbnormalData = async () => {
  try {
    const res = await statsApi.ageGroupAbnormalStats();
    ageGroupAbnormalData.value = res.data;
    initAgeGroupChart();
  } catch (error) {
    console.error(error);
  }
};

const initGenderChart = () => {
  if (!genderChartRef.value) return;
  
  if (genderChart) {
    genderChart.dispose();
  }
  
  genderChart = echarts.init(genderChartRef.value);
  
  const maleData = genderAbnormalData.value.male || [];
  const femaleData = genderAbnormalData.value.female || [];
  
  const maleAbnormal = maleData.find(d => d.name === '异常')?.value || 0;
  const maleNormal = maleData.find(d => d.name === '正常')?.value || 0;
  const femaleAbnormal = femaleData.find(d => d.name === '异常')?.value || 0;
  const femaleNormal = femaleData.find(d => d.name === '正常')?.value || 0;

  genderChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['异常', '正常'] },
    xAxis: { type: 'category', data: ['男性', '女性'] },
    yAxis: { type: 'value' },
    series: [
      {
        name: '异常',
        type: 'bar',
        stack: 'total',
        data: [maleAbnormal, femaleAbnormal],
        itemStyle: { color: '#ef4444' }
      },
      {
        name: '正常',
        type: 'bar',
        stack: 'total',
        data: [maleNormal, femaleNormal],
        itemStyle: { color: '#22c55e' }
      }
    ]
  });
};

const initAgeGroupChart = () => {
  if (!ageGroupChartRef.value) return;
  
  if (ageGroupChart) {
    ageGroupChart.dispose();
  }
  
  ageGroupChart = echarts.init(ageGroupChartRef.value);
  updateAgeGroupChart();
};

const updateAgeGroupChart = () => {
  if (!ageGroupChart) return;
  
  const data = ageGroupAbnormalData.value[selectedIndicator.value] || [];
  const ageGroups = data.map(d => d.ageGroup);
  const abnormalCounts = data.map(d => d.abnormal);

  ageGroupChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { 
      type: 'category', 
      data: ageGroups,
      axisLabel: { rotate: 30 }
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '异常人数',
        type: 'bar',
        data: abnormalCounts,
        itemStyle: { color: '#f59e0b' }
      }
    ]
  });
};

const fetchInterventionData = async () => {
  try {
    const res = await statsApi.interventionEffect();
    Object.assign(interventionData, res.data);
    initChart();
  } catch (error) {
    console.error(error);
  }
};

const initChart = () => {
  if (!trendChartRef.value) return;
  
  if (trendChart) {
    trendChart.dispose();
  }
  
  trendChart = echarts.init(trendChartRef.value);
  const months = interventionData.trends.map(item => item.month);
  const hbpData = interventionData.trends.map(item => item.hbp);
  const diabetesData = interventionData.trends.map(item => item.diabetes);

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['高血压高危', '糖尿病高危'] },
    xAxis: { type: 'category', data: months },
    yAxis: { type: 'value' },
    series: [
      {
        name: '高血压高危',
        type: 'line',
        data: hbpData,
        smooth: true,
        itemStyle: { color: '#ff7f50' }
      },
      {
        name: '糖尿病高危',
        type: 'line',
        data: diabetesData,
        smooth: true,
        itemStyle: { color: '#87cefa' }
      }
    ]
  });
};

watch(activeTab, (newVal) => {
  if (newVal === 'assessment') {
    nextTick(() => {
      fetchInterventionData();
    });
  }
});

onMounted(() => {
  fetchScreeningData();
  fetchGenderAbnormalData();
  fetchAgeGroupAbnormalData();
});
</script>

<style scoped>
.intervention-view {
  padding: 20px;
  background: rgb(255,250,235);
  border-radius: 8px;
}

.screening-panel {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  padding: 20px;
  border-radius: 8px;
  color: #fff;
  text-align: center;
}

.stat-card.blue { background: linear-gradient(135deg, #36d1dc, #5b86e5); }
.stat-card.orange { background: linear-gradient(135deg, #ff9966, #ff5e62); }
.stat-card.purple { background: linear-gradient(135deg, #da22ff, #9733ee); }
.stat-card.green { background: linear-gradient(135deg, #11998e, #38ef7d); }

.stat-card h3 { margin: 0 0 10px; font-size: 16px; opacity: 0.9; }
.stat-card .number { font-size: 36px; font-weight: bold; }
.stat-card .rate { margin-top: 8px; font-size: 14px; opacity: 0.8; }

.control-rates {
  display: flex;
  justify-content: space-around;
  margin-bottom: 40px;
  padding: 20px 0;
}

.percentage-value { display: block; margin-top: 10px; font-size: 28px; }
.percentage-label { display: block; margin-top: 10px; font-size: 12px; color: #909399; }

.chart-container {
  height: 400px;
}

.chart-container-sm {
  height: 300px;
}

.chart-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
