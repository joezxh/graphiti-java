import request from './request'

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
  createTime: string
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

// 操作日志 API（调用后端 /admin/system/log/*）
export const logApi = {
  /**
   * 获取操作日志列表
   * 后端: GET /admin/system/log/list
   */
  async getLogs(params: LogQuery = {}): Promise<{
    list: OperationLog[]
    total: number
    pageNum: number
    pageSize: number
  }> {
    const resp = await request.get<any>('/admin/system/log/list', {
      params: {
        pageNo: params.pageNum || 1,
        pageSize: params.pageSize || 10,
        username: params.username || undefined,
        operation: params.operation || undefined,
        status: params.status !== undefined ? params.status : undefined,
        startTime: params.startTime || undefined,
        endTime: params.endTime || undefined
      }
    })
    return {
      list: ((resp as any)?.list || resp || []).map((log: any) => ({
        id: log.id,
        userId: log.userId,
        username: log.username || '',
        nickname: log.username || '',
        operation: log.operation || '',
        method: log.method || '',
        params: log.params || '',
        ip: log.ip || '',
        location: log.location || '',
        status: log.status ?? 1,
        errorMsg: log.errorMsg || '',
        duration: log.duration ?? 0,
        createTime: log.createTime || log.create_time || ''
      })),
      total: (resp as any)?.total || 0,
      pageNum: (resp as any)?.pageNum || (params.pageNum || 1),
      pageSize: (resp as any)?.pageSize || (params.pageSize || 10)
    }
  },

  /**
   * 获取操作日志详情
   * 后端: GET /admin/system/log/{id}
   */
  async getLog(id: number): Promise<OperationLog> {
    const resp = await request.get<any>(`/admin/system/log/${id}`)
    const log = resp as any
    return {
      id: log.id,
      userId: log.userId,
      username: log.username || '',
      nickname: log.username || '',
      operation: log.operation || '',
      method: log.method || '',
      params: log.params || '',
      ip: log.ip || '',
      location: log.location || '',
      status: log.status ?? 1,
      errorMsg: log.errorMsg || '',
      duration: log.duration ?? 0,
      createTime: log.createTime || log.create_time || ''
    }
  },

  /**
   * 删除操作日志
   * 后端: DELETE /admin/system/log/{id}
   */
  async deleteLog(id: number): Promise<void> {
    await request.delete(`/admin/system/log/${id}`)
  },

  /**
   * 清空操作日志
   * 后端: DELETE /admin/system/log/clear
   */
  async clearLogs(): Promise<void> {
    await request.delete('/admin/system/log/clear')
  },

  /**
   * 导出操作日志
   * 后端: GET /admin/system/log/export
   */
  async exportLogs(params: LogQuery = {}): Promise<Blob> {
    const resp = await request.get('/admin/system/log/export', {
      params: {
        username: params.username || undefined,
        operation: params.operation || undefined,
        status: params.status !== undefined ? params.status : undefined,
        startTime: params.startTime || undefined,
        endTime: params.endTime || undefined
      },
      responseType: 'blob'
    })
    return resp as unknown as Blob
  }
}
