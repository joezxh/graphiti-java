<template>
  <div class="edges-page">
    <div class="page-header">
      <h1 class="page-title">{{ $t('edges.title') }}</h1>
      <p class="page-desc">{{ $t('edges.titleDesc') }}</p>
    </div>

    <a-card class="filter-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-select
            v-model:value="selectedGraphId"
            :placeholder="$t('edges.selectGraph')"
            style="width: 100%"
            allow-clear
            @change="onGraphChange"
          >
            <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
              {{ g.name }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="4">
          <a-button type="primary" @click="showCreateModal">
            <PlusOutlined /> {{ $t('edges.createEdge') }}
          </a-button>
        </a-col>
        <a-col :span="6">
          <a-button @click="loadEdges">
            <ReloadOutlined /> {{ $t('common.refresh') }}
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="edgeList"
        :loading="loading"
        row-key="uuid"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'fact'">
            <span class="fact-text">{{ record.fact || record.name || '-' }}</span>
          </template>
          <template v-if="column.key === 'source'">
            <a-tag color="blue">{{ truncate(record.sourceNodeUuid, 8) }}</a-tag>
          </template>
          <template v-if="column.key === 'target'">
            <a-tag color="purple">{{ truncate(record.targetNodeUuid, 8) }}</a-tag>
          </template>
          <template v-if="column.key === 'episodes'">
            <span class="episode-count">{{ record.episodes?.length || 0 }} {{ t('edges.episodes') }}</span>
          </template>
          <template v-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewDetail(record)">{{ $t('common.view') }}</a-button>
                <a-popconfirm :title="t('edges.confirmDelete')" @confirm="deleteEdge(record.uuid)">
                <a-button type="link" size="small" danger>{{ $t('common.delete') }}</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建边模态框 -->
    <a-modal
      v-model:open="createVisible"
      :title="$t('edges.createEdge')"
      @ok="handleCreate"
      :confirm-loading="creating"
    >
      <a-form :model="createForm" layout="vertical">
        <a-form-item :label="$t('edges.sourceNode')" required>
          <a-input v-model:value="createForm.sourceNodeUuid" :placeholder="t('edges.pleaseInputSource')" />
        </a-form-item>
        <a-form-item :label="$t('edges.targetNode')" required>
          <a-input v-model:value="createForm.targetNodeUuid" :placeholder="t('edges.pleaseInputTarget')" />
        </a-form-item>
        <a-form-item :label="$t('edges.name')">
          <a-input v-model:value="createForm.name" :placeholder="t('edges.pleaseInputFact')" />
        </a-form-item>
        <a-form-item :label="$t('edges.fact')">
          <a-textarea
            v-model:value="createForm.fact"
            :placeholder="t('edges.pleaseInputFact')"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 边详情模态框 -->
    <a-modal
      v-model:open="detailVisible"
      :title="$t('edges.edgeDetails')"
      :footer="null"
    >
      <a-descriptions :column="2" bordered size="small" v-if="detailData">
        <a-descriptions-item :label="$t('edges.uuid')" :span="2">{{ detailData.uuid }}</a-descriptions-item>
        <a-descriptions-item :label="$t('edges.name')">{{ detailData.name || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('edges.sourceNode')">{{ detailData.sourceNodeUuid }}</a-descriptions-item>
        <a-descriptions-item :label="$t('edges.targetNode')">{{ detailData.targetNodeUuid }}</a-descriptions-item>
        <a-descriptions-item :label="$t('edges.fact')" :span="2">{{ detailData.fact }}</a-descriptions-item>
        <a-descriptions-item :label="$t('edges.createdAt')">{{ formatDate(detailData.createdAt) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('edges.episodeCount')">{{ detailData.episodes?.length || 0 }}</a-descriptions-item>
        <a-descriptions-item :label="$t('edges.validFrom')" :span="2">{{ formatDate(detailData.validAt) }}</a-descriptions-item>
        <a-descriptions-item :label="$t('edges.validTo')" :span="2">{{ formatDate(detailData.invalidAt) }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import { edgeApi, type EdgeListItem, type EdgeDetailResp, type CreateEdgeReq } from '@/api/edge'

const { t } = useI18n()

const graphOptions = ref<any[]>([])
const selectedGraphId = ref<string | undefined>(undefined)
const edgeList = ref<EdgeListItem[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => t('common.total', { total })
})

const columns = [
  { title: t('edges.uuid'), dataIndex: 'uuid', key: 'uuid', width: 200, ellipsis: true },
  { title: t('edges.name'), dataIndex: 'name', key: 'name', width: 120 },
  { title: t('edges.fact'), key: 'fact' },
  { title: t('edges.sourceNode'), key: 'source', width: 100 },
  { title: t('edges.targetNode'), key: 'target', width: 100 },
  { title: t('edges.episodes'), key: 'episodes', width: 80, align: 'center' },
  { title: t('edges.createdAt'), key: 'createdAt', width: 160 },
  { title: t('common.actions'), key: 'action', width: 150 }
]

// 创建边
const createVisible = ref(false)
const creating = ref(false)
const createForm = reactive<CreateEdgeReq>({
  sourceNodeUuid: '',
  targetNodeUuid: '',
  name: '',
  fact: ''
})

// 详情
const detailVisible = ref(false)
const detailData = ref<EdgeDetailResp | null>(null)

const loadGraphs = async () => {
  try {
    graphOptions.value = await graphApi.getList()
  } catch (err) {
    console.error(t('edges.loadGraphFailed'), err)
  }
}

const loadEdges = async () => {
  if (!selectedGraphId.value) return
  loading.value = true
  try {
    const resp = await edgeApi.list(selectedGraphId.value)
    edgeList.value = resp || []
    pagination.total = resp?.length || 0
  } catch (err: any) {
    message.error(err.message || t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onGraphChange = () => {
  pagination.current = 1
  loadEdges()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadEdges()
}

const showCreateModal = () => {
  createForm.sourceNodeUuid = ''
  createForm.targetNodeUuid = ''
  createForm.name = ''
  createForm.fact = ''
  createVisible.value = true
}

const handleCreate = async () => {
  if (!selectedGraphId.value) {
    message.error(t('edges.selectGraph'))
    return
  }
  if (!createForm.sourceNodeUuid || !createForm.targetNodeUuid) {
    message.error(t('edges.pleaseInputSource') + ' ' + t('edges.pleaseInputTarget'))
    return
  }
  creating.value = true
  try {
    await edgeApi.create(selectedGraphId.value, createForm)
    message.success(t('common.createSuccess'))
    createVisible.value = false
    loadEdges()
  } catch (err: any) {
    message.error(err.message || t('common.createFailed'))
  } finally {
    creating.value = false
  }
}

const viewDetail = async (record: EdgeListItem) => {
  if (!selectedGraphId.value) return
  try {
    detailData.value = await edgeApi.get(selectedGraphId.value, record.uuid)
    detailVisible.value = true
  } catch (err: any) {
    message.error(err.message || t('edges.loadDetailFailed'))
  }
}

const deleteEdge = async (uuid: string) => {
  if (!selectedGraphId.value) return
  try {
    await edgeApi.delete(selectedGraphId.value, uuid)
    message.success(t('common.deleteSuccess'))
    loadEdges()
  } catch (err: any) {
    message.error(err.message || t('common.deleteFailed'))
  }
}

const truncate = (str: string | undefined, len: number): string => {
  if (!str) return '-'
  return str.length > len ? str.slice(0, len) + '...' : str
}

const formatDate = (date: string | undefined): string => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadGraphs()
})
</script>

<style scoped lang="less">
@import '@/assets/styles/dark.less';

.edges-page {
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

.filter-card, .table-card {
  background: @bg-container;
  border: 1px solid @border-color;
  margin-bottom: 16px;
}

.fact-text {
  color: @text-secondary;
  font-size: 13px;
  max-width: 300px;
  display: inline-block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.episode-count {
  color: @text-tertiary;
  font-size: 12px;
}
</style>
