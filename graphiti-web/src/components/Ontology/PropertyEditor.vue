/**
 * 属性编辑器 — 本体工作台内嵌形式
 * [基本信息] [定义域/值域] [约束条件] [使用统计]
 */
<template>
  <div class="property-editor">
    <div class="editor-toolbar">
      <a-space>
        <a-button type="primary" :loading="saving" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          {{ t('common.save') }}
        </a-button>
        <a-divider type="vertical" />
        <a-button danger :disabled="!propertyId" :loading="deleting" @click="handleDelete">
          <template #icon><DeleteOutlined /></template>
          {{ t('common.delete') }}
        </a-button>
      </a-space>
      <div class="toolbar-right">
        <a-tag v-if="propertyId" color="purple">ID: {{ propertyId }}</a-tag>
        <a-tag v-if="form.localName" color="purple">{{ form.localName }}</a-tag>
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab" class="property-editor-tabs">
      <a-tab-pane key="basic" :tab="t('propertyEditor.tabBasic')">
        <div class="tab-content">
          <a-form :model="form" layout="vertical" class="basic-form">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item :label="t('propertyEditor.propertyName')" required>
                  <a-input v-model:value="form.localName" :placeholder="t('propertyEditor.propertyNamePlaceholder')" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('propertyEditor.propertyUri')">
                  <a-input v-model:value="form.propertyUri" placeholder="http://ontograph.io/ontology/name" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item :label="t('propertyEditor.propertyType')" required>
                  <a-select v-model:value="form.propertyType">
                    <a-select-option value="DATATYPE">{{ t('propertyEditor.typeDatatype') }}</a-select-option>
                    <a-select-option value="OBJECT">{{ t('propertyEditor.typeObject') }}</a-select-option>
                    <a-select-option value="ANNOTATION">{{ t('propertyEditor.typeAnnotation') }}</a-select-option>
                    <a-select-option value="TRANSITIVE">{{ t('propertyEditor.typeTransitive') }}</a-select-option>
                    <a-select-option value="SYMMETRIC">{{ t('propertyEditor.typeSymmetric') }}</a-select-option>
                    <a-select-option value="FUNCTIONAL">{{ t('propertyEditor.typeFunctional') }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('propertyEditor.parentProperty')">
                  <a-select v-model:value="form.parentPropertyId" :placeholder="t('propertyEditor.selectParentProperty')" allow-clear :options="propertyOptions" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item :label="t('propertyEditor.inverseOf')">
              <a-select v-model:value="form.inverseOfId" :placeholder="t('propertyEditor.selectInverse')" allow-clear :options="propertyOptions" />
            </a-form-item>

            <a-form-item :label="t('propertyEditor.equivalentTo')">
              <a-select v-model:value="form.equivalentTo" mode="multiple" :placeholder="t('propertyEditor.selectEquivalent')" allow-clear :options="propertyOptions" />
            </a-form-item>

            <a-form-item :label="t('propertyEditor.description')">
              <a-textarea v-model:value="form.description" :rows="3" />
            </a-form-item>

            <a-form-item :label="t('propertyEditor.example')">
              <a-textarea v-model:value="form.example" :rows="2" />
            </a-form-item>
          </a-form>
        </div>
      </a-tab-pane>

      <a-tab-pane key="domain-range" :tab="t('propertyEditor.tabDomainRange')">
        <div class="tab-content">
          <a-form :model="form" layout="vertical" class="basic-form">
            <a-form-item :label="t('propertyEditor.domain')">
              <a-select v-model:value="form.domainClassId" :placeholder="t('propertyEditor.selectClass')" allow-clear :options="classOptions">
                <template #suffixIcon><ApiOutlined /></template>
              </a-select>
            </a-form-item>

            <a-divider />

            <a-form-item v-if="form.propertyType === 'DATATYPE'" :label="t('propertyEditor.rangeDataType')">
              <a-select v-model:value="form.rangeDataType" :placeholder="t('propertyEditor.selectDataType')">
                <a-select-option value="string">{{ t('propertyEditor.dataTypeString') }}</a-select-option>
                <a-select-option value="integer">{{ t('propertyEditor.dataTypeInteger') }}</a-select-option>
                <a-select-option value="float">{{ t('propertyEditor.dataTypeFloat') }}</a-select-option>
                <a-select-option value="boolean">{{ t('propertyEditor.dataTypeBoolean') }}</a-select-option>
                <a-select-option value="date">{{ t('propertyEditor.dataTypeDate') }}</a-select-option>
                <a-select-option value="datetime">{{ t('propertyEditor.dataTypeDatetime') }}</a-select-option>
                <a-select-option value="json">JSON</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item v-else-if="form.propertyType === 'OBJECT'" :label="t('propertyEditor.rangeClass')">
              <a-select v-model:value="form.rangeClassId" :placeholder="t('propertyEditor.selectTargetClass')" allow-clear :options="classOptions">
                <template #suffixIcon><ApiOutlined /></template>
              </a-select>
            </a-form-item>

            <a-form-item :label="t('propertyEditor.defaultValue')">
              <a-input v-model:value="form.defaultValue" :placeholder="t('propertyEditor.defaultValuePlaceholder')" />
            </a-form-item>
          </a-form>
        </div>
      </a-tab-pane>

      <a-tab-pane key="constraints" :tab="t('propertyEditor.tabConstraints')">
        <div class="tab-content">
          <a-form :model="form" layout="vertical" class="basic-form">
            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item :label="t('propertyEditor.minCardinality')">
                  <a-input-number v-model:value="form.minCardinality" :min="0" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item :label="t('propertyEditor.maxCardinality')">
                  <a-input-number v-model:value="form.maxCardinality" :min="0" style="width: 100%" :placeholder="t('propertyEditor.maxCardinalityPlaceholder')" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item :label="t('propertyEditor.required')">
                  <a-switch v-model:checked="form.isRequired" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item :label="t('propertyEditor.isMultiple')">
                  <a-switch v-model:checked="form.isMultiple" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item :label="t('propertyEditor.pattern')">
                  <a-input v-model:value="form.pattern" placeholder="^[A-Za-z]+$" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item :label="t('propertyEditor.minValue')">
                  <a-input-number v-model:value="form.minValue" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item :label="t('propertyEditor.maxValue')">
                  <a-input-number v-model:value="form.maxValue" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item :label="t('propertyEditor.allowedValues')">
              <a-select
                v-model:value="form.allowedValues"
                mode="tags"
                :placeholder="t('propertyEditor.allowedValuesPlaceholder')"
                style="width: 100%"
              />
            </a-form-item>
          </a-form>
        </div>
      </a-tab-pane>

      <a-tab-pane key="inheritance" :tab="t('propertyEditor.tabInheritance')">
        <div class="tab-content">
          <div class="inheritance-section">
            <div class="section-title">{{ t('propertyEditor.parentPropertyChain') }}</div>
            <div v-if="propertyAncestors.length === 0" class="empty-tip">{{ t('propertyEditor.noParentProperty') }}</div>
            <div v-else class="inheritance-path">
              <span v-for="(p, idx) in propertyAncestors" :key="p.id" class="path-item">
                <span class="path-node" :class="{ current: p.id === propertyId }" @click="openProperty(p.id, p.localName)">
                  {{ p.localName }}
                </span>
                <span v-if="idx < propertyAncestors.length - 1" class="path-arrow">→</span>
              </span>
            </div>
          </div>

          <div class="inheritance-section" style="margin-top: 24px;">
            <div class="section-title">{{ t('propertyEditor.childProperties') }}</div>
            <div v-if="childProperties.length === 0" class="empty-tip">{{ t('propertyEditor.noChildProperties') }}</div>
            <div v-else class="property-tags">
              <span v-for="p in childProperties" :key="p.id" class="property-tag" @click="openProperty(p.id, p.localName)">
                {{ p.localName }}
              </span>
            </div>
          </div>

          <div class="inheritance-section" style="margin-top: 24px;">
            <div class="section-title">{{ t('propertyEditor.equivalentProperties') }}</div>
            <div v-if="equivalentProperties.length === 0" class="empty-tip">{{ t('propertyEditor.noEquivalentProperties') }}</div>
            <div v-else class="property-tags">
              <span v-for="p in equivalentProperties" :key="p.id" class="property-tag equivalent" @click="openProperty(p.id, p.localName)">
                {{ p.localName }}
              </span>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="stats" :tab="t('propertyEditor.tabStats')">
        <div class="tab-content">
          <a-row :gutter="16" class="stats-row">
            <a-col :span="8">
              <a-statistic :title="t('propertyEditor.usingClassCount')" :value="usingClassCount" />
            </a-col>
            <a-col :span="8">
              <a-statistic :title="t('propertyEditor.constraintCount')" :value="constraintCount" />
            </a-col>
          </a-row>

          <div class="using-classes">
            <div class="section-title">{{ t('propertyEditor.usingClasses') }}</div>
            <div v-if="usingClasses.length === 0" class="empty-tip">{{ t('propertyEditor.noUsageRecord') }}</div>
            <div v-else class="class-tags">
              <span v-for="cls in usingClasses" :key="cls.id" class="class-tag" @click="openClass(cls)">
                {{ cls.localName }}
              </span>
            </div>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import { SaveOutlined, DeleteOutlined, ApiOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { ontologyApi } from '@/api/ontology'

const { t } = useI18n()

const props = defineProps<{
  graphId: string
  propertyId?: number
}>()

const emit = defineEmits<{ (e: 'saved'): void }>()

const store = useOntologyStore()
const saving = ref(false)
const deleting = ref(false)
const activeTab = ref('basic')

const form = reactive({
  id: undefined as number | undefined,
  localName: '',
  propertyUri: '',
  propertyType: 'DATATYPE' as 'DATATYPE' | 'OBJECT' | 'ANNOTATION' | 'TRANSITIVE' | 'SYMMETRIC' | 'FUNCTIONAL',
  parentPropertyId: undefined as number | undefined,
  inverseOfId: undefined as number | undefined,
  equivalentTo: [] as number[],
  domainClassId: undefined as number | undefined,
  rangeClassId: undefined as number | undefined,
  rangeDataType: 'string',
  defaultValue: '',
  minCardinality: 0,
  maxCardinality: undefined as number | undefined,
  isRequired: false,
  isMultiple: false,
  pattern: '',
  minValue: undefined as number | undefined,
  maxValue: undefined as number | undefined,
  allowedValues: [] as string[],
  description: '',
  example: ''
})

const classOptions = computed(() => store.classes.map(c => ({ label: c.localName, value: c.id })))
const propertyOptions = computed(() => store.properties.map(p => ({ label: p.localName, value: p.id })))

const usingClasses = computed(() => {
  if (!props.propertyId) return []
  return store.classes.filter(c =>
    store.properties.some(p => p.id === props.propertyId && p.domainClassId === c.id)
  )
})

const usingClassCount = computed(() => usingClasses.value.length)

const constraintCount = computed(() =>
  store.constraints.filter(c => c.propertyId === props.propertyId).length
)

function parseEquivalentTo(equivalentTo: any): number[] {
  if (!Array.isArray(equivalentTo)) return []
  const ids: number[] = []
  for (const uri of equivalentTo) {
    const found = store.properties.find(p => p.propertyUri === uri || p.localName === uri)
    if (found) ids.push(found.id)
  }
  return ids
}

async function loadData() {
  if (!props.propertyId) return
  const prop = store.properties.find(p => p.id === props.propertyId)
  if (!prop) return
  Object.assign(form, {
    id: prop.id,
    localName: prop.localName,
    propertyUri: prop.propertyUri || '',
    propertyType: prop.propertyType,
    parentPropertyId: prop.parentPropertyId,
    inverseOfId: prop.inverseOfId,
    equivalentTo: parseEquivalentTo(prop.equivalentTo),
    domainClassId: prop.domainClassId,
    rangeClassId: prop.rangeClassId,
    rangeDataType: prop.rangeDataType || 'string',
    defaultValue: prop.defaultValue || '',
    minCardinality: prop.minCardinality || 0,
    maxCardinality: prop.maxCardinality,
    isRequired: prop.isRequired || false,
    isMultiple: prop.isMultiple || false,
    pattern: prop.pattern || '',
    minValue: prop.minValue,
    maxValue: prop.maxValue,
    allowedValues: prop.allowedValues ? (Array.isArray(prop.allowedValues) ? prop.allowedValues : []) : [],
    description: prop.description || '',
    example: prop.example || ''
  })
}

async function handleSave() {
  if (!form.localName.trim()) { message.error(t('propertyEditor.errorPropertyName')); return }
  saving.value = true
  try {
    const data = {
      localName: form.localName,
      propertyUri: form.propertyUri || undefined,
      propertyType: form.propertyType,
      parentPropertyId: form.parentPropertyId,
      inverseOfId: form.inverseOfId,
      equivalentTo: form.equivalentTo?.length
        ? form.equivalentTo.map((id: number) => {
            const p = store.properties.find(x => x.id === id)
            return p?.propertyUri || `http://ontograph.io/${p?.localName || id}`
          }).filter(Boolean)
        : undefined,
      domainClassId: form.domainClassId,
      rangeClassId: form.rangeClassId,
      rangeDataType: form.rangeDataType || undefined,
      defaultValue: form.defaultValue || undefined,
      minCardinality: form.minCardinality,
      maxCardinality: form.maxCardinality,
      isRequired: form.isRequired,
      isMultiple: form.isMultiple,
      pattern: form.pattern || undefined,
      minValue: form.minValue,
      maxValue: form.maxValue,
      allowedValues: form.allowedValues.length > 0 ? form.allowedValues : undefined,
      description: form.description || undefined,
      example: form.example || undefined
    }
    if (props.propertyId) {
      await ontologyApi.updateProperty(props.graphId, props.propertyId, data)
    } else {
      await ontologyApi.createProperty(props.graphId, data)
    }
    message.success(t('propertyEditor.propertySaved'))
    await store.loadFullOntology(props.graphId)
    emit('saved')
  } catch (e: any) {
    message.error(e.message || t('common.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function handleDelete() {
  if (!props.propertyId) return
  if (!confirm(t('propertyEditor.confirmDelete'))) return
  deleting.value = true
  try {
    await ontologyApi.deleteProperty(props.graphId, props.propertyId)
    message.success(t('propertyEditor.propertyDeleted'))
    await store.loadFullOntology(props.graphId)
    emit('saved')
  } catch (e: any) {
    message.error(e.message || t('common.deleteFailed'))
  } finally {
    deleting.value = false
  }
}

const propertyAncestors = computed(() => {
  if (!props.propertyId) return []
  const path: any[] = []
  let current = store.properties.find(p => p.id === props.propertyId)
  while (current) {
    path.unshift(current)
    if (current.parentPropertyId) {
      current = store.properties.find(p => p.id === current!.parentPropertyId)
    } else {
      break
    }
  }
  return path
})

const childProperties = computed(() =>
  store.properties.filter(p => p.parentPropertyId === props.propertyId)
)

const equivalentProperties = computed(() => {
  if (!props.propertyId) return []
  const prop = store.properties.find(p => p.id === props.propertyId)
  if (!prop?.equivalentTo?.length) return []
  return store.properties.filter(p =>
    prop.equivalentTo!.some((ref: string) =>
      p.propertyUri === ref || p.localName === ref
    )
  )
})

function openClass(cls: any) {
  store.openTab({ id: `class-editor-${cls.id}`, type: 'class-editor', title: `${t('propertyEditor.class')}: ${cls.localName}`, classId: cls.id })
}

function openProperty(propertyId: number, propertyName: string) {
  store.openTab({ id: `property-editor-${propertyId}`, type: 'property-editor', title: `${t('propertyEditor.property')}: ${propertyName}`, propertyId })
}

onMounted(() => loadData())
watch(() => props.propertyId, () => loadData())
</script>

<style scoped lang="less">
.property-editor {
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

    .toolbar-right { display: flex; gap: 8px; align-items: center; }
  }

  .property-editor-tabs {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;

    :deep(.ant-tabs-content) { height: 100%; flex: 1; }
    :deep(.ant-tabs-tabpane) { height: 100%; overflow-y: auto; }
    :deep(.ant-tabs-nav) {
      margin: 0;
      padding: 0 16px;
      background: #161b22;
      .ant-tabs-tab { color: #8b949e; font-size: 13px; &.ant-tabs-tab-active { color: #e6edf3; } &:hover { color: #e6edf3; } }
    }
  }

  .tab-content { padding: 20px; }
  .basic-form { max-width: 800px; }

  .stats-row { margin-bottom: 32px; }

  // 使用统计区域字体颜色覆盖
  :deep(.ant-statistic) {
    .ant-statistic-title {
      color: #8b949e !important;
    }
    .ant-statistic-content {
      color: #e6edf3 !important;
    }
  }

  .using-classes {
    .section-title { font-size: 14px; font-weight: 600; color: #e6edf3; margin-bottom: 12px; }
    .class-tags { display: flex; flex-wrap: wrap; gap: 8px; }
    .class-tag {
      padding: 4px 12px;
      background: rgba(88, 166, 255, 0.15);
      border: 1px solid rgba(88, 166, 255, 0.3);
      border-radius: 4px;
      font-size: 13px;
      color: #58a6ff;
      cursor: pointer;
      &:hover { background: rgba(88, 166, 255, 0.25); }
    }
    .empty-tip { color: #6e7681; font-size: 13px; }
  }

  .inheritance-section {
    .section-title { font-size: 14px; font-weight: 600; color: #e6edf3; margin-bottom: 12px; }
    .empty-tip { color: #6e7681; font-size: 13px; }

    .inheritance-path {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
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
        cursor: pointer;

        &.current { background: rgba(88, 166, 255, 0.2); color: #58a6ff; border: 1px solid #58a6ff; }
        &:hover { background: #30363d; }
      }
      .path-arrow { color: #6e7681; }
    }

    .property-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .property-tag {
        padding: 4px 12px;
        background: rgba(163, 113, 247, 0.15);
        border: 1px solid rgba(163, 113, 247, 0.3);
        border-radius: 4px;
        font-size: 13px;
        color: #a371f7;
        cursor: pointer;
        transition: all 0.15s;

        &:hover { background: rgba(163, 113, 247, 0.25); }
        &.equivalent {
          background: rgba(88, 166, 255, 0.15);
          border-color: rgba(88, 166, 255, 0.3);
          color: #58a6ff;
          &:hover { background: rgba(88, 166, 255, 0.25); }
        }
      }
    }
  }
}
</style>
