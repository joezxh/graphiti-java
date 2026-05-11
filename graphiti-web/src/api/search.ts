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

// 搜索历史（后端 VO）
export interface SearchHistory {
  id: number
  query: string
  mode: string
  resultCount: number
  createTime: string
}

// 搜索 API
export const searchApi = {
  /**
   * 执行搜索（全局/图谱级别）
   * 后端: POST /graph/search/global 或 /graph/search/graph/{graphId}
   */
  async search(params: SearchParams): Promise<SearchResult[]> {
    if (!params.graphId) {
      const resp = await request.post<{ facts?: any[]; nodes?: any[] }>('/graph/search/global', {
        query: params.query,
        maxFacts: params.limit || 20,
        config: { mode: params.mode || 'hybrid' }
      })
      return ((resp as any)?.facts || []).map(mapFactResult)
    }
    const resp = await request.post<{ facts?: any[]; nodes?: any[] }>(
      `/graph/search/graph/${params.graphId}`,
      {
        query: params.query,
        maxFacts: params.limit || 20,
        config: { mode: params.mode || 'hybrid' }
      }
    )
    return ((resp as any)?.facts || []).map(mapFactResult)
  },

  /**
   * 混合检索（语义 + 全文 + 图遍历）
   * 后端: POST /graph/search/hybrid/{graphId}
   */
  async hybridSearch(graphId: string, query: string, limit: number = 10): Promise<SearchResult[]> {
    const resp = await request.post<{ facts?: any[]; nodes?: any[] }>(
      `/graph/search/hybrid/${graphId}?query=${encodeURIComponent(query)}&limit=${limit}`
    )
    return ((resp as any)?.facts || []).map(mapFactResult)
  },

  /**
   * 语义搜索（向量相似度）
   * 后端: POST /graph/search/semantic/{graphId}
   */
  async semanticSearch(graphId: string, query: string, limit: number = 10): Promise<SearchResult[]> {
    const resp = await request.post<{ facts?: any[]; nodes?: any[] }>(
      `/graph/search/semantic/${graphId}?query=${encodeURIComponent(query)}&limit=${limit}`
    )
    return ((resp as any)?.facts || []).map(mapFactResult)
  },

  /**
   * BFS 搜索（图遍历）
   * 后端: POST /graph/search/bfs/{graphId}
   */
  async bfsSearch(graphId: string, query: string, depth: number = 2, limit: number = 10): Promise<SearchResult[]> {
    const resp = await request.post<{ facts?: any[]; nodes?: any[] }>(
      `/graph/search/bfs/${graphId}?query=${encodeURIComponent(query)}&depth=${depth}&limit=${limit}`
    )
    return ((resp as any)?.facts || []).map(mapFactResult)
  },

  /**
   * 获取搜索历史
   * 后端: GET /admin/graphiti/search-history/list
   */
  async getSearchHistory(pageNo: number = 1, pageSize: number = 20): Promise<{ list: SearchHistory[]; total: number }> {
    const resp = await request.get<any>('/admin/graphiti/search-history/list', {
      params: { pageNo, pageSize }
    })
    const data = resp as any
    return {
      list: (data?.list || []).map((h: any) => ({
        id: h.id,
        query: h.query || '',
        mode: h.mode || '',
        resultCount: h.resultCount ?? h.result_count ?? 0,
        createTime: h.createTime || h.create_time || ''
      })),
      total: data?.total || 0
    }
  },

  /**
   * 保存搜索历史
   * 后端: POST /admin/graphiti/search-history/save
   */
  async saveSearchHistory(query: string, mode: string, resultCount: number): Promise<void> {
    await request.post('/admin/graphiti/search-history/save', null, {
      params: { query, mode, resultCount }
    })
  },

  /**
   * 清空搜索历史
   * 后端: DELETE /admin/graphiti/search-history/clear
   */
  async clearSearchHistory(): Promise<void> {
    await request.delete('/admin/graphiti/search-history/clear')
  }
}

// 将后端 FactResultVO 映射为前端 SearchResult
function mapFactResult(fact: any): SearchResult {
  return {
    id: fact.uuid || fact.id || '',
    type: 'edge',
    name: fact.name || fact.fact || '',
    properties: { fact: fact.fact },
    score: fact.score || 0,
    source: fact.sourceNodeUuid,
    target: fact.targetNodeUuid,
    highlight: undefined
  }
}

export default searchApi
