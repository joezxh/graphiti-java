import request from './request'

// 系统配置类型定义
export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  configName: string
  configDescription: string
  configType: number // 1-文本 2-数字 3-布尔 4-JSON
  groupName: string
  sortNum: number
  status: number // 0-禁用 1-启用
  createTime: string
  updateTime: string
}

export interface SystemConfigQuery {
  configKey?: string
  configName?: string
  groupName?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface SystemConfigForm {
  id?: number
  configKey: string
  configValue: string
  configName: string
  configDescription: string
  configType: number
  groupName: string
  sortNum: number
  status: number
}

// 后端 DO 映射
function mapDO(cfg: any): SystemConfig {
  return {
    id: cfg.id,
    configKey: cfg.configKey,
    configValue: cfg.configValue,
    configName: cfg.configName || '',
    configDescription: cfg.configDescription || '',
    configType: cfg.configType ?? 1,
    groupName: cfg.groupName || '',
    sortNum: cfg.sortNum ?? cfg.sort ?? 0,
    status: cfg.status ?? 1,
    createTime: cfg.createTime || cfg.create_time || '',
    updateTime: cfg.updateTime || cfg.update_time || ''
  }
}

// 系统配置 API（调用后端 /admin/system/config/*）
export const systemApi = {
  /**
   * 获取配置列表（分页）
   * 后端: GET /admin/system/config/list
   */
  async getConfigs(params: SystemConfigQuery = {}): Promise<{
    list: SystemConfig[]
    total: number
    pageNum: number
    pageSize: number
  }> {
    const resp = await request.get<any>('/admin/system/config/list', {
      params: {
        pageNo: params.pageNum || 1,
        pageSize: params.pageSize || 10,
        configKey: params.configKey || undefined,
        configName: params.configName || undefined,
        groupName: params.groupName || undefined,
        status: params.status !== undefined ? params.status : undefined
      }
    })
    return {
      list: ((resp as any)?.list || resp || []).map(mapDO),
      total: (resp as any)?.total || 0,
      pageNum: (resp as any)?.pageNum || (params.pageNum || 1),
      pageSize: (resp as any)?.pageSize || (params.pageSize || 10)
    }
  },

  /**
   * 获取所有配置（全量）
   * 后端: GET /admin/system/config/all
   */
  async getAllConfigs(): Promise<SystemConfig[]> {
    const resp = await request.get<any[]>('/admin/system/config/all')
    return ((resp as any) || []).map(mapDO)
  },

  /**
   * 获取配置详情
   * 后端: GET /admin/system/config/{id}
   */
  async getConfig(id: number): Promise<SystemConfig> {
    const resp = await request.get<any>(`/admin/system/config/${id}`)
    return mapDO(resp)
  },

  /**
   * 根据键获取配置值
   * 后端: GET /admin/system/config/key/{key}
   */
  async getConfigByKey(key: string): Promise<string> {
    const resp = await request.get<any>(`/admin/system/config/key/${key}`)
    return (resp as any)?.configValue ?? ''
  },

  /**
   * 创建配置
   * 后端: POST /admin/system/config/create
   */
  async createConfig(data: SystemConfigForm): Promise<{ id: number }> {
    await request.post<{ id: number }>('/admin/system/config/create', {
      configKey: data.configKey,
      configValue: data.configValue,
      configName: data.configName,
      configDescription: data.configDescription,
      configType: data.configType,
      groupName: data.groupName,
      sortNum: data.sortNum,
      status: data.status,
      deleted: false
    })
    return { id: 0 }
  },

  /**
   * 更新配置
   * 后端: PUT /admin/system/config/{id}
   */
  async updateConfig(id: number, data: Partial<SystemConfigForm>): Promise<void> {
    await request.put(`/admin/system/config/${id}`, {
      id,
      configValue: data.configValue,
      configName: data.configName,
      configDescription: data.configDescription,
      configType: data.configType,
      groupName: data.groupName,
      sortNum: data.sortNum,
      status: data.status
    })
  },

  /**
   * 删除配置
   * 后端: DELETE /admin/system/config/{id}
   */
  async deleteConfig(id: number): Promise<void> {
    await request.delete(`/admin/system/config/${id}`)
  },

  /**
   * 更新配置状态
   * 后端: PUT /admin/system/config/{id}
   */
  async updateStatus(id: number, status: number): Promise<void> {
    await request.put(`/admin/system/config/${id}`, { id, status })
  },

  /**
   * 获取配置分组列表
   * 后端: GET /admin/system/config/all（前端聚合 groupName）
   */
  async getGroups(): Promise<string[]> {
    const all = await this.getAllConfigs()
    return [...new Set(all.map(c => c.groupName).filter(Boolean))]
  }
}

export default systemApi
