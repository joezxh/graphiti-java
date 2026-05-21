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
          <a-tag color="green">+{{ diffResult.added.length }} 新增</a-tag>
          <a-tag color="red">-{{ diffResult.removed.length }} 删除</a-tag>
          <a-tag color="yellow">~{{ diffResult.modified.length }} 修改</a-tag>
        </a-space>
      </div>

      <!-- 三栏Diff -->
      <div class="diff-three-cols">
        <div class="diff-col left-col">
          <div class="col-header">版本A ({{ leftVersion }})</div>
          <div class="col-content">
            <div v-for="item in displayItems" :key="item.id" class="diff-item" :class="item.op">
              <div class="item-name">{{ item.name }}</div>
              <div v-if="item.op !== 'added'" class="item-details">
                <div v-for="fd in item.fieldDiffs" :key="fd.field" class="field-diff removed">
                  <span class="field-name">{{ fd.field }}:</span>
                  <span class="field-value">{{ formatVal(fd.oldValue) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="diff-col center-col">
          <div class="col-header">变更类型</div>
          <div class="col-content">
            <div v-for="item in displayItems" :key="'op-' + item.id" class="diff-item">
              <div class="op-badge" :class="item.op">
                {{ opLabel(item.op) }}
              </div>
            </div>
          </div>
        </div>

        <div class="diff-col right-col">
          <div class="col-header">版本B ({{ rightVersion }})</div>
          <div class="col-content">
            <div v-for="item in displayItems" :key="'b-' + item.id" class="diff-item" :class="item.op">
              <div class="item-name">{{ item.name }}</div>
              <div v-if="item.op !== 'removed'" class="item-details">
                <div v-for="fd in item.fieldDiffs" :key="fd.field" class="field-diff added">
                  <span class="field-name">{{ fd.field }}:</span>
                  <span class="field-value">{{ formatVal(fd.newValue) }}</span>
                </div>
              </div>
            </div>
          </div>
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
import { diffEntityList } from '@/utils/ontologyDiff'
import type { EntityDiff } from '@/utils/ontologyDiff'

const props = defineProps<{ graphId: string }>()
const store = useOntologyStore()

const leftVersion = ref<string | number | undefined>(undefined)
const rightVersion = ref<string | number | undefined>(undefined)
const activeDiffTab = ref('all')
const diffResult = ref<{ added: EntityDiff[]; removed: EntityDiff[]; modified: EntityDiff[] } | null>(null)

const versionOptions = computed(() =>
  store.versionHistory.map(v => ({ label: v.version, value: v.id }))
)

const displayItems = computed(() => {
  if (!diffResult.value) return []
  let items: EntityDiff[] = []
  switch (activeDiffTab.value) {
    case 'class': items = [...diffResult.value.added, ...diffResult.value.removed, ...diffResult.value.modified].filter(i => i.id.toString().startsWith('class-') || true); break
    case 'property': items = [...diffResult.value.added, ...diffResult.value.removed, ...diffResult.value.modified]; break
    case 'constraint': items = [...diffResult.value.added, ...diffResult.value.removed, ...diffResult.value.modified]; break
    default: items = [...diffResult.value.added, ...diffResult.value.removed, ...diffResult.value.modified]
  }
  return items
})

function opLabel(op?: string) {
  return { added: '+ 新增', removed: '- 删除', modified: '~ 修改', unchanged: '= 不变' }[op ?? ''] ?? op
}

function formatVal(val: any) {
  if (val === undefined || val === null) return '-'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}

function handleCompare() {
  if (!leftVersion.value || !rightVersion.value) return
  const v1 = store.versionHistory.find(v => v.id === leftVersion.value)
  const v2 = store.versionHistory.find(v => v.id === rightVersion.value)
  if (!v1 || !v2) return

  // 简单的字段级Diff：直接比较beforeState和afterState的JSON
  const before = v1.afterState ? JSON.parse(v1.afterState) : {}
  const after = v2.afterState ? JSON.parse(v2.afterState) : {}

  const result = diffEntityList([{ id: 'state', ...before }] as any, [{ id: 'state', ...after }] as any, 'localName')
  diffResult.value = result
}

function handleRefresh() {
  store.loadVersionHistory(props.graphId)
  diffResult.value = null
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

  .diff-three-cols {
    display: flex;
    flex: 1;
    overflow: hidden;

    .diff-col {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;

      &.left-col { border-right: 1px solid #30363d; }
      &.center-col { flex: 0 0 120px; border-right: 1px solid #30363d; }
      &.right-col { }

      .col-header {
        padding: 8px 16px;
        background: #161b22;
        border-bottom: 1px solid #30363d;
        font-size: 12px;
        font-weight: 600;
        color: #8b949e;
        flex-shrink: 0;
      }

      .col-content {
        flex: 1;
        overflow-y: auto;
        padding: 8px 0;
      }
    }

    .diff-item {
      padding: 4px 16px;
      border-bottom: 1px solid #21262d;

      &.added { background: rgba(63, 185, 80, 0.05); }
      &.removed { background: rgba(248, 81, 73, 0.05); }
      &.modified { background: rgba(210, 153, 34, 0.05); }

      .item-name { font-size: 13px; font-weight: 500; color: #e6edf3; margin-bottom: 2px; }

      .item-details {
        .field-diff {
          font-size: 12px;
          display: flex;
          gap: 6px;
          margin-top: 2px;
          font-family: monospace;

          &.added .field-name { color: #3fb950; }
          &.removed .field-name { color: #f85149; }
          &.added .field-value { color: #8b949e; }
          &.removed .field-value { color: #8b949e; }
        }
      }

      .op-badge {
        font-size: 12px;
        font-weight: 600;
        padding: 2px 6px;
        border-radius: 3px;
        text-align: center;

        &.added { color: #3fb950; background: rgba(63, 185, 80, 0.15); }
        &.removed { color: #f85149; background: rgba(248, 81, 73, 0.15); }
        &.modified { color: #d29922; background: rgba(210, 153, 34, 0.15); }
      }
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
