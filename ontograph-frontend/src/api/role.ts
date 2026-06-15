import request from './request'

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

// 后端 RoleDO 映射
function mapRoleDO(role: any): Role {
  return {
    id: role.id,
    name: role.name,
    code: role.code,
    description: role.description || '',
    status: role.status ?? 1,
    menuIds: [],
    createdAt: role.createTime || role.createdAt || '',
    updatedAt: role.updateTime || role.updatedAt || ''
  }
}

// 角色管理 API
export const roleApi = {
  /**
   * 获取角色列表（分页）
   * 后端: GET /admin/system/role/list
   * 注意：后端返回全量列表，前端做分页过滤
   */
  async getRoles(params: RoleQuery = {}): Promise<{ list: Role[]; total: number; pageNum: number; pageSize: number }> {
    const resp = await request.get<Role[]>('/admin/system/role/list')
    const all: Role[] = ((resp as any) || []).map(mapRoleDO)

    let filtered = [...all]
    if (params.name) {
      filtered = filtered.filter(r => r.name.includes(params.name!))
    }
    if (params.code) {
      filtered = filtered.filter(r => r.code.includes(params.code!))
    }
    if (params.status !== undefined) {
      filtered = filtered.filter(r => r.status === params.status)
    }

    const total = filtered.length
    const pageNum = params.pageNum || 1
    const pageSize = params.pageSize || 10
    const start = (pageNum - 1) * pageSize

    return {
      list: filtered.slice(start, start + pageSize),
      total,
      pageNum,
      pageSize
    }
  },

  /**
   * 获取所有角色（用于下拉选择）
   * 后端: GET /admin/system/role/list
   */
  async getAllRoles(): Promise<Pick<Role, 'id' | 'name' | 'code'>[]> {
    const resp = await request.get<Role[]>('/admin/system/role/list')
    return ((resp as any) || []).map(mapRoleDO).map((r: Role) => ({ id: r.id, name: r.name, code: r.code }))
  },

  /**
   * 获取角色详情
   * 后端: GET /admin/system/role/get/{roleId}
   */
  async getRole(id: number): Promise<Role> {
    const resp = await request.get<any>(`/admin/system/role/get/${id}`)
    return mapRoleDO(resp)
  },

  /**
   * 创建角色
   * 后端: POST /admin/system/role/create
   */
  async createRole(data: RoleForm): Promise<{ id: number }> {
    await request.post<{ id: number }>('/admin/system/role/create', {
      name: data.name,
      code: data.code,
      description: data.description,
      status: data.status,
      deleted: false
    })
    return { id: 0 }
  },

  /**
   * 更新角色
   * 后端: PUT /admin/system/role/update
   */
  async updateRole(id: number, data: Partial<RoleForm>): Promise<void> {
    await request.put('/admin/system/role/update', {
      id,
      name: data.name,
      code: data.code,
      description: data.description,
      status: data.status
    })
  },

  /**
   * 删除角色
   * 后端: DELETE /admin/system/role/delete/{roleId}
   */
  async deleteRole(id: number): Promise<void> {
    await request.delete(`/admin/system/role/delete/${id}`)
  },

  /**
   * 更新角色状态
   * 后端: PUT /admin/system/role/update
   */
  async updateStatus(id: number, status: number): Promise<void> {
    await request.put('/admin/system/role/update', { id, status })
  }
}

export default roleApi
