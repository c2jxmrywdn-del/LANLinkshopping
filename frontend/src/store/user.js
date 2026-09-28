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
    async login(payload) {
      this.user = await authApi.login(payload)
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
