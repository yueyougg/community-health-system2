<!--预警页面-->
<template>
  <div class="alerts-view">
    <el-alert
      title="预警中心说明"
      type="warning"
      description="系统会自动根据居民的健康检测数据和随访计划生成预警信息。请及时处理高风险预警。"
      show-icon
      class="mb-20"
    />
    <div class="page-card">
    <div class="toolbar">
      <el-select v-model="status" placeholder="状态筛选" style="width: 180px" @change="load">
        <el-option label="全部状态" value="" />
        <el-option label="待处理" value="NEW" />
        <el-option label="处理中" value="PROCESSING" />
        <el-option label="已完成" value="DONE" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="list" border stripe>
      <el-table-column prop="id" label="ID" width="70" align="center" />
      <el-table-column prop="residentName" label="居民姓名" width="120" align="center" />
      <el-table-column label="预警类型" width="140" align="center">
        <template #default="{ row }">
          {{ formatAlertType(row.alertType) }}
        </template>
      </el-table-column>
      <el-table-column prop="level" label="预警级别" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.level === 'HIGH' ? 'danger' : (row.level === 'MEDIUM' ? 'warning' : 'info')">
            {{ row.level === 'HIGH' ? '高风险' : (row.level === 'MEDIUM' ? '中风险' : '低风险') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="预警详情" show-overflow-tooltip />
      <el-table-column prop="status" label="当前状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)" effect="plain">
            {{ getStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <base-button size="small" type="warning" @click="setStatus(row, 'PROCESSING')">处理中</base-button>
          <base-button size="small" type="success" @click="setStatus(row, 'DONE')">已处理</base-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</div>
</template>

<script setup>
import { computed, ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { alertApi, residentApi } from "../api/modules";
import { useAuthStore } from "../store/auth";
import { formatFriendlyDateTime } from "../utils/datetime";

const auth = useAuthStore();
const canEdit = computed(() => auth.role === "ADMIN" || auth.role === "DOCTOR");

const list = ref([]);
const residents = ref([]);
const status = ref(""); // 默认全选

const getStatusLabel = (s) => {
  const labels = {
    'NEW': '待处理',
    'PROCESSING': '处理中',
    'DONE': '已处理'
  };
  return labels[s] || s;
};

const getStatusType = (s) => {
  const types = {
    'NEW': 'danger',
    'PROCESSING': 'warning',
    'DONE': 'success'
  };
  return types[s] || 'info';
};

const formatAlertType = (type) => {
  const map = {
    HEALTH_METRIC: "健康指标预警",
    FOLLOW_UP: "随访提醒"
  };
  return map[type] || "其他预警";
};

const formatDateTime = (dateStr) => formatFriendlyDateTime(dateStr);

const loadResidents = async () => {
  try {
    const res = await residentApi.list({});
    residents.value = res.data || [];
  } catch (e) {
    console.error("加载居民失败", e);
  }
};

const load = async () => {
  try {
    const params = {};
    if (status.value) params.status = status.value;
    const res = await alertApi.list(params);
    list.value = (res.data || []).map(alert => {
      const resi = residents.value.find(r => r.id === alert.residentId);
      return {
        ...alert,
        residentName: resi ? resi.name : `居民ID:${alert.residentId}`
      };
    });
  } catch (error) {
    console.error("加载预警列表失败:", error);
    ElMessage.error(error.message || "获取预警列表失败");
  }
};

const setStatus = async (row, targetStatus) => {
  try {
    await alertApi.updateStatus(row.id, targetStatus);
    ElMessage.success("状态已更新");
    load();
  } catch (error) {
    ElMessage.error(error.message);
  }
};

onMounted(async () => {
  await loadResidents();
  load();
});
</script>

<style scoped>
.alerts-view {
  max-width: 1200px;
  margin: 0 auto;
}
.mb-20 {
  margin-bottom: 20px;
}
</style>
