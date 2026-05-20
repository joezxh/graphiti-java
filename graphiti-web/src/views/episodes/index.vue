<template>
  <div class="episodes-page">
    <div class="page-header">
      <h1 class="page-title">{{ $t('episodes.title') }}</h1>
      <p class="page-desc">{{ $t('episodes.titleDesc') }}</p>
    </div>

    <a-card class="filter-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-select
            v-model:value="selectedGraphId"
            :placeholder="$t('episodes.selectGraph')"
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
          <a-input-search
            v-model:value="searchKeyword"
            :placeholder="$t('episodes.searchSource')"
            allow-clear
            @search="loadEpisodes"
          />
        </a-col>
        <a-col :span="6">
          <a-space>
            <a-button @click="loadEpisodes">
              <ReloadOutlined /> {{ $t('common.refresh') }}
            </a-button>
          </a-space>
        </a-col>
      </a-row>
    </a-card>

    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="episodeList"
        :loading="loading"
        row-key="uuid"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'source'">
            <div>
              <div class="source-name">{{ record.source }}</div>
              <div v-if="record.sourceDescription" class="source-desc">{{ record.sourceDescription }}</div>
            </div>
          </template>
          <template v-if="column.key === 'content'">
            <span class="content-preview">{{ truncate(record.content, 80) }}</span>
          </template>
          <template v-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-if="column.key === 'episodeType'">
            <a-tag v-if="record.episodeType" :color="getEpisodeColor(record.episodeType)">
              {{ record.episodeType }}
            </a-tag>
            <span v-else style="color: #999">-</span>
          </template>
          <template v-if="column.key === 'legalProcess'">
            <a-tag v-if="record.legalProcess" :color="getLegalProcessColor(record.legalProcess)">
              {{ record.legalProcess }}
            </a-tag>
            <span v-else style="color: #999">-</span>
          </template>
          <template v-if="column.key === 'courtLevel'">
            <a-tag v-if="record.courtLevel" color="purple">{{ record.courtLevel }}</a-tag>
            <span v-else>-</span>
          </template>
          <template v-if="column.key === 'isTrialStage'">
            <a-tag :color="record.isTrialStage ? 'green' : 'default'" size="small">
              {{ record.isTrialStage ? '是' : '否' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'timeRange'">
            <span v-if="record.startTime || record.endTime">
              {{ formatEpisodeTime(record.startTime) }}
              <template v-if="record.startTime && record.endTime"> ~ </template>
              {{ formatEpisodeTime(record.endTime) }}
            </span>
            <span v-else>-</span>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewMentions(record)">
                {{ $t('episodes.mentions') }}
              </a-button>
              <a-popconfirm :title="$t('episodes.confirmDelete')" @confirm="deleteEpisode(record.uuid)">
                <a-button type="link" size="small" danger>
                  {{ $t('common.delete') }}
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 提及详情模态框 -->
    <a-modal
      v-model:open="mentionsVisible"
      :title="$t('episodes.mentionDetails')"
      width="700px"
      :footer="null"
    >
      <a-row :gutter="16" v-if="mentionsData">
        <a-col :span="12">
          <div class="mentions-section">
            <div class="mentions-title">{{ $t('common.nodes') }} ({{ mentionsData.nodes.length }})</div>
            <a-tag
              v-for="n in mentionsData.nodes"
              :key="n.uuid"
              color="blue"
              class="mention-tag"
            >
              {{ n.name }}
              <span class="mention-type">{{ n.type }}</span>
            </a-tag>
            <a-empty v-if="mentionsData.nodes.length === 0" :description="$t('common.noData')" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </div>
        </a-col>
        <a-col :span="12">
          <div class="mentions-section">
            <div class="mentions-title">{{ $t('common.edges') }} ({{ mentionsData.edges.length }})</div>
            <div
              v-for="e in mentionsData.edges"
              :key="e.uuid"
              class="mention-edge"
            >
              {{ e.sourceNodeUuid }} → {{ e.targetNodeUuid }}
              <span class="mention-fact">{{ e.fact }}</span>
            </div>
            <a-empty v-if="mentionsData.edges.length === 0" :description="$t('common.noData')" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </div>
        </a-col>
      </a-row>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { Empty } from 'ant-design-vue'
import { graphApi } from '@/api/graph'
import { episodeApi, type EpisodeListItem, type EpisodeMentions } from '@/api/episode'
import { EPISODE_TYPE_COLORS, type EpisodeV3 } from '@/types/legal-graph-v3'

const { t } = useI18n()

/** V3.0.0: 根据 Episode 类型获取颜色 */
const getEpisodeColor = (type?: string): string => {
  if (!type) return 'default'
  return EPISODE_TYPE_COLORS[type] || 'default'
}

/** V3.0.0: 获取法律程序颜色 */
const getLegalProcessColor = (process?: string): string => {
  const map: Record<string, string> = {
    litigation: 'blue',
    mediation: 'pink',
    arbitration: 'orange',
    execution: 'gray',
  }
  return map[process || ''] || 'default'
}

/** V3.0.0: 格式化 Episode 时间 */
const formatEpisodeTime = (timeStr?: string): string => {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  } catch {
    return timeStr
  }
}

const graphOptions = ref<any[]>([])
const selectedGraphId = ref<string | undefined>(undefined)
const searchKeyword = ref('')
const episodeList = ref<EpisodeListItem[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => t('common.total', { count: total })
})

