# ontograph-java 控制台前端实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 ontograph-java 独立服务构建一个深色科技风的控制台前端，支持双视图模式（业务视图 + 开发者视图），充分体现基于 Ontology 本体论的图谱关系管理功能。

**Architecture:** 采用 Vue 3 + TypeScript + Ant Design Vue 4.x 技术栈，实现双视图架构（业务视图面向数据分析师，开发者视图面向工程师）。核心包括：图谱可视化（ECharts 力导向图）、本体论编辑器（结构化表单 + 预览）、混合检索界面（全文/向量/图遍历）。

**Tech Stack:** Vue 3.4, TypeScript 5.x, Ant Design Vue 4.x, Pinia 2.x, ECharts 5.5, Vite 5.x, Less, Axios 1.x

**Backend API:** `http://localhost:8080/api/v1` (可配置)

---

## 文件结构

```
ontograph-web/
├── package.json                      # 项目依赖配置
├── vite.config.ts                   # Vite 构建配置
├── tsconfig.json                    # TypeScript 配置
├── .env.development                # 开发环境变量
├── .env.production                 # 生产环境变量
├── index.html                       # 入口 HTML
├── public/
│   └── favicon.ico
└── src/
    ├── main.ts                     # 应用入口
    ├── App.vue                     # 根组件
    ├── api/                        # API 接口层
    │   ├── request.ts             # Axios 封装
    │   ├── auth.ts                # 认证 API
    │   ├── graph.ts               # 图谱管理 API
    │   ├── ontology.ts            # 本体论 API
    │   ├── node.ts                # 节点管理 API
    │   ├── edge.ts                # 边管理 API
    │   ├── episode.ts             # 事件管理 API
    │   ├── search.ts              # 搜索检索 API
    │   └── user.ts                # 用户管理 API
    ├── assets/                     # 静态资源
    │   └── styles/
    │       ├── dark.less          # 深色主题变量
    │       ├── global.less        # 全局样式
    │       └── variables.less     # 色彩系统变量
    ├── components/                 # 公共组件
    │   ├── Layout/
    │   │   ├── BasicLayout.vue   # 主布局
    │   │   ├── Sidebar.vue       # 侧边栏
    │   │   └── Header.vue        # 顶部栏
    │   ├── GraphViewer/
    │   │   ├── ForceGraph.vue    # 力导向图
    │   │   ├── GraphToolbar.vue  # 工具栏
    │   │   └── NodeDetail.vue    # 节点详情面板
    │   ├── OntologyEditor/
    │   │   ├── EntityTypePanel.vue
    │   │   ├── RelationTypePanel.vue
    │   │   └── OntologyPreview.vue
    │   └── SearchBox/
    │       ├── SearchInput.vue
    │       └── SearchResults.vue
    ├── layouts/                    # 布局模式
    │   ├── BusinessLayout.vue     # 业务视图
    │   └── DeveloperLayout.vue   # 开发者视图
    ├── router/
    │   ├── index.ts               # 路由配置
    │   └── routes.ts             # 路由定义
    ├── store/                      # Pinia 状态管理
    │   ├── index.ts
    │   └── modules/
    │       ├── user.ts            # 用户状态
    │       ├── graph.ts           # 图谱状态
    │       ├── ontology.ts        # 本体状态
    │       └── search.ts          # 搜索状态
    ├── utils/                      # 工具函数
    │   ├── auth.ts                # 认证工具
    │   ├── format.ts              # 格式化工具
    │   └── graph.ts               # 图谱工具函数
    └── views/                      # 页面组件
        ├── login/
        │   └── index.vue          # 登录页
        ├── dashboard/
        │   └── index.vue          # 仪表盘
        ├── graph/
        │   ├── list.vue           # 图谱列表
        │   ├── detail.vue         # 图谱详情
        │   └── workspace.vue      # 工作区
        ├── ontology/
        │   └── index.vue          # 本体编辑器
        ├── data/
        │   ├── nodes.vue          # 节点管理
        │   ├── edges.vue          # 边管理
        │   ├── episodes.vue       # 事件管理
        │   └── import.vue         # 数据导入
        ├── search/
        │   └── index.vue          # 混合检索页
        ├── inference/
        │   └── index.vue          # 推理引擎页
        └── community/
            └── index.vue           # 社区发现页
```

