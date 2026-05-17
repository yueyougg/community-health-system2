import axios from "axios";
import { ElMessage } from "element-plus";
import router from "../router";

const http = axios.create({
  baseURL: "/api",
  timeout: 15000 // 适当增加超时时间
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => {
    // 统一处理后端返回的 success 标志 (依据接口文档 I)
    const res = response.data;
    if (res.success === false) {
      // 登录相关的错误不在拦截器中显示，由登录组件自己处理
      if (!response.config.url.includes('/auth/login')) {
        ElMessage.error(res.message || "操作失败");
      }
      return Promise.reject(new Error(res.message || "操作失败"));
    }
    return res;
  },
  (error) => {
    // 如果请求配置了 _silent 标志，则不弹出错误提示
    if (error.config && error.config._silent) {
      return Promise.reject(error);
    }

    let message = "服务器连接失败";
    
    if (error.response) {
      const { status, data } = error.response;
      message = data.message || "请求出错";
      
      // 登录失效处理 (401)
      if (status === 401) {
        localStorage.removeItem("token");
        localStorage.removeItem("auth");
        router.push("/login");
        message = "登录已过期，请重新登录";
      } else if (status === 403) {
        message = "权限不足，拒绝访问";
      } else if (status === 404) {
        message = "请求的资源不存在";
      } else if (status === 500) {
        message = "后端服务异常";
      }
    } else if (error.request) {
      message = "未收到服务器响应，请检查网络或后端状态";
    }

    // 登录、注册和用户管理相关的错误不在拦截器中显示，由对应组件自己处理
    const errorUrl = error.config?.url || '';
    const isAuthError = errorUrl.includes('/auth/login') || errorUrl.includes('/auth/register');
    const isUserManagementError = errorUrl.includes('/users');
    
    if (!isAuthError && !isUserManagementError) {
      ElMessage.error(message);
    }
    return Promise.reject(new Error(message));
  }
);

export default http;
