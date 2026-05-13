import request from './request'

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

// 后端 UserDO 映射
function mapUserDO(user: any): User {
  return {
    id: user.id,
    username: user.username,
    nickname: user.nickname || '',
    email: user.email || '',
    phone: user.mobile || '',
    avatar: user.avatar || '',
    status: user.status ?? 1,
    roleIds: [],
    createdAt: user.createTime || user.createdAt || '',
    updatedAt: user.updateTime || user.updatedAt || ''
  }
}

// 用户管理 API
export const userApi = {
  /**
   * 获取用户列表（分页）
   * 后端: GET /admin/system/user/list
   */
  async getUsers(params: UserQuery = {}): Promise<{ list: User[]; total: number; pageNum: number; pageSize: number }> {
    const resp = await request.get<any>(`/admin/system/user/list`, {
      params: {
        pageNo: params.pageNum || 1,
        pageSize: params.pageSize || 10,
        username: params.username || undefined,
        nickname: params.nickname || undefined,
        status: params.status !== undefined ? params.status : undefined
      }
    })
    const list: User[] = ((resp as any)?.list || []).map(mapUserDO)
    return {
      list,
      total: (resp as any)?.total || 0,
      pageNum: (resp as any)?.pageNum || (params.pageNum || 1),
      pageSize: (resp as any)?.pageSize || (params.pageSize || 10)
    }
  },

  /**
   * 获取用户详情
   * 后端: GET /admin/system/user/get/{userId}
   */
  async getUser(id: number): Promise<User> {
    const resp = await request.get<any>(`/admin/system/user/get/${id}`)
    return mapUserDO(resp)
  },

  /**
   * 创建用户
   * 后端: POST /admin/system/user/create
   */
  async createUser(data: UserForm): Promise<{ id: number }> {
    await request.post<{ id: number }>('/admin/system/user/create', {
      username: data.username,
      nickname: data.nickname,
      password: data.password,
      email: data.email,
      mobile: data.phone,
      status: data.status,
      deleted: false
    })
    return { id: 0 }
  },

  /**
   * 更新用户
   * 后端: PUT /admin/system/user/update
   */
  async updateUser(id: number, data: Partial<UserForm>): Promise<void> {
    const req: any = { id }
    if (data.nickname !== undefined) req.nickname = data.nickname
    if (data.email !== undefined) req.email = data.email
    if (data.phone !== undefined) req.mobile = data.phone
    if (data.status !== undefined) req.status = data.status
    await request.put('/admin/system/user/update', req)
  },

  /**
   * 删除用户
   * 后端: DELETE /admin/system/user/delete/{userId}
   */
  async deleteUser(id: number): Promise<void> {
    await request.delete(`/admin/system/user/delete/${id}`)
  },

  /**
   * 批量删除用户
   * 后端: 无独立接口，循环调用单个删除
   */
  async batchDeleteUsers(ids: number[]): Promise<void> {
    await Promise.all(ids.map(id => request.delete(`/admin/system/user/delete/${id}`)))
  },

  /**
   * 更新用户状态
   * 后端: PUT /admin/system/user/update
   */
  async updateStatus(id: number, status: number): Promise<void> {
    await request.put('/admin/system/user/update', { id, status })
  },

  /**
   * 重置密码
   * 后端: 无此接口，抛出错误提示
   */
  async resetPassword(id: number): Promise<{ message: string }> {
    // 后端无此接口，提示用户手动处理
    throw new Error(`密码重置功能后端尚未实现，请联系管理员处理用户 ${id}`)
  }
}

export default userApi
