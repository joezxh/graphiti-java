// 搜索参数
export interface SearchParams {
  query: string
  mode: 'semantic' | 'structured' | 'hybrid'
  graphId?: string
  filters?: SearchFilter[]
  limit?: number
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
  // 执行搜索
  async search(params: SearchParams): Promise<SearchResult[]> {
    await delay(600)

    // 模拟搜索结果
    const mockResults: SearchResult[] = [
      {
        id: 'node-001',
        type: 'node',
        name: '张三',
        entityType: 'Person',
        properties: { name: '张三', age: 30, email: 'zhangsan@example.com' },
        score: 0.95,
        highlight: { name: ['<em>张三</em>'] }
      },
      {
        id: 'node-002',
        type: 'node',
        name: '李四',
        entityType: 'Person',
        properties: { name: '李四', age: 28, email: 'lisi@example.com' },
        score: 0.72
      },
      {
        id: 'edge-001',
        type: 'edge',
        name: 'WORKS_AT',
        relationType: 'WORKS_AT',
        properties: { since: '2020-01-01', position: '工程师' },
        score: 0.68,
        source: 'node-001',
        target: 'node-003'
      },
      {
        id: 'node-003',
        type: 'node',
        name: 'ABC科技',
        entityType: 'Company',
        properties: { name: 'ABC科技', industry: '互联网', founded: '2018-05-10' },
        score: 0.55
      }
    ]

    // 根据查询词过滤
    if (params.query) {
      const kw = params.query.toLowerCase()
      const filtered = mockResults.filter(r =>
        r.name.toLowerCase().includes(kw) ||
        JSON.stringify(r.properties).toLowerCase().includes(kw)
      )

      // 模拟结构化过滤
      if (params.filters && params.filters.length > 0) {
        return filtered.filter(r => {
          return params.filters!.every(f => {
            const val = r.properties[f.field]
            switch (f.operator) {
              case 'eq': return val === f.value
              case 'gt': return val > f.value
              case 'gte': return val >= f.value
              case 'lt': return val < f.value
              case 'lte': return val <= f.value
              case 'contains': return String(val).includes(f.value)
              default: return true
            }
          })
        })
      }

      return filtered.slice(0, params.limit || 20)
    }

    return mockResults.slice(0, params.limit || 20)
  },

  // 获取搜索历史
  async getSearchHistory(): Promise<SearchHistory[]> {
    await delay(300)
    return [...mockSearchHistory]
  },

  // 保存搜索历史
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
    // 只保留最近 20 条
    if (mockSearchHistory.length > 20) {
      mockSearchHistory = mockSearchHistory.slice(0, 20)
    }
    return item
  },

  // 清空搜索历史
  async clearSearchHistory(): Promise<void> {
    await delay(200)
    mockSearchHistory = []
  }
}

export default searchApi
