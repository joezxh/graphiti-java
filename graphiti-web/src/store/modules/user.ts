import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api/auth'
import { getToken, setToken, clearToken, getUser, type LoginResult } from '@/utils/auth'
import { message } from 'ant-design-vue'

export interface UserState {
  token: string | null
  userInfo: LoginResult['user'] | null
}

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string | null>(getToken())
  const userInfo = ref<LoginResult['user'] | null>(getUser())

  // Getters
  const isLoggedIn = () => !!token.value

  // Actions
  const login = async (username: string, password: string) => {
    try {
      const result = await authApi.login({ username, password })
      setToken(result)
      token.value = result.token
      userInfo.value = result.user
      message.success('登录成功')
      return result
    } catch (error: any) {
      message.error(error.message || '登录失败')
      throw error
    }
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } finally {
      clearToken()
      token.value = null
      userInfo.value = null
      message.success('已登出')
    }
  }

  const fetchUserInfo = async () => {
    if (!token.value) return
    try {
      const info = await authApi.getInfo()
      userInfo.value = info
    } catch (error) {
      console.error('获取用户信息失败', error)
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    logout,
    fetchUserInfo
  }
})

export default useUserStore
