<template>
  <div class="profile-page">
    <a-row :gutter="16">
      <!-- 左侧用户信息卡片 -->
      <a-col :span="8">
        <a-card class="user-card" :bordered="false">
          <div class="user-info">
            <a-avatar :size="100" class="user-avatar">
              {{ userInfo?.nickname?.charAt(0) || 'U' }}
            </a-avatar>
            <h2 class="user-name">{{ userInfo?.nickname || '用户' }}</h2>
            <p class="user-role">{{ roleName }}</p>
            <div class="user-stats">
              <div class="stat-item">
                <div class="stat-value">12</div>
                <div class="stat-label">创建图谱</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">256</div>
                <div class="stat-label">实体数量</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">89</div>
                <div class="stat-label">检索次数</div>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
      
      <!-- 右侧用户信息表单 -->
      <a-col :span="16">
        <a-card class="info-card" :bordered="false">
          <a-tabs v-model:activeKey="activeTab">
            <a-tab-pane key="basic" tab="基本信息">
              <a-form
                ref="basicFormRef"
                :model="basicForm"
                :rules="basicRules"
                :label-col="{ span: 4 }"
                :wrapper-col="{ span: 16 }"
                style="margin-top: 24px"
              >
                <a-form-item label="用户名" name="username">
                  <a-input v-model:value="basicForm.username" disabled />
                </a-form-item>
                
                <a-form-item label="昵称" name="nickname">
                  <a-input v-model:value="basicForm.nickname" placeholder="请输入昵称" />
                </a-form-item>
                
                <a-form-item label="邮箱" name="email">
                  <a-input v-model:value="basicForm.email" placeholder="请输入邮箱" />
                </a-form-item>
                
                <a-form-item label="手机号" name="phone">
                  <a-input v-model:value="basicForm.phone" placeholder="请输入手机号" />
                </a-form-item>
                
                <a-form-item label="个人简介" name="description">
                  <a-textarea v-model:value="basicForm.description" placeholder="请输入个人简介" :rows="4" />
                </a-form-item>
                
                <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
                  <a-button type="primary" @click="handleUpdateBasic">保存修改</a-button>
                </a-form-item>
              </a-form>
            </a-tab-pane>
            
            <a-tab-pane key="password" tab="修改密码" force-render>
              <a-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                :label-col="{ span: 4 }"
                :wrapper-col="{ span: 16 }"
                style="margin-top: 24px"
              >
                <a-form-item label="当前密码" name="oldPassword">
                  <a-input-password v-model:value="passwordForm.oldPassword" placeholder="请输入当前密码" />
                </a-form-item>
                
                <a-form-item label="新密码" name="newPassword">
                  <a-input-password v-model:value="passwordForm.newPassword" placeholder="请输入新密码" />
                </a-form-item>
                
                <a-form-item label="确认密码" name="confirmPassword">
                  <a-input-password v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
                </a-form-item>
                
                <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
                  <a-button type="primary" @click="handleUpdatePassword">修改密码</a-button>
                </a-form-item>
              </a-form>
            </a-tab-pane>
            
            <a-tab-pane key="notification" tab="通知设置">
              <div style="margin-top: 24px">
                <a-spin :spinning="settingsLoading">
                  <a-list :data-source="notificationSettings" :split="false">
                    <template #renderItem="{ item }">
                      <a-list-item>
                        <a-list-item-meta :title="item.title" :description="item.description" />
                        <template #extra>
                          <a-switch v-model:checked="item.enabled" @change="onSettingsChange" />
                        </template>
                      </a-list-item>
                    </template>
                  </a-list>
                </a-spin>

                <div style="text-align: center; margin-top: 24px">
                  <a-button type="primary" :loading="savingSettings" @click="handleSaveNotification">保存设置</a-button>
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/store/modules/user'
import { notificationApi, type NotificationSettings } from '@/api/notification'
// import { authApi } from '@/api/auth'

// 用户 store
const userStore = useUserStore()

// 用户信息
const userInfo = computed(() => userStore.userInfo)

// 角色名称
const roleName = computed(() => {
  const r = (userInfo.value as any)?.roleName || (userInfo.value as any)?.role
  if (!r) return '未知角色'
  
  switch (r) {
    case 'ADMIN': return '管理员'
    case 'EDITOR': return '编辑员'
    case 'VIEWER': return '观察员'
    default: return r
  }
})

// 当前激活的标签页
const activeTab = ref('basic')

// 基本信息表单
const basicFormRef = ref()
const basicForm = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  description: ''
})

// 基本信息表单校验规则
const basicRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' }
  ]
}

