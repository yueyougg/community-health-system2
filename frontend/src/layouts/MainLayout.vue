<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-left">
        <img :src="logoImg" alt="Logo" class="logo-img" />
        <div class="system-title">社区健康档案管理系统</div>
      </div>
      <div class="user-info">
        <span class="user-name">{{ auth.username }}</span>
        <el-tag size="default" effect="dark" :type="roleType">{{ roleName }}</el-tag>
        <el-divider direction="vertical" />
        <el-button type="danger" link @click="onLogout">
          <el-icon><SwitchButton /></el-icon> 退出
        </el-button>
      </div>
    </el-header>
    <el-container class="main-container">
      <el-aside width="220px" class="aside">
        <el-menu
          :default-active="$route.fullPath"
          class="sidebar-menu"
          router
        >
          <!-- 数据看板：仅限 管理员 (依据接口文档 VII) -->
          <el-menu-item v-if="auth.role === 'ADMIN'" index="/dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>运行看板</span>
          </el-menu-item>

          <!-- 居民菜单 (依据接口文档 III, IV, V, VIII) -->
          <template v-if="auth.role === 'RESIDENT'">
            <el-menu-item index="/health-overview">
              <el-icon><DataBoard /></el-icon>
              <span>健康概览</span>
            </el-menu-item>
            <el-menu-item index="/health-reminders">
              <el-icon><Bell /></el-icon>
              <span>健康提醒</span>
            </el-menu-item>
            <el-sub-menu index="profile-group">
              <template #title>
                <el-icon><User /></el-icon>
                <span>健康档案</span>
              </template>
              <el-menu-item index="/my-profile" class="sub-menu-item">
                <el-icon class="sub-icon"><InfoFilled /></el-icon>
                <span>基本信息</span>
              </el-menu-item>
              <el-menu-item index="/my-profile?tab=measurements" class="sub-menu-item">
                <el-icon class="sub-icon"><Monitor /></el-icon>
                <span>测量记录</span>
              </el-menu-item>
              <el-menu-item index="/my-profile?tab=history" class="sub-menu-item">
                <el-icon class="sub-icon"><FirstAidKit /></el-icon>
                <span>我的健康史</span>
              </el-menu-item>
              <el-menu-item index="/my-profile?tab=visits" class="sub-menu-item">
                <el-icon class="sub-icon"><List /></el-icon>
                <span>就诊记录</span>
              </el-menu-item>
              <el-menu-item index="/my-profile?tab=medications" class="sub-menu-item">
                <el-icon class="sub-icon"><Management /></el-icon>
                <span>用药记录</span>
              </el-menu-item>
              <el-menu-item index="/my-profile?tab=vaccinations" class="sub-menu-item">
                <el-icon class="sub-icon"><Check /></el-icon>
                <span>疫苗接种</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/knowledge">
              <el-icon><Reading /></el-icon>
              <span>健康知识</span>
            </el-menu-item>
            <el-menu-item index="/ai-assistant">
              <el-icon><ChatDotRound /></el-icon>
              <span>健康助手</span>
            </el-menu-item>
          </template>

          <!-- 社区医生菜单 (依据接口文档 III, IV, V, VI) -->
          <template v-if="auth.role === 'DOCTOR'">
            <el-menu-item index="/doctor-dashboard">
              <el-icon><DataBoard /></el-icon>
              <span>工作台</span>
            </el-menu-item>
            <el-sub-menu index="doctor-group">
              <template #title>
                <el-icon><UserFilled /></el-icon>
                <span>居民档案管理</span>
              </template>
              <el-menu-item index="/residents" class="sub-menu-item">
                <el-icon class="sub-icon"><UserFilled /></el-icon>
                <span>档案列表</span>
              </el-menu-item>
              <el-menu-item index="/measurements" class="sub-menu-item">
                <el-icon class="sub-icon"><Monitor /></el-icon>
                <span>测量录入</span>
              </el-menu-item>
              <el-menu-item index="/doctor-medical-histories" class="sub-menu-item">
                <el-icon class="sub-icon"><FirstAidKit /></el-icon>
                <span>居民健康史</span>
              </el-menu-item>
              <el-menu-item index="/health-assessments" class="sub-menu-item">
                <el-icon class="sub-icon"><Guide /></el-icon>
                <span>健康评估</span>
              </el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="follow-up-group">
              <template #title>
                <el-icon><Calendar /></el-icon>
                <span>随访管理</span>
              </template>
              <el-menu-item index="/follow-up-plans" class="sub-menu-item">
                <el-icon class="sub-icon"><Calendar /></el-icon>
                <span>随访计划</span>
              </el-menu-item>
              <el-menu-item index="/follow-up-records" class="sub-menu-item">
                <el-icon class="sub-icon"><Document /></el-icon>
                <span>随访记录</span>
              </el-menu-item>
            </el-sub-menu>
            <el-sub-menu index="clinical-group">
              <template #title>
                <el-icon><FirstAidKit /></el-icon>
                <span>诊疗记录管理</span>
              </template>
              <el-menu-item index="/doctor-visits" class="sub-menu-item">
                <el-icon class="sub-icon"><List /></el-icon>
                <span>就诊记录</span>
              </el-menu-item>
              <el-menu-item index="/doctor-medications" class="sub-menu-item">
                <el-icon class="sub-icon"><Management /></el-icon>
                <span>用药记录</span>
              </el-menu-item>
              <el-menu-item index="/doctor-vaccinations" class="sub-menu-item">
                <el-icon class="sub-icon"><Check /></el-icon>
                <span>疫苗接种</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/alerts">
              <el-icon><Warning /></el-icon>
              <span>预警中心</span>
            </el-menu-item>
            <el-menu-item index="/knowledge">
              <el-icon><Reading /></el-icon>
              <span>健康知识库</span>
            </el-menu-item>
            <el-menu-item index="/intervention-effect">
              <el-icon><TrendCharts /></el-icon>
              <span>干预效果评估</span>
            </el-menu-item>
          </template>

          <!-- 系统管理员菜单 (依据接口文档 VIII) -->
          <template v-if="auth.role === 'ADMIN'">
            <el-sub-menu index="admin-group">
              <template #title>
                <el-icon><Setting /></el-icon>
                <span>系统管理</span>
              </template>
              <el-menu-item index="/users" class="sub-menu-item">
                <el-icon class="sub-icon"><UserFilled /></el-icon>
                <span>账号管理</span>
              </el-menu-item>
              <el-menu-item index="/permissions" class="sub-menu-item">
                <el-icon class="sub-icon"><Lock /></el-icon>
                <span>权限管理</span>
              </el-menu-item>
              <el-menu-item index="/backups" class="sub-menu-item">
                <el-icon class="sub-icon"><Management /></el-icon>
                <span>数据备份</span>
              </el-menu-item>
              <el-menu-item index="/system-logs" class="sub-menu-item">
                <el-icon class="sub-icon"><Operation /></el-icon>
                <span>操作日志</span>
              </el-menu-item>
            </el-sub-menu>
          </template>

          <!-- 公共卫生管理者菜单 (依据接口文档 VII) -->
          <template v-if="auth.role === 'PUBLIC_HEALTH_MANAGER'">
            <el-menu-item index="/phm-dashboard">
              <el-icon><DataBoard /></el-icon>
              <span>全区概览</span>
            </el-menu-item>
            <el-sub-menu index="phm-group">
              <template #title>
                <el-icon><TrendCharts /></el-icon>
                <span>统计分析</span>
              </template>
              <el-menu-item index="/disease-analysis" class="sub-menu-item">
                <el-icon class="sub-icon"><TrendCharts /></el-icon>
                <span>疾病分析</span>
              </el-menu-item>
              <el-menu-item index="/intervention-effect" class="sub-menu-item">
                <el-icon class="sub-icon"><PieChart /></el-icon>
                <span>效果评估</span>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-aside>
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" :key="$route.fullPath" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { useAuthStore } from "../store/auth";
import { useAiChatStore } from "../store/aiChat";
import logoImg from "../assets/社区健康档案管理系统logo设计.png";
import { 
  DataBoard, User, Bell, Reading, UserFilled, 
  Calendar, Monitor, Document, Guide, Warning, 
  Setting, Operation, TrendCharts, PieChart,
  SwitchButton, InfoFilled, FirstAidKit, List,
  Management, Check, Lock, ChatDotRound
} from "@element-plus/icons-vue";

