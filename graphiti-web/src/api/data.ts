import request from './request'

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

// 后端 AddDataReqVO 对应的请求类型
export interface AddDataReq {
  graphId: string
  content: string
  sourceType?: string
  sourceDescription?: string
  name?: string
}

// 后端 AddDataBatchReqVO 对应的请求类型
export interface AddDataBatchReq {
  graphId: string
  items: BatchDataItem[]
  updateCommunities?: boolean
}

export interface BatchDataItem {
  content: string
  sourceType?: string
  sourceDescription?: string
  name?: string
}

// 后端 FactTripleReqVO 对应的请求类型
export interface FactTripleReq {
  graphId: string
  sourceNodeName: string
  relationType: string
  targetNodeName: string
  fact?: string
  properties?: Record<string, any>
}

// 后端 AddEntityNodeReq 对应的请求类型
export interface AddEntityNodeReq {
  name: string
  type?: string
  summary?: string
  properties?: Record<string, any>
}

// 导入响应（与后端 AddDataReqVO / AddDataBatchReqVO 对应）
export interface ImportDataResp {
  // 与后端 CommonResult<Boolean> 对应，request 拦截器解包后 data 为 true
  success: boolean
}

// 实体节点添加响应
export interface AddEntityNodeResp {
  uuid: string
  name: string
  type: string
}

// Mock 数据（用于历史记录展示）
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

