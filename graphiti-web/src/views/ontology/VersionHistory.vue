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
            <a-button type="link" size="small" @click="showDetail(record)">{{ $t('ontology.detail') }}</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 详情模态框 -->
    <a-modal
      v-model:open="detailVisible"
      :title="$t('ontology.versionDetail')"
      width="800px"
      :footer="null"
    >
      <a-descriptions v-if="currentRecord" :column="1" bordered size="small">
        <a-descriptions-item :label="$t('ontology.version')">{{ currentRecord.version }}</a-descriptions-item>
        <a-descriptions-item :label="$t('ontology.changeType')">
          <a-tag :color="getChangeTypeColor(currentRecord.changeType)">
            {{ getChangeTypeLabel(currentRecord.changeType) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('ontology.entityType')">{{ currentRecord.entityType }}</a-descriptions-item>
        <a-descriptions-item :label="$t('ontology.changeSummary')">{{ currentRecord.diffSummary }}</a-descriptions-item>
        <a-descriptions-item :label="$t('ontology.changedBy')">{{ currentRecord.changedBy || '-' }}</a-descriptions-item>
        <a-descriptions-item :label="$t('ontology.changedAt')">{{ formatDate(currentRecord.changedAt) }}</a-descriptions-item>
      </a-descriptions>

      <a-divider>{{ $t('ontology.beforeChange') }}</a-divider>
      <pre class="json-view">{{ formatJson(currentRecord?.beforeState) }}</pre>

      <a-divider>{{ $t('ontology.afterChange') }}</a-divider>
      <pre class="json-view">{{ formatJson(currentRecord?.afterState) }}</pre>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ontologyApi, type OntVersionHistoryVO } from '@/api/ontology'

const { t } = useI18n()

const props = defineProps<{
  graphId: string | undefined
}>()

const loading = ref(false)
const historyList = ref<OntVersionHistoryVO[]>([])
const detailVisible = ref(false)
const currentRecord = ref<OntVersionHistoryVO | null>(null)

const columns = [
  { title: t('ontology.version'), dataIndex: 'version', key: 'version', width: 100 },
  { title: t('ontology.changeType'), key: 'changeType', width: 120 },
  { title: t('ontology.entityType'), key: 'entityType', width: 100 },
  { title: t('ontology.changeSummary'), dataIndex: 'diffSummary', key: 'diffSummary', ellipsis: true },
  { title: t('ontology.changedBy'), dataIndex: 'changedBy', key: 'changedBy', width: 100 },
  { title: t('ontology.time'), key: 'changedAt', width: 180 },
  { title: t('ontology.action'), key: 'action', width: 80 }
]

async function loadHistory() {
  if (!props.graphId) return

  loading.value = true
  try {
    historyList.value = await ontologyApi.getVersionHistory(props.graphId)
  } catch (err) {
    console.error(t('ontology.loadFailed'), err)
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
    case 'CLASS_ADDED': return t('ontology.classAdded')
    case 'CLASS_MODIFIED': return t('ontology.classModified')
    case 'CLASS_DELETED': return t('ontology.classDeleted')
    case 'PROPERTY_ADDED': return t('ontology.propertyAdded')
    case 'PROPERTY_MODIFIED': return t('ontology.propertyModified')
    case 'PROPERTY_DELETED': return t('ontology.propertyDeleted')
    case 'CONSTRAINT_ADDED': return t('ontology.constraintAdded')
    case 'CONSTRAINT_MODIFIED': return t('ontology.constraintModified')
    case 'CONSTRAINT_DELETED': return t('ontology.constraintDeleted')
    case 'DEFINITION_CREATED': return t('ontology.definitionCreated')
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
  if (!str) return t('ontology.noData')
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
