<template>
  <div class="permission-management animate-fade-in" v-loading="loading">
    <el-alert
      title="权限管理说明"
      type="info"
      description="在此您可以查看系统不同角色的权限范围。当前权限策略基于角色访问控制 (RBAC)。"
      show-icon
      class="mb-20"
    />

    <el-row :gutter="20">
      <el-col v-for="role in roles" :key="role.name" :span="12" class="mb-20">
        <el-card class="page-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <el-icon class="header-icon"><UserFilled /></el-icon>
                <span class="title">{{ role.label }} ({{ role.name }})</span>
              </div>
              <el-tag :type="role.type" size="small">{{ role.name }}</el-tag>
            </div>
          </template>
          
          <div class="permission-list">
            <div v-for="p in role.permissions" :key="p" class="p-item">
              <el-icon class="p-icon"><Check /></el-icon>
              <span>{{ p }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { UserFilled, Check } from "@element-plus/icons-vue";

const loading = ref(false);

const roles = ref([
  {
    name: "ADMIN",
    label: "系统管理员",
    type: "danger",
    permissions: [
      "系统全局看板", "账号全量管理", "操作日志审计", "数据备份与恢复",
      "权限策略查看", "疾病分布统计", "居民档案管理"
    ]
  },
  {
    name: "DOCTOR",
    label: "社区医生",
    type: "success",
    permissions: [
      "医生工作台", "居民档案建立与维护", "体征数据录入", "随访计划制定",
      "随访记录录入", "异常预警处理", "健康评估与指导"
    ]
  },
  {
    name: "PUBLIC_HEALTH_MANAGER",
    label: "公共卫生管理者",
    type: "warning",
    permissions: [
      "全区健康看板", "疾病分布分析", "高危人群筛查", "干预效果评价",
      "健康知识库维护", "操作日志查看"
    ]
  },
  {
    name: "RESIDENT",
    label: "社区居民",
    type: "info",
    permissions: [
      "个人健康概览", "个人档案自助维护", "测量记录查询", "就诊记录查询",
      "用药/疫苗记录查询", "随访提醒查看", "健康评估反馈", "健康知识库阅读"
    ]
  }
]);
</script>

<style scoped>
.permission-management { max-width: 1200px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.page-card { border-radius: 12px; border: none; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; gap: 10px; }
.header-icon { font-size: 20px; color: #1aa094; }
.title { font-size: 16px; font-weight: 600; color: #2f4056; }

.permission-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.p-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #64748b;
}

.p-icon {
  color: #10b981;
}

.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
</style>
