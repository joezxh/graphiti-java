<template>
  <div class="version-history">
    <a-table
      :columns="columns"
      :data-source="historyList"
      :loading="loading"
      :pagination="{ pageSize: 10 }"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'changeType'">
          <a-tag :color="getChangeTypeColor(record.changeType)">{{ getChangeTypeLabel(record.changeType) }}</a-tag>
        </template>
        <template v-if="column.key === 'entityType'">
          <a-tag>{{ record.entityType }}</a-tag>
        </template>
        <template v-if="column.key === 'severity'">
          <a-tag v-if="record.severity" :color="getSeverityColor(record.severity)">{{ record.severity }}</a-tag>
          <span v-else>-</span>
        </template>
        <template v-if="column.key === 'diffSummary'">
          <span :title="record.diffSummary">{{ record.diffSummary }}</span>
        </template>
        <template v-if="column.key === 'changedAt'">
          {{ formatDate(record.changedAt) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 详情模态框 -->
    <a-modal
      v-model:open="detailVisible"
      title="变更详情"
      width="800px"
      :footer="null"
    >
      <a-descriptions v-if="currentRecord" :column="1" bordered size="small">
        <a-descriptions-item label="版本">{{ currentRecord.version }}</a-descriptions-item>
        <a-descriptions-item label="变更类型">
          <a-tag :color="getChangeTypeColor(currentRecord.changeType)">
            {{ getChangeTypeLabel(currentRecord.changeType) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="实体类型">{{ currentRecord.entityType }}</a-descriptions-item>
        <a-descriptions-item label="变更摘要">{{ currentRecord.diffSummary }}</a-descriptions-item>
        <a-descriptions-item label="变更人">{{ currentRecord.changedBy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="变更时间">{{ formatDate(currentRecord.changedAt) }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>变更前</a-divider>
      <pre class="json-view">{{ formatJson(currentRecord?.beforeState) }}</pre>

      <a-divider>变更后</a-divider>
      <pre class="json-view">{{ formatJson(currentRecord?.afterState) }}</pre>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ontologyApi, type OntVersionHistoryVO } from '@/api/ontology'

const props = defineProps<{
  graphId: string | undefined
}>()

const loading = ref(false)
const historyList = ref<OntVersionHistoryVO[]>([])
const detailVisible = ref(false)
const currentRecord = ref<OntVersionHistoryVO | null>(null)

const columns = [
  { title: '版本', dataIndex: 'version', key: 'version', width: 100 },
  { title: '变更类型', key: 'changeType', width: 120 },
  { title: '实体类型', key: 'entityType', width: 100 },
  { title: '变更摘要', dataIndex: 'diffSummary', key: 'diffSummary', ellipsis: true },
  { title: '变更人', dataIndex: 'changedBy', key: 'changedBy', width: 100 },
  { title: '时间', key: 'changedAt', width: 180 },
  { title: '操作', key: 'action', width: 80 }
]

async function loadHistory() {
  if (!props.graphId) return

  loading.value = true
  try {
    historyList.value = await ontologyApi.getVersionHistory(props.graphId)
  } catch (err) {
    console.error('加载版本历史失败', err)
    historyList.value = []
  } finally {
    loading.value = false
  }
}

function showDetail(record: OntVersionHistoryVO) {
  currentRecord.value = record
  detailVisible.value = true
}

function getChangeTypeColor(type: string) {
  switch (type) {
    case 'CLASS_ADDED': return 'green'
    case 'CLASS_MODIFIED': return 'blue'
    case 'CLASS_DELETED': return 'red'
    case 'PROPERTY_ADDED': return 'green'
    case 'PROPERTY_MODIFIED': return 'blue'
    case 'PROPERTY_DELETED': return 'red'
    case 'CONSTRAINT_ADDED': return 'green'
    case 'CONSTRAINT_MODIFIED': return 'blue'
    case 'CONSTRAINT_DELETED': return 'red'
    case 'DEFINITION_CREATED': return 'purple'
    default: return 'default'
  }
}

function getChangeTypeLabel(type: string) {
  switch (type) {
    case 'CLASS_ADDED': return '新增类'
    case 'CLASS_MODIFIED': return '修改类'
    case 'CLASS_DELETED': return '删除类'
    case 'PROPERTY_ADDED': return '新增属性'
    case 'PROPERTY_MODIFIED': return '修改属性'
    case 'PROPERTY_DELETED': return '删除属性'
    case 'CONSTRAINT_ADDED': return '新增约束'
    case 'CONSTRAINT_MODIFIED': return '修改约束'
    case 'CONSTRAINT_DELETED': return '删除约束'
    case 'DEFINITION_CREATED': return '创建本体'
    default: return type
  }
}

function getSeverityColor(severity: string) {
  switch (severity) {
    case 'ERROR': return 'red'
    case 'WARNING': return 'orange'
    case 'INFO': return 'blue'
    default: return 'default'
  }
}

function formatDate(date: string | undefined) {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

function formatJson(str: string | undefined) {
  if (!str) return '（无）'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

onMounted(() => {
  loadHistory()
})

watch(() => props.graphId, () => {
  loadHistory()
})
</script>

<style scoped lang="less">
.version-history {
  .json-view {
    background: #f5f5f5;
    padding: 12px;
    border-radius: 4px;
    max-height: 300px;
    overflow: auto;
    font-size: 12px;
    line-height: 1.5;
  }
}
</style>