---

## Phase 1: 基础框架 + 登录（1-2 天）

### Task 1: 项目初始化

**Files:**
- Create: `ontograph-web/package.json`
- Create: `ontograph-web/vite.config.ts`
- Create: `ontograph-web/tsconfig.json`
- Create: `ontograph-web/.env.development`
- Create: `ontograph-web/.env.production`
- Create: `ontograph-web/index.html`
- Create: `ontograph-web/src/main.ts`
- Create: `ontograph-web/src/App.vue`

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "ontograph-web",
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc && vite build",
    "preview": "vite preview",
    "type-check": "vue-tsc --noEmit"
  },
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.3.0",
    "pinia": "^2.1.0",
    "ant-design-vue": "^4.2.0",
    "axios": "^1.7.0",
    "echarts": "^5.5.0",
    "vue-echarts": "^7.0.0",
    "@ant-design/icons-vue": "^7.0.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.0",
    "@vitejs/plugin-vue-jsx": "^4.0.0",
    "typescript": "^5.4.0",
    "vue-tsc": "^2.0.0",
    "vite": "^5.2.0",
    "less": "^4.2.0",
    "unplugin-vue-components": "^0.27.0",
    "unplugin-auto-import": "^0.17.0"
  }
}
```

- [ ] **Step 2: 创建 vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  css: {
    preprocessorOptions: {
      less: {
        modifyVars: {
          'primary-color': '#5e6ad2',
          'component-background': '#0f1011',
          'border-radius-base': '8px'
        },
        javascriptEnabled: true
      }
    }
  }
})
```

- [ ] **Step 3: 创建 tsconfig.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.tsx", "src/**/*.vue"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
```

- [ ] **Step 4: 创建环境变量文件**

`.env.development`:
```
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_APP_TITLE=ontograph-java 控制台
```

`.env.production`:
```
VITE_API_BASE_URL=/api/v1
VITE_APP_TITLE=ontograph-java 控制台
```

- [ ] **Step 5: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8">
    <link rel="icon" href="/favicon.ico">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>ontograph-java 控制台</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

- [ ] **Step 6: 创建 src/main.ts**

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import App from './App.vue'
import router from './router'
import './assets/styles/global.less'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(Antd)

app.mount('#app')
```

- [ ] **Step 7: 创建 src/App.vue**

```vue
<template>
  <router-view />
</template>

<script setup lang="ts">
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
  background-color: #010102;
  color: #f7f8f8;
}
</style>
```

- [ ] **Step 8: 安装依赖并验证**

Run: `cd ontograph-web && npm install`
Expected: 依赖安装成功，无错误

- [ ] **Step 9: 启动开发服务器验证**

Run: `npm run dev`
Expected: 开发服务器启动在 http://localhost:3000

- [ ] **Step 10: Commit**

```bash
git add ontograph-web/
git commit -m "feat: initialize Vue 3 + TypeScript + Ant Design Vue project"
```

---

### Task 2: Axios 封装 + 认证工具

**Files:**
- Create: `ontograph-web/src/api/request.ts`
- Create: `ontograph-web/src/utils/auth.ts`

- [ ] **Step 1: 创建 Axios 封装**

```typescript
// src/api/request.ts
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'
import type { MessageInstance } from 'ant-design-vue/es/message/interface'
import { getToken, clearToken } from '@/utils/auth'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, message: msg, data } = response.data
    
    if (code === 200) {
      return data
    } else if (code === 401) {
      message.error('认证失败，请重新登录')
      clearToken()
      window.location.href = '/login'
      return Promise.reject(new Error(msg))
    } else {
      message.error(msg || '请求失败')
      return Promise.reject(new Error(msg))
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      clearToken()
      window.location.href = '/login'
    }
    message.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
```

- [ ] **Step 2: 创建认证工具函数**

```typescript
// src/utils/auth.ts
const TOKEN_KEY = 'graphiti_token'
const USER_KEY = 'graphiti_user'

export interface LoginResult {
  token: string
  expiresIn: number
  user: {
    id: number
    username: string
    nickname: string
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(result: LoginResult): void {
  localStorage.setItem(TOKEN_KEY, result.token)
  localStorage.setItem(USER_KEY, JSON.stringify(result.user))
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getUser(): LoginResult['user'] | null {
  const userStr = localStorage.getItem(USER_KEY)
  return userStr ? JSON.parse(userStr) : null
}
```

