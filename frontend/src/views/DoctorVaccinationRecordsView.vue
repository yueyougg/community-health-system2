<template>
  <div class="page-container animate-fade-in" v-loading="loading">
    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Check /></el-icon>
            <span class="title">疫苗接种管理</span>
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
        <el-table-column prop="vaccineName" label="疫苗名称" width="180" />
        <el-table-column label="接种日期" width="140">
          <template #default="{ row }">{{ formatFriendlyDate(row.vaccinatedAt) }}</template>
        </el-table-column>
        <el-table-column prop="institution" label="接种机构" />
        <el-table-column prop="batchNo" label="批号" width="180" />
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" title="新增疫苗接种记录" width="620px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <el-form-item label="居民">
          <el-select v-model="form.residentId" filterable style="width:100%">
            <el-option v-for="item in residents" :key="item.id" :label="`${item.name} (${item.archiveNo})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="疫苗名称"><el-input v-model="form.vaccineName" /></el-form-item>
        <el-form-item label="接种日期"><el-date-picker v-model="form.vaccinatedAt" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="接种机构"><el-input v-model="form.institution" /></el-form-item>
        <el-form-item label="批号"><el-input v-model="form.batchNo" /></el-form-item>
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
import { Check, Plus } from "@element-plus/icons-vue";
import { residentApi, vaccinationRecordApi } from "../api/modules";
import { formatFriendlyDate } from "../utils/datetime";

const loading = ref(false);
const submitting = ref(false);
const residents = ref([]);
const selectedResidentId = ref();
const list = ref([]);
const showDialog = ref(false);
const form = ref({
  residentId: null,
  vaccineName: "",
  vaccinatedAt: "",
  institution: "",
  batchNo: ""
});

const loadResidents = async () => {
  const res = await residentApi.list({});
  residents.value = res.data || [];
};

const load = async () => {
  loading.value = true;
  try {
    const res = await vaccinationRecordApi.list(selectedResidentId.value);
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
    vaccineName: "",
    vaccinatedAt: "",
    institution: "",
    batchNo: ""
  };
  showDialog.value = true;
};

const save = async () => {
  if (!form.value.residentId) return ElMessage.warning("请选择居民");
  if (!form.value.vaccineName) return ElMessage.warning("请填写疫苗名称");
  submitting.value = true;
  try {
    await vaccinationRecordApi.create(form.value);
    ElMessage.success("接种记录已保存");
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
