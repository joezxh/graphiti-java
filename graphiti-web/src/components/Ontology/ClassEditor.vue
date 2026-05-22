/**
 * 类编辑器 — 本体工作台内嵌形式，支持多Tab
 * [基本信息] [属性列表] [继承关系] [约束规则] [实例数据]
 */
<template>
  <div class="class-editor">
    <!-- 编辑器工具栏 -->
    <div class="editor-toolbar">
      <a-space>
        <a-button type="primary" :loading="saving" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
        <a-button v-if="isNew" type="default" @click="handleSave">保存并新建</a-button>
        <a-divider type="vertical" />
        <a-button danger :disabled="!classId" :loading="deleting" @click="handleDelete">
          <template #icon><DeleteOutlined /></template>
          删除
        </a-button>
      </a-space>
      <div class="toolbar-right">
        <a-tag v-if="classId" color="blue">ID: {{ classId }}</a-tag>
        <a-tag v-if="form.localName" color="green">{{ form.localName }}</a-tag>
      </div>
    </div>

    <!-- Tab页签 -->
    <a-tabs v-model:activeKey="activeTab" class="class-editor-tabs">
      <a-tab-pane key="basic" tab="基本信息">
        <div class="tab-content">
          <a-form :model="form" layout="vertical" class="basic-form">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="类名称（localName）" required>
                  <a-input v-model:value="form.localName" placeholder="如 Person" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="完整URI（classUri）">
                  <a-input v-model:value="form.classUri" placeholder="http://graphiti.io/ontology/Person" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="父类（多继承）">
                  <a-select
                    v-model:value="form.parentClassIds"
                    mode="multiple"
                    placeholder="选择父类"
                    allow-clear
                    :options="classOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="领域分类">
                  <a-select v-model:value="form.domainHint" placeholder="选择领域" allow-clear>
                    <a-select-option value="FINANCIAL">金融</a-select-option>
                    <a-select-option value="MEDICAL">医疗</a-select-option>
                    <a-select-option value="ECOMMERCE">电商</a-select-option>
                    <a-select-option value="LEGAL">法律</a-select-option>
                    <a-select-option value="KNOWLEDGE">通用知识</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="等价类（equivalentTo）">
              <a-select v-model:value="form.equivalentTo" mode="multiple" placeholder="选择等价类" allow-clear :options="classOptions" />
            </a-form-item>

            <a-form-item label="不相交类（disjointWith）">
              <a-select v-model:value="form.disjointWith" mode="multiple" placeholder="选择不相交类" allow-clear :options="classOptions" />
            </a-form-item>

            <a-form-item label="描述">
              <a-textarea v-model:value="form.description" :rows="3" placeholder="类的功能描述..." />
            </a-form-item>

            <a-form-item label="示例">
              <a-textarea v-model:value="form.example" :rows="2" placeholder="类的使用示例..." />
            </a-form-item>
          </a-form>
        </div>
      </a-tab-pane>

      <a-tab-pane key="properties" tab="属性列表">
        <div class="tab-content">
          <div class="section-header">
            <span class="section-title">类属性（直接定义 + 继承）</span>
            <a-button size="small" type="primary" @click="showAddProperty = true">
              <template #icon><PlusOutlined /></template>
              添加属性
            </a-button>
          </div>
          <a-table
            :columns="propertyColumns"
            :data-source="classProperties"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'propertyType'">
                <a-tag :color="getPropertyTypeColor(record.propertyType)">{{ record.propertyType }}</a-tag>
              </template>
              <template v-if="column.key === 'isRequired'">
                <a-tag :color="record.isRequired ? 'red' : 'default'">
                  {{ record.isRequired ? '必填' : '可选' }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="openProperty(record)">编辑</a-button>
                  <a-popconfirm title="确定删除此属性？" ok-text="确定" cancel-text="取消" @confirm="deleteProperty(record)">
                    <a-button type="link" size="small" danger>删除</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>

      <a-tab-pane key="inheritance" tab="继承关系">
        <div class="tab-content">
          <div class="section-header">
            <span class="section-title">继承树</span>
          </div>
          <div class="inheritance-tree">
            <div v-if="inheritancePath.length === 0" class="empty-tip">无继承关系（顶级类）</div>
            <div v-else class="inheritance-path">
              <span v-for="(cls, idx) in inheritancePath" :key="cls.id" class="path-item">
                <span
                  class="path-node"
                  :class="{ current: cls.id === classId, clickable: cls.id !== classId }"
                  @click="cls.id !== classId ? openParentClass(cls) : undefined"
                >{{ cls.localName }}</span>
                <span v-if="idx < inheritancePath.length - 1" class="path-arrow">→</span>
              </span>
            </div>
            <div class="subclasses-section">
              <div class="section-title">子类</div>
              <div v-if="subclasses.length === 0" class="empty-tip">无子类</div>
              <div v-else class="subclass-list">
                <span v-for="sub in subclasses" :key="sub.id" class="subclass-tag" @click="openSubclass(sub)">
                  {{ sub.localName }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="constraints" tab="约束规则">
        <div class="tab-content">
          <div class="section-header">
            <span class="section-title">类约束</span>
            <a-button size="small" type="primary" @click="showAddConstraint = true">
              <template #icon><PlusOutlined /></template>
              添加约束
            </a-button>
          </div>
          <a-table
            :columns="constraintColumns"
            :data-source="classConstraints"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'severity'">
                <a-tag :color="getSeverityColor(record.severity)">{{ record.severity }}</a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-popconfirm title="确定删除？" ok-text="确定" cancel-text="取消" @confirm="deleteConstraint(record)">
                  <a-button type="link" size="small" danger>删除</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
          <div v-if="classConstraints.length === 0" class="empty-tip">暂无约束</div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="instances" tab="实例数据">
        <div class="tab-content">
          <div class="instance-stats-bar">
            <div class="stat-item">
              <span class="stat-label">实例数量</span>
              <span class="stat-value">{{ instanceCount }}</span>
            </div>
          </div>
          <a-table
            :columns="instanceColumns"
            :data-source="instanceList"
            :pagination="instancePagination"
            :loading="instanceLoading"
            row-key="uuid"
            size="small"
            @change="handleInstanceTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a-button type="link" size="small" @click="viewInstance(record)">{{ record.name }}</a-button>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- 新建属性 Modal -->
    <a-modal v-model:open="showAddProperty" title="添加属性" @ok="handleAddProperty" :confirm-loading="addingProperty">
      <a-form :model="propertyForm" layout="vertical">
        <a-form-item label="属性名称" required>
          <a-input v-model:value="propertyForm.localName" placeholder="如 name" />
        </a-form-item>
        <a-form-item label="属性类型" required>
          <a-select v-model:value="propertyForm.propertyType">
            <a-select-option value="DATATYPE">数据类型属性</a-select-option>
            <a-select-option value="OBJECT">对象属性</a-select-option>
            <a-select-option value="ANNOTATION">注解属性</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="propertyForm.propertyType === 'DATATYPE'" label="数据类型">
          <a-select v-model:value="propertyForm.rangeDataType">
            <a-select-option value="string">字符串</a-select-option>
            <a-select-option value="integer">整数</a-select-option>
            <a-select-option value="float">浮点数</a-select-option>
            <a-select-option value="boolean">布尔值</a-select-option>
            <a-select-option value="date">日期</a-select-option>
            <a-select-option value="datetime">日期时间</a-select-option>
            <a-select-option value="json">JSON</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="必填">
          <a-switch v-model:checked="propertyForm.isRequired" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 新建约束 Modal -->
    <a-modal v-model:open="showAddConstraint" title="添加约束" @ok="handleAddConstraint" :confirm-loading="addingConstraint">
      <a-form :model="constraintForm" layout="vertical">
        <a-form-item label="约束类型" required>
          <a-select v-model:value="constraintForm.constraintType">
            <a-select-option value="CARDINALITY">基数约束</a-select-option>
            <a-select-option value="RANGE">值域约束</a-select-option>
            <a-select-option value="PATTERN">正则约束</a-select-option>
            <a-select-option value="NOT_NULL">非空约束</a-select-option>
            <a-select-option value="ENUM">枚举约束</a-select-option>
            <a-select-option value="CUSTOM">自定义约束</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="约束值">
          <ConstraintValueEditor v-model:model-value="constraintForm.value" :type="constraintForm.constraintType" />
        </a-form-item>
        <a-form-item label="严重程度">
          <a-select v-model:value="constraintForm.severity">
            <a-select-option value="ERROR">错误</a-select-option>
            <a-select-option value="WARNING">警告</a-select-option>
            <a-select-option value="INFO">信息</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="错误消息">
          <a-input v-model:value="constraintForm.errorMessage" placeholder="验证失败时的提示" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="constraintForm.description" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SaveOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { ontologyApi } from '@/api/ontology'
import { graphApi } from '@/api/graph'
import type { OntClassVO, OntPropertyVO, OntConstraintVO } from '@/api/ontology'
import type { ClassInstance } from '@/api/graph'
import ConstraintValueEditor from './ConstraintValueEditor.vue'

const props = defineProps<{
  graphId: string
  classId?: number
}>()

const emit = defineEmits<{
  (e: 'saved'): void
}>()

const store = useOntologyStore()
const saving = ref(false)
const deleting = ref(false)
const activeTab = ref('basic')
const showAddProperty = ref(false)
const showAddConstraint = ref(false)
const addingProperty = ref(false)
const addingConstraint = ref(false)

const isNew = computed(() => !props.classId)

const form = reactive({
  id: undefined as number | undefined,
  localName: '',
  classUri: '',
  parentClassId: undefined as number | undefined,
  parentClassIds: [] as number[],
  domainHint: '',
  description: '',
  example: '',
  equivalentTo: [] as number[],
  disjointWith: [] as number[]
})

const propertyForm = reactive({
  localName: '',
  propertyType: 'DATATYPE',
  rangeDataType: 'string',
  isRequired: false,
  minCardinality: 0,
  maxCardinality: undefined as number | undefined
})

const constraintForm = reactive({
  constraintType: 'REQUIRED',
  value: '',
  severity: 'ERROR',
  errorMessage: '',
  description: ''
})

const propertyColumns = [
  { title: '名称', dataIndex: 'localName', key: 'localName' },
  { title: '类型', key: 'propertyType' },
  { title: '数据类型', dataIndex: 'rangeDataType', key: 'rangeDataType' },
  { title: '必填', key: 'isRequired' },
  { title: '操作', key: 'action', width: 160 }
]

const constraintColumns = [
  { title: '类型', dataIndex: 'constraintType', key: 'constraintType' },
  { title: '约束值', dataIndex: 'value', key: 'value', ellipsis: true },
  { title: '严重程度', key: 'severity' },
  { title: '操作', key: 'action', width: 80 }
]

const classOptions = computed(() =>
  store.classes.map(c => ({ label: c.localName, value: c.id }))
)

const classProperties = computed(() => {
  if (!props.classId) return []
  return store.properties.filter(p => p.domainClassId === props.classId)
})

const classConstraints = computed(() => {
  if (!props.classId) return []
  return store.constraints.filter(c => c.classId === props.classId)
})

const inheritancePath = computed(() => {
  const path: OntClassVO[] = []
  let current = store.classes.find(c => c.id === props.classId)
  while (current) {
    path.unshift(current)
    if (current.parentClassId) {
      current = store.classes.find(c => c.id === current!.parentClassId)
    } else {
      break
    }
  }
  return path
})

const subclasses = computed(() =>
  store.classes.filter(c => c.parentClassId === props.classId)
)

const instanceCount = ref(0)
const instanceList = ref<ClassInstance[]>([])
const instanceLoading = ref(false)
const instancePagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (total: number) => `共 ${total} 条`
})

