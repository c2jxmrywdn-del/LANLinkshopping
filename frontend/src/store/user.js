import { defineStore } from 'pinia'
import { authApi } from '../api'

export const useUserStore = defineStore('user', {
  state: () => ({ user: null }),
  getters: {
    logged: (s) => !!s.user,
    isMerchant: (s) => s.user && s.user.roleCode === 'merchant',
    isAdmin: (s) => s.user && s.user.roleCode === 'admin'
  },
  actions: {
    /**
     * 登录：密码通过后若服务端要求两步验证（require2fa），不建立会话、不写 ll_user，
     * 返回原始数据（含 loginTicket），由登录页引导输入动态码后调 login2fa。
     */
    async login(payload) {
      const data = await authApi.login(payload)
      if (data && data.require2fa) return data
      this.user = data
      localStorage.setItem('ll_user', JSON.stringify(data))
      return data
    },
    /** 两步验证登录：校验动态码成功后建立会话 */
    async login2fa(loginTicket, code) {
      this.user = await authApi.login2fa({ loginTicket, code })
      localStorage.setItem('ll_user', JSON.stringify(this.user))
    },
    async register(payload) {
      await authApi.register(payload)
    },
    async fetchMe() {
      try {
        this.user = await authApi.me()
        localStorage.setItem('ll_user', JSON.stringify(this.user))
      } catch (e) { this.user = null }
    },
    async logout() {
      try { await authApi.logout() } catch (e) {}
      this.user = null
      localStorage.removeItem('ll_user')
    }
  }
})