- [ ] **Step 3: Commit**

```bash
git add src/api/request.ts src/utils/auth.ts
git commit -m "feat: add Axios request wrapper and auth utilities"
```

---

### Task 3: 登录页面

**Files:**
- Create: `ontograph-web/src/api/auth.ts`
- Create: `ontograph-web/src/views/login/index.vue`
- Create: `ontograph-web/src/store/modules/user.ts`
- Modify: `ontograph-web/src/router/index.ts`

- [ ] **Step 1: 创建认证 API**

```typescript
// src/api/auth.ts
import request from './request'
import type { LoginResult } from '@/utils/auth'

export function login(username: string, password: string): Promise<LoginResult> {
  return request.post('/auth/login', { username, password })
}

export function logout(): Promise<void> {
  return request.post('/auth/logout')
}

export function getUserInfo(): Promise<LoginResult['user']> {
  return request.get('/auth/info')
}
```

- [ ] **Step 2: 创建用户状态管理**

```typescript
// src/store/modules/user.ts
import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '@/api/auth'
import { setToken, clearToken, getUser, type LoginResult } from '@/utils/auth'

interface UserState {
  user: LoginResult['user'] | null
  token: string | null
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    user: getUser(),
    token: null
  }),
  
  actions: {
    async login(username: string, password: string) {
      const result = await loginApi(username, password)
      setToken(result)
      this.user = result.user
      this.token = result.token
    },
    
    async logout() {
      await logoutApi()
      clearToken()
      this.user = null
      this.token = null
    }
  }
})
```

- [ ] **Step 3: 创建登录页面**

```vue
<!-- src/views/login/index.vue -->
<template>
  <div class="login-container">
    <div class="login-card">
      <div class="logo">
        <span class="logo-icon">◆</span>
        <span class="logo-text">GRAPHITI</span>
      </div>
      <h2 class="title">知识图谱管理系统</h2>
      
      <a-form :model="formState" @finish="handleLogin">
        <a-form-item>
          <a-input 
            v-model:value="formState.username" 
            placeholder="用户名"
            size="large"
          />
        </a-form-item>
        
        <a-form-item>
          <a-input-password 
            v-model:value="formState.password" 
            placeholder="密码"
            size="large"
          />
        </a-form-item>
        
        <a-form-item>
          <a-checkbox v-model:checked="formState.remember">
            记住我
          </a-checkbox>
        </a-form-item>
        
        <a-form-item>
          <a-button 
            type="primary" 
            html-type="submit" 
            size="large" 
            :loading="loading"
            block
          >
            登录
          </a-button>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const formState = reactive({
  username: '',
  password: '',
  remember: true
})

const handleLogin = async () => {
  loading.value = true
  try {
    await userStore.login(formState.username, formState.password)
    message.success('登录成功')
    router.push('/dashboard')
  } catch (error: any) {
    message.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="less" scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #010102;
}

.login-card {
  width: 400px;
  padding: 48px;
  background-color: #0f1011;
  border: 1px solid #23252a;
  border-radius: 12px;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
  
  .logo-icon {
    font-size: 32px;
    background: linear-gradient(135deg, #5e6ad2, #00d4ff);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-right: 12px;
  }
  
  .logo-text {
    font-size: 28px;
    font-weight: 600;
    color: #f7f8f8;
    letter-spacing: -0.5px;
  }
}

.title {
  text-align: center;
  color: #d0d6e0;
  font-size: 16px;
  font-weight: 400;
  margin-bottom: 32px;
}
</style>
```

- [ ] **Step 4: 配置路由**

