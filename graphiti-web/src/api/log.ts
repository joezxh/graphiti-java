// 操作日志类型定义
export interface OperationLog {
  id: number
  userId: number
  username: string
  nickname: string
  operation: string
  method: string
  params: string
  ip: string
  location: string
  status: number // 0-失败 1-成功
  errorMsg: string
  duration: number // 耗时（毫秒）
  createdAt: string
}

export interface LogQuery {
  username?: string
  operation?: string
  status?: number
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

// Mock 数据
const mockLogs: OperationLog[] = [
  {
    id: 1,
    userId: 1,
    username: 'admin',
    nickname: '管理员',
    operation: '用户登录',
    method: 'POST /api/auth/login',
    params: '{"username":"admin","password":"***"}',
    ip: '127.0.0.1',
    location: '本地',
    status: 1,
    errorMsg: '',
    duration: 120,
    createdAt: '2024-05-08 10:00:00'
  },
  {
    id: 2,
    userId: 1,
    username: 'admin',
    nickname: '管理员',
    operation: '创建图谱',
    method: 'POST /api/graph/create',
    params: '{"name":"金融知识图谱","description":"金融领域知识图谱"}',
    ip: '127.0.0.1',
    location: '本地',
    status: 1,
    errorMsg: '',
    duration: 350,
    createdAt: '2024-05-07 15:30:00'
  },
  {
    id: 3,
    userId: 2,
    username: 'editor',
    nickname: '编辑员',
    operation: '导入数据',
    method: 'POST /api/data/import',
    params: '{"graphId":1,"format":"json"}',
    ip: '192.168.1.100',
    location: '北京',
    status: 1,
    errorMsg: '',
    duration: 1200,
    createdAt: '2024-05-07 14:20:00'
  },
  {
    id: 4,
    userId: 1,
    username: 'admin',
    nickname: '管理员',
    operation: '删除用户',
    method: 'DELETE /api/user/delete',
    params: '{"id":3}',
    ip: '127.0.0.1',
    location: '本地',
    status: 0,
    errorMsg: '权限不足',
    duration: 50,
    createdAt: '2024-05-06 09:15:00'
  },
  {
    id: 5,
    userId: 2,
    username: 'editor',
    nickname: '编辑员',
    operation: '执行检索',
    method: 'POST /api/search',
    params: '{"query":"人工智能","mode":"semantic"}',
    ip: '192.168.1.100',
    location: '北京',
    status: 1,
    errorMsg: '',
    duration: 800,
    createdAt: '2024-05-05 16:45:00'
  },
  {
    id: 6,
    userId: 1,
    username: 'admin',
    nickname: '管理员',
    operation: '更新配置',
    method: 'PUT /api/system/config',
    params: '{"id":1,"configValue":"新系统名称"}',
    ip: '127.0.0.1',
    location: '本地',
    status: 1,
    errorMsg: '',
    duration: 100,
    createdAt: '2024-05-04 11:20:00'
  }
]

// 模拟延迟
const delay = (ms: number = 500) => new Promise(resolve => setTimeout(resolve, ms))

// 操作日志 API
export const logApi = {
  // 获取操作日志列表
  getLogs: async (params: LogQuery = {}) => {
    await delay()
    
    let filteredLogs = [...mockLogs]
    
    if (params.username) {
      filteredLogs = filteredLogs.filter(l => l.username.includes(params.username!))
    }
    
    if (params.operation) {
      filteredLogs = filteredLogs.filter(l => l.operation.includes(params.operation!))
    }
    
    if (params.status !== undefined) {
      filteredLogs = filteredLogs.filter(l => l.status === params.status)
    }
    
    if (params.startTime) {
      filteredLogs = filteredLogs.filter(l => l.createdAt >= params.startTime!)
    }
    
    if (params.endTime) {
      filteredLogs = filteredLogs.filter(l => l.createdAt <= params.endTime!)
    }
    
    // 按创建时间倒序排序
    filteredLogs.sort((a, b) => {
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
    
    const pageNum = params.pageNum || 1
    const pageSize = params.pageSize || 10
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    
    return {
      list: filteredLogs.slice(start, end),
      total: filteredLogs.length,
      pageNum,
      pageSize
    }
  },
  
  // 获取操作日志详情
  getLog: async (id: number) => {
    await delay()
    
    const log = mockLogs.find(l => l.id === id)
    if (!log) {
      throw new Error('日志不存在')
    }
    
    return log
  },
  
  // 删除操作日志
  deleteLog: async (id: number) => {
    await delay()
    
    const index = mockLogs.findIndex(l => l.id === id)
    if (index === -1) {
      throw new Error('日志不存在')
    }
    
    mockLogs.splice(index, 1)
    
    return {}
  },
  
  // 清空操作日志
  clearLogs: async () => {
    await delay()
    
    mockLogs.length = 0
    
    return {}
  },
  
  // 导出操作日志
  exportLogs: async (_params: LogQuery = {}) => {
    await delay(1000)
    
    // 模拟导出，实际应该返回文件下载链接
    return { url: 'https://example.com/logs/export.xlsx' }
  }
}
