<template>
  <div class="page-container animate-fade-in" v-loading="loading">
    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><List /></el-icon>
            <span class="title">就诊记录管理</span>
          </div>
          <div class="header-right">
            <el-select v-model="selectedResidentId" clearable filterable placeholder="选择居民查看" style="width: 220px" @change="load">
              <el-option v-for="item in residents" :key="item.id" :label="`${item.name} (${item.archiveNo})`" :value="item.id" />
            </el-select>
            <el-button type="primary" @click="openDialog">
              <el-icon><Plus /></el-icon> 新增记录
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="list" border stripe>
        <el-table-column prop="residentName" label="居民姓名" width="140" />
        <el-table-column label="就诊时间" width="170">
          <template #default="{ row }">{{ formatFriendlyDateTime(row.visitTime) }}</template>
        </el-table-column>
        <el-table-column prop="organization" label="就诊机构" width="160" />
        <el-table-column prop="diagnosis" label="诊断结果" />
        <el-table-column prop="prescription" label="处方" show-overflow-tooltip />
        <el-table-column prop="examReport" label="检查报告" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" title="新增就诊记录" width="620px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <el-form-item label="居民">
          <el-select v-model="form.residentId" filterable style="width:100%">
            <el-option v-for="item in residents" :key="item.id" :label="`${item.name} (${item.archiveNo})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="就诊时间">
          <el-date-picker v-model="form.visitTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="就诊机构"><el-input v-model="form.organization" /></el-form-item>
        <el-form-item label="诊断结果"><el-input v-model="form.diagnosis" /></el-form-item>
        <el-form-item label="处方"><el-input v-model="form.prescription" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="检查报告"><el-input v-model="form.examReport" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { List, Plus } from "@element-plus/icons-vue";
import { residentApi, visitRecordApi } from "../api/modules";
import { formatFriendlyDateTime } from "../utils/datetime";

const loading = ref(false);
const submitting = ref(false);
const residents = ref([]);
const selectedResidentId = ref();
const list = ref([]);
const showDialog = ref(false);
const form = ref({
  residentId: null,
  visitTime: new Date().toISOString().slice(0, 19),
  organization: "社区卫生服务中心",
  diagnosis: "",
  prescription: "",
  examReport: ""
});

const loadResidents = async () => {
  const res = await residentApi.list({});
  residents.value = res.data || [];
};

const load = async () => {
  loading.value = true;
  try {
    const res = await visitRecordApi.list(selectedResidentId.value);
    list.value = (res.data || []).map((v) => {
      const r = residents.value.find((i) => Number(i.id) === Number(v.residentId));
      return { ...v, residentName: r?.name || "未知居民" };
    });
  } finally {
    loading.value = false;
  }
};

const openDialog = () => {
  form.value = {
    residentId: selectedResidentId.value || null,
    visitTime: new Date().toISOString().slice(0, 19),
    organization: "社区卫生服务中心",
    diagnosis: "",
    prescription: "",
    examReport: ""
  };
  showDialog.value = true;
};

const save = async () => {
  if (!form.value.residentId) return ElMessage.warning("请选择居民");
  submitting.value = true;
  try {
    await visitRecordApi.create(form.value);
    ElMessage.success("就诊记录已保存");
    showDialog.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
};

onMounted(async () => {
  await loadResidents();
  await load();
});
</script>

<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0,0,0,.05); }
.page-card:hover { transform: none !important; }
.mb-20 { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; gap: 10px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.header-icon { color: #1aa094; font-size: 20px; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }
.animate-fade-in { animation: fadeIn .4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px);} to { opacity: 1; transform: translateY(0);} }
</style>
