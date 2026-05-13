<template>
  <header class="graphiti-header">
    <div class="header-left">
      <div class="logo" @click="goHome">
        <svg viewBox="0 0 24 24" class="logo-icon">
          <path d="M12 2L2 7l10 5 10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" fill="none" />
        </svg>
        <span class="logo-text">Graphiti Console</span>
      </div>

    </div>

    <div class="header-right">
      <div class="notification-bell" @click="goToNotification">
        <a-badge :count="unreadCount" :offset="[-2, 2]">
          <BellOutlined class="bell-icon" />
        </a-badge>
      </div>
      
      <a-dropdown>
        <div class="user-info">
          <a-avatar size="small" :style="{ backgroundColor: '#5e6ad2' }">
            {{ userNickInitial }}
          </a-avatar>
          <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '用户' }}</span>
        </div>
        <template #overlay>
          <a-menu class="user-menu" @click="handleUserMenuClick">
            <a-menu-item key="profile">
              <UserOutlined />
              个人中心
            </a-menu-item>
            <a-menu-divider />
            <a-menu-item key="logout">
              <LogoutOutlined />
              退出登录
            </a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LogoutOutlined, BellOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { notificationApi } from '@/api/notification'

const router = useRouter()
const userStore = useUserStore()
// const isDevView = ref(false) // 视图切换功能已禁用
const unreadCount = ref(0)

const userNickInitial = computed(() => {
  const u = userStore.userInfo
  return (u?.nickname || u?.username || 'U').charAt(0).toUpperCase()
})

// 获取未读通知数量
const fetchUnreadCount = async () => {
  try {
    const res = await notificationApi.getUnreadCount()
    unreadCount.value = res.count
  } catch (error) {
    console.error('获取未读通知数量失败', error)
  }
}

// 跳转到通知中心
const goToNotification = () => {
  router.push('/notification')
}

onMounted(() => {
  fetchUnreadCount()
  
  // 每60秒刷新一次未读通知数量
  setInterval(fetchUnreadCount, 60000)
})

const goHome = () => {
  router.push('/dashboard')
}

/* 视图切换功能已禁用
const handleViewSwitch = async (checked: boolean) => {
  isDevView.value = checked
  try {
    // 保存视图模式到 localStorage
    localStorage.setItem('view-mode', checked ? 'dev' : 'biz')
    await router.push(checked ? '/dev/dashboard' : '/dashboard')
    message.info(checked ? '已切换到开发者视图' : '已切换到业务视图')
  } catch (error) {
    message.error('视图切换失败')
    isDevView.value = !checked // 回滚状态
  }
}
*/

const handleUserMenuClick = async ({ key }: { key: string }) => {
  if (key === 'logout') {
    await userStore.logout()
    message.success('已退出登录')
    router.push('/login')
    return
  }
  if (key === 'profile') {
    router.push('/profile')
  }
}
</script>

<style scoped lang="less">
.graphiti-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #0a0e1a;
  border-bottom: 1px solid rgba(94, 106, 210, 0.2);
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: opacity 0.2s;
  
  &:hover {
    opacity: 0.8;
  }
}

.logo-icon {
  width: 28px;
  height: 28px;
  color: #5e6ad2;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #eceff6;
  letter-spacing: 0.5px;
}

.view-switch {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.notification-bell {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
  
  &:hover {
    background: rgba(94, 106, 210, 0.1);
  }
  
  .bell-icon {
    font-size: 16px;
    color: #a4aab8;
    
    &:hover {
      color: #eceff6;
    }
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 6px;
  transition: background 0.2s;
  
  &:hover {
    background: rgba(94, 106, 210, 0.1);
  }
}

.username {
  font-size: 13px;
  color: #a4aab8;
}

.user-menu {
  min-width: 140px;
}
</style>