// 数据管理 API
export const dataApi = {
  /**
   * 执行数据导入
   * 根据 format 路由到不同后端端点：
   * - json/csv -> /admin/graphiti/data/batch  (批量 Episode 导入)
   * - triple   -> /admin/graphiti/data/fact-triple  (三元组导入)
   */
  async importData(
    graphId: string,
    format: 'json' | 'csv' | 'triple',
    data: string
  ): Promise<ImportTask> {
    const task: ImportTask = {
      id: `imp-${Date.now()}`,
      graphId,
      format,
      status: 'processing',
      totalRows: 0,
      processedRows: 0,
      createdAt: new Date().toISOString()
    }

    if (format === 'triple') {
      // triple 格式：解析 N-Triples 并逐条调用 fact-triple 接口
      const triples = parseNTriples(data)
      task.totalRows = triples.length
      for (const triple of triples) {
        await request.post('/admin/graphiti/data/fact-triple', {
          graphId,
          sourceNodeName: triple.subject,
          relationType: triple.predicate,
          targetNodeName: triple.object,
          fact: `${triple.subject} ${triple.predicate} ${triple.object}`
        } as FactTripleReq)
        task.processedRows++
      }
      task.status = 'completed'
      task.completedAt = new Date().toISOString()
    } else {
      // json / csv 格式：解析后批量导入为 Episode
      const items = format === 'json' ? parseJson(data) : parseCsv(data)
      task.totalRows = items.length
      await request.post('/admin/graphiti/data/batch', {
        graphId,
        items,
        updateCommunities: false
      } as AddDataBatchReq)
      task.processedRows = task.totalRows
      task.status = 'completed'
      task.completedAt = new Date().toISOString()
    }

    return task
  },

  /**
   * 预览导入数据
   * 解析数据格式，返回前 5 条预览
   */
  async previewImport(
    _graphId: string,
    format: 'json' | 'csv' | 'triple',
    data: string
  ): Promise<EntityItem[]> {
    if (format === 'triple') {
      const triples = parseNTriples(data)
      return triples.slice(0, 5).map((t, i) => ({
        uuid: `preview-${i}`,
        name: t.subject,
        type: 'Entity',
        properties: { predicate: t.predicate, object: t.object }
      }))
    }
    if (format === 'json') {
      const nodes = parseJson(data)
      return nodes.slice(0, 5).map((n, i) => ({
        uuid: `preview-${i}`,
        name: (n as any).name || `Node-${i}`,
        type: (n as any).type || 'Entity',
        properties: (n as any).properties || {}
      }))
    }
    // csv 预览：返回表头结构
    const lines = data.trim().split('\n')
    const headers = lines[0].split(',').map(h => h.trim())
    return headers.map((h, i) => ({
      uuid: `header-${i}`,
      name: h,
      type: 'Column',
      properties: {}
    }))
  },

  /**
   * 获取导入历史
   * 注意：后端暂无导入历史记录接口，当前返回 Mock 数据
   */
  async getImportHistory(graphId?: string): Promise<ImportTask[]> {
    if (graphId) {
      return mockImportTasks.filter(t => t.graphId === graphId)
    }
    return [...mockImportTasks]
  },

  /**
   * 添加单条数据（Episode）
   */
  async addData(req: AddDataReq): Promise<void> {
    await request.post('/admin/graphiti/data/add', req)
  },

  /**
   * 批量添加数据
   */
  async addDataBatch(req: AddDataBatchReq): Promise<void> {
    await request.post('/admin/graphiti/data/batch', req)
  },

  /**
   * 添加事实三元组
   */
  async addFactTriple(req: FactTripleReq): Promise<void> {
    await request.post('/admin/graphiti/data/fact-triple', req)
  },

  /**
   * 直接添加实体节点（不经 LLM 提取）
   */
  async addEntityNode(graphId: string, nodeData: AddEntityNodeReq): Promise<AddEntityNodeResp> {
    return request.post(`/admin/graphiti/data/entity-node?graphId=${graphId}`, nodeData)
  },

  // ==================== 导出功能（后端: GET /api/v1/graph/{graphId}/export） ====================

  /**
   * 导出图谱数据
   */
  async exportData(graphId: string, format: 'json' | 'csv' | 'triple'): Promise<ExportTask> {
    const resp = await request.get(`/api/v1/graph/${graphId}/export`) as any
    const nodes: any[] = resp?.nodes || []
    const edges: any[] = resp?.edges || []
    let content = ''
    if (format === 'json') {
      content = JSON.stringify({ nodes, edges }, null, 2)
    } else if (format === 'csv') {
      // CSV: 每行一个节点属性
      const headers = ['uuid', 'name', 'type']
      const rows = nodes.map((n: any) => [n.uuid, n.name, n.type].join(','))
      content = [headers.join(','), ...rows].join('\n')
    } else {
      // N-Triples
      const triples = edges.map((e: any) =>
        `<urn:uuid:${e.source}> <urn:uuid:${e.target}> "${e.name || ''}" .`
      )
      content = triples.join('\n')
    }
    const task: ExportTask = {
      id: `exp-${Date.now()}`,
      graphId,
      format,
      status: 'completed',
      fileName: `${graphId}-export.${format === 'triple' ? 'nt' : format}`,
      fileSize: new Blob([content]).size,
      createdAt: new Date().toISOString(),
      completedAt: new Date().toISOString()
    }
    // 下载文件
    const blob = new Blob([content], { type: format === 'json' ? 'application/json' : 'text/plain' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = task.fileName || `${graphId}-export.${format}`
    a.click()
    URL.revokeObjectURL(url)
    return task
  },

  /**
   * 获取导出历史（本地存储）
   */
  async getExportHistory(_graphId?: string): Promise<ExportTask[]> {
    const key = 'graphiti_export_history'
    try {
      const raw = localStorage.getItem(key)
      const all: ExportTask[] = raw ? JSON.parse(raw) : []
      return _graphId ? all.filter(t => t.graphId === _graphId) : all
    } catch {
      return []
    }
  },

  /**
   * 下载导出文件（本地存储）
   */
  async downloadExport(_taskId: string): Promise<Blob> {
    throw new Error('后端暂无导出文件下载接口，请使用 exportData 直接下载')
  },

  // ==================== 实体管理（后端: /api/v1/nodes） ====================

  /**
   * 获取实体列表
   */
  async listEntities(params: EntityListParams): Promise<EntityListResp> {
    if (!params.graphId) return { list: [], total: 0 }
    const graphId = params.graphId
    const page = params.page || 1
    const pageSize = params.pageSize || 10
    const resp = await request.get(`/api/v1/nodes/list?graphId=${graphId}&name=${encodeURIComponent(params.keyword || '')}&type=${encodeURIComponent(params.type || '')}&skip=${(page - 1) * pageSize}&limit=${pageSize}`) as any[]
    const list: EntityItem[] = (resp || []).map((n: any) => ({
      uuid: n.uuid,
      name: n.name,
      type: n.type,
      properties: n.properties || {},
      createdAt: n.createdAt,
      updatedAt: n.updatedAt
    }))
    return { list, total: list.length }
  },

  /**
   * 更新实体
   */
  async updateEntity(uuid: string, properties: Record<string, any>): Promise<EntityItem> {
    if (!properties.graphId) throw new Error('缺少 graphId')
    const graphId = properties.graphId
    delete properties.graphId
    const resp = await request.put(`/api/v1/nodes/${uuid}?graphId=${graphId}`, properties) as any
    return {
      uuid: resp?.uuid || uuid,
      name: resp?.name || (properties as any).name || '',
      type: resp?.type || (properties as any).type || '',
      properties: resp?.properties || properties
    }
  },

  /**
   * 删除实体
   */
  async deleteEntity(uuid: string, graphId: string): Promise<void> {
    await request.delete(`/api/v1/nodes/${uuid}?graphId=${graphId}`)
  },

  /**
   * 获取实体类型列表（从图谱节点类型聚合）
   */
  async getEntityTypes(graphId: string): Promise<string[]> {
    try {
    const resp = await request.get(`/api/v1/nodes/list?graphId=${graphId}&limit=1000`) as any[]
    const types = new Set((resp || []).map((n: any) => n.type).filter(Boolean))
    return Array.from(types) as string[]
    } catch {
      return []
    }
  }
}

// ==================== 数据解析工具函数 ====================

/**
 * 解析 JSON 格式数据
 * 期望格式: { nodes: [{name, type, properties}], edges: [...] }
 * 或直接为数组: [{name, type, properties}, ...]
 */
function parseJson(data: string): BatchDataItem[] {
  try {
    const parsed = JSON.parse(data)
    if (Array.isArray(parsed)) {
      return parsed.map(item => ({
        content: JSON.stringify(item),
        sourceType: 'json',
        name: (item as any).name
      }))
    }
    if (parsed.nodes && Array.isArray(parsed.nodes)) {
      return parsed.nodes.map((node: any) => ({
        content: JSON.stringify(node),
        sourceType: 'json',
        name: node.name
      }))
    }
    return [{ content: data, sourceType: 'json' }]
  } catch {
    return [{ content: data, sourceType: 'json' }]
  }
}

/**
 * 解析 CSV 格式数据
 * 第一行为表头，后续每行为一条数据
 */
function parseCsv(data: string): BatchDataItem[] {
  const lines = data.trim().split('\n')
  if (lines.length < 2) return [{ content: data, sourceType: 'csv' }]

  const headers = lines[0].split(',').map(h => h.trim())
  const items: BatchDataItem[] = []

  for (let i = 1; i < lines.length; i++) {
    const values = lines[i].split(',').map(v => v.trim())
    const obj: Record<string, string> = {}
    headers.forEach((h, idx) => {
      obj[h] = values[idx] || ''
    })
    items.push({
      content: JSON.stringify(obj),
      sourceType: 'csv',
      sourceDescription: `row ${i + 1}`,
      name: obj.name || `Row-${i}`
    })
  }
  return items
}

/**
 * 解析 N-Triples 格式数据
 * 格式: <http://example.org/s> <http://example.org/p> "o" .
 */
function parseNTriples(data: string): { subject: string; predicate: string; object: string }[] {
  const triples: { subject: string; predicate: string; object: string }[] = []
  const lines = data.trim().split('\n')

  for (const line of lines) {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) continue

    const match = trimmed.match(/<([^>]+)>\s*<([^>]+)>\s*(.+)\s*\.\s*$/)
    if (match) {
      const [, subject, predicate, objectRaw] = match
      let object = objectRaw.trim()
      // 去掉引号（字符串字面量）
      if ((object.startsWith('"') && object.endsWith('"')) ||
          (object.startsWith('"') && object.endsWith('" .'))) {
        object = object.replace(/^["']|["']\s*(?:\^\^|$)/g, '').replace(/\s*\.\s*$/, '')
        // 去掉语言标签或数据类型
        object = object.replace(/(@en|@\w+|\^\^<[^>]+>)\s*$/, '')
      }
      triples.push({ subject, predicate, object: object.trim() })
    }
  }
  return triples
}

export default dataApi
