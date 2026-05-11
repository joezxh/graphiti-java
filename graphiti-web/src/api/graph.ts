import request from './request'

// 类型定义
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
