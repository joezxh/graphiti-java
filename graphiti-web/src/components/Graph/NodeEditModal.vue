<template>
  <a-modal
    :open="visible"
    :title="isEdit ? t('graphIde.nodeEditTitleEdit') : t('graphIde.nodeEditTitleNew')"
    width="500px"
    @ok="handleOk"
    @cancel="handleClose"
    :ok-button-props="{ loading: loading }"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="rules"
      layout="vertical"
    >
      <a-form-item :label="t('graphIde.labelNodeName')" name="name">
        <a-input
          v-model:value="formState.name"
          :placeholder="t('graphIde.placeholderEnterNodeName')"
        />
      </a-form-item>

      <a-form-item :label="t('graphIde.labelNodeType')" name="type">
        <a-select
          v-model:value="formState.type"
          :placeholder="t('graphIde.placeholderSelectNodeType')"
        >
          <a-select-option v-for="cls in classes" :key="cls.id" :value="cls.localName">
            {{ cls.localName }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item :label="t('graphIde.sectionProperties')">
        <div class="properties-list">
          <div
            v-for="(value, key) in formState.properties"
            :key="key"
            class="property-row"
          >
            <a-input
              :value="String(key)"
              disabled
              style="width: 120px"
              size="small"
            />
            <span class="property-equal">=</span>
            <a-input
              :value="String(value)"
              @change="(e: any) => updateProperty(String(key), e.target.value)"
              style="flex: 1"
              size="small"
            />
            <a-button
              type="text"
              size="small"
              danger
              @click="removeProperty(String(key))"
            >
              <template #icon><MinusOutlined /></template>
            </a-button>
          </div>

          <div class="add-property-row">
            <a-input
              v-model:value="newPropertyKey"
              :placeholder="t('graphIde.placeholderPropertyName')"
              style="width: 120px"
              size="small"
            />
            <span class="property-equal">=</span>
            <a-input
              v-model:value="newPropertyValue"
              :placeholder="t('graphIde.placeholderPropertyValue')"
              style="flex: 1"
              size="small"
            />
            <a-button
              type="primary"
              size="small"
              @click="addProperty"
              :disabled="!newPropertyKey"
            >
              <template #icon><PlusOutlined /></template>
            </a-button>
          </div>
        </div>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { PlusOutlined, MinusOutlined } from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import type { SchemaClass, GraphIDENode } from '@/api/graph'

const { t } = useI18n()

interface Props {
  visible: boolean
  graphId: string
  node: GraphIDENode | null
  classes: SchemaClass[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success', node: GraphIDENode): void
}>()

// Refs
const formRef = ref()
const loading = ref(false)

// State
const formState = reactive({
  name: '',
  type: '',
  properties: {} as Record<string, any>
})

const newPropertyKey = ref('')
const newPropertyValue = ref('')

// Computed
const isEdit = computed(() => !!props.node)

// Rules
const rules = computed(() => ({
  name: [
    { required: true, message: t('graphIde.requiredNodeName'), trigger: 'blur' },
    { min: 1, max: 100, message: t('graphIde.nodeNameLength'), trigger: 'blur' }
  ],
  type: [
    { required: true, message: t('graphIde.requiredNodeType'), trigger: 'change' }
  ]
}))

// Watch visible change
watch(() => props.visible, (val) => {
  if (val) {
    if (props.node) {
      // Edit mode
      formState.name = props.node.name || ''
      formState.type = props.node.type || ''
      formState.properties = { ...props.node.properties } || {}
    } else {
      // Create mode
      formState.name = ''
      formState.type = ''
      formState.properties = {}
    }
    newPropertyKey.value = ''
    newPropertyValue.value = ''
  }
})

// Methods
const updateProperty = (key: string, value: string) => {
  formState.properties[key] = value
}

const addProperty = () => {
  if (newPropertyKey.value) {
    formState.properties[newPropertyKey.value] = newPropertyValue.value
    newPropertyKey.value = ''
    newPropertyValue.value = ''
  }
}

const removeProperty = (key: string) => {
  delete formState.properties[key]
}

const handleOk = async () => {
  try {
    await formRef.value.validate()
    
    loading.value = true
    
    if (isEdit.value && props.node) {
      // Update existing node
      const updated = await graphApi.updateNode(props.graphId, props.node.uuid, {
        name: formState.name,
        properties: formState.properties
      })
      message.success(t('common.updateSuccess'))
      emit('success', updated)
    } else {
      // Create new node
      const created = await graphApi.createNode(props.graphId, {
        name: formState.name,
        type: formState.type,
        properties: formState.properties
      })
      message.success(t('common.createSuccess'))
      emit('success', created)
    }
    
    handleClose()
  } catch (error) {
    // Validation failed
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  emit('update:visible', false)
  formRef.value?.resetFields()
}
</script>

<style scoped lang="less">
.properties-list {
  .property-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
  }
  
  .add-property-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
  }
  
  .property-equal {
    color: #58a6ff;
    font-weight: 600;
  }
}

:deep(.ant-form-item-label > label) {
  color: #8b949e;
}

:deep(.ant-input),
:deep(.ant-select-selector) {
  background: #21262d !important;
  border-color: #30363d !important;
  color: #e6edf3 !important;
}

:deep(.ant-select-dropdown) {
  background: #161b22;
}

:deep(.ant-select-item) {
  color: #e6edf3;
}

:deep(.ant-select-item-option-selected) {
  background: rgba(88, 166, 255, 0.15);
}

:deep(.ant-select-item-option-active) {
  background: #21262d;
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

:deep(.ant-modal-footer) {
  border-top: 1px solid #30363d;
  
  .ant-btn {
    background: transparent;
    border-color: #30363d;
    color: #8b949e;
  }
  
  .ant-btn-primary {
    background: #58a6ff;
    border-color: #58a6ff;
    color: white;
  }
}
</style>
