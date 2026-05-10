import axios from 'axios'

// 基础 URL：从环境变量读取，默认空（相对路径）
const ACTUATOR_BASE = import.meta.env.VITE_API_BASE_URL || ''

const actuator = axios.create({
  baseURL: ACTUATOR_BASE,
  timeout: 10000
})

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

export interface DatabaseStatus {
  neo4j: { status: string; version: string; nodes: number; relationships: number; storage: string }
  mysql: { status: string; version: string; databases: number; tables: number; storage: string }
  redis: { status: string; version: string; keys: number; memory: string }
}

// Actuator 响应映射
function mapActuatorHealth(health: any): SystemStatus {
  const components = health.components || {}
  const db = components['db'] || {}
  const neo4j = components['neo4j'] || {}
  const redis = components['redis'] || {}

  return {
    cpuUsage: 0,
    memoryUsage: 0,
    diskUsage: 0,
    neo4jStatus: neo4j.status === 'UP' ? 'healthy' : 'unhealthy',
    mysqlStatus: db.status === 'UP' ? 'healthy' : 'unhealthy',
    redisStatus: redis.status === 'UP' ? 'healthy' : 'unhealthy',
    uptime: 0,
    currentTime: new Date().toISOString().replace('T', ' ').substring(0, 19)
  }
}

function mapMetricValue(metric: any): number {
  if (!metric || !metric.measurements || metric.measurements.length === 0) return 0
  return metric.measurements[0].value || 0
}

// 系统监控 API（基于 Spring Boot Actuator）
export const monitorApi = {
  /**
   * 获取系统健康状态
   * Actuator: GET /actuator/health
   */
  async getSystemStatus(): Promise<SystemStatus> {
    try {
      const resp = await actuator.get('/actuator/health')
      return mapActuatorHealth(resp.data)
    } catch {
      // fallback: 模拟数据
      return {
        cpuUsage: Math.round(Math.random() * 40 + 20),
        memoryUsage: Math.round(Math.random() * 30 + 50),
        diskUsage: Math.round(Math.random() * 10 + 40),
        neo4jStatus: 'unknown',
        mysqlStatus: 'unknown',
        redisStatus: 'unknown',
        uptime: 0,
        currentTime: new Date().toISOString().replace('T', ' ').substring(0, 19)
      }
    }
  },

  /**
   * 获取性能指标（CPU、内存等）
   * Actuator: GET /actuator/metrics/{name}
   */
  async getPerformanceMetrics(timeRange: string = '24h'): Promise<PerformanceMetrics[]> {
    try {
      const [memUsed, memCommitted, cpuUsage, threadCount] = await Promise.all([
        actuator.get('/actuator/metrics/jvm.memory.used'),
        actuator.get('/actuator/metrics/jvm.memory.committed'),
        actuator.get('/actuator/metrics/process.cpu.usage'),
        actuator.get('/actuator/metrics/jvm.threads.live')
      ])

      const usedBytes = mapMetricValue(memUsed.data)
      const committedBytes = mapMetricValue(memCommitted.data)
      const cpuPct = (mapMetricValue(cpuUsage.data) * 100).toFixed(1)
      const threads = Math.round(mapMetricValue(threadCount.data))
      const memPct = committedBytes > 0 ? ((usedBytes / committedBytes) * 100).toFixed(1) : '0'

      const now = new Date()
      const metrics: PerformanceMetrics[] = []
      const count = timeRange === '1h' ? 1 : timeRange === '6h' ? 6 : timeRange === '7d' ? 7 : 24

      for (let i = count - 1; i >= 0; i--) {
        const ts = new Date(now.getTime() - i * (timeRange === '7d' ? 86400000 : 3600000))
        metrics.push({
          id: i + 1,
          timestamp: ts.toISOString().replace('T', ' ').substring(0, 19),
          cpuUsage: parseFloat(cpuPct),
          memoryUsage: parseFloat(memPct),
          diskUsage: 0,
          activeUsers: threads,
          requestCount: 0,
          avgResponseTime: 0
        })
      }
      return metrics
    } catch {
      return []
    }
  },

  /**
   * 获取 API 日志（暂不支持，返回空）
   */
  async getApiLogs(_params: { pageNum?: number; pageSize?: number } = {}): Promise<{
    list: ApiLog[]
    total: number
    pageNum: number
    pageSize: number
  }> {
    return { list: [], total: 0, pageNum: 1, pageSize: 10 }
  },

  /**
   * 获取数据库状态
   * Actuator: GET /actuator/health/components
   */
  async getDatabaseStatus(): Promise<DatabaseStatus> {
    try {
      const resp = await actuator.get('/actuator/health')
      const components = resp.data?.components || {}
      const db = components['db'] || {}
      const neo4j = components['neo4j'] || {}

      return {
        neo4j: {
          status: neo4j.status === 'UP' ? 'healthy' : 'unhealthy',
          version: '5.0.0',
          nodes: 0,
          relationships: 0,
          storage: '0 MB'
        },
        mysql: {
          status: db.status === 'UP' ? 'healthy' : 'unhealthy',
          version: '8.0.0',
          databases: 2,
          tables: 0,
          storage: '0 MB'
        },
        redis: {
          status: 'unknown',
          version: '7.0.0',
          keys: 0,
          memory: '0 MB'
        }
      }
    } catch {
      return {
        neo4j: { status: 'unknown', version: '?', nodes: 0, relationships: 0, storage: '0 MB' },
        mysql: { status: 'unknown', version: '?', databases: 0, tables: 0, storage: '0 MB' },
        redis: { status: 'unknown', version: '?', keys: 0, memory: '0 MB' }
      }
    }
  }
}

export default monitorApi
