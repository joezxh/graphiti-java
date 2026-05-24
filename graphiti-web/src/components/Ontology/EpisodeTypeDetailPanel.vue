<template>
  <div class="episode-type-detail-panel">
    <!-- Tab 切换 -->
    <div class="panel-tabs">
      <div
        class="panel-tab"
        :class="{ active: activeTab === 'info' }"
        @click="activeTab = 'info'"
      >
        类型详情
      </div>
      <div
        class="panel-tab"
        :class="{ active: activeTab === 'instances' }"
        @click="activeTab = 'instances'"
      >
        实例列表
      </div>
    </div>

    <!-- 类型详情 Tab -->
    <div v-if="activeTab === 'info'" class="tab-content">
      <a-descriptions :column="1" bordered size="small" class="dark-descriptions">
        <a-descriptions-item label="类型代码">
          <a-tag color="blue">{{ typeData?.typeCode || '-' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="类型名称">
          {{ typeData?.typeName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="英文名称">
          {{ typeData?.typeNameEn || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="父类型">
          <a-tag v-if="typeData?.parentTypeCode">{{ typeData.parentTypeCode }}</a-tag>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="层级">
          <a-tag color="purple">{{ typeData?.level || 1 }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="流程类型">
          {{ typeData?.processType || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="阶段标签">
          {{ typeData?.stageLabel || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="阶段级别">
          {{ typeData?.stageLevel || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="审查阶段">
          <a-tag :color="typeData?.isReviewStage ? 'green' : 'default'">
            {{ typeData?.isReviewStage ? '是' : '否' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getStatusColor(typeData?.status)">
            {{ typeData?.status || '-' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="排序">
          {{ typeData?.sortOrder ?? '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="实例数量">
          {{ typeData?.instanceCount ?? '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="描述" :span="1">
          <div class="description-text">{{ typeData?.description || '-' }}</div>
        </a-descriptions-item>
      </a-descriptions>

      <!-- 操作按钮 -->
      <div class="panel-actions">
        <a-button type="primary" size="small" @click="handleEdit">
          <template #icon><EditOutlined /></template>
          编辑
        </a-button>
        <a-popconfirm
          title="确定要删除此类型吗？"
          :disabled="!canDelete"
          @confirm="handleDelete"
        >
          <a-button type="primary" danger size="small" :disabled="!canDelete">
            <template #icon><DeleteOutlined /></template>
            删除
          </a-button>
        </a-popconfirm>
        <a-tooltip v-if="deleteCheck?.reason" :title="deleteCheck.reason">
          <InfoCircleOutlined style="color: #d29922; margin-left: 8px" />
        </a-tooltip>
      </div>
    </div>

    <!-- 实例列表 Tab -->
    <div v-else-if="activeTab === 'instances'" class="tab-content">
      <a-table
        :data-source="instanceList"
        :columns="instanceColumns"
        :pagination="false"
        size="small"
        :loading="instancesLoading"
        :scroll="{ x: 'max-content' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="handleNavigateToInstance(record.uuid)">{{ record.name }}</a>
          </template>
          <template v-if="column.key === 'source'">
            <a-tag size="small">{{ record.source }}</a-tag>
          </template>
        </template>
      </a-table>

      <!-- 同步分页控件 -->
      <div class="instance-pagination-bar">
        <a-pagination
          v-model:current="currentPage"
          :total="props.pagination.total"
          :pageSize="props.pagination.pageSize"
          size="small"
          show-less-items
          @change="handlePageChange"
        />
        <a-select
          v-model:value="currentDepth"
          size="small"
          style="width: 70px; margin-left: 8px"
          @change="handleDepthChange"
        >
          <a-select-option :value="1">1跳</a-select-option>
          <a-select-option :value="2">2跳</a-select-option>
          <a-select-option :value="3">3跳</a-select-option>
        </a-select>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import {
  EditOutlined,
  DeleteOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'
import { episodeTypeApi } from '@/api/metadata'
import type { OntEpisodeTypeVO, EpisodeTypeDeleteCheckVO } from '@/api/metadata'

const props = defineProps<{
  graphId: string
  typeId: number
  typeData?: OntEpisodeTypeVO
  pagination: {
    page: number
    pageSize: number
    total: number
    totalPages: number
    hasNextPage: boolean
  }
  depth: number
}>()

const emit = defineEmits<{
  (e: 'edit-type', typeId: number): void
  (e: 'delete-type', typeId: number): void
  (e: 'navigate-to-instance', uuid: string): void
  (e: 'pagination-change', page: number): void
  (e: 'depth-change', depth: number): void
}>()

const activeTab = ref<'info' | 'instances'>('info')
const deleteCheck = ref<EpisodeTypeDeleteCheckVO | null>(null)
const instanceList = ref<any[]>([])
const instancesLoading = ref(false)

const canDelete = computed(() => deleteCheck.value?.canDelete ?? false)

// 分页和深度计算属性（双向绑定风格）
const currentPage = computed({
  get: () => props.pagination.page,
  set: (val) => emit('pagination-change', val)
})

const currentDepth = computed({
  get: () => props.depth,
  set: (val) => emit('depth-change', val)
})

function handlePageChange(page: number) {
  emit('pagination-change', page)
}

function handleDepthChange(depth: number) {
  emit('depth-change', depth)
}

const instanceColumns = [
  { title: '名称', dataIndex: 'name', key: 'name', width: 'auto', minWidth: 200 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 100, ellipsis: false },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 160, ellipsis: false }
]

function getStatusColor(status?: string): string {
  switch (status) {
    case 'ACTIVE': return 'green'
    case 'INACTIVE': return 'orange'
    case 'DEPRECATED': return 'red'
    default: return 'default'
  }
}

function handleEdit() {
  emit('edit-type', props.typeId)
}

function handleDelete() {
  emit('delete-type', props.typeId)
}

function handleNavigateToInstance(uuid: string) {
  emit('navigate-to-instance', uuid)
}

async function loadDeleteCheck() {
  try {
    const res = await episodeTypeApi.checkDelete(props.graphId, props.typeId)
    deleteCheck.value = res
  } catch (e) {
    console.error('删除检查失败:', e)
  }
}

async function loadInstances() {
  instancesLoading.value = true
  try {
    const res = await episodeTypeApi.getInstances(
      props.graphId,
      props.typeId,
      props.pagination.page,
      props.pagination.pageSize
    )
    instanceList.value = res.episodes || []
  } catch (e) {
    console.error('加载实例列表失败:', e)
    instanceList.value = []
  } finally {
    instancesLoading.value = false
  }
}

// 监听分页变化自动刷新
watch(() => props.pagination.page, () => {
  if (activeTab.value === 'instances') {
    loadInstances()
  }
}, { immediate: false })

watch(() => props.typeId, () => {
  if (props.typeId) {
    activeTab.value = 'info'
    deleteCheck.value = null
    instanceList.value = []
    loadDeleteCheck()
  }
}, { immediate: true })

watch(activeTab, (tab: 'info' | 'instances') => {
  if (tab === 'instances') {
    loadInstances()
  }
})
</script>

<style scoped lang="less">
.episode-type-detail-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  .panel-tabs {
    display: flex;
    border-bottom: 1px solid #21262d;
    flex-shrink: 0;

    .panel-tab {
      padding: 8px 16px;
      font-size: 13px;
      color: #8b949e;
      cursor: pointer;
      border-bottom: 2px solid transparent;
      transition: all 0.15s;

      &:hover {
        color: #e6edf3;
      }

      &.active {
        color: #58a6ff;
        border-bottom-color: #58a6ff;
      }
    }
  }

  .tab-content {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
  }

  // 实例表格不换行样式
  :deep(.ant-table) {
    .ant-table-cell {
      white-space: nowrap;
      overflow: visible;
      text-overflow: unset;
    }

    .ant-table-tbody > tr > td {
      white-space: nowrap;
    }
  }

  .description-text {
    white-space: pre-wrap;
    word-break: break-word;
    font-size: 13px;
    line-height: 1.6;
    color: #c9d1d9;
  }

  .panel-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #21262d;
  }

  .instance-pagination-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 0;
    margin-top: 8px;
    border-top: 1px solid #21262d;
  }
}

:deep(.ant-descriptions) {
  .ant-descriptions-item-label {
    background: #161b22;
    color: #8b949e;
    font-size: 12px;
    width: 100px;
  }

  .ant-descriptions-item-content {
    background: #0d1117;
    color: #e6edf3;
    font-size: 13px;
  }

  .ant-descriptions-bordered .ant-descriptions-item {
    border-color: #21262d;
  }
}
</style>
