import request from './request'

// 类型定义（扩展自 api/graph.ts）
export interface Graph {
  id?: string        // 兼容前端旧代码（实际值为 graphId）
  graphId: string
  name: string
  description?: string
  nodeCount?: number
  edgeCount?: number
  episodeCount?: number
  createdAt?: string
  updatedAt?: string
}

// IDE 相关类型
export interface GraphVisualizationData {
  nodes: GraphIDENode[]
  edges: GraphIDEEdge[]
  pagination?: {
    page: number
    pageSize: number
    total: number
    totalPages: number
  }
  aggregations?: {
    byClass: Array<{ type: string; count: number }>
  }
}

export interface GraphIDENode {
  uuid: string
  name: string
  type: string
  x?: number
  y?: number
  properties?: Record<string, any>
  summary?: string
  createdAt?: string
  updatedAt?: string
}

export interface GraphIDEEdge {
  uuid: string
  source: string
  target: string
  type: string
  fact?: string
  properties?: Record<string, any>
}

export interface ClassInstance {
  uuid: string
  name: string
  type: string
  properties?: Record<string, any>
  summary?: string
  createdAt?: string
  updatedAt?: string
}

export interface GraphMetadata {
  graphId: string
  name: string
  description?: string
  status: 'ACTIVE' | 'INACTIVE' | 'DRAFT'
  nodeCount: number
  edgeCount: number
  classCount: number
  episodeCount: number
  communityCount: number
}

export interface SchemaClass {
  id: number
  definitionId: number
  classUri: string
  localName: string
  description?: string
  parentClassIds: number[]
  propertyCount: number
  properties?: SchemaProperty[]
}

export interface SchemaProperty {
  id: number
  definitionId: number
  localName: string
  propertyType: 'DATATYPE' | 'OBJECT' | 'ANNOTATION'
  rangeDataType?: string
  domainClassId?: number
  rangeClassId?: number
  isRequired: boolean
  isMultiple: boolean
  defaultValue?: any
  allowedValues?: any[]
  pattern?: string
  minValue?: number
  maxValue?: number
}

export interface CascadeFilter {
  classType: string
  conditions: PropertyCondition[]
  logic: 'AND' | 'OR'
}

export interface PropertyCondition {
  propertyName: string
  operator: 'eq' | 'ne' | 'gt' | 'lt' | 'gte' | 'lte' | 'contains' | 'not_contains' | 'in' | 'not_in' | 'is_null' | 'is_not_null'
  value: any
}

export interface CascadePreviewResponse {
  totalMatch: number
  distribution: Array<{ groupBy: string; value: string; count: number }>
}

export interface CascadeExecuteResponse {
  success: boolean
  affectedCount: number
  failedCount: number
  errors: string[]
}

export interface Node {
  uuid: string
  name: string
  type: string
  properties?: Record<string, any>
}

export interface Edge {
  uuid: string
  source: string
  target: string
  type: string
  weight?: number
  properties?: Record<string, any>
}

export interface GraphStats {
  totalGraphs: number
  totalNodes: number
  totalEdges: number
  totalEpisodes: number
  nodeTrend?: number
  edgeTrend?: number
  episodeTrend?: number
}

export interface CreateGraphReq {
  name: string
  description?: string
}

export interface ImportDataReq {
  format: 'json' | 'csv' | 'triple'
  data: any
}

