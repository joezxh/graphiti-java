/**
 * 版本历史面板 — 时间轴 + 列表视图
 */
<template>
  <div class="version-history-panel">
    <div class="panel-toolbar">
      <a-space>
        <a-button :loading="loading" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button v-if="selectedVersions.length === 2" type="primary" @click="handleDiff">
          <template #icon><BranchesOutlined /></template>
          对比选中版本
        </a-button>
        <a-button v-if="selectedVersions.length === 1" @click="handleRollback">
          <template #icon><UndoOutlined /></template>
          回滚到此版本
        </a-button>
      </a-space>
      <div class="toolbar-right">
        <a-radio-group v-model:value="viewMode" size="small">
          <a-radio-button value="timeline">时间轴</a-radio-button>
          <a-radio-button value="list">列表</a-radio-button>
        </a-radio-group>
      </div>
    </div>

    <!-- 时间轴视图 -->
    <div v-if="viewMode === 'timeline'" class="timeline-view">
      <a-timeline v-if="store.versionHistory.length > 0">
        <a-timeline-item
          v-for="item in store.versionHistory"
          :key="item.id"
          :color="item.changeType === 'CREATE' ? 'green' : item.changeType === 'DELETE' ? 'red' : 'blue'"
        >
          <div class="timeline-item">
            <div class="timeline-header">
              <a-tag :color="changeTypeColor(item.changeType)">{{ changeTypeLabel(item.changeType) }}</a-tag>
              <span class="timeline-version">{{ item.version }}</span>
              <span class="timeline-time">{{ formatDate(item.changedAt) }}</span>
              <a-checkbox
                class="timeline-check"
                :checked="selectedVersions.includes(item.id)"
                @change="toggleVersionSelect(item.id)"
              />
            </div>
            <div class="timeline-content">
              <span class="entity-type">{{ item.entityType }}</span>
              <span class="diff-summary">{{ item.diffSummary || '-' }}</span>
            </div>
            <div class="timeline-by">操作者: {{ item.changedBy || '系统' }}</div>
          </div>
        </a-timeline-item>
      </a-timeline>
      <div v-else class="empty-state">
        <a-empty description="暂无版本历史" />
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-else class="list-view">
      <a-table
        :columns="columns"
        :data-source="store.versionHistory"
        :loading="loading"
        :pagination="{ pageSize: 20 }"
        :row-selection="{ selectedRowKeys: selectedVersions, onChange: (keys: any[]) => selectedVersions = keys }"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'changeType'">
            <a-tag :color="changeTypeColor(record.changeType)">{{ changeTypeLabel(record.changeType) }}</a-tag>
          </template>
          <template v-if="column.key === 'diffSummary'">
            <span :title="record.diffSummary">{{ record.diffSummary || '-' }}</span>
          </template>
          <template v-if="column.key === 'changedAt'">
            {{ formatDate(record.changedAt) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleViewDetail(record)">详情</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 详情 Modal -->
    <a-modal
      v-model:open="detailVisible"
      title="版本详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions v-if="currentRecord" :column="2" bordered size="small">
        <a-descriptions-item label="版本号">{{ currentRecord.version }}</a-descriptions-item>
        <a-descriptions-item label="变更类型">
          <a-tag :color="changeTypeColor(currentRecord.changeType)">{{ changeTypeLabel(currentRecord.changeType) }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="实体类型">{{ currentRecord.entityType }}</a-descriptions-item>
        <a-descriptions-item label="变更者">{{ currentRecord.changedBy || '系统' }}</a-descriptions-item>
        <a-descriptions-item label="变更时间" :span="2">{{ formatDate(currentRecord.changedAt) }}</a-descriptions-item>
        <a-descriptions-item label="变更摘要" :span="2">{{ currentRecord.diffSummary || '-' }}</a-descriptions-item>
        <a-descriptions-item label="变更前" :span="2">
          <pre class="state-pre">{{ formatState(currentRecord.beforeState) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="变更后" :span="2">
          <pre class="state-pre">{{ formatState(currentRecord.afterState) }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, BranchesOutlined, UndoOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'

const props = defineProps<{ graphId: string }>()
const store = useOntologyStore()
const loading = ref(false)
const viewMode = ref<'timeline' | 'list'>('timeline')
const selectedVersions = ref<(string | number)[]>([])
const detailVisible = ref(false)
const currentRecord = ref<any>(null)

const columns = [
  { title: '版本', dataIndex: 'version', key: 'version', width: 100 },
  { title: '变更类型', key: 'changeType', width: 100 },
  { title: '实体类型', dataIndex: 'entityType', key: 'entityType', width: 80 },
  { title: '变更摘要', key: 'diffSummary', ellipsis: true },
  { title: '变更时间', key: 'changedAt', width: 180 },
  { title: '操作', key: 'action', width: 80 }
]

function changeTypeColor(type?: string) {
  return { CREATE: 'green', UPDATE: 'blue', DELETE: 'red' }[type ?? ''] ?? 'default'
}

function changeTypeLabel(type?: string) {
  return { CREATE: '新增', UPDATE: '修改', DELETE: '删除' }[type ?? ''] ?? type ?? '-'
}

function formatDate(date?: string) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

function formatState(state?: string) {
  if (!state) return '-'
  try {
    return JSON.stringify(JSON.parse(state), null, 2)
  } catch {
    return state
  }
}

function toggleVersionSelect(id: number | string) {
  const idx = selectedVersions.value.indexOf(id)
  if (idx >= 0) {
    selectedVersions.value.splice(idx, 1)
  } else {
    if (selectedVersions.value.length >= 2) {
      selectedVersions.value.shift()
    }
    selectedVersions.value.push(id)
  }
}

function handleViewDetail(record: any) {
  currentRecord.value = record
  detailVisible.value = true
}

function handleDiff() {
  if (selectedVersions.value.length !== 2) return
  store.openTab({
    id: `version-diff-${selectedVersions.value.join('-')}`,
    type: 'version-diff',
    title: '版本对比'
  })
}

function handleRollback() {
  if (selectedVersions.value.length !== 1) return
  Modal.confirm({
    title: '确定回滚到此版本？',
    content: '当前版本回滚功能需后端支持，现提供回滚脚本导出',
    okText: '导出回滚脚本',
    onOk() {
      const record = store.versionHistory.find(v => v.id === selectedVersions.value[0])
      const script = {
        action: 'rollback',
        targetVersion: record?.version,
        beforeState: record?.beforeState,
        note: '请将此脚本发送给后端执行回滚操作'
      }
      const blob = new Blob([JSON.stringify(script, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `rollback-script-${record?.version}.json`
      a.click()
      URL.revokeObjectURL(url)
      message.success('回滚脚本已导出，请联系后端执行')
    }
  })
}

async function handleRefresh() {
  loading.value = true
  await store.loadVersionHistory(props.graphId)
  loading.value = false
}

onMounted(() => handleRefresh())
</script>

<style scoped lang="less">
.version-history-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  padding: 16px;

  .panel-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    flex-shrink: 0;
  }

  .timeline-view {
    flex: 1;
    overflow-y: auto;
    padding: 0 16px;

    .timeline-item {
      .timeline-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 4px;

        .timeline-version { font-weight: 600; color: #e6edf3; }
        .timeline-time { font-size: 12px; color: #6e7681; }
        .timeline-check { margin-left: auto; }
      }

      .timeline-content {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 4px;

        .entity-type {
          font-size: 12px;
          color: #8b949e;
          background: #21262d;
          padding: 2px 6px;
          border-radius: 3px;
        }

        .diff-summary { font-size: 13px; color: #e6edf3; }
      }

      .timeline-by {
        font-size: 11px;
        color: #6e7681;
      }
    }
  }

  .list-view {
    flex: 1;
    overflow-y: auto;
  }

  .state-pre {
    background: #0d1117;
    padding: 8px;
    border-radius: 4px;
    font-size: 12px;
    max-height: 200px;
    overflow-y: auto;
    color: #8b949e;
    white-space: pre-wrap;
    margin: 0;
  }

  .empty-state {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 200px;
  }
}
</style>
