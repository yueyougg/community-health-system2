<template>
  <div class="login-wrap">
    <!-- 静态几何背景层 -->
    <div class="bg-grid"></div>
    <div class="bg-static-shapes">
      <div class="shape circle-1"></div>
      <div class="shape circle-2"></div>
      <div class="shape lines-1"></div>
      <!-- 新增亮色亮元素 -->
      <div class="shape bright-rect-1"></div>
      <div class="shape bright-rect-2"></div>
      <div class="shape bright-triangle"></div>
    </div>
    
    <div class="login-card">
      <h2>社区健康档案管理系统</h2>
      <el-form :model="form" @submit.prevent>
        <!-- 隐藏的输入框用于干扰浏览器的自动填充 -->
        <div style="position: absolute; top: -9999px; left: -9999px;">
          <input type="text" name="fake_username" tabindex="-1">
          <input type="password" name="fake_password" tabindex="-1">
        </div>
        <el-form-item label="角色">
          <el-select v-model="form.role" placeholder="请选择角色">
            <el-option label="社区居民" value="RESIDENT" />
            <el-option label="社区医生" value="DOCTOR" />
            <el-option label="系统管理员" value="ADMIN" />
            <el-option label="公共卫生管理者" value="PUBLIC_HEALTH_MANAGER" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名（系统账号）" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" autocomplete="new-password" />
        </el-form-item>
        <el-form-item>
          <div class="button-group">
            <LoginButton type="primary" :disabled="loading" @click="onLogin" style="flex: 1; justify-content: center; height: 44px; border-radius: 8px;">
              {{ loading ? '登录中...' : '登录' }}
            </LoginButton>
            <LoginButton @click="onRegister" style="flex: 1; justify-content: center; height: 44px; border-radius: 8px;">
              去注册
            </LoginButton>
          </div>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { useAuthStore } from "../store/auth";
import { residentApi } from "../api/modules";
import LoginButton from "../components/LoginButton.vue";

const router = useRouter();
const auth = useAuthStore();
const loading = ref(false);
const form = reactive({
  role: "RESIDENT",
  username: "",
  password: "",
});

const onLogin = async () => {
  loading.value = true;
  try {
    await auth.login(form);
    // 如果是居民登录，且没有建档，弹出提醒
    if (auth.role === "RESIDENT") {
      try {
        const res = await residentApi.me();
        if (!res.data || !res.data.id) {
          ElMessageBox.confirm(
            '系统检测到您尚未建立个人健康档案。建立档案后，您可以查看测量记录、就诊历史及接种记录。是否立即前往建立档案？',
            '温馨提示',
            {
              confirmButtonText: '立即前往',
              cancelButtonText: '暂不建立',
              type: 'info',
              center: true
            }
          ).then(() => {
            router.push("/my-profile");
          }).catch(() => {
            router.push("/health-overview");
          });
          return; // 弹出提示后由弹窗逻辑处理跳转
        }
      }
      catch (error) {
        if (error.response?.status === 404) {
          ElMessageBox.confirm(
            '系统检测到您尚未建立个人健康档案。建立档案后，您可以查看测量记录、就诊历史及接种记录。是否立即前往建立档案？',
            '温馨提示',
            {
              confirmButtonText: '立即前往',
              cancelButtonText: '暂不建立',
              type: 'info',
              center: true
            }
          ).then(() => {
            router.push("/my-profile");
          }).catch(() => {
            router.push("/health-overview");
          });
          return;
        }
      }
      router.push("/health-overview");
    }
    else {
      ElMessage.success("登录成功");
      router.push("/dashboard");
    }
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
};

const onRegister = () => {
  router.push("/register");
};
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: grid;
  place-items: center;
  position: relative;
  overflow: hidden;
  background-color: #f3f4f6; /* 浅灰色背景 */
}

/* 静态网格背景 - 适配浅色背景 */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(0, 0, 0, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 0, 0, 0.03) 1px, transparent 1px);
  background-size: 50px 50px;
  z-index: 0;
}

