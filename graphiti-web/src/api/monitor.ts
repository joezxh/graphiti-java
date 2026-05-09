// 系统监控类型定义
export interface SystemStatus {
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  neo4jStatus: string
  mysqlStatus: string
  redisStatus: string
  uptime: number
  currentTime: string
}

export interface PerformanceMetrics {
  id: number
  timestamp: string
  cpuUsage: number
  memoryUsage: number
  diskUsage: number
  activeUsers: number
  requestCount: number
  avgResponseTime: number
}

export interface ApiLog {
  id: number
  method: string
  path: string
  statusCode: number
  responseTime: number
  ip: string
  timestamp: string
}

// Mock 数据 - 系统状态
const mockSystemStatus: SystemStatus = {
  cpuUsage: 35.6,
  memoryUsage: 68.2,
  diskUsage: 42.8,
  neo4jStatus: 'healthy',
  mysqlStatus: 'healthy',
  redisStatus: 'healthy',
  uptime: 86400, // 24小时（秒）
  currentTime: '2024-05-08 10:30:00'
}

// Mock 数据 - 性能指标
const mockPerformanceMetrics: PerformanceMetrics[] = []
const now = new Date()

// 生成最近24小时的性能数据（每小时一个点）
for (let i = 0; i < 24; i++) {
  const timestamp = new Date(now.getTime() - (23 - i) * 60 * 60 * 1000)
  
  mockPerformanceMetrics.push({
    id: i + 1,
    timestamp: timestamp.toISOString().split('T')[0] + ' ' + timestamp.toTimeString().split(' ')[0],
    cpuUsage: Math.random() * 40 + 20, // 20-60%
    memoryUsage: Math.random() * 30 + 50, // 50-80%
    diskUsage: Math.random() * 10 + 40, // 40-50%
    activeUsers: Math.floor(Math.random() * 10) + 1,
    requestCount: Math.floor(Math.random() * 100) + 50,
    avgResponseTime: Math.floor(Math.random() * 200) + 100
  })
}

// Mock 数据 - API日志
const mockApiLogs: ApiLog[] = [
  {
    id: 1,
    method: 'GET',
    path: '/api/graph/list',
    statusCode: 200,
    responseTime: 120,
    ip: '127.0.0.1',
    timestamp: '2024-05-08 10:30:00'
  },
  {
    id: 2,
    method: 'POST',
    path: '/api/data/import',
    statusCode: 200,
    responseTime: 1200,
    ip: '192.168.1.100',
    timestamp: '2024-05-08 10:25:00'
  },
  {
    id: 3,
    method: 'GET',
    path: '/api/search',
    statusCode: 200,
    responseTime: 350,
    ip: '192.168.1.100',
    timestamp: '2024-05-08 10:20:00'
  },
  {
    id: 4,
    method: 'PUT',
    path: '/api/system/config',
    statusCode: 200,
    responseTime: 80,
    ip: '127.0.0.1',
    timestamp: '2024-05-08 10:15:00'
  },
  {
    id: 5,
    method: 'DELETE',
    path: '/api/user/delete',
    statusCode: 403,
    responseTime: 50,
    ip: '192.168.1.101',
    timestamp: '2024-05-08 10:10:00'
  }
]

// 模拟延迟
const delay = (ms: number = 500) => new Promise(resolve => setTimeout(resolve, ms))

// 系统监控 API
export const monitorApi = {
  // 获取系统状态
  getSystemStatus: async () => {
    await delay(300)
    
    // 模拟实时数据变化
    const status = { ...mockSystemStatus }
    status.cpuUsage = Math.random() * 40 + 20
    status.memoryUsage = Math.random() * 30 + 50
    status.diskUsage = Math.random() * 10 + 40
    status.currentTime = new Date().toISOString().split('T')[0] + ' ' + new Date().toTimeString().split(' ')[0]
    
    return status
  },
  
  // 获取性能指标
  getPerformanceMetrics: async (timeRange: string = '24h') => {
    await delay(500)
    
    let filteredMetrics = [...mockPerformanceMetrics]
    
    // 根据时间范围筛选
    if (timeRange === '1h') {
      filteredMetrics = filteredMetrics.slice(-1)
    } else if (timeRange === '6h') {
      filteredMetrics = filteredMetrics.slice(-6)
    } else if (timeRange === '7d') {
      // 模拟7天数据
      const sevenDayMetrics = []
      for (let i = 0; i < 7; i++) {
        const date = new Date(now.getTime() - (6 - i) * 24 * 60 * 60 * 1000)
        sevenDayMetrics.push({
          id: i + 100,
          timestamp: date.toISOString().split('T')[0] + ' 00:00:00',
          cpuUsage: Math.random() * 40 + 20,
          memoryUsage: Math.random() * 30 + 50,
          diskUsage: Math.random() * 10 + 40,
          activeUsers: Math.floor(Math.random() * 10) + 1,
          requestCount: Math.floor(Math.random() * 1000) + 500,
          avgResponseTime: Math.floor(Math.random() * 200) + 100
        })
      }
      filteredMetrics = sevenDayMetrics
    }
    
    return filteredMetrics
  },
  
  // 获取API日志
  getApiLogs: async (params: { pageNum?: number; pageSize?: number } = {}) => {
    await delay()
    
    const pageNum = params.pageNum || 1
    const pageSize = params.pageSize || 10
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    
    return {
      list: mockApiLogs.slice(start, end),
      total: mockApiLogs.length,
      pageNum,
      pageSize
    }
  },
  
  // 获取数据库状态
  getDatabaseStatus: async () => {
    await delay(300)
    
    return {
      neo4j: {
        status: 'healthy',
        version: '5.0.0',
        nodes: 1250,
        relationships: 3680,
        storage: '256 MB'
      },
      mysql: {
        status: 'healthy',
        version: '8.0.33',
        databases: 2,
        tables: 15,
        storage: '128 MB'
      },
      redis: {
        status: 'healthy',
        version: '7.0.0',
        keys: 156,
        memory: '32 MB'
      }
    }
  }
}
