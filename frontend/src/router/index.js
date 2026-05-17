import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "../store/auth";

const LoginView = () => import("../views/LoginView.vue");
const RegisterView = () => import("../views/RegisterView.vue");
const MainLayout = () => import("../layouts/MainLayout.vue");
const DashboardView = () => import("../views/DashboardView.vue");
const ResidentsView = () => import("../views/ResidentsView.vue");
const MeasurementsView = () => import("../views/MeasurementsView.vue");
const HealthAssessmentView = () => import("../views/HealthAssessmentView.vue");
const DoctorDashboardView = () => import("../views/DoctorDashboardView.vue");
const DoctorMedicalHistoryView = () => import("../views/DoctorMedicalHistoryView.vue");
const FollowUpPlanView = () => import("../views/FollowUpPlanView.vue");
const FollowUpRecordView = () => import("../views/FollowUpRecordView.vue");
const DoctorVisitRecordsView = () => import("../views/DoctorVisitRecordsView.vue");
const DoctorMedicationRecordsView = () => import("../views/DoctorMedicationRecordsView.vue");
const DoctorVaccinationRecordsView = () => import("../views/DoctorVaccinationRecordsView.vue");
const AlertsView = () => import("../views/AlertsView.vue");

const MyProfileView = () => import("../views/MyProfileView.vue");
const HealthOverviewView = () => import("../views/HealthOverviewView.vue");
const HealthRemindersView = () => import("../views/HealthRemindersView.vue");
const HealthKnowledgeView = () => import("../views/HealthKnowledgeView.vue");
const UserManagementView = () => import("../views/UserManagementView.vue");
const PhmDashboardView = () => import("../views/PhmDashboardView.vue");
const PermissionManagementView = () => import("../views/PermissionManagementView.vue");
const BackupManagementView = () => import("../views/BackupManagementView.vue");
const DiseaseAnalysisView = () => import("../views/DiseaseAnalysisView.vue");
const AiAssistantView = () => import("../views/AiAssistantView.vue");

// 补齐缺失视图导入
const SystemLogsView = () => import("../views/SystemLogsView.vue");
const InterventionEffectView = () => import("../views/InterventionEffectView.vue");

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      name: "login",
      component: LoginView
    },
    {
      path: "/register",
      name: "register",
      component: RegisterView
    },
    {
      path: "/",
      component: MainLayout,
      children: [
        { path: "dashboard", name: "dashboard", component: DashboardView },
        { path: "doctor-dashboard", name: "doctor-dashboard", component: DoctorDashboardView },
        { path: "doctor-medical-histories", name: "doctor-medical-histories", component: DoctorMedicalHistoryView },
        { path: "residents", name: "residents", component: ResidentsView },
        { path: "measurements", name: "measurements", component: MeasurementsView },
        { path: "health-assessments", name: "health-assessments", component: HealthAssessmentView },
        { path: "follow-up-plans", name: "follow-up-plans", component: FollowUpPlanView },
        { path: "follow-up-records", name: "follow-up-records", component: FollowUpRecordView },
        { path: "doctor-visits", name: "doctor-visits", component: DoctorVisitRecordsView },
        { path: "doctor-medications", name: "doctor-medications", component: DoctorMedicationRecordsView },
        { path: "doctor-vaccinations", name: "doctor-vaccinations", component: DoctorVaccinationRecordsView },
        { path: "alerts", name: "alerts", component: AlertsView },
        
        // Resident routes
        { path: "health-overview", name: "health-overview", component: HealthOverviewView },
        { path: "health-reminders", name: "health-reminders", component: HealthRemindersView },
        { path: "my-profile", name: "my-profile", component: MyProfileView },
        { path: "knowledge", name: "knowledge", component: HealthKnowledgeView },
        { path: "ai-assistant", name: "ai-assistant", component: AiAssistantView },

        // Admin routes
        { path: "users", name: "users", component: UserManagementView },
        { path: "permissions", name: "permissions", component: PermissionManagementView },
        { path: "backups", name: "backups", component: BackupManagementView },
        { path: "system-logs", name: "system-logs", component: SystemLogsView },

        // Public Health Manager routes
        { path: "phm-dashboard", name: "phm-dashboard", component: PhmDashboardView },
        { path: "disease-analysis", name: "disease-analysis", component: DiseaseAnalysisView },
        { path: "intervention-effect", name: "intervention-effect", component: InterventionEffectView }
      ]
    }
  ]
});

router.beforeEach((to, from, next) => {
  const store = useAuthStore();
  
  // 未登录拦截
  if (to.path !== "/login" && to.path !== "/register" && !store.isLogin) {
    next("/login");
    return;
  }
  
  // 角色首屏重定向映射
  const roleHomeMap = {
    'RESIDENT': '/health-overview',
    'DOCTOR': '/doctor-dashboard',
    'ADMIN': '/dashboard',
    'PUBLIC_HEALTH_MANAGER': '/phm-dashboard'
  };

  const homePath = roleHomeMap[store.role] || '/login';

  // 已登录重定向逻辑
  if ((to.path === "/login" || to.path === "/register") && store.isLogin) {
    next(homePath);
    return;
  }

  // 根路径重定向
  if (to.path === "/") {
    next(homePath);
    return;
  }

  // 权限校验：防止非法访问 dashboard
  if (to.path === "/dashboard") {
    if (['RESIDENT', 'DOCTOR', 'PUBLIC_HEALTH_MANAGER'].includes(store.role)) {
      next(homePath);
      return;
    }
  }

  next();
});

export default router;
