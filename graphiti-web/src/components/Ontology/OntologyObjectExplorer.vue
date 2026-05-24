/**
 * 本体对象浏览器 — 左侧树形导航（Navicat风格）
 */
<template>
  <div class="object-explorer">
    <!-- 全局搜索 + 工具栏 -->
    <div class="explorer-search-toolbar">
      <a-input-search v-model:value="searchKeyword" placeholder="搜索本体..." size="small" allow-clear @search="handleSearch" @change="handleSearch" class="search-input"><template #prefix><SearchOutlined style="color: #6e7681; font-size: 12px" /></template></a-input-search>
      <div class="toolbar-actions">
        <a-tooltip title="刷新">
          <a-button type="text" size="small" :loading="store.loading" @click="handleRefresh" class="toolbar-btn">
            <template #icon><ReloadOutlined :spin="store.loading" /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip title="新建类">
          <a-button type="text" size="small" @click="handleNewClass" class="toolbar-btn">
            <template #icon><PlusOutlined /></template>
          </a-button>
        </a-tooltip>
      </div>
    </div>

    <!-- 树形结构 -->
    <div class="explorer-tree">
      <a-tree
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
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="ctxMenu.visible"
      class="ctx-menu"
      :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
      @click.stop
    >
      <a-menu @click="handleCtxMenuAction">
        <template v-if="ctxMenu.nodeType === 'class'">
          <a-menu-item key="edit">编辑类</a-menu-item>
          <a-menu-item key="new-subclass">新建子类</a-menu-item>
          <a-menu-item key="view-instances">查看实例</a-menu-item>
          <a-menu-divider />
          <a-menu-item key="delete" style="color: #f85149">删除类</a-menu-item>
        </template>
        <template v-else-if="ctxMenu.nodeType === 'class-group'">
          <a-menu-item key="new-class">新建类</a-menu-item>
        </template>
        <template v-else-if="ctxMenu.nodeType === 'property-group'">
          <a-menu-item key="new-property">新建属性</a-menu-item>
        </template>
        <template v-else-if="ctxMenu.nodeType === 'property'">
          <a-menu-item key="edit-property">编辑属性</a-menu-item>
          <a-menu-divider />
          <a-menu-item key="delete-property" style="color: #f85149">删除属性</a-menu-item>
        </template>
        <template v-else-if="ctxMenu.nodeType === 'instance-class'">
          <a-menu-item key="view-instances">查看实例</a-menu-item>
          <a-menu-item key="import-instances">导入实例</a-menu-item>
          <a-menu-item key="export-instances">导出实例</a-menu-item>
        </template>
        <template v-else>
          <a-menu-item key="refresh">刷新</a-menu-item>
        </template>
      </a-menu>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted, onBeforeUnmount } from 'vue'
import { ApiOutlined, SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import type { OntologyExplorerNode } from '@/store/modules/ontology'

const props = defineProps<{
  graphId: string
  ontologyMode?: 'class' | 'episodes' | 'communities'
}>()

const emit = defineEmits<{
  (e: 'open-tab', payload: { type: string; title: string; classId?: number; propertyId?: number; constraintId?: number; classType?: string; schemaClass?: any }): void
  (e: 'class-selected', classId: number): void
  (e: 'open-episode', payload?: { stageNode?: any; processNode?: any }): void
  (e: 'open-community', payload?: { node?: any }): void
}>()

const store = useOntologyStore()
const searchKeyword = ref('')
const selectedKeys = ref<string[]>([])

const hiddenGroupKeys = new Set(['properties', 'constraints', 'instances'])

const expandedKeys = computed(() => {
  const keys = Array.from(store.expandedTreeKeys)
  return props.ontologyMode === 'class'
    ? keys.filter(k => !hiddenGroupKeys.has(k))
    : keys
})

const treeData = computed(() => {
  const tree = store.buildExplorerTree()

  if (!searchKeyword.value.trim()) {
    if (props.ontologyMode === 'class') {
      return [tree].map(t => ({
        ...t,
        children: t.children?.filter(c => !hiddenGroupKeys.has(c.key))
      }))
    }
    return [tree]
  }

  const filtered = filterTree([tree], searchKeyword.value.trim().toLowerCase())
  if (props.ontologyMode === 'class') {
    return filtered.map(f => ({
      ...f,
      children: f?.children?.filter(c => !hiddenGroupKeys.has(c.key))
    }))
  }
  return filtered
})

function filterTree(nodes: OntologyExplorerNode[], keyword: string): OntologyExplorerNode[] {
  const result: OntologyExplorerNode[] = []
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
    root: '📁',
    'class-group': '📂',
    'property-group': '📂',
    'constraint-group': '📂',
    'instance-group': '📂',
    class: '◉',
    property: '◆',
    constraint: '◇',
    'instance-class': '◉',
    'version-history': '📋',
    'tool-consistency': '🔧',
    'tool-validation': '🔧',
    'tool-graph': '📊',
    tool: '📂'
  }
  return map[type] ?? '📄'
}

function getNodeColor(type: string): string {
  const map: Record<string, string> = {
    class: '#58a6ff',
    property: '#a371f7',
    constraint: '#d29922',
    'instance-class': '#3fb950',
    'version-history': '#8b949e',
    'tool-consistency': '#f0883e',
    'tool-validation': '#f0883e',
    'tool-graph': '#58a6ff'
  }
  return map[type] ?? '#8b949e'
}

