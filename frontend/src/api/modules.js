import http from "./http";

export const authApi = {
  login: (payload) => http.post("/auth/login", payload),
  register: (payload) => http.post("/auth/register", payload),
  me: () => http.get("/auth/me")
};

export const statsApi = {
  overview: () => http.get("/stats/overview"),
  disease: () => http.get("/stats/disease-distribution"),
  indicatorDistribution: () => http.get("/stats/health-indicator-distribution"),
  gender: () => http.get("/stats/gender-distribution"),
  riskScreening: () => http.get("/stats/risk-screening"),
  interventionEffect: () => http.get("/stats/intervention-effect"),
  genderAbnormalStats: () => http.get("/stats/gender-abnormal-stats"),
  ageGroupAbnormalStats: () => http.get("/stats/age-group-abnormal-stats")
};

export const residentApi = {
  list: (params) => http.get("/residents", { params }),
  availableUsers: () => http.get("/residents/available-users"),
  me: () => http.get("/residents/me", { _silent: true }), // 静默处理未建档的 404
  create: (payload) => http.post("/residents", payload),
  createMe: (payload) => http.post("/residents/me", payload), // 居民自助建档
  updateMe: (payload) => http.put("/residents/me", payload), // 居民自助更新档案
  update: (id, payload) => http.put(`/residents/${id}`, payload),
  remove: (id) => http.delete(`/residents/${id}`),
};

export const measurementApi = {
  list: (residentId) => http.get("/measurements", { params: { residentId } }),
  me: () => http.get("/measurements/me", { _silent: true }), // 静默处理未建档的 404
  create: (payload) => http.post("/measurements", payload),
  trend: (residentId) => http.get(`/measurements/trend/${residentId}`),
  trendMe: () => http.get("/measurements/trend/me", { _silent: true }) // 查看个人趋势图
};

export const alertApi = {
  list: (params) => http.get("/alerts", { params }),
  updateStatus: (id, status) => http.put(`/alerts/${id}/status`, null, { params: { status } })
};

export const medicalHistoryApi = {
  list: (residentId) => http.get("/medical-histories", { params: { residentId } }),
  me: () => http.get("/medical-histories/me", { _silent: true }), // 我的病史/过敏史
  create: (payload) => http.post("/medical-histories", payload),
  updateMe: (id, payload) => http.put(`/medical-histories/me/${id}`, payload),
  update: (id, payload) => http.put(`/medical-histories/${id}`, payload)
};

export const visitRecordApi = {
  list: (residentId) => http.get("/visits", { params: { residentId } }),
  create: (payload) => http.post("/visits", payload)
};

export const medicationRecordApi = {
  list: (residentId) => http.get("/medications", { params: { residentId } }),
  create: (payload) => http.post("/medications", payload)
};

export const vaccinationRecordApi = {
  list: (residentId) => http.get("/vaccinations", { params: { residentId } }),
  create: (payload) => http.post("/vaccinations", payload)
};

export const followUpApi = {
  plans: (paramsOrResidentId) => {
    if (paramsOrResidentId != null && typeof paramsOrResidentId === "object" && !Array.isArray(paramsOrResidentId)) {
      return http.get("/follow-ups/plans", { params: paramsOrResidentId });
    }
    return http.get("/follow-ups/plans", { params: { residentId: paramsOrResidentId } });
  },
  records: (params) => http.get("/follow-ups/records", { params }),
  createPlan: (payload) => http.post("/follow-ups/plans", payload),
  createRecord: (payload) => http.post("/follow-ups/records", payload)
};

export const userApi = {
  list: (params) => http.get("/users", { params }),
  update: (id, payload) => http.put(`/users/${id}`, payload),
  updateRole: (id, role) => http.put(`/users/${id}/role`, null, { params: { role } }),
  updateStatus: (id, enabled) => http.put(`/users/${id}/status`, null, { params: { enabled } }),
  resetPassword: (id, newPassword) => http.put(`/users/${id}/password`, newPassword),
  delete: (id) => http.delete(`/users/${id}`)
};

export const systemApi = {
  logs: () => http.get("/system/logs"),
  stats: () => http.get("/system/stats"),
  monitor: () => http.get("/system/monitor"),
  backups: () => http.get("/system/backups"),
  nextBackup: () => http.get("/system/backups/next-backup"),
  createBackup: () => http.post("/system/backups"),
  restoreBackup: (filePath) => http.post("/system/backups/restore", null, { params: { filePath } }),
  deleteBackup: (filePath) => http.delete("/system/backups/delete", { params: { filePath } })
};

export const knowledgeApi = {
  list: (params) => http.get("/knowledge", { params }),
  create: (payload) => http.post("/knowledge", payload)
};

export const assessmentApi = {
  list: (residentId) => http.get("/assessments", { params: { residentId } }),
  create: (payload) => http.post("/assessments", payload)
};

export const interventionApi = {
  list: (params) => http.get("/interventions", { params }),
  create: (payload) => http.post("/interventions", payload)
};

export const aiApi = {
  chat: (payload) => http.post("/ai/chat", payload)
};