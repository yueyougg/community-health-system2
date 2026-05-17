<template>
  <div class="health-assessment animate-fade-in" v-loading="loading">
    <el-alert
      title="健康评估与指导"
      type="success"
      description="在此您可以对居民的健康状况进行综合评估，并提供针对性的健康指导建议。评估结果将实时同步至居民端。"
      show-icon
      class="mb-20"
    />

    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Monitor /></el-icon>
            <span class="title">健康评估管理</span>
          </div>
          <div class="header-right">
            <el-select v-model="selectedResidentId" placeholder="选择居民" filterable style="width: 220px; margin-right: 12px" @change="load">
              <el-option v-for="item in residents" :key="item.id" :label="`${item.name} (${item.archiveNo})`" :value="item.id" />
            </el-select>
            <el-button type="primary" :disabled="!selectedResidentId" @click="openDialog">
              <el-icon><Plus /></el-icon> 新增评估
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="assessments" border stripe style="width: 100%">
        <el-table-column prop="assessmentDate" label="评估时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.assessmentDate) }}
          </template>
        </el-table-column>
        <el-table-column label="健康评分" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getScoreTag(row.healthScore)">{{ row.healthScore }}分</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="healthLevel" label="健康等级" width="100" align="center">
          <template #default="{ row }">
            {{ formatLevel(row.healthLevel) }}
          </template>
        </el-table-column>
        <el-table-column prop="diseaseType" label="常见病症" width="140" />
        <el-table-column prop="evaluation" label="评估结论" show-overflow-tooltip />
        <el-table-column prop="guidance" label="指导建议" show-overflow-tooltip />
        <el-table-column prop="doctorName" label="评估人" width="120" />
      </el-table>
    </el-card>

    <!-- 评估对话框 -->
    <el-dialog v-model="showDialog" title="居民健康评估与指导" width="650px" destroy-on-close class="custom-dialog">
      <el-form :model="form" label-position="top" class="p-20">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="健康评分 (0-100)">
              <el-input-number v-model="form.healthScore" :min="0" :max="100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="健康等级">
              <el-select v-model="form.healthLevel" style="width: 100%">
                <el-option label="优" value="EXCELLENT" />
                <el-option label="良" value="GOOD" />
                <el-option label="一般" value="FAIR" />
                <el-option label="较差" value="POOR" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="常见病症">
          <el-select v-model="form.diseaseType" style="width: 100%" placeholder="请选择病症类型">
            <el-option v-for="item in diseaseOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="综合评估结论">
          <el-input v-model="form.evaluation" type="textarea" :rows="3" placeholder="请对居民的整体健康状况进行评价..." />
        </el-form-item>
        <el-form-item label="健康指导建议">
          <el-input v-model="form.guidance" type="textarea" :rows="4" placeholder="请输入针对性的饮食、运动、用药等指导建议..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="onSubmit">保存评估</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Monitor, Plus } from "@element-plus/icons-vue";
import { residentApi, assessmentApi } from "../api/modules";
import { useAuthStore } from "../store/auth";
import { formatFriendlyDateTime } from "../utils/datetime";

const auth = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const residents = ref([]);
const selectedResidentId = ref();
const assessments = ref([]);
const showDialog = ref(false);

const form = ref({
  residentId: null,
  doctorId: auth.userId,
  doctorName: auth.username,
  healthScore: 80,
  healthLevel: "GOOD",
  diseaseType: "高血压",
  evaluation: "",
  guidance: ""
});
const diseaseOptions = ["高血压", "糖尿病", "冠心病", "慢性支气管炎", "哮喘", "其他"];

const formatDateTime = (dateStr) => formatFriendlyDateTime(dateStr);

const getScoreTag = (score) => {
  if (score >= 90) return "success";
  if (score >= 70) return "";
  if (score >= 60) return "warning";
  return "danger";
};

const formatLevel = (level) => {
  const map = { EXCELLENT: "优", GOOD: "良", FAIR: "一般", POOR: "较差" };
  return map[level] || level;
};

const loadResidents = async () => {
  try {
    const res = await residentApi.list({}); // 确保传入空对象获取全部
    residents.value = res.data || [];
  } catch (e) { console.error(e); }
};

const load = async () => {
  if (!selectedResidentId.value) return;
  loading.value = true;
  try {
    const res = await assessmentApi.list(selectedResidentId.value);
    assessments.value = res.data || [];
  } catch (e) {
    ElMessage.error("加载评估列表失败");
  } finally {
    loading.value = false;
  }
};

const openDialog = () => {
  form.value.residentId = selectedResidentId.value;
  showDialog.value = true;
};

const onSubmit = async () => {
  if (!form.value.evaluation || !form.value.guidance) {
    return ElMessage.warning("请填写完整的评估结论和指导建议");
  }
  submitting.value = true;
  try {
    await assessmentApi.create(form.value);
    ElMessage.success("健康评估已保存");
    showDialog.value = false;
    form.value.evaluation = "";
    form.value.guidance = "";
    load();
  } catch (e) {
    console.error(e);
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadResidents();
});
</script>

<style scoped>
.health-assessment { max-width: 1200px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.p-20 { padding: 20px; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
.page-card:hover { transform: none !important; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; gap: 10px; }
.header-icon { font-size: 20px; color: #1aa094; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }

.dialog-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 0 20px 20px; }
.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