const instanceColumns = [
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: 'UUID', dataIndex: 'uuid', key: 'uuid', ellipsis: true },
  { title: '类型', dataIndex: 'type', key: 'type' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' }
]

async function loadData() {
  if (!props.classId) return
  const cls = store.classes.find(c => c.id === props.classId)
  if (!cls) return

  // 将后端返回的 URI/localName 字符串数组转换为类ID数组
  const equivIds: number[] = []
  if (Array.isArray(cls.equivalentTo)) {
    for (const uri of cls.equivalentTo) {
      const found = store.classes.find(c => c.classUri === uri || c.localName === uri)
      if (found) equivIds.push(found.id)
    }
  }
  const disjointIds: number[] = []
  if (Array.isArray(cls.disjointWith)) {
    for (const name of cls.disjointWith) {
      const found = store.classes.find(c => c.localName === name || c.classUri === name)
      if (found) disjointIds.push(found.id)
    }
  }

  Object.assign(form, {
    id: cls.id,
    localName: cls.localName,
    classUri: cls.classUri || '',
    parentClassId: cls.parentClassId,
    parentClassIds: cls.parentClassId ? [cls.parentClassId] : [],
    domainHint: cls.domainHint || '',
    description: cls.description || '',
    example: cls.example || '',
    equivalentTo: equivIds,
    disjointWith: disjointIds
  })

  await loadInstances()
}