// 密码表单
const passwordFormRef = ref()
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码表单校验规则
const validateConfirmPassword = async (_rule: any, value: string) => {
  if (value !== passwordForm.newPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 通知设置
interface NotificationSetting {
  key: string
  title: string
  description: string
  enabled: boolean
  dbField: 'systemEnabled' | 'graphEnabled' | 'searchEnabled' | 'emailEnabled'
}

const notificationSettings = ref<NotificationSetting[]>([
  {
    key: 'system',
    title: '系统通知',
    description: '接收系统更新、维护等通知',
    enabled: true,
    dbField: 'systemEnabled'
  },
  {
    key: 'graph',
    title: '图谱更新通知',
    description: '当图谱有更新时接收通知',
    enabled: true,
    dbField: 'graphEnabled'
  },
  {
    key: 'search',
    title: '检索完成通知',
    description: '当检索任务完成时接收通知',
    enabled: true,
    dbField: 'searchEnabled'
  },
  {
    key: 'email',
    title: '邮件通知',
    description: '通过邮件接收通知',
    enabled: false,
    dbField: 'emailEnabled'
  }
])

const settingsLoading = ref(false)
const savingSettings = ref(false)
let settingsDirty = false

// 初始化表单数据
const initFormData = () => {
  if (userInfo.value) {
    basicForm.username = userInfo.value.username || ''
    basicForm.nickname = userInfo.value.nickname || ''
    basicForm.email = (userInfo.value as any).email || ''
    basicForm.phone = (userInfo.value as any).phone || ''
    basicForm.description = (userInfo.value as any).description || ''
  }
}

// 加载通知设置
const loadNotificationSettings = async () => {
  settingsLoading.value = true
  try {
    const settings = await notificationApi.getSettings()
    const settingMap: Record<string, number> = {
      systemEnabled: settings.systemEnabled,
      graphEnabled: settings.graphEnabled,
      searchEnabled: settings.searchEnabled,
      emailEnabled: settings.emailEnabled
    }
    for (const item of notificationSettings.value) {
      const val = settingMap[item.dbField]
      if (val !== undefined) {
        item.enabled = val === 1
      }
    }
    settingsDirty = false
  } catch (error) {
    console.error('加载通知设置失败', error)
  } finally {
    settingsLoading.value = false
  }
}

// 开关变化时标记为脏
const onSettingsChange = () => {
  settingsDirty = true
}

// 更新基本信息
const handleUpdateBasic = async () => {
  try {
    await basicFormRef.value.validate()
    
    // 这里应该调用 API 更新用户信息
    // 由于是 Mock 数据，我们只是模拟更新
    
    message.success('基本信息更新成功')
  } catch (error) {
    console.error('表单验证失败', error)
  }
}

// 更新密码
const handleUpdatePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    
    // 这里应该调用 API 更新密码
    // 由于是 Mock 数据，我们只是模拟更新
    
    message.success('密码修改成功')
    
    // 清空表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
    console.error('表单验证失败', error)
  }
}

// 保存通知设置
const handleSaveNotification = async () => {
  savingSettings.value = true
  try {
    const settings: NotificationSettings = {
      systemEnabled: notificationSettings.value.find(s => s.key === 'system')!.enabled ? 1 : 0,
      graphEnabled: notificationSettings.value.find(s => s.key === 'graph')!.enabled ? 1 : 0,
      searchEnabled: notificationSettings.value.find(s => s.key === 'search')!.enabled ? 1 : 0,
      emailEnabled: notificationSettings.value.find(s => s.key === 'email')!.enabled ? 1 : 0
    }
    await notificationApi.saveSettings(settings)
    settingsDirty = false
    message.success('通知设置保存成功')
  } catch (error) {
    message.error('保存失败')
    console.error(error)
  } finally {
    savingSettings.value = false
  }
}

onMounted(() => {
  initFormData()
})

// 切换到通知设置 tab 时加载设置
watch(activeTab, (tab) => {
  if (tab === 'notification' && !settingsDirty) {
    loadNotificationSettings()
  }
})
</script>

<style scoped lang="less">
.profile-page {
  .user-card {
    .user-info {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 24px 0;
      
      .user-avatar {
        background: linear-gradient(135deg, #5e6ad2, #00d4ff);
        color: #fff;
        font-size: 36px;
        margin-bottom: 16px;
      }
      
      .user-name {
        font-size: 20px;
        font-weight: 600;
        color: #f7f8f8;
        margin: 0 0 4px 0;
      }
      
      .user-role {
        font-size: 14px;
        color: #8a8f98;
        margin: 0 0 24px 0;
      }
      
      .user-stats {
        display: flex;
        justify-content: space-around;
        width: 100%;
        padding-top: 24px;
        border-top: 1px solid @border-color;
        
        .stat-item {
          text-align: center;
          
          .stat-value {
            font-size: 24px;
            font-weight: 600;
            color: #5e6ad2;
            line-height: 1;
            margin-bottom: 4px;
          }
          
          .stat-label {
            font-size: 12px;
            color: #8a8f98;
          }
        }
      }
    }
  }
  
  .info-card {
    min-height: 500px;
  }

  :deep(.ant-tabs-tab) {
    color: rgba(247, 248, 248, 0.65);
    &.ant-tabs-tab-active .ant-tabs-tab-btn {
      color: #f7f8f8;
    }
  }

  :deep(.ant-list-item-meta-title) {
    color: #ffffff !important;
  }

  :deep(.ant-list-item-meta-description) {
    color: rgba(255, 255, 255, 0.65) !important;
  }
}
</style>
