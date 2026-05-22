/**
 * 约束编辑器 — 编辑单个约束详情
 */
<template>
  <div class="constraint-editor">
    <div class="editor-toolbar">
      <a-space>
        <a-button type="primary" :loading="saving" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
        <a-button danger :disabled="!constraintId" :loading="deleting" @click="handleDelete">
          <template #icon><DeleteOutlined /></template>
          删除
        </a-button>
      </a-space>
      <div class="toolbar-right">
        <a-tag v-if="constraintId" color="blue">ID: {{ constraintId }}</a-tag>
      </div>
    </div>

    <div class="editor-form">
      <a-form :model="form" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="约束类型" required>
              <a-select v-model:value="form.constraintType" disabled>
                <a-select-option value="CARDINALITY">基数约束</a-select-option>
                <a-select-option value="RANGE">值域约束</a-select-option>
                <a-select-option value="PATTERN">正则约束</a-select-option>
                <a-select-option value="REQUIRED">必填约束</a-select-option>
                <a-select-option value="ENUM">枚举约束</a-select-option>
                <a-select-option value="NOT_NULL">非空约束</a-select-option>
                <a-select-option value="CUSTOM">自定义约束</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="严重程度">
              <a-select v-model:value="form.severity">
                <a-select-option value="ERROR">错误</a-select-option>
                <a-select-option value="WARNING">警告</a-select-option>
                <a-select-option value="INFO">信息</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="关联类">
              <a-select v-model:value="form.classId" placeholder="选择类" allow-clear :options="classOptions" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="关联属性">
              <a-select v-model:value="form.propertyId" placeholder="选择属性" allow-clear :options="propertyOptions" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="约束值">
          <ConstraintValueEditor v-model:model-value="form.value" :type="form.constraintType" />
        </a-form-item>

        <a-form-item label="错误消息">
          <a-input v-model:value="form.errorMessage" placeholder="验证失败时的提示" />
        </a-form-item>

        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="3" />
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SaveOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { ontologyApi } from '@/api/ontology'
import ConstraintValueEditor from './ConstraintValueEditor.vue'

const props = defineProps<{
  graphId: string
  constraintId?: number
}>()

const emit = defineEmits<{
  (e: 'saved'): void
}>()

const store = useOntologyStore()
const saving = ref(false)
const deleting = ref(false)

const form = reactive({
  id: undefined as number | undefined,
  constraintType: 'REQUIRED',
  classId: undefined as number | undefined,
  propertyId: undefined as number | undefined,
  value: '',
  severity: 'ERROR',
  errorMessage: '',
  description: ''
})

const classOptions = computed(() => store.classes.map(c => ({ label: c.localName, value: c.id })))
const propertyOptions = computed(() => store.properties.map(p => ({ label: p.localName, value: p.id })))

function loadData() {
  if (!props.constraintId) return
  const c = store.constraints.find(x => x.id === props.constraintId)
  if (!c) return

  Object.assign(form, {
    id: c.id,
    constraintType: c.constraintType,
    classId: c.classId,
    propertyId: c.propertyId,
    value: c.value || '',
    severity: c.severity || 'ERROR',
    errorMessage: c.errorMessage || '',
    description: c.description || ''
  })
}

async function handleSave() {
  if (!props.constraintId) return
  saving.value = true
  try {
    await ontologyApi.updateConstraint(props.graphId, props.constraintId, {
      classId: form.classId,
      propertyId: form.propertyId,
      constraintType: form.constraintType,
      value: form.value,
      severity: form.severity,
      errorMessage: form.errorMessage,
      description: form.description
    })
    message.success('约束已更新')
    await store.loadFullOntology(props.graphId)
    emit('saved')
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!props.constraintId) return
  Modal.confirm({
    title: '确定删除此约束？',
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      deleting.value = true
      try {
        await ontologyApi.deleteConstraint(props.graphId, props.constraintId!)
        message.success('约束已删除')
        await store.loadFullOntology(props.graphId)
        emit('saved')
      } catch (e: any) {
        message.error(e.message || '删除失败')
      } finally {
        deleting.value = false
      }
    }
  })
}

onMounted(() => loadData())
watch(() => props.constraintId, () => loadData())
</script>

<style scoped lang="less">
.constraint-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  .editor-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: #161b22;
    border-bottom: 1px solid #30363d;
    flex-shrink: 0;

    .toolbar-right {
      display: flex;
      gap: 8px;
      align-items: center;
    }
  }

  .editor-form {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    max-width: 800px;
  }
}
</style>
