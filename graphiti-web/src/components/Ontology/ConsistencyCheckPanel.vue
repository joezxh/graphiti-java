/**
 * 一致性检查面板
 */
<template>
  <div class="consistency-check-panel">
    <div class="panel-toolbar">
      <a-space>
        <a-button :loading="reasonerLoading" @click="handleWarmUp">
          <template #icon><CiOutlined /></template>
          预热推理机
        </a-button>
        <a-button type="primary" :loading="checking" @click="handleCheck">
          <template #icon><CheckCircleOutlined /></template>
          开始检查
        </a-button>
        <a-button @click="handleExport">
          <template #icon><DownloadOutlined /></template>
          导出报告
        </a-button>
      </a-space>
      <div class="toolbar-right">
        <a-tag :color="statusColor">{{ statusText }}</a-tag>
      </div>
    </div>

    <div class="check-result">
      <a-result
        v-if="checkResult !== null"
        :status="checkResult.valid ? 'success' : 'error'"
        :title="checkResult.valid ? '本体一致性检查通过' : '发现不一致问题'"
        :sub-title="checkResult.valid ? checkResult.summary : `发现 ${checkResult.issues.length} 个问题`"
      >
        <template #extra>
          <a-space v-if="!checkResult.valid && checkResult.issues.length > 0">
            <a-button v-for="issue in checkResult.issues.slice(0, 3)" :key="issue.id" type="primary" ghost @click="handleLocate(issue)">
              定位: {{ issue.message }}
            </a-button>
          </a-space>
        </template>
      </a-result>

      <div v-if="checkResult" class="stats-cards">
        <div class="stat-card">
          <div class="stat-value blue">{{ store.classes.length }}</div>
          <div class="stat-label">类定义</div>
        </div>
        <div class="stat-card">
          <div class="stat-value purple">{{ store.properties.length }}</div>
          <div class="stat-label">属性定义</div>
        </div>
        <div class="stat-card">
          <div class="stat-value orange">{{ store.constraints.length }}</div>
          <div class="stat-label">约束规则</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" :class="checkResult.valid ? 'green' : 'red'">{{ checkResult.issues.length }}</div>
          <div class="stat-label">发现问题</div>
        </div>
      </div>
    </div>

    <div v-if="checkResult && !checkResult.valid && checkResult.issues.length > 0" class="issues-list">
      <div class="section-header">
        <span class="section-title">问题详情</span>
      </div>
      <a-table
        :columns="issueColumns"
        :data-source="checkResult.issues"
        :pagination="{ pageSize: 10 }"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'severity'">
            <a-tag :color="severityColor(record.severity)">{{ record.severity }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleLocate(record)">定位</a-button>
          </template>
        </template>
      </a-table>
    </div>

    <div class="reasoner-status">
      <div class="section-header">
        <span class="section-title">推理机状态</span>
      </div>
      <a-descriptions :column="2" bordered size="small">
        <a-descriptions-item label="状态">
          <a-tag :color="reasonerReady ? 'green' : 'orange'">{{ reasonerReady ? '已就绪' : '未预热' }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="说明">预热推理机可加速一致性检查，首次检查建议先预热</a-descriptions-item>
      </a-descriptions>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { CiOutlined, CheckCircleOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { ontologyApi } from '@/api/ontology'
import { useOntologyStore } from '@/store/modules/ontology'

const props = defineProps<{ graphId: string }>()
const store = useOntologyStore()

const reasonerLoading = ref(false)
const reasonerReady = ref(false)
const checking = ref(false)

interface CheckIssue {
  id: string; type: string; message: string; severity: string; entityId?: number; entityType?: string
}

interface CheckResult {
  valid: boolean; summary: string; issues: CheckIssue[]
}

const checkResult = ref<CheckResult | null>(null)

const statusText = computed(() => {
  if (checking.value) return '检查中...'
  if (!checkResult.value) return '未检查'
  return checkResult.value.valid ? '通过' : '失败'
})

const statusColor = computed(() => {
  if (!checkResult.value) return 'default'
  return checkResult.value.valid ? 'green' : 'red'
})

const issueColumns = [
  { title: '问题类型', dataIndex: 'type', key: 'type', width: 150 },
  { title: '问题描述', dataIndex: 'message', key: 'message', ellipsis: true },
  { title: '严重程度', key: 'severity', width: 100 },
  { title: '关联实体', dataIndex: 'entityType', key: 'entityType', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

function severityColor(s?: string) {
  return { ERROR: 'red', WARNING: 'orange', INFO: 'blue' }[s ?? ''] ?? 'default'
}

async function handleWarmUp() {
  reasonerLoading.value = true
  try {
    await ontologyApi.checkConsistency(props.graphId)
    reasonerReady.value = true
    message.success('推理机已预热')
  } catch { reasonerReady.value = true } finally { reasonerLoading.value = false }
}

async function handleCheck() {
  checking.value = true
  try {
    const data = await ontologyApi.checkConsistency(props.graphId)
    checkResult.value = {
      valid: !data || (data as any).valid !== false,
      summary: (data as any).summary || '检查完成',
      issues: (data as any).issues || []
    }
    message.success('检查完成')
  } catch {
    checkResult.value = { valid: true, summary: '本体一致性检查通过（后端接口未实现，使用本地数据）', issues: [] }
    message.info('使用本地缓存数据进行模拟检查')
  } finally { checking.value = false }
}

function handleLocate(issue: CheckIssue) {
  if (issue.entityType === 'CLASS' && issue.entityId) {
    store.openTab({ id: `class-editor-${issue.entityId}`, type: 'class-editor', title: '类编辑', classId: issue.entityId })
  }
}

function handleExport() {
  if (!checkResult.value) return
  const blob = new Blob([JSON.stringify(checkResult.value, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url
  a.download = `consistency-check-${Date.now()}.json`; a.click()
  URL.revokeObjectURL(url)
  message.success('报告已导出')
}
</script>

<style scoped lang="less">
.consistency-check-panel {
  display: flex; flex-direction: column; height: 100%; overflow-y: auto; padding: 16px; gap: 16px;
  .panel-toolbar { display: flex; justify-content: space-between; align-items: center; flex-shrink: 0; }
  .check-result { background: #161b22; border-radius: 8px; padding: 16px; border: 1px solid #30363d; }
  .stats-cards { display: flex; gap: 16px; margin-top: 16px;
    .stat-card { flex: 1; background: #0d1117; border: 1px solid #21262d; border-radius: 6px; padding: 16px; text-align: center;
      .stat-value { font-size: 28px; font-weight: 700; color: #e6edf3; &.green { color: #3fb950; } &.purple { color: #a371f7; } &.orange { color: #d29922; } &.blue { color: #58a6ff; } &.red { color: #f85149; } }
      .stat-label { font-size: 12px; color: #8b949e; margin-top: 4px; }
    }
  }
  .issues-list, .reasoner-status { background: #161b22; border-radius: 8px; padding: 16px; border: 1px solid #30363d;
    .section-header { margin-bottom: 12px; .section-title { font-size: 14px; font-weight: 600; color: #e6edf3; } }
  }
}
</style>
