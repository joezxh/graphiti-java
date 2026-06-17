import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api/auth'
import { getToken, setToken, clearToken, getUser, type LoginResult } from '@/utils/auth'
import { usePermissionStore } from './permission'
import { message } from 'ant-design-vue'
import router from '@/router'
import { generateRoutesFromMenus } from '@/utils/permission'

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
      // 清除权限信息
      const permissionStore = usePermissionStore()
      permissionStore.clearPermissions()
      // 重置路由到静态路由
      resetRoutes()
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

  /**
   * 获取用户菜单并动态注册路由
   */
  const fetchUserMenus = async () => {
    if (!token.value) return []

    const permissionStore = usePermissionStore()

    try {
      const menus = await authApi.getMenus()
      permissionStore.setMenuList(menus)

      // 动态生成并注册路由
      const dynamicRoutes = generateRoutesFromMenus(menus)
      registerDynamicRoutes(dynamicRoutes)

      return menus
    } catch (error) {
      console.error('获取用户菜单失败', error)
      throw error
    }
  }

  /**
   * 注册动态路由
   */
  const registerDynamicRoutes = (routes: any[]) => {
    if (!routes.length) return

    // 获取当前已注册的动态路由（避免重复注册）
    const existingPaths = new Set(
      router.getRoutes()
        .filter(r => r.meta?.menuId)
        .map(r => r.path)
    )

    // 只注册新的路由
    const newRoutes = routes.filter(r => !existingPaths.has(r.path))
    if (newRoutes.length) {
      newRoutes.forEach(route => {
        router.addRoute('Layout', route)
      })
      console.log('[UserStore] 动态注册路由:', newRoutes.map(r => r.path))
    }
  }

  /**
   * 重置路由到初始状态
   */
  const resetRoutes = () => {
    // 移除所有动态添加的路由
    const routes = router.getRoutes()
    routes.forEach(route => {
      if (route.meta?.menuId) {
        router.removeRoute(route.name as string)
      }
    })
    console.log('[UserStore] 已重置动态路由')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    logout,
    fetchUserInfo,
    fetchUserMenus,
    registerDynamicRoutes,
    resetRoutes
  }
})

export default useUserStore
