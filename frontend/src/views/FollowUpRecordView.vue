<template>
  <div class="follow-up-record-container animate-fade-in" v-loading="loading">
    <el-alert
      title="随访记录提示"
      type="success"
      description="记录每次随访的详细情况，包括症状表现和医生的健康指导建议，以便长期追踪居民健康状况。"
      show-icon
      class="mb-20"
    />

    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Document /></el-icon>
            <span class="title">随访记录列表</span>
          </div>
          <div class="header-right">
            <el-select v-model="filter.planId" placeholder="按随访计划筛选" clearable style="width: 200px; margin-right: 12px" @change="load">
              <el-option v-for="item in plans" :key="item.id" :label="`计划#${item.id} - ${item.diseaseType}`" :value="item.id" />
            </el-select>
            <el-date-picker
              v-model="filter.timeRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 250px; margin-right: 12px"
              @change="load"
            />
            <el-button type="primary" @click="load">查询</el-button>
            <el-button v-if="canEdit" type="success" @click="openRecordDialog">
              <el-icon><Check /></el-icon> 录入随访
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="records" border stripe style="width: 100%">
        <el-table-column prop="planId" label="关联计划" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small">计划#{{ row.planId }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="residentName" label="居民姓名" width="120" />
        <el-table-column prop="archiveNo" label="档案编号" width="180" />
        <el-table-column label="随访时间" width="180" align="center">
          <template #default="{ row }">
            {{ formatDateTime(row.followUpTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="doctorName" label="随访医生" width="120" align="center" />
        <el-table-column prop="symptoms" label="症状表现" show-overflow-tooltip />
        <el-table-column prop="guidance" label="健康指导" show-overflow-tooltip />
        <el-table-column label="下次提醒" width="130" align="center">
          <template #default="{ row }">
            {{ formatFriendlyDate(row.nextReminderDate) }}
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无随访记录" :image-size="100" />
        </template>
      </el-table>
    </el-card>

    <!-- 录入弹窗 -->
    <el-dialog v-model="recordDialog" title="录入随访记录" width="600px" destroy-on-close class="custom-dialog">
      <el-form :model="recordForm" label-position="top" class="p-20">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="随访计划" required>
              <el-select v-model="recordForm.planId" style="width: 100%" placeholder="关联计划" @change="onPlanChange">
                <el-option v-for="item in plansWithResidentName" :key="item.id" :label="`计划#${item.id} (${item.residentName}) - ${item.diseaseType}`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="随访时间">
              <el-date-picker v-model="recordForm.followUpTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="症状描述">
          <el-input v-model="recordForm.symptoms" placeholder="描述居民当前症状..." />
        </el-form-item>
        <el-form-item label="健康指导建议">
          <el-input v-model="recordForm.guidance" type="textarea" :rows="2" placeholder="给居民的健康生活建议..." />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="随访医生">
              <el-input v-model="recordForm.doctorName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="下次提醒日期">
              <el-date-picker v-model="recordForm.nextReminderDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="recordDialog = false">取消</el-button>
          <el-button type="success" :loading="submitting" @click="saveRecord">确认录入</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, reactive } from "vue";
import { ElMessage } from "element-plus";
import { Document, Check } from "@element-plus/icons-vue";
import { followUpApi, residentApi } from "../api/modules";
import { useAuthStore } from "../store/auth";
import { formatFriendlyDate, formatFriendlyDateTime } from "../utils/datetime";

const auth = useAuthStore();
const canEdit = computed(() => auth.role === "ADMIN" || auth.role === "DOCTOR");

const loading = ref(false);
const submitting = ref(false);
const residents = ref([]);
const plans = ref([]);
const records = ref([]);
const recordDialog = ref(false);

const plansWithResidentName = computed(() => {
  return plans.value.map(p => {
    const res = residents.value.find(r => r.id === p.residentId);
    return { ...p, residentName: res ? res.name : `居民ID:${p.residentId}` };
  });
});

const filter = reactive({
  planId: null,
  timeRange: []
});

const formatDateTime = (dateStr) => formatFriendlyDateTime(dateStr);

const defaultRecordForm = () => ({
  planId: null,
  residentId: null,
  followUpTime: new Date().toISOString().slice(0, 19),
  doctorName: auth.username,
  symptoms: "",
  physicalSigns: "",
  guidance: "",
  medicationAdjustment: "",
  nextReminderDate: ""
});

const recordForm = ref(defaultRecordForm());

const loadResidents = async () => {
  try {
    const res = await residentApi.list({});
    residents.value = res.data || [];
  } catch (e) {
    console.error("加载居民失败", e);
  }
};

const loadPlans = async () => {
  try {
    const res = await followUpApi.plans({});
    plans.value = res.data || [];
  } catch (e) {
    console.error("加载随访计划失败", e);
  }
};

const load = async () => {
  loading.value = true;
  try {
    const params = {
      planId: filter.planId,
      startTime: filter.timeRange?.[0],
      endTime: filter.timeRange?.[1]
    };
    const res = await followUpApi.records(params);
    records.value = (res.data || []).map(rec => {
      const resi = residents.value.find(r => r.id === rec.residentId);
      return { 
        ...rec, 
        residentName: resi ? resi.name : `未知居民`,
        archiveNo: resi ? resi.archiveNo : `-`
      };
    });
  } catch (error) {
    console.error("加载随访记录失败", error);
    ElMessage.error("获取随访记录失败");
  } finally {
    loading.value = false;
  }
};

const openRecordDialog = async () => {
  await loadPlans();
  if (plans.value.length === 0) {
    return ElMessage.warning("请先为居民制定随访计划，才能录入随访记录。");
  }
  recordForm.value = defaultRecordForm();
  recordDialog.value = true;
};

const onPlanChange = (planId) => {
  const plan = plans.value.find(p => p.id === planId);
  if (plan) {
    recordForm.value.residentId = plan.residentId;
  }
};

const saveRecord = async () => {
  if (!recordForm.value.planId) return ElMessage.warning("请选择关联的随访计划");
  submitting.value = true;
  try {
    await followUpApi.createRecord(recordForm.value);
    ElMessage.success("随访记录已保存");
    recordDialog.value = false;
    load();
  } catch (error) {
    // 错误处理
  } finally {
    submitting.value = false;
  }
};

onMounted(async () => {
  await loadResidents();
  await loadPlans();
  load();
});
</script>

<style scoped>
.follow-up-record-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.page-card:hover {
  transform: none !important;
}

.mb-20 { margin-bottom: 20px; }
.p-20 { padding: 20px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-icon {
  font-size: 20px;
  color: #1aa094;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: #2f4056;
}

.animate-fade-in {
  animation: fadeIn 0.4s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table th.el-table__cell) {
  background-color: #f8fafc;
  color: #475569;
  font-weight: 600;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 20px 20px;
}

.custom-dialog :deep(.el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  margin-right: 0;
  padding: 20px 24px;
}

.custom-dialog :deep(.el-dialog__title) {
  font-weight: 700;
  color: #2f4056;
}
</style>
