<!--系统数据面板-->
<template>
  <div class="dashboard-view animate-fade-in" v-loading="loading">
    <el-alert
      title="系统维护看板"
      type="warning"
      description="管理员模式：监控系统运行状态、资源负载及数据库备份情况，确保系统稳定运行。"
      show-icon
      class="mb-20"
    />

    <!-- 系统状态统计 -->
    <el-row :gutter="20" class="mb-20">
      <el-col :span="6">
        <div class="stat-card sys-card">
          <div class="label">系统用户总数</div>
          <div class="value">{{ systemStats.totalUsers }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card sys-card">
          <div class="label">数据库大小 / 备份数</div>
          <div class="value">{{ systemStats.dbSize }} / {{ systemStats.totalBackups }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card sys-card">
          <div class="label">当天登录数</div>
          <div class="value">{{ systemStats.todayLogins }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card sys-card">
          <div class="label">系统运行时间</div>
          <div class="value">{{ systemStats.uptimeFormatted || '加载中...' }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 系统资源监控 -->
    <el-row :gutter="20" class="mb-20">
      <el-col :span="12">
        <div class="stat-card resource-card">
          <div class="label">CPU 使用率</div>
          <div class="value">{{ systemStats.cpuUsage || '0%' }}</div>
          <div class="detail">{{ systemStats.cpuDetail || '加载中...' }}</div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="stat-card resource-card">
          <div class="label">内存使用率</div>
          <div class="value">{{ systemStats.memoryUsage || '0%' }}</div>
          <div class="detail">{{ systemStats.memoryDetail || '加载中...' }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 系统统计图表 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="page-card mb-20">
          <template #header>
            <div class="card-header"><span class="title">角色登录统计</span></div>
          </template>
          <div ref="roleChartRef" class="chart"></div>
          <div v-if="!roleLoginStats.length" class="empty-tip">
            <el-empty description="暂无登录统计信息" :image-size="80" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="page-card mb-20">
          <template #header>
            <div class="card-header"><span class="title">最近登录记录</span></div>
          </template>
          <div class="login-records">
            <el-table v-if="recentLogins.length" :data="recentLogins" style="width: 100%">
              <el-table-column prop="username" label="用户名" />
              <el-table-column prop="role" label="角色" />
              <el-table-column prop="loginTime" label="登录时间" />
              <el-table-column prop="ipAddress" label="IP地址" />
            </el-table>
            <div v-else class="empty-tip">
              <el-empty description="暂无最近登录记录" :image-size="80" />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import * as echarts from "echarts";
import { nextTick, onBeforeUnmount, onMounted, ref, reactive } from "vue";
import { ElMessage } from "element-plus";
import { statsApi, systemApi } from "../api/modules";

const loading = ref(false);
const systemStats = reactive({
  totalUsers: 0,
  totalLogs: 0,
  totalBackups: 0,
  todayLogins: 0,
  dbSize: "0 MB",
  cpuUsage: "0%",
  memoryUsage: "0%",
  uptimeFormatted: "00:00:00",
  cpuDetail: "加载中...",
  memoryDetail: "加载中..."
});

const recentLogins = ref([]);
const roleLoginStats = ref([]);

const roleChartRef = ref();
let roleChart;

let uptimeInterval = null;
let resourceInterval = null;

const load = async () => {
  loading.value = true;
  try {
    const [sysRes] = await Promise.all([
      systemApi.stats()
    ]);
    
    Object.assign(systemStats, {
      totalUsers: sysRes.data.totalUsers,
      totalLogs: sysRes.data.totalLogs,
      totalBackups: sysRes.data.totalBackups,
      todayLogins: sysRes.data.todayLogins,
      dbSize: sysRes.data.dbSize,
      cpuUsage: sysRes.data.cpuUsage,
      memoryUsage: sysRes.data.memoryUsage,
      uptimeFormatted: sysRes.data.uptime?.uptimeFormatted || "00:00:00",
      cpuDetail: `${sysRes.data.cpuUsageDetail?.availableProcessors || 0} 核`,
      memoryDetail: `${sysRes.data.memoryUsageDetail?.usedMemory || "0 MB"} / ${sysRes.data.memoryUsageDetail?.maxMemory || "0 GB"}`
    });
    
    recentLogins.value = sysRes.data.recentLogins || [];
    roleLoginStats.value = sysRes.data.roleLoginStats || [];
    
    await nextTick();
    renderCharts();
  } catch (error) {
    console.error("加载管理员看板失败", error);
    ElMessage.error("获取统计数据失败");
  } finally {
    loading.value = false;
  }
};

const loadMonitorData = async () => {
  try {
    const res = await systemApi.monitor();
    const data = res.data;
    
    if (data.uptime) {
      systemStats.uptimeFormatted = data.uptime.uptimeFormatted;
    }
    
    if (data.cpu) {
      systemStats.cpuUsage = data.cpu.cpuUsage;
      systemStats.cpuDetail = `${data.cpu.availableProcessors} 核 | 负载: ${data.cpu.systemLoadAverage}`;
    }
    
    if (data.memory) {
      systemStats.memoryUsage = data.memory.memoryUsage;
      systemStats.memoryDetail = `${data.memory.usedMemory} / ${data.memory.maxMemory}`;
    }
  } catch (error) {
    console.error("加载监控数据失败", error);
  }
};

const renderCharts = () => {
  if (roleChartRef.value) {
    roleChart = echarts.init(roleChartRef.value);
    
    if (roleLoginStats.value.length > 0) {
      roleChart.setOption({
        tooltip: { trigger: "item" },
        series: [{
          type: "pie",
          radius: ["35%", "65%"],
          data: roleLoginStats.value,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          }
        }]
      });
    } else {
      roleChart.setOption({
        title: {
          text: "暂无登录统计信息",
          left: "center",
          top: "40%",
          textStyle: {
            fontSize: 14,
            color: "#909399"
          }
        }
      });
    }
  }
};

onMounted(() => {
  load();
  
  // 每秒更新运行时间
  uptimeInterval = setInterval(() => {
    loadMonitorData();
  }, 1000);
  
  // 每5秒更新CPU和内存
  resourceInterval = setInterval(() => {
    loadMonitorData();
  }, 5000);
});

onBeforeUnmount(() => {
  roleChart?.dispose();
  if (uptimeInterval) {
    clearInterval(uptimeInterval);
    uptimeInterval = null;
  }
  if (resourceInterval) {
    clearInterval(resourceInterval);
    resourceInterval = null;
  }
});
</script>

<style scoped>
.dashboard-view {
  max-width: 1200px;
  margin: 0 auto;
}

.mb-20 {
  margin-bottom: 20px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-card {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  text-align: center;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
}

.sys-card {
  border-top: 4px solid #f59e0b;
}

.resource-card {
  border-top: 4px solid #1aa094;
}

.phm-card {
  border-top: 4px solid #1aa094;
}

.stat-card .label {
  color: #64748b;
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-card .value {
  font-size: 24px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 4px;
}

.stat-card .detail {
  color: #94a3b8;
  font-size: 12px;
}

.page-card {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #2f4056;
  margin-bottom: 20px;
  text-align: center;
}

.chart {
  height: 350px;
}

.login-records {
  max-height: 350px;
  overflow-y: auto;
  position: relative;
}

.empty-tip {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
  text-align: center;
}

.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>