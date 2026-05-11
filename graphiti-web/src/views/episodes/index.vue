<template>
  <div class="episodes-page">
    <div class="page-header">
      <h1 class="page-title">Episode 管理</h1>
      <p class="page-desc">浏览和管理图谱中的 Episode（事件/对话片段）</p>
    </div>

    <a-card class="filter-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-select
            v-model:value="selectedGraphId"
            placeholder="选择图谱"
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
            placeholder="搜索来源"
            allow-clear
            @search="loadEpisodes"
          />
        </a-col>
        <a-col :span="6">
          <a-space>
            <a-button @click="loadEpisodes">
              <ReloadOutlined /> 刷新
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
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewMentions(record)">
                提及
              </a-button>
              <a-popconfirm title="确定删除该 Episode？" @confirm="deleteEpisode(record.uuid)">
                <a-button type="link" size="small" danger>
                  删除
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
      title="Episode 提及的节点和边"
      width="700px"
      :footer="null"
    >
      <a-row :gutter="16" v-if="mentionsData">
        <a-col :span="12">
          <div class="mentions-section">
            <div class="mentions-title">节点 ({{ mentionsData.nodes.length }})</div>
            <a-tag
              v-for="n in mentionsData.nodes"
              :key="n.uuid"
              color="blue"
              class="mention-tag"
            >
              {{ n.name }}
              <span class="mention-type">{{ n.type }}</span>
            </a-tag>
            <a-empty v-if="mentionsData.nodes.length === 0" description="无" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </div>
        </a-col>
        <a-col :span="12">
          <div class="mentions-section">
            <div class="mentions-title">边 ({{ mentionsData.edges.length }})</div>
            <div
              v-for="e in mentionsData.edges"
              :key="e.uuid"
              class="mention-edge"
            >
              {{ e.sourceNodeUuid }} → {{ e.targetNodeUuid }}
              <span class="mention-fact">{{ e.fact }}</span>
            </div>
            <a-empty v-if="mentionsData.edges.length === 0" description="无" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
          </div>
        </a-col>
      </a-row>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { Empty } from 'ant-design-vue'
import { graphApi } from '@/api/graph'
import { episodeApi, type EpisodeListItem, type EpisodeMentions } from '@/api/episode'

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
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  { title: 'UUID', dataIndex: 'uuid', key: 'uuid', width: 200, ellipsis: true },
  { title: '名称', dataIndex: 'name', key: 'name', width: 150 },
  { title: '来源', key: 'source' },
  { title: '内容预览', key: 'content' },
  { title: '创建时间', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 150 }
]

const mentionsVisible = ref(false)
const mentionsData = ref<EpisodeMentions | null>(null)

const loadGraphs = async () => {
  try {
    graphOptions.value = await graphApi.getList()
  } catch (err) {
    console.error('加载图谱列表失败', err)
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
    message.error(err.message || '加载失败')
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
    message.error(err.message || '加载提及失败')
  }
}

const deleteEpisode = async (uuid: string) => {
  if (!selectedGraphId.value) return
  try {
    await episodeApi.delete(selectedGraphId.value, uuid)
    message.success('删除成功')
    loadEpisodes()
  } catch (err: any) {
    message.error(err.message || '删除失败')
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
