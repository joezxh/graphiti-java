<template>
  <div class="graph-create-container">
    <div class="page-header">
      <a-button type="text" @click="goBack">
        <template #icon><ArrowLeftOutlined /></template>
        返回列表
      </a-button>
      <h2 class="page-title">创建新图谱</h2>
    </div>
    
    <div class="create-form-section">
      <a-card :bordered="false" class="form-card">
        <a-form
          ref="formRef"
          :model="formState"
          :rules="formRules"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item label="图谱名称" name="name">
            <a-input
              v-model:value="formState.name"
              placeholder="请输入图谱名称"
              size="large"
            />
          </a-form-item>
          
          <a-form-item label="图谱描述" name="description">
            <a-textarea
              v-model:value="formState.description"
              placeholder="请输入图谱描述"
              :rows="4"
            />
          </a-form-item>
          
          <a-form-item>
            <div class="form-actions">
              <a-button @click="goBack">取消</a-button>
              <a-button type="primary" html-type="submit" :loading="submitting">
                创建图谱
              </a-button>
            </div>
          </a-form-item>
        </a-form>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const formState = reactive({
  name: '',
  description: ''
})

const formRules = {
  name: [
    { required: true, message: '请输入图谱名称' },
    { min: 2, max: 50, message: '图谱名称长度为 2-50 个字符' }
  ]
}

const goBack = () => {
  router.push('/graph/list')
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    const result = await graphApi.create(formState)
    message.success('创建成功')
    router.push(`/graph/detail/${result.id}`)
  } catch (error) {
    message.error('创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="less">
.graph-create-container {
  padding: 20px;
  background: #010102;
  min-height: calc(100vh - 56px);
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  
  :deep(.ant-btn-text) {
    color: #8a8f98;
    
    &:hover {
      color: #f7f8f8;
    }
  }
  
  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: #f7f8f8;
    margin: 0;
  }
}

.create-form-section {
  max-width: 600px;
  
  .form-card {
    background: rgba(15, 16, 17, 0.8);
    border: 1px solid #23252a;
    
    :deep(.ant-card-body) {
      padding: 24px;
    }
    
    :deep(.ant-form-item-label > label) {
      color: #f7f8f8;
    }
    
    :deep(.ant-input),
    :deep(.ant-input-affix-wrapper) {
      background: rgba(15, 16, 17, 0.8);
      border-color: #23252a;
      color: #f7f8f8;
      
      &:hover, &:focus {
        border-color: #5e6ad2;
      }
    }
    
    :deep(.ant-input-textarea) {
      textarea {
        background: rgba(15, 16, 17, 0.8);
        border-color: #23252a;
        color: #f7f8f8;
        
        &:hover, &:focus {
          border-color: #5e6ad2;
        }
      }
    }
  }
  
  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    
    :deep(.ant-btn) {
      background: rgba(15, 16, 17, 0.8);
      border-color: #23252a;
      color: #f7f8f8;
      
      &:hover {
        border-color: #5e6ad2;
        color: #5e6ad2;
      }
    }
    
    :deep(.ant-btn-primary) {
      background: #5e6ad2;
      border-color: #5e6ad2;
      color: #f7f8f8;
      
      &:hover {
        background: #7b7ff0;
        border-color: #7b7ff0;
      }
    }
  }
}
</style>
