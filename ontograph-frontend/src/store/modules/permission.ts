import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { MenuItem } from '@/api/menu'
import { menuApi, mapMenuDO } from '@/api/menu'

export interface PermissionState {
  menuList: MenuItem[]
  permissions: string[]
  isMenuLoaded: boolean
}

export const usePermissionStore = defineStore('permission', () => {
  // State
  const menuList = ref<MenuItem[]>([])
  const permissions = ref<string[]>([])
  const isMenuLoaded = ref(false)

  // Getters
  const hasMenuPermission = computed(() => (path: string) => {
    return menuList.value.some(menu => menu.path === path)
  })

  const hasPermission = computed(() => (permission: string) => {
    return permissions.value.includes(permission)
  })

  const hasAnyPermission = computed(() => (permissionList: string[]) => {
    return permissionList.some(p => permissions.value.includes(p))
  })

  // 将菜单树扁平化为路径数组
  const flatMenuPaths = computed(() => {
    const paths: string[] = []
    function traverse(menus: MenuItem[]) {
      for (const menu of menus) {
        if (menu.path && menu.status === 1) {
          paths.push(menu.path)
        }
        if (menu.children?.length) {
          traverse(menu.children)
        }
      }
    }
    traverse(menuList.value)
    return paths
  })

  // 将菜单树扁平化为权限数组
  const flatMenuPermissions = computed(() => {
    const perms: string[] = []
    function traverse(menus: MenuItem[]) {
      for (const menu of menus) {
        if (menu.permission && menu.status === 1) {
          perms.push(menu.permission)
        }
        if (menu.children?.length) {
          traverse(menu.children)
        }
      }
    }
    traverse(menuList.value)
    return perms
  })

  // 获取顶级菜单（用于侧边栏分组）
  const topLevelMenus = computed(() => {
    return menuList.value.filter(m => m.parentId === 0 || m.parentId === null)
      .sort((a, b) => a.sort - b.sort)
  })

  // Actions
  const setMenuList = (menus: MenuItem[]) => {
    // 规范化菜单数据（确保 url→path 等字段映射，以及递归 children）
    menuList.value = menus.map(mapMenuDO)
    isMenuLoaded.value = true
    // 从菜单中提取权限标识
    permissions.value = flatMenuPermissions.value
  }

  const setPermissions = (perms: string[]) => {
    permissions.value = perms
  }

  const fetchUserMenus = async () => {
    try {
      // 调用后端获取用户有权限的菜单
      const menus = await menuApi.getMenus()
      setMenuList(menus)
      return menus
    } catch (error) {
      console.error('获取用户菜单失败', error)
      throw error
    }
  }

  const clearPermissions = () => {
    menuList.value = []
    permissions.value = []
    isMenuLoaded.value = false
  }

  return {
    // State
    menuList,
    permissions,
    isMenuLoaded,
    // Getters
    hasMenuPermission,
    hasPermission,
    hasAnyPermission,
    flatMenuPaths,
    flatMenuPermissions,
    topLevelMenus,
    // Actions
    setMenuList,
    setPermissions,
    fetchUserMenus,
    clearPermissions
  }
})

export default usePermissionStore
