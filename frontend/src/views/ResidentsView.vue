<template>
  <div class="residents-view">
    <el-alert
      title="档案管理提示"
      type="info"
      description="在此您可以查看和管理社区居民的健康档案。您可以搜索居民姓名来快速查找档案。"
      show-icon
      class="mb-20"
    />
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="filter.keyword" placeholder="搜索姓名或档案编号" style="width: 250px" @keyup.enter="load" clearable />
        <base-button type="primary" icon="Search" @click="load">搜索</base-button>
        <base-button v-if="canEdit" type="success" @click="openDialog()">新增居民档案</base-button>
      </div>
      <el-table :data="list" border>
        <el-table-column prop="archiveNo" label="档案编号" width="170" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="birthDate" label="出生日期" width="120" />
        <el-table-column prop="phone" label="联系方式" width="140" />
        <el-table-column prop="occupation" label="职业" width="120" />
        <el-table-column prop="address" label="住址" />
        <el-table-column prop="archiveOrg" label="建档机构" width="150" />
        <el-table-column v-if="canEdit" label="操作" width="180">
          <template #default="{ row }">
            <base-button size="small" type="primary" @click="openDialog(row)">编辑</base-button>
            <base-button size="small" type="danger" @click="onDelete(row.id)">删除</base-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showDialog" :title="form.id ? '编辑居民档案' : '新增居民档案'" width="640px">
      <el-form :model="form" label-width="95px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="档案编号">
              <el-input v-model="form.archiveNo" :placeholder="form.id ? '' : '保存后自动生成'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="!form.id" label="系统用户名">
              <el-select v-model="form.userId" placeholder="请选择居民用户" filterable style="width: 100%">
                <el-option
                  v-for="u in availableUsers"
                  :key="u.id"
                  :label="u.username"
                  :value="u.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item v-else label="系统用户名">
              <el-input value="已绑定（不可修改）" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-select v-model="form.gender" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="身份证号">
              <el-input v-model="form.idCard" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系方式">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="职业">
              <el-input v-model="form.occupation" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="建档日期">
              <el-date-picker v-model="form.archiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="建档机构">
          <el-input v-model="form.archiveOrg" />
        </el-form-item>
        <el-form-item label="住址">
          <el-input v-model="form.address" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { residentApi } from "../api/modules";
import { useAuthStore } from "../store/auth";

const auth = useAuthStore();
const canEdit = computed(() => auth.role === "ADMIN" || auth.role === "DOCTOR");

const list = ref([]);
const filter = reactive({
  keyword: ""
});
const showDialog = ref(false);
const availableUsers = ref([]);
const emptyForm = () => ({
  id: null,
  userId: null,
  archiveNo: "",
  name: "",
  gender: "",
  birthDate: "",
  idCard: "",
  phone: "",
  address: "",
  occupation: "",
  archiveDate: "",
  archiveOrg: ""
});
const form = reactive(emptyForm());

const load = async () => {
  try {
    const res = await residentApi.list(filter);
    list.value = res.data || [];
  } catch (error) {
    console.error("加载居民档案列表失败:", error);
    ElMessage.error(error.message || "获取档案列表失败");
  }
};

const openDialog = (row) => {
  Object.assign(form, emptyForm(), row || {});
  if (!row) {
    loadAvailableUsers();
  }
  showDialog.value = true;
};

const loadAvailableUsers = async () => {
  const res = await residentApi.availableUsers();
  availableUsers.value = res.data || [];
};

const onSubmit = async () => {
  try {
    if (!form.id && !form.userId) {
      ElMessage.warning("请先选择系统居民用户名");
      return;
    }
    if (form.id) {
      await residentApi.update(form.id, form);
    } else {
      await residentApi.create(form);
    }
    ElMessage.success("保存成功");
    showDialog.value = false;
    load();
  } catch (error) {
    ElMessage.error(error.message);
  }
};

const onDelete = async (id) => {
  try {
    await ElMessageBox.confirm("确认删除该居民档案？", "提示", { type: "warning" });
    await residentApi.remove(id);
    ElMessage.success("删除成功");
    load();
  } catch (error) {
    if (error !== "cancel") {
      console.error("删除档案失败:", error);
      ElMessage.error(error.message || "删除失败");
    }
  }
};

load();
</script>

<style scoped>
.residents-view {
  max-width: 1200px;
  margin: 0 auto;
}
.mb-20 {
  margin-bottom: 20px;
}
</style>
