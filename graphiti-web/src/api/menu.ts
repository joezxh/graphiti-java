// 菜单类型定义
export interface MenuItem {
  id: number
  parentId: number
  name: string
  code: string
  type: number // 1-目录 2-菜单 3-按钮
  icon: string
  path: string
  component: string
  permission: string
  sort: number
  status: number // 0-禁用 1-启用
  children?: MenuItem[]
}

export interface MenuQuery {
  name?: string
  status?: number
}

export interface MenuForm {
  id?: number
  parentId: number
  name: string
  code: string
  type: number
  icon: string
  path: string
  component: string
  permission: string
  sort: number
  status: number
}

// Mock 数据
const mockMenus: MenuItem[] = [
  {
    id: 1,
    parentId: 0,
    name: '仪表盘',
    code: 'dashboard',
    type: 2,
    icon: 'DashboardOutlined',
    path: '/dashboard',
    component: 'views/dashboard/index.vue',
    permission: 'dashboard:view',
    sort: 1,
    status: 1
  },
  {
    id: 2,
    parentId: 0,
    name: '图谱管理',
    code: 'graph',
    type: 1,
    icon: 'ShareAltOutlined',
    path: '',
    component: '',
    permission: '',
    sort: 2,
    status: 1
  },
  {
    id: 3,
    parentId: 2,
    name: '图谱列表',
    code: 'graph:list',
    type: 2,
    icon: '',
    path: '/graph/list',
    component: 'views/graph/list.vue',
    permission: 'graph:list:view',
    sort: 1,
    status: 1
  },
  {
    id: 4,
    parentId: 2,
    name: '创建图谱',
    code: 'graph:create',
    type: 2,
    icon: '',
    path: '/graph/create',
    component: 'views/graph/create.vue',
    permission: 'graph:create:view',
    sort: 2,
    status: 1
  },
  {
    id: 5,
    parentId: 2,
    name: '图谱详情',
    code: 'graph:detail',
    type: 2,
    icon: '',
    path: '/graph/detail/:id',
    component: 'views/graph/detail.vue',
    permission: 'graph:detail:view',
    sort: 3,
    status: 1
  },
  {
    id: 6,
    parentId: 0,
    name: '本体配置',
    code: 'ontology',
    type: 2,
    icon: 'ApartmentOutlined',
    path: '/ontology',
    component: 'views/ontology/index.vue',
    permission: 'ontology:view',
    sort: 3,
    status: 1
  },
  {
    id: 7,
    parentId: 0,
    name: '数据管理',
    code: 'data',
    type: 1,
    icon: 'DatabaseOutlined',
    path: '',
    component: '',
    permission: '',
    sort: 4,
    status: 1
  },
  {
    id: 8,
    parentId: 7,
    name: '数据导入',
    code: 'data:import',
    type: 2,
    icon: '',
    path: '/data/import',
    component: 'views/data/import.vue',
    permission: 'data:import:view',
    sort: 1,
    status: 1
  },
  {
    id: 9,
    parentId: 7,
    name: '数据导出',
    code: 'data:export',
    type: 2,
    icon: '',
    path: '/data/export',
    component: 'views/data/export.vue',
    permission: 'data:export:view',
    sort: 2,
    status: 1
  },
  {
    id: 10,
    parentId: 7,
    name: '实体管理',
    code: 'data:entities',
    type: 2,
    icon: '',
    path: '/data/entities',
    component: 'views/data/entities.vue',
    permission: 'data:entities:view',
    sort: 3,
    status: 1
  },
  {
    id: 11,
    parentId: 0,
    name: '混合检索',
    code: 'search',
    type: 2,
    icon: 'SearchOutlined',
    path: '/search',
    component: 'views/search/index.vue',
    permission: 'search:view',
    sort: 5,
    status: 1
  },
  {
    id: 12,
    parentId: 0,
    name: '系统管理',
    code: 'system',
    type: 1,
    icon: 'SettingOutlined',
    path: '',
    component: '',
    permission: '',
    sort: 6,
    status: 1
  },
  {
    id: 13,
    parentId: 12,
    name: '用户管理',
    code: 'system:user',
    type: 2,
    icon: '',
    path: '/system/user',
    component: 'views/system/user/index.vue',
    permission: 'system:user:view',
    sort: 1,
    status: 1
  },
  {
    id: 14,
    parentId: 12,
    name: '角色管理',
    code: 'system:role',
    type: 2,
    icon: '',
    path: '/system/role',
    component: 'views/system/role/index.vue',
    permission: 'system:role:view',
    sort: 2,
    status: 1
  },
  {
    id: 15,
    parentId: 12,
    name: '菜单管理',
    code: 'system:menu',
    type: 2,
    icon: '',
    path: '/system/menu',
    component: 'views/system/menu/index.vue',
    permission: 'system:menu:view',
    sort: 3,
    status: 1
  }
]

