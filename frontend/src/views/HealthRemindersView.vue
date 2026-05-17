<template>
  <div class="health-reminders-container animate-fade-in" v-loading="loading">
    <el-alert
      title="健康提醒说明"
      type="warning"
      description="在此您可以查看医生为您制定的随访计划以及系统生成的异常预警。请务必关注高风险预警并按时参加随访。"
      show-icon
      class="mb-20"
    />

    <!-- 待处理预警 -->
    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Bell /></el-icon>
            <span class="title">异常预警提醒</span>
            <el-button link class="help-btn" @click="showAlertGuide = true">
              <el-icon><QuestionFilled /></el-icon>
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="newAlerts" border stripe style="width: 100%">
        <el-table-column label="预警类型" width="140">
          <template #default="{ row }">
            {{ formatAlertType(row.alertType) }}
          </template>
        </el-table-column>
        <el-table-column label="内容">
          <template #default="{ row }">{{ row.message || '无' }}</template>
        </el-table-column>
        <el-table-column label="级别" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getLevelTagType(row.level)">
              {{ formatAlertLevel(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="产生时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无未处理的异常预警" :image-size="100" />
        </template>
      </el-table>
    </el-card>

    <!-- 随访提醒 -->
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Calendar /></el-icon>
            <span class="title">随访计划提醒</span>
          </div>
        </div>
      </template>
      <el-table :data="activePlans" border stripe style="width: 100%">
        <el-table-column label="慢病类型" width="150">
          <template #default="{ row }">{{ row.diseaseType || '无' }}</template>
        </el-table-column>
        <el-table-column label="对应医生" width="130">
          <template #default="{ row }">
            {{ getPlanDoctorName(row) }}
          </template>
        </el-table-column>
        <el-table-column label="随访要求" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || '无' }}</template>
        </el-table-column>
        <el-table-column prop="nextFollowUpDate" label="预计下次随访" width="150" align="center">
          <template #default="{ row }">
            <span :class="{'text-warning': isNear(row.nextFollowUpDate)}">{{ formatFriendlyDate(row.nextFollowUpDate) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="periodDays" label="周期(天)" width="100" align="center" />
        <template #empty>
          <el-empty description="暂无进行中的随访计划" :image-size="100" />
        </template>
      </el-table>
    </el-card>

    <el-card class="page-card mt-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Calendar /></el-icon>
            <span class="title">随访记录</span>
          </div>
        </div>
      </template>
      <el-table :data="followUpRecords" border stripe style="width: 100%">
        <el-table-column label="随访时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.followUpTime) }}
          </template>
        </el-table-column>
        <el-table-column label="对应医生" width="140">
          <template #default="{ row }">{{ row.doctorName || '无' }}</template>
        </el-table-column>
        <el-table-column label="症状表现" show-overflow-tooltip>
          <template #default="{ row }">{{ row.symptoms || '无' }}</template>
        </el-table-column>
        <el-table-column label="健康指导" show-overflow-tooltip>
          <template #default="{ row }">{{ row.guidance || '无' }}</template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无随访记录" :image-size="100" />
        </template>
      </el-table>
    </el-card>

    <el-dialog v-model="showAlertGuide" title="预警说明" width="680px" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        description="预警用于提示健康风险，请结合医生指导及时处理。"
        class="mb-12"
      />
      <el-divider content-position="left">预警级别说明</el-divider>
      <el-table :data="levelGuide" border size="small">
        <el-table-column prop="name" label="级别" width="120">
          <template #default="{ row }">
            <el-tag :type="row.tagType">{{ row.name }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="desc" label="含义" />
      </el-table>
      <el-divider content-position="left">预警类型说明</el-divider>
      <el-table :data="typeGuide" border size="small">
        <el-table-column prop="name" label="类型" width="140" />
        <el-table-column prop="desc" label="含义" />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="showAlertGuide = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Bell, Calendar, QuestionFilled } from "@element-plus/icons-vue";
import { residentApi, alertApi, followUpApi } from "../api/modules";
import { formatFriendlyDate, formatFriendlyDateTime } from "../utils/datetime";

const loading = ref(false);
const newAlerts = ref([]);
const activePlans = ref([]);
const followUpRecords = ref([]);
const showAlertGuide = ref(false);

const levelGuide = [
  { name: "高风险", tagType: "danger", desc: "提示近期存在明显健康风险，需要优先处理或尽快联系医生。" },
  { name: "中风险", tagType: "warning", desc: "提示存在一定风险，建议按计划复测并关注相关指标变化。" },
  { name: "低风险", tagType: "info", desc: "提示轻度异常或提醒信息，保持观察并遵循健康建议。" }
];

const typeGuide = [
  { name: "健康指标预警", desc: "来源于血压、血糖、心率等检测指标异常。" },
  { name: "随访提醒", desc: "来源于随访计划临近或需按时执行随访任务。" },
  { name: "其他预警", desc: "系统生成的其他健康相关提醒信息。" }
];

const formatAlertLevel = (level) => {
  const map = { HIGH: "高风险", MEDIUM: "中风险", LOW: "低风险" };
  return map[level] || level || "未知";
};

const getLevelTagType = (level) => {
  const map = { HIGH: "danger", MEDIUM: "warning", LOW: "info" };
  return map[level] || "info";
};

const formatAlertType = (type) => {
  const map = {
    HEALTH_METRIC: "健康指标预警",
    FOLLOW_UP: "随访提醒"
  };
  return map[type] || "其他预警";
};

const formatDateTime = (dateStr) => formatFriendlyDateTime(dateStr);

const isNear = (dateStr) => {
  if (!dateStr) return false;
  const target = new Date(dateStr);
  const now = new Date();
  const diff = target.getTime() - now.getTime();
  return diff > 0 && diff < 3 * 24 * 3600 * 1000; // 3天内
};

const getPlanDoctorName = (plan) => {
  const records = (followUpRecords.value || []).filter((r) => r.planId === plan.id && r.doctorName);
  if (records.length === 0) return "待安排";
  records.sort((a, b) => new Date(b.followUpTime) - new Date(a.followUpTime));
  return records[0].doctorName || "待安排";
};

const loadData = async () => {
  loading.value = true;
  try {
    const profileRes = await residentApi.me();
    const profile = profileRes.data;
    if (!profile) return;

    const residentId = profile.id;

    const [alertRes, planRes, recordRes] = await Promise.all([
      alertApi.list({ residentId, status: 'NEW' }),
      followUpApi.plans(residentId),
      followUpApi.records({ residentId })
    ]);

    newAlerts.value = alertRes.data || [];
    activePlans.value = (planRes.data || []).filter(p => p.active);
    followUpRecords.value = recordRes.data || [];

  } catch (error) {
    console.error("加载提醒数据失败", error);
    if (error.response?.status !== 404) {
      ElMessage.error("获取健康提醒失败");
    }
  } finally {
    loading.value = false;
  }
};

onMounted(loadData);
</script>

<style scoped>
.health-reminders-container { max-width: 1200px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.mb-12 { margin-bottom: 12px; }
.mt-20 { margin-top: 20px; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; gap: 10px; }
.header-icon { font-size: 20px; color: #1aa094; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }
.help-btn { color: #64748b; padding: 0; }
.help-btn:hover { color: #1aa094; }
.text-warning { color: #f59e0b; font-weight: bold; }

.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>