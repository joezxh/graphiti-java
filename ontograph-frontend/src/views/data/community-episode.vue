<template>
  <div class="community-episode-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">{{ t('communityEpisode.title') }}</h1>
        <p class="page-desc">{{ t('communityEpisode.desc') }}</p>
      </div>
      <div class="header-right">
        <a-select
          v-model:value="selectedGraphId"
          :placeholder="t('common.pleaseSelect') + t('graph.graphName')"
          style="width: 220px"
          @change="onGraphChange"
        >
          <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
            {{ g.name }}
          </a-select-option>
        </a-select>
      </div>
    </div>

    <!-- Main Tabs -->
    <a-tabs v-if="selectedGraphId" v-model:activeKey="activeTab" class="management-tabs">
      <!-- 社区管理 -->
      <a-tab-pane key="communities" :tab="t('communityEpisode.communityTab')">
        <!-- 工具栏 -->
        <div class="tab-toolbar">
          <a-input-search
            v-model:value="communitySearch"
            :placeholder="t('communityEpisode.searchCommunity')"
            style="width: 240px"
            @search="loadCommunities"
            allow-clear
          />
          <a-select
            v-model:value="communityDomainFilter"
            :placeholder="t('communityEpisode.domainType')"
            style="width: 160px"
            allow-clear
            @change="loadCommunities"
          >
            <a-select-option v-for="d in domainOptions" :key="d.typeCode" :value="d.typeCode">{{ d.typeName }}</a-select-option>
          </a-select>
          <a-select
            v-model:value="communityTypeFilter"
            :placeholder="t('communityEpisode.communityType')"
            style="width: 140px"
            allow-clear
            @change="loadCommunities"
          >
            <a-select-option v-for="type in communityTypeOptions" :key="type" :value="type">{{ type }}</a-select-option>
          </a-select>
          <a-button type="primary" @click="openCommunityModal()">
            <PlusOutlined /> {{ t('communityEpisode.newCommunity') }}
          </a-button>
          <a-button @click="exportCommunities">
            <DownloadOutlined /> {{ t('common.export') }}
          </a-button>
        </div>

        <!-- 表格 -->
        <a-table
          :columns="communityColumns"
          :data-source="communityList"
          :loading="communityLoading"
          :pagination="communityPagination"
          row-key="uuid"
          size="small"
          @change="handleCommunityTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'communityType'">
              <a-tag :color="getCommunityColor(record.communityType)">
                {{ record.communityType || '-' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'memberCount'">
              <a-badge :count="record.memberCount || 0" :number-style="{ backgroundColor: '#0958d9' }" :show-zero="true" />
            </template>
            <template v-else-if="column.key === 'createdAt'">
              <span style="font-size: 12px; color: #8b949e">{{ formatDate(record.createdAt) }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space size="small">
                <a-button type="link" size="small" @click="openCommunityModal(record)">
                  <EditOutlined /> {{ t('common.edit') }}
                </a-button>
                <a-popconfirm :title="t('communityEpisode.confirmDeleteCommunity')" :ok-text="t('common.confirm')" :cancel-text="t('common.cancel')" @confirm="deleteCommunity(record.uuid)">
                  <a-button type="link" danger size="small">
                    <DeleteOutlined /> {{ t('common.delete') }}
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <!-- 剧集管理 -->
      <a-tab-pane key="episodes" :tab="t('communityEpisode.episodeTab')">
        <!-- 工具栏 -->
        <div class="tab-toolbar">
          <a-input-search
            v-model:value="episodeSearch"
            :placeholder="t('communityEpisode.searchEpisode')"
            style="width: 240px"
            @search="loadEpisodes"
            allow-clear
          />
          <a-select
            v-model:value="episodeTypeFilter"
            :placeholder="t('communityEpisode.episodeType')"
            style="width: 140px"
            allow-clear
            @change="loadEpisodes"
          >
            <a-select-option v-for="type in episodeTypeOptions" :key="type" :value="type">{{ type }}</a-select-option>
          </a-select>
          <a-button type="primary" @click="openEpisodeModal()">
            <PlusOutlined /> {{ t('communityEpisode.newEpisode') }}
          </a-button>
          <a-button @click="exportEpisodes">
            <DownloadOutlined /> {{ t('common.export') }}
          </a-button>
        </div>

        <!-- 表格 -->
        <a-table
          :columns="episodeColumns"
          :data-source="episodeList"
          :loading="episodeLoading"
          :pagination="episodePagination"
          row-key="uuid"
          size="small"
          @change="handleEpisodeTableChange"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'episodeType'">
              <a-tag color="orange">{{ record.episodeType || '-' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'processType'">
              <span>{{ record.processType || '-' }}</span>
            </template>
            <template v-if="column.key === 'createdAt'">
              <span style="font-size: 12px; color: #8b949e">{{ formatDate(record.createdAt) }}</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space size="small">
                <a-button type="link" size="small" @click="openEpisodeModal(record)">
                  <EditOutlined /> {{ t('common.edit') }}
                </a-button>
                <a-popconfirm :title="t('communityEpisode.confirmDeleteEpisode')" :ok-text="t('common.confirm')" :cancel-text="t('common.cancel')" @confirm="deleteEpisode(record.uuid)">
                  <a-button type="link" danger size="small">
                    <DeleteOutlined /> {{ t('common.delete') }}
                  </a-button>
                </a-popconfirm>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-tab-pane>
    </a-tabs>

    <!-- 无图谱时的提示 -->
    <a-empty v-else :description="t('communityEpisode.selectGraphFirst')" style="margin-top: 80px" />

    <!-- 社区编辑 Modal -->
    <a-modal
      v-model:open="communityModalVisible"
      :title="editingCommunity ? t('communityEpisode.editCommunity') : t('communityEpisode.newCommunity')"
      :confirm-loading="communityModalLoading"
      width="600px"
      @ok="saveCommunity"
      @cancel="communityModalVisible = false"
    >
      <a-form :model="communityForm" layout="vertical" :label-col="{ span: 6 }">
        <a-form-item :label="t('graphIde.labelName')" required>
          <a-input v-model:value="communityForm.name" :placeholder="t('communityEpisode.communityNamePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('graphIde.labelType')">
          <a-select v-model:value="communityForm.communityType" :placeholder="t('communityEpisode.selectCommunityType')" allow-clear>
            <a-select-option v-for="type in communityTypeOptions" :key="type" :value="type">{{ type }}</a-select-option>
          </a-select>
        </a-form-item>
        <!-- V3.1.0 通用化字段 -->
        <a-form-item :label="t('graphIde.labelLegalDomain')">
          <a-select v-model:value="communityForm.domainType" :placeholder="t('communityEpisode.selectDomainType')" allow-clear @change="onDomainTypeChange">
            <a-select-option v-for="d in domainOptions" :key="d.typeCode" :value="d.typeCode">{{ d.typeName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="t('communityEpisode.subDomain')">
          <a-select v-model:value="communityForm.subDomainType" :placeholder="t('communityEpisode.selectSubDomain')" allow-clear :disabled="!communityForm.domainType">
            <a-select-option v-for="d in subDomainOptions" :key="d.typeCode" :value="d.typeCode">{{ d.typeName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="t('communityEpisode.region')">
          <a-select v-model:value="communityForm.region" :placeholder="t('communityEpisode.selectRegion')" allow-clear>
            <a-select-option v-for="r in regionOptions" :key="r.typeCode" :value="r.typeCode">{{ r.typeName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item :label="t('communityEpisode.scenarioType')">
          <a-select v-model:value="communityForm.scenarioType" :placeholder="t('communityEpisode.selectScenario')" allow-clear>
            <a-select-option v-for="s in scenarioOptions" :key="s.typeCode" :value="s.typeCode">{{ s.typeName }}</a-select-option>
          </a-select>
        </a-form-item>
        <!-- 向后兼容旧字段（隐藏，用户如传旧字段自动映射） -->
        <a-form-item :label="t('graphIde.labelSummary')">
          <a-textarea v-model:value="communityForm.summary" :placeholder="t('communityEpisode.summaryPlaceholder')" :rows="2" />
        </a-form-item>
        <a-form-item :label="t('graphIde.labelDescription')">
          <a-textarea v-model:value="communityForm.description" :placeholder="t('communityEpisode.descPlaceholder')" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 剧集编辑 Modal -->
    <a-modal
      v-model:open="episodeModalVisible"
      :title="editingEpisode ? t('episodeEdit.titleEdit') : t('episodeEdit.titleNew')"
      :confirm-loading="episodeModalLoading"
      width="600px"
      @ok="saveEpisode"
      @cancel="episodeModalVisible = false"
    >
      <a-form :model="episodeForm" layout="vertical" :label-col="{ span: 6 }">
        <a-form-item :label="t('episodeEdit.labelSource')" required>
          <a-input v-model:value="episodeForm.source" :placeholder="t('communityEpisode.sourcePlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('episodeEdit.labelName')">
          <a-input v-model:value="episodeForm.name" :placeholder="t('episodeEdit.placeholderName')" />
        </a-form-item>
        <a-form-item :label="t('communityEpisode.sourceDesc')">
          <a-input v-model:value="episodeForm.sourceDescription" :placeholder="t('communityEpisode.sourceDescPlaceholder')" />
        </a-form-item>
        <a-form-item :label="t('episodeEdit.labelContent')">
          <a-textarea v-model:value="episodeForm.content" :placeholder="t('episodeEdit.placeholderContent')" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, EditOutlined, DeleteOutlined, DownloadOutlined
} from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import { episodeApi } from '@/api/episode'
import { communityTypeApi, type OntCommunityTypeVO } from '@/api/metadata'

const { t } = useI18n()

// ==================== State ====================

const selectedGraphId = ref<string>('')
const activeTab = ref<string>('communities')
const graphOptions = ref<{ graphId: string; name: string }[]>([])

// 社区
const communityList = ref<any[]>([])
const communityLoading = ref(false)
const communitySearch = ref('')
const communityDomainFilter = ref<string>()
const communityTypeFilter = ref<string>()
const communityPagination = reactive({ current: 1, pageSize: 20, total: 0 })
const communityModalVisible = ref(false)
const communityModalLoading = ref(false)
const editingCommunity = ref<any>(null)
const communityForm = reactive({
  name: '',
  communityType: undefined as string | undefined,
  // V3.1.0 通用化新字段
  domainType: undefined as string | undefined,
  subDomainType: undefined as string | undefined,
  region: undefined as string | undefined,
  scenarioType: undefined as string | undefined,
  // 向后兼容旧字段（用于读取旧数据）
  legalDomain: undefined as string | undefined,
  jurisdiction: '',
  practiceType: '',
  summary: '',
  description: '',
})

// 剧集
const episodeList = ref<any[]>([])
const episodeLoading = ref(false)
const episodeSearch = ref('')
const episodeTypeFilter = ref<string>()
const episodePagination = reactive({ current: 1, pageSize: 20, total: 0 })
const episodeModalVisible = ref(false)
const episodeModalLoading = ref(false)
const editingEpisode = ref<any>(null)
const episodeForm = reactive({
  name: '',
  source: '',
  sourceDescription: '',
  content: '',
})

// 下拉选项 - API 动态加载
const communityTypes = ref<OntCommunityTypeVO[]>([])

const loadCommunityTypes = async () => {
  if (!selectedGraphId.value) return
  try {
    const res = await communityTypeApi.list(selectedGraphId.value, 0)
    communityTypes.value = (res as any).data || []
  } catch (error) {
    console.error(t('communityEpisode.loadTypesFailed'), error)
  }
}

// 一级领域选项（category === 'domain'，parentTypeCode === 'DOMAIN_ROOT' 或 null）
const domainOptions = computed(() =>
  communityTypes.value
    .filter(t => t.category === 'domain' && (t.parentTypeCode === 'DOMAIN_ROOT' || t.parentTypeCode === null))
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
)

// 子领域选项（根据选中的顶层领域动态过滤）
const subDomainOptions = computed(() => {
  if (!communityForm.domainType) return []
  return communityTypes.value
    .filter(t => t.category === 'domain' && t.parentTypeCode === communityForm.domainType)
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
})

// 区域选项（category === 'region'）
const regionOptions = computed(() =>
  communityTypes.value
    .filter(t => t.category === 'region')
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
)

// 场景选项（category === 'scenario'）
const scenarioOptions = computed(() =>
  communityTypes.value
    .filter(t => t.category === 'scenario')
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
)

// 社区类型选项（category === 'community_type'）
const communityTypeOptions = computed(() =>
  communityTypes.value
    .filter(t => t.category === 'community_type')
    .sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
    .map(t => t.typeName)
)

// 剧集类型选项（从 episodeTypeApi 获取）
const episodeTypeOptions = ref<string[]>([])

// 领域类型层级计算：用于校验新建社区类型时不超过 3 层
// - 无 domainType → 0 层（根节点）
// - 有 domainType 但无 subDomainType → 1 层（可直接创建新 communityType）
// - 有 subDomainType → 2 层（再创建新 communityType 会是第 3 层，最多，不允许）
const domainDepth = computed(() => {
  if (communityForm.subDomainType) return 2
  if (communityForm.domainType) return 1
  return 0
})

// ==================== Columns ====================

const communityColumns = computed(() => [
  { title: t('graphIde.labelName'), dataIndex: 'name', key: 'name', width: 180, ellipsis: true },
  { title: t('graphIde.labelType'), key: 'communityType', width: 100 },
  { title: t('graphIde.labelLegalDomain'), key: 'domainType', width: 120, customRender: ({ text }: { text: string }) => text || '-' },
  { title: t('communityEpisode.subDomain'), key: 'subDomainType', width: 140, customRender: ({ text }: { text: string }) => text || '-' },
  { title: t('communityEpisode.region'), key: 'region', width: 90, customRender: ({ text }: { text: string }) => text || '-' },
  { title: t('communityEpisode.scenarioType'), key: 'scenarioType', width: 90, customRender: ({ text }: { text: string }) => text || '-' },
  { title: t('graphIde.labelMemberCount'), key: 'memberCount', width: 90, align: 'center' as const },
  { title: t('common.createdAt'), key: 'createdAt', width: 140 },
  { title: t('common.action'), key: 'action', width: 160, fixed: 'right' as const },
])

const episodeColumns = computed(() => [
  { title: t('episodeEdit.labelSource'), dataIndex: 'source', key: 'source', width: 160, ellipsis: true },
  { title: t('episodeEdit.labelName'), dataIndex: 'name', key: 'name', width: 180, ellipsis: true },
  { title: t('graphIde.labelType'), key: 'episodeType', width: 120 },
  { title: t('graphIde.labelProcessType'), key: 'processType', width: 110, customRender: ({ text }: { text: string }) => text || '-' },
  { title: t('graphIde.labelStageLevel'), key: 'stageLevel', width: 90, customRender: ({ text }: { text: string }) => text || '-' },
  { title: t('graphIde.labelStage'), key: 'stageLabel', width: 90, customRender: ({ text }: { text: string }) => text || '-' },
  { title: t('common.createdAt'), key: 'createdAt', width: 140 },
  { title: t('common.action'), key: 'action', width: 160, fixed: 'right' as const },
])

// ==================== Graph Selection ====================

onMounted(async () => {
  await loadGraphOptions()
  await loadCommunityTypes()
})

const loadGraphOptions = async () => {
  try {
    const graphs = await graphApi.getList()
    graphOptions.value = graphs.map((g: any) => ({ graphId: g.graphId || g.id, name: g.name }))
    if (graphOptions.value.length > 0) {
      selectedGraphId.value = graphOptions.value[0].graphId
      await loadCommunities()
      await loadEpisodes()
    }
  } catch (e) {
    console.error(t('graph.loadFailed'), e)
  }
}

const onGraphChange = async () => {
  await loadCommunityTypes()
  await loadCommunities()
  await loadEpisodes()
}

// ==================== Community CRUD ====================

const loadCommunities = async () => {
  if (!selectedGraphId.value) return
  communityLoading.value = true
  try {
    const skip = (communityPagination.current - 1) * communityPagination.pageSize
    const result = await graphApi.listCommunities(selectedGraphId.value, {
      keyword: communitySearch.value || undefined,
      domain: communityDomainFilter.value || undefined,
      type: communityTypeFilter.value || undefined,
      skip,
      limit: communityPagination.pageSize,
    })
    communityList.value = result.communities || []
    communityPagination.total = result.totalCount || 0
  } catch (e) {
    console.error(t('communityEpisode.loadCommunitiesFailed'), e)
    message.error(t('communityEpisode.loadCommunitiesFailed'))
  } finally {
    communityLoading.value = false
  }
}

const openCommunityModal = (record?: any) => {
  editingCommunity.value = record || null
  if (record) {
    communityForm.name = record.name || ''
    communityForm.communityType = record.communityType
    // V3.1.0 优先读新字段，兼容旧字段
    communityForm.domainType = record.domainType || record.legalDomain
    communityForm.subDomainType = record.subDomainType || ''
    communityForm.region = record.region || record.jurisdiction || ''
    communityForm.scenarioType = record.scenarioType || record.practiceType || ''
    communityForm.legalDomain = record.legalDomain
    communityForm.jurisdiction = record.jurisdiction || ''
    communityForm.practiceType = record.practiceType || ''
    communityForm.summary = record.summary || ''
    communityForm.description = record.description || ''
  } else {
    Object.assign(communityForm, {
      name: '',
      communityType: undefined,
      domainType: undefined,
      subDomainType: undefined,
      region: undefined,
      scenarioType: undefined,
      legalDomain: undefined,
      jurisdiction: '',
      practiceType: '',
      summary: '',
      description: '',
    })
  }
  communityModalVisible.value = true
}

const saveCommunity = async () => {
  if (!selectedGraphId.value) return
  if (!communityForm.name?.trim()) {
    message.warning(t('communityEpisode.pleaseEnterName'))
    return
  }
  // 层级校验：subDomainType 已选时，不能再填 communityType（会创建第 4 层，不允许）
  if (domainDepth.value >= 2 && communityForm.communityType) {
    message.warning(t('communityEpisode.maxDepthWarning'))
    return
  }
  communityModalLoading.value = true
  try {
    const payload = {
      name: communityForm.name,
      communityType: communityForm.communityType,
      // V3.1.0 通用化新字段
      domainType: communityForm.domainType,
      subDomainType: communityForm.subDomainType,
      region: communityForm.region,
      scenarioType: communityForm.scenarioType,
      // 向后兼容旧字段
      legalDomain: communityForm.legalDomain,
      jurisdiction: communityForm.jurisdiction,
      practiceType: communityForm.practiceType,
      summary: communityForm.summary,
      description: communityForm.description,
    }
    if (editingCommunity.value) {
      await graphApi.updateCommunity(selectedGraphId.value, editingCommunity.value.uuid, payload)
      message.success(t('common.updateSuccess'))
    } else {
      await graphApi.createCommunity(selectedGraphId.value, payload)
      message.success(t('common.createSuccess'))
    }
    communityModalVisible.value = false
    await loadCommunities()
  } catch (e) {
    console.error(t('communityEpisode.saveCommunityFailed'), e)
    message.error(t('common.saveFailed'))
  } finally {
    communityModalLoading.value = false
  }
}

// 领域类型变化时，清空子领域选择
const onDomainTypeChange = () => {
  communityForm.subDomainType = undefined
}

const deleteCommunity = async (uuid: string) => {
  if (!selectedGraphId.value) return
  try {
    await graphApi.deleteCommunity(selectedGraphId.value, uuid)
    message.success(t('common.deleteSuccess'))
    await loadCommunities()
  } catch (e) {
    console.error(t('communityEpisode.deleteCommunityFailed'), e)
    message.error(t('common.deleteFailed'))
  }
}

const handleCommunityTableChange = (pag: any) => {
  communityPagination.current = pag.current
  communityPagination.pageSize = pag.pageSize
  loadCommunities()
}

const exportCommunities = () => {
  const data = communityList.value.map(c => ({
    [t('graphIde.labelName')]: c.name,
    [t('graphIde.labelType')]: c.communityType || '',
    [t('graphIde.labelLegalDomain')]: c.domainType || c.legalDomain || '',
    [t('communityEpisode.subDomain')]: c.subDomainType || '',
    [t('communityEpisode.region')]: c.region || c.jurisdiction || '',
    [t('communityEpisode.scenarioType')]: c.scenarioType || c.practiceType || '',
    [t('graphIde.labelMemberCount')]: c.memberCount || 0,
    [t('graphIde.labelSummary')]: c.summary || '',
    [t('graphIde.labelDescription')]: c.description || '',
    [t('common.createdAt')]: c.createdAt || '',
  }))
  downloadCSV(data, `communities-${selectedGraphId.value}.csv`)
}

// ==================== Episode CRUD ====================

const loadEpisodes = async () => {
  if (!selectedGraphId.value) return
  episodeLoading.value = true
  try {
    const skip = (episodePagination.current - 1) * episodePagination.pageSize
    const allEpisodes = await episodeApi.list(selectedGraphId.value, 0, 1000)
    // 过滤搜索
    let filtered = allEpisodes
    if (episodeSearch.value) {
      const kw = episodeSearch.value.toLowerCase()
      filtered = filtered.filter((e: any) =>
        (e.name || '').toLowerCase().includes(kw) ||
        (e.source || '').toLowerCase().includes(kw)
      )
    }
    if (episodeTypeFilter.value) {
      filtered = filtered.filter((e: any) => e.episodeType === episodeTypeFilter.value)
    }
    // 分页
    const total = filtered.length
    const page = filtered.slice(skip, skip + episodePagination.pageSize)
    episodeList.value = page
    episodePagination.total = total
  } catch (e) {
    console.error(t('communityEpisode.loadEpisodesFailed'), e)
    message.error(t('communityEpisode.loadEpisodesFailed'))
  } finally {
    episodeLoading.value = false
  }
}

const openEpisodeModal = (record?: any) => {
  editingEpisode.value = record || null
  if (record) {
    episodeForm.name = record.name || ''
    episodeForm.source = record.source || ''
    episodeForm.sourceDescription = record.sourceDescription || ''
    episodeForm.content = record.content || ''
  } else {
    Object.assign(episodeForm, { name: '', source: '', sourceDescription: '', content: '' })
  }
  episodeModalVisible.value = true
}

const saveEpisode = async () => {
  if (!selectedGraphId.value) return
  if (!episodeForm.source?.trim()) {
    message.warning(t('communityEpisode.pleaseEnterSource'))
    return
  }
  episodeModalLoading.value = true
  try {
    if (editingEpisode.value) {
      message.warning(t('communityEpisode.updateNotImplemented'))
    } else {
      await episodeApi.create(selectedGraphId.value, {
        name: episodeForm.name || undefined,
        source: episodeForm.source,
        sourceDescription: episodeForm.sourceDescription || undefined,
        content: episodeForm.content || undefined,
      })
      message.success(t('common.createSuccess'))
    }
    episodeModalVisible.value = false
    await loadEpisodes()
  } catch (e) {
    console.error(t('communityEpisode.saveEpisodeFailed'), e)
    message.error(t('common.saveFailed'))
  } finally {
    episodeModalLoading.value = false
  }
}

const deleteEpisode = async (uuid: string) => {
  if (!selectedGraphId.value) return
  try {
    await episodeApi.delete(selectedGraphId.value, uuid)
    message.success(t('common.deleteSuccess'))
    await loadEpisodes()
  } catch (e) {
    console.error(t('communityEpisode.deleteEpisodeFailed'), e)
    message.error(t('common.deleteFailed'))
  }
}

const handleEpisodeTableChange = (pag: any) => {
  episodePagination.current = pag.current
  episodePagination.pageSize = pag.pageSize
  loadEpisodes()
}

const exportEpisodes = () => {
  const data = episodeList.value.map(e => ({
    [t('episodeEdit.labelSource')]: e.source || '',
    [t('episodeEdit.labelName')]: e.name || '',
    [t('graphIde.labelType')]: e.episodeType || '',
    [t('graphIde.labelProcessType')]: e.legalProcess || '',
    [t('graphIde.labelStage')]: e.stageLabel || '',
    [t('graphIde.labelStageLevel')]: e.courtLevel || '',
    [t('episodeEdit.labelContent')]: e.content || '',
    [t('common.createdAt')]: e.createdAt || '',
  }))
  downloadCSV(data, `episodes-${selectedGraphId.value}.csv`)
}

// ==================== Helpers ====================

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  try {
    return new Date(dateStr).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch {
    return dateStr
  }
}

const getCommunityColor = (type?: string) => {
  if (!type) return 'default'
  // 从 communityTypes 中查找颜色
  const found = communityTypes.value.find(t => t.typeName === type || t.typeCode === type)
  if (found?.metadata && typeof found.metadata === 'object' && 'color' in found.metadata) {
    return (found.metadata as { color: string }).color
  }
  // 回退到默认颜色映射
  const colors: Record<string, string> = {
    '合同纠纷': 'blue', '侵权纠纷': 'cyan', '物权纠纷': 'geekblue',
    '婚姻家庭': 'magenta', '公司治理': 'purple', '金融证券': 'volcano',
    '房地产': 'orange', '刑事': 'red', '行政': 'green',
  }
  return colors[type] || 'default'
}

const downloadCSV = (data: Record<string, any>[], filename: string) => {
  if (data.length === 0) {
    message.warning(t('communityEpisode.noDataToExport'))
    return
  }
  const headers = Object.keys(data[0])
  const rows = data.map(row => headers.map(h => {
    const val = String(row[h] ?? '')
    return val.includes(',') || val.includes('"') || val.includes('\n')
      ? `"${val.replace(/"/g, '""')}"`
      : val
  }))
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.community-episode-page {
  padding: 24px;
  height: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left .page-title {
  font-size: 20px;
  font-weight: 600;
  margin: 0 0 4px 0;
}

.header-left .page-desc {
  color: #8b949e;
  font-size: 13px;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.management-tabs {
  background: #fff;
  padding: 0 16px;
}

.tab-toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 16px 0;
  flex-wrap: wrap;
}

.tab-toolbar :deep(.ant-btn) {
  white-space: nowrap;
}

.community-content {
  font-size: 13px;
  color: #595959;
  line-height: 1.6;
}
</style>
