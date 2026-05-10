import request from './request'

// 类型定义
export interface Graph {
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
    return request.get('/graph/list')
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

  // 获取边列表
  async getEdges(graphId: string): Promise<Edge[]> {
    return request.get(`/graph/${graphId}/edges`)
  },

  // 添加数据
  async addData(graphId: string, data: ImportDataReq): Promise<void> {
    return request.post(`/graph/${graphId}/data`, data)
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

  // 克隆图谱
  async clone(graphId: string): Promise<Graph> {
    return request.post(`/graph/${graphId}/clone`)
  },

  // 历史状态查询
  async getHistory(graphId: string, time: string): Promise<{ nodes: any[]; edges: any[] }> {
    return request.get(`/graph/${graphId}/history?time=${time}`)
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
