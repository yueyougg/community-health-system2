<template>
  <div class="user-manage">
    <h2>用户管理</h2>
    
    <div class="filter-bar">
      <el-select v-model="filter.role" placeholder="选择角色" clearable style="width: 150px" @change="fetchUsers">
        <el-option label="社区居民" value="RESIDENT" />
        <el-option label="社区医生" value="DOCTOR" />
        <el-option label="系统管理员" value="ADMIN" />
        <el-option label="公共卫生管理者" value="PUBLIC_HEALTH_MANAGER" />
      </el-select>
      <el-input v-model="filter.keyword" placeholder="搜索用户名" style="width: 200px" @keyup.enter="fetchUsers" />
      <button class="button" @click="fetchUsers">查询</button>
    </div>

    <el-table :data="users" v-loading="loading" style="width: 100%; margin-top: 20px">
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="role" label="角色">
        <template #default="{ row }">
          <el-tag :type="getRoleType(row.role)">{{ getRoleName(row.role) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'">
            {{ row.enabled ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <base-button size="small" @click="openEdit(row)">修改角色</base-button>
          <base-button 
            size="small" 
            :type="row.enabled ? 'danger' : 'success'" 
            @click="toggleStatus(row)"
          >
            {{ row.enabled ? '禁用' : '启用' }}
          </base-button>
          <base-button size="small" type="warning" @click="openReset(row)">重置密码</base-button>
          <base-button size="small" type="danger" @click="deleteUser(row)">删除</base-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 修改角色弹窗 -->
    <el-dialog v-model="roleDialog.visible" title="修改角色" width="400px">
      <el-form :model="roleDialog.form">
        <el-form-item label="角色">
          <el-select v-model="roleDialog.form.role">
            <el-option label="社区居民" value="RESIDENT" />
            <el-option label="社区医生" value="DOCTOR" />
            <el-option label="系统管理员" value="ADMIN" />
            <el-option label="公共卫生管理者" value="PUBLIC_HEALTH_MANAGER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitRole">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="pwdDialog.visible" title="重置密码" width="400px">
      <el-form :model="pwdDialog.form">
        <el-form-item label="新密码">
          <el-input v-model="pwdDialog.form.password" placeholder="请输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { userApi } from "../api/modules";

const loading = ref(false);
const users = ref([]);
const filter = reactive({
  keyword: "",
  role: ""
});

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
});

const roleDialog = reactive({
  visible: false,
  form: { id: null, role: "" }
});

const pwdDialog = reactive({
  visible: false,
  form: { id: null, password: "" }
});

const fetchUsers = async () => {
  loading.value = true;
  try {
    const params = {
      keyword: filter.keyword || null,
      role: filter.role || null,
      page: pagination.currentPage - 1,
      size: pagination.pageSize
    };
    const res = await userApi.list(params);
    users.value = res.data.content;
    pagination.total = res.data.totalElements;
  } catch (error) {
    ElMessage.error("获取用户列表失败");
  } finally {
    loading.value = false;
  }
};

const handleSizeChange = (size) => {
  pagination.pageSize = size;
  pagination.currentPage = 1;
  fetchUsers();
};

const handleCurrentChange = (page) => {
  pagination.currentPage = page;
  fetchUsers();
};

const getRoleName = (role) => {
  const map = {
    RESIDENT: "社区居民",
    DOCTOR: "社区医生",
    ADMIN: "系统管理员",
    PUBLIC_HEALTH_MANAGER: "公共卫生管理者"
  };
  return map[role] || role;
};

const getRoleType = (role) => {
  const map = {
    RESIDENT: "",
    DOCTOR: "success",
    ADMIN: "danger",
    PUBLIC_HEALTH_MANAGER: "warning"
  };
  return map[role] || "info";
};

const openEdit = (row) => {
  roleDialog.form.id = row.id;
  roleDialog.form.role = row.role;
  roleDialog.visible = true;
};

const submitRole = async () => {
  try {
    await userApi.updateRole(roleDialog.form.id, roleDialog.form.role);
    ElMessage.success("修改成功");
    roleDialog.visible = false;
    fetchUsers();
  } catch (error) {
    ElMessage.error("修改失败");
  }
};

const toggleStatus = async (row) => {
  const action = row.enabled ? "禁用" : "启用";
  try {
    await ElMessageBox.confirm(`确定要${action}该用户吗？`, "提示", {
      type: "warning"
    });
    await userApi.updateStatus(row.id, !row.enabled);
    ElMessage.success(`${action}成功`);
    fetchUsers();
  } catch (error) {
    if (error !== "cancel") ElMessage.error("操作失败");
  }
};

const openReset = (row) => {
  pwdDialog.form.id = row.id;
  pwdDialog.form.password = "";
  pwdDialog.visible = true;
};

const submitReset = async () => {
  if (!pwdDialog.form.password) {
    return ElMessage.warning("请输入新密码");
  }
  try {
    await userApi.resetPassword(pwdDialog.form.id, pwdDialog.form.password);
    ElMessage.success("重置成功");
    pwdDialog.visible = false;
  } catch (error) {
    ElMessage.error("重置失败");
  }
};

const deleteUser = async (row) => {
  try {
    await ElMessageBox.confirm("确定要删除该用户吗？此操作将同时删除其关联的居民档案（如果有）", "警告", {
      type: "error",
      confirmButtonText: "确定",
      cancelButtonText: "取消"
    });
    await userApi.delete(row.id);
    ElMessage.success("删除成功");
    fetchUsers();
  } catch (error) {
    if (error !== "cancel") ElMessage.error("删除失败");
  }
};

onMounted(() => {
  fetchUsers();
});
</script>

<style scoped>
.user-manage {
  padding: 20px;
  background: rgb(255,250,235);
  border-radius: 8px;
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
/* From Uiverse.io by levxyca */
.button {
  font-size: 14px;
  color: #fafafa;
  text-transform: uppercase;
  padding: 8px 20px;
  border-radius: 10px;
  border: 2px solid #fafafa;
  background: rgb(51, 153, 255);
  box-shadow: 3px 3px rgba(192, 192, 192, 0.9);
  cursor: pointer;
  margin: 0;
  line-height: 1.2;
}

.button:active {
  box-shadow: none;
  transform: translate(3px, 3px);
}
</style>