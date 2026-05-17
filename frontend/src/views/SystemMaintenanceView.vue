<template>
  <div class="system-maintenance">
    <h2>系统维护</h2>
    
    <el-tabs v-model="activeTab">
      <!-- 审计日志 -->
      <el-tab-pane label="审计日志" name="logs">
        <el-table :data="logs" v-loading="loadingLogs" height="500" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="操作用户" width="120" />
          <el-table-column prop="moduleName" label="模块" width="120" />
          <el-table-column prop="actionName" label="操作" width="150" />
          <el-table-column prop="ipAddress" label="IP地址" width="150" />
          <el-table-column label="操作时间">
            <template #default="{ row }">
              {{ formatFriendlyDateTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="详情" width="100">
             <template #default="{ row }">
               <el-popover placement="top" :width="300" trigger="click">
                 <template #reference>
                   <base-button size="small" type="primary">查看</base-button>
                 </template>
                 <div>
                   <p><b>请求路径:</b> {{ row.requestPath }}</p>
                   <p><b>请求方法:</b> {{ row.httpMethod }}</p>
                 </div>
               </el-popover>
             </template>
          </el-table-column>
        </el-table>
        <div class="refresh-bar">
          <base-button @click="fetchLogs">刷新日志</base-button>
        </div>
      </el-tab-pane>

      <!-- 数据备份 -->
      <el-tab-pane label="数据备份" name="backup">
        <div class="backup-panel">
          <el-alert
            title="数据备份与恢复"
            type="info"
            description="请定期进行数据备份。恢复数据会覆盖当前数据库，请谨慎操作。"
            show-icon
            :closable="false"
          />
          
          <div class="backup-actions">
            <el-card class="action-card">
              <template #header>
                <div class="card-header">
                  <span>立即备份</span>
                </div>
              </template>
              <p>生成当前数据库的完整快照。</p>
              <base-button type="primary" :disabled="backingUp" @click="doBackup">
                {{ backingUp ? '备份中...' : '开始备份' }}
              </base-button>
            </el-card>

            <el-card class="action-card">
              <template #header>
                <div class="card-header">
                  <span>数据恢复</span>
                </div>
              </template>
              <p>从最新的备份文件中恢复数据。</p>
              <base-button type="danger" :disabled="restoring" @click="doRestore">
                {{ restoring ? '恢复中...' : '恢复数据' }}
              </base-button>
            </el-card>
          </div>

          <div class="manual-guide" v-if="guide">
            <h3>手动操作指南</h3>
            <p>如果系统备份失败，您可以使用以下命令手动操作：</p>
            <div class="code-block">
              <p><b>备份:</b> {{ guide.backup }}</p>
              <p><b>恢复:</b> {{ guide.restore }}</p>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { systemApi } from "../api/modules";
import { formatFriendlyDateTime } from "../utils/datetime";

const activeTab = ref("logs");
const logs = ref([]);
const loadingLogs = ref(false);
const guide = ref(null);
const backingUp = ref(false);
const restoring = ref(false);

const fetchLogs = async () => {
  loadingLogs.value = true;
  try {
    const res = await systemApi.logs();
    logs.value = res.data;
  } catch (error) {
    ElMessage.error("获取日志失败");
  } finally {
    loadingLogs.value = false;
  }
};

const fetchGuide = async () => {
  try {
    const res = await systemApi.backupGuide();
    guide.value = res.data;
  } catch (error) {
    console.error(error);
  }
};

const doBackup = async () => {
  backingUp.value = true;
  try {
    const res = await systemApi.backup();
    ElMessage.success(res.message);
  } catch (error) {
    ElMessage.error("备份请求失败");
  } finally {
    backingUp.value = false;
  }
};

const doRestore = async () => {
  try {
    await ElMessageBox.confirm("确定要恢复数据吗？此操作将覆盖当前所有数据！", "警告", {
      type: "warning",
      confirmButtonText: "确定恢复",
      cancelButtonText: "取消"
    });
    
    restoring.value = true;
    const res = await systemApi.restore();
    ElMessage.success(res.message);
  } catch (error) {
    if (error !== "cancel") ElMessage.error("恢复请求失败");
  } finally {
    restoring.value = false;
  }
};

onMounted(() => {
  fetchLogs();
  fetchGuide();
});
</script>

<style scoped>
.system-maintenance {
  padding: 20px;
  background: rgb(255,250,235);
  border-radius: 8px;
}

.refresh-bar {
  margin-top: 16px;
  text-align: right;
}

.backup-panel {
  padding: 20px 0;
}

.backup-actions {
  display: flex;
  gap: 20px;
  margin-top: 20px;
}

.action-card {
  flex: 1;
}

.manual-guide {
  margin-top: 30px;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 4px;
}

.code-block {
  background: #2d2d2d;
  color: #fff;
  padding: 15px;
  border-radius: 4px;
  font-family: monospace;
}
</style>
