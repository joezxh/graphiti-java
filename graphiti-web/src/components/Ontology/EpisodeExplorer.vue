/**
 * 剧集浏览器 — 左侧树形导航（独立剧集视图）
 */
<template>
  <div class="object-explorer">
    <div class="explorer-search">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索剧集..."
        size="small"
        allow-clear
        @search="handleSearch"
        @change="handleSearch"
      >
        <template #prefix><SearchOutlined style="color: #6e7681; font-size: 12px" /></template>
      </a-input-search>
    </div>

    <div class="explorer-toolbar">
      <a-tooltip title="刷新">
        <a-button type="text" size="small" :loading="loading" @click="handleRefresh">
          <template #icon><ReloadOutlined :spin="loading" /></template>
        </a-button>
      </a-tooltip>
    </div>

    <div class="explorer-tree">
      <a-tree
        v-if="treeData.length > 0"
        :tree-data="treeData"
        :expanded-keys="expandedKeys"
        :selected-keys="selectedKeys"
        :auto-expand-parent="false"
        :show-icon="true"
        block-node
        @select="handleNodeSelect"
        @expand="handleExpand"
      >
        <template #icon><ApiOutlined /></template>
        <template #title="node">
          <div class="tree-node-content" @contextmenu.prevent="handleContextMenu($event, node)">
            <span class="node-icon" :style="{ color: getNodeColor(node.type) }">
              {{ getNodeIcon(node.type) }}
            </span>
            <span class="node-label">{{ node.title }}</span>
            <span v-if="node.count !== undefined" class="node-count">{{ node.count }}</span>
          </div>
        </template>
      </a-tree>
      <div v-else-if="loading" class="empty-tree-tip">
        <a-spin size="small" /> 加载中...
      </div>
      <div v-else class="empty-tree-tip">
        暂无线集数据
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ApiOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { graphApi } from '@/api/graph'
import { EPISODE_TYPE_COLORS } from '@/types/legal-graph-v3'

interface EpisodeNode {
  key: string
  title: string
  icon: string
  type: string
  count?: number
  children?: EpisodeNode[]
  _stageNode?: any
  _processNode?: any
}

const props = defineProps<{
  graphId: string
}>()

const emit = defineEmits<{
  (e: 'open-episode', payload?: { stageNode?: any; processNode?: any }): void
}>()

const searchKeyword = ref('')
const selectedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])
const hierarchy = ref<any[]>([])
const loading = ref(false)

const treeData = computed(() => {
  if (!hierarchy.value.length) return []

  const root: EpisodeNode = {
    key: 'episodes-root',
    title: '剧集',
    icon: '📂',
    type: 'root',
    children: hierarchy.value.map(pn => ({
      key: `episode-process-${pn.processType || pn.legalProcess}`,
      title: pn.processType || pn.legalProcess || '未分类',
      icon: '📁',
      type: 'process',
      count: pn.count,
      children: (pn.children || []).map((sn: any): EpisodeNode => ({
        key: `episode-stage-${sn.stageLabel}`,
        title: sn.stageLabel || '未分类',
        icon: '◇',
        type: 'stage',
        count: sn.count,
        children: [],
        _stageNode: sn,
        _processNode: pn
      }))
    }))
  }

  if (!searchKeyword.value.trim()) return [root]

  return filterTree([root], searchKeyword.value.trim().toLowerCase())
})

function filterTree(nodes: EpisodeNode[], keyword: string): EpisodeNode[] {
  const result: EpisodeNode[] = []
  for (const node of nodes) {
    const titleMatch = node.title.toLowerCase().includes(keyword)
    const filteredChildren = node.children ? filterTree(node.children, keyword) : []
    if (titleMatch || filteredChildren.length > 0) {
      result.push({ ...node, children: filteredChildren })
    }
  }
  return result
}

function getNodeIcon(type: string): string {
  const map: Record<string, string> = {
    root: '📂',
    process: '📁',
    stage: '◇'
  }
  return map[type] ?? '📄'
}

function getNodeColor(type: string): string {
  const map: Record<string, string> = {
    root: '#58a6ff',
    process: '#a371f7',
    stage: '#3fb950'
  }
  return map[type] ?? '#8b949e'
}

function findNode(nodes: EpisodeNode[], key: string): EpisodeNode | null {
  for (const node of nodes) {
    if (node.key === key) return node
    if (node.children) {
      const found = findNode(node.children, key)
      if (found) return found
    }
  }
  return null
}

function handleNodeSelect(keys: (string | number)[]) {
  if (keys.length === 0) return
  const key = String(keys[0])
  selectedKeys.value = [key]
  const node = findNode(treeData.value, key)
  if (!node) return

  if (key.startsWith('episode-stage-')) {
    emit('open-episode', { stageNode: node._stageNode, processNode: node._processNode })
  } else if (key.startsWith('episode-process-')) {
    // Process node — just expand, handled by tree expand
  } else if (key !== 'episodes-root') {
    emit('open-episode', {})
  }
}

function handleExpand(keys: (string | number)[]) {
  expandedKeys.value = keys.map(String)
}

function handleSearch() {
  if (searchKeyword.value.trim()) {
    expandAllMatching(treeData.value, searchKeyword.value.trim().toLowerCase())
  }
}

function expandAllMatching(nodes: EpisodeNode[], keyword: string) {
  for (const node of nodes) {
    if (node.title.toLowerCase().includes(keyword)) {
      if (!expandedKeys.value.includes(node.key)) {
        expandedKeys.value.push(node.key)
      }
    }
    if (node.children) expandAllMatching(node.children, keyword)
  }
}

async function handleRefresh() {
  await loadData()
  message.success('剧集已刷新')
}

const ctxMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  nodeKey: '',
  nodeType: ''
})

function handleContextMenu(e: MouseEvent, node: EpisodeNode) {
  ctxMenu.nodeKey = node.key
  ctxMenu.nodeType = node.type
  ctxMenu.x = e.clientX
  ctxMenu.y = e.clientY
  ctxMenu.visible = true
}

onMounted(async () => {
  document.addEventListener('click', handleClickOutside)
  await loadData()
})

async function loadData() {
  if (!props.graphId) return
  loading.value = true
  try {
    const data = await graphApi.getEpisodeHierarchy(props.graphId)
    hierarchy.value = data || []
  } catch (e) {
    console.error('加载剧集数据失败:', e)
    hierarchy.value = []
  } finally {
    loading.value = false
  }
}

function handleClickOutside() {
  if (ctxMenu.visible) ctxMenu.visible = false
}
</script>

<style scoped lang="less">
.object-explorer {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.explorer-search {
  padding: 8px;
  flex-shrink: 0;
}

.explorer-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 8px 8px;
  flex-shrink: 0;
}

.explorer-tree {
  flex: 1;
  overflow-y: auto;
  padding: 0 4px;
}

.tree-node-content {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  cursor: pointer;

  .node-icon {
    width: 16px;
    font-size: 12px;
    flex-shrink: 0;
  }

  .node-label {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
    color: #e6edf3;
  }

  .node-count {
    font-size: 11px;
    color: #6e7681;
    background: #21262d;
    padding: 2px 6px;
    border-radius: 10px;
    flex-shrink: 0;
  }
}

.empty-tree-tip {
  padding: 16px 12px;
  font-size: 13px;
  color: #6e7681;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
</style>
