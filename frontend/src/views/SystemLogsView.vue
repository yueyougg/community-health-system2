<template>
  <div class="system-logs-container animate-fade-in" v-loading="loading">
    <el-alert
      title="操作日志说明"
      type="info"
      description="记录系统内所有关键业务操作，包括操作人、操作内容、请求详情等，用于安全审计和追踪。"
      show-icon
      class="mb-20"
    />
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Operation /></el-icon>
            <span class="title">系统操作日志</span>
          </div>
          <el-button @click="loadData">
            <el-icon><Refresh /></el-icon> 刷新
          </el-button>
        </div>
      </template>

      <el-table :data="list" border stripe>
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column label="操作事项" width="220">
          <template #default="{ row }">
            <span class="module-tag">{{ row.moduleName }}</span>
            <span class="action-name">{{ row.actionName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="httpMethod" label="请求方法" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getMethodType(row.httpMethod)" size="small">{{ row.httpMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestPath" label="请求路径" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column label="操作时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无操作日志" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { Operation, Refresh } from "@element-plus/icons-vue";
import { systemApi } from "../api/modules";
import { formatFriendlyDateTime } from "../utils/datetime";

const loading = ref(false);
const list = ref([]);

const getMethodType = (method) => {
  const map = { 'POST': 'success', 'PUT': 'warning', 'PATCH': 'warning', 'DELETE': 'danger', 'GET': 'info' };
  return map[method?.toUpperCase()] || '';
};

const formatDateTime = (dateStr) => formatFriendlyDateTime(dateStr);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await systemApi.logs();
    list.value = res.data || [];
  } catch (e) {
    console.error("加载日志失败", e);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.system-logs-container { max-width: 1200px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; gap: 10px; }
.header-icon { font-size: 20px; color: #1aa094; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }
.module-tag {
  background: #f0f9eb;
  color: #67c23a;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 8px;
}
.action-name {
  color: #606266;
  font-size: 14px;
}
.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
