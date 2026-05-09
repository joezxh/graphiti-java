// 导入任务
export interface ImportTask {
  id: string
  graphId: string
  format: 'json' | 'csv' | 'triple'
  status: 'pending' | 'processing' | 'completed' | 'failed'
  totalRows: number
  processedRows: number
  errorMessage?: string
  createdAt: string
  completedAt?: string
}

// 导出任务
export interface ExportTask {
  id: string
  graphId: string
  format: 'json' | 'csv' | 'triple'
  status: 'pending' | 'processing' | 'completed' | 'failed'
  fileName?: string
  fileSize?: number
  createdAt: string
  completedAt?: string
}

// 实体查询参数
export interface EntityListParams {
  graphId?: string
  keyword?: string
  type?: string
  page?: number
  pageSize?: number
}

// 实体列表响应
export interface EntityListResp {
  list: EntityItem[]
  total: number
}

// 实体项
export interface EntityItem {
  uuid: string
  name: string
  type: string
  properties: Record<string, any>
  createdAt?: string
  updatedAt?: string
}

// Mock 数据
let mockEntities: EntityItem[] = [
  {
    uuid: 'node-001',
    name: '张三',
    type: 'Person',
    properties: { name: '张三', age: 30, email: 'zhangsan@example.com' },
    createdAt: '2024-01-15T08:00:00Z',
    updatedAt: '2024-03-20T10:30:00Z'
  },
  {
    uuid: 'node-002',
    name: '李四',
    type: 'Person',
    properties: { name: '李四', age: 28, email: 'lisi@example.com' },
    createdAt: '2024-01-16T09:00:00Z',
    updatedAt: '2024-03-21T11:00:00Z'
  },
  {
    uuid: 'node-003',
    name: 'ABC科技',
    type: 'Company',
    properties: { name: 'ABC科技', industry: '互联网', founded: '2018-05-10' },
    createdAt: '2024-02-01T10:00:00Z',
    updatedAt: '2024-03-22T12:00:00Z'
  },
  {
    uuid: 'node-004',
    name: 'XYZ产品',
    type: 'Product',
    properties: { name: 'XYZ产品', price: 299.99, category: '软件' },
    createdAt: '2024-02-15T11:00:00Z',
    updatedAt: '2024-03-23T13:00:00Z'
  },
  {
    uuid: 'node-005',
    name: '王五',
    type: 'Person',
    properties: { name: '王五', age: 35, email: 'wangwu@example.com' },
    createdAt: '2024-03-01T12:00:00Z',
    updatedAt: '2024-03-24T14:00:00Z'
  }
]

let mockImportTasks: ImportTask[] = [
  {
    id: 'imp-001',
    graphId: 'graph-001',
    format: 'json',
    status: 'completed',
    totalRows: 150,
    processedRows: 150,
    createdAt: '2024-03-20T08:00:00Z',
    completedAt: '2024-03-20T08:02:30Z'
  },
  {
    id: 'imp-002',
    graphId: 'graph-001',
    format: 'csv',
    status: 'failed',
    totalRows: 200,
    processedRows: 45,
    errorMessage: '第 46 行数据格式错误：缺少必填字段 "name"',
    createdAt: '2024-03-21T09:00:00Z'
  }
]

let mockExportTasks: ExportTask[] = [
  {
    id: 'exp-001',
    graphId: 'graph-001',
    format: 'json',
    status: 'completed',
    fileName: 'graph-001-export.json',
    fileSize: 1024 * 1024 * 2.5,
    createdAt: '2024-03-22T10:00:00Z',
    completedAt: '2024-03-22T10:01:15Z'
  }
]

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// 数据管理 API
export const dataApi = {
  // 预览导入数据
  async previewImport(_graphId: string, _format: 'json' | 'csv' | 'triple', _data: string): Promise<EntityItem[]> {
    await delay(500)
    // 模拟解析返回前 5 条预览数据
    return mockEntities.slice(0, 5).map(e => ({ ...e, uuid: `${e.uuid}-preview` }))
  },

  // 执行导入
  async importData(graphId: string, format: 'json' | 'csv' | 'triple', _data: string): Promise<ImportTask> {
    await delay(800)
    const task: ImportTask = {
      id: `imp-${Date.now()}`,
      graphId,
      format,
      status: 'completed',
      totalRows: Math.floor(Math.random() * 500) + 50,
      processedRows: 0,
      createdAt: new Date().toISOString()
    }
    task.processedRows = task.totalRows
    task.completedAt = new Date().toISOString()
    mockImportTasks.unshift(task)
    return task
  },

  // 获取导入历史
  async getImportHistory(graphId?: string): Promise<ImportTask[]> {
    await delay(300)
    if (graphId) {
      return mockImportTasks.filter(t => t.graphId === graphId)
    }
    return [...mockImportTasks]
  },

  // 导出数据
  async exportData(graphId: string, format: 'json' | 'csv' | 'triple'): Promise<ExportTask> {
    await delay(600)
    const task: ExportTask = {
      id: `exp-${Date.now()}`,
      graphId,
      format,
      status: 'completed',
      fileName: `${graphId}-export.${format === 'triple' ? 'nt' : format}`,
      fileSize: Math.floor(Math.random() * 1024 * 1024 * 5) + 1024 * 100,
      createdAt: new Date().toISOString(),
      completedAt: new Date().toISOString()
    }
    mockExportTasks.unshift(task)
    return task
  },

  // 获取导出历史
  async getExportHistory(graphId?: string): Promise<ExportTask[]> {
    await delay(300)
    if (graphId) {
      return mockExportTasks.filter(t => t.graphId === graphId)
    }
    return [...mockExportTasks]
  },

  // 下载导出文件
  async downloadExport(_taskId: string): Promise<Blob> {
    await delay(300)
    // 模拟返回文件内容
    const content = JSON.stringify({ nodes: mockEntities }, null, 2)
    return new Blob([content], { type: 'application/json' })
  },

  // 获取实体列表
  async listEntities(params: EntityListParams): Promise<EntityListResp> {
    await delay(400)
    let list = [...mockEntities]

    if (params.keyword) {
      const kw = params.keyword.toLowerCase()
      list = list.filter(e => e.name.toLowerCase().includes(kw))
    }

    if (params.type) {
      list = list.filter(e => e.type === params.type)
    }

    const total = list.length
    const page = params.page || 1
    const pageSize = params.pageSize || 10
    const start = (page - 1) * pageSize
    list = list.slice(start, start + pageSize)

    return { list, total }
  },

  // 更新实体
  async updateEntity(uuid: string, properties: Record<string, any>): Promise<EntityItem> {
    await delay(300)
    const idx = mockEntities.findIndex(e => e.uuid === uuid)
    if (idx === -1) throw new Error('Entity not found')
    mockEntities[idx] = {
      ...mockEntities[idx],
      properties,
      name: properties.name || mockEntities[idx].name,
      updatedAt: new Date().toISOString()
    }
    return mockEntities[idx]
  },

  // 删除实体
  async deleteEntity(uuid: string): Promise<void> {
    await delay(300)
    mockEntities = mockEntities.filter(e => e.uuid !== uuid)
  },

  // 获取实体类型列表（用于筛选）
  async getEntityTypes(): Promise<string[]> {
    await delay(200)
    const types = new Set(mockEntities.map(e => e.type))
    return Array.from(types)
  }
}

export default dataApi