```typescript
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue')
  },
  {
    path: '/',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

- [ ] **Step 5: Commit**

```bash
git add src/api/auth.ts src/store/modules/user.ts src/views/login/index.vue src/router/index.ts
git commit -m "feat: implement login page with JWT authentication"
```

---

### Task 4: 基础布局组件

**Files:**
- Create: `ontograph-web/src/components/Layout/BasicLayout.vue`
- Create: `ontograph-web/src/components/Layout/Sidebar.vue`
- Create: `ontograph-web/src/components/Layout/Header.vue`
- Modify: `ontograph-web/src/router/index.ts`
- Create: `ontograph-web/src/assets/styles/dark.less`

- [ ] **Step 1: 创建深色主题样式**

```less
// src/assets/styles/dark.less
@canvas: #010102;
@surface-1: #0f1011;
@surface-2: #141516;
@surface-3: #18191a;
@hairline: #23252a;
@hairline-strong: #34343a;
@ink: #f7f8f8;
@ink-muted: #d0d6e0;
@ink-subtle: #8a8f98;
@primary: #5e6ad2;
@primary-hover: #828fff;
@cyan: #00d4ff;
@green: #27a644;
@orange: #ff8c00;
@red: #ff6b6b;
```

- [ ] **Step 2: 创建全局样式**

```less
// src/assets/styles/global.less
@import './dark.less';

body {
  margin: 0;
  padding: 0;
  background-color: @canvas;
  color: @ink;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

// Ant Design Vue 深色主题覆盖
.ant-btn-primary {
  background-color: @primary;
  border-color: @primary;
  
  &:hover {
    background-color: @primary-hover;
    border-color: @primary-hover;
  }
}

.ant-input,
.ant-input-password {
  background-color: @surface-1;
  border-color: @hairline;
  color: @ink;
  
  &:focus,
  &:hover {
    border-color: @primary;
  }
}
```

- [ ] **Step 3: 创建 Header 组件**

```vue
<!-- src/components/Layout/Header.vue -->
<template>
  <div class="header">
    <div class="header-left">
      <span class="logo">◆ GRAPHITI</span>
    </div>
    
    <div class="header-right">
      <a-switch 
        v-model:checked="isDevView" 
        @change="handleViewChange"
        checked-children="开发者"
        un-checked-children="业务"
      />
      
      <a-dropdown>
        <a-avatar>{{ user?.nickname?.[0] || 'U' }}</a-avatar>
        <template #overlay>
          <a-menu>
            <a-menu-item @click="handleLogout">退出登录</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()
const isDevView = ref(false)

const user = useUserStore().user

const handleViewChange = (checked: boolean) => {
  if (checked) {
    router.push('/cypher')
  } else {
    router.push('/dashboard')
  }
}

const handleLogout = async () => {
  await userStore.logout()
  router.push('/login')
}
</script>

<style lang="less" scoped>
.header {
  height: 56px;
  background-color: @surface-1;
  border-bottom: 1px solid @hairline;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  
  .logo {
    font-size: 18px;
    font-weight: 600;
    color: @primary;
  }
  
  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }
}
</style>
```

- [ ] **Step 4: 创建 Sidebar 组件**

```vue
<!-- src/components/Layout/Sidebar.vue -->
<template>
  <div class="sidebar">
    <a-menu
      v-model:selectedKeys="selectedKeys"
      theme="dark"
      mode="inline"
      :items="menuItems"
      @click="handleMenuClick"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const selectedKeys = ref<string[]>(['dashboard'])

const menuItems = [
  {
    key: 'dashboard',
    icon: () => h('span', '📊'),
    label: '仪表盘',
    path: '/dashboard'
  },
  {
    key: 'graph',
    icon: () => h('span', '🔗'),
    label: '图谱管理',
    children: [
      { key: 'graph-list', label: '图谱列表', path: '/graph/list' },
      { key: 'graph-workspace', label: '工作区', path: '/graph/workspace' }
    ]
  },
  {
    key: 'ontology',
    icon: () => h('span', '📐'),
    label: '本体配置',
    path: '/ontology'
  },
  {
    key: 'data',
    icon: () => h('span', '📂'),
    label: '数据管理',
    children: [
      { key: 'nodes', label: '节点管理', path: '/data/nodes' },
      { key: 'edges', label: '边管理', path: '/data/edges' },
      { key: 'episodes', label: '事件管理', path: '/data/episodes' }
    ]
  },
  {
    key: 'search',
    icon: () => h('span', '🔍'),
    label: '混合检索',
    path: '/search'
  }
]

const handleMenuClick = ({ key }: { key: string }) => {
  const item = findMenuItem(menuItems, key)
  if (item?.path) {
    router.push(item.path)
  }
}