/* 静态几何形状 - 适配浅色背景 */
.bg-static-shapes {
  position: absolute;
  inset: 0;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}

.shape {
  position: absolute;
}

.circle-1 {
  width: 400px;
  height: 400px;
  border: 1px solid rgba(45, 212, 191, 0.2);
  border-radius: 50%;
  top: -100px;
  right: 10%;
}

.circle-2 {
  width: 250px;
  height: 250px;
  border: 1px solid rgba(45, 212, 191, 0.15);
  border-radius: 50%;
  bottom: 15%;
  left: 5%;
}

.lines-1 {
  width: 200px;
  height: 200px;
  background: repeating-linear-gradient(
    45deg,
    rgba(45, 212, 191, 0.05),
    rgba(45, 212, 191, 0.05) 1px,
    transparent 1px,
    transparent 10px
  );
  bottom: 10%;
  right: 15%;
}

/* 亮色元素 - 适配浅色背景 */
.bright-rect-1 {
  width: 120px;
  height: 120px;
  background: linear-gradient(45deg, rgba(45, 212, 191, 0.6), rgba(45, 212, 191, 0.2));
  top: 15%;
  left: 12%;
  transform: rotate(15deg);
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(45, 212, 191, 0.15);
}

.bright-rect-2 {
  width: 80px;
  height: 80px;
  background: linear-gradient(225deg, rgba(251, 191, 36, 0.5), rgba(251, 191, 36, 0.1));
  bottom: 25%;
  right: 8%;
  transform: rotate(-10deg);
  border-radius: 15px;
  box-shadow: 0 10px 25px rgba(251, 191, 36, 0.15);
}

.bright-triangle {
  width: 150px;
  height: 150px;
  background: rgba(45, 212, 191, 0.1);
  clip-path: polygon(50% 0%, 0% 100%, 100% 100%);
  top: 45%;
  right: 20%;
  transform: rotate(160deg);
  filter: blur(2px);
}

/* 装饰性几何元素 - 更加灵动 */
.login-card::before {
  content: "";
  position: absolute;
  top: -100px;
  right: -80px;
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, rgba(45, 212, 191, 0.5), transparent);
  clip-path: polygon(30% 0%, 70% 0%, 100% 30%, 100% 70%, 70% 100%, 30% 100%, 0% 70%, 0% 30%); /* 不规则八边形 */
  z-index: -1;
  animation: rotate-slow 15s infinite linear;
}

@keyframes rotate-slow {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.login-card {
  width: min(440px, 92vw);
  background: rgba(255, 255, 255, 0.4); 
  border: 1px solid rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(20px) saturate(150%);
  border-radius: 24px;
  padding: 50px 40px;
  box-shadow: 
    0 20px 40px -10px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 1);
  position: relative;
  z-index: 2;
  transition: all 0.3s ease;
}

/* 移除悬浮上浮效果，改为微妙的亮度变化 */
.login-card:hover {
  background: rgba(255, 255, 255, 0.95); /* 悬浮时透明度也相应降低 */
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.08);
}

.button-group {
  display: flex;
  gap: 16px;
  width: 100%;
  margin-top: 24px;
}

h2 {
  margin: 0 0 30px;
  text-align: center;
  color: #1f2937;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.5px;
}

/* 优化表单文字颜色 */
:deep(.el-form-item__label) {
  color: #3e4651 !important;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px !important;
}

:deep(.el-input__wrapper) {
  background-color: #f9fafb !important;
  box-shadow: inset 0 0 0 1px #e5e7eb !important;
  border-radius: 8px !important;
  padding: 5px 16px !important;
  transition: all 0.2s ease;
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: inset 0 0 0 1px #2dd4bf !important;
  background-color: #ffffff !important;
}

:deep(.el-input__inner) {
  color: #1f2937 !important;
  height: 32px;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-select .el-input__wrapper) {
  background-color: #f9fafb !important;
}

:deep(.el-button) {
  height: 44px;
  border-radius: 8px;
  font-weight: 600;
}
</style>
