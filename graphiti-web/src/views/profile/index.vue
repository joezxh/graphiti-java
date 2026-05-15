<template>
  <div class="profile-page">
    <a-row :gutter="16">
      <a-col :span="8">
        <a-card class="user-card" :bordered="false">
          <div class="user-info">
            <a-avatar :size="100" class="user-avatar">
              {{ userInfo?.nickname?.charAt(0) || "U" }}
            </a-avatar>
            <h2 class="user-name">{{ userInfo?.nickname || $t("profile.user") }}</h2>
            <p class="user-role">{{ roleName }}</p>
            <div class="user-stats">
              <div class="stat-item">
                <div class="stat-value">12</div>
                <div class="stat-label">{{ $t("profile.createdGraphs") }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">256</div>
                <div class="stat-label">{{ $t("profile.entities") }}</div>
              </div>
              <div class="stat-item">
                <div class="stat-value">89</div>
                <div class="stat-label">{{ $t("profile.searchCount") }}</div>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :span="16">
        <a-card class="info-card" :bordered="false">
          <a-tabs v-model:activeKey="activeTab">
            <a-tab-pane key="basic" :tab="$t('profile.basicInfo')">
              <a-form
                ref="basicFormRef"
                :model="basicForm"
                :rules="basicRules"
                :label-col="{ span: 4 }"
                :wrapper-col="{ span: 16 }"
                style="margin-top: 24px"
              >
                <a-form-item :label="$t('profile.username')" name="username">
                  <a-input v-model:value="basicForm.username" disabled />
                </a-form-item>

                <a-form-item :label="$t('profile.nickname')" name="nickname">
                  <a-input v-model:value="basicForm.nickname" :placeholder="$t('profile.enterNickname')" />
                </a-form-item>

                <a-form-item :label="$t('profile.email')" name="email">
                  <a-input v-model:value="basicForm.email" :placeholder="$t('profile.enterEmail')" />
                </a-form-item>

                <a-form-item :label="$t('profile.phone')" name="phone">
                  <a-input v-model:value="basicForm.phone" :placeholder="$t('profile.enterPhone')" />
                </a-form-item>

                <a-form-item :label="$t('profile.bio')" name="description">
                  <a-textarea v-model:value="basicForm.description" :placeholder="$t('profile.enterBio')" :rows="4" />
                </a-form-item>

                <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
                  <a-button type="primary" @click="handleUpdateBasic">{{ $t("profile.saveChanges") }}</a-button>
                </a-form-item>
              </a-form>
            </a-tab-pane>

            <a-tab-pane key="password" :tab="$t('profile.changePassword')" force-render>
              <a-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                :label-col="{ span: 4 }"
                :wrapper-col="{ span: 16 }"
                style="margin-top: 24px"
              >
                <a-form-item :label="$t('profile.currentPassword')" name="oldPassword">
                  <a-input-password v-model:value="passwordForm.oldPassword" :placeholder="$t('profile.enterCurrentPassword')" />
                </a-form-item>

                <a-form-item :label="$t('profile.newPassword')" name="newPassword">
                  <a-input-password v-model:value="passwordForm.newPassword" :placeholder="$t('profile.enterNewPassword')" />
                </a-form-item>

                <a-form-item :label="$t('profile.confirmPassword')" name="confirmPassword">
                  <a-input-password v-model:value="passwordForm.confirmPassword" :placeholder="$t('profile.enterConfirmPassword')" />
                </a-form-item>

                <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
                  <a-button type="primary" @click="handleUpdatePassword">{{ $t("profile.changePassword") }}</a-button>
                </a-form-item>
              </a-form>
            </a-tab-pane>

            <a-tab-pane key="notification" :tab="$t('profile.notificationSettings')">
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
                  <a-button type="primary" :loading="savingSettings" @click="handleSaveNotification">{{ $t("profile.saveSettings") }}</a-button>
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
import { ref, reactive, computed, onMounted, watch } from "vue"
import { message } from "ant-design-vue"
import { useUserStore } from "@/store/modules/user"
import { notificationApi, type NotificationSettings } from "@/api/notification"

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const roleName = computed(() => {
  const r = (userInfo.value as any)?.roleName || (userInfo.value as any)?.role
  if (!r) return "profile.unknownRole"

  switch (r) {
    case "ADMIN": return "profile.admin"
    case "EDITOR": return "profile.editor"
    case "VIEWER": return "profile.viewer"
    default: return r
  }
})

const activeTab = ref("basic")

const basicFormRef = ref()
const basicForm = reactive({
  username: "",
  nickname: "",
  email: "",
  phone: "",
  description: ""
})

const basicRules = {
  nickname: [{ required: true, message: "profile.enterNickname", trigger: "blur" }],
  email: [
    { required: true, message: "profile.enterEmail", trigger: "blur" },
    { type: "email", message: "profile.invalidEmail", trigger: "blur" }
  ],
  phone: [{ required: true, message: "profile.enterPhone", trigger: "blur" }]
}

const passwordFormRef = ref()
const passwordForm = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: ""
})

