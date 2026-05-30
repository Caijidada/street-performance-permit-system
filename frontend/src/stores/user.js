import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role >= 2)
  const isArtist = computed(() => userInfo.value?.role === 1)

  async function login(data) {
    const res = await authApi.login(data)
    token.value = res.token
    userInfo.value = res
    localStorage.setItem('token', res.token)
    localStorage.setItem('userInfo', JSON.stringify(res))
    return res
  }

  async function logout() {
    try { await authApi.logout() } catch {}
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function refreshUserInfo() {
    const info = await authApi.getUserInfo()
    userInfo.value = { ...userInfo.value, ...info }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  return { token, userInfo, isLoggedIn, isAdmin, isArtist, login, logout, refreshUserInfo }
})
