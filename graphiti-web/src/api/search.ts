import request from './request'

// 搜索参数
export interface SearchParams {
  query: string
  mode: 'semantic' | 'structured' | 'hybrid' | 'bm25' | 'vector' | 'bfs'
  graphId?: string
  filters?: SearchFilter[]
  limit?: number
  depth?: number
}

// 搜索过滤条件
export interface SearchFilter {
  field: string
  operator: 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte' | 'contains' | 'in'
  value: any
}

// 搜索结果
export interface SearchResult {
  id: string
  type: 'node' | 'edge'
  name: string
  entityType?: string
  relationType?: string
  properties: Record<string, any>
  score: number
  source?: string
  target?: string
  highlight?: Record<string, string[]>
}

// 搜索历史
export interface SearchHistory {
  id: string
  query: string
  mode: string
  resultCount: number
  createdAt: string
}

// Mock 数据
let mockSearchHistory: SearchHistory[] = [
  {
    id: 'sh-001',
    query: '张三',
    mode: 'semantic',
    resultCount: 3,
    createdAt: '2024-03-24T10:00:00Z'
  },
  {
    id: 'sh-002',
    query: 'age > 25',
    mode: 'structured',
    resultCount: 5,
    createdAt: '2024-03-23T14:30:00Z'
  },
  {
    id: 'sh-003',
    query: '科技公司创始人',
    mode: 'hybrid',
    resultCount: 2,
    createdAt: '2024-03-22T09:15:00Z'
  }
]

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

// 混合检索 API
export const searchApi = {
  // 执行搜索（全局/图谱级别）
  async search(params: SearchParams): Promise<SearchResult[]> {
    if (!params.graphId) {
      const resp = await request.post('/admin/graphiti/search/global', {
        query: params.query,
        maxFacts: params.limit || 20,
        config: { mode: params.mode || 'hybrid' }
      })
      return resp.data?.edges || []
    }
    const resp = await request.post(`/admin/graphiti/search/graph/${params.graphId}`, {
      query: params.query,
      maxFacts: params.limit || 20,
      config: { mode: params.mode || 'hybrid' }
    })
    return resp.data?.edges || []
  },

  // 混合检索
  async hybridSearch(graphId: string, query: string, limit: number = 10): Promise<SearchResult[]> {
    const resp = await request.post(`/admin/graphiti/search/hybrid/${graphId}?query=${encodeURIComponent(query)}&limit=${limit}`)
    return resp.data?.edges || []
  },

  // 语义搜索
  async semanticSearch(graphId: string, query: string, limit: number = 10): Promise<SearchResult[]> {
    const resp = await request.post(`/admin/graphiti/search/semantic/${graphId}?query=${encodeURIComponent(query)}&limit=${limit}`)
    return resp.data?.edges || []
  },

  // BFS 搜索
  async bfsSearch(graphId: string, query: string, depth: number = 2, limit: number = 10): Promise<SearchResult[]> {
    const resp = await request.post(`/admin/graphiti/search/bfs/${graphId}?query=${encodeURIComponent(query)}&depth=${depth}&limit=${limit}`)
    return resp.data?.edges || []
  },

  // 获取搜索历史（Mock）
  async getSearchHistory(): Promise<SearchHistory[]> {
    await delay(300)
    return [...mockSearchHistory]
  },

  // 保存搜索历史（Mock）
  async saveSearchHistory(query: string, mode: string, resultCount: number): Promise<SearchHistory> {
    await delay(200)
    const item: SearchHistory = {
      id: `sh-${Date.now()}`,
      query,
      mode,
      resultCount,
      createdAt: new Date().toISOString()
    }
    mockSearchHistory.unshift(item)
    if (mockSearchHistory.length > 20) {
      mockSearchHistory = mockSearchHistory.slice(0, 20)
    }
    return item
  },

  // 清空搜索历史（Mock）
  async clearSearchHistory(): Promise<void> {
    await delay(200)
    mockSearchHistory = []
  }
}

export default searchApi