async function handleSave() {
  if (!form.localName.trim()) {
    message.error('请填写类名称')
    return
  }
  saving.value = true
  try {
    const data = {
      localName: form.localName,
      classUri: form.classUri || undefined,
      parentClassId: form.parentClassIds[0] || undefined,
      equivalentTo: form.equivalentTo?.length
        ? form.equivalentTo.map((id: number) => {
            const cls = store.classes.find(c => c.id === id)
            return cls?.classUri || `http://graphiti.io/${cls?.localName || id}`
          }).filter(Boolean)
        : undefined,
      disjointWith: form.disjointWith?.length
        ? form.disjointWith.map((id: number) => {
            const cls = store.classes.find(c => c.id === id)
            return cls?.localName || String(id)
          }).filter(Boolean)
        : undefined,
      domainHint: form.domainHint || undefined,
      description: form.description || undefined,
      example: form.example || undefined
    }
    if (props.classId) {
      await ontologyApi.updateClass(props.graphId, props.classId, data)
      message.success('类已更新')
    } else {
      await ontologyApi.createClass(props.graphId, data)
      message.success('类已创建')
      emit('saved')
    }
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!props.classId) return
  Modal.confirm({
    title: '确定删除此类？',
    content: '删除类不会自动删除其实例，是否继续？',
    okText: '确定',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      deleting.value = true
      try {
        await ontologyApi.deleteClass(props.graphId, props.classId!)
        message.success('类已删除')
        store.loadFullOntology(props.graphId)
        emit('saved')
      } catch (e: any) {
        message.error(e.message || '删除失败')
      } finally {
        deleting.value = false
      }
    }
  })
}