// 模拟延迟
const delay = (ms: number = 500) => new Promise(resolve => setTimeout(resolve, ms))

// 菜单管理 API
export const menuApi = {
  // 获取菜单列表（树形结构）
  getMenus: async (params: MenuQuery = {}) => {
    await delay()
    
    let filteredMenus = [...mockMenus]
    
    if (params.name) {
      filteredMenus = filteredMenus.filter(m => m.name.includes(params.name!))
    }
    
    if (params.status !== undefined) {
      filteredMenus = filteredMenus.filter(m => m.status === params.status)
    }
    
    // 构建树形结构
    const buildTree = (parentId: number): MenuItem[] => {
      return filteredMenus
        .filter(m => m.parentId === parentId)
        .sort((a, b) => a.sort - b.sort)
        .map(m => ({
          ...m,
          children: buildTree(m.id)
        }))
    }
    
    return buildTree(0)
  },
  
  // 获取所有菜单（扁平结构，用于下拉选择父菜单）
  getAllMenus: async () => {
    await delay(300)
    
    return mockMenus
      .filter(m => m.status === 1)
      .map(m => ({
        id: m.id,
        parentId: m.parentId,
        name: m.name,
        type: m.type
      }))
  },
  
  // 获取菜单详情
  getMenu: async (id: number) => {
    await delay()
    
    const menu = mockMenus.find(m => m.id === id)
    if (!menu) {
      throw new Error('菜单不存在')
    }
    
    return menu
  },
  
  // 创建菜单
  createMenu: async (data: MenuForm) => {
    await delay()
    
    const newMenu: MenuItem = {
      id: mockMenus.length + 1,
      parentId: data.parentId,
      name: data.name,
      code: data.code,
      type: data.type,
      icon: data.icon,
      path: data.path,
      component: data.component,
      permission: data.permission,
      sort: data.sort,
      status: data.status
    }
    
    mockMenus.push(newMenu)
    
    return { id: newMenu.id }
  },
  
  // 更新菜单
  updateMenu: async (id: number, data: Partial<MenuForm>) => {
    await delay()
    
    const index = mockMenus.findIndex(m => m.id === id)
    if (index === -1) {
      throw new Error('菜单不存在')
    }
    
    if (data.parentId !== undefined) mockMenus[index].parentId = data.parentId
    if (data.name !== undefined) mockMenus[index].name = data.name
    if (data.code !== undefined) mockMenus[index].code = data.code
    if (data.type !== undefined) mockMenus[index].type = data.type
    if (data.icon !== undefined) mockMenus[index].icon = data.icon
    if (data.path !== undefined) mockMenus[index].path = data.path
    if (data.component !== undefined) mockMenus[index].component = data.component
    if (data.permission !== undefined) mockMenus[index].permission = data.permission
    if (data.sort !== undefined) mockMenus[index].sort = data.sort
    if (data.status !== undefined) mockMenus[index].status = data.status
    
    return {}
  },
  
  // 删除菜单
  deleteMenu: async (id: number) => {
    await delay()
    
    // 检查是否有子菜单
    const hasChildren = mockMenus.some(m => m.parentId === id)
    if (hasChildren) {
      throw new Error('该菜单下有子菜单，无法删除')
    }
    
    const index = mockMenus.findIndex(m => m.id === id)
    if (index === -1) {
      throw new Error('菜单不存在')
    }
    
    mockMenus.splice(index, 1)
    
    return {}
  },
  
  // 更新菜单状态
  updateStatus: async (id: number, status: number) => {
    await delay()
    
    const index = mockMenus.findIndex(m => m.id === id)
    if (index === -1) {
      throw new Error('菜单不存在')
    }
    
    mockMenus[index].status = status
    
    return {}
  }
}
