/**
 * 版本对比工具 — 三栏Diff视图
 */
<template>
  <div class="version-diff-viewer">
    <div class="panel-toolbar">
      <a-space>
        <a-button @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-divider type="vertical" />
        <span style="color: #8b949e; font-size: 13px">选择要对比的版本:</span>
        <a-select v-model:value="leftVersion" placeholder="版本A" style="width: 120px" :options="versionOptions" />
        <span style="color: #6e7681">←→</span>
        <a-select v-model:value="rightVersion" placeholder="版本B" style="width: 120px" :options="versionOptions" />
        <a-button type="primary" :disabled="!leftVersion || !rightVersion" @click="handleCompare">
          对比
        </a-button>
      </a-space>
    </div>

    <!-- Diff分类Tab -->
    <a-tabs v-if="diffResult" v-model:activeKey="activeDiffTab" class="diff-tabs">
      <a-tab-pane key="all" tab="全部变更" />
      <a-tab-pane key="class" tab="类变更" />
      <a-tab-pane key="property" tab="属性变更" />
      <a-tab-pane key="constraint" tab="约束变更" />
    </a-tabs>

    <!-- Diff 结果 -->
    <div v-if="diffResult" class="diff-content">
      <!-- 统计栏 -->
      <div class="diff-stats">
        <a-space>
          <a-tag color="green">+{{ diffResult.added.length }} 新增字段</a-tag>
          <a-tag color="red">-{{ diffResult.removed.length }} 删除字段</a-tag>
          <a-tag color="yellow">~{{ diffResult.modified.length }} 修改字段</a-tag>
        </a-space>
      </div>

      <!-- 版本信息 -->
      <div class="version-info">
        <a-descriptions :column="2" size="small" bordered>
          <a-descriptions-item label="版本A">
            <a-tag>{{ leftRecord?.version }}</a-tag>
            <span class="meta">{{ leftRecord?.changeType }} · {{ leftRecord?.entityType }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="版本B">
            <a-tag>{{ rightRecord?.version }}</a-tag>
            <span class="meta">{{ rightRecord?.changeType }} · {{ rightRecord?.entityType }}</span>
          </a-descriptions-item>
        </a-descriptions>
      </div>

      <!-- 字段级Diff -->
      <div class="diff-table">
        <div class="diff-header">
          <span class="header-cell" style="flex: 1">字段</span>
          <span class="header-cell" style="flex: 2">版本A</span>
          <span class="header-cell" style="flex: 2">版本B</span>
        </div>
        <div v-for="item in displayItems" :key="item.field" class="diff-row" :class="item.op">
          <span class="row-cell field-name" style="flex: 1">
            <a-tag :color="opColor(item.op)">{{ opLabel(item.op) }}</a-tag>
            {{ item.field }}
          </span>
          <span class="row-cell old-value" style="flex: 2">
            <pre v-if="item.oldValue !== undefined">{{ formatVal(item.oldValue) }}</pre>
            <span v-else class="null">—</span>
          </span>
          <span class="row-cell new-value" style="flex: 2">
            <pre v-if="item.newValue !== undefined">{{ formatVal(item.newValue) }}</pre>
            <span v-else class="null">—</span>
          </span>
        </div>
        <div v-if="displayItems.length === 0" class="diff-empty">
          <a-empty description="无字段级差异" />
        </div>
      </div>
    </div>

    <div v-else class="empty-diff">
      <a-empty description="请选择两个版本进行对比" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { parseStateDiff } from '@/utils/ontologyDiff'
import type { FieldDiff } from '@/utils/ontologyDiff'

const props = defineProps<{ graphId: string }>()
const store = useOntologyStore()

const leftVersion = ref<string | number | undefined>(undefined)
const rightVersion = ref<string | number | undefined>(undefined)
const activeDiffTab = ref('all')
const diffResult = ref<{ added: FieldDiff[]; removed: FieldDiff[]; modified: FieldDiff[] } | null>(null)
const leftRecord = ref<any>(null)
const rightRecord = ref<any>(null)

const versionOptions = computed(() =>
  store.versionHistory.map(v => ({ label: `${v.version} · ${v.changeType}`, value: v.id }))
)

const displayItems = computed(() => {
  if (!diffResult.value) return []
  let items: FieldDiff[] = []
  switch (activeDiffTab.value) {
    case 'added': items = diffResult.value.added; break
    case 'removed': items = diffResult.value.removed; break
    case 'modified': items = diffResult.value.modified; break
    default: items = [...diffResult.value.added, ...diffResult.value.removed, ...diffResult.value.modified]
  }
  return items
})

function opLabel(op?: string) {
  return { added: '新增', removed: '删除', modified: '修改', unchanged: '不变' }[op ?? ''] ?? op
}

function opColor(op?: string) {
  return { added: 'green', removed: 'red', modified: 'orange', unchanged: 'default' }[op ?? ''] ?? 'default'
}

function formatVal(val: any) {
  if (val === undefined || val === null) return '—'
  if (typeof val === 'object') return JSON.stringify(val, null, 2)
  return String(val)
}

function handleCompare() {
  if (!leftVersion.value || !rightVersion.value) return
  const v1 = store.versionHistory.find(v => v.id === leftVersion.value)
  const v2 = store.versionHistory.find(v => v.id === rightVersion.value)
  if (!v1 || !v2) return

  leftRecord.value = v1
  rightRecord.value = v2

  // 字段级Diff：比较两个版本的 afterState
  const diffs = parseStateDiff(v1.afterState, v2.afterState)
  const result = { added: [] as FieldDiff[], removed: [] as FieldDiff[], modified: [] as FieldDiff[] }
  for (const d of diffs) {
    if (d.op === 'added') result.added.push(d)
    else if (d.op === 'removed') result.removed.push(d)
    else if (d.op === 'modified') result.modified.push(d)
  }
  diffResult.value = result
}

function handleRefresh() {
  store.loadVersionHistory(props.graphId)
  diffResult.value = null
  leftRecord.value = null
  rightRecord.value = null
}
</script>

<style scoped lang="less">
.version-diff-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  .panel-toolbar {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    background: #161b22;
    border-bottom: 1px solid #30363d;
    flex-shrink: 0;
  }

  .diff-tabs {
    background: #161b22;
    flex-shrink: 0;

    :deep(.ant-tabs-nav) {
      margin: 0;
      padding: 0 16px;
      background: #161b22;

      .ant-tabs-tab { color: #8b949e; font-size: 13px; &.ant-tabs-tab-active { color: #e6edf3; } }
    }
  }

  .diff-stats {
    padding: 8px 16px;
    background: #161b22;
    border-bottom: 1px solid #21262d;
  }

  .version-info {
    padding: 12px 16px;
    background: #161b22;
    border-bottom: 1px solid #21262d;

    .meta { color: #8b949e; font-size: 12px; margin-left: 8px; }
  }

  .diff-table {
    flex: 1;
    overflow-y: auto;
    background: #0d1117;

    .diff-header {
      display: flex;
      padding: 8px 16px;
      background: #161b22;
      border-bottom: 1px solid #30363d;
      position: sticky;
      top: 0;
      z-index: 1;

      .header-cell {
        font-size: 12px;
        font-weight: 600;
        color: #8b949e;
      }
    }

    .diff-row {
      display: flex;
      padding: 8px 16px;
      border-bottom: 1px solid #21262d;
      align-items: flex-start;

      &.added { background: rgba(63, 185, 80, 0.05); }
      &.removed { background: rgba(248, 81, 73, 0.05); }
      &.modified { background: rgba(210, 153, 34, 0.05); }

      .row-cell {
        font-size: 12px;
        font-family: monospace;
        overflow: hidden;

        pre {
          margin: 0;
          white-space: pre-wrap;
          word-break: break-word;
          color: #c9d1d9;
          background: #161b22;
          padding: 4px 8px;
          border-radius: 4px;
          max-height: 120px;
          overflow-y: auto;
        }

        .null { color: #6e7681; }
      }

      .field-name {
        color: #e6edf3;
        font-weight: 500;
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .old-value pre { border-left: 2px solid #f85149; }
      .new-value pre { border-left: 2px solid #3fb950; }
    }

    .diff-empty {
      padding: 48px 0;
      display: flex;
      justify-content: center;
    }
  }

  .empty-diff {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}
</style>