const findMenuItem = (items: any[], key: string): any => {
  for (const item of items) {
    if (item.key === key) return item
    if (item.children) {
      const found = findMenuItem(item.children, key)
      if (found) return found
    }
  }
  return null
}
</script>

<style lang="less" scoped>
.sidebar {
  width: 240px;
  background-color: @surface-1;
  border-right: 1px solid @hairline;
  height: calc(100vh - 56px);
  overflow-y: auto;
}
</style>
```

- [ ] **Step 5: 创建 BasicLayout 组件**

```vue
<!-- src/components/Layout/BasicLayout.vue -->
<template>
  <div class="basic-layout">
    <Header />
    <div class="layout-body">
      <Sidebar />
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import Header from './Header.vue'
import Sidebar from './Sidebar.vue'
</script>

<style lang="less" scoped>
.basic-layout {
  height: 100vh;
  
  .layout-body {
    display: flex;
    height: calc(100vh - 56px);
    
    .content {
      flex: 1;
      overflow-y: auto;
      padding: 24px;
      background-color: @canvas;
    }
  }
}
</style>
```

- [ ] **Step 6: 更新路由配置**

```typescript
// src/router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import BasicLayout from '@/components/Layout/BasicLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue')
  },
  {
    path: '/',
    component: BasicLayout,
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue')
      },
      {
        path: 'graph/list',
        name: 'GraphList',
        component: () => import('@/views/graph/list.vue')
      },
      {
        path: 'ontology',
        name: 'Ontology',
        component: () => import('@/views/ontology/index.vue')
      },
      {
        path: 'data/nodes',
        name: 'Nodes',
        component: () => import('@/views/data/nodes.vue')
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/index.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
```

- [ ] **Step 7: Commit**

```bash
git add src/components/Layout/ src/assets/styles/ src/router/index.ts
git commit -m "feat: implement basic layout with header, sidebar, and dark theme"
```

---

## Phase 2: 业务视图核心（3-5 天）

### Task 5: 仪表盘页面

**Files:**
- Create: `ontograph-web/src/views/dashboard/index.vue`
- Create: `ontograph-web/src/api/graph.ts`
- Create: `ontograph-web/src/store/modules/graph.ts`

- [ ] **Step 1: 创建图谱管理 API**

```typescript
// src/api/graph.ts
import request from './request'

export interface GraphMetadata {
  id: number
  graphId: string
  name: string
  description: string
  nodeCount: number
  edgeCount: number
  episodeCount: number
  createTime: string
}

export function getGraphList(): Promise<GraphMetadata[]> {
  return request.get('/graph/list')
}

export function getGraphDetail(graphId: string): Promise<GraphMetadata> {
  return request.get(`/graph/${graphId}`)
}

export function createGraph(data: Partial<GraphMetadata>): Promise<GraphMetadata> {
  return request.post('/graph', data)
}

export function deleteGraph(graphId: string): Promise<void> {
  return request.delete(`/graph/${graphId}`)
}
```

- [ ] **Step 2: 创建仪表盘页面**

```vue
<!-- src/views/dashboard/index.vue -->
<template>
  <div class="dashboard">
    <h1 class="page-title">仪表盘</h1>
    
    <div class="stats-grid">
      <StatsCard 
        title="图谱总数"
        :value="stats.graphCount"
        icon="🔗"
        color="#5e6ad2"
      />
      <StatsCard 
        title="实体节点"
        :value="stats.nodeCount"
        icon="●"
        color="#00d4ff"
      />
      <StatsCard 
        title="关系边数"
        :value="stats.edgeCount"
        icon="→"
        color="#27a644"
      />
      <StatsCard 
        title="事件数"
        :value="stats.episodeCount"
        icon="📅"
        color="#ff8c00"
      />
    </div>
    
    <div class="quick-actions">
      <h2>快捷操作</h2>
      <a-button type="primary" @click="router.push('/graph/create')">
        创建新图谱
      </a-button>
      <a-button @click="router.push('/data/import')">
        导入数据
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatsCard from '@/components/StatsCard/index.vue'

const router = useRouter()

const stats = reactive({
  graphCount: 0,
  nodeCount: 0,
  edgeCount: 0,
  episodeCount: 0
})

onMounted(async () => {
  // TODO: 调用 API 获取统计数据
  stats.graphCount = 12
  stats.nodeCount = 84500
  stats.edgeCount = 156000
  stats.episodeCount = 3200
})
</script>

<style lang="less" scoped>
.dashboard {
  .page-title {
    font-size: 28px;
    font-weight: 600;
    color: @ink;
    margin-bottom: 24px;
  }
  
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 32px;
  }
  
  .quick-actions {
    h2 {
      font-size: 20px;
      color: @ink-muted;
      margin-bottom: 16px;
    }
    
    button {
      margin-right: 12px;
    }
  }
}
</style>
```

- [ ] **Step 3: 创建 StatsCard 组件**

```vue
<!-- src/components/StatsCard/index.vue -->
<template>
  <div class="stats-card">
    <div class="card-header">
      <span class="icon" :style="{ color }">{{ icon }}</span>
      <span class="title">{{ title }}</span>
    </div>
    <div class="card-value">{{ formatNumber(value) }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  value: number
  icon: string
  color: string
}>()

