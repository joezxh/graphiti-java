<template>
  <div class="constraint-list-panel">
    <div class="panel-toolbar">
      <a-space>
        <a-button type="primary" @click="showModal = true">
          <template #icon><PlusOutlined /></template>
          新建约束
        </a-button>
        <a-button :loading="refreshing" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </div>

    <a-table
      :columns="columns"
      :data-source="store.constraints"
      :loading="store.loading"
      :pagination="{ pageSize: 20 }"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'severity'">
          <a-tag :color="severityColor(record.severity)">{{ record.severity }}</a-tag>
        </template>
        <template v-if="column.key === 'class'">
          {{ getClassName(record.classId) }}
        </template>
        <template v-if="column.key === 'property'">
          {{ getPropertyName(record.propertyId) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确定删除？" ok-text="确定" @confirm="handleDelete(record)">
            <a-button type="link" size="small" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <a-modal v-model:open="showModal" title="新建约束" @ok="handleCreate">
      <a-form :model="form" layout="vertical">
        <a-form-item label="约束类型" required>
          <a-select v-model:value="form.constraintType">
            <a-select-option value="CARDINALITY">基数约束</a-select-option>
            <a-select-option value="RANGE">值域约束</a-select-option>
            <a-select-option value="PATTERN">正则约束</a-select-option>
            <a-select-option value="REQUIRED">必填约束</a-select-option>
            <a-select-option value="ENUM">枚举约束</a-select-option>
            <a-select-option value="NOT_NULL">非空约束</a-select-option>
            <a-select-option value="CUSTOM">自定义约束</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="关联类">
          <a-select v-model:value="form.classId" placeholder="选择类" allow-clear :options="classOptions" />
        </a-form-item>
        <a-form-item label="关联属性">
          <a-select v-model:value="form.propertyId" placeholder="选择属性" allow-clear :options="propertyOptions" />
        </a-form-item>
        <a-form-item label="约束值">
          <ConstraintValueEditor v-model:model-value="form.value" :type="form.constraintType" />
        </a-form-item>
        <a-form-item label="严重程度">
          <a-select v-model:value="form.severity">
            <a-select-option value="ERROR">错误</a-select-option>
            <a-select-option value="WARNING">警告</a-select-option>
            <a-select-option value="INFO">信息</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="错误消息">
          <a-input v-model:value="form.errorMessage" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { ontologyApi } from '@/api/ontology'
import ConstraintValueEditor from './ConstraintValueEditor.vue'

const props = defineProps<{ graphId: string }>()
const store = useOntologyStore()
const refreshing = ref(false)
const showModal = ref(false)

const form = reactive({
  constraintType: 'REQUIRED',
  classId: undefined as number | undefined,
  propertyId: undefined as number | undefined,
  value: '',
  severity: 'ERROR',
  errorMessage: '',
  description: ''
})

const columns = [
  { title: '类型', dataIndex: 'constraintType', key: 'constraintType' },
  { title: '关联类', key: 'class', width: 120 },
  { title: '关联属性', key: 'property', width: 120 },
  { title: '约束值', dataIndex: 'value', key: 'value', ellipsis: true },
  { title: '严重程度', key: 'severity', width: 100 },
  { title: '错误消息', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
  { title: '操作', key: 'action', width: 80 }
]

const classOptions = computed(() => store.classes.map(c => ({ label: c.localName, value: c.id })))
const propertyOptions = computed(() => store.properties.map(p => ({ label: p.localName, value: p.id })))

function getClassName(id?: number) { return id ? (store.classes.find(c => c.id === id)?.localName ?? '-') : '-' }
function getPropertyName(id?: number) { return id ? (store.properties.find(p => p.id === id)?.localName ?? '-') : '-' }
function severityColor(s?: string) { return { ERROR: 'red', WARNING: 'orange', INFO: 'blue' }[s ?? ''] ?? 'default' }

async function handleCreate() {
  try {
    await ontologyApi.createConstraint(props.graphId, { ...form })
    message.success('约束已创建')
    showModal.value = false
    await store.loadFullOntology(props.graphId)
  } catch (e: any) { message.error(e.message || '创建失败') }
}

async function handleDelete(record: any) {
  try {
    await ontologyApi.deleteConstraint(props.graphId, record.id)
    message.success('删除成功')
    await store.loadFullOntology(props.graphId)
  } catch (e: any) { message.error(e.message || '删除失败') }
}

async function handleRefresh() {
  refreshing.value = true
  await store.loadFullOntology(props.graphId)
  refreshing.value = false
  message.success('已刷新')
}
</script>

<style scoped lang="less">
.constraint-list-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  padding: 16px;
  .panel-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
}
</style>
