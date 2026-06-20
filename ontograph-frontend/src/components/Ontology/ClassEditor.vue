/**
 * 类编辑器 — 本体工作台内嵌形式，支持多Tab
 * [基本信息] [属性列表] [继承关系] [约束规则]
 */
<template>
  <div class="class-editor">
    <!-- 编辑器工具栏 -->
    <div class="editor-toolbar">
      <a-space>
        <a-button type="primary" :loading="saving" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          {{ t('common.save') }}
        </a-button>
        <a-button v-if="isNew" type="default" @click="handleSave">{{ t('classEditor.saveAndNew') }}</a-button>
        <a-divider type="vertical" />
        <a-button danger :disabled="!classId" :loading="deleting" @click="handleDelete">
          <template #icon><DeleteOutlined /></template>
          {{ t('common.delete') }}
        </a-button>
      </a-space>
      <div class="toolbar-right">
        <a-tag v-if="classId" color="blue">ID: {{ classId }}</a-tag>
        <a-tag v-if="form.localName" color="green">{{ form.localName }}</a-tag>
      </div>
    </div>

    <!-- Tab页签 -->
    <a-tabs v-model:activeKey="activeTab" class="class-editor-tabs">
      <a-tab-pane key="basic" :tab="t('classEditor.tabBasic')">
        <div class="tab-content">
          <a-form :model="form" layout="vertical" class="basic-form">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item :label="t('classEditor.className')" required>
                  <a-input v-model:value="form.localName" :placeholder="t('classEditor.classNamePlaceholder')" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('classEditor.classUri')">
                  <a-input v-model:value="form.classUri" placeholder="http://ontograph.io/ontology/Person" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item :label="t('classEditor.parentClass')">
                  <a-select
                    v-model:value="form.parentClassIds"
                    mode="multiple"
                    :placeholder="t('classEditor.selectParentClass')"
                    allow-clear
                    :options="classOptions"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('classEditor.domain')">
                  <a-select v-model:value="form.domainHint" :placeholder="t('classEditor.selectDomain')" allow-clear>
                    <a-select-option value="FINANCIAL">{{ t('classEditor.domainFinancial') }}</a-select-option>
                    <a-select-option value="MEDICAL">{{ t('classEditor.domainMedical') }}</a-select-option>
                    <a-select-option value="ECOMMERCE">{{ t('classEditor.domainEcommerce') }}</a-select-option>
                    <a-select-option value="LEGAL">{{ t('classEditor.domainLegal') }}</a-select-option>
                    <a-select-option value="KNOWLEDGE">{{ t('classEditor.domainKnowledge') }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item :label="t('classEditor.equivalentTo')">
              <a-select v-model:value="form.equivalentTo" mode="multiple" :placeholder="t('classEditor.selectEquivalent')" allow-clear :options="classOptions" />
            </a-form-item>

            <a-form-item :label="t('classEditor.disjointWith')">
              <a-select v-model:value="form.disjointWith" mode="multiple" :placeholder="t('classEditor.selectDisjoint')" allow-clear :options="classOptions" />
            </a-form-item>

            <a-form-item :label="t('classEditor.description')">
              <a-textarea v-model:value="form.description" :rows="3" :placeholder="t('classEditor.descriptionPlaceholder')" />
            </a-form-item>

            <a-form-item :label="t('classEditor.example')">
              <a-textarea v-model:value="form.example" :rows="2" :placeholder="t('classEditor.examplePlaceholder')" />
            </a-form-item>
          </a-form>
        </div>
      </a-tab-pane>

      <a-tab-pane key="properties" :tab="t('classEditor.tabProperties')">
        <div class="tab-content">
          <div class="section-header">
            <span class="section-title">{{ t('classEditor.classProperties') }}</span>
            <a-button size="small" type="primary" @click="showAddProperty = true">
              <template #icon><PlusOutlined /></template>
              {{ t('classEditor.addProperty') }}
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
                  {{ record.isRequired ? t('classEditor.required') : t('classEditor.optional') }}
                </a-tag>
              </template>
              <template v-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="openProperty(record)">{{ t('common.edit') }}</a-button>
                  <a-popconfirm :title="t('classEditor.confirmDeleteProperty')" :ok-text="t('common.confirm')" :cancel-text="t('common.cancel')" @confirm="deleteProperty(record)">
                    <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>
      </a-tab-pane>

      <a-tab-pane key="inheritance" :tab="t('classEditor.tabInheritance')">
        <div class="tab-content">
          <div class="section-header">
            <span class="section-title">{{ t('classEditor.inheritanceTree') }}</span>
          </div>
          <div class="inheritance-tree">
            <div v-if="inheritancePath.length === 0" class="empty-tip">{{ t('classEditor.noInheritance') }}</div>
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
              <div class="section-title">{{ t('classEditor.subclasses') }}</div>
              <div v-if="subclasses.length === 0" class="empty-tip">{{ t('classEditor.noSubclasses') }}</div>
              <div v-else class="subclass-list">
                <span v-for="sub in subclasses" :key="sub.id" class="subclass-tag" @click="openSubclass(sub)">
                  {{ sub.localName }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="constraints" :tab="t('classEditor.tabConstraints')">
        <div class="tab-content">
          <div class="section-header">
            <span class="section-title">{{ t('classEditor.classConstraints') }}</span>
            <a-button size="small" type="primary" @click="showAddConstraint = true">
              <template #icon><PlusOutlined /></template>
              {{ t('classEditor.addConstraint') }}
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
                <a-space>
                  <a-button type="link" size="small" @click="openConstraint(record)">{{ t('common.edit') }}</a-button>
                  <a-popconfirm :title="t('classEditor.confirmDeleteConstraint')" :ok-text="t('common.confirm')" :cancel-text="t('common.cancel')" @confirm="deleteConstraint(record)">
                    <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
                  </a-popconfirm>
                </a-space>
              </template>
            </template>
          </a-table>
          <div v-if="classConstraints.length === 0" class="empty-tip">{{ t('classEditor.noConstraints') }}</div>
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- 新建/编辑属性 Modal -->
    <a-modal v-model:open="showAddProperty" :title="editingPropertyId ? t('classEditor.editProperty') : t('classEditor.addPropertyModal')" @ok="handleAddProperty" :confirm-loading="addingProperty">
      <a-form :model="propertyForm" layout="vertical">
        <a-form-item :label="t('classEditor.propertyName')" required>
          <a-input v-model:value="propertyForm.localName" :placeholder="t('classEditor.propertyNamePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('classEditor.propertyType')" required>
          <a-select v-model:value="propertyForm.propertyType">
            <a-select-option value="DATATYPE">{{ t('classEditor.typeDatatype') }}</a-select-option>
            <a-select-option value="OBJECT">{{ t('classEditor.typeObject') }}</a-select-option>
            <a-select-option value="ANNOTATION">{{ t('classEditor.typeAnnotation') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="propertyForm.propertyType === 'DATATYPE'" :label="t('classEditor.dataType')">
          <a-select v-model:value="propertyForm.rangeDataType">
            <a-select-option value="string">{{ t('classEditor.dataTypeString') }}</a-select-option>
            <a-select-option value="integer">{{ t('classEditor.dataTypeInteger') }}</a-select-option>
            <a-select-option value="float">{{ t('classEditor.dataTypeFloat') }}</a-select-option>
            <a-select-option value="boolean">{{ t('classEditor.dataTypeBoolean') }}</a-select-option>
            <a-select-option value="date">{{ t('classEditor.dataTypeDate') }}</a-select-option>
            <a-select-option value="datetime">{{ t('classEditor.dataTypeDatetime') }}</a-select-option>
            <a-select-option value="json">JSON</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="t('classEditor.required')">
          <a-switch v-model:checked="propertyForm.isRequired" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 新建/编辑约束 Modal -->
    <a-modal v-model:open="showAddConstraint" :title="editingConstraintId ? t('classEditor.editConstraint') : t('classEditor.addConstraintModal')" @ok="handleAddConstraint" :confirm-loading="addingConstraint">
      <a-form :model="constraintForm" layout="vertical">
        <a-form-item :label="t('classEditor.constraintType')" required>
          <a-select v-model:value="constraintForm.constraintType">
            <a-select-option value="CARDINALITY">{{ t('classEditor.constraintCardinality') }}</a-select-option>
            <a-select-option value="RANGE">{{ t('classEditor.constraintRange') }}</a-select-option>
            <a-select-option value="PATTERN">{{ t('classEditor.constraintPattern') }}</a-select-option>
            <a-select-option value="NOT_NULL">{{ t('classEditor.constraintNotNull') }}</a-select-option>
            <a-select-option value="ENUM">{{ t('classEditor.constraintEnum') }}</a-select-option>
            <a-select-option value="CUSTOM">{{ t('classEditor.constraintCustom') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="t('classEditor.constraintValue')">
          <ConstraintValueEditor v-model:model-value="constraintForm.value" :type="constraintForm.constraintType" />
        </a-form-item>
        <a-form-item :label="t('classEditor.severity')">
          <a-select v-model:value="constraintForm.severity">
            <a-select-option value="ERROR">{{ t('classEditor.severityError') }}</a-select-option>
            <a-select-option value="WARNING">{{ t('classEditor.severityWarning') }}</a-select-option>
            <a-select-option value="INFO">{{ t('classEditor.severityInfo') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="t('classEditor.errorMessage')">
          <a-input v-model:value="constraintForm.errorMessage" :placeholder="t('classEditor.errorMessagePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('classEditor.description')">
          <a-textarea v-model:value="constraintForm.description" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { SaveOutlined, DeleteOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { ontologyApi } from '@/api/ontology'
import type { OntClassVO, OntPropertyVO, OntConstraintVO } from '@/api/ontology'
import ConstraintValueEditor from './ConstraintValueEditor.vue'

const { t } = useI18n()

const props = defineProps<{
  graphId: string
  classId?: number
}>()

const emit = defineEmits<{
  (e: 'saved'): void
  (e: 'deleted'): void
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

const editingPropertyId = ref<number | null>(null)

const constraintForm = reactive({
  constraintType: 'REQUIRED',
  value: '',
  severity: 'ERROR',
  errorMessage: '',
  description: ''
})

const editingConstraintId = ref<number | null>(null)

const propertyColumns = computed(() => [
  { title: t('classEditor.colName'), dataIndex: 'localName', key: 'localName' },
  { title: t('classEditor.colType'), key: 'propertyType' },
  { title: t('classEditor.colDataType'), dataIndex: 'rangeDataType', key: 'rangeDataType' },
  { title: t('classEditor.colRequired'), key: 'isRequired' },
  { title: t('classEditor.colActions'), key: 'action', width: 160 }
])

const constraintColumns = computed(() => [
  { title: t('classEditor.colConstraintType'), dataIndex: 'constraintType', key: 'constraintType' },
  { title: t('classEditor.colConstraintValue'), dataIndex: 'value', key: 'value', ellipsis: true },
  { title: t('classEditor.colSeverity'), key: 'severity' },
  { title: t('classEditor.colActions'), key: 'action', width: 140 }
])

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

async function loadData() {
  if (!props.classId) return
  const cls = store.classes.find(c => c.id === props.classId)
  if (!cls) return

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
}

async function handleSave() {
  if (!form.localName.trim()) {
    message.error(t('classEditor.errorClassName'))
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
            return cls?.classUri || `http://ontograph.io/${cls?.localName || id}`
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
      message.success(t('classEditor.classUpdated'))
    } else {
      await ontologyApi.createClass(props.graphId, data)
      message.success(t('classEditor.classCreated'))
      emit('saved')
    }
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || t('common.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!props.classId) return
  Modal.confirm({
    title: t('classEditor.confirmDeleteClass'),
    content: t('classEditor.confirmDeleteClassContent'),
    okText: t('common.confirm'),
    okType: 'danger',
    cancelText: t('common.cancel'),
    async onOk() {
      deleting.value = true
      try {
        await ontologyApi.deleteClass(props.graphId, props.classId!)
        message.success(t('classEditor.classDeleted'))
        store.loadFullOntology(props.graphId)
        emit('saved')
        emit('deleted')
      } catch (e: any) {
        message.error(e.message || t('common.deleteFailed'))
      } finally {
        deleting.value = false
      }
    }
  })
}

async function handleAddProperty() {
  if (!propertyForm.localName.trim()) {
    message.error(t('classEditor.errorPropertyName'))
    return
  }
  addingProperty.value = true
  try {
    if (editingPropertyId.value) {
      await ontologyApi.updateProperty(props.graphId, editingPropertyId.value, {
        localName: propertyForm.localName,
        propertyType: propertyForm.propertyType as any,
        rangeDataType: propertyForm.rangeDataType || undefined,
        domainClassId: props.classId,
        isRequired: propertyForm.isRequired,
        minCardinality: propertyForm.minCardinality,
        maxCardinality: propertyForm.maxCardinality
      })
      message.success(t('classEditor.propertyUpdated'))
    } else {
      await ontologyApi.createProperty(props.graphId, {
        localName: propertyForm.localName,
        propertyType: propertyForm.propertyType as any,
        rangeDataType: propertyForm.rangeDataType || undefined,
        domainClassId: props.classId,
        isRequired: propertyForm.isRequired,
        minCardinality: propertyForm.minCardinality,
        maxCardinality: propertyForm.maxCardinality
      })
      message.success(t('classEditor.propertyAdded'))
    }
    showAddProperty.value = false
    editingPropertyId.value = null
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || t('common.saveFailed'))
  } finally {
    addingProperty.value = false
  }
}

function openProperty(prop: OntPropertyVO) {
  editingPropertyId.value = prop.id
  propertyForm.localName = prop.localName
  propertyForm.propertyType = prop.propertyType || 'DATATYPE'
  propertyForm.rangeDataType = prop.rangeDataType || 'string'
  propertyForm.isRequired = prop.isRequired || false
  propertyForm.minCardinality = prop.minCardinality || 0
  propertyForm.maxCardinality = prop.maxCardinality
  showAddProperty.value = true
}

async function deleteProperty(prop: OntPropertyVO) {
  try {
    await ontologyApi.deleteProperty(props.graphId, prop.id)
    message.success(t('classEditor.propertyDeleted'))
    const idx = store.properties.findIndex(x => x.id === prop.id)
    if (idx !== -1) {
      store.properties.splice(idx, 1)
    }
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || t('common.deleteFailed'))
  }
}

function openConstraint(c: OntConstraintVO) {
  editingConstraintId.value = c.id
  constraintForm.constraintType = c.constraintType || 'REQUIRED'
  constraintForm.value = c.value || ''
  constraintForm.severity = c.severity || 'ERROR'
  constraintForm.errorMessage = c.errorMessage || ''
  constraintForm.description = c.description || ''
  showAddConstraint.value = true
}

async function handleAddConstraint() {
  addingConstraint.value = true
  try {
    if (editingConstraintId.value) {
      await ontologyApi.updateConstraint(props.graphId, editingConstraintId.value, {
        classId: props.classId,
        constraintType: constraintForm.constraintType,
        value: constraintForm.value,
        severity: constraintForm.severity,
        errorMessage: constraintForm.errorMessage,
        description: constraintForm.description
      })
      message.success(t('classEditor.constraintUpdated'))
    } else {
      await ontologyApi.createConstraint(props.graphId, {
        classId: props.classId,
        constraintType: constraintForm.constraintType,
        value: constraintForm.value,
        severity: constraintForm.severity,
        errorMessage: constraintForm.errorMessage,
        description: constraintForm.description
      })
      message.success(t('classEditor.constraintAdded'))
    }
    showAddConstraint.value = false
    editingConstraintId.value = null
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || t('common.saveFailed'))
  } finally {
    addingConstraint.value = false
  }
}

async function deleteConstraint(c: OntConstraintVO) {
  try {
    await ontologyApi.deleteConstraint(props.graphId, c.id)
    message.success(t('classEditor.constraintDeleted'))
    const idx = store.constraints.findIndex(x => x.id === c.id)
    if (idx !== -1) {
      store.constraints.splice(idx, 1)
    }
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || t('common.deleteFailed'))
  }
}

function openSubclass(cls: OntClassVO) {
  store.openTab({ id: `class-editor-${cls.id}`, type: 'class-editor', title: `${t('classEditor.class')}: ${cls.localName}`, classId: cls.id })
}

function openParentClass(cls: OntClassVO) {
  store.openTab({ id: `class-editor-${cls.id}`, type: 'class-editor', title: `${t('classEditor.class')}: ${cls.localName}`, classId: cls.id })
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
    padding: 5px;
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
}
</style>
