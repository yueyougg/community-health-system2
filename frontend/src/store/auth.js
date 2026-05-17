import { defineStore } from "pinia";
import { authApi } from "../api/modules";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("token") || "",
    username: localStorage.getItem("username") || "",
    role: localStorage.getItem("role") || ""
  }),
  getters: {
    isLogin: (state) => !!state.token
  },
  actions: {
    async login(payload) {
      const res = await authApi.login(payload);
      const info = res.data;
      this.token = info.token;
      this.username = info.username;
      this.role = info.role;
      localStorage.setItem("token", info.token);
      localStorage.setItem("username", info.username);
      localStorage.setItem("role", info.role);
    },
    async register(payload) {
      await authApi.register(payload);
    },
    logout() {
      this.token = "";
      this.username = "";
      this.role = "";
      localStorage.removeItem("token");
      localStorage.removeItem("username");
      localStorage.removeItem("role");
    }
  }
});
