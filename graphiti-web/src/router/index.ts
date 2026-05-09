import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken, clearToken } from '@/utils/auth'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/store/modules/user'
import { message } from 'ant-design-vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/components/Layout/BasicLayout.vue'),
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', requiresAuth: true }
      },
      {
        path: 'graph/list',
        name: 'GraphList',
        component: () => import('@/views/graph/list.vue'),
        meta: { title: '图谱列表', requiresAuth: true }
      },
      {
        path: 'graph/create',
        name: 'GraphCreate',
        component: () => import('@/views/graph/create.vue'),
        meta: { title: '创建图谱', requiresAuth: true }
      },
      {
        path: 'graph/detail/:id',
        name: 'GraphDetail',
        component: () => import('@/views/graph/detail.vue'),
        meta: { title: '图谱详情', requiresAuth: true }
      },
      {
        path: 'ontology',
        name: 'Ontology',
        component: () => import('@/views/ontology/index.vue'),
        meta: { title: '本体配置', requiresAuth: true }
      },
      {
        path: 'data/import',
        name: 'DataImport',
        component: () => import('@/views/data/import.vue'),
        meta: { title: '数据导入', requiresAuth: true }
      },
      {
        path: 'data/export',
        name: 'DataExport',
        component: () => import('@/views/data/export.vue'),
        meta: { title: '数据导出', requiresAuth: true }
      },
      {
        path: 'data/entities',
        name: 'DataEntities',
        component: () => import('@/views/data/entities.vue'),
        meta: { title: '实体管理', requiresAuth: true }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/index.vue'),
        meta: { title: '混合检索', requiresAuth: true }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', requiresAuth: true }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', requiresAuth: true }
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', requiresAuth: true }
      },
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: '系统配置', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'notification',
        name: 'Notification',
        component: () => import('@/views/notification/index.vue'),
        meta: { title: '通知中心', requiresAuth: true }
      },
      {
        path: 'system/log',
        name: 'SystemLog',
        component: () => import('@/views/system/log/index.vue'),
        meta: { title: '操作日志', requiresAuth: true }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/index.vue'),
        meta: { title: '系统监控', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/404/index.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// 路由守卫
let lastAuthCheck = 0
const AUTH_CHECK_INTERVAL = 60000 // 1分钟

router.beforeEach(async (to, _from) => {
  const requiresAuth = to.meta.requiresAuth === true
  const token = getToken()

  // 需要认证但未登录
  if (requiresAuth && !token) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  // 需要认证且已登录，验证 token 有效性（带缓存）
  if (requiresAuth && token) {
    const now = Date.now()
    if (now - lastAuthCheck > AUTH_CHECK_INTERVAL) {
      try {
        await authApi.getInfo()
        lastAuthCheck = now
      } catch (error: any) {
        // Token 无效，清除并跳转登录页
        clearToken()
        const userStore = useUserStore()
        userStore.logout()
        
        // 根据错误类型处理
        if (error.response?.status === 401) {
          message.error('登录已过期，请重新登录')
        } else {
          message.error('认证失败，请重新登录')
        }
        
        return { name: 'Login', query: { redirect: to.fullPath } }
      }
    }
  }

  // 已登录用户访问登录页，跳转仪表盘
  if (to.name === 'Login' && token) {
    return { name: 'Dashboard' }
  }

  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - Graphiti Console`
  }

  return true
})

export default router
