<template>
  <div class="follow-up-plan-container animate-fade-in" v-loading="loading">
    <el-alert
      title="随访计划提示"
      type="info"
      description="为慢病患者制定长期的随访计划，系统会自动生成下次随访提醒，并在预警中心提示医生。"
      show-icon
      class="mb-20"
    />
    
    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Calendar /></el-icon>
            <span class="title">随访计划管理</span>
          </div>
          <div class="header-right">
            <el-input v-model="filter.residentName" placeholder="居民姓名" style="width: 150px; margin-right: 12px" clearable @input="load" @keyup.enter="load" />
            <el-select v-model="filter.diseaseType" placeholder="病症类型" clearable style="width: 150px; margin-right: 12px" @change="load">
              <el-option v-for="item in diseaseTypes" :key="item" :label="item" :value="item" />
            </el-select>
            <el-date-picker v-model="filter.date" type="date" value-format="YYYY-MM-DD" placeholder="随访日期" clearable style="width: 150px; margin-right: 12px" @change="load" />
            <el-button type="primary" @click="load">查询</el-button>
            <el-button v-if="canEdit" type="success" @click="openPlanDialog">
              <el-icon><Plus /></el-icon> 新增计划
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="plans" border stripe style="width: 100%">
        <el-table-column prop="id" label="计划编号" width="90" align="center" />
        <el-table-column prop="residentName" label="居民姓名" width="120" />
        <el-table-column prop="diseaseType" label="随访类型" width="150">
          <template #default="{ row }">
            <el-tag :type="row.diseaseType === '常规随访' ? 'info' : 'warning'">{{ row.diseaseType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="periodDays" label="周期(天)" width="100" align="center" />
        <el-table-column label="下次随访日期" width="140" align="center">
          <template #default="{ row }">
            {{ formatFriendlyDate(row.nextFollowUpDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="content" label="计划内容" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" effect="light">
              {{ row.active ? "进行中" : "已完成" }}
            </el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无随访计划" :image-size="100" />
        </template>
      </el-table>
    </el-card>

    <!-- 弹窗部分 -->
    <el-dialog v-model="planDialog" title="制定随访计划" width="550px" destroy-on-close class="custom-dialog">
      <el-form :model="planForm" label-position="top" class="p-20" ref="formRef" :rules="rules">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="居民对象" prop="residentId">
              <el-select v-model="planForm.residentId" style="width: 100%" filterable placeholder="请选择居民">
                <el-option v-for="item in residents" :key="item.id" :label="`${item.name} (${item.archiveNo})`" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="随访类型" prop="diseaseType">
              <el-select v-model="planForm.diseaseType" style="width: 100%" placeholder="选择病症或类型">
                <el-option v-for="item in diseaseTypes" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="随访周期 (天)" prop="periodDays">
              <el-input-number v-model="planForm.periodDays" :min="1" :max="365" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="首次随访日期" prop="nextFollowUpDate">
              <el-date-picker v-model="planForm.nextFollowUpDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="计划详情" prop="content">
          <el-input v-model="planForm.content" type="textarea" :rows="3" placeholder="例如：检查血压波动情况、询问用药依从性、进行生活方式指导等..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="planDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="savePlan">保存计划</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, reactive } from "vue";
import { ElMessage } from "element-plus";
import { Calendar, Plus } from "@element-plus/icons-vue";
import { followUpApi, residentApi } from "../api/modules";
import { useAuthStore } from "../store/auth";
import { formatFriendlyDate } from "../utils/datetime";

const auth = useAuthStore();
const canEdit = computed(() => auth.role === "ADMIN" || auth.role === "DOCTOR");

const loading = ref(false);
const submitting = ref(false);
const residents = ref([]);
const plans = ref([]);
const planDialog = ref(false);
const formRef = ref();

const filter = reactive({
  residentName: "",
  diseaseType: "",
  date: ""
});

const diseaseTypes = [
  "高血压",
  "糖尿病",
  "冠心病",
  "慢性支气管炎",
  "哮喘",
  "常规随访",
  "其他病症"
];

const rules = {
  residentId: [{ required: true, message: "请选择居民", trigger: "change" }],
  diseaseType: [{ required: true, message: "请选择随访类型", trigger: "change" }],
  periodDays: [{ required: true, message: "请输入周期", trigger: "blur" }],
  nextFollowUpDate: [{ required: true, message: "请选择日期", trigger: "change" }],
  content: [{ required: true, message: "请输入计划详情", trigger: "blur" }]
};

const defaultForm = () => ({
  residentId: null,
  diseaseType: "",
  periodDays: 30,
  nextFollowUpDate: "",
  content: "",
  active: true
});

const planForm = ref(defaultForm());

const openPlanDialog = () => {
  planForm.value = defaultForm();
  planDialog.value = true;
};

const loadResidents = async () => {
  try {
    const res = await residentApi.list({});
    residents.value = res.data || [];
  } catch (e) {
    console.error("加载居民失败", e);
  }
};

const load = async () => {
  loading.value = true;
  try {
    const params = {
      residentName: (filter.residentName || "").trim(),
      diseaseType: (filter.diseaseType || "").trim(),
      date: filter.date || undefined
    };
    if (!params.residentName) delete params.residentName;
    if (!params.diseaseType) delete params.diseaseType;
    const res = await followUpApi.plans(params);
    plans.value = (res.data || []).map(p => {
      const r = residents.value.find(res => Number(res.id) === Number(p.residentId));
      return { 
        ...p, 
        residentName: r ? r.name : `未知居民`,
        archiveNo: r ? r.archiveNo : `-`
      };
    });
  } catch (error) {
    console.error("加载随访计划失败", error);
    ElMessage.error("获取随访计划失败");
  } finally {
    loading.value = false;
  }
};

const savePlan = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      await followUpApi.createPlan(planForm.value);
      ElMessage.success("随访计划已创建");
      planDialog.value = false;
      load();
    } catch (error) {
      // 错误已由拦截器处理
    } finally {
      submitting.value = false;
    }
  });
};

onMounted(async () => {
  await loadResidents();
  await load();
});
</script>

<style scoped>
.follow-up-plan-container {
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