const columns = [
  { title: 'UUID', dataIndex: 'uuid', key: 'uuid', width: 200, ellipsis: true },
  { title: t('common.name'), dataIndex: 'name', key: 'name', width: 150 },
  { title: t('common.source'), key: 'source' },
  { title: t('episodes.contentPreview'), key: 'content' },
  { title: '类型', dataIndex: 'episodeType', key: 'episodeType', width: 140 },
  { title: '法律程序', dataIndex: 'legalProcess', key: 'legalProcess', width: 100 },
  { title: '审级', dataIndex: 'courtLevel', key: 'courtLevel', width: 80 },
  { title: '审判阶段', dataIndex: 'isTrialStage', key: 'isTrialStage', width: 90 },
  { title: '时间', key: 'timeRange', width: 180 },
  { title: t('common.createdAt'), key: 'createdAt', width: 160 },
  { title: t('common.actions'), key: 'action', width: 150 }
]

const mentionsVisible = ref(false)
const mentionsData = ref<EpisodeMentions | null>(null)

const loadGraphs = async () => {
  try {
    graphOptions.value = await graphApi.getList()
  } catch (err) {
    console.error(t('common.loadGraphListFailed'), err)
  }
}

const loadEpisodes = async () => {
  if (!selectedGraphId.value) return
  loading.value = true
  try {
    const skip = (pagination.current - 1) * pagination.pageSize
    const resp = await episodeApi.list(selectedGraphId.value, skip, pagination.pageSize)
    episodeList.value = resp || []
    pagination.total = resp.length
  } catch (err: any) {
    message.error(err.message || t('common.loadFailed'))
  } finally {
    loading.value = false
  }
}

const onGraphChange = () => {
  pagination.current = 1
  loadEpisodes()
}

const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadEpisodes()
}

const viewMentions = async (record: EpisodeListItem) => {
  if (!selectedGraphId.value) return
  try {
    mentionsData.value = await episodeApi.getMentions(selectedGraphId.value, record.uuid)
    mentionsVisible.value = true
  } catch (err: any) {
    message.error(err.message || t('episodes.loadMentionsFailed'))
  }
}

const deleteEpisode = async (uuid: string) => {
  if (!selectedGraphId.value) return
  try {
    await episodeApi.delete(selectedGraphId.value, uuid)
    message.success(t('common.deleteSuccess'))
    loadEpisodes()
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

.episodes-page {
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

.source-name {
  font-weight: 500;
  color: @text-primary;
}

.source-desc {
  font-size: 12px;
  color: @text-tertiary;
}

.content-preview {
  color: @text-secondary;
  font-size: 13px;
}

.mentions-section {
  .mentions-title {
    font-weight: 600;
    color: @text-primary;
    margin-bottom: 12px;
  }
}

.mention-tag {
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;

  .mention-type {
    font-size: 11px;
    opacity: 0.7;
  }
}

.mention-edge {
  padding: 6px 8px;
  background: @bg-elevated;
  border-radius: 4px;
  margin-bottom: 6px;
  font-size: 12px;
  color: @text-primary;

  .mention-fact {
    display: block;
    color: @text-tertiary;
    font-size: 11px;
    margin-top: 2px;
  }
}
</style>
