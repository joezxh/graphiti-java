// 系统配置类型定义
export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  configName: string
  configDescription: string
  configType: number // 1-文本 2-数字 3-布尔 4-JSON
  groupName: string
  sort: number
  status: number // 0-禁用 1-启用
  createdAt: string
  updatedAt: string
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
  sort: number
  status: number
}

// Mock 数据
const mockConfigs: SystemConfig[] = [
  {
    id: 1,
    configKey: 'system.name',
    configValue: 'Graphiti知识图谱系统',
    configName: '系统名称',
    configDescription: '系统显示名称',
    configType: 1,
    groupName: '系统设置',
    sort: 1,
    status: 1,
    createdAt: '2024-01-01 00:00:00',
    updatedAt: '2024-01-01 00:00:00'
  },
  {
    id: 2,
    configKey: 'system.logo',
    configValue: '/logo.png',
    configName: '系统Logo',
    configDescription: '系统Logo图片路径',
    configType: 1,
    groupName: '系统设置',
    sort: 2,
    status: 1,
    createdAt: '2024-01-01 00:00:00',
    updatedAt: '2024-01-01 00:00:00'
  },
  {
    id: 3,
    configKey: 'ai.model',
    configValue: 'gpt-4',
    configName: 'AI模型',
    configDescription: '使用的AI模型名称',
    configType: 1,
    groupName: 'AI设置',
    sort: 1,
    status: 1,
    createdAt: '2024-01-15 00:00:00',
    updatedAt: '2024-01-15 00:00:00'
  },
  {
    id: 4,
    configKey: 'ai.temperature',
    configValue: '0.7',
    configName: '温度参数',
    configDescription: 'AI生成温度参数',
    configType: 2,
    groupName: 'AI设置',
    sort: 2,
    status: 1,
    createdAt: '2024-01-15 00:00:00',
    updatedAt: '2024-01-15 00:00:00'
  },
  {
    id: 5,
    configKey: 'ai.maxTokens',
    configValue: '2048',
    configName: '最大Token数',
    configDescription: 'AI生成最大Token数',
    configType: 2,
    groupName: 'AI设置',
    sort: 3,
    status: 1,
    createdAt: '2024-01-15 00:00:00',
    updatedAt: '2024-01-15 00:00:00'
  },
  {
    id: 6,
    configKey: 'neo4j.uri',
    configValue: 'bolt://localhost:7687',
    configName: 'Neo4j URI',
    configDescription: 'Neo4j数据库连接URI',
    configType: 1,
    groupName: '数据库设置',
    sort: 1,
    status: 1,
    createdAt: '2024-02-01 00:00:00',
    updatedAt: '2024-02-01 00:00:00'
  },
  {
    id: 7,
    configKey: 'neo4j.username',
    configValue: 'neo4j',
    configName: 'Neo4j用户名',
    configDescription: 'Neo4j数据库用户名',
    configType: 1,
    groupName: '数据库设置',
    sort: 2,
    status: 1,
    createdAt: '2024-02-01 00:00:00',
    updatedAt: '2024-02-01 00:00:00'
  },
  {
    id: 8,
    configKey: 'cache.enabled',
    configValue: 'true',
    configName: '启用缓存',
    configDescription: '是否启用缓存',
    configType: 3,
    groupName: '缓存设置',
    sort: 1,
    status: 1,
    createdAt: '2024-02-15 00:00:00',
    updatedAt: '2024-02-15 00:00:00'
  },
  {
    id: 9,
    configKey: 'cache.ttl',
    configValue: '3600',
    configName: '缓存TTL',
    configDescription: '缓存过期时间（秒）',
    configType: 2,
    groupName: '缓存设置',
    sort: 2,
    status: 1,
    createdAt: '2024-02-15 00:00:00',
    updatedAt: '2024-02-15 00:00:00'
  }
]

// 模拟延迟
const delay = (ms: number = 500) => new Promise(resolve => setTimeout(resolve, ms))

