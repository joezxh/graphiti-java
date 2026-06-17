/**
 * 权限指令和组合式函数
 * 用于组件级别的权限控制
 */
import type { Directive } from 'vue'
import { usePermissionStore } from '@/store/modules/permission'

/**
 * v-permission 指令
 * 用法: v-permission="'system:user:list'"
 *      v-permission="['system:user:list', 'system:user:create']"
 */
export const vPermission: Directive = {
  mounted(el: HTMLElement, binding) {
    const permissionStore = usePermissionStore()
    const value = binding.value

    const checkPermission = () => {
      if (!value) return true

      if (Array.isArray(value)) {
        return permissionStore.hasAnyPermission(value)
      }

      return permissionStore.hasPermission(value)
    }

    if (!checkPermission()) {
      el.parentNode?.removeChild(el)
    }
  }
}

/**
 * 权限检查组合式函数
 */
export function usePermission() {
  const permissionStore = usePermissionStore()

  const hasPermission = (permission: string) => {
    return permissionStore.hasPermission(permission)
  }

  const hasAnyPermission = (permissions: string[]) => {
    return permissionStore.hasAnyPermission(permissions)
  }

  const hasMenuPermission = (path: string) => {
    return permissionStore.hasMenuPermission(path)
  }

  return {
    hasPermission,
    hasAnyPermission,
    hasMenuPermission,
    permissions: permissionStore.permissions,
    menuList: permissionStore.menuList
  }
}

export default {
  vPermission,
  usePermission
}
