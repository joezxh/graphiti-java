/**
 * 权限工具函数
 * 提供路由守卫和组件级别权限控制
 */
import type { RouteRecordRaw } from 'vue-router'
import { usePermissionStore } from '@/store/modules/permission'

/**
 * 路由映射配置
 * 将后端返回的菜单 path 映射到前端组件
 */
export const routeMapping: Record<string, () => Promise<any>> = {
  '/dashboard': () => import('@/views/dashboard/index.vue'),
  '/graph/list': () => import('@/views/graph/list.vue'),
  '/graph/ide': () => import('@/views/graph/ide.vue'),
  '/graph/create': () => import('@/views/graph/create.vue'),
  '/graph/temporal': () => import('@/views/graph/temporal.vue'),
  '/data/classes': () => import('@/views/data/classes.vue'),
  '/data/properties': () => import('@/views/data/properties.vue'),
  '/data/constraints': () => import('@/views/data/constraints.vue'),
  '/data/entities': () => import('@/views/data/entities.vue'),
  '/data/edges': () => import('@/views/data/edges.vue'),
  '/data/communities': () => import('@/views/data/communities.vue'),
  '/data/community-episode': () => import('@/views/data/community-episode.vue'),
  '/data/episodes': () => import('@/views/data/episodes.vue'),
  '/data/import': () => import('@/views/data/import.vue'),
  '/data/export': () => import('@/views/data/export.vue'),
  '/legal-kg': () => import('@/views/legal-kg/index.vue'),
  '/search': () => import('@/views/search/index.vue'),
  '/custom-instructions': () => import('@/views/custom-instructions/index.vue'),
  '/prompt': () => import('@/views/prompt/index.vue'),
  '/system/user': () => import('@/views/system/user/index.vue'),
  '/system/role': () => import('@/views/system/role/index.vue'),
  '/system/menu': () => import('@/views/system/menu/index.vue'),
  '/system/config': () => import('@/views/system/config/index.vue'),
  '/system/log': () => import('@/views/system/log/index.vue'),
  '/monitor': () => import('@/views/monitor/index.vue'),
  '/profile': () => import('@/views/profile/index.vue'),
  '/notification': () => import('@/views/notification/index.vue')
}

/**
 * 菜单路径对应的标题 i18n key
 */
export const routeTitleMapping: Record<string, string> = {
  '/dashboard': 'nav.dashboard',
  '/graph/list': 'nav.graphList',
  '/graph/ide': 'nav.graphIDE',
  '/graph/create': 'graph.createGraph',
  '/graph/temporal': 'nav.temporalHistory',
  '/data/classes': 'nav.classManagement',
  '/data/properties': 'nav.propertyManagement',
  '/data/constraints': 'nav.constraintManagement',
  '/data/entities': 'nav.entityManagement',
  '/data/edges': 'nav.edgeManagement',
  '/data/communities': 'nav.communityDetection',
  '/data/community-episode': 'nav.communityEpisodeManagement',
  '/data/episodes': 'nav.episodeManagement',
  '/data/import': 'nav.dataImport',
  '/data/export': 'nav.dataExport',
  '/legal-kg': 'nav.legalKnowledgeGraph',
  '/search': 'nav.hybridSearch',
  '/custom-instructions': 'nav.customInstructions',
  '/prompt': 'nav.promptManagement',
  '/system/user': 'nav.userManagement',
  '/system/role': 'nav.roleManagement',
  '/system/menu': 'nav.menuManagement',
  '/system/config': 'nav.systemConfig',
  '/system/log': 'nav.operationLog',
  '/monitor': 'nav.systemMonitor',
  '/profile': 'app.personalCenter',
  '/notification': 'app.notification'
}

/**
 * 根据后端菜单数据生成动态路由
 * @param menus 菜单树
 * @returns Vue Router 路由记录数组
 */
export function generateRoutesFromMenus(menus: any[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  function traverse(menuList: any[]) {
    for (const menu of menuList) {
      // 只处理类型为"菜单"的节点（type=2）
      if (menu.type === 2 && menu.url && menu.status === 1) {
        const path = menu.url
        const componentLoader = routeMapping[path]

        if (componentLoader) {
          routes.push({
            path,
            name: path.replace(/\//g, '_').replace(/^_/, ''),
            component: componentLoader,
            meta: {
              title: routeTitleMapping[path] || menu.name,
              requiresAuth: true,
              menuId: menu.id,
              permission: menu.permission
            }
          })
        }
      }

      // 递归处理子菜单
      if (menu.children?.length) {
        traverse(menu.children)
      }
    }
  }

  traverse(menus)
  return routes
}

/**
 * 检查用户是否有权限访问指定路径
 * @param path 菜单路径
 */
export function hasAccess(path: string): boolean {
  const permissionStore = usePermissionStore()
  return permissionStore.hasMenuPermission(path)
}

/**
 * 检查是否拥有指定权限标识
 * @param permission 权限标识
 */
export function hasPermission(permission: string): boolean {
  const permissionStore = usePermissionStore()
  return permissionStore.hasPermission(permission)
}

/**
 * 检查是否拥有任意一个指定权限
 * @param permissions 权限标识数组
 */
export function hasAnyPermission(permissions: string[]): boolean {
  const permissionStore = usePermissionStore()
  return permissionStore.hasAnyPermission(permissions)
}