function handleNodeSelect(keys: (string | number)[]) {
  if (keys.length === 0) return
  const key = String(keys[0])
  selectedKeys.value = [key]
  const node = findNode(treeData.value, key)
  if (!node) return

  switch (node.type) {
    case 'class': {
      const schemaClass = store.classes.find(c => c.id === node.classId)
      emit('open-tab', { type: 'class-instance-view', title: `实例: ${node.title}`, classId: node.classId, schemaClass })
      emit('class-selected', node.classId)
      break
    }
    case 'property':
      emit('open-tab', { type: 'property-editor', title: `属性: ${node.title}`, propertyId: node.propertyId })
      break
    case 'class-group':
      emit('open-tab', { type: 'class-list', title: '类列表' })
      break
    case 'property-group':
      emit('open-tab', { type: 'property-list', title: '属性列表' })
      break
    case 'constraint-group':
      emit('open-tab', { type: 'constraint-list', title: '约束列表' })
      break
    case 'constraint':
      emit('open-tab', { type: 'constraint-editor', title: `约束: ${node.title}`, constraintId: node.constraintId })
      break
    case 'instance-class':
      emit('open-tab', { type: 'instance-table', title: `实例: ${node.title}`, classType: node.classType })
      break
    case 'version-history':
      emit('open-tab', { type: 'version-history', title: '版本历史' })
      break
    case 'tool-consistency':
      emit('open-tab', { type: 'consistency-check', title: '一致性检查' })
      break
    case 'tool-validation':
      emit('open-tab', { type: 'batch-validation', title: '批量验证' })
      break
    case 'tool-graph':
      emit('open-tab', { type: 'ontology-graph', title: '本体可视化' })
      break
  }
}

function handleExpand(keys: (string | number)[]) {
  store.expandedTreeKeys = new Set(keys.map(String))
}

function handleSearch() {
  // 搜索时自动展开所有匹配节点
  if (searchKeyword.value.trim()) {
    expandAllMatchingNodes(treeData.value, searchKeyword.value.trim().toLowerCase())
  }
}

function expandAllMatchingNodes(nodes: OntologyExplorerNode[], keyword: string) {
  for (const node of nodes) {
    if (node.title.toLowerCase().includes(keyword)) {
      store.expandTreeNode(node.key)
    }
    if (node.children) expandAllMatchingNodes(node.children, keyword)
  }
}

function findNode(nodes: OntologyExplorerNode[], key: string): OntologyExplorerNode | null {
  for (const node of nodes) {
    if (node.key === key) return node
    if (node.children) {
      const found = findNode(node.children, key)
      if (found) return found
    }
  }
  return null
}

async function handleRefresh() {
  if (!props.graphId) return
  await store.loadFullOntology(props.graphId)
  message.success('本体已刷新')
}

function handleNewClass() {
  emit('open-tab', { type: 'class-editor', title: '新建类' })
}

const ctxMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  nodeKey: '',
  nodeType: ''
})

function handleContextMenu(e: MouseEvent, node: any) {
  ctxMenu.nodeKey = node.key
  ctxMenu.nodeType = node.type
  ctxMenu.x = e.clientX
  ctxMenu.y = e.clientY
  ctxMenu.visible = true
}

function handleCtxMenuAction({ key }: { key: string }) {
  ctxMenu.visible = false
  const node = findNode(treeData.value, ctxMenu.nodeKey)
  switch (key) {
    case 'edit':
    case 'new-subclass':
      if (ctxMenu.nodeType === 'class') {
        const cls = store.classes.find(c => `class-${c.id}` === ctxMenu.nodeKey)
        emit('open-tab', { type: 'class-editor', title: `类: ${node?.title}`, classId: cls?.id })
      }
      break
    case 'view-instances':
      if (ctxMenu.nodeType === 'class' || ctxMenu.nodeType === 'instance-class') {
        const classType = ctxMenu.nodeType === 'instance-class'
          ? ctxMenu.nodeKey.replace('instance-', '')
          : store.classes.find(c => `class-${c.id}` === ctxMenu.nodeKey)?.localName
        emit('open-tab', { type: 'instance-table', title: `实例: ${classType}`, classType })
      }
      break
    case 'delete':
      message.warning('删除类功能待实现')
      break
    case 'new-class':
      handleNewClass()
      break
    case 'new-property':
      emit('open-tab', { type: 'property-editor', title: '新建属性' })
      break
    case 'edit-property':
      if (ctxMenu.nodeType === 'property') {
        const prop = store.properties.find(p => `property-${p.id}` === ctxMenu.nodeKey)
        emit('open-tab', { type: 'property-editor', title: `属性: ${node?.title}`, propertyId: prop?.id })
      }
      break
    case 'delete-property':
      message.warning('删除属性功能待实现')
      break
    case 'import-instances':
      emit('open-tab', { type: 'instance-table', title: `实例: ${node?.title}`, classType: node?.classType })
      break
    case 'export-instances':
      message.warning('导出功能待实现')
      break
    case 'refresh':
      handleRefresh()
      break
  }
}

function handleClickOutside(_e: MouseEvent) {
  if (ctxMenu.visible) ctxMenu.visible = false
}

onMounted(async () => {
  if (props.graphId) {
    await store.loadFullOntology(props.graphId)
  }
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped lang="less">
.object-explorer {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

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
          font-size: 13px;
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

<style lang="less">
.ctx-menu {
  position: fixed;
  z-index: 9999;

  .ant-menu {
    background: #161b22;
    border: 1px solid #30363d;
    border-radius: 8px;
    padding: 4px;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
  }

  .ant-menu-item {
    color: #e6edf3;
    font-size: 13px;
    border-radius: 4px;
    margin: 2px 0;
    height: 32px;
    line-height: 32px;

    &:hover {
      background: #21262d;
    }
  }

  .ant-menu-divider {
    background: #30363d;
    margin: 4px 0;
  }
}
</style>