async function handleAddProperty() {
  if (!propertyForm.localName.trim()) {
    message.error('请填写属性名称')
    return
  }
  addingProperty.value = true
  try {
    await ontologyApi.createProperty(props.graphId, {
      localName: propertyForm.localName,
      propertyType: propertyForm.propertyType as any,
      rangeDataType: propertyForm.rangeDataType || undefined,
      domainClassId: props.classId,
      isRequired: propertyForm.isRequired,
      minCardinality: propertyForm.minCardinality,
      maxCardinality: propertyForm.maxCardinality
    })
    message.success('属性已添加')
    showAddProperty.value = false
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || '添加失败')
  } finally {
    addingProperty.value = false
  }
}

function openProperty(_prop: OntPropertyVO) {
  // Reserved for future: open property in new tab
}

async function deleteProperty(prop: OntPropertyVO) {
  try {
    await ontologyApi.deleteProperty(props.graphId, prop.id)
    message.success('属性已删除')
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

async function handleAddConstraint() {
  addingConstraint.value = true
  try {
    await ontologyApi.createConstraint(props.graphId, {
      classId: props.classId,
      constraintType: constraintForm.constraintType,
      value: constraintForm.value,
      severity: constraintForm.severity,
      errorMessage: constraintForm.errorMessage,
      description: constraintForm.description
    })
    message.success('约束已添加')
    showAddConstraint.value = false
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || '添加失败')
  } finally {
    addingConstraint.value = false
  }
}

async function deleteConstraint(c: OntConstraintVO) {
  try {
    await ontologyApi.deleteConstraint(props.graphId, c.id)
    message.success('约束已删除')
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

function openSubclass(cls: OntClassVO) {
  store.openTab({ id: `class-editor-${cls.id}`, type: 'class-editor', title: `类: ${cls.localName}`, classId: cls.id })
}

function openParentClass(cls: OntClassVO) {
  store.openTab({ id: `class-editor-${cls.id}`, type: 'class-editor', title: `类: ${cls.localName}`, classId: cls.id })
}

async function loadInstances() {
  if (!props.classId || !form.localName) {
    instanceList.value = []
    instanceCount.value = 0
    instancePagination.total = 0
    return
  }
  instanceLoading.value = true
  try {
    const res = await graphApi.getClassInstances(props.graphId, form.localName, {
      page: instancePagination.current,
      pageSize: instancePagination.pageSize
    })
    instanceList.value = res.data || []
    instanceCount.value = res.total || 0
    instancePagination.total = res.total || 0
  } catch (e: any) {
    message.error(e.message || '加载实例失败')
  } finally {
    instanceLoading.value = false
  }
}

function handleInstanceTableChange(pagination: any) {
  instancePagination.current = pagination.current
  instancePagination.pageSize = pagination.pageSize
  loadInstances()
}

function viewInstance(record: ClassInstance) {
  store.openTab({
    id: `instance-editor-${record.uuid}`,
    type: 'instance-editor',
    title: `实例: ${record.name}`,
    classType: record.type
  })
}

function getPropertyTypeColor(type: string) {
  return { DATATYPE: 'blue', OBJECT: 'green', ANNOTATION: 'purple', TRANSITIVE: 'orange', SYMMETRIC: 'cyan', FUNCTIONAL: 'magenta' }[type] ?? 'default'
}

function getSeverityColor(severity: string) {
  return { ERROR: 'red', WARNING: 'orange', INFO: 'blue' }[severity] ?? 'default'
}

onMounted(() => loadData())
watch(() => props.classId, () => loadData())
</script>

<style scoped lang="less">
.class-editor {
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

  .class-editor-tabs {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;

    :deep(.ant-tabs-content) {
      height: 100%;
      flex: 1;
    }

    :deep(.ant-tabs-tabpane) {
      height: 100%;
      overflow-y: auto;
    }

    :deep(.ant-tabs-nav) {
      margin: 0;
      padding: 0 16px;
      background: #161b22;

      .ant-tabs-tab {
        color: #8b949e;
        font-size: 13px;

        &.ant-tabs-tab-active { color: #e6edf3; }
        &:hover { color: #e6edf3; }
      }
    }
  }

  .tab-content {
    padding: 20px;
  }

  .basic-form {
    max-width: 800px;
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    .section-title {
      font-size: 14px;
      font-weight: 600;
      color: #e6edf3;
    }
  }

  .inheritance-tree {
    .empty-tip {
      color: #6e7681;
      font-size: 13px;
      padding: 16px 0;
    }

    .inheritance-path {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 24px;
      padding: 16px;
      background: #161b22;
      border-radius: 8px;
      border: 1px solid #30363d;

      .path-node {
        padding: 4px 12px;
        background: #21262d;
        border-radius: 4px;
        font-size: 13px;
        color: #8b949e;

        &.current { background: rgba(88, 166, 255, 0.2); color: #58a6ff; border: 1px solid #58a6ff; }

        &.clickable {
          cursor: pointer;
          &:hover { background: #30363d; color: #e6edf3; }
        }
      }
      .path-arrow { color: #6e7681; }
    }

    .subclasses-section {
      margin-top: 16px;

      .section-title {
        font-size: 14px;
        font-weight: 600;
        color: #e6edf3;
      }

      .subclass-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-top: 8px;

        .subclass-tag {
          padding: 4px 12px;
          background: rgba(230, 237, 243, 0.1);
          border: 1px solid rgba(230, 237, 243, 0.3);
          border-radius: 4px;
          font-size: 13px;
          color: #e6edf3;
          cursor: pointer;
          transition: all 0.15s;

          &:hover { background: rgba(230, 237, 243, 0.2); }
        }
      }
    }
  }

  .instance-stats-bar {
    display: flex;
    align-items: center;
    gap: 24px;
    margin-bottom: 16px;
    padding: 12px 16px;
    background: #161b22;
    border-radius: 8px;
    border: 1px solid #30363d;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 8px;

      .stat-label {
        font-size: 13px;
        color: #8b949e;
      }

      .stat-value {
        font-size: 16px;
        font-weight: 600;
        color: #e6edf3;
      }
    }
  }
}
</style>
