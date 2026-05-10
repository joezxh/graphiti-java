import request from './request'

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

// 后端 MenuDO 映射
function mapMenuDO(menu: any): MenuItem {
  return {
    id: menu.id,
    parentId: menu.parentId ?? 0,
    name: menu.name,
    code: menu.permission || '',
    type: 2, // 后端 MenuDO 没有 type 字段，默认菜单
    icon: '',
    path: menu.url || '',
    component: '',
    permission: menu.permission || '',
    sort: menu.sort ?? 0,
    status: menu.status ?? 1,
    children: menu.children ? menu.children.map(mapMenuDO) : undefined
  }
}

// 菜单管理 API
export const menuApi = {
  /**
   * 获取菜单列表（树形结构）
   * 后端: GET /admin/system/menu/list
   */
  async getMenus(_params: MenuQuery = {}): Promise<MenuItem[]> {
    const resp = await request.get<any[]>('/admin/system/menu/list')
    return (resp || []).map(mapMenuDO)
  },

  /**
   * 获取所有菜单（扁平结构，用于下拉选择父菜单）
   * 后端: GET /admin/system/menu/list
   */
  async getAllMenus(): Promise<Pick<MenuItem, 'id' | 'parentId' | 'name' | 'type'>[]> {
    const resp = await request.get<any[]>('/admin/system/menu/list')
    const all = (resp || []).map(mapMenuDO)
    // 扁平化为所有节点（含子节点）
    const flat: MenuItem[] = []
    function flatten(items: MenuItem[]) {
      for (const item of items) {
        flat.push(item)
        if (item.children?.length) flatten(item.children)
      }
    }
    flatten(resp ? resp.map(mapMenuDO) : [])
    return flat
      .filter(m => m.status === 1)
      .map(m => ({ id: m.id, parentId: m.parentId, name: m.name, type: m.type }))
  },

  /**
   * 获取菜单详情
   * 后端: GET /admin/system/menu/get/{menuId}
   */
  async getMenu(id: number): Promise<MenuItem> {
    const resp = await request.get<any>(`/admin/system/menu/get/${id}`)
    return mapMenuDO(resp)
  },

  /**
   * 创建菜单
   * 后端: POST /admin/system/menu/create
   */
  async createMenu(data: MenuForm): Promise<{ id: number }> {
    const resp = await request.post<{ id: number }>('/admin/system/menu/create', {
      name: data.name,
      permission: data.code,
      url: data.path,
      parentId: data.parentId,
      sort: data.sort,
      status: data.status,
      deleted: false
    })
    return resp
  },

  /**
   * 更新菜单
   * 后端: PUT /admin/system/menu/update
   */
  async updateMenu(id: number, data: Partial<MenuForm>): Promise<void> {
    await request.put('/admin/system/menu/update', {
      id,
      name: data.name,
      permission: data.code,
      url: data.path,
      parentId: data.parentId,
      sort: data.sort,
      status: data.status
    })
  },

  /**
   * 删除菜单
   * 后端: DELETE /admin/system/menu/delete/{menuId}
   */
  async deleteMenu(id: number): Promise<void> {
    await request.delete(`/admin/system/menu/delete/${id}`)
  },

  /**
   * 更新菜单状态
   * 后端: PUT /admin/system/menu/update
   */
  async updateStatus(id: number, status: number): Promise<void> {
    await request.put('/admin/system/menu/update', { id, status })
  }
}

export default menuApi
