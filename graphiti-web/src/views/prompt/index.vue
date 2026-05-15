<template>
  <div class="prompt-page">
    <!-- Page Header -->
    <div class="page-header">
      <h1 class="page-title">{{ $t('prompt.title') }}</h1>
      <p class="page-desc">{{ $t('prompt.titleDesc') }}</p>
    </div>

    <!-- Filter Bar -->
    <a-card class="filter-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-select
            v-model:value="filterType"
            :placeholder="$t('prompt.templateType')"
            style="width: 100%"
            allow-clear
            @change="loadTemplates"
          >
            <a-select-option v-for="t in templateTypes" :key="t.value" :value="t.value">
              {{ t.label }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="6">
          <a-input-search
            v-model:value="searchKeyword"
            :placeholder="($t('prompt.searchPlaceholder') as string) || 'Search template name/code'"
            style="width: 100%"
            @search="loadTemplates"
          />
        </a-col>
        <a-col :span="12">
          <a-space>
            <a-button type="primary" @click="showCreateModal">
              <PlusOutlined /> {{ $t('prompt.createPrompt') }}
            </a-button>
            <a-button @click="loadTemplates">
              <ReloadOutlined /> {{ $t('common.refresh') }}
            </a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <!-- Template List -->
    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="templateList"
        :loading="loading"
        row-key="id"
        :pagination="{ pageSize: 10 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="template-name">{{ record.name }}</div>
          </template>
          <template v-if="column.key === 'code'">
            <code class="template-code">{{ record.code }}</code>
          </template>
          <template v-if="column.key === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeLabel(record.type) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'enabled'">
            <a-switch
              :checked="record.enabled"
              size="small"
              @change="(checked: boolean) => toggleEnabled(record.id, checked)"
            />
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="editTemplate(record)">{{ $t('common.edit') }}</a-button>
              <a-button type="link" size="small" @click="testTemplate(record)">{{ $t('prompt.testPrompt') }}</a-button>
              <a-dropdown>
                <a-button type="link" size="small">
                  <MoreOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="view" @click="viewDetail(record)">{{ $t('prompt.viewDetail') }}</a-menu-item>
                    <a-menu-item key="versions" @click="viewVersions(record)">{{ $t('prompt.versionHistory') }}</a-menu-item>
                    <a-menu-item key="copy" @click="copyTemplate(record)">{{ $t('prompt.copyTemplate') }}</a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="delete" @click="deleteTemplate(record.id)">
                      <span style="color: #ff4d4f">{{ $t('common.delete') }}</span>
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- Create/Edit Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="isEditing ? $t('prompt.editPrompt') : $t('prompt.createPrompt')"
      @ok="handleSave"
      :confirm-loading="saving"
      width="800px"
      :maskClosable="false"
    >
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        :label-col="{ span: 3 }"
        layout="horizontal"
      >
        <a-form-item :label="$t('prompt.templateCode')" name="code">
          <a-input
            v-model:value="form.code"
            :placeholder="$t('prompt.pleaseInputCode')"
            :disabled="isEditing"
          />
        </a-form-item>
        <a-form-item :label="$t('prompt.templateName')" name="name">
          <a-input v-model:value="form.name" :placeholder="$t('prompt.pleaseInputName')" />
        </a-form-item>
        <a-form-item :label="$t('prompt.templateDesc')" name="description">
          <a-textarea v-model:value="form.description" :rows="2" :placeholder="$t('prompt.templateDesc')" />
        </a-form-item>
        <a-form-item :label="$t('prompt.templateType')" name="type">
          <a-select v-model:value="form.type" :placeholder="$t('prompt.pleaseSelectType')">
            <a-select-option v-for="t in templateTypes" :key="t.value" :value="t.value">
              {{ t.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('prompt.model')" name="model">
          <a-input v-model:value="form.model" :placeholder="$t('prompt.model')" />
        </a-form-item>

        <a-divider>{{ $t('prompt.systemPrompt') }}</a-divider>

        <a-form-item :label="$t('prompt.systemPrompt')" name="systemPrompt">
          <a-textarea
            v-model:value="form.systemPrompt"
            :rows="6"
            :placeholder="$t('prompt.inputPrompt')"
          />
        </a-form-item>
        <a-form-item :label="$t('prompt.userPrompt')" name="userPromptTemplate">
          <a-textarea
            v-model:value="form.userPromptTemplate"
            :rows="6"
            :placeholder="$t('prompt.inputUserPrompt')"
          />
        </a-form-item>
        <a-form-item :label="$t('prompt.responseFormat')" name="responseFormat">
          <a-textarea
            v-model:value="form.responseFormat"
            :rows="4"
            :placeholder="$t('prompt.inputResponseFormat')"
          />
        </a-form-item>

        <a-divider>{{ $t('prompt.variables') }}</a-divider>

        <div class="variables-section">
          <a-button type="dashed" block @click="addVariable" style="margin-bottom: 12px">
            <PlusOutlined /> {{ $t('prompt.addVariable') }}
          </a-button>
          <div v-for="(v, idx) in form.variables" :key="idx" class="variable-item">
            <a-row :gutter="8" align="middle">
              <a-col :span="3">
                <a-input v-model:value="v.variableName" :placeholder="$t('prompt.variableName')" />
              </a-col>
              <a-col :span="3">
                <a-select v-model:value="v.variableType" style="width: 100%">
                  <a-select-option value="string">{{ $t('prompt.string') }}</a-select-option>
                  <a-select-option value="text">{{ $t('prompt.text') }}</a-select-option>
                  <a-select-option value="list">{{ $t('prompt.list') }}</a-select-option>
                  <a-select-option value="json">JSON</a-select-option>
                </a-select>
              </a-col>
              <a-col :span="4">
                <a-input v-model:value="v.description" :placeholder="$t('prompt.variableDesc')" />
              </a-col>
              <a-col :span="3">
                <a-select v-model:value="v.source" style="width: 100%">
                  <a-select-option value="context">{{ $t('prompt.context') }}</a-select-option>
                  <a-select-option value="static">{{ $t('prompt.static') }}</a-select-option>
                  <a-select-option value="llm">{{ $t('prompt.dynamic') }}</a-select-option>
                </a-select>
              </a-col>
              <a-col :span="4">
                <a-input v-model:value="v.defaultValue" :placeholder="$t('prompt.defaultValue')" />
              </a-col>
              <a-col :span="2">
                <a-checkbox v-model:checked="v.required">{{ $t('prompt.required') }}</a-checkbox>
              </a-col>
              <a-col :span="4">
                <a-button type="text" danger size="small" @click="removeVariable(idx)">
                  <DeleteOutlined />
                </a-button>
              </a-col>
            </a-row>
          </div>
        </div>
      </a-form>
    </a-modal>

    <!-- Detail Modal -->
    <a-modal
      v-model:open="detailVisible"
      :title="$t('prompt.viewDetail')"
      :footer="null"
      width="700px"
    >
      <a-descriptions :column="1" bordered v-if="currentTemplate">
        <a-descriptions-item :label="$t('prompt.templateCode')">{{ currentTemplate.code }}</a-descriptions-item>
        <a-descriptions-item :label="$t('prompt.templateName')">{{ currentTemplate.name }}</a-descriptions-item>
        <a-descriptions-item :label="$t('prompt.templateDesc')">{{ currentTemplate.description || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('prompt.templateType')">{{ getTypeLabel(currentTemplate.type) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('prompt.model')">{{ currentTemplate.model || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('prompt.enabled')">
          <a-tag :color="currentTemplate.enabled ? 'green' : 'red'">
            {{ currentTemplate.enabled ? $t('prompt.enabled') : $t('prompt.disabled') }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('prompt.systemPrompt')">
          <pre class="prompt-content">{{ currentTemplate.systemPrompt }}</pre>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('prompt.userPrompt')">
          <pre class="prompt-content">{{ currentTemplate.userPromptTemplate }}</pre>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('prompt.responseFormat')">
          <pre class="prompt-content">{{ currentTemplate.responseFormat || '-' }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- Version History Modal -->
    <a-modal
      v-model:open="versionsVisible"
      :title="$t('prompt.versionHistory')"
      :footer="null"
      width="700px"
    >
      <a-timeline v-if="versionList.length > 0">
        <a-timeline-item
          v-for="v in versionList"
          :key="v.id"
          :color="v.active ? 'green' : 'gray'"
        >
          <div class="version-item">
            <div class="version-header">
              <strong>{{ $t('prompt.version') || 'Version' }} {{ v.version }}</strong>
              <a-tag v-if="v.active" color="green">{{ $t('prompt.currentVersion') }}</a-tag>
            </div>
            <div class="version-desc">{{ v.description || $t('prompt.noDescription') }}</div>
            <div class="version-time">{{ formatDate(v.createdAt) }}</div>
            <a-button
              v-if="!v.active"
              type="link"
              size="small"
              @click="rollbackVersion(v.templateId, v.version)"
            >
              {{ $t('prompt.rollbackToVersion') }}
            </a-button>
          </div>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else :description="$t('prompt.noDescription')" />
    </a-modal>

    <!-- Test Modal -->
    <a-modal
      v-model:open="testVisible"
      :title="$t('prompt.testPrompt')"
      @ok="handleTest"
      :confirm-loading="testing"
      width="1000px"
      :maskClosable="false"
    >
      <a-row :gutter="16">
        <!-- Left: Test Config & Input -->
        <a-col :span="12">
          <a-form layout="vertical">
            <a-form-item :label="$t('prompt.testPrompt')">
              <a-input :value="currentTemplate?.name" disabled />
            </a-form-item>
            <a-form-item :label="$t('prompt.inputContent')">
              <a-textarea
                v-model:value="testInput"
                :rows="10"
                :placeholder="$t('prompt.inputContent')"
              />
            </a-form-item>
            <a-form-item :label="$t('prompt.extraVariables')">
              <a-textarea
                v-model:value="testVariables"
                :rows="3"
                placeholder="{}"
              />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleTest" :loading="testing">
                  {{ $t('prompt.executeTest') }}
                </a-button>
                <a-button @click="generateSampleData" :loading="generating">
                  {{ $t('prompt.generateTestData') }}
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-col>

        <!-- Right: Test Result -->
        <a-col :span="12">
          <div class="test-result">
            <div class="result-header">
              <span>{{ $t('prompt.testResult') }}</span>
              <a-space v-if="testResult">
                <a-tag v-if="testResult.success" color="green">{{ $t('prompt.success') }}</a-tag>
                <a-tag v-else color="red">{{ $t('prompt.failed') }}</a-tag>
                <span class="elapsed">{{ testResult.elapsedMs }}ms</span>
              </a-space>
            </div>
            <div class="result-content">
              <a-spin :spinning="testing">
                <template v-if="testResult">
                  <div v-if="testResult.success" class="result-success">
                    <div v-if="testResult.entityCount !== undefined || testResult.edgeCount !== undefined" class="stats">
                      <span>{{ $t('data.entities') }}: {{ testResult.entityCount || 0 }}</span>
                      <span>{{ $t('data.edges') }}: {{ testResult.edgeCount || 0 }}</span>
                    </div>
                    <div v-if="testResult.rawResponse" class="raw-response">
                      <div class="section-title">{{ $t('prompt.rawResponse') }}:</div>
                      <pre>{{ testResult.rawResponse }}</pre>
                    </div>
                    <div v-if="testResult.parsedData" class="parsed-data">
                      <div class="section-title">{{ $t('prompt.parsedResult') }}:</div>
                      <pre>{{ testResult.parsedData }}</pre>
                    </div>
                  </div>
                  <div v-else class="result-error">
                    <a-alert type="error" :message="testResult.errorMessage" />
                  </div>
                </template>
                  <a-empty v-else :description="$t('prompt.clickExecuteTest')" />
              </a-spin>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-modal>

    <!-- Generate Sample Modal -->
    <a-modal
      v-model:open="sampleModalVisible"
      :title="$t('prompt.generateTestData')"
      @ok="handleUseSample"
      width="600px"
    >
      <a-form layout="vertical">
        <a-form-item :label="$t('prompt.dataType')">
          <a-select v-model:value="sampleConfig.dataType">
            <a-select-option value="legal">{{ $t('prompt.legalCase') }}</a-select-option>
            <a-select-option value="medical">{{ $t('prompt.medicalRecord') }}</a-select-option>
            <a-select-option value="financial">{{ $t('prompt.financial') }}</a-select-option>
            <a-select-option value="general">{{ $t('prompt.general') }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="$t('prompt.generateCount')">
          <a-input-number v-model:value="sampleConfig.count" :min="1" :max="10" />
        </a-form-item>
        <a-form-item :label="$t('prompt.scenario')">
          <a-input v-model:value="sampleConfig.scenario" :placeholder="$t('prompt.scenario')" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="doGenerateSample" :loading="generating">
            {{ $t('prompt.executeTest') }}
          </a-button>
        </a-form-item>
      </a-form>

      <a-divider>{{ $t('prompt.generatedSamples') }}</a-divider>

      <div v-if="generatedSamples.length > 0" class="sample-list">
        <div
          v-for="(sample, idx) in generatedSamples"
          :key="idx"
          class="sample-item"
          :class="{ selected: selectedSampleIdx === idx }"
          @click="selectedSampleIdx = idx"
        >
          <div class="sample-index">{{ $t('prompt.generatedSamples') }} {{ idx + 1 }}</div>
          <div class="sample-content">{{ sample.content?.substring(0, 200) }}...</div>
        </div>
      </div>
      <a-empty v-else :description="$t('prompt.clickExecuteTest')" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  DeleteOutlined,
  MoreOutlined
} from '@ant-design/icons-vue'
import { promptApi, type PromptTemplate, type PromptVariable, type PromptVersion, type PromptTestResp, type SampleData } from '@/api/prompt'

const { t } = useI18n()

// State
const templateList = ref<PromptTemplate[]>([])
const templateTypes = ref<Array<{ value: string; label: string }>>([])
const loading = ref(false)
const filterType = ref<string | undefined>(undefined)
const searchKeyword = ref('')

// Form state
const modalVisible = ref(false)
const detailVisible = ref(false)
const versionsVisible = ref(false)
const testVisible = ref(false)
const sampleModalVisible = ref(false)
const saving = ref(false)
const isEditing = ref(false)
const currentTemplate = ref<PromptTemplate | null>(null)
const versionList = ref<PromptVersion[]>([])
const formRef = ref()

const form = reactive({
  id: undefined as number | undefined,
  code: '',
  name: '',
  description: '',
  type: '',
  systemPrompt: '',
  userPromptTemplate: '',
  responseFormat: '',
  enabled: true,
  model: '',
  sort: 0,
  tags: [] as string[],
  variables: [] as PromptVariable[]
})

const formRules = {
  code: [{ required: true, message: t('prompt.pleaseInputCode') }],
  name: [{ required: true, message: t('prompt.pleaseInputName') }],
  type: [{ required: true, message: t('prompt.pleaseSelectType') }],
  systemPrompt: [{ required: true, message: t('prompt.pleaseInputSystemPrompt') }],
  userPromptTemplate: [{ required: true, message: t('prompt.pleaseInputUserPrompt') }]
}

// Test state
const testInput = ref('')
const testVariables = ref('')
const testing = ref(false)
const testResult = ref<PromptTestResp | null>(null)

// Sample generation state
const generating = ref(false)
const sampleConfig = reactive({
  dataType: 'legal',
  count: 3,
  scenario: ''
})
const generatedSamples = ref<SampleData[]>([])
const selectedSampleIdx = ref(0)

// Columns
const columns = [
  { title: t('prompt.templateName'), key: 'name', width: 250 },
  { title: t('prompt.templateCode'), key: 'code', width: 200 },
  { title: t('prompt.templateType'), key: 'type', width: 120 },
  { title: t('prompt.model'), dataIndex: 'model', width: 120 },
  { title: t('prompt.enabled'), key: 'enabled', width: 80 },
  { title: t('common.updatedAt'), dataIndex: 'updatedAt', width: 160 },
  { title: t('common.action'), key: 'action', width: 180 }
]

// Methods
const loadTemplates = async () => {
  loading.value = true
  try {
    if (filterType.value) {
      templateList.value = await promptApi.listByType(filterType.value)
    } else {
      templateList.value = await promptApi.list()
    }
    // Simple client-side search
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase()
      templateList.value = templateList.value.filter(
        t => t.name.toLowerCase().includes(kw) || t.code.toLowerCase().includes(kw)
      )
    }
  } catch (err: any) {
    message.error(err.message || t('notification.loadFailed'))
  } finally {
    loading.value = false
  }
}

const loadTemplateTypes = async () => {
  try {
    templateTypes.value = await promptApi.getTypes()
  } catch (err) {
    // Fallback types
    templateTypes.value = [
      { value: 'entity_extract', label: t('prompt.entityExtract') },
      { value: 'edge_extract', label: t('prompt.edgeExtract') },
      { value: 'dedupe', label: t('prompt.dedupe') },
      { value: 'summary', label: t('prompt.summary') }
    ]
  }
}

const getTypeColor = (type: string): string => {
  const colors: Record<string, string> = {
    entity_extract: 'blue',
    edge_extract: 'green',
    dedupe: 'orange',
    summary: 'purple',
    classify: 'cyan',
    attribute: 'magenta'
  }
  return colors[type] || 'default'
}

const getTypeLabel = (type: string): string => {
  const t = templateTypes.value.find(x => x.value === type)
  return t?.label || type
}

const showCreateModal = () => {
  isEditing.value = false
  resetForm()
  modalVisible.value = true
}

const editTemplate = (record: PromptTemplate) => {
  isEditing.value = true
  Object.assign(form, {
    id: record.id,
    code: record.code,
    name: record.name,
    description: record.description || '',
    type: record.type,
    systemPrompt: record.systemPrompt,
    userPromptTemplate: record.userPromptTemplate,
    responseFormat: record.responseFormat || '',
    enabled: record.enabled,
    model: record.model || '',
    sort: record.sort || 0,
    tags: record.tags || [],
    variables: record.variables || []
  })
  modalVisible.value = true
}

const viewDetail = (record: PromptTemplate) => {
  currentTemplate.value = record
  detailVisible.value = true
}

const viewVersions = async (record: PromptTemplate) => {
  try {
    versionList.value = await promptApi.getVersionHistory(record.id)
    currentTemplate.value = record
    versionsVisible.value = true
  } catch (err: any) {
    message.error(err.message || t('prompt.testResult') + ' failed')
  }
}

const copyTemplate = (record: PromptTemplate) => {
  isEditing.value = false
  Object.assign(form, {
    code: record.code + '_copy',
    name: record.name + ' - ' + t('prompt.copyTemplate'),
    description: record.description || '',
    type: record.type,
    systemPrompt: record.systemPrompt,
    userPromptTemplate: record.userPromptTemplate,
    responseFormat: record.responseFormat || '',
    enabled: true,
    model: record.model || '',
    sort: (record.sort || 0) + 1,
    tags: [...(record.tags || [])],
    variables: (record.variables || []).map(v => ({ ...v, id: undefined, templateId: undefined }))
  })
  modalVisible.value = true
}

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    code: '',
    name: '',
    description: '',
    type: '',
    systemPrompt: '',
    userPromptTemplate: '',
    responseFormat: '',
    enabled: true,
    model: '',
    sort: 0,
    tags: [],
    variables: []
  })
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    if (isEditing.value) {
      await promptApi.update(form.id!, form)
      message.success(t('common.success'))
    } else {
      await promptApi.create(form)
      message.success(t('common.create') + ' ' + t('common.success'))
    }
    modalVisible.value = false
    loadTemplates()
  } catch (err: any) {
    message.error(err.message || t('common.error'))
  } finally {
    saving.value = false
  }
}

const deleteTemplate = (id: number) => {
  Modal.confirm({
    title: t('common.confirm') + ' ' + t('common.delete'),
    content: t('prompt.confirmDeletePrompt'),
    onOk: async () => {
      try {
        await promptApi.delete(id)
        message.success(t('common.delete') + ' ' + t('common.success'))
        loadTemplates()
      } catch (err: any) {
        message.error(err.message || t('common.error'))
      }
    }
  })
}

const toggleEnabled = async (id: number, enabled: boolean) => {
  try {
    await promptApi.toggle(id, enabled)
    message.success(enabled ? t('prompt.enabled') : t('prompt.disabled'))
    loadTemplates()
  } catch (err: any) {
    message.error(err.message || t('common.error'))
  }
}

const rollbackVersion = async (templateId: number, version: number) => {
  try {
    await promptApi.rollback(templateId, version)
    message.success(t('prompt.rollbackToVersion') + ' ' + t('common.success'))
    versionsVisible.value = false
    loadTemplates()
  } catch (err: any) {
    message.error(err.message || t('common.error'))
  }
}

// Variable management
const addVariable = () => {
  form.variables.push({
    id: 0,
    templateId: 0,
    variableName: '',
    variableType: 'string',
    required: true,
    source: 'context',
    sort: form.variables.length
  })
}

const removeVariable = (idx: number) => {
  form.variables.splice(idx, 1)
}

// Test functions
const testTemplate = (record: PromptTemplate) => {
  currentTemplate.value = record
  testInput.value = ''
  testVariables.value = ''
  testResult.value = null
  testVisible.value = true
}

const handleTest = async () => {
  if (!testInput.value.trim()) {
    message.error(t('prompt.inputContent') + ': ' + t('common.required'))
    return
  }

  testing.value = true
  testResult.value = null

  try {
    const resp = await promptApi.testExtract({
      templateId: String(currentTemplate.value?.id),
      inputContent: testInput.value,
      customVariables: testVariables.value || undefined,
      sourceType: 'text'
    })
    testResult.value = resp
  } catch (err: any) {
    testResult.value = {
      success: false,
      errorMessage: err.message || t('prompt.failed')
    }
  } finally {
    testing.value = false
  }
}

// Sample data generation
const generateSampleData = () => {
  if (!currentTemplate.value) return
  sampleConfig.dataType = 'legal'
  sampleConfig.count = 3
  sampleConfig.scenario = ''
  generatedSamples.value = []
  selectedSampleIdx.value = 0
  sampleModalVisible.value = true
}

const doGenerateSample = async () => {
  generating.value = true
  try {
    const resp = await promptApi.generateSample({
      templateId: String(currentTemplate.value?.id),
      ...sampleConfig
    })
    if (resp.success && resp.samples) {
      generatedSamples.value = resp.samples
      message.success(t('prompt.generateTestData') + ' ' + t('common.success') + ': ' + resp.samples.length + ' samples')
    } else {
      message.error(resp.errorMessage || t('prompt.failed'))
    }
  } catch (err: any) {
    message.error(err.message || t('prompt.failed'))
  } finally {
    generating.value = false
  }
}

const handleUseSample = () => {
  if (generatedSamples.value.length > 0) {
    testInput.value = generatedSamples.value[selectedSampleIdx.value].content
    sampleModalVisible.value = false
    message.success(t('prompt.useSampleData') + ' ' + t('common.success'))
  }
}

const formatDate = (date: string | undefined): string => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

// Lifecycle
onMounted(() => {
  loadTemplateTypes()
  loadTemplates()
})
</script>

<style scoped lang="less">
@import '@/assets/styles/dark.less';

.prompt-page {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: @text-primary;
  margin-bottom: 4px;
}

.page-desc {
  color: @text-secondary;
  font-size: 14px;
}

.filter-card,
.table-card {
  background: @bg-container;
  border: 1px solid @border-color;
  margin-bottom: 16px;
}

.template-name {
  color: #000;
  font-weight: 500;
  text-shadow: none;
}

.template-code {
  font-family: 'Monaco', 'Menlo', monospace;
  font-weight: 500;
  font-size: 14px;
  padding: 2px 6px;
  border-radius: 4px;
  color: #000;
  text-shadow: none;
}

.variables-section {
  padding: 0 8px;
}

.variable-item {
  padding: 8px;
  margin-bottom: 8px;
  background: @bg-elevated;
  border-radius: 4px;
}

.prompt-content {
  max-height: 200px;
  overflow: auto;
  padding: 8px;
  background: @bg-elevated;
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.version-item {
  padding: 8px 0;
  .version-header {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .version-desc {
    color: @text-secondary;
    font-size: 13px;
    margin: 4px 0;
  }
  .version-time {
    color: @text-tertiary;
    font-size: 12px;
  }
}

.test-result {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid @border-color;
  border-radius: 8px;
  overflow: hidden;

  .result-header {
    padding: 12px 16px;
    background: @bg-elevated;
    border-bottom: 1px solid @border-color;
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: 500;

    .elapsed {
      color: @text-tertiary;
      font-size: 12px;
    }
  }

  .result-content {
    flex: 1;
    padding: 16px;
    overflow: auto;
  }
}

.raw-response,
.parsed-data {
  margin-top: 16px;
  .section-title {
    font-weight: 500;
    margin-bottom: 8px;
    color: @text-primary;
  }
  pre {
    max-height: 300px;
    overflow: auto;
    padding: 12px;
    background: @bg-elevated;
    border-radius: 4px;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

.stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  span {
    padding: 4px 12px;
    background: @bg-elevated;
    border-radius: 4px;
    font-size: 13px;
  }
}

.sample-list {
  max-height: 300px;
  overflow: auto;
}

.sample-item {
  padding: 12px;
  margin-bottom: 8px;
  border: 1px solid @border-color;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: @primary-color;
  }

  &.selected {
    border-color: @primary-color;
    background: fade(@primary-color, 10%);
  }

  .sample-index {
    font-weight: 500;
    margin-bottom: 4px;
  }

  .sample-content {
    font-size: 12px;
    color: @text-secondary;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
