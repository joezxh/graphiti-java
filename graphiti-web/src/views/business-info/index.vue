<template>
  <div class="business-info-page">
    <div class="scan-line"></div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="title-area">
        <h1 class="page-title gradient-text">{{ $t('businessInfo.title') }}</h1>
        <p class="page-desc">{{ $t('businessInfo.titleDesc') }}</p>
      </div>
      <div class="header-right">
        <a-select
          v-model:value="selectedGraphId"
          :placeholder="$t('businessInfo.selectGraph')"
          style="width: 220px"
          @change="loadDrafts"
        >
          <a-select-option v-for="g in graphList" :key="g.graphId" :value="g.graphId">
            {{ g.name }}
          </a-select-option>
        </a-select>
        <a-button type="primary" @click="goToOntologyViewer">
          <ApiOutlined /> {{ $t('businessInfo.ontologyViewer') }}
        </a-button>
      </div>
    </div>

    <!-- Tab 导航 -->
    <a-tabs v-model:activeKey="activeTab" class="main-tabs glass-card">
      <!-- Tab 1: 本体生成 -->
      <a-tab-pane key="generate" :tab="$t('businessInfo.tabOntologyGenerate')">
        <div class="tab-content">
          <div class="generate-layout">
            <!-- 左：输入区 -->
            <div class="input-panel glass-card">
              <div class="panel-title">{{ $t('businessInfo.businessInfoInput') }}</div>
              <a-form :model="generateForm" layout="vertical" class="generate-form">
                <a-form-item :label="$t('businessInfo.draftName')">
                  <a-input v-model:value="generateForm.draftName" :placeholder="$t('businessInfo.draftNamePlaceholder')" />
                </a-form-item>
                <a-form-item :label="$t('businessInfo.businessScenario')">
                  <a-textarea
                    v-model:value="generateForm.businessScenario"
                    :rows="4"
                    :placeholder="$t('businessInfo.businessScenarioPlaceholder')"
                  />
                </a-form-item>
                <a-form-item :label="$t('businessInfo.domainHint')">
                  <a-select v-model:value="generateForm.domainHint" :placeholder="$t('businessInfo.selectDomain')">
                    <a-select-option value="GOVERNMENT">{{ $t('businessInfo.government') }}</a-select-option>
                    <a-select-option value="FINANCIAL">{{ $t('businessInfo.financial') }}</a-select-option>
                    <a-select-option value="MEDICAL">{{ $t('businessInfo.medical') }}</a-select-option>
                    <a-select-option value="ECOMMERCE">{{ $t('businessInfo.ecommerce') }}</a-select-option>
                    <a-select-option value="LEGAL">{{ $t('businessInfo.legal') }}</a-select-option>
                    <a-select-option value="KNOWLEDGE">{{ $t('businessInfo.knowledge') }}</a-select-option>
                    <a-select-option value="GENERAL">{{ $t('businessInfo.general') }}</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item :label="$t('businessInfo.userInput')">
                  <a-textarea
                    v-model:value="generateForm.userInput"
                    :rows="5"
                    :placeholder="$t('businessInfo.userInputPlaceholder')"
                  />
                </a-form-item>
                <a-form-item>
                  <a-space>
                    <a-button type="primary" :loading="generating" @click="handleGenerate">
                      <RobotOutlined /> {{ $t('businessInfo.generate') }}
                    </a-button>
                    <a-button @click="handleSaveDraft">
                      <SaveOutlined /> {{ $t('common.save') }}
                    </a-button>
                    <a-button @click="resetGenerateForm">
                      <ClearOutlined /> {{ $t('common.reset') }}
                    </a-button>
                  </a-space>
                </a-form-item>
              </a-form>
            </div>

            <!-- 右：预览区 -->
            <div class="preview-panel glass-card">
              <div class="panel-title">{{ $t('businessInfo.ontologyPreview') }}</div>
              <a-spin :spinning="generating" tip="LLM 正在生成...">
                <div v-if="generatedResult" class="preview-content">
                  <!-- 本体定义概览 -->
                  <div class="result-section">
                    <div class="section-title">{{ $t('businessInfo.ontologyDefinition') }}</div>
                    <a-descriptions :column="2" bordered size="small">
                      <a-descriptions-item :label="$t('businessInfo.name')">{{ generatedResult.definition?.name }}</a-descriptions-item>
                      <a-descriptions-item :label="$t('businessInfo.version')">{{ generatedResult.definition?.version }}</a-descriptions-item>
                      <a-descriptions-item :label="$t('businessInfo.namespace')" :span="2">{{ generatedResult.definition?.namespace }}</a-descriptions-item>
                    </a-descriptions>
                  </div>

                  <!-- 类列表 -->
                  <div v-if="generatedResult.classes?.length" class="result-section">
                    <div class="section-title">
                      {{ $t('businessInfo.classes') }} ({{ generatedResult.classes.length }})
                    </div>
                    <div class="class-list">
                      <div v-for="cls in generatedResult.classes" :key="cls.localName" class="class-item">
                        <span class="class-icon" style="color: #00f0ff">◈</span>
                        <span class="class-name">{{ cls.localName }}</span>
                        <a-tag size="small">{{ cls.domainHint }}</a-tag>
                      </div>
                    </div>
                  </div>

                  <!-- 属性列表 -->
                  <div v-if="generatedResult.properties?.length" class="result-section">
                    <div class="section-title">
                      {{ $t('businessInfo.properties') }} ({{ generatedResult.properties.length }})
                    </div>
                    <a-table
                      :columns="propertyPreviewColumns"
                      :data-source="generatedResult.properties"
                      :pagination="false"
                      size="small"
                      row-key="localName"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'propertyType'">
                          <a-tag :color="record.propertyType === 'DATATYPE' ? 'blue' : 'green'">{{ record.propertyType }}</a-tag>
                        </template>
                      </template>
                    </a-table>
                  </div>

                  <!-- 操作按钮 -->
                  <div class="result-actions">
                    <a-button type="primary" @click="showApplyModal = true">
                      <CloudUploadOutlined /> {{ $t('businessInfo.applyToOntology') }}
                    </a-button>
                  </div>
                </div>
                <a-empty v-else :description="$t('businessInfo.generateHint')" />
              </a-spin>
            </div>
          </div>

          <!-- 草稿列表 -->
          <div class="drafts-section glass-card">
            <div class="section-header">
              <span class="section-title">{{ $t('businessInfo.draftList') }}</span>
            </div>
            <a-table
              :columns="draftColumns"
              :data-source="drafts"
              :loading="draftsLoading"
              :pagination="{ pageSize: 5 }"
              row-key="id"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'status'">
                  <a-tag :color="getStatusColor(record.status)">{{ record.status }}</a-tag>
                </template>
                <template v-if="column.key === 'draftType'">
                  <a-tag>{{ record.draftType }}</a-tag>
                </template>
                <template v-if="column.key === 'action'">
                  <a-space>
                    <a-button type="link" size="small" @click="previewDraft(record)">{{ $t('common.view') }}</a-button>
                    <a-button type="link" size="small" @click="applyDraft(record)">{{ $t('businessInfo.apply') }}</a-button>
                    <a-button type="link" size="small" danger @click="removeDraft(record)">{{ $t('common.delete') }}</a-button>
                  </a-space>
                </template>
              </template>
            </a-table>
          </div>
        </div>
      </a-tab-pane>

      <!-- Tab 2: 描述优化 -->
      <a-tab-pane key="optimize" :tab="$t('businessInfo.tabOptimize')">
        <div class="tab-content optimize-content">
          <div class="optimize-panel glass-card">
            <div class="panel-title">{{ $t('businessInfo.optimizeDescription') }}</div>
            <a-form :model="optimizeForm" layout="vertical">
              <a-form-item :label="$t('businessInfo.originalDescription')">
                <a-textarea
                  v-model:value="optimizeForm.originalDescription"
                  :rows="4"
                  :placeholder="$t('businessInfo.originalDescPlaceholder')"
                />
              </a-form-item>
              <a-form-item :label="$t('businessInfo.context')">
                <a-input v-model:value="optimizeForm.context" :placeholder="$t('businessInfo.contextPlaceholder')" />
              </a-form-item>
              <a-form-item>
                <a-space>
                  <a-button type="primary" :loading="optimizing" @click="handleOptimize">
                    <RobotOutlined /> {{ $t('businessInfo.optimize') }}
                  </a-button>
                </a-space>
              </a-form-item>
            </a-form>

            <!-- 优化结果 -->
            <div v-if="optimizeResult" class="optimize-result">
              <div class="result-title">{{ $t('businessInfo.optimizeResults') }}</div>
              <div v-for="opt in optimizeResult.optimizations" :key="opt.version" class="optimization-option glass-panel">
                <div class="opt-header">
                  <span class="opt-version glow-text-cyan">{{ opt.version }}</span>
                  <a-button type="link" size="small" @click="adoptOptimization(opt)">
                    {{ $t('businessInfo.adopt') }}
                  </a-button>
                </div>
                <div class="opt-content">{{ opt.description }}</div>
                <div class="opt-highlights">
                  <a-tag v-for="h in opt.highlights" :key="h" color="cyan" size="small">{{ h }}</a-tag>
                </div>
              </div>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <!-- Tab 3: 数据模拟生成 -->
      <a-tab-pane key="mockdata" :tab="$t('businessInfo.tabMockData')">
        <div class="tab-content">
          <div class="generate-layout">
            <div class="input-panel glass-card">
              <div class="panel-title">{{ $t('businessInfo.generateMockData') }}</div>
              <a-form :model="mockDataForm" layout="vertical">
                <a-form-item :label="$t('businessInfo.selectSource')">
                  <a-select v-model:value="mockDataForm.source">
                    <a-select-option value="current">{{ $t('businessInfo.fromCurrentOntology') }}</a-select-option>
                    <a-select-option value="draft">{{ $t('businessInfo.fromDraft') }}</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item v-if="mockDataForm.source === 'draft'" :label="$t('businessInfo.selectDraft')">
                  <a-select v-model:value="mockDataForm.draftId">
                    <a-select-option v-for="d in drafts" :key="d.id" :value="d.id">
                      {{ d.draftName }}
                    </a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item :label="$t('businessInfo.dataCount')">
                  <a-input-number v-model:value="mockDataForm.count" :min="5" :max="1000" style="width: 100%" />
                </a-form-item>
                <a-form-item :label="$t('businessInfo.dataFormat')">
                  <a-select v-model:value="mockDataForm.format">
                    <a-select-option value="JSON">JSON</a-select-option>
                    <a-select-option value="CSV">CSV</a-select-option>
                    <a-select-option value="N-TRIPLES">N-Triples</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item>
                  <a-space>
                    <a-button type="primary" :loading="mockGenerating" @click="handleGenerateMockData">
                      <RobotOutlined /> {{ $t('businessInfo.generate') }}
                    </a-button>
                  </a-space>
                </a-form-item>
              </a-form>
            </div>

            <div class="preview-panel glass-card">
              <div class="panel-title">{{ $t('businessInfo.mockDataPreview') }}</div>
              <a-spin :spinning="mockGenerating" tip="正在生成模拟数据...">
                <div v-if="mockDataResult" class="mock-data-content">
                  <div class="mock-stats">
                    <div class="mock-stat-item">
                      <span class="stat-value" style="color: #00ffcc">{{ mockDataResult.stats?.totalEntities }}</span>
                      <span class="stat-label">{{ $t('businessInfo.entities') }}</span>
                    </div>
                    <div class="mock-stat-item">
                      <span class="stat-value" style="color: #ffe066">{{ mockDataResult.stats?.totalRelationships }}</span>
                      <span class="stat-label">{{ $t('businessInfo.relationships') }}</span>
                    </div>
                  </div>
                  <a-table
                    :columns="mockDataColumns"
                    :data-source="mockDataResult.entities?.slice(0, 10)"
                    :pagination="false"
                    size="small"
                    row-key="id"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'type'">
                        <a-tag color="cyan">{{ record.type }}</a-tag>
                      </template>
                    </template>
                  </a-table>
                  <div class="result-actions">
                    <a-button type="primary" @click="goToMockDataViewer">
                      <ApiOutlined /> {{ $t('businessInfo.viewInGraph') }}
                    </a-button>
                    <a-button @click="downloadMockData">
                      <DownloadOutlined /> {{ $t('common.export') }}
                    </a-button>
                  </div>
                </div>
                <a-empty v-else :description="$t('businessInfo.generateMockDataHint')" />
              </a-spin>
            </div>
          </div>
        </div>
      </a-tab-pane>
    </a-tabs>

    <!-- 应用确认弹窗 -->
    <a-modal
      v-model:open="showApplyModal"
      :title="$t('businessInfo.confirmApply')"
      @ok="handleApply"
      :confirm-loading="applying"
    >
      <a-alert type="info" show-icon>
        <template #message>{{ $t('businessInfo.applyWarningTitle') }}</template>
        <template #description>{{ $t('businessInfo.applyWarningDesc') }}</template>
      </a-alert>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  ApiOutlined, RobotOutlined, SaveOutlined, ClearOutlined,
  CloudUploadOutlined, DownloadOutlined
} from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { graphApi } from '@/api/graph'
import {
  businessInfoApi,
  type GenerateOntologyReqVO,
  type GenerateOntologyRespVO,
  type OptimizeDescRespVO,
  type OptimizationVO,
  type GenerateDataRespVO,
  type OntDraftVO
} from '@/api/business-info'

