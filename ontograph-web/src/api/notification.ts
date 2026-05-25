import request from './request'

// 通知类型定义
export interface Notification {
  id: number
  title: string
  content: string
  type: number // 1-系统通知 2-图谱通知 3-检索通知
  isRead: number // 0-未读 1-已读
  createdAt: string
}

export interface NotificationQuery {
  type?: number
  isRead?: number
  pageNum?: number
  pageSize?: number
}

export interface NotificationListResponse {
  list: Notification[]
  total: number
  pageNum: number
  pageSize: number
}

// 用户通知设置
export interface NotificationSettings {
  id?: number
  userId?: number
  systemEnabled: number  // 0-关闭 1-开启
  graphEnabled: number    // 0-关闭 1-开启
  searchEnabled: number  // 0-关闭 1-开启
  emailEnabled: number    // 0-关闭 1-开启
}

// 通知 API
export const notificationApi = {
  // 获取通知列表
  getNotifications: async (params: NotificationQuery = {}): Promise<NotificationListResponse> => {
    return request.get('/notifications/list', { params })
  },

  // 获取未读通知数量
  getUnreadCount: async (): Promise<{ count: number }> => {
    return request.get('/notifications/unread-count')
  },

  // 标记通知为已读
  markAsRead: async (id: number): Promise<void> => {
    return request.put(`/notifications/${id}/read`)
  },

  // 标记所有通知为已读
  markAllAsRead: async (): Promise<void> => {
    return request.put('/notifications/read-all')
  },

  // 删除通知
  deleteNotification: async (id: number): Promise<void> => {
    return request.delete(`/notifications/${id}`)
  },

  // 清空所有通知
  clearAllNotifications: async (): Promise<void> => {
    return request.delete('/notifications/clear')
  },

  // 获取通知设置
  getSettings: async (): Promise<NotificationSettings> => {
    return request.get('/notifications/settings')
  },

  // 保存通知设置
  saveSettings: async (settings: NotificationSettings): Promise<void> => {
    return request.put('/notifications/settings', settings)
  }
}
