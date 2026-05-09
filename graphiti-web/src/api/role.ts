// 角色类型定义
export interface Role {
  id: number
  name: string
  code: string
  description: string
  status: number // 0-禁用 1-启用
  menuIds: number[]
  createdAt: string
  updatedAt: string
}

export interface RoleQuery {
  name?: string
  code?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface RoleForm {
  id?: number
  name: string
  code: string
  description: string
  status: number
  menuIds: number[]
}

// Mock 数据
const mockRoles: Role[] = [
  {
    id: 1,
    name: '管理员',
    code: 'ADMIN',
    description: '系统管理员，拥有所有权限',
    status: 1,
    menuIds: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15],
    createdAt: '2024-01-01 00:00:00',
    updatedAt: '2024-01-01 00:00:00'
  },
  {
    id: 2,
    name: '编辑员',
    code: 'EDITOR',
    description: '编辑人员，可以管理图谱和数据',
    status: 1,
    menuIds: [1, 2, 3, 6, 7, 8, 9, 10, 11],
    createdAt: '2024-01-15 00:00:00',
    updatedAt: '2024-01-15 00:00:00'
  },
  {
    id: 3,
    name: '观察员',
    code: 'VIEWER',
    description: '观察人员，只能查看数据',
    status: 1,
    menuIds: [1, 2, 6, 9, 10],
    createdAt: '2024-02-01 00:00:00',
    updatedAt: '2024-02-01 00:00:00'
  },
  {
    id: 4,
    name: '禁用角色',
    code: 'DISABLED',
    description: '已禁用的角色',
    status: 0,
    menuIds: [],
    createdAt: '2024-03-01 00:00:00',
    updatedAt: '2024-03-10 00:00:00'
  }
]

// 模拟延迟
const delay = (ms: number = 500) => new Promise(resolve => setTimeout(resolve, ms))

// 角色管理 API
export const roleApi = {
  // 获取角色列表
  getRoles: async (params: RoleQuery = {}) => {
    await delay()
    
    let filteredRoles = [...mockRoles]
    
    if (params.name) {
      filteredRoles = filteredRoles.filter(r => r.name.includes(params.name!))
    }
    
    if (params.code) {
      filteredRoles = filteredRoles.filter(r => r.code.includes(params.code!))
    }
    
    if (params.status !== undefined) {
      filteredRoles = filteredRoles.filter(r => r.status === params.status)
    }
    
    const pageNum = params.pageNum || 1
    const pageSize = params.pageSize || 10
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    
    return {
      list: filteredRoles.slice(start, end),
      total: filteredRoles.length,
      pageNum,
      pageSize
    }
  },
  
  // 获取所有角色（用于下拉选择）
  getAllRoles: async (): Promise<Pick<Role, 'id' | 'name' | 'code'>[]> => {
    await delay(300)
    
    return mockRoles
      .filter(r => r.status === 1)
      .map(r => ({
        id: r.id,
        name: r.name,
        code: r.code
      }))
  },
  
  // 获取角色详情
  getRole: async (id: number) => {
    await delay()
    
    const role = mockRoles.find(r => r.id === id)
    if (!role) {
      throw new Error('角色不存在')
    }
    
    return role
  },
  
  // 创建角色
  createRole: async (data: RoleForm) => {
    await delay()
    
    const newRole: Role = {
      id: mockRoles.length + 1,
      name: data.name,
      code: data.code,
      description: data.description,
      status: data.status,
      menuIds: data.menuIds,
      createdAt: new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0],
      updatedAt: new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0]
    }
    
    mockRoles.push(newRole)
    
    return { id: newRole.id }
  },
  
  // 更新角色
  updateRole: async (id: number, data: Partial<RoleForm>) => {
    await delay()
    
    const index = mockRoles.findIndex(r => r.id === id)
    if (index === -1) {
      throw new Error('角色不存在')
    }
    
    if (data.name !== undefined) mockRoles[index].name = data.name
    if (data.code !== undefined) mockRoles[index].code = data.code
    if (data.description !== undefined) mockRoles[index].description = data.description
    if (data.status !== undefined) mockRoles[index].status = data.status
    if (data.menuIds !== undefined) mockRoles[index].menuIds = data.menuIds
    mockRoles[index].updatedAt = new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0]
    
    return {}
  },
  
  // 删除角色
  deleteRole: async (id: number) => {
    await delay()
    
    const index = mockRoles.findIndex(r => r.id === id)
    if (index === -1) {
      throw new Error('角色不存在')
    }
    
    mockRoles.splice(index, 1)
    
    return {}
  },
  
  // 更新角色状态
  updateStatus: async (id: number, status: number) => {
    await delay()
    
    const index = mockRoles.findIndex(r => r.id === id)
    if (index === -1) {
      throw new Error('角色不存在')
    }
    
    mockRoles[index].status = status
    mockRoles[index].updatedAt = new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0]
    
    return {}
  }
}
