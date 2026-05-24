import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getToken, clearToken } from '@/utils/auth'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/store/modules/user'
import { message } from 'ant-design-vue'
import { i18n } from '@/i18n'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: 'login.title' }
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
        meta: { title: 'nav.dashboard', requiresAuth: true }
      },
      {
        path: 'graph/list',
        name: 'GraphList',
        component: () => import('@/views/graph/list.vue'),
        meta: { title: 'nav.graphList', requiresAuth: true }
      },
      {
        path: 'graph/ide',
        name: 'GraphIDESelector',
        component: () => import('@/views/graph/ide.vue'),
        meta: { title: 'nav.graphIDE', requiresAuth: true }
      },
      {
        path: 'graph/create',
        name: 'GraphCreate',
        component: () => import('@/views/graph/create.vue'),
        meta: { title: 'graph.createGraph', requiresAuth: true }
      },
      {
        path: 'data/import',
        name: 'DataImport',
        component: () => import('@/views/data/import.vue'),
        meta: { title: 'nav.dataImport', requiresAuth: true }
      },
      {
        path: 'data/export',
        name: 'DataExport',
        component: () => import('@/views/data/export.vue'),
        meta: { title: 'nav.dataExport', requiresAuth: true }
      },
      {
        path: 'data/classes',
        name: 'DataClasses',
        component: () => import('@/views/data/classes.vue'),
        meta: { title: 'nav.classManagement', requiresAuth: true }
      },
      {
        path: 'data/properties',
        name: 'DataProperties',
        component: () => import('@/views/data/properties.vue'),
        meta: { title: 'nav.propertyManagement', requiresAuth: true }
      },
      {
        path: 'data/constraints',
        name: 'DataConstraints',
        component: () => import('@/views/data/constraints.vue'),
        meta: { title: 'nav.constraintManagement', requiresAuth: true }
      },
      {
        path: 'data/entities',
        name: 'DataEntities',
        component: () => import('@/views/data/entities.vue'),
        meta: { title: 'nav.entityManagement', requiresAuth: true }
      },
      {
        path: 'data/community-episode',
        name: 'CommunityEpisode',
        component: () => import('@/views/data/community-episode.vue'),
        meta: { title: 'nav.communityEpisodeManagement', requiresAuth: true }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/index.vue'),
        meta: { title: 'nav.hybridSearch', requiresAuth: true }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: 'nav.userManagement', requiresAuth: true }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: 'nav.roleManagement', requiresAuth: true }
      },
      {
        path: 'system/menu',
        name: 'SystemMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: 'nav.menuManagement', requiresAuth: true }
      },
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: () => import('@/views/system/config/index.vue'),
        meta: { title: 'nav.systemConfig', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: 'app.personalCenter', requiresAuth: true }
      },
      {
        path: 'graph/temporal',
        name: 'GraphTemporal',
        component: () => import('@/views/graph/temporal.vue'),
        meta: { title: 'nav.temporalHistory', requiresAuth: true }
      },
      {
        path: 'data/episodes',
        name: 'DataEpisodes',
        component: () => import('@/views/data/episodes.vue'),
        meta: { title: 'nav.episodeManagement', requiresAuth: true }
      },
      {
        path: 'data/edges',
        name: 'DataEdges',
        component: () => import('@/views/data/edges.vue'),
        meta: { title: 'nav.edgeManagement', requiresAuth: true }
      },
      {
        path: 'data/communities',
        name: 'DataCommunities',
        component: () => import('@/views/data/communities.vue'),
        meta: { title: 'nav.communityDetection', requiresAuth: true }
      },
      {
        path: 'custom-instructions',
        name: 'CustomInstructions',
        component: () => import('@/views/custom-instructions/index.vue'),
        meta: { title: 'nav.customInstructions', requiresAuth: true }
      },
      {
        path: 'prompt',
        name: 'PromptManagement',
        component: () => import('@/views/prompt/index.vue'),
        meta: { title: 'nav.promptManagement', requiresAuth: true }
      },
      {
        path: 'legal-kg',
        name: 'LegalKnowledgeGraph',
        component: () => import('@/views/legal-kg/index.vue'),
        meta: { title: 'nav.legalKnowledgeGraph', requiresAuth: true }
      },
      {
        path: 'notification',
        name: 'Notification',
        component: () => import('@/views/notification/index.vue'),
        meta: { title: 'app.notification', requiresAuth: true }
      },
      {
        path: 'system/log',
        name: 'SystemLog',
        component: () => import('@/views/system/log/index.vue'),
        meta: { title: 'nav.operationLog', requiresAuth: true }
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('@/views/monitor/index.vue'),
        meta: { title: 'nav.systemMonitor', requiresAuth: true }
      },
      {
        path: 'graph/ide/:id',
        name: 'GraphIDE',
        component: () => import('@/views/graph/ide.vue'),
        meta: { title: 'nav.graphIDE', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/404/index.vue'),
    meta: { title: 'page404.title' }
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
          message.error(i18n.global.t('login.sessionExpired'))
        } else {
          message.error(i18n.global.t('login.authFailed'))
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
    const titleKey = to.meta.title as string
    const translated = i18n.global.t(titleKey)
    document.title = `${translated} - Graphiti Console`
  }

  return true
})

export default router
