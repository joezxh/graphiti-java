/**
 * 实例表单编辑器 — 动态表单生成
 * 根据类的属性定义自动生成表单字段，支持所有数据类型
 */
<template>
  <div class="instance-form">
    <div class="form-toolbar">
      <a-space>
        <a-button type="primary" :loading="saving" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
        <a-button :disabled="isNew" @click="handleSaveAs">另存为</a-button>
      </a-space>
      <div class="toolbar-right">
        <a-tag v-if="!isNew" color="blue">{{ formData.uuid || recordUuid }}</a-tag>
        <a-tag v-if="recordType" color="green">{{ recordType }}</a-tag>
      </div>
    </div>

    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-col="{ span: 4 }"
      :wrapper-col="{ span: 18 }"
      layout="horizontal"
      class="instance-form-content"
    >
      <a-divider orientation="left">基本信息</a-divider>

      <a-form-item label="UUID" name="uuid" v-if="!isNew">
        <a-input v-model:value="formData.uuid" disabled placeholder="系统自动生成" />
      </a-form-item>

      <a-form-item label="名称" name="name" :rules="[{ required: true, message: '请输入实例名称' }]">
        <a-input v-model:value="formData.name" placeholder="请输入实例名称" />
      </a-form-item>

      <a-form-item label="类型" name="type" :rules="[{ required: true, message: '请选择实例类型' }]">
        <a-select
          v-model:value="formData.type"
          placeholder="请选择类型"
          :disabled="!isNew"
          show-search
          :filter-option="filterClassOption"
        >
          <a-select-option v-for="cls in store.classes" :key="cls.id" :value="cls.localName">
            <span>
              <span style="color: #58a6ff">◉</span> {{ cls.localName }}
            </span>
          </a-select-option>
        </a-select>
      </a-form-item>

      <template v-if="!isNew && formData.createdAt">
        <a-form-item label="创建时间">
          <a-input :value="formData.createdAt" disabled />
        </a-form-item>
        <a-form-item label="更新时间">
          <a-input :value="formData.updatedAt" disabled />
        </a-form-item>
      </template>

      <template v-if="selectedClass && selectedClassProperties.length > 0">
        <a-divider orientation="left">属性信息</a-divider>

        <template v-for="prop in selectedClassProperties" :key="prop.id">
          <a-form-item
            :label="prop.localName"
            :name="['properties', prop.localName]"
            :rules="getPropertyRules(prop)"
          >
            <!-- DATATYPE 属性 -->
            <template v-if="prop.propertyType === 'DATATYPE'">
              <!-- 布尔类型 -->
              <a-switch
                v-if="isBoolType(prop.rangeDataType)"
                v-model:checked="formData.properties[prop.localName]"
                checked-children="是"
                un-checked-children="否"
              />
              <!-- 数字类型 -->
              <a-input-number
                v-else-if="isNumericType(prop.rangeDataType)"
                v-model:value="formData.properties[prop.localName]"
                style="width: 100%"
                :placeholder="`请输入 ${prop.localName}`"
                :min="prop.minValue"
                :max="prop.maxValue"
              />
              <!-- 日期类型 -->
              <a-date-picker
                v-else-if="isDateType(prop.rangeDataType)"
                v-model:value="formData.properties[prop.localName]"
                style="width: 100%"
                :placeholder="`请选择 ${prop.localName}`"
              />
              <!-- 枚举值 -->
              <a-select
                v-else-if="prop.allowedValues && prop.allowedValues.length > 0"
                v-model:value="formData.properties[prop.localName]"
                :placeholder="`请选择 ${prop.localName}`"
              >
                <a-select-option v-for="val in prop.allowedValues" :key="val" :value="val">{{ val }}</a-select-option>
              </a-select>
              <!-- 文本输入 -->
              <a-textarea
                v-else-if="isLongTextType(prop.rangeDataType)"
                v-model:value="formData.properties[prop.localName]"
                :rows="4"
                :placeholder="`请输入 ${prop.localName}`"
              />
              <a-input
                v-else
                v-model:value="formData.properties[prop.localName]"
                :placeholder="`请输入 ${prop.localName}`"
              />
            </template>

            <!-- OBJECT 属性 — 关联节点选择器 -->
            <template v-else-if="prop.propertyType === 'OBJECT'">
              <div class="object-ref-field">
                <a-input
                  v-model:value="formData.properties[prop.localName]"
                  :placeholder="`输入关联节点的 UUID`"
                  style="flex: 1"
                />
                <a-button @click="showNodeSelector(prop)">选择节点</a-button>
              </div>
            </template>

            <!-- 其他属性类型 -->
            <a-input
              v-else
              v-model:value="formData.properties[prop.localName]"
              :placeholder="`请输入 ${prop.localName}`"
            />

            <template #extra>
              <span class="prop-hint">
                <span v-if="prop.isRequired" class="required-hint">必填</span>
                <span v-if="prop.rangeDataType" class="type-hint">{{ prop.rangeDataType }}</span>
                <span v-if="prop.description" class="desc-hint">{{ prop.description }}</span>
              </span>
            </template>
          </a-form-item>
        </template>
      </template>

      <!-- 动态属性（未映射到类属性的额外字段） -->
      <template v-if="extraProps.length > 0">
        <a-divider orientation="left">扩展属性</a-divider>
        <template v-for="prop in extraProps" :key="prop.key">
          <a-form-item :label="prop.label" :name="['properties', prop.key]">
            <a-input v-model:value="formData.properties[prop.key]" :placeholder="`请输入 ${prop.label}`" />
          </a-form-item>
        </template>
      </template>

      <div class="add-extra-prop">
        <a-button type="dashed" block @click="showAddExtraProp = !showAddExtraProp">
          <template #icon><PlusOutlined /></template>
          添加扩展属性
        </a-button>
        <div v-if="showAddExtraProp" class="extra-prop-form">
          <a-input v-model:value="newPropKey" placeholder="属性名" style="width: 120px; margin-right: 8px" />
          <a-input v-model:value="newPropValue" placeholder="属性值" style="width: 200px; margin-right: 8px" />
          <a-button type="primary" size="small" @click="addExtraProp">添加</a-button>
        </div>
      </div>
    </a-form>

    <!-- 节点选择器弹窗 -->
    <a-modal
      v-model:open="nodeSelectorVisible"
      title="选择关联节点"
      width="700px"
      @ok="confirmNodeSelection"
      @cancel="nodeSelectorVisible = false"
    >
      <div class="node-selector">
        <a-input-search
          v-model:value="nodeSearchKeyword"
          placeholder="搜索节点..."
          style="margin-bottom: 12px"
          @search="searchNodes"
        />
        <a-table
          :columns="nodeSelectorColumns"
          :data-source="searchableNodes"
          :pagination="{ pageSize: 10 }"
          row-key="uuid"
          size="small"
          :loading="searchingNodes"
          @row-click="handleNodeRowClick"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="node-selector-name" :class="{ selected: selectedNode?.uuid === record.uuid }">
                <span style="color: #58a6ff; margin-right: 6px">◉</span>
                {{ record.name }}
              </div>
            </template>
          </template>
        </a-table>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { SaveOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { graphApi } from '@/api/graph'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

const props = defineProps<{
  graphId: string
  instanceData?: {
    uuid?: string
    name?: string
    type?: string
    properties?: Record<string, any>
    createdAt?: string
    updatedAt?: string
  }
}>()

const emit = defineEmits<{
  (e: 'saved', data: any): void
}>()

const store = useOntologyStore()
const formRef = ref<FormInstance>()
const saving = ref(false)

const isNew = computed(() => !props.instanceData?.uuid)

const recordUuid = computed(() => props.instanceData?.uuid ?? '')
const recordType = computed(() => props.instanceData?.type ?? '')

const formData = reactive<Record<string, any>>({
  uuid: '',
  name: '',
  type: '',
  properties: {},
  createdAt: '',
  updatedAt: ''
})

// 初始化表单数据
watch(() => props.instanceData, (data) => {
  if (data) {
    formData.uuid = data.uuid ?? ''
    formData.name = data.name ?? ''
    formData.type = data.type ?? ''
    
    // 处理 properties：将日期字符串转换为 dayjs 对象
    const rawProps = data.properties ? { ...data.properties } : {}
    formData.properties = {}
    
    // 根据 type 查找类定义
    const targetClass = store.classes.find(c => c.localName === formData.type)
    const propsDef = targetClass ? store.properties.filter(p => p.domainClassId === targetClass.id) : []
    
    for (const [key, value] of Object.entries(rawProps)) {
      const propDef = propsDef.find(p => p.localName === key)
      // 如果是日期类型，转换为 dayjs 对象
      if (propDef && isDateType(propDef.rangeDataType) && typeof value === 'string') {
        formData.properties[key] = dayjs(value)
      } else {
        formData.properties[key] = value
      }
    }
    
    formData.createdAt = data.createdAt ?? ''
    formData.updatedAt = data.updatedAt ?? ''
  } else {
    formData.uuid = ''
    formData.name = ''
    formData.type = ''
    formData.properties = {}
    formData.createdAt = ''
    formData.updatedAt = ''
  }
}, { immediate: true })

const formRules = {
  name: [{ required: true, message: '请输入实例名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择实例类型', trigger: 'change' }]
}

const selectedClass = computed(() =>
  store.classes.find(c => c.localName === formData.type)
)

const selectedClassProperties = computed(() =>
  store.properties.filter(p => p.domainClassId === selectedClass.value?.id)
)

function isBoolType(dt?: string) {
  return dt === 'boolean' || dt === 'Boolean'
}
function isNumericType(dt?: string) {
  return ['integer', 'long', 'float', 'double', 'decimal', 'Int', 'Long', 'Float', 'Double'].includes(dt ?? '')
}
function isDateType(dt?: string) {
  return ['date', 'datetime', 'dateTime', 'Date', 'DateTime'].includes(dt ?? '')
}
function isLongTextType(dt?: string) {
  return ['text', 'string', 'Text', 'String'].includes(dt ?? '') && false // 默认不用textarea
}

function getPropertyRules(prop: any) {
  const rules: any[] = []
  if (prop.isRequired) {
    rules.push({ required: true, message: `请填写属性 ${prop.localName}`, trigger: 'change' })
  }
  if (prop.pattern) {
    rules.push({ pattern: new RegExp(prop.pattern), message: `格式不符合要求: ${prop.pattern}` })
  }
  // 只有当 minValue 和 maxValue 都有实际值时才添加范围校验
  if (prop.minValue != null && prop.maxValue != null) {
    rules.push({ type: 'number', min: prop.minValue, max: prop.maxValue, message: `值应在 ${prop.minValue} - ${prop.maxValue} 之间` })
  }
  return rules
}

function filterClassOption(input: string, option: any) {
  return option.children()[0].children.toLowerCase().includes(input.toLowerCase())
}

// 扩展属性
const extraProps = ref<Array<{ key: string; label: string }>>([])
const showAddExtraProp = ref(false)
const newPropKey = ref('')
const newPropValue = ref('')

function addExtraProp() {
  if (!newPropKey.value.trim()) {
    message.warning('请输入属性名')
    return
  }
  if (formData.properties[newPropKey.value] !== undefined) {
    message.warning('属性已存在')
    return
  }
  extraProps.value.push({ key: newPropKey.value.trim(), label: newPropKey.value.trim() })
  formData.properties[newPropKey.value] = newPropValue.value
  newPropKey.value = ''
  newPropValue.value = ''
  showAddExtraProp.value = false
}

// 节点选择器
const nodeSelectorVisible = ref(false)
const nodeSearchKeyword = ref('')
const searchingNodes = ref(false)
const searchableNodes = ref<any[]>([])
const selectedNode = ref<any | null>(null)
const currentPropKey = ref('')

const nodeSelectorColumns = [
  { title: '名称', key: 'name' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 120 },
  { title: 'UUID', dataIndex: 'uuid', key: 'uuid', ellipsis: true, width: 200 }
]

async function searchNodes() {
  if (!props.graphId) return
  searchingNodes.value = true
  try {
    const data = await graphApi.getClassInstances(props.graphId, '', {
      page: 1,
      pageSize: 50
    })
    searchableNodes.value = data.data ?? []
  } catch (e) {
    searchableNodes.value = []
  } finally {
    searchingNodes.value = false
  }
}

function showNodeSelector(prop: any) {
  currentPropKey.value = prop.localName
  nodeSearchKeyword.value = ''
  searchableNodes.value = []
  selectedNode.value = null
  nodeSelectorVisible.value = true
  searchNodes()
}

function handleNodeRowClick(record: any) {
  selectedNode.value = record
}

function confirmNodeSelection() {
  if (selectedNode.value) {
    formData.properties[currentPropKey.value] = selectedNode.value.uuid
    message.success(`已选择节点: ${selectedNode.value.name}`)
  }
  nodeSelectorVisible.value = false
}

async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    // 处理 properties：将 dayjs 对象转回字符串
    const cleanProperties: Record<string, any> = {}
    for (const [key, value] of Object.entries(formData.properties)) {
      if (value && typeof value === 'object' && 'format' in value && typeof value.format === 'function') {
        // dayjs 对象，转为 ISO 字符串
        cleanProperties[key] = (value as any).format('YYYY-MM-DD')
      } else {
        cleanProperties[key] = value
      }
    }

    if (isNew.value) {
      const result = await graphApi.createNode(props.graphId, {
        name: formData.name,
        type: formData.type,
        properties: cleanProperties
      })
      message.success('实例创建成功')
      emit('saved', result)
    } else {
      const result = await graphApi.updateNode(props.graphId, recordUuid.value, {
        name: formData.name,
        properties: cleanProperties
      })
      message.success('实例更新成功')
      emit('saved', result)
    }
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleSaveAs() {
  formData.uuid = ''
  formData.createdAt = ''
  formData.updatedAt = ''
  await handleSave()
}
</script>

<style scoped lang="less">
.instance-form {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow-y: auto;
  padding: 16px;

  .form-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    flex-shrink: 0;
  }

  .instance-form-content {
    flex: 1;
  }

  // 分割线标题颜色
  :deep(.ant-divider-inner-text) {
    color: #e6edf3;
  }

  .prop-hint {
    display: flex;
    gap: 8px;
    font-size: 12px;
    color: #6e7681;

    .required-hint { color: #f85149; }
    .type-hint { color: #a371f7; }
    .desc-hint { color: #8b949e; }
  }

  .object-ref-field {
    display: flex;
    gap: 8px;
    align-items: center;
  }

  .add-extra-prop {
    margin-top: 16px;

    .extra-prop-form {
      display: flex;
      align-items: center;
      margin-top: 8px;
    }
  }
}

.node-selector {
  .node-selector-name {
    cursor: pointer;
    padding: 4px;
    border-radius: 4px;
    transition: background 0.15s;

    &:hover { background: #21262d; }
    &.selected { background: rgba(88, 166, 255, 0.15); color: #58a6ff; }
  }
}
</style>
