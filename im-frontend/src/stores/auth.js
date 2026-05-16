import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(Number(localStorage.getItem('userId')) || null)
  const username = ref(localStorage.getItem('username') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')
  const avatarUrl = ref(localStorage.getItem('avatarUrl') || '')

  const isLoggedIn = computed(() => !!token.value)

  async function login(loginData) {
    const res = await authApi.login(loginData)
    if (res.success) {
      setUserInfo(res.data)
    }
    return res
  }

  async function register(registerData) {
    const res = await authApi.register(registerData)
    if (res.success) {
      setUserInfo(res.data)
    }
    return res
  }

  function setUserInfo(data) {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    nickname.value = data.nickname
    avatarUrl.value = data.avatarUrl || ''
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', data.userId)
    localStorage.setItem('username', data.username)
    localStorage.setItem('nickname', data.nickname)
    localStorage.setItem('avatarUrl', data.avatarUrl || '')
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch (e) {
      // 即使 HTTP 登出失败也清除本地状态
    }
    token.value = ''
    userId.value = null
    username.value = ''
    nickname.value = ''
    avatarUrl.value = ''
    localStorage.clear()
  }

  return { token, userId, username, nickname, avatarUrl, isLoggedIn, login, register, logout }
})
