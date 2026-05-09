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

// Mock 数据
const mockNotifications: Notification[] = [
  {
    id: 1,
    title: '系统更新通知',
    content: '系统将于2024年5月10日凌晨2:00-4:00进行更新维护，届时系统将暂时无法访问，请提前做好相关工作安排。',
    type: 1,
    isRead: 0,
    createdAt: '2024-05-08 10:00:00'
  },
  {
    id: 2,
    title: '图谱创建成功',
    content: '您创建的图谱"金融知识图谱"已成功创建，现在可以开始添加数据和配置本体了。',
    type: 2,
    isRead: 0,
    createdAt: '2024-05-07 15:30:00'
  },
  {
    id: 3,
    title: '检索任务完成',
    content: '您的检索任务"查询与人工智能相关的实体"已完成，共找到15个相关实体。',
    type: 3,
    isRead: 1,
    createdAt: '2024-05-06 09:15:00'
  },
  {
    id: 4,
    title: '系统维护通知',
    content: '为了提供更好的服务，系统将于本周末进行例行维护，如有任何问题，请联系系统管理员。',
    type: 1,
    isRead: 1,
    createdAt: '2024-05-05 11:20:00'
  },
  {
    id: 5,
    title: '数据导入完成',
    content: '您的数据导入任务已完成，共导入128个实体和356条关系。',
    type: 2,
    isRead: 0,
    createdAt: '2024-05-04 16:45:00'
  },
  {
    id: 6,
    title: '检索任务失败',
    content: '您的检索任务"查询与区块链相关的实体"执行失败，请检查检索条件后重试。',
    type: 3,
    isRead: 1,
    createdAt: '2024-05-03 14:10:00'
  }
]

// 模拟延迟
const delay = (ms: number = 500) => new Promise(resolve => setTimeout(resolve, ms))

// 通知 API
export const notificationApi = {
  // 获取通知列表
  getNotifications: async (params: NotificationQuery = {}) => {
    await delay()
    
    let filteredNotifications = [...mockNotifications]
    
    if (params.type !== undefined) {
      filteredNotifications = filteredNotifications.filter(n => n.type === params.type)
    }
    
    if (params.isRead !== undefined) {
      filteredNotifications = filteredNotifications.filter(n => n.isRead === params.isRead)
    }
    
    // 按创建时间倒序排序
    filteredNotifications.sort((a, b) => {
      return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    })
    
    const pageNum = params.pageNum || 1
    const pageSize = params.pageSize || 10
    const start = (pageNum - 1) * pageSize
    const end = start + pageSize
    
    return {
      list: filteredNotifications.slice(start, end),
      total: filteredNotifications.length,
      pageNum,
      pageSize
    }
  },
  
  // 获取未读通知数量
  getUnreadCount: async () => {
    await delay(300)
    
    const unreadCount = mockNotifications.filter(n => n.isRead === 0).length
    return { count: unreadCount }
  },
  
  // 标记通知为已读
  markAsRead: async (id: number) => {
    await delay()
    
    const index = mockNotifications.findIndex(n => n.id === id)
    if (index !== -1) {
      mockNotifications[index].isRead = 1
    }
    
    return {}
  },
  
  // 标记所有通知为已读
  markAllAsRead: async () => {
    await delay()
    
    for (const notification of mockNotifications) {
      notification.isRead = 1
    }
    
    return {}
  },
  
  // 删除通知
  deleteNotification: async (id: number) => {
    await delay()
    
    const index = mockNotifications.findIndex(n => n.id === id)
    if (index !== -1) {
      mockNotifications.splice(index, 1)
    }
    
    return {}
  },
  
  // 清空所有通知
  clearAllNotifications: async () => {
    await delay()
    
    mockNotifications.length = 0
    
    return {}
  }
}
