<template>
  <div class="ontology-page">
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ $t('ontology.title') }}</h1>
        <p class="page-desc">{{ $t('ontology.titleDesc') }}</p>
      </div>
      <div class="header-right">
        <a-select
          v-model:value="selectedGraphId"
          :placeholder="$t('ontology.selectGraph')"
          style="width: 220px"
          @change="onGraphChange"
        >
          <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
            {{ g.name }}
          </a-select-option>
        </a-select>
        <a-button @click="refreshOntology">
          <ReloadOutlined :spin="loading" />
          {{ $t('ontology.refresh') }}
        </a-button>
      </div>
    </div>

    <div v-if="ontologyData.definition" class="ontology-overview">
      <a-card class="overview-card">
        <div class="overview-header">
          <div class="overview-title">
            <h3>{{ ontologyData.definition.name || $t('ontology.unnamedOntology') }}</h3>
            <a-tag :color="statusColor">{{ ontologyData.definition.status }}</a-tag>
          </div>
          <div class="overview-meta">
            <span>{{ $t('ontology.version') }}: {{ ontologyData.definition.version }}</span>
            <span>{{ $t('ontology.namespace') }}: {{ ontologyData.definition.namespace }}</span>
          </div>
        </div>
        <div class="overview-stats">
          <div class="stat-item">
            <a-statistic :title="$t('ontology.classCount')" :value="ontologyData.definition.classCount" />
          </div>
          <div class="stat-item">
            <a-statistic :title="$t('ontology.propertyCount')" :value="ontologyData.definition.propertyCount" />
          </div>
          <div class="stat-item">
            <a-statistic :title="$t('ontology.constraintCount')" :value="ontologyData.definition.constraintCount" />
          </div>
        </div>
      </a-card>
    </div>

    <div v-else-if="selectedGraphId && !loading" class="empty-state">
      <a-empty :description="$t('ontology.noOntology')">
        <a-button type="primary" @click="showCreateDefinitionModal = true">
          <PlusOutlined /> {{ $t('ontology.createDefinition') }}
        </a-button>
      </a-empty>
    </div>

    <div v-if="ontologyData.definition" class="main-content">
      <div class="left-panel">
        <a-card class="tree-card" :title="$t('ontology.classHierarchy')">
          <template #extra>
            <a-button type="link" size="small" @click="openClassModal()">
              <PlusOutlined /> {{ $t('ontology.addClass') }}
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
          <a-empty v-else :description="$t('ontology.noOntology')" />
        </a-card>
      </div>

      <div class="right-panel">
        <a-card v-if="selectedClass" class="detail-card" :title="$t('ontology.classDetails')">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item :label="$t('ontology.localName')">{{ selectedClass.localName }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ontology.fullUri')">
              <a-tag>{{ selectedClass.classUri }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ontology.description')" :span="2">{{ selectedClass.description || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ontology.example')" :span="2">{{ selectedClass.example || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ontology.domainClassification')">{{ selectedClass.domainHint || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('common.createdAt')">{{ formatDate(selectedClass.createdAt) }}</a-descriptions-item>
          </a-descriptions>

          <div class="detail-section">
            <div class="section-header">
              <h4>{{ $t('ontology.propertyList') }}</h4>
              <a-button type="link" size="small" @click="openPropertyModal(undefined, selectedClass)">
                <PlusOutlined /> {{ $t('ontology.addProperty') }}
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
                    {{ record.isRequired ? $t('ontology.required') : $t('common.optional') }}
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
              <a-button type="primary" @click="openClassModal(selectedClass)">{{ $t('ontology.editClass') }}</a-button>
            </a-space>
          </div>
        </a-card>

        <a-card v-else-if="selectedProperty" class="detail-card" :title="$t('ontology.propertyDetails')">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item :label="$t('ontology.localName')">{{ selectedProperty.localName }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ontology.fullUri')">
              <a-tag>{{ selectedProperty.propertyUri }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ontology.propertyType')">
              <a-tag :color="getPropertyTypeColor(selectedProperty.propertyType)">
                {{ selectedProperty.propertyType }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ontology.dataType')">{{ selectedProperty.rangeDataType || '-' }}</a-descriptions-item>
            <a-descriptions-item :label="$t('ontology.required')">
              <a-tag :color="selectedProperty.isRequired ? 'red' : 'default'">
                {{ selectedProperty.isRequired ? $t('common.yes') : $t('common.no') }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item :label="$t('ontology.required')">
              <a-tag :color="selectedProperty.isMultiple ? 'blue' : 'default'">
                {{ selectedProperty.isMultiple ? $t('common.yes') : $t('common.no') }}
              </a-tag>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card v-else class="detail-card">
          <a-empty :description="$t('ontology.selectLeftToView')" />
        </a-card>
      </div>
    </div>

    <a-tabs v-if="ontologyData.definition" v-model:activeKey="activeTab" class="data-tabs">
      <a-tab-pane key="classes" :tab="$t('ontology.classList')">
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
                <a-button type="link" size="small" @click="openClassModal(record)">{{ $t('common.edit') }}</a-button>
                <a-button type="link" size="small" danger @click="handleDeleteClass(record)">{{ $t('common.delete') }}</a-button>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="properties" :tab="$t('ontology.propertyListTab')">
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
              <a-tag :color="record.isRequired ? 'red' : 'default'">{{ record.isRequired ? $t('ontology.required') : $t('common.optional') }}</a-tag>
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" danger @click="handleDeleteProperty(record)">{{ $t('common.delete') }}</a-button>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="constraints" :tab="$t('ontology.constraintList')">
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
              <a-button type="link" size="small" danger @click="handleDeleteConstraint(record)">{{ $t('common.delete') }}</a-button>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="history" :tab="$t('ontology.versionHistory')">
        <VersionHistory :graph-id="selectedGraphId" />
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="showCreateDefinitionModal"
      :title="$t('ontology.createOntologyDefinition')"
      @ok="handleCreateDefinition"
      :confirm-loading="definitionSaving"
    >
      <a-form :model="definitionForm" layout="vertical">
        <a-form-item :label="$t('ontology.ontologyName')" required>
          <a-input v-model:value="definitionForm.name" :placeholder="$t('ontology.ontologyNamePlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('ontology.namespace')">
          <a-input v-model:value="definitionForm.namespace" placeholder="http://graphiti.io/ontology" />
        </a-form-item>
        <a-form-item :label="$t('ontology.versionNumber')">
          <a-input v-model:value="definitionForm.version" placeholder="1.0.0" />
        </a-form-item>
        <a-form-item :label="$t('common.description')">
          <a-textarea v-model:value="definitionForm.description" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showClassModal"
      :title="classEditing ? $t('ontology.editClass') : $t('ontology.newClass')"
      @ok="handleSaveClass"
      :confirm-loading="classSaving"
      width="600px"
    >
      <a-form :model="classForm" layout="vertical">
        <a-form-item :label="$t('ontology.localName')" required>
          <a-input v-model:value="classForm.localName" :placeholder="$t('ontology.propertyNamePlaceholder')" />
        </a-form-item>
        <a-form-item :label="$t('ontology.fullUri')">
          <a-input v-model:value="classForm.classUri" placeholder="http://graphiti.io/ontology/..." />
        </a-form-item>
        <a-form-item :label="$t('ontology.parentClass')">
          <a-select v-model:value="classForm.parentClassId" :placeholder="$t('ontology.selectParentOptional')" allow-clear>
            <a-select-option v-for="c in ontologyData.classes" :key="c.id" :value="c.id">
              {{ c.localName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('ontology.domainClassification')">
          <a-select v-model:value="classForm.domainHint" :placeholder="$t('ontology.selectDomain')">
            <a-select-option value="FINANCIAL">{{ $t('ontology.financial') }}</a-select-option>
            <a-select-option value="MEDICAL">{{ $t('ontology.medical') }}</a-select-option>
            <a-select-option value="ECOMMERCE">{{ $t('ontology.ecommerce') }}</a-select-option>
            <a-select-option value="LEGAL">{{ $t('ontology.legal') }}</a-select-option>
            <a-select-option value="KNOWLEDGE">{{ $t('ontology.generalKnowledge') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('common.description')">
          <a-textarea v-model:value="classForm.description" :rows="3" />
        </a-form-item>
        <a-form-item :label="$t('ontology.example')">
          <a-textarea v-model:value="classForm.example" :rows="2" :placeholder="$t('ontology.example')" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="showPropertyModal"
      :title="$t('ontology.addPropertyFor', { class: propertyForm.domainClassName || $t('common.menu') })"
      @ok="handleSaveProperty"
      :confirm-loading="propertySaving"
      width="700px"
    >
      <a-form :model="propertyForm" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item :label="$t('ontology.propertyName')" required>
              <a-input v-model:value="propertyForm.localName" :placeholder="$t('ontology.propertyNamePlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ontology.propertyType')" required>
              <a-select v-model:value="propertyForm.propertyType">
                <a-select-option value="DATATYPE">{{ $t('ontology.datatypeProperty') }}</a-select-option>
                <a-select-option value="OBJECT">{{ $t('ontology.objectProperty') }}</a-select-option>
                <a-select-option value="ANNOTATION">{{ $t('ontology.annotationProperty') }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16" v-if="propertyForm.propertyType === 'DATATYPE'">
          <a-col :span="12">
            <a-form-item :label="$t('ontology.dataType')">
              <a-select v-model:value="propertyForm.rangeDataType">
                <a-select-option value="string">{{ $t('ontology.string') }}</a-select-option>
                <a-select-option value="integer">{{ $t('ontology.integer') }}</a-select-option>
                <a-select-option value="float">{{ $t('ontology.float') }}</a-select-option>
                <a-select-option value="boolean">{{ $t('ontology.boolean') }}</a-select-option>
                <a-select-option value="date">{{ $t('ontology.date') }}</a-select-option>
                <a-select-option value="datetime">{{ $t('ontology.datetime') }}</a-select-option>
                <a-select-option value="json">{{ $t('ontology.json') }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="$t('ontology.defaultValue')">
              <a-input v-model:value="propertyForm.defaultValue" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16" v-if="propertyForm.propertyType === 'OBJECT'">
          <a-col :span="24">
            <a-form-item :label="$t('ontology.targetClass')">
              <a-select v-model:value="propertyForm.rangeClassId" :placeholder="$t('ontology.selectDomain')">
                <a-select-option v-for="c in ontologyData.classes" :key="c.id" :value="c.id">
                  {{ c.localName }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item :label="$t('ontology.minCardinality')">
              <a-input-number v-model:value="propertyForm.minCardinality" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="$t('ontology.maxCardinality')">
              <a-input-number v-model:value="propertyForm.maxCardinality" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="$t('ontology.required')">
              <a-switch v-model:checked="propertyForm.isRequired" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from "vue"
import { message, Modal } from "ant-design-vue"
import { PlusOutlined, DeleteOutlined, ReloadOutlined, ApiOutlined } from "@ant-design/icons-vue"
import { ontologyApi, type OntologyFullVO, type OntClassVO, type OntPropertyVO, type OntConstraintVO } from "@/api/ontology"
import { graphApi, type Graph } from "@/api/graph"
import VersionHistory from "./VersionHistory.vue"

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

const activeTab = ref("classes")

const selectedClassKeys = ref<string[]>([])
const selectedClass = ref<OntClassVO | null>(null)
const selectedProperty = ref<OntPropertyVO | null>(null)

const showCreateDefinitionModal = ref(false)
const showClassModal = ref(false)
const showPropertyModal = ref(false)
const definitionSaving = ref(false)
const classSaving = ref(false)
const propertySaving = ref(false)
const classEditing = ref(false)

const definitionForm = reactive({
  name: "",
  namespace: "http://graphiti.io/ontology",
  version: "1.0.0",
  description: ""
})

const classForm = reactive({
  id: undefined as number | undefined,
  localName: "",
  classUri: "",
  parentClassId: undefined as number | undefined,
  domainHint: "",
  description: "",
  example: ""
})

const propertyForm = reactive({
  id: undefined as number | undefined,
  localName: "",
  propertyType: "DATATYPE" as "DATATYPE" | "OBJECT" | "ANNOTATION",
  rangeDataType: "string",
  rangeClassId: undefined as number | undefined,
  domainClassId: undefined as number | undefined,
  domainClassName: "",
  minCardinality: 0,
  maxCardinality: undefined as number | undefined,
  defaultValue: "",
  isRequired: false,
  isMultiple: false
})

const classColumns = [
  { title: "ontology.localName", dataIndex: "localName", key: "localName" },
  { title: "URI", dataIndex: "classUri", key: "classUri", ellipsis: true },
  { title: "ontology.domainClassification", key: "domainHint" },
  { title: "ontology.description", dataIndex: "description", key: "description", ellipsis: true },
  { title: "common.createdAt", dataIndex: "createdAt", key: "createdAt" },
  { title: "common.action", key: "action", width: 150 }
]

const propertyColumns = [
  { title: "ontology.localName", dataIndex: "localName", key: "localName" },
  { title: "ontology.propertyType", key: "propertyType" },
  { title: "ontology.dataType", dataIndex: "rangeDataType", key: "rangeDataType" },
  { title: "ontology.required", key: "isRequired" },
  { title: "common.createdAt", dataIndex: "createdAt", key: "createdAt" },
  { title: "common.action", key: "action", width: 80 }
]

const constraintColumns = [
  { title: "Type", dataIndex: "constraintType", key: "constraintType" },
  { title: "Severity", key: "severity" },
  { title: "Error Message", dataIndex: "errorMessage", key: "errorMessage", ellipsis: true },
  { title: "ontology.description", dataIndex: "description", key: "description", ellipsis: true },
  { title: "common.action", key: "action", width: 80 }
]

const statusColor = computed(() => {
  switch (ontologyData.definition?.status) {
    case "ACTIVE": return "green"
    case "DEPRECATED": return "orange"
    case "ARCHIVED": return "gray"
    default: return "default"
  }
})

const classTreeData = computed(() => {
  return buildTree(ontologyData.classHierarchy)
})

const classProperties = computed(() => {
  if (!selectedClass.value) return []
  return ontologyData.properties.filter(p => p.domainClassId === selectedClass.value!.id)
})

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
    console.error("ontology.loadGraphsFailed", err)
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
      ontologyData.definition = null
      ontologyData.classes = []
      ontologyData.properties = []
      ontologyData.constraints = []
    } else {
      message.error(err.message || "ontology.loadFailed")
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
      domainHint: cls.domainHint || "",
      description: cls.description || "",
      example: cls.example || ""
    })
  } else {
    Object.assign(classForm, {
      id: undefined,
      localName: "",
      classUri: "",
      parentClassId: undefined,
      domainHint: "",
      description: "",
      example: ""
    })
  }
  showClassModal.value = true
}

async function handleSaveClass() {
  if (!selectedGraphId.value) {
    message.error("ontology.selectGraphFirst")
    return
  }
  if (!classForm.localName.trim()) {
    message.error("ontology.enterClassName")
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
      message.success("ontology.updateSuccess")
    } else {
      await ontologyApi.createClass(selectedGraphId.value, {
        localName: classForm.localName,
        classUri: classForm.classUri || undefined,
        parentClassId: classForm.parentClassId,
        domainHint: classForm.domainHint || undefined,
        description: classForm.description || undefined,
        example: classForm.example || undefined
      })
      message.success("ontology.createSuccess")
    }
    showClassModal.value = false
    loadOntology()
  } catch (err: any) {
    message.error(err.message || "ontology.saveFailed")
  } finally {
    classSaving.value = false
  }
}

function handleDeleteClass(cls: any) {
  Modal.confirm({
    title: "ontology.confirmDeleteClass",
    content: `Ontology:confirmDeleteClass "${cls.localName || cls.title}"?`,
    okText: "common.confirm",
    okType: "danger",
    cancelText: "common.cancel",
    async onOk() {
      try {
        await ontologyApi.deleteClass(selectedGraphId.value!, cls.id || cls.key)
        message.success("ontology.deleteSuccess")
        loadOntology()
      } catch (err: any) {
        message.error(err.message || "ontology.deleteSuccess")
      }
    }
  })
}

function openPropertyModal(prop?: OntPropertyVO, cls?: any) {
  Object.assign(propertyForm, {
    id: prop?.id,
    localName: prop?.localName || "",
    propertyType: prop?.propertyType || "DATATYPE",
    rangeDataType: prop?.rangeDataType || "string",
    rangeClassId: prop?.rangeClassId,
    domainClassId: cls?.id ? Number(cls.id) : (prop?.domainClassId || selectedClass.value?.id),
    domainClassName: cls?.localName || cls?.title || selectedClass.value?.localName,
    minCardinality: prop?.minCardinality || 0,
    maxCardinality: prop?.maxCardinality,
    defaultValue: prop?.defaultValue || "",
    isRequired: prop?.isRequired || false,
    isMultiple: prop?.isMultiple || false
  })
  showPropertyModal.value = true
}

async function handleSaveProperty() {
  if (!selectedGraphId.value) {
    message.error("ontology.selectGraphFirst")
    return
  }
  if (!propertyForm.localName.trim()) {
    message.error("ontology.enterPropertyName")
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
    message.success("ontology.createSuccess")
    showPropertyModal.value = false
    loadOntology()
  } catch (err: any) {
    message.error(err.message || "ontology.saveFailed")
  } finally {
    propertySaving.value = false
  }
}

function handleDeleteProperty(prop: OntPropertyVO) {
  Modal.confirm({
    title: "common.confirm",
    content: `Ontology:deleteProperty "${prop.localName}"?`,
    okText: "common.confirm",
    okType: "danger",
    cancelText: "common.cancel",
    async onOk() {
      try {
        await ontologyApi.deleteProperty(selectedGraphId.value!, prop.id)
        message.success("ontology.deleteSuccess")
        loadOntology()
      } catch (err: any) {
        message.error(err.message || "ontology.deleteSuccess")
      }
    }
  })
}

function handleDeleteConstraint(constraint: OntConstraintVO) {
  Modal.confirm({
    title: "common.confirm",
    content: "ontology.deleteConstraint?",
    okText: "common.confirm",
    okType: "danger",
    cancelText: "common.cancel",
    async onOk() {
      try {
        await ontologyApi.deleteConstraint(selectedGraphId.value!, constraint.id)
        message.success("ontology.deleteSuccess")
        loadOntology()
      } catch (err: any) {
        message.error(err.message || "ontology.deleteSuccess")
      }
    }
  })
}

async function handleCreateDefinition() {
  if (!selectedGraphId.value) {
    message.error("ontology.selectGraphFirst")
    return
  }
  if (!definitionForm.name.trim()) {
    message.error("ontology.ontologyName")
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
    message.success("ontology.createSuccess")
    showCreateDefinitionModal.value = false
    loadOntology()
  } catch (err: any) {
    message.error(err.message || "ontology.saveFailed")
  } finally {
    definitionSaving.value = false
  }
}

function getPropertyTypeColor(type: string) {
  switch (type) {
    case "DATATYPE": return "blue"
    case "OBJECT": return "green"
    case "ANNOTATION": return "purple"
    default: return "default"
  }
}

function getSeverityColor(severity: string) {
  switch (severity) {
    case "ERROR": return "red"
    case "WARNING": return "orange"
    case "INFO": return "blue"
    default: return "default"
  }
}

function formatDate(date: string | undefined) {
  if (!date) return "-"
  return new Date(date).toLocaleString()
}

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
@import "@/assets/styles/dark.less";

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
