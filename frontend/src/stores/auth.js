import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import authApi from '@/api/auth'

// 认证状态管理：token 持久化到 localStorage，用户信息缓存在内存
export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('wms_token') || '')
  const user = ref(JSON.parse(localStorage.getItem('wms_user') || 'null'))

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const displayName = computed(() => user.value?.displayName || user.value?.username || '')

  /** 登录：调用后端 /api/auth/login，成功后保存 token 和用户信息 */
  async function login(username, password) {
    const res = await authApi.login(username, password)
    token.value = res.token
    user.value = {
      userId: res.userId,
      username: res.username,
      displayName: res.displayName,
      role: res.role
    }
    localStorage.setItem('wms_token', token.value)
    localStorage.setItem('wms_user', JSON.stringify(user.value))
    return res
  }

  /** 刷新当前用户信息：调用 /api/auth/me */
  async function fetchMe() {
    try {
      const res = await authApi.me()
      user.value = {
        userId: res.id,
        username: res.username,
        displayName: res.displayName,
        role: res.role
      }
      localStorage.setItem('wms_user', JSON.stringify(user.value))
    } catch {
      // token 无效则清除
      logout()
    }
  }

  /** 退出登录：清除 token 和用户信息 */
  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('wms_token')
    localStorage.removeItem('wms_user')
  }

  return { token, user, isLoggedIn, isAdmin, displayName, login, fetchMe, logout }
})