const router = useRouter();
const auth = useAuthStore();
const aiChatStore = useAiChatStore();

const roleName = computed(() => {
  const roles = {
    'ADMIN': '系统管理员',
    'DOCTOR': '社区医生',
    'RESIDENT': '居民',
    'PUBLIC_HEALTH_MANAGER': '公共卫生管理者'
  };
  return roles[auth.role] || auth.role;
});

const roleType = computed(() => {
  const types = {
    'ADMIN': 'danger',
    'DOCTOR': 'primary',
    'RESIDENT': 'success',
    'PUBLIC_HEALTH_MANAGER': 'warning'
  };
  return types[auth.role] || 'info';
});

const onLogout = () => {
  auth.logout();
  aiChatStore.clearMessages(); // 退出登录时清空AI聊天记录
  router.push("/login");
};
</script>

<style scoped>
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.header {
  background-color: #ffffff;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 64px !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  z-index: 100;
  flex-shrink: 0;
}

.main-container {
  height: calc(100vh - 64px);
  overflow: hidden;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-img {
  height: 40px;
  width: auto;
}

.system-title {
  font-size: 20px;
  font-weight: 600;
  color: #2f4056;
  letter-spacing: 0.5px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-name {
  font-weight: 500;
  color: #606266;
}

.aside {
  background-color: #2f4056;
  transition: width 0.3s ease;
  box-shadow: 2px 0 8px rgba(0,0,0,0.1);
  height: 100%;
  overflow-y: auto;
  flex-shrink: 0;
}

/* 隐藏侧边栏原生滚动条，保留滚动功能 */
.aside::-webkit-scrollbar {
  width: 6px;
}
.aside::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.sidebar-menu {
  border-right: none;
  background-color: #2f4056;
}

:deep(.el-menu) {
  background-color: transparent;
  border-right: none;
}

:deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.7);
}

:deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.7) !important;
}

:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.1) !important;
  color: #ffffff !important;
}

:deep(.el-menu-item.is-active) {
  background-color: #1aa094 !important;
  color: #ffffff !important;
  font-weight: 600;
}

.sub-menu-item {
  background-color: rgba(0, 0, 0, 0.2) !important;
  padding-left: 50px !important;
}

.sub-icon {
  font-size: 16px;
  margin-right: 8px;
}

.main-content {
  padding: 24px;
  background-color: #f5f7fa;
  height: 100%;
  overflow-y: auto;
}
</style>
