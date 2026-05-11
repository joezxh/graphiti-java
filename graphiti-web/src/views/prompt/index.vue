<template>
  <div class="prompt-page">
    <!-- Page Header -->
    <div class="page-header">
      <h1 class="page-title">提示词管理</h1>
      <p class="page-desc">管理实体抽取、关系抽取等提示词模板，支持版本管理和在线测试</p>
    </div>

    <!-- Filter Bar -->
    <a-card class="filter-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-select
            v-model:value="filterType"
            placeholder="模板类型"
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
            placeholder="搜索模板名称/编码"
            style="width: 100%"
            @search="loadTemplates"
          />
        </a-col>
        <a-col :span="12">
          <a-space>
            <a-button type="primary" @click="showCreateModal">
              <PlusOutlined /> 新建模板
            </a-button>
            <a-button @click="loadTemplates">
              <ReloadOutlined /> 刷新
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
              <a-button type="link" size="small" @click="editTemplate(record)">编辑</a-button>
              <a-button type="link" size="small" @click="testTemplate(record)">测试</a-button>
              <a-dropdown>
                <a-button type="link" size="small">
                  <MoreOutlined />
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="view" @click="viewDetail(record)">查看详情</a-menu-item>
                    <a-menu-item key="versions" @click="viewVersions(record)">版本历史</a-menu-item>
                    <a-menu-item key="copy" @click="copyTemplate(record)">复制</a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="delete" @click="deleteTemplate(record.id)">
                      <span style="color: #ff4d4f">删除</span>
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
      :title="isEditing ? '编辑提示词模板' : '新建提示词模板'"
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
        <a-form-item label="模板编码" name="code">
          <a-input
            v-model:value="form.code"
            placeholder="如：LEGAL_ENTITY_EXTRACT"
            :disabled="isEditing"
          />
        </a-form-item>
        <a-form-item label="模板名称" name="name">
          <a-input v-model:value="form.name" placeholder="如：法律案件实体提取" />
        </a-form-item>
        <a-form-item label="模板描述" name="description">
          <a-textarea v-model:value="form.description" :rows="2" placeholder="简要描述模板用途" />
        </a-form-item>
        <a-form-item label="模板类型" name="type">
          <a-select v-model:value="form.type" placeholder="选择模板类型">
            <a-select-option v-for="t in templateTypes" :key="t.value" :value="t.value">
              {{ t.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="所属模型" name="model">
          <a-input v-model:value="form.model" placeholder="如：gpt-4o-mini（留空使用默认）" />
        </a-form-item>

        <a-divider>提示词内容</a-divider>

        <a-form-item label="系统提示词" name="systemPrompt">
          <a-textarea
            v-model:value="form.systemPrompt"
            :rows="6"
            placeholder="输入系统提示词内容，支持变量占位符 {variable}"
          />
        </a-form-item>
        <a-form-item label="用户提示词" name="userPromptTemplate">
          <a-textarea
            v-model:value="form.userPromptTemplate"
            :rows="6"
            placeholder="输入用户提示词模板，使用 {episode_content}, {nodes} 等变量"
          />
        </a-form-item>
        <a-form-item label="响应格式" name="responseFormat">
          <a-textarea
            v-model:value="form.responseFormat"
            :rows="4"
            placeholder='JSON Schema 格式，如：{"type":"object","properties":{"entities":...}}'
          />
        </a-form-item>

        <a-divider>变量配置</a-divider>

        <div class="variables-section">
          <a-button type="dashed" block @click="addVariable" style="margin-bottom: 12px">
            <PlusOutlined /> 添加变量
          </a-button>
          <div v-for="(v, idx) in form.variables" :key="idx" class="variable-item">
            <a-row :gutter="8" align="middle">
              <a-col :span="3">
                <a-input v-model:value="v.variableName" placeholder="变量名" />
              </a-col>
              <a-col :span="3">
                <a-select v-model:value="v.variableType" style="width: 100%">
                  <a-select-option value="string">字符串</a-select-option>
                  <a-select-option value="text">长文本</a-select-option>
                  <a-select-option value="list">列表</a-select-option>
                  <a-select-option value="json">JSON</a-select-option>
                </a-select>
              </a-col>
              <a-col :span="4">
                <a-input v-model:value="v.description" placeholder="描述" />
              </a-col>
              <a-col :span="3">
                <a-select v-model:value="v.source" style="width: 100%">
                  <a-select-option value="context">上下文</a-select-option>
                  <a-select-option value="static">静态值</a-select-option>
                  <a-select-option value="llm">动态生成</a-select-option>
                </a-select>
              </a-col>
              <a-col :span="4">
                <a-input v-model:value="v.defaultValue" placeholder="默认值" />
              </a-col>
              <a-col :span="2">
                <a-checkbox v-model:checked="v.required">必需</a-checkbox>
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
      title="模板详情"
      :footer="null"
      width="700px"
    >
      <a-descriptions :column="1" bordered v-if="currentTemplate">
        <a-descriptions-item label="模板编码">{{ currentTemplate.code }}</a-descriptions-item>
        <a-descriptions-item label="模板名称">{{ currentTemplate.name }}</a-descriptions-item>
        <a-descriptions-item label="模板描述">{{ currentTemplate.description || '-' }}</a-descriptions-item>
        <a-descriptions-item label="模板类型">{{ getTypeLabel(currentTemplate.type) }}</a-descriptions-item>
        <a-descriptions-item label="所属模型">{{ currentTemplate.model || '默认' }}</a-descriptions-item>
        <a-descriptions-item label="启用状态">
          <a-tag :color="currentTemplate.enabled ? 'green' : 'red'">
            {{ currentTemplate.enabled ? '已启用' : '已禁用' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="系统提示词">
          <pre class="prompt-content">{{ currentTemplate.systemPrompt }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="用户提示词">
          <pre class="prompt-content">{{ currentTemplate.userPromptTemplate }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="响应格式">
          <pre class="prompt-content">{{ currentTemplate.responseFormat || '-' }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>

    <!-- Version History Modal -->
    <a-modal
      v-model:open="versionsVisible"
      title="版本历史"
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
              <strong>版本 {{ v.version }}</strong>
              <a-tag v-if="v.active" color="green">当前版本</a-tag>
            </div>
            <div class="version-desc">{{ v.description || '无描述' }}</div>
            <div class="version-time">{{ formatDate(v.createdAt) }}</div>
            <a-button
              v-if="!v.active"
              type="link"
              size="small"
              @click="rollbackVersion(v.templateId, v.version)"
            >
              回滚到此版本
            </a-button>
          </div>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-else description="暂无版本历史" />
    </a-modal>

    <!-- Test Modal -->
    <a-modal
      v-model:open="testVisible"
      title="测试提示词"
      @ok="handleTest"
      :confirm-loading="testing"
      width="1000px"
      :maskClosable="false"
    >
      <a-row :gutter="16">
        <!-- Left: Test Config & Input -->
        <a-col :span="12">
          <a-form layout="vertical">
            <a-form-item label="测试模板">
              <a-input :value="currentTemplate?.name" disabled />
            </a-form-item>
            <a-form-item label="输入内容">
              <a-textarea
                v-model:value="testInput"
                :rows="10"
                placeholder="输入待测试的文本内容或JSON数据"
              />
            </a-form-item>
            <a-form-item label="额外变量 (JSON格式)">
              <a-textarea
                v-model:value="testVariables"
                :rows="3"
                placeholder='{"extras": "", "entity_types": ""}'
              />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleTest" :loading="testing">
                  执行测试
                </a-button>
                <a-button @click="generateSampleData" :loading="generating">
                  生成测试数据
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-col>

        <!-- Right: Test Result -->
        <a-col :span="12">
          <div class="test-result">
            <div class="result-header">
              <span>测试结果</span>
              <a-space v-if="testResult">
                <a-tag v-if="testResult.success" color="green">成功</a-tag>
                <a-tag v-else color="red">失败</a-tag>
                <span class="elapsed">{{ testResult.elapsedMs }}ms</span>
              </a-space>
            </div>
            <div class="result-content">
              <a-spin :spinning="testing">
                <template v-if="testResult">
                  <div v-if="testResult.success" class="result-success">
                    <div v-if="testResult.entityCount !== undefined || testResult.edgeCount !== undefined" class="stats">
                      <span>实体: {{ testResult.entityCount || 0 }}</span>
                      <span>关系: {{ testResult.edgeCount || 0 }}</span>
                    </div>
                    <div v-if="testResult.rawResponse" class="raw-response">
                      <div class="section-title">原始响应:</div>
                      <pre>{{ testResult.rawResponse }}</pre>
                    </div>
                    <div v-if="testResult.parsedData" class="parsed-data">
                      <div class="section-title">解析结果:</div>
                      <pre>{{ testResult.parsedData }}</pre>
                    </div>
                  </div>
                  <div v-else class="result-error">
                    <a-alert type="error" :message="testResult.errorMessage" />
                  </div>
                </template>
                  <a-empty v-else description='点击"执行测试"开始' />
              </a-spin>
            </div>
          </div>
        </a-col>
      </a-row>
    </a-modal>

    <!-- Generate Sample Modal -->
    <a-modal
      v-model:open="sampleModalVisible"
      title="生成测试数据"
      @ok="handleUseSample"
      width="600px"
    >
      <a-form layout="vertical">
        <a-form-item label="数据类型">
          <a-select v-model:value="sampleConfig.dataType">
            <a-select-option value="legal">法律案件</a-select-option>
            <a-select-option value="medical">医疗记录</a-select-option>
            <a-select-option value="financial">金融资讯</a-select-option>
            <a-select-option value="general">通用文本</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="生成数量">
          <a-input-number v-model:value="sampleConfig.count" :min="1" :max="10" />
        </a-form-item>
        <a-form-item label="具体场景">
          <a-input v-model:value="sampleConfig.scenario" placeholder="如：民事合同纠纷案件" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="doGenerateSample" :loading="generating">
            生成
          </a-button>
        </a-form-item>
      </a-form>

      <a-divider>生成的样本</a-divider>

      <div v-if="generatedSamples.length > 0" class="sample-list">
        <div
          v-for="(sample, idx) in generatedSamples"
          :key="idx"
          class="sample-item"
          :class="{ selected: selectedSampleIdx === idx }"
          @click="selectedSampleIdx = idx"
        >
          <div class="sample-index">样本 {{ idx + 1 }}</div>
          <div class="sample-content">{{ sample.content?.substring(0, 200) }}...</div>
        </div>
      </div>
      <a-empty v-else description="点击生成创建样本数据" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  DeleteOutlined,
  MoreOutlined
} from '@ant-design/icons-vue'
import { promptApi, type PromptTemplate, type PromptVariable, type PromptVersion, type PromptTestResp, type SampleData } from '@/api/prompt'

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
  code: [{ required: true, message: '请输入模板编码' }],
  name: [{ required: true, message: '请输入模板名称' }],
  type: [{ required: true, message: '请选择模板类型' }],
  systemPrompt: [{ required: true, message: '请输入系统提示词' }],
  userPromptTemplate: [{ required: true, message: '请输入用户提示词模板' }]
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
  { title: '模板名称', key: 'name', width: 250 },
  { title: '模板编码', key: 'code', width: 200 },
  { title: '类型', key: 'type', width: 120 },
  { title: '模型', dataIndex: 'model', width: 120 },
  { title: '启用', key: 'enabled', width: 80 },
  { title: '更新时间', dataIndex: 'updatedAt', width: 160 },
  { title: '操作', key: 'action', width: 180 }
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
    message.error(err.message || '加载失败')
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
      { value: 'entity_extract', label: '实体抽取' },
      { value: 'edge_extract', label: '关系抽取' },
      { value: 'dedupe', label: '去重处理' },
      { value: 'summary', label: '摘要生成' }
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
    message.error(err.message || '加载版本历史失败')
  }
}

const copyTemplate = (record: PromptTemplate) => {
  isEditing.value = false
  Object.assign(form, {
    code: record.code + '_copy',
    name: record.name + ' (复制)',
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
      message.success('更新成功')
    } else {
      await promptApi.create(form)
      message.success('创建成功')
    }
    modalVisible.value = false
    loadTemplates()
  } catch (err: any) {
    message.error(err.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const deleteTemplate = (id: number) => {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该提示词模板吗？此操作不可恢复。',
    onOk: async () => {
      try {
        await promptApi.delete(id)
        message.success('删除成功')
        loadTemplates()
      } catch (err: any) {
        message.error(err.message || '删除失败')
      }
    }
  })
}

const toggleEnabled = async (id: number, enabled: boolean) => {
  try {
    await promptApi.toggle(id, enabled)
    message.success(enabled ? '已启用' : '已禁用')
    loadTemplates()
  } catch (err: any) {
    message.error(err.message || '操作失败')
  }
}

const rollbackVersion = async (templateId: number, version: number) => {
  try {
    await promptApi.rollback(templateId, version)
    message.success('回滚成功')
    versionsVisible.value = false
    loadTemplates()
  } catch (err: any) {
    message.error(err.message || '回滚失败')
  }
}

// Variable management
const addVariable = () => {
  form.variables.push({
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
    message.error('请输入测试内容')
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
      errorMessage: err.message || '测试失败'
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
      message.success(`生成 ${resp.samples.length} 条样本数据`)
    } else {
      message.error(resp.errorMessage || '生成失败')
    }
  } catch (err: any) {
    message.error(err.message || '生成失败')
  } finally {
    generating.value = false
  }
}

const handleUseSample = () => {
  if (generatedSamples.value.length > 0) {
    testInput.value = generatedSamples.value[selectedSampleIdx.value].content
    sampleModalVisible.value = false
    message.success('已使用选中的样本数据')
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
