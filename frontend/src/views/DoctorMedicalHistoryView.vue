<template>
  <div class="page-container animate-fade-in" v-loading="loading">
    <el-card class="page-card mb-20">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-icon class="header-icon"><FirstAidKit /></el-icon>
            <span class="title">居民健康史管理</span>
          </div>
          <div class="header-right">
            <el-select
              v-model="selectedResidentId"
              filterable
              clearable
              placeholder="请选择居民"
              style="width: 260px"
              @change="load"
            >
              <el-option
                v-for="item in residents"
                :key="item.id"
                :label="`${item.name} (${item.archiveNo})`"
                :value="item.id"
              />
            </el-select>
            <el-button type="primary" :disabled="!selectedResidentId" @click="openDialog">
              <el-icon><Plus /></el-icon> {{ currentHistory?.id ? "修改健康史" : "帮居民录入" }}
            </el-button>
          </div>
        </div>
      </template>

      <el-empty
        v-if="!selectedResidentId"
        description="请先选择居民"
      />
      <el-empty
        v-else-if="!currentHistory?.id"
        description="该居民暂无健康史，可点击右上角录入"
      />
      <el-descriptions v-else :column="2" border class="custom-descriptions">
        <el-descriptions-item label="过往疾病" :span="2">
          <div v-if="(currentHistory.diseases || []).length > 0" class="row-list">
            <div v-for="(d, idx) in currentHistory.diseases" :key="`d-${idx}`">
              {{ d.diseaseName }}｜{{ d.treatmentStatus || "治疗情况未填" }}｜{{ d.checkedAt || "检查时间未填" }}
            </div>
          </div>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="遗传病史" :span="2">
          <div v-if="(currentHistory.geneticHistories || []).length > 0" class="row-list">
            <div v-for="(g, idx) in currentHistory.geneticHistories" :key="`g-${idx}`">
              {{ g.diseaseName }}（{{ g.relationToResident }}）
            </div>
          </div>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="过敏史" :span="2">
          <div v-if="(currentHistory.allergies || []).length > 0" class="row-list">
            <div v-for="(a, idx) in currentHistory.allergies" :key="`a-${idx}`">
              {{ a.allergen }}｜{{ a.allergicReaction || "过敏反应未填" }}
            </div>
          </div>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="吸烟习惯">{{ currentHistory.smokingHabit || "无" }}</el-descriptions-item>
        <el-descriptions-item label="饮酒习惯">{{ currentHistory.drinkingHabit || "无" }}</el-descriptions-item>
        <el-descriptions-item label="饮食习惯">{{ currentHistory.dietHabit || "无" }}</el-descriptions-item>
        <el-descriptions-item label="运动习惯">{{ currentHistory.exerciseHabit || "无" }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-dialog v-model="showDialog" :title="editingId ? '修改健康史' : '录入健康史'" width="640px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <el-form-item label="过往疾病（可填写多个）">
          <div class="multi-editor">
            <div v-for="(item, idx) in diseaseItems" :key="`disease-${idx}`" class="multi-row">
              <el-input v-model="item.diseaseName" placeholder="疾病名称（如：高血压）" />
              <el-input v-model="item.treatmentStatus" placeholder="治疗情况（可选）" />
              <el-date-picker v-model="item.checkedAt" type="date" value-format="YYYY-MM-DD" placeholder="检查时间（可选）" />
              <el-button type="danger" plain @click="removeDiseaseItem(idx)">删除</el-button>
            </div>
            <el-button type="primary" link @click="addDiseaseItem">+ 新增一项疾病</el-button>
          </div>
        </el-form-item>
        <el-form-item label="遗传病史（可填写多个）">
          <div class="multi-editor">
            <div v-for="(item, idx) in geneticItems" :key="`gen-${idx}`" class="multi-row">
              <el-input v-model="item.diseaseName" placeholder="病名（如：糖尿病）" />
              <el-input v-model="item.relationToResident" placeholder="与居民关系（如：母亲）" />
              <el-button type="danger" plain @click="removeGeneticItem(idx)">删除</el-button>
            </div>
            <el-button type="primary" link @click="addGeneticItem">+ 新增一条遗传病史</el-button>
          </div>
        </el-form-item>
        <el-form-item label="过敏史（可填写多个）">
          <div class="multi-editor">
            <div v-for="(item, idx) in allergyItems" :key="`all-${idx}`" class="multi-row">
              <el-input v-model="item.allergen" placeholder="过敏物质（如：青霉素）" />
              <el-input v-model="item.allergicReaction" placeholder="过敏反应（如：皮疹）" />
              <el-button type="danger" plain @click="removeAllergyItem(idx)">删除</el-button>
            </div>
            <el-button type="primary" link @click="addAllergyItem">+ 新增一条过敏史</el-button>
          </div>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="吸烟习惯"><el-input v-model="form.smokingHabit" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="饮酒习惯"><el-input v-model="form.drinkingHabit" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="饮食习惯"><el-input v-model="form.dietHabit" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="运动习惯"><el-input v-model="form.exerciseHabit" /></el-form-item></el-col>
        </el-row>
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
import { FirstAidKit, Plus } from "@element-plus/icons-vue";
import { medicalHistoryApi, residentApi } from "../api/modules";

const loading = ref(false);
const submitting = ref(false);
const residents = ref([]);
const selectedResidentId = ref();
const currentHistory = ref(null);
const showDialog = ref(false);
const editingId = ref(null);
const diseaseItems = ref([{ diseaseName: "", treatmentStatus: "", checkedAt: "" }]);
const geneticItems = ref([{ diseaseName: "", relationToResident: "" }]);
const allergyItems = ref([{ allergen: "", allergicReaction: "" }]);

const defaultForm = () => ({
  residentId: null,
  smokingHabit: "",
  drinkingHabit: "",
  dietHabit: "",
  exerciseHabit: "",
  sourceType: "DOCTOR_ENTRY"
});
const form = ref(defaultForm());

const addDiseaseItem = () => diseaseItems.value.push({ diseaseName: "", treatmentStatus: "", checkedAt: "" });
const removeDiseaseItem = (idx) => {
  diseaseItems.value.splice(idx, 1);
  if (diseaseItems.value.length === 0) diseaseItems.value = [{ diseaseName: "", treatmentStatus: "", checkedAt: "" }];
};
const addGeneticItem = () => geneticItems.value.push({ diseaseName: "", relationToResident: "" });
const removeGeneticItem = (idx) => {
  geneticItems.value.splice(idx, 1);
  if (geneticItems.value.length === 0) geneticItems.value = [{ diseaseName: "", relationToResident: "" }];
};
const addAllergyItem = () => allergyItems.value.push({ allergen: "", allergicReaction: "" });
const removeAllergyItem = (idx) => {
  allergyItems.value.splice(idx, 1);
  if (allergyItems.value.length === 0) allergyItems.value = [{ allergen: "", allergicReaction: "" }];
};

const loadResidents = async () => {
  const res = await residentApi.list({});
  residents.value = res.data || [];
};

const load = async () => {
  if (!selectedResidentId.value) {
    currentHistory.value = null;
    return;
  }
  loading.value = true;
  try {
    const res = await medicalHistoryApi.list(selectedResidentId.value);
    const one = (res.data || [])[0] || null;
    currentHistory.value = one;
  } finally {
    loading.value = false;
  }
};

const openDialog = () => {
  const row = currentHistory.value;
  editingId.value = row?.id || null;
  form.value = {
    residentId: row?.residentId || selectedResidentId.value,
    smokingHabit: row?.smokingHabit || "",
    drinkingHabit: row?.drinkingHabit || "",
    dietHabit: row?.dietHabit || "",
    exerciseHabit: row?.exerciseHabit || "",
    sourceType: row?.sourceType || "DOCTOR_ENTRY"
  };
  diseaseItems.value = (row?.diseases || []).map((i) => ({
    diseaseName: i.diseaseName || "",
    treatmentStatus: i.treatmentStatus || "",
    checkedAt: i.checkedAt || ""
  }));
  geneticItems.value = (row?.geneticHistories || []).map((i) => ({
    diseaseName: i.diseaseName || "",
    relationToResident: i.relationToResident || ""
  }));
  allergyItems.value = (row?.allergies || []).map((i) => ({
    allergen: i.allergen || "",
    allergicReaction: i.allergicReaction || ""
  }));
  showDialog.value = true;
};

const save = async () => {
  if (!form.value.residentId) return ElMessage.warning("请选择居民");
  
  // 检查是否所有字段都为空
  const hasNoDiseases = diseaseItems.value.every(item => !item.diseaseName);
  const hasNoGeneticHistories = geneticItems.value.every(item => !item.diseaseName || !item.relationToResident);
  const hasNoAllergies = allergyItems.value.every(item => !item.allergen);
  const hasNoHabits = !form.value.smokingHabit && !form.value.drinkingHabit && !form.value.dietHabit && !form.value.exerciseHabit;
  
  if (hasNoDiseases && hasNoGeneticHistories && hasNoAllergies && hasNoHabits) {
    try {
      await ElMessageBox.confirm(
        '您未填写任何健康史信息，是否确认保存空记录？',
        '确认保存',
        {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        }
      );
    } catch {
      return; // 用户取消保存
    }
  }
  
  submitting.value = true;
  try {
    const diseases = diseaseItems.value
      .map((i) => ({
        diseaseName: String(i.diseaseName || "").trim(),
        treatmentStatus: String(i.treatmentStatus || "").trim(),
        checkedAt: i.checkedAt ? i.checkedAt.split('T')[0] : null
      }))
      .filter((i) => i.diseaseName);
    const geneticHistories = geneticItems.value
      .map((i) => ({
        diseaseName: String(i.diseaseName || "").trim(),
        relationToResident: String(i.relationToResident || "").trim()
      }))
      .filter((i) => i.diseaseName && i.relationToResident);
    const allergies = allergyItems.value
      .map((i) => ({
        allergen: String(i.allergen || "").trim(),
        allergicReaction: String(i.allergicReaction || "").trim()
      }))
      .filter((i) => i.allergen);
    const payload = {
      ...form.value,
      diseases,
      geneticHistories,
      allergies
    };
    if (editingId.value) {
      await medicalHistoryApi.update(editingId.value, payload);
      ElMessage.success("健康史已更新");
    } else {
      await medicalHistoryApi.create(payload);
      ElMessage.success("健康史已录入");
    }
    showDialog.value = false;
    await load();
  } finally {
    submitting.value = false;
  }
};

onMounted(async () => {
  await loadResidents();
});
</script>

<style scoped>
.page-container { max-width: 1200px; margin: 0 auto; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
.page-card:hover { transform: none !important; }
.mb-20 { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; gap: 10px; }
.header-right { display: flex; align-items: center; gap: 12px; }
.header-icon { color: #1aa094; font-size: 20px; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }
.custom-descriptions { margin-top: 4px; }
.mr-8 { margin-right: 8px; }
.mb-8 { margin-bottom: 8px; }
.multi-editor { width: 100%; }
.multi-row { display: flex; gap: 8px; margin-bottom: 8px; align-items: center; }
.row-list { display: flex; flex-direction: column; gap: 6px; }
.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>