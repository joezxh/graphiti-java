<template>
  <div class="episode-type-explorer">
    <!-- 搜索栏 -->
    <div class="explorer-search">
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索类型..."
        size="small"
        allow-clear
        @search="handleSearch"
        @change="handleSearch"
      >
        <template #prefix><SearchOutlined style="color: #6e7681; font-size: 12px" /></template>
      </a-input-search>
    </div>

    <!-- 工具栏 -->
    <div class="explorer-toolbar">
      <a-tooltip title="刷新">
        <a-button type="text" size="small" :loading="loading" @click="handleRefresh">
          <template #icon><ReloadOutlined :spin="loading" /></template>
        </a-button>
      </a-tooltip>
      <a-tooltip title="新建类型">
        <a-button type="text" size="small" @click="handleCreate">
          <template #icon><PlusOutlined /></template>
        </a-button>
      </a-tooltip>
      <a-segmented v-model:value="viewMode" :options="viewOptions" size="small" />
    </div>

    <!-- 树形/列表视图 -->
    <div class="explorer-body">
      <a-tree
        v-if="viewMode === 'tree' && treeData.length > 0"
        :tree-data="treeData"
        :expanded-keys="expandedKeys"
        :selected-keys="selectedKeys"
        :auto-expand-parent="false"
        :show-icon="true"
        block-node
        draggable
        @select="handleNodeSelect"
        @expand="handleExpand"
        @drop="handleDrop"
      >
        <template #icon><FolderOpenOutlined /></template>
        <template #title="node">
          <div class="tree-node-content">
            <span class="node-icon" :style="{ color: getNodeColor(node.level) }">
              {{ getNodeIcon(node.level) }}
            </span>
            <span class="node-label">{{ node.title }}</span>
            <span v-if="node.instanceCount !== undefined" class="node-count">{{ node.instanceCount }}</span>
          </div>
        </template>
      </a-tree>

      <a-table
        v-else-if="viewMode === 'list' && flatList.length > 0"
        :data-source="flatList"
        :columns="listColumns"
        :pagination="false"
        size="small"
        :scroll="{ x: 'max-content' }"
        :row-selection="{ selectedRowKeys: selectedKeys, onChange: handleRowSelect }"
        @row-click="handleRowClick"
      />

      <div v-else-if="loading" class="empty-tip">
        <a-spin size="small" /> 加载中...
      </div>
      <div v-else class="empty-tip">
        暂无剧集类型数据
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  FolderOpenOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { episodeTypeApi } from '@/api/metadata'
import type { OntEpisodeTypeVO } from '@/api/metadata'

const props = defineProps<{
  graphId: string
  definitionId?: number
}>()

const emit = defineEmits<{
  (e: 'select-type', payload: { typeId: number; typeCode: string; typeName: string }): void
  (e: 'create-type'): void
}>()

const searchKeyword = ref('')
const viewMode = ref<'tree' | 'list'>('tree')
const viewOptions = [
  { label: '树', value: 'tree' },
  { label: '列表', value: 'list' }
]
const selectedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])
const typeTree = ref<OntEpisodeTypeVO[]>([])
const loading = ref(false)

const listColumns = [
  { title: '类型名称', dataIndex: 'typeName', key: 'typeName', ellipsis: true },
  { title: '代码', dataIndex: 'typeCode', key: 'typeCode', width: 100, ellipsis: true },
  { title: '层级', dataIndex: 'level', key: 'level', width: 60, ellipsis: true },
  { title: '实例数', dataIndex: 'instanceCount', key: 'instanceCount', width: 70, ellipsis: true }
]

// 将后端树转为 ant-design-vue tree 格式
function buildTreeData(nodes: OntEpisodeTypeVO[], parentKey = ''): any[] {
  return nodes.map(node => {
    const key = parentKey ? `${parentKey}-${node.id}` : `et-${node.id}`
    return {
      key,
      title: node.typeName || node.typeCode,
      level: node.level || 1,
      instanceCount: node.instanceCount,
      typeId: node.id,
      typeCode: node.typeCode,
      typeName: node.typeName,
      children: node.children?.length ? buildTreeData(node.children, key) : undefined
    }
  })
}

const treeData = computed(() => {
  const data = buildTreeData(typeTree.value)
  if (!searchKeyword.value.trim()) return data
  return filterTree(data, searchKeyword.value.trim().toLowerCase())
})