// API 对象（符合设计文档要求）
export const graphApi = {
  // 获取图谱列表
  async getList(): Promise<Graph[]> {
    const data = await request.get<{ graphs: Graph[]; totalCount: number; rowCount: number }>('/graph/list')
    return data?.graphs ?? []
  },

  // 获取图谱详情
  async getDetail(id: string): Promise<Graph> {
    return request.get(`/graph/${id}`)
  },

  // 创建图谱
  async create(data: CreateGraphReq): Promise<Graph> {
    return request.post('/graph/create', data)
  },

  // 更新图谱
  async update(id: string, data: Partial<CreateGraphReq>): Promise<Graph> {
    return request.put(`/graph/${id}`, data)
  },

  // 删除图谱
  async delete(id: string): Promise<void> {
    return request.delete(`/graph/${id}`)
  },

  // 获取节点列表
  async getNodes(graphId: string): Promise<Node[]> {
    return request.get(`/graph/${graphId}/nodes`)
  },

  // 获取图谱统计信息（按 graphId）
  async getStats(graphId: string): Promise<{ nodeCount: number; edgeCount: number }> {
    return request.get(`/graph/${graphId}/stats`)
  },

  // 获取边列表
  async getEdges(graphId: string): Promise<Edge[]> {
    return request.get(`/graph/${graphId}/edges`)
  },

  // 添加数据（调用后端 POST /graph/data/add）
  async addData(graphId: string, data: ImportDataReq): Promise<void> {
    return request.post('/graph/data/add', { graphId, ...data })
  },

  // 导出数据
  async exportData(graphId: string): Promise<Blob> {
    return request.get(`/graph/${graphId}/export`, {
      responseType: 'blob'
    })
  },

  // 清空图谱
  async clear(graphId: string): Promise<void> {
    return request.post(`/graph/${graphId}/clear`)
  },

  // 构建社区
  async buildCommunity(graphId: string): Promise<{ communityCount: number; message: string }> {
    return request.post(`/graph/${graphId}/communities/build`)
  },

  // 获取社区列表
  async getCommunities(graphId: string): Promise<any[]> {
    return request.get(`/graph/${graphId}/communities`)
  },

  // 搜索社区
  async searchCommunities(graphId: string, query: string): Promise<any[]> {
    return request.get(`/graph/${graphId}/communities/search?query=${encodeURIComponent(query)}`)
  },

  // 克隆图谱
  async clone(graphId: string): Promise<Graph> {
    return request.post(`/graph/${graphId}/clone`)
  },

  // 历史状态查询
  async getHistory(graphId: string, time: number): Promise<{ nodes: any[]; edges: any[] }> {
    return request.get(`/graph/${graphId}/history?time=${time}`)
  },

  // ===== Episode 操作 =====

  // 获取 Episode 列表
  async getEpisodes(graphId: string, skip?: number, limit?: number): Promise<any[]> {
    const data = await request.get<{ episodes: any[]; totalCount: number; rowCount: number }>(
      `/graph/episode/list/${graphId}`,
      { params: { skip: skip || 0, limit: limit || 20 } }
    )
    return data?.episodes ?? []
  },

  // 获取 Episode 详情
  async getEpisodeDetail(graphId: string, episodeUuid: string): Promise<any> {
    return request.get(`/graph/episode/${graphId}/${episodeUuid}`)
  },

  // 获取 Episode 提及的节点和边
  async getEpisodeMentions(graphId: string, episodeUuid: string): Promise<{ nodes: any[]; edges: any[] }> {
    return request.get(`/graph/episode/${graphId}/${episodeUuid}/mentions`)
  },

  // 删除 Episode
  async deleteEpisode(graphId: string, episodeUuid: string): Promise<void> {
    return request.delete(`/graph/episode/${graphId}/${episodeUuid}`)
  },

  // ===== Ontology 操作 =====

  // 获取图谱本体定义
  async getOntology(graphId: string): Promise<any> {
    return request.get(`/ontology/${graphId}`)
  },

  // 设置图谱本体定义
  async setOntology(graphId: string, ontology: any): Promise<any> {
    return request.post(`/ontology/${graphId}`, ontology)
  },

  // ===== Custom Instruction 操作 =====

  // 获取自定义指令列表
  async getCustomInstructions(graphId?: string): Promise<any[]> {
    return request.get('/custom-instructions', {
      params: { graphId: graphId || undefined }
    })
  },

  // 创建自定义指令
  async createCustomInstruction(data: { instruction: string; graphId?: string }): Promise<any> {
    return request.post('/custom-instructions', data)
  },

  // 删除自定义指令
  async deleteCustomInstruction(id: string): Promise<void> {
    return request.delete(`/custom-instructions/${id}`)
  },

  // ===== Graph IDE API =====

  // 获取图谱可视化数据
  async getVisualization(
    graphId: string,
    params?: {
      layout?: string
      page?: number
      pageSize?: number
      classType?: string
      keyword?: string
    }
  ): Promise<GraphVisualizationData> {
    return request.get(`/graph/${graphId}/visualization`, { params })
  },

  // 获取图谱元数据
  async getGraphMetadata(graphId: string): Promise<GraphMetadata> {
    return request.get(`/graph/${graphId}/metadata`)
  },

  // 获取节点详情
  async getNodeDetail(graphId: string, uuid: string): Promise<GraphIDENode & { relations: any[] }> {
    return request.get(`/graph/${graphId}/nodes/${uuid}`)
  },

  // 创建节点
  async createNode(
    graphId: string,
    data: { name: string; type: string; properties?: Record<string, any> }
  ): Promise<GraphIDENode> {
    return request.post(`/graph/${graphId}/nodes`, data)
  },

  // 更新节点
  async updateNode(
    graphId: string,
    uuid: string,
    data: { name?: string; properties?: Record<string, any> }
  ): Promise<GraphIDENode> {
    return request.put(`/graph/${graphId}/nodes/${uuid}`, data)
  },

  // 删除节点
  async deleteNode(graphId: string, uuid: string): Promise<void> {
    return request.delete(`/graph/${graphId}/nodes/${uuid}`)
  },

  // 创建边
  async createEdge(
    graphId: string,
    data: { sourceUuid: string; targetUuid: string; type: string; fact?: string }
  ): Promise<GraphIDEEdge> {
    return request.post(`/graph/${graphId}/edges`, data)
  },

  // 展开邻居节点
  async expandNeighbors(
    graphId: string,
    uuid: string,
    options?: { depth?: number; edgeTypes?: string[]; maxNodes?: number }
  ): Promise<{ nodes: GraphIDENode[]; edges: GraphIDEEdge[] }> {
    return request.post(`/graph/${graphId}/nodes/${uuid}/expand`, options || {})
  },

  // ===== Schema API =====

  // 获取类列表
  async getSchemaClasses(graphId: string): Promise<SchemaClass[]> {
    return request.get(`/graph/${graphId}/ontology/classes`)
  },

  // 获取类详情
  async getSchemaClassDetail(graphId: string, classId: number): Promise<SchemaClass> {
    return request.get(`/graph/${graphId}/ontology/classes/${classId}`)
  },

  // 获取类的实例数据列表
  async getClassInstances(
    graphId: string,
    classType: string,
    params?: { page?: number; pageSize?: number; keyword?: string }
  ): Promise<{ data: ClassInstance[]; total: number }> {
    return request.get(`/graph/${graphId}/instances`, {
      params: { classType, ...params }
    })
  },

  // 创建类
  async createSchemaClass(
    graphId: string,
    data: { localName: string; description?: string; parentClassIds?: number[] }
  ): Promise<SchemaClass> {
    return request.post(`/graph/${graphId}/ontology/classes`, data)
  },

  // 更新类
  async updateSchemaClass(
    graphId: string,
    classId: number,
    data: { localName?: string; description?: string; parentClassIds?: number[] }
  ): Promise<SchemaClass> {
    return request.put(`/graph/${graphId}/ontology/classes/${classId}`, data)
  },

  // 删除类
  async deleteSchemaClass(graphId: string, classId: number): Promise<void> {
    return request.delete(`/graph/${graphId}/ontology/classes/${classId}`)
  },

  // 获取类属性列表
  async getClassProperties(graphId: string, classId: number): Promise<SchemaProperty[]> {
    return request.get(`/graph/${graphId}/ontology/classes/${classId}/properties`)
  },

  // 创建属性
  async createClassProperty(
    graphId: string,
    classId: number,
    data: {
      localName: string
      propertyType?: string
      rangeDataType?: string
      isRequired?: boolean
      defaultValue?: any
      allowedValues?: any[]
      pattern?: string
      minValue?: number
      maxValue?: number
    }
  ): Promise<SchemaProperty> {
    return request.post(`/graph/${graphId}/ontology/classes/${classId}/properties`, data)
  },

  // 更新属性
  async updateClassProperty(
    graphId: string,
    classId: number,
    propertyId: number,
    data: Partial<SchemaProperty>
  ): Promise<SchemaProperty> {
    return request.put(`/graph/${graphId}/ontology/classes/${classId}/properties/${propertyId}`, data)
  },

  // 删除属性
  async deleteClassProperty(graphId: string, classId: number, propertyId: number): Promise<void> {
    return request.delete(`/graph/${graphId}/ontology/classes/${classId}/properties/${propertyId}`)
  },

  // ===== Cascade Edit API =====

  // 预览级联编辑影响范围
  async previewCascade(
    graphId: string,
    data: CascadeFilter
  ): Promise<CascadePreviewResponse> {
    return request.post(`/graph/${graphId}/cascade/preview`, data)
  },

  // 执行级联编辑
  async executeCascade(
    graphId: string,
    data: {
      classType: string
      conditions: PropertyCondition[]
      logic: 'AND' | 'OR'
      updates: Record<string, any>
    }
  ): Promise<CascadeExecuteResponse> {
    return request.post(`/graph/${graphId}/cascade/execute`, data)
  },

  // ===== Schema Validation API =====

  // 验证 Schema 变更影响
  async validateSchemaChange(
    graphId: string,
    data: {
      type: 'UPDATE_CLASS' | 'UPDATE_PROPERTY' | 'DELETE_PROPERTY' | 'ADD_REQUIRED_PROPERTY'
      classId?: number
      propertyId?: number
      changes?: {
        newLocalName?: string
        oldIsRequired?: boolean
        newIsRequired?: boolean
        newRangeDataType?: string
        newAllowedValues?: string[]
        newPattern?: string
        newMinValue?: number
        newMaxValue?: number
      }
    }
  ): Promise<{
    compatible: boolean
    affectedNodes: number
    violations: Array<{
      nodeUuid?: string
      nodeName?: string
      violationType: string
      reason: string
      currentValue?: any
      expectedValue?: any
    }>
  }> {
    return request.post(`/graph/${graphId}/ontology/validate-change`, data)
  }
}

// 获取图谱统计信息
export async function getGraphStats(): Promise<GraphStats> {
  return request.get('/graph/stats')
}

// 获取图谱列表（命名导出）
export async function listGraphs(): Promise<Graph[]> {
  return graphApi.getList()
}

// 默认导出
export default graphApi
