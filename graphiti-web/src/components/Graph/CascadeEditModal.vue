<template>
  <a-modal
    :open="visible"
    title="属性级联编辑"
    width="640px"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="cascade-edit-modal">
      <!-- Filter Section -->
      <div class="section">
        <div class="section-title">筛选条件</div>
        
        <div class="condition-list">
          <div v-for="(condition, index) in conditions" :key="index" class="condition-row">
            <a-select
              v-model:value="condition.propertyName"
              placeholder="选择属性"
              style="width: 140px"
              size="small"
              allow-clear
            >
              <a-select-option v-for="prop in propertyOptions" :key="prop" :value="prop">
                {{ prop }}
              </a-select-option>
            </a-select>
            
            <a-select
              v-model:value="condition.operator"
              style="width: 100px"
              size="small"
            >
              <a-select-option value="eq">等于</a-select-option>
              <a-select-option value="ne">不等于</a-select-option>
              <a-select-option value="gt">大于</a-select-option>
              <a-select-option value="lt">小于</a-select-option>
              <a-select-option value="gte">大于等于</a-select-option>
              <a-select-option value="lte">小于等于</a-select-option>
              <a-select-option value="contains">包含</a-select-option>
              <a-select-option value="not_contains">不包含</a-select-option>
              <a-select-option value="in">在列表中</a-select-option>
              <a-select-option value="not_in">不在列表</a-select-option>
              <a-select-option value="is_null">为空</a-select-option>
              <a-select-option value="is_not_null">不为空</a-select-option>
            </a-select>
            
            <a-input
              v-if="!isNoValueOperator(condition.operator)"
              v-model:value="condition.value"
              placeholder="值"
              style="flex: 1"
              size="small"
            />
            <div v-else style="flex: 1" />
            
            <a-button
              v-if="conditions.length > 1"
              type="text"
              size="small"
              danger
              @click="removeCondition(index)"
            >
              <template #icon><MinusOutlined /></template>
            </a-button>
          </div>
        </div>
        
        <a-button type="link" size="small" @click="addCondition">
          <template #icon><PlusOutlined /></template>
          添加条件
        </a-button>
        
        <div class="logic-selector" v-if="conditions.length > 1">
          <a-select v-model:value="logic" style="width: 80px" size="small">
            <a-select-option value="AND">AND</a-select-option>
            <a-select-option value="OR">OR</a-select-option>
          </a-select>
          <span class="logic-hint">多个条件的组合方式</span>
        </div>
      </div>
      
      <!-- Preview Section -->
      <div class="section">
        <a-button type="primary" ghost @click="handlePreview" :loading="previewLoading">
          <template #icon><SearchOutlined /></template>
          预览影响范围
        </a-button>
      </div>
      
      <!-- Preview Result -->
      <div v-if="previewResult" class="preview-result">
        <div class="preview-header">
          <span class="preview-icon">📊</span>
          <span class="preview-title">影响范围预览</span>
        </div>
        
        <div class="preview-stat">
          <span class="stat-number">{{ previewResult.totalMatch }}</span>
          <span class="stat-label">个节点匹配</span>
        </div>
        
        <div v-if="previewResult.distribution.length > 0" class="preview-distribution">
          <div class="distribution-title">分布</div>
          <div
            v-for="dist in previewResult.distribution"
            :key="dist.value"
            class="distribution-item"
          >
            <span class="distribution-label">{{ dist.value }}</span>
            <div class="distribution-bar">
              <div
                class="distribution-fill"
                :style="{ width: (dist.count / previewResult.totalMatch * 100) + '%' }"
              />
            </div>
            <span class="distribution-count">{{ dist.count }}</span>
          </div>
        </div>
      </div>
      
      <!-- Update Section -->
      <div v-if="previewResult" class="section">
        <div class="section-title">修改内容</div>
        
        <div class="update-row">
          <a-select
            v-model:value="updateProperty"
            placeholder="选择属性"
            style="width: 140px"
            size="small"
          >
            <a-select-option v-for="prop in propertyOptions" :key="prop" :value="prop">
              {{ prop }}
            </a-select-option>
          </a-select>
          
          <span class="update-equal">=</span>
          
          <a-input
            v-model:value="updateValue"
            placeholder="新值"
            style="flex: 1"
            size="small"
          />
        </div>
        
        <a-button type="link" size="small" @click="addUpdateProperty" v-if="updateValues.length <= 2">
          <template #icon><PlusOutlined /></template>
          添加更多修改
        </a-button>
        
        <div v-for="(uv, index) in updateValues" :key="'uv-' + index" class="update-row" style="margin-top: 8px">
          <a-input :value="uv.property" disabled style="width: 140px" size="small" />
          <span class="update-equal">=</span>
          <a-input v-model:value="uv.value" placeholder="新值" style="flex: 1" size="small" />
          <a-button type="text" size="small" danger @click="removeUpdateProperty(index)">
            <template #icon><MinusOutlined /></template>
          </a-button>
        </div>
      </div>
      
      <!-- Actions -->
      <div class="modal-actions">
        <a-button @click="handleClose">{{ t('common.cancel') }}</a-button>
        <a-button
          type="primary"
          :disabled="!canExecute"
          :loading="executeLoading"
          @click="handleExecute"
        >
          确认修改 {{ previewResult ? `(${previewResult.totalMatch} 个节点)` : '' }}
        </a-button>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
