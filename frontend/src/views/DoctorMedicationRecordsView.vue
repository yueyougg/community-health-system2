<template>
  <div class="page-container animate-fade-in" v-loading="loading">
    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><Management /></el-icon>
            <span class="title">用药记录管理</span>
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
        <el-table-column prop="drugName" label="药品名称" width="140" />
        <el-table-column label="用药周期" width="220">
          <template #default="{ row }">{{ formatFriendlyDate(row.startDate) }} 至 {{ formatFriendlyDate(row.endDate) }}</template>
        </el-table-column>
        <el-table-column prop="dosage" label="剂量" width="120" />
        <el-table-column prop="usageMethod" label="用法" width="120" />
        <el-table-column prop="reason" label="用药原因" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-dialog v-model="showDialog" title="新增用药记录" width="620px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <el-form-item label="居民">
          <el-select v-model="form.residentId" filterable style="width:100%">
            <el-option v-for="item in residents" :key="item.id" :label="`${item.name} (${item.archiveNo})`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="药品名称"><el-input v-model="form.drugName" /></el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="剂量"><el-input v-model="form.dosage" /></el-form-item>
        <el-form-item label="用法"><el-input v-model="form.usageMethod" /></el-form-item>
        <el-form-item label="用药原因"><el-input v-model="form.reason" type="textarea" :rows="2" /></el-form-item>
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
import { Management, Plus } from "@element-plus/icons-vue";
import { medicationRecordApi, residentApi } from "../api/modules";
import { formatFriendlyDate } from "../utils/datetime";

const loading = ref(false);
const submitting = ref(false);
const residents = ref([]);
const selectedResidentId = ref();
const list = ref([]);
const showDialog = ref(false);
const form = ref({
  residentId: null,
  drugName: "",
  startDate: "",
  endDate: "",
  dosage: "",
  usageMethod: "",
  reason: ""
});

const loadResidents = async () => {
  const res = await residentApi.list({});
  residents.value = res.data || [];
};

const load = async () => {
  loading.value = true;
  try {
    const res = await medicationRecordApi.list(selectedResidentId.value);
    list.value = (res.data || []).map((m) => {
      const r = residents.value.find((i) => Number(i.id) === Number(m.residentId));
      return { ...m, residentName: r?.name || "未知居民" };
    });
  } finally {
    loading.value = false;
  }
};

const openDialog = () => {
  form.value = {
    residentId: selectedResidentId.value || null,
    drugName: "",
    startDate: "",
    endDate: "",
    dosage: "",
    usageMethod: "",
    reason: ""
  };
  showDialog.value = true;
};

const save = async () => {
  if (!form.value.residentId) return ElMessage.warning("请选择居民");
  if (!form.value.drugName) return ElMessage.warning("请填写药品名称");
  submitting.value = true;
  try {
    await medicationRecordApi.create(form.value);
    ElMessage.success("用药记录已保存");
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