const validateConfirmPassword = async (_rule: any, value: string) => {
  if (value !== passwordForm.newPassword) {
    return Promise.reject("profile.passwordMismatch")
  }
  return Promise.resolve()
}

const passwordRules = {
  oldPassword: [{ required: true, message: "profile.enterCurrentPassword", trigger: "blur" }],
  newPassword: [
    { required: true, message: "profile.enterNewPassword", trigger: "blur" },
    { min: 6, message: "profile.passwordMin", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, message: "profile.enterConfirmPassword", trigger: "blur" },
    { validator: validateConfirmPassword, trigger: "blur" }
  ]
}

interface NotificationSetting {
  key: string
  title: string
  description: string
  enabled: boolean
  dbField: "systemEnabled" | "graphEnabled" | "searchEnabled" | "emailEnabled"
}

const notificationSettings = ref<NotificationSetting[]>([
  { key: "system", title: "profile.systemNotification", description: "profile.systemNotificationDesc", enabled: true, dbField: "systemEnabled" },
  { key: "graph", title: "profile.graphUpdateNotification", description: "profile.graphUpdateNotificationDesc", enabled: true, dbField: "graphEnabled" },
  { key: "search", title: "profile.searchCompleteNotification", description: "profile.searchCompleteNotificationDesc", enabled: true, dbField: "searchEnabled" },
  { key: "email", title: "profile.emailNotification", description: "profile.emailNotificationDesc", enabled: false, dbField: "emailEnabled" }
])

const settingsLoading = ref(false)
const savingSettings = ref(false)
let settingsDirty = false

const initFormData = () => {
  if (userInfo.value) {
    basicForm.username = userInfo.value.username || ""
    basicForm.nickname = userInfo.value.nickname || ""
    basicForm.email = (userInfo.value as any).email || ""
    basicForm.phone = (userInfo.value as any).phone || ""
    basicForm.description = (userInfo.value as any).description || ""
  }
}

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
    console.error("profile.loadSettingsFailed", error)
  } finally {
    settingsLoading.value = false
  }
}

const onSettingsChange = () => {
  settingsDirty = true
}

const handleUpdateBasic = async () => {
  try {
    await basicFormRef.value.validate()
    message.success("profile.updateSuccess")
  } catch (error) {
    console.error("common.submitFailed", error)
  }
}

const handleUpdatePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    message.success("profile.passwordUpdateSuccess")
    passwordForm.oldPassword = ""
    passwordForm.newPassword = ""
    passwordForm.confirmPassword = ""
  } catch (error) {
    console.error("common.submitFailed", error)
  }
}

const handleSaveNotification = async () => {
  savingSettings.value = true
  try {
    const settings: NotificationSettings = {
      systemEnabled: notificationSettings.value.find(s => s.key === "system")!.enabled ? 1 : 0,
      graphEnabled: notificationSettings.value.find(s => s.key === "graph")!.enabled ? 1 : 0,
      searchEnabled: notificationSettings.value.find(s => s.key === "search")!.enabled ? 1 : 0,
      emailEnabled: notificationSettings.value.find(s => s.key === "email")!.enabled ? 1 : 0
    }
    await notificationApi.saveSettings(settings)
    settingsDirty = false
    message.success("profile.saveSettingsSuccess")
  } catch (error) {
    message.error("profile.saveFailed")
    console.error(error)
  } finally {
    savingSettings.value = false
  }
}

onMounted(() => {
  initFormData()
})

watch(activeTab, (tab) => {
  if (tab === "notification" && !settingsDirty) {
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