const flatList = computed(() => {
  const result: any[] = []
  function flatten(nodes: OntEpisodeTypeVO[], depth = 0) {
    for (const node of nodes) {
      result.push({
        key: `et-${node.id}`,
        typeName: '  '.repeat(depth) + (node.typeName || node.typeCode),
        typeCode: node.typeCode,
        level: node.level,
        instanceCount: node.instanceCount ?? '-',
        typeId: node.id,
        raw: node
      })
      if (node.children?.length) flatten(node.children, depth + 1)
    }
  }
  flatten(typeTree.value)
  if (!searchKeyword.value.trim()) return result
  const kw = searchKeyword.value.trim().toLowerCase()
  return result.filter(r =>
    r.typeCode.toLowerCase().includes(kw) ||
    r.typeName.toLowerCase().includes(kw)
  )
})

function filterTree(nodes: any[], keyword: string): any[] {
  const result: any[] = []
  for (const node of nodes) {
    const titleMatch = String(node.title).toLowerCase().includes(keyword)
    const codeMatch = String(node.typeCode).toLowerCase().includes(keyword)
    const filteredChildren = node.children ? filterTree(node.children, keyword) : []
    if (titleMatch || codeMatch || filteredChildren.length > 0) {
      result.push({ ...node, children: filteredChildren.length ? filteredChildren : undefined })
    }
  }
  return result
}

function getNodeIcon(level?: number): string {
  if (level === 1) return '📂'
  if (level === 2) return '📁'
  return '📄'
}

function getNodeColor(level?: number): string {
  if (level === 1) return '#58a6ff'
  if (level === 2) return '#a371f7'
  return '#8b949e'
}

function handleNodeSelect(keys: (string | number)[]) {
  if (keys.length === 0) return
  const key = String(keys[0])
  selectedKeys.value = [key]
  const node = findNode(treeData.value, key)
  if (node?.typeId) {
    emit('select-type', {
      typeId: node.typeId,
      typeCode: node.typeCode,
      typeName: node.typeName
    })
  }
}

function handleRowSelect(keys: (string | number)[]) {
  selectedKeys.value = keys.map(String)
}

function handleRowClick(record: any) {
  if (record?.typeId) {
    emit('select-type', {
      typeId: record.typeId,
      typeCode: record.typeCode,
      typeName: record.raw?.typeName || record.typeCode
    })
  }
}

function handleExpand(keys: (string | number)[]) {
  expandedKeys.value = keys.map(String)
}

function handleSearch() {
  if (searchKeyword.value.trim() && viewMode.value === 'tree') {
    expandAllMatching(treeData.value, searchKeyword.value.trim().toLowerCase())
  }
}

function expandAllMatching(nodes: any[], keyword: string) {
  for (const node of nodes) {
    if (String(node.title).toLowerCase().includes(keyword)) {
      if (!expandedKeys.value.includes(node.key)) {
        expandedKeys.value.push(node.key)
      }
    }
    if (node.children) expandAllMatching(node.children, keyword)
  }
}

async function handleDrop(info: any) {
  const dropKey = info.node?.key
  const dragKey = info.dragNode?.key
  const dropPos = info.node.pos?.split('-')
  const dropPosition = info.dropPosition - Number(dropPos?.[dropPos.length - 1])

  // 简单实现：同层级拖拽更新排序
  // 实际项目中应调用 episodeTypeApi.reorder
  message.info('拖拽排序功能开发中')
}

async function handleRefresh() {
  await loadData()
  message.success('类型列表已刷新')
}

function handleCreate() {
  emit('create-type')
}

function findNode(nodes: any[], key: string): any | null {
  for (const node of nodes) {
    if (node.key === key) return node
    if (node.children) {
      const found = findNode(node.children, key)
      if (found) return found
    }
  }
  return null
}

async function loadData() {
  if (!props.graphId) return
  const defId = props.definitionId
  if (!defId) {
    // 尝试从 store 或接口获取 definitionId
    return
  }
  loading.value = true
  try {
    const data = await episodeTypeApi.getTree(props.graphId, defId)
    typeTree.value = data || []
  } catch (e) {
    console.error('加载剧集类型树失败:', e)
    typeTree.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.graphId, () => {
  if (props.graphId && props.definitionId) loadData()
}, { immediate: true })

watch(() => props.definitionId, () => {
  if (props.graphId && props.definitionId) loadData()
})

onMounted(() => {
  if (props.graphId && props.definitionId) loadData()
})
</script>

<style scoped lang="less">
.episode-type-explorer {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  .explorer-search {
    padding: 8px;
    border-bottom: 1px solid #21262d;
    flex-shrink: 0;
  }

  .explorer-toolbar {
    display: flex;
    align-items: center;
    padding: 4px 8px;
    gap: 4px;
    border-bottom: 1px solid #21262d;
    flex-shrink: 0;
  }

  .explorer-body {
    flex: 1;
    overflow: auto;
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

  .empty-tip {
    padding: 16px 12px;
    font-size: 13px;
    color: #6e7681;
    text-align: center;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
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

:deep(.ant-table) {
  .ant-table-cell {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .ant-table-body {
    overflow-x: auto !important;
  }
}
</style>