// 系统配置 API
export const systemApi = {
  // 获取配置列表
  getConfigs: async (params: SystemConfigQuery = {}) => {
    await delay()
    
    let filteredConfigs = [...mockConfigs]
    
    if (params.configKey) {
      filteredConfigs = filteredConfigs.filter(c => c.configKey.includes(params.configKey!))
    }
    
    if (params.configName) {
      filteredConfigs = filteredConfigs.filter(c => c.configName.includes(params.configName!))
    }
    
    if (params.groupName) {
      filteredConfigs = filteredConfigs.filter(c => c.groupName === params.groupName)
    }
    
    if (params.status !== undefined) {
      filteredConfigs = filteredConfigs.filter(c => c.status === params.status)
    }
    
    // 按分组和排序排序
    filteredConfigs.sort((a, b) => {
      if (a.groupName !== b.groupName) {
        return a.groupName.localeCompare(b.groupName)
      }
      return a.sort - b.sort
    })
    
    const pageNum = params.pageNum || 1
    const pageSize = params.pageSize || 10
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    
    return {
      list: filteredConfigs.slice(start, end),
      total: filteredConfigs.length,
      pageNum,
      pageSize
    }
  },
  
  // 获取所有配置（用于下拉选择分组）
  getAllConfigs: async () => {
    await delay(300)
    
    return mockConfigs
  },
  
  // 获取配置详情
  getConfig: async (id: number) => {
    await delay()
    
    const config = mockConfigs.find(c => c.id === id)
    if (!config) {
      throw new Error('配置不存在')
    }
    
    return config
  },
  
  // 根据键获取配置值
  getConfigByKey: async (key: string) => {
    await delay(300)
    
    const config = mockConfigs.find(c => c.configKey === key)
    if (!config) {
      throw new Error('配置不存在')
    }
    
    return config.configValue
  },
  
  // 创建配置
  createConfig: async (data: SystemConfigForm) => {
    await delay()
    
    const newConfig: SystemConfig = {
      id: mockConfigs.length + 1,
      configKey: data.configKey,
      configValue: data.configValue,
      configName: data.configName,
      configDescription: data.configDescription,
      configType: data.configType,
      groupName: data.groupName,
      sort: data.sort,
      status: data.status,
      createdAt: new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0],
      updatedAt: new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0]
    }
    
    mockConfigs.push(newConfig)
    
    return { id: newConfig.id }
  },
  
  // 更新配置
  updateConfig: async (id: number, data: Partial<SystemConfigForm>) => {
    await delay()
    
    const index = mockConfigs.findIndex(c => c.id === id)
    if (index === -1) {
      throw new Error('配置不存在')
    }
    
    if (data.configKey !== undefined) mockConfigs[index].configKey = data.configKey
    if (data.configValue !== undefined) mockConfigs[index].configValue = data.configValue
    if (data.configName !== undefined) mockConfigs[index].configName = data.configName
    if (data.configDescription !== undefined) mockConfigs[index].configDescription = data.configDescription
    if (data.configType !== undefined) mockConfigs[index].configType = data.configType
    if (data.groupName !== undefined) mockConfigs[index].groupName = data.groupName
    if (data.sort !== undefined) mockConfigs[index].sort = data.sort
    if (data.status !== undefined) mockConfigs[index].status = data.status
    mockConfigs[index].updatedAt = new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0]
    
    return {}
  },
  
  // 删除配置
  deleteConfig: async (id: number) => {
    await delay()
    
    const index = mockConfigs.findIndex(c => c.id === id)
    if (index === -1) {
      throw new Error('配置不存在')
    }
    
    mockConfigs.splice(index, 1)
    
    return {}
  },
  
  // 更新配置状态
  updateStatus: async (id: number, status: number) => {
    await delay()
    
    const index = mockConfigs.findIndex(c => c.id === id)
    if (index === -1) {
      throw new Error('配置不存在')
    }
    
    mockConfigs[index].status = status
    mockConfigs[index].updatedAt = new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0]
    
    return {}
  },
  
  // 获取配置分组列表
  getGroups: async () => {
    await delay(300)
    
    const groups = [...new Set(mockConfigs.map(c => c.groupName))]
    return groups
  }
}
