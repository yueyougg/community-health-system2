<!--系统数据库管理-->
<template>
  <div class="backup-management animate-fade-in" v-loading="loading">
    <el-alert
      title="数据备份与恢复说明"
      type="warning"
      description="建议定期进行全量数据库备份。恢复操作将覆盖当前系统所有数据，请务必谨慎操作。"
      show-icon
      class="mb-20"
    />

    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Management /></el-icon>
            <span class="title">备份记录</span>
          </div>
          <div class="header-right">
            <div class="countdown-display" v-if="nextBackup">
              <el-icon class="countdown-icon"><Clock /></el-icon>
              <span class="countdown-text">
                距离下次自动备份: 
                <span class="countdown-time">{{ countdownText }}</span>
              </span>
            </div>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon> 立即执行备份
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" border stripe style="width: 100%">
        <el-table-column prop="fileName" label="备份文件名" show-overflow-tooltip />
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ (row.fileSize / 1024 / 1024).toFixed(2) }} MB
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="backupTime" label="备份时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.backupTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button type="warning" link @click="handleRestore(row)">恢复</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无备份记录" />
        </template>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, computed } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Management, Plus, Clock } from "@element-plus/icons-vue";
import { systemApi } from "../api/modules";
import { formatFriendlyDateTime } from "../utils/datetime";

const loading = ref(false);
const list = ref([]);
const nextBackup = ref(null);
const countdown = ref({ days: 0, hours: 0, minutes: 0 });
let countdownTimer = null;

const formatDateTime = (dateStr) => formatFriendlyDateTime(dateStr);

const countdownText = computed(() => {
  const { days, hours, minutes } = countdown.value;
  if (days > 0) {
    return `${days}天 ${hours}小时 ${minutes}分钟`;
  } else if (hours > 0) {
    return `${hours}小时 ${minutes}分钟`;
  } else {
    return `${minutes}分钟`;
  }
});

const loadNextBackup = async () => {
  try {
    const res = await systemApi.nextBackup();
    nextBackup.value = res.data;
    updateCountdown();
  } catch (e) {
    console.error("获取下次备份时间失败", e);
  }
};

const updateCountdown = () => {
  if (!nextBackup.value) return;
  
  countdown.value = {
    days: nextBackup.value.daysRemaining || 0,
    hours: nextBackup.value.hoursRemaining || 0,
    minutes: nextBackup.value.minutesRemaining || 0
  };
};

const startCountdown = () => {
  countdownTimer = setInterval(() => {
    if (countdown.value.minutes > 0) {
      countdown.value.minutes--;
    } else if (countdown.value.hours > 0) {
      countdown.value.hours--;
      countdown.value.minutes = 59;
    } else if (countdown.value.days > 0) {
      countdown.value.days--;
      countdown.value.hours = 23;
      countdown.value.minutes = 59;
    } else {
      loadNextBackup();
    }
  }, 60000);
};

const loadData = async () => {
  loading.value = true;
  try {
    const res = await systemApi.backups();
    list.value = res.data || [];
  } catch (e) {
    ElMessage.error("获取备份记录失败");
  } finally {
    loading.value = false;
  }
};

const handleCreate = async () => {
  try {
    await ElMessageBox.confirm('确定要执行数据备份吗？', '提示', { type: 'info' });
    
    loading.value = true;
    await systemApi.createBackup();
    ElMessage.success("备份成功");
    loadData();
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error("备份失败");
    }
  } finally {
    loading.value = false;
  }
};

const handleRestore = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要将系统恢复到 ${formatDateTime(row.backupTime)} 的状态吗？此操作不可逆！`,
      '严正警告',
      { type: 'error', confirmButtonText: '确认恢复', cancelButtonText: '取消' }
    );
    
    loading.value = true;
    await systemApi.restoreBackup(row.filePath);
    ElMessage.success("数据恢复成功");
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error("恢复失败");
    }
  } finally {
    loading.value = false;
  }
};

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要永久删除此备份文件吗？', '提示', { type: 'warning' });
    await systemApi.deleteBackup(row.filePath);
    ElMessage.success("已删除");
    loadData();
  } catch (e) {}
};

onMounted(() => {
  loadData();
  loadNextBackup();
  startCountdown();
});

onUnmounted(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer);
  }
});
</script>

<style scoped>
.backup-management { max-width: 1200px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; gap: 10px; }
.header-icon { font-size: 20px; color: #1aa094; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }
.header-right { display: flex; align-items: center; gap: 20px; }
.countdown-display { display: flex; align-items: center; gap: 8px; padding: 8px 16px; background: #f0f9ff; border-radius: 8px; border: 1px solid #bae6fd; }
.countdown-icon { font-size: 18px; color: #0284c7; }
.countdown-text { font-size: 14px; color: #0369a1; }
.countdown-time { font-weight: 600; color: #0284c7; }
.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