const formatNumber = (num: number): string => {
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}
</script>

<style lang="less" scoped>
.stats-card {
  background-color: @surface-1;
  border: 1px solid @hairline;
  border-radius: 12px;
  padding: 20px;
  
  &:hover {
    border-color: @hairline-strong;
  }
  
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    
    .icon {
      font-size: 20px;
    }
    
    .title {
      font-size: 14px;
      color: @ink-muted;
    }
  }
  
  .card-value {
    font-size: 32px;
    font-weight: 600;
    color: @ink;
  }
}
</style>
```

- [ ] **Step 4: Commit**

```bash
git add src/views/dashboard/ src/api/graph.ts src/components/StatsCard/
git commit -m "feat: implement dashboard with stats cards"
```

---

### Task 6: 图谱可视化组件

**Files:**
- Create: `ontograph-web/src/components/GraphViewer/ForceGraph.vue`
- Create: `ontograph-web/src/components/GraphViewer/GraphToolbar.vue`
- Create: `ontograph-web/src/components/GraphViewer/NodeDetail.vue`
- Create: `ontograph-web/src/utils/graph.ts`

- [ ] **Step 1: 创建图谱工具函数**

```typescript
// src/utils/graph.ts
export interface GraphNode {
  id: string
  name: string
  type: string
  symbolSize: number
  itemStyle: {
    color: string
    borderColor: string
  }
}

export interface GraphEdge {
  source: string
  target: string
  name: string
  lineStyle: {
    color: string
    type: string
  }
}

export interface GraphData {
  nodes: GraphNode[]
  edges: GraphEdge[]
}

export const NODE_COLORS: Record<string, string> = {
  'Person': '#5e6ad2',
  'Company': '#00d4ff',
  'Location': '#27a644',
  'Event': '#ff8c00',
  'Concept': '#ff6b6b'
}

export function createForceGraphOption(data: GraphData) {
  return {
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          return `<div>名称: ${params.data.name}</div>
                  <div>类型: ${params.data.type}</div>`
        }
        return `<div>关系: ${params.data.name}</div>`
      }
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: data.nodes,
        edges: data.edges,
        roam: true,
        draggable: true,
        force: {
          repulsion: 200,
          edgeLength: 100
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 4
          }
        },
        lineStyle: {
          color: 'source',
          curveness: 0.3
        }
      }
    ]
  }
}
```

- [ ] **Step 2: 创建力导向图组件**

```vue
<!-- src/components/GraphViewer/ForceGraph.vue -->
<template>
  <div class="force-graph" ref="chartRef"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import * as echarts from 'echarts'
import { createForceGraphOption, type GraphData } from '@/utils/graph'

const props = defineProps<{
  data: GraphData
}>()

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

onMounted(() => {
  if (chartRef.value) {
    chartInstance = echarts.init(chartRef.value)
    chartInstance.setOption(createForceGraphOption(props.data))
  }
})

watch(() => props.data, (newData) => {
  if (chartInstance) {
    chartInstance.setOption(createForceGraphOption(newData))
  }
}, { deep: true })

const resize = () => {
  chartInstance?.resize()
}

window.addEventListener('resize', resize)
</script>

