<template>
  <div class="notification-page">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">通知中心</h2>
          <p class="page-description">查看系统通知和消息</p>
        </div>
        <div class="header-actions">
          <a-space>
            <a-button @click="handleMarkAllAsRead" :disabled="unreadCount === 0">
              <template #icon><CheckCircleOutlined /></template>
              全部已读
            </a-button>
            <a-button danger @click="handleClearAll" :disabled="notificationList.length === 0">
              <template #icon><DeleteOutlined /></template>
              清空通知
            </a-button>
          </a-space>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <!-- 通知类型筛选 -->
      <div class="notification-filter">
        <a-radio-group v-model:value="filterType" button-style="solid" @change="handleFilterChange">
          <a-radio-button :value="0">全部</a-radio-button>
          <a-radio-button :value="1">系统通知</a-radio-button>
          <a-radio-button :value="2">图谱通知</a-radio-button>
          <a-radio-button :value="3">检索通知</a-radio-button>
        </a-radio-group>
        
        <a-radio-group v-model:value="filterRead" button-style="solid" @change="handleFilterChange" style="margin-left: 16px">
          <a-radio-button :value="-1">全部</a-radio-button>
          <a-radio-button :value="0">未读</a-radio-button>
          <a-radio-button :value="1">已读</a-radio-button>
        </a-radio-group>
      </div>

      <!-- 通知列表 -->
      <a-list
        :data-source="notificationList"
        :loading="loading"
        item-layout="horizontal"
        class="notification-list"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <template #actions>
              <a-space>
                <a-button 
                  type="link" 
                  size="small" 
                  v-if="item.isRead === 0"
                  @click="handleMarkAsRead(item.id)"
                >
                  标为已读
                </a-button>
                <a-popconfirm
                  title="确定要删除此通知吗？"
                  ok-text="确定"
                  cancel-text="取消"
                  @confirm="handleDelete(item.id)"
                >
                  <a-button type="link" size="small" danger>
                    删除
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
            
            <a-list-item-meta>
              <template #title>
                <div class="notification-title">
                  <a-badge :status="item.isRead === 0 ? 'processing' : 'default'" />
                  <span :style="{ fontWeight: item.isRead === 0 ? '600' : 'normal' }">
                    {{ item.title }}
                  </span>
                  <a-tag :color="getNotificationTypeColor(item.type)" style="margin-left: 8px">
                    {{ getNotificationTypeText(item.type) }}
                  </a-tag>
                </div>
              </template>
              <template #description>
                <div class="notification-content">
                  <div class="notification-text">{{ item.content }}</div>
                  <div class="notification-time">{{ item.createdAt }}</div>
                </div>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
        
        <template #loadMore>
          <div class="load-more" v-if="notificationList.length < total">
            <a-button :loading="loadingMore" @click="handleLoadMore">加载更多</a-button>
          </div>
        </template>
      </a-list>
      
      <!-- 空状态 -->
      <a-empty 
        v-if="!loading && notificationList.length === 0" 
        description="暂无通知" 
        style="margin-top: 48px"
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  CheckCircleOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { notificationApi, type Notification, type NotificationQuery } from '@/api/notification'

// 查询参数
const filterType = ref<number>(0) // 0-全部 1-系统通知 2-图谱通知 3-检索通知
const filterRead = ref<number>(-1) // -1-全部 0-未读 1-已读

// 通知列表
const notificationList = ref<Notification[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// 未读数量
const unreadCount = ref(0)

// 获取通知列表
const fetchNotifications = async (isLoadMore = false) => {
  if (isLoadMore) {
    loadingMore.value = true
  } else {
    loading.value = true
  }
  
  try {
    const params: NotificationQuery = {
      pageNum: isLoadMore ? pageNum.value : 1,
      pageSize: pageSize.value
    }
    
    if (filterType.value !== 0) {
      params.type = filterType.value
    }
    
    if (filterRead.value !== -1) {
      params.isRead = filterRead.value
    }
    
    const res = await notificationApi.getNotifications(params)
    
    if (isLoadMore) {
      notificationList.value = [...notificationList.value, ...res.list]
    } else {
      notificationList.value = res.list
    }
    
    total.value = res.total
    pageNum.value = res.pageNum + 1
  } catch (error) {
    message.error('获取通知列表失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 获取未读通知数量
const fetchUnreadCount = async () => {
  try {
    const res = await notificationApi.getUnreadCount()
    unreadCount.value = res.count
  } catch (error) {
    console.error('获取未读通知数量失败', error)
  }
}

// 获取通知类型颜色
const getNotificationTypeColor = (type: number) => {
  switch (type) {
    case 1: return 'blue'
    case 2: return 'green'
    case 3: return 'orange'
    default: return 'default'
  }
}

// 获取通知类型文本
const getNotificationTypeText = (type: number) => {
  switch (type) {
    case 1: return '系统通知'
    case 2: return '图谱通知'
    case 3: return '检索通知'
    default: return '未知'
  }
}

// 筛选条件变化
const handleFilterChange = () => {
  pageNum.value = 1
  fetchNotifications()
}

// 加载更多
const handleLoadMore = () => {
  fetchNotifications(true)
}

// 标记通知为已读
const handleMarkAsRead = async (id: number) => {
  try {
    await notificationApi.markAsRead(id)
    message.success('已标记为已读')
    
    // 更新本地状态
    const index = notificationList.value.findIndex(n => n.id === id)
    if (index !== -1) {
      notificationList.value[index].isRead = 1
    }
    
    fetchUnreadCount()
  } catch (error) {
    message.error('操作失败')
  }
}

// 标记所有通知为已读
const handleMarkAllAsRead = async () => {
  try {
    await notificationApi.markAllAsRead()
    message.success('已全部标记为已读')
    
    // 更新本地状态
    for (const notification of notificationList.value) {
      notification.isRead = 1
    }
    
    unreadCount.value = 0
  } catch (error) {
    message.error('操作失败')
  }
}

// 删除通知
const handleDelete = async (id: number) => {
  try {
    await notificationApi.deleteNotification(id)
    message.success('删除成功')
    
    // 更新本地状态
    const index = notificationList.value.findIndex(n => n.id === id)
    if (index !== -1) {
      notificationList.value.splice(index, 1)
      total.value--
    }
    
    fetchUnreadCount()
  } catch (error) {
    message.error('删除失败')
  }
}

// 清空所有通知
const handleClearAll = async () => {
  try {
    await notificationApi.clearAllNotifications()
    message.success('已清空所有通知')
    
    // 更新本地状态
    notificationList.value = []
    total.value = 0
    unreadCount.value = 0
  } catch (error) {
    message.error('操作失败')
  }
}

onMounted(() => {
  fetchNotifications()
  fetchUnreadCount()
})
</script>

<style scoped lang="less">
.notification-page {
  .page-header {
    margin-bottom: 16px;
    
    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #f7f8f8;
      margin: 0 0 4px 0;
    }
    
    .page-description {
      font-size: 14px;
      color: #8a8f98;
      margin: 0;
    }
  }
  
  .content-card {
    .notification-filter {
      margin-bottom: 16px;
    }
    
    .notification-list {
      .notification-title {
        display: flex;
        align-items: center;
        
        .ant-badge {
          margin-right: 8px;
        }
      }
      
      .notification-content {
        .notification-text {
          color: #a4aab8;
          margin-bottom: 4px;
        }
        
        .notification-time {
          font-size: 12px;
          color: #6b7280;
        }
      }
    }
    
    .load-more {
      text-align: center;
      margin-top: 16px;
    }
  }
}
</style>
