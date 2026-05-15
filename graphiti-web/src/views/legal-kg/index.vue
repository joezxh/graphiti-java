<template>
  <div class="legal-kg-page">
    <div class="page-header">
      <h1 class="page-title">{{ $t('legalKg.title') }}</h1>
      <p class="page-desc">{{ $t('legalKg.titleDesc') }}</p>
    </div>

    <!-- 图谱选择 & 操作区 -->
    <a-card class="action-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="8">
          <a-select
            v-model:value="currentGraphId"
            :placeholder="$t('legalKg.selectGraph')"
            style="width: 100%"
            @change="onGraphChange"
          >
            <a-select-option v-for="g in graphList" :key="g.graphId" :value="g.graphId">
              {{ g.name }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="16">
          <a-space>
            <a-button type="primary" @click="handleImport">
              <CloudUploadOutlined /> {{ $t('legalKg.importLegalData') }}
            </a-button>
            <a-button @click="handleImportProvisions">
              <FileTextOutlined /> {{ $t('legalKg.importExampleLaw') }}
            </a-button>
            <a-button @click="handleImportCases">
              <AppstoreOutlined /> {{ $t('legalKg.importExampleCase') }}
            </a-button>
            <a-button @click="handleSetOntology">
              <SettingOutlined /> {{ $t('legalKg.legalOntology') }}
            </a-button>
            <a-button @click="loadGraphStats">
              <ReloadOutlined /> {{ $t('legalKg.refresh') }}
            </a-button>
          </a-space>
        </a-col>
      </a-row>

      <!-- 图谱统计 -->
      <a-row :gutter="16" style="margin-top: 16px">
        <a-col :span="4">
          <a-statistic :title="$t('legalKg.nodeCount')" :value="stats.nodeCount" />
        </a-col>
        <a-col :span="4">
          <a-statistic :title="$t('legalKg.edgeCount')" :value="stats.edgeCount" />
        </a-col>
        <a-col :span="4">
          <a-statistic :title="$t('legalKg.caseCount')" :value="stats.caseCount" />
        </a-col>
        <a-col :span="4">
          <a-statistic :title="$t('legalKg.provisionCount')" :value="stats.provisionCount" />
        </a-col>
        <a-col :span="4">
          <a-statistic :title="$t('legalKg.partyCount')" :value="stats.partyCount" />
        </a-col>
        <a-col :span="4">
          <a-statistic :title="$t('legalKg.courtJudgeCount')" :value="stats.courtJudgeCount" />
        </a-col>
      </a-row>
    </a-card>

    <!-- 标签页 -->
    <a-card class="content-card">
      <a-tabs v-model:activeKey="activeTab">
        <!-- 案件列表 -->
        <a-tab-pane key="cases" :tab="$t('legalKg.caseManagement')">
          <a-table
            :columns="caseColumns"
            :data-source="caseList"
            :loading="loading"
            row-key="uuid"
            :pagination="{ pageSize: 10 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'caseType'">
                <a-tag :color="getCaseTypeColor(record.properties?.caseType)">
                  {{ record.properties?.caseType || '-' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'caseStatus'">
                <a-tag :color="getCaseStatusColor(record.properties?.caseStatus)">
                  {{ record.properties?.caseStatus || '-' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'amount'">
                {{ formatAmount(record.properties?.amountInDispute) }}
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="viewNodeDetail(record)">
                    {{ $t('legalKg.details') }}
                  </a-button>
                  <a-button type="link" size="small" @click="viewNodeRelations(record)">
                    {{ $t('legalKg.relations') }}
                  </a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 法律条文 -->
        <a-tab-pane key="provisions" :tab="$t('legalKg.legalProvisions')">
          <a-table
            :columns="provisionColumns"
            :data-source="provisionList"
            :loading="loading"
            row-key="uuid"
            :pagination="{ pageSize: 10 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'articleNumber'">
                <a-tag color="purple">{{ record.properties?.articleNumber }}</a-tag>
              </template>
              <template v-else-if="column.key === 'content'">
                <div class="provision-content">{{ record.properties?.content }}</div>
              </template>
              <template v-else-if="column.key === 'keywords'">
                <a-tag v-for="kw in splitKeywords(record.properties?.keywords)" :key="kw" color="blue">
                  {{ kw }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-button type="link" size="small" @click="viewNodeDetail(record)">
                  {{ $t('legalKg.details') }}
                </a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 当事人管理 -->
        <a-tab-pane key="parties" :tab="$t('legalKg.partyManagement')">
          <a-table
            :columns="partyColumns"
            :data-source="partyList"
            :loading="loading"
            row-key="uuid"
            :pagination="{ pageSize: 10 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'partyType'">
                <a-tag :color="record.properties?.isEnterprise ? 'cyan' : 'green'">
                  {{ record.properties?.partyType }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'role'">
                <a-tag color="orange">{{ record.properties?.role }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a-button type="link" size="small" @click="viewNodeDetail(record)">{{ $t('legalKg.details') }}</a-button>
                  <a-button type="link" size="small" @click="viewNodeRelations(record)">{{ $t('legalKg.relations') }}</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 法院 & 法官 -->
        <a-tab-pane key="courts" :tab="$t('legalKg.courtsJudges')">
          <a-row :gutter="16">
            <a-col :span="12">
              <h3>{{ $t('legalKg.courts') }}</h3>
              <a-table
                :columns="courtColumns"
                :data-source="courtList"
                :loading="loading"
                row-key="uuid"
                :pagination="false"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'level'">
                    <a-tag color="geekblue">{{ record.properties?.level }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-button type="link" size="small" @click="viewNodeDetail(record)">{{ $t('legalKg.details') }}</a-button>
                  </template>
                </template>
              </a-table>
            </a-col>
            <a-col :span="12">
              <h3>{{ $t('legalKg.judges') || 'Judges' }}</h3>
              <a-table
                :columns="judgeColumns"
                :data-source="judgeList"
                :loading="loading"
                row-key="uuid"
                :pagination="false"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'title'">
                    <a-tag color="gold">{{ record.properties?.title }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'action'">
                    <a-button type="link" size="small" @click="viewNodeDetail(record)">{{ $t('legalKg.details') }}</a-button>
                  </template>
                </template>
              </a-table>
            </a-col>
          </a-row>
        </a-tab-pane>

        <!-- 关系视图 -->
        <a-tab-pane key="relations" :tab="$t('legalKg.relationList')">
          <a-row :gutter="16">
            <a-col :span="8">
              <h3>{{ $t('legalKg.relationType') }}</h3>
              <a-list
                :data-source="relationTypes"
                size="small"
                bordered
              >
                <template #renderItem="{ item }">
                  <a-list-item>
                    <a-list-item-meta :title="item.name" :description="item.count + ' '" />
                  </a-list-item>
                </template>
              </a-list>
            </a-col>
            <a-col :span="16">
              <h3>{{ $t('legalKg.relationList') }}</h3>
              <a-table
                :columns="edgeColumns"
                :data-source="edgeList"
                :loading="loading"
                row-key="uuid"
                :pagination="{ pageSize: 10 }"
              >
                <template #bodyCell="{ column, record }">
                  <template v-if="column.key === 'type'">
                    <a-tag color="volcano">{{ record.type }}</a-tag>
                  </template>
                  <template v-else-if="column.key === 'fact'">
                    <div class="edge-fact">{{ record.fact || '-' }}</div>
                  </template>
                </template>
              </a-table>
            </a-col>
          </a-row>
        </a-tab-pane>

        <!-- 检索 -->
        <a-tab-pane key="search" :tab="$t('legalKg.legalSearch')">
          <a-space direction="vertical" style="width: 100%">
            <a-space>
              <a-input-search
                v-model:value="searchQuery"
                :placeholder="$t('legalKg.enterLegalQuestion')"
                style="width: 500px"
                :enter-button="$t('legalKg.search')"
                @search="handleLegalSearch"
              />
              <a-select v-model:value="searchMode" style="width: 120px">
                <a-select-option value="hybrid">{{ $t('legalKg.hybridSearch') }}</a-select-option>
                <a-select-option value="semantic">{{ $t('legalKg.semanticSearch') }}</a-select-option>
                <a-select-option value="keyword">{{ $t('legalKg.keywordSearch') }}</a-select-option>
                <a-select-option value="graph">{{ $t('legalKg.graphSearch') }}</a-select-option>
              </a-select>
            </a-space>
            <a-spin :spinning="searching">
              <a-list
                v-if="searchResults.length > 0"
                :data-source="searchResults"
                size="large"
                bordered
                item-layout="vertical"
              >
                <template #renderItem="{ item, index }">
                  <a-list-item :key="index">
                    <template #actions>
                      <a-tag :color="item.nodeType === 'Case' ? 'red' : item.nodeType === 'LegalProvision' ? 'purple' : 'blue'">
                        {{ item.nodeType }}
                      </a-tag>
                      <span>{{ $t('legalKg.similarity') }}: {{ (item.similarity * 100).toFixed(1) }}%</span>
                    </template>
                    <a-list-item-meta>
                      <template #title>
                        <a @click="viewNodeDetailByUuid(item.uuid)">{{ item.name }}</a>
                      </template>
                      <template #description>
                        <div class="search-result-content">{{ item.summary || item.content }}</div>
                      </template>
                    </a-list-item-meta>
                  </a-list-item>
                </template>
              </a-list>
              <a-empty v-else-if="!searching && searched" :description="$t('legalKg.noResults')" />
            </a-spin>
          </a-space>
        </a-tab-pane>

        <!-- LLM 提取 -->
        <a-tab-pane key="extract" :tab="$t('legalKg.llmExtract')">
          <a-space direction="vertical" style="width: 100%" :size="16">
            <!-- 文件上传 -->
            <a-card :title="$t('legalKg.uploadJsonStep')" size="small">
              <a-upload-dragger
                :before-upload="handleExtractFileUpload"
                :file-list="extractFileList"
                accept=".json"
                name="file"
                @remove="handleExtractFileRemove"
              >
                <p class="ant-upload-drag-icon">
                  <InboxOutlined />
                </p>
                <p class="ant-upload-text">{{ $t('legalKg.clickOrDragJson') }}</p>
                <p class="ant-upload-hint">{{ $t('legalKg.jsonFileHint') }}</p>
              </a-upload-dragger>
            </a-card>

            <!-- 预览与字段映射 -->
            <a-card v-if="jsonPreview" :title="$t('legalKg.previewMappingStep')" size="small">
              <a-space direction="vertical" style="width: 100%" :size="12">
                <a-descriptions :column="4" size="small" bordered>
                  <a-descriptions-item :label="$t('legalKg.fileName')">{{ jsonPreview.fileName }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('legalKg.fileSize')">{{ formatFileSize(jsonPreview.fileSize) }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('legalKg.fieldCount')">{{ jsonPreview.fieldCount }}</a-descriptions-item>
                  <a-descriptions-item :label="$t('legalKg.parsed')">
                    <a-tag color="green">{{ $t('legalKg.parsed') }}</a-tag>
                  </a-descriptions-item>
                </a-descriptions>

                <a-collapse>
                  <a-collapse-panel key="1" :header="$t('legalKg.jsonFieldPreview')">
                    <a-descriptions :column="2" size="small">
                      <a-descriptions-item
                        v-for="(info, path) in flattenFieldTree(jsonPreview.fieldTree)"
                        :key="path"
                        :label="path"
                      >
                        <a-tag :color="getFieldTypeColor(info.type)">{{ info.type }}</a-tag>
                        {{ info.value }}
                      </a-descriptions-item>
                    </a-descriptions>
                  </a-collapse-panel>
                </a-collapse>

                <a-divider>{{ $t('legalKg.fieldMappingConfig') }}</a-divider>
                <a-alert type="info" show-icon>
                  <template #message>{{ $t('legalKg.mappingHelp') }}</template>
                  <template #description>
                    {{ $t('legalKg.mappingHelpContent') }}
                  </template>
                </a-alert>

                <div v-if="ontologyFields">
                  <a-card
                    v-for="(entity, entityType) in ontologyFields"
                    :key="entityType"
                    size="small"
                    :bordered="false"
                    class="entity-mapping-card"
                  >
                    <template #title>
                      <a-space>
                        <a-tag color="blue">{{ entityType }}</a-tag>
                        <span>{{ entity.displayName }}</span>
                      </a-space>
                    </template>
                    <a-table
                      :columns="mappingColumns"
                      :data-source="Object.entries(entity.fields)"
                      :pagination="false"
                      size="small"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'ontField'">
                          <a-tag>{{ record[0] }}</a-tag>
                          <span style="color: #8c8c8c">{{ record[1] }}</span>
                        </template>
                        <template v-else-if="column.key === 'jsonField'">
                          <a-select
                            v-model:value="fieldMappings[`${entityType}.${record[0]}`]"
                            :placeholder="$t('legalKg.selectJsonField')"
                            style="width: 100%"
                            allow-clear
                            show-search
                            :filter-option="filterOption"
                          >
                            <a-select-option
                              v-for="(info, path) in flattenFieldTree(jsonPreview.fieldTree)"
                              :key="path"
                              :value="path"
                            >
                              <a-space>
                                <code>{{ path }}</code>
                                <a-tag size="small" :color="getFieldTypeColor(info.type)">{{ info.type }}</a-tag>
                              </a-space>
                            </a-select-option>
                          </a-select>
                        </template>
                      </template>
                    </a-table>
                  </a-card>
                </div>

                <a-space>
                  <a-button type="primary" :loading="extracting" :disabled="!hasMapping" @click="handleExtract">
                    <RobotOutlined /> {{ $t('legalKg.extractLegalKnowledge') }}
                  </a-button>
                  <a-button :loading="extracting" :disabled="!hasMapping" @click="handleExtractAndSave">
                    <CloudUploadOutlined /> {{ $t('legalKg.extractAndSave') }}
                  </a-button>
                  <a-button @click="resetExtract">{{ $t('legalKg.reset') }}</a-button>
                </a-space>
              </a-space>
            </a-card>

            <!-- 提取结果 -->
            <a-card v-if="extractResult" :title="$t('legalKg.extractResultStep')" size="small">
              <a-space direction="vertical" style="width: 100%" :size="12">
                <a-row :gutter="16">
                  <a-col :span="6">
                    <a-statistic :title="$t('legalKg.cases')" :value="extractResult.cases?.length || 0" />
                  </a-col>
                  <a-col :span="6">
                    <a-statistic :title="$t('legalKg.parties')" :value="extractResult.parties?.length || 0" />
                  </a-col>
                  <a-col :span="6">
                    <a-statistic :title="$t('legalKg.courts')" :value="extractResult.courts?.length || 0" />
                  </a-col>
                  <a-col :span="6">
                    <a-statistic :title="$t('legalKg.legalProvisions')" :value="extractResult.provisions?.length || 0" />
                  </a-col>
                </a-row>

                <a-alert v-if="extractResult.errors?.length" type="error" show-icon>
                  <template #message>{{ $t('legalKg.extractFailed') }}</template>
                  <template #description>
                    <a-list size="small">
                      <a-list-item v-for="(err, idx) in extractResult.errors" :key="idx">{{ err }}</a-list-item>
                    </a-list>
                  </template>
                </a-alert>

                <a-collapse>
                  <a-collapse-panel key="cases" :header="$t('legalKg.cases') + ' ' + $t('legalKg.details')">
                    <a-table
                      v-if="extractResult.cases?.length"
                      :columns="caseResultColumns"
                      :data-source="extractResult.cases"
                      :pagination="false"
                      size="small"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'amount'">
                          {{ record.amountInDispute ? record.amountInDispute.toLocaleString() + ' ' : '-' }}
                        </template>
                      </template>
                    </a-table>
                    <a-empty v-else :description="t('legalKg.noResults')" />
                  </a-collapse-panel>

                  <a-collapse-panel key="parties" :header="$t('legalKg.parties') + ' ' + $t('legalKg.details')">
                    <a-table
                      v-if="extractResult.parties?.length"
                      :columns="partyResultColumns"
                      :data-source="extractResult.parties"
                      :pagination="false"
                      size="small"
                    />
                    <a-empty v-else :description="t('legalKg.noResults')" />
                  </a-collapse-panel>

                  <a-collapse-panel key="provisions" :header="$t('legalKg.legalProvisions') + ' ' + $t('legalKg.details')">
                    <a-table
                      v-if="extractResult.provisions?.length"
                      :columns="provisionResultColumns"
                      :data-source="extractResult.provisions"
                      :pagination="false"
                      size="small"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'content'">
                          <div class="provision-content">{{ record.content }}</div>
                        </template>
                      </template>
                    </a-table>
                    <a-empty v-else :description="t('legalKg.noResults')" />
                  </a-collapse-panel>

                  <a-collapse-panel key="courts" :header="$t('legalKg.courts') + ' ' + $t('legalKg.details')">
                    <a-table
                      v-if="extractResult.courts?.length"
                      :columns="courtResultColumns"
                      :data-source="extractResult.courts"
                      :pagination="false"
                      size="small"
                    />
                    <a-empty v-else :description="t('legalKg.noResults')" />
                  </a-collapse-panel>
                </a-collapse>
              </a-space>
            </a-card>
          </a-space>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 节点详情抽屉 -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      :title="detailDrawerTitle"
      width="600"
      placement="right"
    >
      <a-descriptions v-if="currentNode" :column="1" bordered>
        <a-descriptions-item v-for="(value, key) in currentNode.properties" :key="key" :label="key">
          {{ value }}
        </a-descriptions-item>
      </a-descriptions>

      <a-divider>{{ $t('legalKg.relations') }}</a-divider>
      <a-list v-if="nodeEdges.length > 0" :data-source="nodeEdges" size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-tag :color="getEdgeColor(item.type)">{{ item.type }}</a-tag>
            <span>{{ item.target || item.source }}</span>
            <template #actions>
              <a-tag>{{ item.fact || '-' }}</a-tag>
            </template>
          </a-list-item>
        </template>
      </a-list>
      <a-empty v-else :description="t('legalKg.noResults')" />
    </a-drawer>

    <!-- 导入确认弹窗 -->
    <a-modal
      v-model:open="importModalVisible"
      :title="t('legalKg.importModalTitle')"
      @ok="confirmImport"
      @cancel="importModalVisible = false"
      :confirm-loading="importing"
    >
      <a-space direction="vertical" style="width: 100%">
        <a-alert type="info" show-icon>
          <template #message>{{ t('legalKg.importContent') }}</template>
          <template #description>
            {{ t('legalKg.importContentDesc') }}
          </template>
        </a-alert>
        <a-list size="small" bordered>
          <a-list-item>{{ t('legalKg.provisions33') }}</a-list-item>
          <a-list-item>{{ t('legalKg.cases3') }}</a-list-item>
          <a-list-item>{{ t('legalKg.orgs2') }}</a-list-item>
          <a-list-item>{{ t('legalKg.mediators3') }}</a-list-item>
          <a-list-item>{{ t('legalKg.courtsJudgesList') }}</a-list-item>
          <a-list-item>{{ t('legalKg.lawyers2') }}</a-list-item>
          <a-list-item>{{ t('legalKg.parties2') }}</a-list-item>
          <a-list-item>{{ t('legalKg.evidenceDocs') }}</a-list-item>
        </a-list>
        <a-checkbox v-model:checked="importWithOntology">
          {{ t('legalKg.setOntologyDef') }}
        </a-checkbox>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import type { TableColumnsType } from 'ant-design-vue'

const { t } = useI18n()
import {
  CloudUploadOutlined,
  FileTextOutlined,
  AppstoreOutlined,
  SettingOutlined,
  ReloadOutlined,
  InboxOutlined,
  RobotOutlined
} from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import { nodeApi } from '@/api/node'
import { edgeApi } from '@/api/edge'
// import { searchApi } from '@/api/search' // 未使用
import request from '@/api/request'
import { LEGAL_ENTITIES, LEGAL_EDGES, LEGAL_GRAPH_ID } from '@/api/legal-kg-data'
import {
  previewJsonFile,
  extractLegalKG,
  extractAndSaveLegalKG,
  getOntologyFields,
  type JsonPreviewResp,
  type LegalExtractResultVO,
  type OntologyFieldsResp
} from '@/api/legal-extract'

const currentGraphId = ref('')
const graphList = ref<any[]>([])
const activeTab = ref('cases')
const loading = ref(false)
const searching = ref(false)
const searched = ref(false)

// 统计数据
const stats = reactive({
  nodeCount: 0,
  edgeCount: 0,
  caseCount: 0,
  provisionCount: 0,
  partyCount: 0,
  courtJudgeCount: 0
})

// 列表数据
const caseList = ref<any[]>([])
const provisionList = ref<any[]>([])
const partyList = ref<any[]>([])
const courtList = ref<any[]>([])
const judgeList = ref<any[]>([])
const edgeList = ref<any[]>([])
const relationTypes = ref<any[]>([])

// 搜索
const searchQuery = ref('')
const searchMode = ref('hybrid')
const searchResults = ref<any[]>([])

// 详情抽屉
const detailDrawerVisible = ref(false)
const detailDrawerTitle = ref('')
const currentNode = ref<any>(null)
const nodeEdges = ref<any[]>([])

// 导入
const importModalVisible = ref(false)
const importWithOntology = ref(true)
const importing = ref(false)

// LLM 提取
const extractFileList = ref<any[]>([])
const jsonPreview = ref<JsonPreviewResp | null>(null)
const ontologyFields = ref<OntologyFieldsResp | null>(null)
const fieldMappings = ref<Record<string, string>>({})
const extractResult = ref<LegalExtractResultVO | null>(null)
const extracting = ref(false)
const extractedFile = ref<File | null>(null)

// 表格列定义
const caseColumns: TableColumnsType = [
  { title: t('legalKg.caseNumber') || '案件编号', dataIndex: ['properties', 'caseNumber'], key: 'caseNumber', width: 200 },
  { title: t('legalKg.caseName') || '案件名称', dataIndex: 'name', key: 'name' },
  { title: t('legalKg.caseType') || '案件类型', key: 'caseType', width: 100 },
  { title: t('legalKg.caseStatus') || '案件状态', key: 'caseStatus', width: 100 },
  { title: t('legalKg.amount') || '争议金额', key: 'amount', width: 120 },
  { title: t('legalKg.action') || '操作', key: 'action', width: 150 }
]

const provisionColumns: TableColumnsType = [
  { title: t('legalKg.article') || '条款', dataIndex: ['properties', 'articleNumber'], key: 'articleNumber', width: 120 },
  { title: t('legalKg.provisionName') || '条文名称', dataIndex: 'name', key: 'name' },
  { title: t('legalKg.content') || '内容摘要', key: 'content', ellipsis: true },
  { title: t('legalKg.keywords') || '关键词', key: 'keywords', width: 200 },
  { title: t('legalKg.action') || '操作', key: 'action', width: 100 }
]

const partyColumns: TableColumnsType = [
  { title: t('legalKg.name') || '名称', dataIndex: 'name', key: 'name' },
  { title: t('legalKg.type') || '类型', key: 'partyType', width: 100 },
  { title: t('legalKg.role') || '角色', key: 'role', width: 100 },
  { title: t('legalKg.address') || '住所地', dataIndex: ['properties', 'address'], key: 'address' },
  { title: t('legalKg.action') || '操作', key: 'action', width: 150 }
]

const courtColumns: TableColumnsType = [
  { title: t('legalKg.courtName') || '法院名称', dataIndex: 'name', key: 'name' },
  { title: t('legalKg.level') || '级别', key: 'level', width: 150 },
  { title: t('legalKg.location') || '所在地', dataIndex: ['properties', 'location'], key: 'location' },
  { title: t('legalKg.action') || '操作', key: 'action', width: 80 }
]

const judgeColumns: TableColumnsType = [
  { title: t('legalKg.name') || '姓名', dataIndex: 'name', key: 'name' },
  { title: t('legalKg.title') || '职务', key: 'title', width: 120 },
  { title: t('legalKg.court') || '所属法院', dataIndex: ['properties', 'courtName'], key: 'courtName' },
  { title: t('legalKg.specialty') || '专业领域', dataIndex: ['properties', 'specialty'], key: 'specialty' },
  { title: t('legalKg.action') || '操作', key: 'action', width: 80 }
]

const edgeColumns: TableColumnsType = [
  { title: t('legalKg.relationType') || '关系类型', key: 'type', width: 200 },
  { title: t('legalKg.sourceNode') || '来源节点', dataIndex: 'source', key: 'source', width: 200 },
  { title: t('legalKg.targetNode') || '目标节点', dataIndex: 'target', key: 'target', width: 200 },
  { title: t('legalKg.relationFact') || '关系说明', key: 'fact', ellipsis: true }
]

// LLM 提取 - 字段映射表格列
const mappingColumns: TableColumnsType = [
  { title: t('legalKg.ontologyField') || '本体字段', key: 'ontField', width: 250 },
  { title: t('legalKg.jsonFieldPath') || 'JSON 字段路径', key: 'jsonField' }
]

// LLM 提取 - 结果表格列
const caseResultColumns: TableColumnsType = [
  { title: t('legalKg.caseName') || '案件名称', dataIndex: 'caseName', key: 'caseName', ellipsis: true },
  { title: t('legalKg.caseNumber') || '案号', dataIndex: 'caseNumber', key: 'caseNumber' },
  { title: t('legalKg.caseType') || '案件类型', dataIndex: 'caseType', key: 'caseType' },
  { title: t('legalKg.amount') || '争议金额', key: 'amount' }
]

const partyResultColumns: TableColumnsType = [
  { title: t('legalKg.name') || '名称', dataIndex: 'name', key: 'name' },
  { title: t('legalKg.type') || '类型', dataIndex: 'partyType', key: 'partyType' },
  { title: t('legalKg.role') || '角色', dataIndex: 'role', key: 'role' },
  { title: t('legalKg.address') || '住所地', dataIndex: 'address', key: 'address', ellipsis: true }
]

const provisionResultColumns: TableColumnsType = [
  { title: t('legalKg.provisionId') || '条文编号', dataIndex: 'provisionId', key: 'provisionId' },
  { title: t('legalKg.lawName') || '法律名称', dataIndex: 'lawName', key: 'lawName' },
  { title: t('legalKg.article') || '条款', dataIndex: 'articleNumber', key: 'articleNumber' },
  { title: t('legalKg.content') || '内容', key: 'content', ellipsis: true }
]

const courtResultColumns: TableColumnsType = [
  { title: t('legalKg.courtName') || '法院名称', dataIndex: 'name', key: 'name' },
  { title: t('legalKg.level') || '级别', dataIndex: 'level', key: 'level' },
  { title: t('legalKg.location') || '所在地', dataIndex: 'location', key: 'location' }
]

// 生命周期
onMounted(() => {
  loadGraphs()
})

// 方法
async function loadGraphs() {
  try {
    const resp = await graphApi.getList()
    graphList.value = resp as any[] || []
    if (graphList.value.length > 0) {
      // 查找或创建法律知识图谱
      const legalGraph = graphList.value.find(g => g.graphId === LEGAL_GRAPH_ID)
      if (legalGraph) {
        currentGraphId.value = legalGraph.graphId
      } else {
        currentGraphId.value = graphList.value[0].graphId
      }
      onGraphChange()
    }
  } catch (e) {
    console.error('加载图谱列表失败', e)
  }
}

async function onGraphChange() {
  if (!currentGraphId.value) return
  await loadGraphStats()
  await loadAllData()
}

async function loadGraphStats() {
  try {
    const resp = await graphApi.getStats(currentGraphId.value)
    Object.assign(stats, {
      nodeCount: resp?.nodeCount || 0,
      edgeCount: resp?.edgeCount || 0
    })

    // 分类统计
    const nodesResp = await nodeApi.list({ graphId: currentGraphId.value, limit: 10000 })
    const nodes = nodesResp as any[] || []

    stats.caseCount = nodes.filter(n => n.type === 'Case').length
    stats.provisionCount = nodes.filter(n => n.type === 'LegalProvision').length
    stats.partyCount = nodes.filter(n => n.type === 'Party').length
    stats.courtJudgeCount = nodes.filter(n => n.type === 'Court' || n.type === 'Judge').length

    // 统计关系类型
    const allEdgesResp = await edgeApi.list(currentGraphId.value, {})
    const edges = (allEdgesResp as any[]) || []
    const typeCount: Record<string, number> = {}
    edges.forEach((e: any) => {
      typeCount[e.type] = (typeCount[e.type] || 0) + 1
    })
    relationTypes.value = Object.entries(typeCount).map(([name, count]) => ({ name, count }))
    edgeList.value = edges
  } catch (e) {
    console.error('加载图谱统计失败', e)
  }
}

async function loadAllData() {
  loading.value = true
  try {
    const resp = await nodeApi.list({ graphId: currentGraphId.value, limit: 10000 })
    const nodes = resp as any[] || []

    caseList.value = nodes.filter(n => n.type === 'Case')
    provisionList.value = nodes.filter(n => n.type === 'LegalProvision')
    partyList.value = nodes.filter(n => n.type === 'Party')
    courtList.value = nodes.filter(n => n.type === 'Court')
    judgeList.value = nodes.filter(n => n.type === 'Judge')
  } catch (e) {
    console.error('加载数据失败', e)
    message.error(t('legalKg.loadFailed') || '加载数据失败')
  } finally {
    loading.value = false
  }
}

function handleImport() {
  importModalVisible.value = true
}

async function confirmImport() {
  if (!currentGraphId.value) {
    message.warning(t('legalKg.selectGraphFirst') || '请先选择图谱')
    return
  }

  importing.value = true
  try {
    // 导入节点数据
    const { LEGAL_NODES } = await import('@/api/legal-kg-data')
    const nodes = LEGAL_NODES.map((n: any) => ({
      name: n.name,
      type: n.type,
      summary: n.summary,
      properties: n.properties
    }))

    for (const node of nodes) {
      try {
        await nodeApi.create(currentGraphId.value, node, true)
      } catch (e) {
        console.warn('节点导入失败:', node.name, e)
      }
    }

    // 导入边数据
    const { LEGAL_EDGES_DATA } = await import('@/api/legal-kg-data')

    // 建立名称->UUID 映射（通过 API 查询）
    const allNodesResp = await nodeApi.list({ graphId: currentGraphId.value, limit: 10000 })
    const allNodes = allNodesResp as any[] || []
    const nameToUuid: Record<string, string> = {}
    allNodes.forEach((n: any) => {
      nameToUuid[n.name] = n.uuid
    })

    for (const edge of LEGAL_EDGES_DATA) {
      try {
        const sourceUuid = nameToUuid[edge.source]
        const targetUuid = nameToUuid[edge.target]
        if (sourceUuid && targetUuid) {
          await edgeApi.create(currentGraphId.value, {
            sourceNodeUuid: sourceUuid,
            targetNodeUuid: targetUuid,
            type: edge.type,
            fact: `${edge.type}: ${edge.source} -> ${edge.target}`,
            properties: edge.properties
          })
        }
      } catch (e) {
        console.warn('边导入失败:', edge, e)
      }
    }

    // 设置本体
    if (importWithOntology.value) {
      try {
        await graphApi.setOntology(currentGraphId.value, {
          entities: JSON.stringify(LEGAL_ENTITIES),
          edges: JSON.stringify(LEGAL_EDGES)
        })
      } catch (e) {
        console.warn('本体设置失败', e)
      }
    }

    message.success(t('legalKg.importSuccess') || '法律知识图谱数据导入成功')
    importModalVisible.value = false
    await loadGraphStats()
    await loadAllData()
  } catch (e: any) {
    console.error('导入失败', e)
    message.error(t('legalKg.importFailed') + ': ' + (e?.message || ''))
  } finally {
    importing.value = false
  }
}

async function handleImportProvisions() {
  if (!currentGraphId.value) {
    message.warning(t('legalKg.selectGraphFirst') || '请先选择图谱')
    return
  }
  try {
    const { LEGAL_NODES } = await import('@/api/legal-kg-data')
    const provisions = LEGAL_NODES
      .filter((n: any) => n.type === 'LegalProvision')
      .map((n: any) => ({
        name: n.name,
        type: n.type,
        summary: n.summary,
        properties: n.properties
      }))

    let success = 0
    for (const p of provisions) {
      try {
        await nodeApi.create(currentGraphId.value, p, true)
        success++
      } catch (e) {
        console.warn('导入失败:', p.name, e)
      }
    }

    message.success(t('legalKg.importedProvisions').replace('{count}', success.toString()).replace('{total}', provisions.length.toString()) || `成功导入 ${success}/${provisions.length} 条法律条文`)
    await loadGraphStats()
    await loadAllData()
  } catch (e) {
    message.error(t('legalKg.importFailed') || '导入失败')
  }
}

async function handleImportCases() {
  if (!currentGraphId.value) {
    message.warning(t('legalKg.selectGraphFirst') || '请先选择图谱')
    return
  }
  try {
    const { LEGAL_NODES } = await import('@/api/legal-kg-data')
    const cases = LEGAL_NODES
      .filter((n: any) => n.type === 'Case')
      .map((n: any) => ({
        name: n.name,
        type: n.type,
        summary: n.summary,
        properties: n.properties
      }))

    let success = 0
    for (const c of cases) {
      try {
        await nodeApi.create(currentGraphId.value, c, true)
        success++
      } catch (e) {
        console.warn('导入失败:', c.name, e)
      }
    }

    message.success(t('legalKg.importedCases').replace('{count}', success.toString()).replace('{total}', cases.length.toString()) || `成功导入 ${success}/${cases.length} 个案例`)
    await loadGraphStats()
    await loadAllData()
  } catch (e) {
    message.error(t('legalKg.importFailed') || '导入失败')
  }
}

async function handleSetOntology() {
  if (!currentGraphId.value) {
    message.warning(t('legalKg.selectGraphFirst') || '请先选择图谱')
    return
  }
  try {
    await request.post(`/ontology/${currentGraphId.value}`, {
      entities: JSON.stringify(LEGAL_ENTITIES),
      edges: JSON.stringify(LEGAL_EDGES)
    })
    message.success(t('legalKg.ontologySetSuccess') || '法律本体设置成功')
  } catch (e: any) {
    message.error(t('legalKg.ontologySetFailed') + ': ' + (e?.message || ''))
  }
}

async function handleLegalSearch() {
  if (!searchQuery.value.trim()) return

  searching.value = true
  searched.value = true
  searchResults.value = []

  try {
    const resp = await request.post(`/graph/search/graph/${currentGraphId.value}`, {
      query: searchQuery.value,
      maxFacts: 20,
      config: { mode: searchMode.value },
      filters: []
    })
    const facts = (resp as any)?.facts || []
    searchResults.value = facts.map((f: any) => ({
      uuid: f.uuid,
      name: f.name || f.fact || '',
      nodeType: '',
      summary: f.fact,
      similarity: f.score || 0
    }))
  } catch (e) {
    console.error('搜索失败', e)
    message.error(t('legalKg.searchFailed') || '搜索失败')
  } finally {
    searching.value = false
  }
}

async function viewNodeDetail(node: any) {
  currentNode.value = node
  detailDrawerTitle.value = node.name
  detailDrawerVisible.value = true

  try {
    const res = await nodeApi.getEdges(currentGraphId.value, node.uuid, 0, 100)
    nodeEdges.value = res as any[] || []
  } catch (e) {
    nodeEdges.value = []
  }
}

async function viewNodeDetailByUuid(uuid: string) {
  try {
    const resp = await nodeApi.get(currentGraphId.value, uuid)
    await viewNodeDetail(resp as any)
  } catch (e) {
    console.error('获取节点详情失败', e)
  }
}

async function viewNodeRelations(node: any) {
  await viewNodeDetail(node)
  activeTab.value = 'relations'
}

// 辅助方法
function getCaseTypeColor(type?: string) {
  const colors: Record<string, string> = {
    '商事': 'red',
    '民事': 'blue',
    '刑事': 'purple',
    '行政': 'orange'
  }
  return colors[type || ''] || 'default'
}

function getCaseStatusColor(status?: string) {
  const colors: Record<string, string> = {
    '调解成功': 'green',
    '结案': 'cyan',
    '审理中': 'blue',
    '判决': 'geekblue',
    '上诉中': 'orange'
  }
  return colors[status || ''] || 'default'
}

function formatAmount(amount: any) {
  if (!amount) return '-'
  const num = Number(amount)
  if (isNaN(num)) return amount
  return num.toLocaleString('zh-CN') + ' 元'
}

function splitKeywords(keywords?: string) {
  if (!keywords) return []
  return keywords.split(/[,，]/).filter(Boolean)
}

function getEdgeColor(type: string) {
  const colors: Record<string, string> = {
    'CASE_PARTY': 'blue',
    'CASE_JUDGE': 'gold',
    'CASE_COURT': 'cyan',
    'CASE_LEGAL_PROVISION': 'purple',
    'CASE_EVIDENCE': 'green',
    'CASE_JUDGMENT': 'red',
    'PARTY_LAWYER': 'orange',
    'LEGAL_PROVISION_RELATED': 'magenta',
    'CASE_RELATED': 'volcano',
    'ORG_MEDIATOR': 'lime',
    'CASE_MEDIATION_ORG': 'geekblue',
    'CASE_MEDIATION_AGREEMENT': 'green'
  }
  return colors[type] || 'default'
}

// ============ LLM 提取相关方法 ============

async function loadOntologyFields() {
  try {
    const resp = await getOntologyFields()
    ontologyFields.value = resp
  } catch (e) {
    console.error('加载本体字段失败', e)
    message.error(t('legalKg.loadOntologyFailed') || '加载本体字段定义失败')
  }
}

async function handleExtractFileUpload(file: File) {
  extractFileList.value = [file]
  extractedFile.value = file

  try {
    const resp = await previewJsonFile(file)
    jsonPreview.value = resp
    fieldMappings.value = {}
    extractResult.value = null

    // 加载本体字段
    if (!ontologyFields.value) {
      await loadOntologyFields()
    }

    // 自动推荐映射
    autoSuggestMappings()
  } catch (e: any) {
    console.error('预览JSON文件失败', e)
    message.error(t('legalKg.previewFailed') + ': ' + (e?.message || ''))
    extractFileList.value = []
    extractedFile.value = null
  }

  return false // 阻止默认上传行为
}

function handleExtractFileRemove() {
  extractFileList.value = []
  extractedFile.value = null
  jsonPreview.value = null
  extractResult.value = null
  fieldMappings.value = {}
}

function autoSuggestMappings() {
  if (!jsonPreview.value) return

  const flatFields = flattenFieldTree(jsonPreview.value.fieldTree)
  const suggestions: Record<string, string> = {}

  // 简单的关键词匹配
  for (const [jsonPath, info] of Object.entries(flatFields)) {
    const path = jsonPath.toLowerCase()
    const value = String(info.value || '').toLowerCase()

    // Case 字段匹配
    if (path.includes('ajmc') || value.includes('案') && value.includes('名')) {
      suggestions['Case.caseName'] = jsonPath
    }
    if (path.includes('ah') || path.includes('ajh') || path.includes('case_number')) {
      suggestions['Case.caseNumber'] = jsonPath
    }
    if (path.includes('ajlx') || value.includes('商事') || value.includes('民事')) {
      suggestions['Case.caseType'] = jsonPath
    }
    if (path.includes('jar') || path.includes('filing')) {
      suggestions['Case.filingDate'] = jsonPath
    }
    if (path.includes('je') || path.includes('amount')) {
      suggestions['Case.amountInDispute'] = jsonPath
    }
    if (path.includes('ssjl') || value.includes('案件') && value.includes('事实')) {
      suggestions['Case.summary'] = jsonPath
    }

    // Party 字段匹配
    if (path.includes('dsr') || value.includes('原告') || value.includes('被告')) {
      suggestions['Party.name'] = jsonPath
    }

    // Court 字段匹配
    if (path.includes('fy') || value.includes('法院')) {
      suggestions['Court.name'] = jsonPath
    }

    // LegalProvision 字段匹配
    if (path.includes('tznr') || value.includes('条') && value.includes('本条例')) {
      suggestions['LegalProvision.content'] = jsonPath
    }
  }

  // 应用建议
  for (const [key, jsonPath] of Object.entries(suggestions)) {
    if (!fieldMappings.value[key]) {
      fieldMappings.value[key] = jsonPath
    }
  }
}

function flattenFieldTree(tree: Record<string, any>, result: Record<string, any> = {}): Record<string, any> {
  for (const [key, value] of Object.entries(tree)) {
    if (value && typeof value === 'object') {
      if ('type' in value && 'children' in value) {
        result[key] = { type: value.type, value: value.value || '' }
        flattenFieldTree(value.children || {}, result)
      } else if (Array.isArray(value)) {
        result[key] = { type: 'array', value: '' }
      } else {
        flattenFieldTree(value, result)
      }
    } else {
      result[key] = { type: 'string', value: String(value || '') }
    }
  }
  return result
}

function getFieldTypeColor(type: string) {
  const colors: Record<string, string> = {
    'string': 'blue',
    'integer': 'cyan',
    'number': 'geekblue',
    'boolean': 'orange',
    'array': 'purple',
    'object': 'green'
  }
  return colors[type] || 'default'
}

function filterOption(input: string, option: any) {
  return option.value.toLowerCase().includes(input.toLowerCase())
}

function formatFileSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const hasMapping = computed(() => {
  return Object.keys(fieldMappings.value).some(k => fieldMappings.value[k])
})

async function handleExtract() {
  if (!extractedFile.value) {
    message.warning(t('legalKg.uploadJsonFirst') || '请先上传 JSON 文件')
    return
  }
  if (!currentGraphId.value) {
    message.warning(t('legalKg.selectGraphFirst') || '请先选择图谱')
    return
  }
  if (!hasMapping.value) {
    message.warning(t('legalKg.configMappingFirst') || '请至少配置一个字段映射')
    return
  }

  extracting.value = true
  try {
    const resp = await extractLegalKG(extractedFile.value, currentGraphId.value, fieldMappings.value)
    extractResult.value = resp as LegalExtractResultVO
    message.success(t('legalKg.extractSuccess') || '提取完成')
  } catch (e: any) {
    console.error('提取失败', e)
    message.error(t('legalKg.extractFailed') + ': ' + (e?.message || ''))
  } finally {
    extracting.value = false
  }
}

async function handleExtractAndSave() {
  if (!extractedFile.value) {
    message.warning(t('legalKg.uploadJsonFirst') || '请先上传 JSON 文件')
    return
  }
  if (!currentGraphId.value) {
    message.warning(t('legalKg.selectGraphFirst') || '请先选择图谱')
    return
  }
  if (!hasMapping.value) {
    message.warning(t('legalKg.configMappingFirst') || '请至少配置一个字段映射')
    return
  }

  extracting.value = true
  try {
    const resp = await extractAndSaveLegalKG(extractedFile.value, currentGraphId.value, fieldMappings.value)
    extractResult.value = resp as LegalExtractResultVO

    const result = resp as LegalExtractResultVO
    const nodes = result.totalNodes || 0
    const edges = result.totalEdges || 0

    message.success(t('legalKg.extractAndSaveSuccess').replace('{nodes}', nodes.toString()).replace('{edges}', edges.toString()) || `提取并保存成功！共导入 ${nodes} 个节点，${edges} 条边`)

    // 刷新数据
    await loadGraphStats()
    await loadAllData()
  } catch (e: any) {
    console.error('提取并保存失败', e)
    message.error(t('legalKg.extractAndSaveFailed') + ': ' + (e?.message || ''))
  } finally {
    extracting.value = false
  }
}

function resetExtract() {
  extractFileList.value = []
  extractedFile.value = null
  jsonPreview.value = null
  extractResult.value = null
  fieldMappings.value = {}
}
</script>

<style scoped>
@import '@/assets/styles/dark.less';

.legal-kg-page {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
  margin-bottom: 8px;
}

.page-desc {
  color: #ffffff;
  opacity: 0.7;
  font-size: 14px;
}

/* 统计数字颜色 */
:deep(.ant-statistic-title) {
  color: #ffffff;
}

:deep(.ant-statistic-content-value) {
  color: #ffffff;
  font-weight: 600;
}

:deep(.ant-tabs-nav) {
  color: #ffffff;
}

:deep(.ant-tabs-tab) {
  color: #ffffff;
  opacity: 0.7;
}

:deep(.ant-tabs-tab:hover) {
  color: #ffffff;
  opacity: 1;
}

:deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #ffffff !important;
  opacity: 1;
}

:deep(.ant-tabs-ink-bar) {
  background: @primary-color;
}

.action-card {
  margin-bottom: 16px;
}

.content-card {
  margin-bottom: 24px;
}

.provision-content {
  max-height: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.edge-fact {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.search-result-content {
  max-height: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  color: @text-secondary;
  font-size: 13px;
}

.instruction-text {
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.entity-mapping-card {
  margin-bottom: 12px;
  background: @bg-elevated;
  border: 1px solid @border-color;
}
</style>