const { t } = useI18n()
const router = useRouter()

// State
const selectedGraphId = ref('')
const graphList = ref<any[]>([])
const activeTab = ref('generate')
const generating = ref(false)
const optimizing = ref(false)
const mockGenerating = ref(false)
const draftsLoading = ref(false)
const applying = ref(false)
const showApplyModal = ref(false)

const generateForm = reactive<GenerateOntologyReqVO>({
  draftName: '',
  businessScenario: '',
  domainHint: 'GOVERNMENT',
  userInput: '',
  saveAsDraft: false,
})

const generatedResult = ref<GenerateOntologyRespVO | null>(null)

const optimizeForm = reactive({
  originalDescription: '',
  context: '',
})
const optimizeResult = ref<OptimizeDescRespVO | null>(null)

const mockDataForm = reactive({
  source: 'current',
  draftId: undefined as number | undefined,
  count: 20,
  format: 'JSON',
})
const mockDataResult = ref<GenerateDataRespVO | null>(null)

const drafts = ref<OntDraftVO[]>([])

const propertyPreviewColumns = [
  { title: '属性名', dataIndex: 'localName', key: 'localName' },
  { title: '类型', key: 'propertyType' },
  { title: '域', dataIndex: 'domainClass', key: 'domainClass' },
  { title: '值域', dataIndex: 'rangeClass', key: 'rangeClass' },
]

const mockDataColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id' },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '类型', key: 'type' },
]

const draftColumns = [
  { title: '名称', dataIndex: 'draftName', key: 'draftName' },
  { title: '类型', key: 'draftType' },
  { title: '状态', key: 'status' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt' },
  { title: '操作', key: 'action', width: 180 },
]

// Methods
async function loadGraphs() {
  try {
    graphList.value = await graphApi.getList() as any[] || []
    if (graphList.value.length > 0) {
      selectedGraphId.value = graphList.value[0].graphId
    }
    await loadDrafts()
  } catch (e) {
    console.error('加载图谱列表失败', e)
  }
}

async function loadDrafts() {
  if (!selectedGraphId.value) return
  draftsLoading.value = true
  try {
    drafts.value = await businessInfoApi.listDrafts(selectedGraphId.value) as any[] || []
  } catch (e) {
    console.error('加载草稿列表失败', e)
  } finally {
    draftsLoading.value = false
  }
}

async function handleGenerate() {
  if (!selectedGraphId.value) {
    message.warning(t('businessInfo.selectGraphFirst'))
    return
  }
  if (!generateForm.userInput.trim()) {
    message.warning(t('businessInfo.enterUserInput'))
    return
  }
  generating.value = true
  try {
    const resp = await businessInfoApi.generateOntology(selectedGraphId.value, generateForm)
    generatedResult.value = resp as any
    message.success(t('businessInfo.generateSuccess'))
  } catch (e: any) {
    message.error(e?.message || t('businessInfo.generateFailed'))
  } finally {
    generating.value = false
  }
}

async function handleSaveDraft() {
  if (!selectedGraphId.value) {
    message.warning(t('businessInfo.selectGraphFirst'))
    return
  }
  if (!generateForm.userInput.trim()) {
    message.warning(t('businessInfo.enterUserInput'))
    return
  }
  try {
    await businessInfoApi.saveDraft(selectedGraphId.value, { ...generateForm, saveAsDraft: true })
    message.success(t('common.saveSuccess'))
    await loadDrafts()
  } catch (e: any) {
    message.error(e?.message || t('common.saveFailed'))
  }
}

function resetGenerateForm() {
  generateForm.draftName = ''
  generateForm.businessScenario = ''
  generateForm.userInput = ''
  generatedResult.value = null
}

async function handleOptimize() {
  if (!optimizeForm.originalDescription.trim()) {
    message.warning(t('businessInfo.enterOriginalDesc'))
    return
  }
  optimizing.value = true
  try {
    const resp = await businessInfoApi.optimizeDescription({
      originalDescription: optimizeForm.originalDescription,
      context: optimizeForm.context,
      language: 'zh',
    })
    optimizeResult.value = resp as any
  } catch (e: any) {
    message.error(e?.message || t('businessInfo.optimizeFailed'))
  } finally {
    optimizing.value = false
  }
}

function adoptOptimization(opt: OptimizationVO) {
  optimizeForm.originalDescription = opt.description
  message.success(t('businessInfo.adopted'))
}

async function handleGenerateMockData() {
  if (!selectedGraphId.value) {
    message.warning(t('businessInfo.selectGraphFirst'))
    return
  }
  mockGenerating.value = true
  try {
    let resp
    if (mockDataForm.source === 'draft' && mockDataForm.draftId) {
      resp = await businessInfoApi.generateMockDataFromDraft(selectedGraphId.value, mockDataForm.draftId, {
        count: mockDataForm.count,
        format: mockDataForm.format as any,
      })
    } else {
      resp = await businessInfoApi.generateMockData(selectedGraphId.value, {
        count: mockDataForm.count,
        format: mockDataForm.format as any,
      })
    }
    mockDataResult.value = resp as any
    message.success(t('businessInfo.generateSuccess'))
  } catch (e: any) {
    message.error(e?.message || t('businessInfo.generateFailed'))
  } finally {
    mockGenerating.value = false
  }
}

function downloadMockData() {
  if (!mockDataResult.value?.formattedData) return
  const blob = new Blob([mockDataResult.value.formattedData], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `mock-data.${mockDataForm.format.toLowerCase()}`
  a.click()
  URL.revokeObjectURL(url)
}

function previewDraft(draft: OntDraftVO) {
  if (draft.draftType === 'GENERATED') {
    businessInfoApi.getDraftContent(selectedGraphId.value, draft.id).then((resp: any) => {
      generatedResult.value = resp
      activeTab.value = 'generate'
    })
  }
}

async function applyDraft(draft: OntDraftVO) {
  try {
    await businessInfoApi.applyDraft(selectedGraphId.value, draft.id)
    message.success(t('businessInfo.applySuccess'))
    await loadDrafts()
  } catch (e: any) {
    message.error(e?.message || t('businessInfo.applyFailed'))
  }
}

function removeDraft(draft: OntDraftVO) {
  Modal.confirm({
    title: t('common.confirm'),
    content: `${t('common.delete')} "${draft.draftName}"?`,
    okText: t('common.confirm'),
    okType: 'danger',
    async onOk() {
      try {
        await businessInfoApi.deleteDraft(selectedGraphId.value, draft.id)
        message.success(t('common.deleteSuccess'))
        await loadDrafts()
      } catch (e: any) {
        message.error(e?.message || t('common.deleteFailed'))
      }
    },
  })
}

async function handleApply() {
  if (!generatedResult.value?.draftId) {
    message.warning(t('businessInfo.pleaseGenerateFirst'))
    return
  }
  applying.value = true
  try {
    await businessInfoApi.applyDraft(selectedGraphId.value, generatedResult.value.draftId)
    message.success(t('businessInfo.applySuccess'))
    showApplyModal.value = false
    await loadDrafts()
  } catch (e: any) {
    message.error(e?.message || t('businessInfo.applyFailed'))
  } finally {
    applying.value = false
  }
}

function goToOntologyViewer() {
  router.push(`/business-info/ontology/${selectedGraphId.value}`)
}

function goToMockDataViewer() {
  router.push(`/business-info/mock-data/${selectedGraphId.value}`)
}

function getStatusColor(status: string): string {
  const colors: Record<string, string> = {
    PENDING: 'orange',
    APPROVED: 'green',
    REJECTED: 'red',
    APPLIED: 'blue',
  }
  return colors[status] || 'default'
}

onMounted(() => {
  loadGraphs()
})
</script>

<style scoped lang="less">
@import '@/assets/styles/scifi-variables.less';
@import '@/assets/styles/scifi-glass.less';
@import '@/assets/styles/scifi-animation.less';

.business-info-page {
  padding: 24px;
  min-height: 100vh;
  background: @bg-deepest;
  position: relative;
}

.scan-line {
  position: fixed;
  top: 0; left: 0; right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(0, 240, 255, 0.12), transparent);
  animation: scanLine 8s linear infinite;
  pointer-events: none;
  z-index: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.title-area {
  .page-title {
    font-size: 24px;
    font-weight: 700;
    margin: 0 0 4px 0;
    background: linear-gradient(90deg, @neon-cyan, @neon-purple, @neon-cyan);
    background-size: 200% 100%;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    animation: gradientText 3s ease infinite;
  }
  .page-desc { font-size: 13px; color: @text-secondary; margin: 0; }
}

.header-right { display: flex; gap: 12px; align-items: center; }

.main-tabs {
  position: relative;
  z-index: 1;

  :deep(.ant-tabs-tab) {
    color: rgba(255, 255, 255, 0.65);
    font-weight: 500;

    &:hover {
      color: #ffffff;
    }

    &.ant-tabs-tab-active .ant-tabs-tab-btn {
      color: #ffffff;
      font-weight: 600;
    }
  }
}

.tab-content {
  padding: 8px 0;
}

.generate-layout {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
}

.input-panel {
  flex: 1;
  padding: 20px;
}

.preview-panel {
  flex: 2;
  padding: 20px;
  min-height: 400px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: @neon-cyan;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 240, 255, 0.1);
}

.generate-form {
  :deep(.ant-form-item-label > label) {
    color: @text-secondary;
    font-size: 12px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
}

.preview-content {
  max-height: 500px;
  overflow-y: auto;
}

.result-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: @neon-purple;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.class-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.class-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 6px;
  background: rgba(0, 240, 255, 0.05);
  border: 1px solid rgba(0, 240, 255, 0.15);
  font-size: 12px;

  .class-icon { font-size: 12px; }
  .class-name { color: @text-primary; }
}

.result-actions {
  padding-top: 16px;
  border-top: 1px solid rgba(0, 240, 255, 0.1);
}

.drafts-section {
  padding: 20px;
}

.section-header {
  margin-bottom: 16px;
  .section-title {
    font-size: 14px;
    font-weight: 600;
    color: @neon-cyan;
    text-transform: uppercase;
    letter-spacing: 1px;
  }
}

// Optimize tab
.optimize-content {
  max-width: 700px;
  margin: 0 auto;
}

.optimize-result {
  margin-top: 20px;
}

.result-title {
  font-size: 14px;
  font-weight: 600;
  color: @neon-cyan;
  margin-bottom: 12px;
  text-transform: uppercase;
}

.optimization-option {
  margin-bottom: 12px;
  padding: 16px;
}

.opt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;

  .opt-version {
    font-size: 14px;
    font-weight: 700;
  }
}

.opt-content {
  font-size: 13px;
  color: @text-primary;
  line-height: 1.6;
  margin-bottom: 8px;
}

.opt-highlights {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

// Mock data tab
.mock-data-content {
  max-height: 450px;
  overflow-y: auto;
}

.mock-stats {
  display: flex;
  gap: 24px;
  margin-bottom: 16px;
}

.mock-stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .stat-value {
    font-size: 32px;
    font-weight: 700;
  }
  .stat-label {
    font-size: 12px;
    color: @text-dim;
    text-transform: uppercase;
  }
}
</style>