const { t } = useI18n()
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, MinusOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import type { SchemaClass, CascadePreviewResponse } from '@/api/graph'
import { useI18n } from 'vue-i18n'

interface Props {
  visible: boolean
  graphId: string
  classes: SchemaClass[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

// State
const classType = ref('')
const conditions = reactive<Array<{
  propertyName: string
  operator: string
  value: any
}>>([{ propertyName: '', operator: 'eq', value: '' }])
const logic = ref<'AND' | 'OR'>('AND')
const previewResult = ref<CascadePreviewResponse | null>(null)
const previewLoading = ref(false)
const executeLoading = ref(false)
const updateProperty = ref('')
const updateValue = ref('')
const updateValues = reactive<Array<{ property: string; value: string }>>([])

// Property options
const propertyOptions = computed(() => {
  const selectedClass = props.classes.find(c => c.localName === classType.value)
  if (selectedClass?.properties) {
    return selectedClass.properties.map(p => p.localName)
  }
  // Default properties
  return ['name', 'type', 'summary', 'status', 'age', 'city', 'company']
})

// Can execute
const canExecute = computed(() => {
  return previewResult.value &&
         previewResult.value.totalMatch > 0 &&
         (updateProperty.value || updateValues.length > 0)
})

// Watch class type change
watch(classType, () => {
  previewResult.value = null
})

// Methods
const isNoValueOperator = (operator: string): boolean => {
  return operator === 'is_null' || operator === 'is_not_null'
}

const addCondition = () => {
  conditions.push({ propertyName: '', operator: 'eq', value: '' })
}

const removeCondition = (index: number) => {
  conditions.splice(index, 1)
}

const handlePreview = async () => {
  if (!classType.value) {
    message.warning(t('TODO_请选择节点类型'))
    return
  }
  
  const validConditions = conditions.filter(c => c.propertyName)
  if (validConditions.length === 0) {
    message.warning(t('TODO_请至少添加一个筛选条件'))
    return
  }
  
  previewLoading.value = true
  try {
    const result = await graphApi.previewCascade(props.graphId, {
      classType: classType.value,
      conditions: validConditions.map(c => ({
        propertyName: c.propertyName,
        operator: c.operator as any,
        value: c.value
      })),
      logic: logic.value
    })
    previewResult.value = result
  } catch (error) {
    message.error(t('TODO_预览失败'))
    console.error(error)
  } finally {
    previewLoading.value = false
  }
}

const addUpdateProperty = () => {
  if (updateProperty.value && updateValue.value) {
    updateValues.push({ property: updateProperty.value, value: updateValue.value })
    updateProperty.value = ''
    updateValue.value = ''
  }
}

const removeUpdateProperty = (index: number) => {
  updateValues.splice(index, 1)
}

const handleExecute = async () => {
  if (!previewResult.value || previewResult.value.totalMatch === 0) {
    message.warning(t('TODO_没有匹配的节点'))
    return
  }
  
  const updates: Record<string, any> = {}
  
  if (updateProperty.value) {
    updates[updateProperty.value] = updateValue.value
  }
  
  updateValues.forEach(uv => {
    updates[uv.property] = uv.value
  })
  
  if (Object.keys(updates).length === 0) {
    message.warning(t('TODO_请设置要修改的属性'))
    return
  }
  
  executeLoading.value = true
  try {
    const result = await graphApi.executeCascade(props.graphId, {
      classType: classType.value,
      conditions: conditions.filter(c => c.propertyName).map(c => ({
        propertyName: c.propertyName,
        operator: c.operator as any,
        value: c.value
      })),
      logic: logic.value,
      updates
    })
    
    if (result.success) {
      message.success(`成功修改 ${result.affectedCount} 个节点`)
      emit('success')
      handleClose()
    } else {
      message.warning(`部分失败: ${result.failedCount} 个`)
    }
  } catch (error) {
    message.error(t('TODO_执行失败'))
    console.error(error)
  } finally {
    executeLoading.value = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
  // Reset state
  setTimeout(() => {
    classType.value = ''
    conditions.splice(0, conditions.length, { propertyName: '', operator: 'eq', value: '' })
    previewResult.value = null
    updateProperty.value = ''
    updateValue.value = ''
    updateValues.splice(0, updateValues.length)
  }, 300)
}
</script>

<style scoped lang="less">
.cascade-edit-modal {
  .section {
    margin-bottom: 20px;
  }
  
  .section-title {
    font-size: 13px;
    font-weight: 600;
    color: #8b949e;
    margin-bottom: 12px;
  }
  
  .condition-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 8px;
  }
  
  .condition-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .logic-selector {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
    
    .logic-hint {
      font-size: 12px;
      color: #6e7681;
    }
  }
  
  .preview-result {
    background: linear-gradient(135deg, rgba(63, 185, 80, 0.1) 0%, rgba(88, 166, 255, 0.1) 100%);
    border: 1px solid rgba(63, 185, 80, 0.3);
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 20px;
    
    .preview-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      
      .preview-icon {
        font-size: 16px;
      }
      
      .preview-title {
        font-size: 14px;
        font-weight: 600;
        color: #3fb950;
      }
    }
    
    .preview-stat {
      margin-bottom: 16px;
      
      .stat-number {
        font-size: 32px;
        font-weight: 700;
        color: #e6edf3;
        margin-right: 8px;
      }
      
      .stat-label {
        font-size: 14px;
        color: #8b949e;
      }
    }
    
    .preview-distribution {
      .distribution-title {
        font-size: 12px;
        color: #6e7681;
        margin-bottom: 8px;
      }
      
      .distribution-item {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 6px;
        
        .distribution-label {
          width: 60px;
          font-size: 12px;
          color: #8b949e;
        }
        
        .distribution-bar {
          flex: 1;
          height: 6px;
          background: #21262d;
          border-radius: 3px;
          overflow: hidden;
          
          .distribution-fill {
            height: 100%;
            background: linear-gradient(90deg, #58a6ff, #a371f7);
            border-radius: 3px;
            transition: width 0.3s;
          }
        }
        
        .distribution-count {
          width: 40px;
          text-align: right;
          font-size: 12px;
          color: #e6edf3;
        }
      }
    }
  }
  
  .update-row {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .update-equal {
      color: #58a6ff;
      font-weight: 600;
    }
  }
  
  .modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    padding-top: 16px;
    border-top: 1px solid #30363d;
  }
}

:deep(.ant-modal-content) {
  background: #161b22;
}

:deep(.ant-modal-header) {
  background: #161b22;
  border-bottom: 1px solid #30363d;
  
  .ant-modal-title {
    color: #e6edf3;
  }
}

:deep(.ant-modal-close) {
  color: #8b949e;
}
</style>
