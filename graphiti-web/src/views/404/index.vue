<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <div class="error-code">404</div>
      <div class="error-message">页面未找到</div>
      <div class="error-description">
        抱歉，您访问的页面不存在或已被移除。
      </div>
      <a-button type="primary" @click="goHome">
        <template #icon><HomeOutlined /></template>
        返回首页
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { HomeOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const goHome = () => {
  router.push('/dashboard')
}
</script>

<style scoped lang="less">
.not-found-container {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #010102;
  color: #f7f8f8;
}

.not-found-content {
  text-align: center;
  
  .error-code {
    font-size: 120px;
    font-weight: 700;
    background: linear-gradient(135deg, #5e6ad2, #00d4ff);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    line-height: 1;
    margin-bottom: 16px;
  }
  
  .error-message {
    font-size: 24px;
    font-weight: 600;
    color: #f7f8f8;
    margin-bottom: 8px;
  }
  
  .error-description {
    font-size: 14px;
    color: #8a8f98;
    margin-bottom: 32px;
  }
  
  :deep(.ant-btn-primary) {
    background: #5e6ad2;
    border-color: #5e6ad2;
    
    &:hover {
      background: #7b7ff0;
      border-color: #7b7ff0;
    }
  }
}
</style>
