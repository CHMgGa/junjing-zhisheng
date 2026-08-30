import { defineStore } from 'pinia'
import { login, register } from '../services/auth'

export const useSessionStore = defineStore('session', {
  state: () => ({
    token: localStorage.getItem('greenhouse_token') || '',
    profile: JSON.parse(localStorage.getItem('greenhouse_profile') || 'null')
  }),
  actions: {
    async signIn(payload) {
      const data = await login(payload)
      this.setSession(data)
    },
    async signUp(payload) {
      const data = await register(payload)
      this.setSession(data)
    },
    setSession(data) {
      this.token = data.token
      this.profile = {
        ...data.profile,
        role: data.profile?.role ? String(data.profile.role).trim().toUpperCase() : data.profile?.role
      }
      localStorage.setItem('greenhouse_token', data.token)
      localStorage.setItem('greenhouse_profile', JSON.stringify(this.profile))
    },
    signOut() {
      this.token = ''
      this.profile = null
      localStorage.removeItem('greenhouse_token')
      localStorage.removeItem('greenhouse_profile')
    }
  }
})
