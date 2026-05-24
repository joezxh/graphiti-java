<template>
  <div class="episode-type-explorer">
    <!-- 搜索栏 + 工具栏 -->
    <div class="explorer-search-toolbar">
      <a-input-search
        v-model:value="searchKeyword"
        :placeholder="t('ontology.searchType')"
        size="small"
        allow-clear
        @search="handleSearch"
        @change="handleSearch"
        class="search-input"
      >
        <template #prefix><SearchOutlined style="color: #6e7681; font-size: 12px" /></template>
      </a-input-search>
      <div class="toolbar-actions">
        <a-tooltip :title="t('common.refresh')">
          <a-button type="text" size="small" :loading="loading" @click="handleRefresh" class="toolbar-btn">
            <template #icon><ReloadOutlined :spin="loading" /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip :title="t('common.create')">
          <a-button type="text" size="small" @click="handleCreate" class="toolbar-btn">
            <template #icon><PlusOutlined /></template>
          </a-button>
        </a-tooltip>
        <a-tooltip :title="viewMode === 'tree' ? t('ontology.switchList') : t('ontology.switchTree')">
          <a-button type="text" size="small" @click="viewMode = viewMode === 'tree' ? 'list' : 'tree'" class="toolbar-btn">
            <template #icon>
              <UnorderedListOutlined v-if="viewMode === 'tree'" />
              <AppstoreOutlined v-else />
            </template>
          </a-button>
        </a-tooltip>
      </div>
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
        <a-spin size="small" /> {{ t('common.loading') }}
      </div>
      <div v-else class="empty-tip">
        {{ t('common.noData') }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  FolderOpenOutlined,
  UnorderedListOutlined,
  AppstoreOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { episodeTypeApi } from '@/api/metadata'
import type { OntEpisodeTypeVO } from '@/api/metadata'

const { t } = useI18n()

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
  { label: t('ontology.treeView'), value: 'tree' },
  { label: t('ontology.listView'), value: 'list' }
]
const selectedKeys = ref<string[]>([])
const expandedKeys = ref<string[]>([])
const typeTree = ref<OntEpisodeTypeVO[]>([])
const loading = ref(false)

const listColumns = computed(() => [
  { title: t('ontology.typeName'), dataIndex: 'typeName', key: 'typeName', ellipsis: true },
  { title: t('common.code'), dataIndex: 'typeCode', key: 'typeCode', width: 100, ellipsis: true },
  { title: t('ontology.level'), dataIndex: 'level', key: 'level', width: 60, ellipsis: true },
  { title: t('ontology.instanceCount'), dataIndex: 'instanceCount', key: 'instanceCount', width: 70, ellipsis: true }
])

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
  message.info(t('ontology.dragSortDev'))
}

async function handleRefresh() {
  await loadData()
  message.success(t('common.refreshSuccess'))
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
    console.error(t('ontology.loadEpisodeTypeTreeFailed'), e)
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

  .explorer-search-toolbar {
    display: flex;
    align-items: center;
    padding: 8px;
    gap: 8px;
    border-bottom: 1px solid #21262d;
    flex-shrink: 0;
    white-space: nowrap;

    .search-input {
      flex: 1;
      min-width: 0;
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
