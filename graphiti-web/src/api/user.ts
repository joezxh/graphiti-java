// 用户类型定义
export interface User {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  status: number // 0-禁用 1-启用
  roleIds: number[]
  createdAt: string
  updatedAt: string
}

export interface UserQuery {
  username?: string
  nickname?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface UserForm {
  id?: number
  username: string
  nickname: string
  password?: string
  email: string
  phone: string
  status: number
  roleIds: number[]
}

// Mock 数据
const mockUsers: User[] = [
  {
    id: 1,
    username: 'admin',
    nickname: '管理员',
    email: 'admin@graphiti.com',
    phone: '13800138000',
    avatar: '',
    status: 1,
    roleIds: [1],
    createdAt: '2024-01-01 00:00:00',
    updatedAt: '2024-01-01 00:00:00'
  },
  {
    id: 2,
    username: 'editor',
    nickname: '编辑员',
    email: 'editor@graphiti.com',
    phone: '13800138001',
    avatar: '',
    status: 1,
    roleIds: [2],
    createdAt: '2024-01-15 00:00:00',
    updatedAt: '2024-01-15 00:00:00'
  },
  {
    id: 3,
    username: 'viewer',
    nickname: '观察员',
    email: 'viewer@graphiti.com',
    phone: '13800138002',
    avatar: '',
    status: 1,
    roleIds: [3],
    createdAt: '2024-02-01 00:00:00',
    updatedAt: '2024-02-01 00:00:00'
  },
  {
    id: 4,
    username: 'disabled_user',
    nickname: '禁用用户',
    email: 'disabled@graphiti.com',
    phone: '13800138003',
    avatar: '',
    status: 0,
    roleIds: [3],
    createdAt: '2024-03-01 00:00:00',
    updatedAt: '2024-03-10 00:00:00'
  }
]

// 模拟延迟
const delay = (ms: number = 500) => new Promise(resolve => setTimeout(resolve, ms))

// 用户管理 API
export const userApi = {
  // 获取用户列表
  getUsers: async (params: UserQuery = {}) => {
    await delay()
    
    let filteredUsers = [...mockUsers]
    
    if (params.username) {
      filteredUsers = filteredUsers.filter(u => u.username.includes(params.username!))
    }
    
    if (params.nickname) {
      filteredUsers = filteredUsers.filter(u => u.nickname.includes(params.nickname!))
    }
    
    if (params.status !== undefined) {
      filteredUsers = filteredUsers.filter(u => u.status === params.status)
    }
    
    const pageNum = params.pageNum || 1
    const pageSize = params.pageSize || 10
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    
    return {
      list: filteredUsers.slice(start, end),
      total: filteredUsers.length,
      pageNum,
      pageSize
    }
  },
  
  // 获取用户详情
  getUser: async (id: number) => {
    await delay()
    
    const user = mockUsers.find(u => u.id === id)
    if (!user) {
      throw new Error('用户不存在')
    }
    
    return user
  },
  
  // 创建用户
  createUser: async (data: UserForm) => {
    await delay()
    
    const newUser: User = {
      id: mockUsers.length + 1,
      username: data.username,
      nickname: data.nickname,
      email: data.email,
      phone: data.phone,
      avatar: '',
      status: data.status,
      roleIds: data.roleIds,
      createdAt: new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0],
      updatedAt: new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0]
    }
    
    mockUsers.push(newUser)
    
    return { id: newUser.id }
  },
  
  // 更新用户
  updateUser: async (id: number, data: Partial<UserForm>) => {
    await delay()
    
    const index = mockUsers.findIndex(u => u.id === id)
    if (index === -1) {
      throw new Error('用户不存在')
    }
    
    if (data.nickname !== undefined) mockUsers[index].nickname = data.nickname
    if (data.email !== undefined) mockUsers[index].email = data.email
    if (data.phone !== undefined) mockUsers[index].phone = data.phone
    if (data.status !== undefined) mockUsers[index].status = data.status
    if (data.roleIds !== undefined) mockUsers[index].roleIds = data.roleIds
    mockUsers[index].updatedAt = new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0]
    
    return {}
  },
  
  // 删除用户
  deleteUser: async (id: number) => {
    await delay()
    
    const index = mockUsers.findIndex(u => u.id === id)
    if (index === -1) {
      throw new Error('用户不存在')
    }
    
    mockUsers.splice(index, 1)
    
    return {}
  },
  
  // 批量删除用户
  batchDeleteUsers: async (ids: number[]) => {
    await delay()
    
    for (const id of ids) {
      const index = mockUsers.findIndex(u => u.id === id)
      if (index !== -1) {
        mockUsers.splice(index, 1)
      }
    }
    
    return {}
  },
  
  // 更新用户状态
  updateStatus: async (id: number, status: number) => {
    await delay()
    
    const index = mockUsers.findIndex(u => u.id === id)
    if (index === -1) {
      throw new Error('用户不存在')
    }
    
    mockUsers[index].status = status
    mockUsers[index].updatedAt = new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0]
    
    return {}
  },
  
  // 重置密码
  resetPassword: async (id: number) => {
    await delay()
    
    const index = mockUsers.findIndex(u => u.id === id)
    if (index === -1) {
      throw new Error('用户不存在')
    }
    
    return { message: '密码已重置为默认密码' }
  }
}
