/**
 * 社区浏览器 — 左侧树形导航（独立社区视图）
 */
<template>
  <div class="object-explorer">
    <!-- 搜索栏 + 工具栏 -->
    <div class="explorer-search-toolbar">
      <a-input-search
        v-model:value="searchKeyword"
        :placeholder="t('communityEpisode.searchCommunity')"
        size="small"
        allow-clear
        @search="handleSearch"
        @change="handleSearch"
        class="search-input"
      >
        <template #prefix><SearchOutlined style="color: #6e7681; font-size: 12px" /></template>
      </a-input-search>
      <div class="toolbar-actions">
        <a-tooltip :title="$t('common.refresh')">
          <a-button type="text" size="small" :loading="loading" @click="handleRefresh" class="toolbar-btn">
            <template #icon><ReloadOutlined :spin="loading" /></template>
          </a-button>
        </a-tooltip>
      </div>
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
              {{ node.children && node.children.length > 0 ? '📁' : getNodeIcon(node.type) }}
            </span>
            <span class="node-label">{{ node.title }}</span>
            <span v-if="node.count !== undefined" class="node-count">{{ node.count }}</span>
          </div>
        </template>
      </a-tree>
      <div v-else-if="loading" class="empty-tree-tip">
        <a-spin size="small" /> {{ $t('common.loading') }}
      </div>
      <div v-else class="empty-tree-tip">
        {{ $t('common.noData') }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ApiOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { graphApi } from '@/api/graph'
import { LEGAL_DOMAIN_COLORS } from '@/types/legal-graph-v3'

const { t } = useI18n()

interface CommunityNode {
  key: string
  title: string
  icon: string
  type: string
  count?: number
  children?: CommunityNode[]
  _communityNode?: any
}

const props = defineProps<{
  graphId: string
}>()

const emit = defineEmits<{
  (e: 'open-community', payload?: { node?: any }): void
}>()

const searchKeyword = ref('')
const selectedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])
const hierarchy = ref<any[]>([])
const loading = ref(false)

const treeData = computed(() => {
  if (!hierarchy.value.length) return []

  const root: CommunityNode = {
    key: 'communities-root',
    title: t('communityEpisode.communityTab'),
    icon: '📂',
    type: 'root',
    children: hierarchy.value.map(node => ({
      key: `community-type-${node.typeCode}`,
      title: node.typeName || node.title,
      icon: '◉',
      type: 'type',
      count: node.count,
      children: (node.children || []).map((child: any): CommunityNode => ({
        key: `community-node-${child.typeCode || child.uuid}`,
        title: child.typeName || child.name || child.title,
        icon: '◉',
        type: 'community',
        children: [],
        _communityNode: child
      }))
    }))
  }

  if (!searchKeyword.value.trim()) return [root]

  return filterTree([root], searchKeyword.value.trim().toLowerCase())
})

function filterTree(nodes: CommunityNode[], keyword: string): CommunityNode[] {
  const result: CommunityNode[] = []
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
    type: '◉',
    community: '◉'
  }
  return map[type] ?? '📄'
}

function getNodeColor(type: string): string {
  const map: Record<string, string> = {
    root: '#58a6ff',
    type: '#a371f7',
    community: '#3fb950'
  }
  return map[type] ?? '#8b949e'
}

function findNode(nodes: CommunityNode[], key: string): CommunityNode | null {
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

  if (key.startsWith('community-node-')) {
    emit('open-community', { node: node._communityNode || node })
  } else if (key.startsWith('community-type-')) {
    // Type node — just expand, handled by tree expand
  } else if (key !== 'communities-root') {
    emit('open-community', {})
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

function expandAllMatching(nodes: CommunityNode[], keyword: string) {
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
  message.success(t('common.refreshSuccess'))
}

const ctxMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  nodeKey: '',
  nodeType: ''
})

function handleContextMenu(e: MouseEvent, node: CommunityNode) {
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
    const data = await graphApi.getCommunityHierarchy(props.graphId)
    hierarchy.value = data || []
  } catch (e) {
    console.error(t('communityEpisode.loadCommunitiesFailed'), e)
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

.explorer-search-toolbar {
  display: flex;
  align-items: center;
  padding: 2px 4px;
  gap: 4px;
  border-bottom: 1px solid #21262d;
  flex-shrink: 0;
  white-space: nowrap;
  margin: 0;
  width: 100%;
  box-sizing: border-box;

  .search-input {
    flex: 1;
    min-width: 0;
    
    :deep(.ant-input) {
      padding: 1px 6px;
      font-size: 12px;
      height: 24px;
    }
    
    :deep(.ant-input-search) {
      padding: 0;
    }
  }

  .toolbar-actions {
    display: flex;
    align-items: center;
    gap: 2px;
    flex-shrink: 0;

    .toolbar-btn {
      padding: 0;
      height: 24px;
      width: 24px;
      min-width: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0;

      :deep(.anticon) {
        font-size: 12px;
      }
    }
  }
}

.explorer-tree {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
}

.tree-node-content {
  display: flex;
  align-items: center;
  gap: 6px;
  width: 100%;

  .node-icon {
    font-size: 12px;
    flex-shrink: 0;
    width: 14px;
  }

  .node-label {
    flex: 1;
    font-size: 13px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .node-count {
    font-size: 11px;
    color: #6e7681;
    background: #21262d;
    padding: 1px 5px;
    border-radius: 8px;
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

:deep(.ant-tree) {
  background: transparent;
  color: #e6edf3;
  font-size: 13px;

  .ant-tree-treenode {
    padding: 2px 0;
    width: 100%;
  }

  .ant-tree-node-content-wrapper {
    padding: 2px 4px;
    min-height: 28px;
    border-radius: 4px;
    width: 100%;
    transition: background 0.15s;

    &:hover {
      background: #21262d;
    }
  }

  .ant-tree-node-selected .ant-tree-node-content-wrapper {
    background: rgba(88, 166, 255, 0.15) !important;
    color: #58a6ff;
  }

  .ant-tree-switcher {
    color: #6e7681;
  }

  .ant-tree-iconEle {
    display: none;
  }
}
</style>
