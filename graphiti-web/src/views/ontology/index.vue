<template>
  <div class="ontology-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">本体管理</h1>
        <p class="page-desc">管理图谱的类、属性、约束定义</p>
      </div>
      <div class="header-right">
        <a-select
          v-model:value="selectedGraphId"
          placeholder="请选择图谱"
          style="width: 220px"
          @change="onGraphChange"
        >
          <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
            {{ g.name }}
          </a-select-option>
        </a-select>
        <a-button @click="refreshOntology">
          <ReloadOutlined :spin="loading" />
          刷新
        </a-button>
      </div>
    </div>

    <!-- 本体概览卡片 -->
    <div v-if="ontologyData.definition" class="ontology-overview">
      <a-card class="overview-card">
        <div class="overview-header">
          <div class="overview-title">
            <h3>{{ ontologyData.definition.name || '未命名本体' }}</h3>
            <a-tag :color="statusColor">{{ ontologyData.definition.status }}</a-tag>
          </div>
          <div class="overview-meta">
            <span>版本: {{ ontologyData.definition.version }}</span>
            <span>命名空间: {{ ontologyData.definition.namespace }}</span>
          </div>
        </div>
        <div class="overview-stats">
          <div class="stat-item">
            <a-statistic title="类数量" :value="ontologyData.definition.classCount" />
          </div>
          <div class="stat-item">
            <a-statistic title="属性数量" :value="ontologyData.definition.propertyCount" />
          </div>
          <div class="stat-item">
            <a-statistic title="约束数量" :value="ontologyData.definition.constraintCount" />
          </div>
        </div>
      </a-card>
    </div>

    <!-- 无本体时显示引导 -->
    <div v-else-if="selectedGraphId && !loading" class="empty-state">
      <a-empty description="该图谱尚未创建本体定义">
        <a-button type="primary" @click="showCreateDefinitionModal = true">
          <PlusOutlined /> 创建本体定义
        </a-button>
      </a-empty>
    </div>

    <!-- 主内容区：树形结构 + 详情 -->
    <div v-if="ontologyData.definition" class="main-content">
      <!-- 左侧：类树形结构 -->
      <div class="left-panel">
        <a-card class="tree-card" title="类层次结构">
          <template #extra>
            <a-button type="link" size="small" @click="openClassModal()">
              <PlusOutlined /> 新增类
            </a-button>
          </template>
          <a-tree
            v-if="classTreeData.length > 0"
            :treeData="classTreeData"
            :show-icon="true"
            :selected-keys="selectedClassKeys"
            @select="onClassSelect"
          >
            <template #icon><ApiOutlined /></template>
            <template #title="node">
              <span class="tree-node">
                <span>{{ node.title }}</span>
                <a-space size="small" class="node-actions" @click.stop>
                  <a-button type="link" size="small" @click="openPropertyModal(undefined, node)">
                    <PlusOutlined />
                  </a-button>
                  <a-button type="link" size="small" danger @click="handleDeleteClass(node)">
                    <DeleteOutlined />
                  </a-button>
                </a-space>
              </span>
            </template>
          </a-tree>
          <a-empty v-else description="暂无类定义" />
        </a-card>
      </div>

      <!-- 右侧：详情面板 -->
      <div class="right-panel">
        <!-- 类详情 -->
        <a-card v-if="selectedClass" class="detail-card" title="类详情">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="本地名称">{{ selectedClass.localName }}</a-descriptions-item>
            <a-descriptions-item label="完整URI">
              <a-tag>{{ selectedClass.classUri }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="描述" :span="2">{{ selectedClass.description || '-' }}</a-descriptions-item>
            <a-descriptions-item label="示例" :span="2">{{ selectedClass.example || '-' }}</a-descriptions-item>
            <a-descriptions-item label="领域">{{ selectedClass.domainHint || '-' }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDate(selectedClass.createdAt) }}</a-descriptions-item>
          </a-descriptions>

          <div class="detail-section">
            <div class="section-header">
              <h4>属性列表</h4>
              <a-button type="link" size="small" @click="openPropertyModal(undefined, selectedClass)">
                <PlusOutlined /> 添加属性
              </a-button>
            </div>
            <a-table
              :columns="propertyColumns"
              :data-source="classProperties"
              :pagination="false"
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
                  <a-button type="link" size="small" danger @click="handleDeleteProperty(record)">
                    <DeleteOutlined />
                  </a-button>
                </template>
              </template>
            </a-table>
          </div>

          <div class="detail-actions">
            <a-space>
              <a-button type="primary" @click="openClassModal(selectedClass)">编辑类</a-button>
            </a-space>
          </div>
        </a-card>

        <!-- 属性详情 -->
        <a-card v-else-if="selectedProperty" class="detail-card" title="属性详情">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="本地名称">{{ selectedProperty.localName }}</a-descriptions-item>
            <a-descriptions-item label="完整URI">
              <a-tag>{{ selectedProperty.propertyUri }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="属性类型">
              <a-tag :color="getPropertyTypeColor(selectedProperty.propertyType)">
                {{ selectedProperty.propertyType }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="数据类型">{{ selectedProperty.rangeDataType || '-' }}</a-descriptions-item>
            <a-descriptions-item label="是否必填">
              <a-tag :color="selectedProperty.isRequired ? 'red' : 'default'">
                {{ selectedProperty.isRequired ? '是' : '否' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="是否多值">
              <a-tag :color="selectedProperty.isMultiple ? 'blue' : 'default'">
                {{ selectedProperty.isMultiple ? '是' : '否' }}
              </a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 空状态 -->
        <a-card v-else class="detail-card">
          <a-empty description="请从左侧选择一个类或属性查看详情" />
        </a-card>
      </div>
    </div>

    <!-- 标签页：表格视图 -->
    <a-tabs v-if="ontologyData.definition" v-model:activeKey="activeTab" class="data-tabs">
      <a-tab-pane key="classes" tab="类列表">
        <a-table
          :columns="classColumns"
          :data-source="ontologyData.classes"
          :pagination="{ pageSize: 10 }"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'domainHint'">
              <a-tag>{{ record.domainHint || '-' }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-space>
                <a-button type="link" size="small" @click="openClassModal(record)">编辑</a-button>
                <a-button type="link" size="small" danger @click="handleDeleteClass(record)">删除</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="properties" tab="属性列表">
        <a-table
          :columns="propertyColumns"
          :data-source="ontologyData.properties"
          :pagination="{ pageSize: 10 }"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'propertyType'">
              <a-tag :color="getPropertyTypeColor(record.propertyType)">{{ record.propertyType }}</a-tag>
            </template>
            <template v-if="column.key === 'isRequired'">
              <a-tag :color="record.isRequired ? 'red' : 'default'">{{ record.isRequired ? '必填' : '可选' }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" danger @click="handleDeleteProperty(record)">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="constraints" tab="约束列表">
        <a-table
          :columns="constraintColumns"
          :data-source="ontologyData.constraints"
          :pagination="{ pageSize: 10 }"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'severity'">
              <a-tag :color="getSeverityColor(record.severity)">{{ record.severity }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" danger @click="handleDeleteConstraint(record)">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="history" tab="版本历史">
        <VersionHistory :graph-id="selectedGraphId" />
      </a-tab-pane>
    </a-tabs>

    <!-- 创建本体定义模态框 -->
    <a-modal
      v-model:open="showCreateDefinitionModal"
      title="创建本体定义"
      @ok="handleCreateDefinition"
      :confirm-loading="definitionSaving"
    >
      <a-form :model="definitionForm" layout="vertical">
        <a-form-item label="本体名称" required>
          <a-input v-model:value="definitionForm.name" placeholder="如：法律知识图谱本体" />
        </a-form-item>
        <a-form-item label="命名空间">
          <a-input v-model:value="definitionForm.namespace" placeholder="http://graphiti.io/ontology" />
        </a-form-item>
        <a-form-item label="版本号">
          <a-input v-model:value="definitionForm.version" placeholder="1.0.0" />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="definitionForm.description" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 类编辑模态框 -->
    <a-modal
      v-model:open="showClassModal"
      :title="classEditing ? '编辑类' : '新增类'"
      @ok="handleSaveClass"
      :confirm-loading="classSaving"
      width="600px"
    >
      <a-form :model="classForm" layout="vertical">
        <a-form-item label="本地名称" required>
          <a-input v-model:value="classForm.localName" placeholder="如：Person、Company" />
        </a-form-item>
        <a-form-item label="完整URI">
          <a-input v-model:value="classForm.classUri" placeholder="http://graphiti.io/ontology/..." />
        </a-form-item>
        <a-form-item label="父类">
          <a-select v-model:value="classForm.parentClassId" placeholder="选择父类（可选）" allow-clear>
            <a-select-option v-for="c in ontologyData.classes" :key="c.id" :value="c.id">
              {{ c.localName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="领域分类">
          <a-select v-model:value="classForm.domainHint" placeholder="选择领域">
            <a-select-option value="FINANCIAL">金融</a-select-option>
            <a-select-option value="MEDICAL">医疗</a-select-option>
            <a-select-option value="ECOMMERCE">电商</a-select-option>
            <a-select-option value="LEGAL">法律</a-select-option>
            <a-select-option value="KNOWLEDGE">通用知识</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="classForm.description" :rows="3" />
        </a-form-item>
        <a-form-item label="示例">
          <a-textarea v-model:value="classForm.example" :rows="2" placeholder="使用示例" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 属性编辑模态框 -->
    <a-modal
      v-model:open="showPropertyModal"
      :title="'为 ' + (propertyForm.domainClassName || '类') + ' 添加属性'"
      @ok="handleSaveProperty"
      :confirm-loading="propertySaving"
      width="700px"
    >
      <a-form :model="propertyForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="属性名称" required>
              <a-input v-model:value="propertyForm.localName" placeholder="如：hasName、createdAt" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="属性类型" required>
              <a-select v-model:value="propertyForm.propertyType">
                <a-select-option value="DATATYPE">数据属性</a-select-option>
                <a-select-option value="OBJECT">对象属性</a-select-option>
                <a-select-option value="ANNOTATION">注释属性</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16" v-if="propertyForm.propertyType === 'DATATYPE'">
          <a-col :span="12">
            <a-form-item label="数据类型">
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
          </a-col>
          <a-col :span="12">
            <a-form-item label="默认值">
              <a-input v-model:value="propertyForm.defaultValue" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16" v-if="propertyForm.propertyType === 'OBJECT'">
          <a-col :span="24">
            <a-form-item label="目标类">
              <a-select v-model:value="propertyForm.rangeClassId" placeholder="选择目标类">
                <a-select-option v-for="c in ontologyData.classes" :key="c.id" :value="c.id">
                  {{ c.localName }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="最小基数">
              <a-input-number v-model:value="propertyForm.minCardinality" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="最大基数">
              <a-input-number v-model:value="propertyForm.maxCardinality" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="必填">
              <a-switch v-model:checked="propertyForm.isRequired" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, DeleteOutlined, ReloadOutlined, ApiOutlined } from '@ant-design/icons-vue'
import { ontologyApi, type OntologyFullVO, type OntClassVO, type OntPropertyVO, type OntConstraintVO } from '@/api/ontology'
import { graphApi, type Graph } from '@/api/graph'
import VersionHistory from './VersionHistory.vue'

// ==================== 状态 ====================
const selectedGraphId = ref<string | undefined>(undefined)
const graphOptions = ref<Graph[]>([])
const loading = ref(false)
const ontologyData = reactive<OntologyFullVO>({
  definition: null as any,
  classes: [],
  classHierarchy: [],
  properties: [],
  constraints: []
})

// 标签页
const activeTab = ref('classes')

// 选中状态
const selectedClassKeys = ref<string[]>([])
const selectedClass = ref<OntClassVO | null>(null)
const selectedProperty = ref<OntPropertyVO | null>(null)

// 模态框状态
const showCreateDefinitionModal = ref(false)
const showClassModal = ref(false)
const showPropertyModal = ref(false)
const definitionSaving = ref(false)
const classSaving = ref(false)
const propertySaving = ref(false)
const classEditing = ref(false)

// ==================== 表单数据 ====================
const definitionForm = reactive({
  name: '',
  namespace: 'http://graphiti.io/ontology',
  version: '1.0.0',
  description: ''
})

const classForm = reactive({
  id: undefined as number | undefined,
  localName: '',
  classUri: '',
  parentClassId: undefined as number | undefined,
  domainHint: '',
  description: '',
  example: ''
})

const propertyForm = reactive({
  id: undefined as number | undefined,
  localName: '',
  propertyType: 'DATATYPE' as 'DATATYPE' | 'OBJECT' | 'ANNOTATION',
  rangeDataType: 'string',
  rangeClassId: undefined as number | undefined,
  domainClassId: undefined as number | undefined,
  domainClassName: '',
  minCardinality: 0,
  maxCardinality: undefined as number | undefined,
  defaultValue: '',
  isRequired: false,
  isMultiple: false
})

// ==================== 表格列定义 ====================
const classColumns = [
  { title: '本地名称', dataIndex: 'localName', key: 'localName' },
  { title: 'URI', dataIndex: 'classUri', key: 'classUri', ellipsis: true },
  { title: '领域', key: 'domainHint' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 150 }
]

const propertyColumns = [
  { title: '名称', dataIndex: 'localName', key: 'localName' },
  { title: '类型', key: 'propertyType' },
  { title: '数据类型', dataIndex: 'rangeDataType', key: 'rangeDataType' },
  { title: '必填', key: 'isRequired' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 80 }
]

const constraintColumns = [
  { title: '类型', dataIndex: 'constraintType', key: 'constraintType' },
  { title: '严重级别', key: 'severity' },
  { title: '错误信息', dataIndex: 'errorMessage', key: 'errorMessage', ellipsis: true },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 80 }
]

// ==================== 计算属性 ====================
const statusColor = computed(() => {
  switch (ontologyData.definition?.status) {
    case 'ACTIVE': return 'green'
    case 'DEPRECATED': return 'orange'
    case 'ARCHIVED': return 'gray'
    default: return 'default'
  }
})

const classTreeData = computed(() => {
  return buildTree(ontologyData.classHierarchy)
})

const classProperties = computed(() => {
  if (!selectedClass.value) return []
  return ontologyData.properties.filter(p => p.domainClassId === selectedClass.value!.id)
})

// ==================== 方法 ====================
function buildTree(hierarchy: any[]): any[] {
  return hierarchy.map(node => ({
    key: String(node.id),
    title: node.localName,
    icon: () => null,
    children: node.children?.length > 0 ? buildTree(node.children) : undefined,
    ...node
  }))
}

async function loadGraphs() {
  try {
    graphOptions.value = await graphApi.getList()
    if (graphOptions.value.length > 0 && !selectedGraphId.value) {
      selectedGraphId.value = graphOptions.value[0].graphId
    }
  } catch (err) {
    console.error('加载图谱列表失败', err)
  }
}

async function loadOntology() {
  if (!selectedGraphId.value) {
    ontologyData.definition = null
    ontologyData.classes = []
    ontologyData.properties = []
    ontologyData.constraints = []
    return
  }

  loading.value = true
  try {
    const data = await ontologyApi.getFullOntology(selectedGraphId.value)
    ontologyData.definition = data.definition
    ontologyData.classes = data.classes || []
    ontologyData.classHierarchy = data.classHierarchy || []
    ontologyData.properties = data.properties || []
    ontologyData.constraints = data.constraints || []
    selectedClass.value = null
    selectedProperty.value = null
    selectedClassKeys.value = []
  } catch (err: any) {
    if (err.code === 1002) {
      // 本体未定义
      ontologyData.definition = null
      ontologyData.classes = []
      ontologyData.properties = []
      ontologyData.constraints = []
    } else {
      message.error(err.message || '加载本体失败')
    }
  } finally {
    loading.value = false
  }
}

function onGraphChange() {
  loadOntology()
}

function refreshOntology() {
  loadOntology()
}

function onClassSelect(keys: string[]) {
  selectedClassKeys.value = keys
  if (keys.length > 0) {
    selectedClass.value = ontologyData.classes.find(c => String(c.id) === keys[0]) || null
    selectedProperty.value = null
  } else {
    selectedClass.value = null
  }
}

function openClassModal(cls?: OntClassVO) {
  classEditing.value = !!cls
  if (cls) {
    Object.assign(classForm, {
      id: cls.id,
      localName: cls.localName,
      classUri: cls.classUri,
      parentClassId: cls.parentClassId,
      domainHint: cls.domainHint || '',
      description: cls.description || '',
      example: cls.example || ''
    })
  } else {
    Object.assign(classForm, {
      id: undefined,
      localName: '',
      classUri: '',
      parentClassId: undefined,
      domainHint: '',
      description: '',
      example: ''
    })
  }
  showClassModal.value = true
}

async function handleSaveClass() {
  if (!selectedGraphId.value) {
    message.error('请先选择图谱')
    return
  }
  if (!classForm.localName.trim()) {
    message.error('请输入类名称')
    return
  }

  classSaving.value = true
  try {
    if (classEditing.value && classForm.id) {
      await ontologyApi.updateClass(selectedGraphId.value, classForm.id, {
        localName: classForm.localName,
        classUri: classForm.classUri || undefined,
        parentClassId: classForm.parentClassId,
        domainHint: classForm.domainHint || undefined,
        description: classForm.description || undefined,
        example: classForm.example || undefined
      })
      message.success('更新成功')
    } else {
      await ontologyApi.createClass(selectedGraphId.value, {
        localName: classForm.localName,
        classUri: classForm.classUri || undefined,
        parentClassId: classForm.parentClassId,
        domainHint: classForm.domainHint || undefined,
        description: classForm.description || undefined,
        example: classForm.example || undefined
      })
      message.success('创建成功')
    }
    showClassModal.value = false
    loadOntology()
  } catch (err: any) {
    message.error(err.message || '保存失败')
  } finally {
    classSaving.value = false
  }
}

function handleDeleteClass(cls: any) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除类 "${cls.localName || cls.title}" 吗？`,
    okText: '确认',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await ontologyApi.deleteClass(selectedGraphId.value!, cls.id || cls.key)
        message.success('删除成功')
        loadOntology()
      } catch (err: any) {
        message.error(err.message || '删除失败')
      }
    }
  })
}

function openPropertyModal(prop?: OntPropertyVO, cls?: any) {
  Object.assign(propertyForm, {
    id: prop?.id,
    localName: prop?.localName || '',
    propertyType: prop?.propertyType || 'DATATYPE',
    rangeDataType: prop?.rangeDataType || 'string',
    rangeClassId: prop?.rangeClassId,
    domainClassId: cls?.id ? Number(cls.id) : (prop?.domainClassId || selectedClass.value?.id),
    domainClassName: cls?.localName || cls?.title || selectedClass.value?.localName,
    minCardinality: prop?.minCardinality || 0,
    maxCardinality: prop?.maxCardinality,
    defaultValue: prop?.defaultValue || '',
    isRequired: prop?.isRequired || false,
    isMultiple: prop?.isMultiple || false
  })
  showPropertyModal.value = true
}

async function handleSaveProperty() {
  if (!selectedGraphId.value) {
    message.error('请先选择图谱')
    return
  }
  if (!propertyForm.localName.trim()) {
    message.error('请输入属性名称')
    return
  }

  propertySaving.value = true
  try {
    await ontologyApi.createProperty(selectedGraphId.value, {
      localName: propertyForm.localName,
      propertyType: propertyForm.propertyType,
      rangeDataType: propertyForm.rangeDataType || undefined,
      rangeClassId: propertyForm.rangeClassId,
      domainClassId: propertyForm.domainClassId,
      minCardinality: propertyForm.minCardinality,
      maxCardinality: propertyForm.maxCardinality,
      defaultValue: propertyForm.defaultValue || undefined,
      isRequired: propertyForm.isRequired,
      isMultiple: propertyForm.isMultiple
    })
    message.success('创建成功')
    showPropertyModal.value = false
    loadOntology()
  } catch (err: any) {
    message.error(err.message || '保存失败')
  } finally {
    propertySaving.value = false
  }
}

function handleDeleteProperty(prop: OntPropertyVO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除属性 "${prop.localName}" 吗？`,
    okText: '确认',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await ontologyApi.deleteProperty(selectedGraphId.value!, prop.id)
        message.success('删除成功')
        loadOntology()
      } catch (err: any) {
        message.error(err.message || '删除失败')
      }
    }
  })
}

function handleDeleteConstraint(constraint: OntConstraintVO) {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除此约束吗？`,
    okText: '确认',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await ontologyApi.deleteConstraint(selectedGraphId.value!, constraint.id)
        message.success('删除成功')
        loadOntology()
      } catch (err: any) {
        message.error(err.message || '删除失败')
      }
    }
  })
}

async function handleCreateDefinition() {
  if (!selectedGraphId.value) {
    message.error('请先选择图谱')
    return
  }
  if (!definitionForm.name.trim()) {
    message.error('请输入本体名称')
    return
  }

  definitionSaving.value = true
  try {
    await ontologyApi.createDefinition(selectedGraphId.value, {
      name: definitionForm.name,
      namespace: definitionForm.namespace,
      version: definitionForm.version,
      description: definitionForm.description
    })
    message.success('本体定义创建成功')
    showCreateDefinitionModal.value = false
    loadOntology()
  } catch (err: any) {
    message.error(err.message || '创建失败')
  } finally {
    definitionSaving.value = false
  }
}

function getPropertyTypeColor(type: string) {
  switch (type) {
    case 'DATATYPE': return 'blue'
    case 'OBJECT': return 'green'
    case 'ANNOTATION': return 'purple'
    default: return 'default'
  }
}

function getSeverityColor(severity: string) {
  switch (severity) {
    case 'ERROR': return 'red'
    case 'WARNING': return 'orange'
    case 'INFO': return 'blue'
    default: return 'default'
  }
}

function formatDate(date: string | undefined) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadGraphs()
})

watch(selectedGraphId, () => {
  if (selectedGraphId.value) {
    loadOntology()
  }
})
</script>

<style scoped lang="less">
@import '@/assets/styles/dark.less';

.ontology-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  .page-title {
    font-size: 24px;
    font-weight: 600;
    color: #ffffff;
    margin-bottom: 4px;
  }

  .page-desc {
    color: rgba(255, 255, 255, 0.8);
    font-size: 14px;
  }
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.ontology-overview {
  margin-bottom: 24px;

  .overview-card {
    .overview-header {
      margin-bottom: 16px;

      .overview-title {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 8px;

        h3 {
          margin: 0;
          font-size: 18px;
          color: #ffffff;
        }
      }

      .overview-meta {
        display: flex;
        gap: 24px;
        color: rgba(255, 255, 255, 0.8);
        font-size: 13px;
      }
    }

    .overview-stats {
      display: flex;
      gap: 48px;

      .stat-item {
        flex: 1;

        :deep(.ant-statistic-title) {
          color: rgba(255, 255, 255, 0.8);
        }

        :deep(.ant-statistic-content) {
          color: #ffffff;
        }
      }
    }
  }
}

.empty-state {
  padding: 60px 0;
  text-align: center;
}

.main-content {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  min-height: 400px;

  .left-panel {
    width: 320px;
    flex-shrink: 0;

    .tree-card {
      height: 100%;
      overflow: auto;
    }
  }

  .right-panel {
    flex: 1;

    .detail-card {
      height: 100%;
    }

    .detail-section {
      margin-top: 24px;

      .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        h4 {
          margin: 0;
        }
      }
    }

    .detail-actions {
      margin-top: 24px;
      padding-top: 16px;
      border-top: 1px solid @border-color;
    }
  }
}

.data-tabs {
  background: @bg-container;
  border-radius: 8px;
  padding: 16px;

  :deep(.ant-tabs-nav) {
    .ant-tabs-tab {
      color: rgba(255, 255, 255, 0.7);
      font-weight: 500;

      &:hover {
        color: #ffffff;
      }

      &.ant-tabs-tab-active {
        color: #ffffff;
        font-weight: 600;
      }
    }

    .ant-tabs-ink-bar {
      background: @primary-color;
    }
  }
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .node-actions {
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .node-actions {
    opacity: 1;
  }
}

:deep(.ant-tree-title) {
  width: 100%;
}

:deep(.ant-tree-node-content-wrapper) {
  width: 100%;
}
</style>