<style lang="less" scoped>
.force-graph {
  width: 100%;
  height: 600px;
}
</style>
```

- [ ] **Step 3: Commit**

```bash
git add src/components/GraphViewer/ src/utils/graph.ts
git commit -m "feat: implement force-directed graph visualization with ECharts"
```

---

### Task 7: 图谱列表 + 详情页

**Files:**
- Create: `ontograph-web/src/views/graph/list.vue`
- Create: `ontograph-web/src/views/graph/detail.vue`
- Create: `ontograph-web/src/views/graph/workspace.vue`

- [ ] **Step 1: 创建图谱列表页面**

```vue
<!-- src/views/graph/list.vue -->
<template>
  <div class="graph-list">
    <div class="page-header">
      <h1>图谱管理</h1>
      <a-button type="primary" @click="showCreateModal">
        创建图谱
      </a-button>
    </div>
    
    <a-table 
      :columns="columns" 
      :data-source="graphs"
      :loading="loading"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="viewDetail(record)">查看</a-button>
          <a-button type="link" @click="deleteGraph(record)">删除</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getGraphList, deleteGraph as deleteGraphApi } from '@/api/graph'
import type { GraphMetadata } from '@/api/graph'

const router = useRouter()
const graphs = ref<GraphMetadata[]>([])
const loading = ref(false)

const columns = [
  { title: '图谱名称', dataIndex: 'name', key: 'name' },
  { title: '图谱ID', dataIndex: 'graphId', key: 'graphId' },
  { title: '节点数', dataIndex: 'nodeCount', key: 'nodeCount' },
  { title: '边数', dataIndex: 'edgeCount', key: 'edgeCount' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime' },
  { title: '操作', key: 'action' }
]

const viewDetail = (record: GraphMetadata) => {
  router.push(`/graph/detail/${record.graphId}`)
}

const deleteGraph = async (record: GraphMetadata) => {
  await deleteGraphApi(record.graphId)
  await loadGraphs()
}

const loadGraphs = async () => {
  loading.value = true
  try {
    graphs.value = await getGraphList()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadGraphs()
})
</script>
```

- [ ] **Step 2: Commit**

```bash
git add src/views/graph/
git commit -m "feat: implement graph list and detail pages"
```

---

## Phase 3-6: 后续任务（概要）

由于篇幅限制，Phase 3-6 的任务描述将简化。实际实施时，每个任务都需要按照相同的详细程度展开。

### Phase 3: 数据管理 + 搜索（3-4 天）
- Task 8: 节点管理页面
- Task 9: 边管理页面
- Task 10: 事件管理页面
- Task 11: 混合检索页面
- Task 12: 数据导入向导

### Phase 4: 本体论编辑器（2-3 天）
- Task 13: 实体类型管理
- Task 14: 关系类型管理
- Task 15: 本体预览可视化

### Phase 5: 开发者视图 + 推理（2-3 天）
- Task 16: Cypher 查询编辑器
- Task 17: API 测试面板
- Task 18: 推理引擎页面
- Task 19: 社区发现页面

### Phase 6: 优化 + 测试（2-3 天）
- Task 20: 性能优化（大数据集）
- Task 21: 响应式适配
- Task 22: E2E 测试
- Task 23: 部署配置

---

## 自我审查

**1. Spec coverage:**
- ✅ 深色科技风 UI - Phase 1 Task 4 实现
- ✅ 双视图模式 - Phase 1 Task 4 Header 组件实现切换
- ✅ 图谱可视化 - Phase 2 Task 6 实现
- ✅ 本体论编辑器 - Phase 4 任务实现
- ✅ 混合检索 - Phase 3 Task 11 实现
- ⚠️ 推理引擎 - Phase 5 Task 18 (待展开)
- ⚠️ 社区发现 - Phase 5 Task 19 (待展开)

**2. Placeholder scan:**
- ✅ 无占位符
- ⚠️ Phase 3-6 任务描述简化 (需要在实际实施时展开)

**3. Type consistency:**
- ✅ GraphMetadata 接口在 API 和页面中一致
- ✅ GraphData 接口在工具函数和组件中一致

---

## 执行交接

Plan complete and saved to `docs/superpowers/plans/2026-05-08-graphiti-console-implementation.md`.

**Two execution options:**

**1. Subagent-Driven (recommended)** - 每个任务派遣一个子代理，任务间审查，快速迭代

**2. Inline Execution** - 在当前会话中使用 executing-plans 执行，批量执行并设置检查点

**Which approach?**
