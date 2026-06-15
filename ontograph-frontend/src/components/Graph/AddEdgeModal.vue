<template>
  <a-modal
    :open="visible"
    title="添加关联"
    width="480px"
    @ok="handleOk"
    @cancel="handleClose"
    :ok-button-props="{ loading: loading }"
  >
    <a-form layout="vertical">
      <a-form-item label="源节点">
        <a-input :value="sourceNode.name" disabled />
      </a-form-item>
      
      <a-form-item label="关系类型" required>
        <a-select
          v-model:value="formState.edgeType"
          placeholder="选择关系类型"
          show-search
          allow-create
          :filter-option="filterOption"
        >
          <a-select-option value="WORKS_FOR">WORKS_FOR (雇佣关系)</a-select-option>
          <a-select-option value="KNOWS">KNOWS (认识)</a-select-option>
          <a-select-option value="PURCHASED">PURCHASED (购买)</a-select-option>
          <a-select-option value="LOCATED_IN">LOCATED_IN (位于)</a-select-option>
          <a-select-option value="PART_OF">PART_OF (属于)</a-select-option>
          <a-select-option value="RELATES_TO">RELATES_TO (关联)</a-select-option>
          <a-select-option value="CREATED">CREATED (创建)</a-select-option>
          <a-select-option value="MANAGES">MANAGES (管理)</a-select-option>
        </a-select>
      </a-form-item>
      
      <a-form-item label="目标节点" required>
        <a-select
          v-model:value="formState.targetUuid"
          placeholder="选择目标节点"
          show-search
          :filter-option="filterNodeOption"
        >
          <a-select-option
            v-for="node in availableTargets"
            :key="node.uuid"
            :value="node.uuid"
          >
            <div class="node-option">
              <span class="node-name">{{ node.name }}</span>
              <span class="node-type">{{ node.type }}</span>
            </div>
          </a-select-option>
        </a-select>
      </a-form-item>
      
      <a-form-item label="关系描述">
        <a-textarea
          v-model:value="formState.fact"
          placeholder="可选：描述这条关系的具体内容"
          :rows="3"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
const { t } = useI18n()
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { graphApi } from '@/api/graph'
import type { GraphIDENode, GraphIDEEdge } from '@/api/graph'
import { useI18n } from 'vue-i18n'

interface Props {
  visible: boolean
  graphId: string
  sourceNode: GraphIDENode
  nodes: GraphIDENode[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success', edge: GraphIDEEdge): void
}>()

// Refs
const loading = ref(false)

// Form state
const formState = reactive({
  edgeType: '',
  targetUuid: '',
  fact: ''
})

// Computed
const availableTargets = computed(() => {
  return props.nodes.filter(node => node.uuid !== props.sourceNode.uuid)
})

// Methods
const filterOption = (input: string, option: any) => {
  return option.value.toLowerCase().includes(input.toLowerCase())
}

const filterNodeOption = (input: string, option: any) => {
  const node = props.nodes.find(n => n.uuid === option.value)
  if (!node) return false
  return (
    node.name.toLowerCase().includes(input.toLowerCase()) ||
    node.type.toLowerCase().includes(input.toLowerCase())
  )
}

const handleOk = async () => {
  if (!formState.edgeType) {
    message.warning(t('TODO_请选择或输入关系类型'))
    return
  }
  
  if (!formState.targetUuid) {
    message.warning(t('TODO_请选择目标节点'))
    return
  }
  
  loading.value = true
  try {
    const edge = await graphApi.createEdge(props.graphId, {
      sourceUuid: props.sourceNode.uuid,
      targetUuid: formState.targetUuid,
      type: formState.edgeType,
      fact: formState.fact || undefined
    })
    
    message.success(t('TODO_创建成功'))
    emit('success', edge)
    handleClose()
  } catch (error) {
    message.error(t('TODO_创建失败'))
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  formState.edgeType = ''
  formState.targetUuid = ''
  formState.fact = ''
  emit('update:visible', false)
}
</script>

<style scoped lang="less">
.node-option {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .node-name {
    flex: 1;
  }
  
  .node-type {
    font-size: 11px;
    color: #6e7681;
    background: #21262d;
    padding: 2px 6px;
    border-radius: 4px;
  }
}

:deep(.ant-form-item-label > label) {
  color: #8b949e;
}

:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-input-textarea) {
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
