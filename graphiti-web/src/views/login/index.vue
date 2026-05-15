<template>
  <div class="login-page">
    <!-- 背景动画 -->
    <div class="bg-grid" />
    <div class="bg-glow" />

    <!-- 登录卡片 -->
    <div class="login-card" :class="{ shake: shakeAnimation }">
      <!-- Logo 和标题 -->
      <div class="card-header">
        <div class="logo">
          <svg viewBox="0 0 40 40" class="logo-icon">
            <defs>
              <linearGradient id="logoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#5e6ad2" />
                <stop offset="100%" stop-color="#00d4ff" />
              </linearGradient>
            </defs>
            <polygon points="20,4 36,12 36,28 20,36 4,28 4,12" fill="none" stroke="url(#logoGradient)" stroke-width="2" />
            <circle cx="20" cy="20" r="6" fill="url(#logoGradient)" />
          </svg>
        </div>
        <h1 class="title">{{ $t('login.title') }}</h1>
        <p class="subtitle">{{ $t('login.subtitle') }}</p>
      </div>

      <!-- 登录表单 -->
      <a-form
        ref="formRef"
        :model="form"
        :rules="rules"
        layout="vertical"
        @finish="handleLogin"
      >
        <a-form-item name="username" :label="$t('login.username')">
          <a-input
            v-model:value="form.username"
            size="large"
            :placeholder="$t('login.enterUsername')"
          >
            <template #prefix>
              <UserOutlined />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item name="password" :label="$t('login.password')">
          <a-input-password
            v-model:value="form.password"
            size="large"
            :placeholder="$t('login.enterPassword')"
          >
            <template #prefix>
              <LockOutlined />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-checkbox v-model:checked="rememberMe">{{ $t('login.rememberMe') }}</a-checkbox>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            :loading="loading"
            block
            class="login-btn"
          >
            {{ $t('login.login') }}
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <!-- 页脚 -->
    <div class="footer">
      © 2026 Graphiti-Java · All rights reserved
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, nextTick, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const rememberMe = ref(true)
const shakeAnimation = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [
    { required: true, message: 'login.enterUsername' },
    { min: 3, max: 20, message: 'login.usernameLength' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: 'login.usernamePattern' }
  ],
  password: [
    { required: true, message: 'login.enterPassword' },
    { min: 6, message: 'login.passwordMin' }
  ]
}

const shakeTimer = ref<number | null>(null)

onUnmounted(() => {
  if (shakeTimer.value) {
    clearTimeout(shakeTimer.value)
  }
})

const handleLogin = async () => {
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (error: any) {
    // 强制重新触发动画：先设为 false，再在下一个 tick 设为 true
    shakeAnimation.value = false
    await nextTick()
    shakeAnimation.value = true
    if (shakeTimer.value) clearTimeout(shakeTimer.value)
    shakeTimer.value = window.setTimeout(() => {
      shakeAnimation.value = false
    }, 600)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="less" scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #010102;
  position: relative;
  overflow: hidden;
}

// 背景网格
.bg-grid {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(rgba(94, 106, 210, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(94, 106, 210, 0.03) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

// 背景光晕
.bg-glow {
  position: absolute;
  width: 600px;
  height: 600px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(94, 106, 210, 0.15) 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none;
  filter: blur(60px);
}

// 登录卡片
.login-card {
  position: relative;
  width: 400px;
  padding: 40px;
  background: #0f1011;
  border: 1px solid #23252a;
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(94, 106, 210, 0.1);
  z-index: 1;

  &:focus-within {
    border-color: #34343a;
    box-shadow: 0 0 0 2px rgba(94, 106, 210, 0.3);
  }
}

// 卡片头部
.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  margin-bottom: 16px;
}

.logo-icon {
  width: 48px;
  height: 48px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: #f7f8f8;
  margin: 0 0 8px;
}

.subtitle {
  font-size: 14px;
  color: #8a8f98;
  margin: 0;
}

// 表单样式覆盖
:deep(.ant-form-item-label > label) {
  color: #d0d6e0;
}

:deep(.ant-input-affix-wrapper) {
  background: #141516;
  border-color: #23252a;
  border-radius: 8px;
  
  &:hover, &:focus, &-focused {
    border-color: #5e6ad2;
    box-shadow: 0 0 0 2px rgba(94, 106, 210, 0.2);
  }
  
  .ant-input {
    background: transparent;
    color: #f7f8f8;
    
    &::placeholder {
      color: #8a8f98;
    }
  }
  
  .anticon {
    color: #8a8f98;
  }
}

:deep(.ant-checkbox-wrapper) {
  color: #d0d6e0;
  
  .ant-checkbox-inner {
    background: #141516;
    border-color: #23252a;
  }
  
  .ant-checkbox-checked .ant-checkbox-inner {
    background: #5e6ad2;
    border-color: #5e6ad2;
  }
}

// 登录按钮
.login-btn {
  height: 46px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 500;
  background: #5e6ad2;
  border-color: #5e6ad2;
  
  &:hover, &:focus {
    background: #828fff;
    border-color: #828fff;
  }
  
  &:active {
    background: #4c54b8;
    border-color: #4c54b8;
  }
}

// 页脚
.footer {
  position: relative;
  z-index: 1;
  margin-top: 24px;
  font-size: 12px;
  color: #8a8f98;
}

// Shake 动画
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-4px); }
  20%, 40%, 60%, 80% { transform: translateX(4px); }
}

.shake {
  animation: shake 0.6s ease-in-out;
}

// 错误状态红色边框
:deep(.ant-form-item-has-error .ant-input-affix-wrapper) {
  border-color: #ff6b6b !important;
  box-shadow: 0 0 0 2px rgba(255, 107, 107, 0.3);
}

:deep(.ant-form-item-has-error .ant-input-affix-wrapper:hover) {
  border-color: #ff6b6b !important;
}

// 输入框 focus 外发光增强
:deep(.ant-input-affix-wrapper:focus),
:deep(.ant-input-affix-wrapper-focused) {
  box-shadow: 0 0 0 2px rgba(94, 106, 210, 0.4);
}
</style>
