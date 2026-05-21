/**
 * 实例数据表格 — Navicat风格网格视图
 * 支持动态列、内联编辑、批量操作、导入导出
 */
<template>
  <div class="instance-data-table">
    <!-- 工具栏 -->
    <div class="table-toolbar">
      <div class="toolbar-left">
        <a-space>
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新增
          </a-button>
          <a-button danger :disabled="selectedRowKeys.length === 0" @click="handleBatchDelete">
            <template #icon><DeleteOutlined /></template>
            删除 ({{ selectedRowKeys.length }})
          </a-button>
          <a-divider type="vertical" />
          <a-button @click="importExportRef?.open()">
            <template #icon><UploadOutlined /></template>
            导入
          </a-button>
          <a-button @click="handleQuickExport">
            <template #icon><DownloadOutlined /></template>
            导出
          </a-button>
          <a-divider type="vertical" />
          <a-button :loading="refreshing" @click="handleRefresh">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </a-space>
      </div>
      <div class="toolbar-right">
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索..."
          style="width: 200px"
          @search="loadData"
          @change="debouncedSearch"
        />
      </div>
    </div>

    <!-- 统计栏 -->
    <div class="table-stats">
      <a-space>
        <span class="stat-label">类:</span>
        <a-tag color="blue">{{ classType || '全部' }}</a-tag>
        <span class="stat-label">总数:</span>
        <span class="stat-value">{{ total }}</span>
        <span class="stat-label">当前页:</span>
        <span class="stat-value">{{ dataSource.length }}</span>
      </a-space>
    </div>

    <!-- 表格 -->
    <div class="table-container">
      <a-table
        :columns="dynamicColumns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="{
          current: page,
          pageSize: pageSize,
          total,
          showSizeChanger: true,
          showTotal: (t: number) => `共 ${t} 条`,
          pageSizeOptions: ['20', '50', '100', '500']
        }"
        :row-selection="{ selectedRowKeys, onChange: handleSelectionChange }"
        :scroll="{ x: tableScrollX, y: tableScrollY }"
        :row-key="rowKey"
        :scroll-to-first-error="true"
        size="middle"
        @change="handleTableChange"
        @row-click="handleRowClick"
        @row-contextmenu="handleContextMenu"
      >
        <!-- 可编辑单元格 -->
        <template
          #[`bodyCell`]="{ column, record }"
        >
          <div v-if="editingKey === (record as any)[rowKey]" class="cell-editor">
            <a-input
              v-model:value="editingRecord[column.dataIndex as keyof typeof editingRecord]"
              size="small"
              @pressEnter="handleCellSave(record)"
              @blur="handleCellSave(record)"
            />
          </div>
          <div v-else class="cell-display" @dblclick="startCellEdit(record, column)">
            {{ getCellText(record, column) }}
          </div>
        </template>

        <template #bodyCell="{ column, record }">
          <!-- UUID列特殊处理 -->
          <template v-if="column.key === 'uuid'">
            <a-tooltip :title="(record as any).uuid">
              <span class="uuid-text">{{ truncateUuid((record as any).uuid) }}</span>
              <a-button type="link" size="small" @click="copyUuid((record as any).uuid)">复制</a-button>
            </a-tooltip>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
              <a-popconfirm title="确定删除？" ok-text="确定" cancel-text="取消" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 右键菜单 -->
    <a-menu
      v-if="contextMenu.visible"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px', position: 'fixed', zIndex: 9999 }"
      @click="handleCtxAction"
    >
      <a-menu-item key="view" @click="handleView(contextMenu.record)">查看详情</a-menu-item>
      <a-menu-item key="edit">编辑节点</a-menu-item>
      <a-menu-divider />
      <a-menu-item key="copy-uuid" @click="copyUuid(contextMenu.record?.uuid)">复制UUID</a-menu-item>
      <a-menu-divider />
      <a-menu-item key="delete" style="color: #f85149" @click="handleDelete(contextMenu.record)">删除</a-menu-item>
    </a-menu>

    <!-- 查看详情抽屉 -->
    <a-drawer
      v-model:open="detailDrawerVisible"
      title="节点详情"
      width="500"
      placement="right"
    >
      <a-descriptions v-if="selectedRecord" :column="1" bordered size="small">
        <a-descriptions-item v-for="col in detailColumns" :key="col.key" :label="String(col.title)">
          {{ getCellText(selectedRecord, col) }}
        </a-descriptions-item>
      </a-descriptions>
    </a-drawer>

    <!-- 数据导入导出 -->
    <DataImportExportModal ref="importExportRef" :graph-id="graphId" :class-type="props.classType" @imported="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, DeleteOutlined, UploadOutlined, DownloadOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import { useOntologyStore } from '@/store/modules/ontology'
import DataImportExportModal from './DataImportExportModal.vue'

const props = defineProps<{
  graphId: string
  classType?: string
}>()

const emit = defineEmits<{
  (e: 'edit-instance', data: any): void
}>()

const store = useOntologyStore()

// ---- State ----
const dataSource = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const refreshing = ref(false)
const keyword = ref('')
const selectedRowKeys = ref<string[]>([])
const rowKey = 'uuid'
const importExportRef = ref()

// ---- Edit State ----
const editingKey = ref('')
const editingRecord = reactive<Record<string, any>>({})

// ---- Detail Drawer ----
const detailDrawerVisible = ref(false)
const selectedRecord = ref<any>(null)

// ---- Context Menu ----
const contextMenu = reactive({ visible: false, x: 0, y: 0, record: null as any })

// ---- Dynamic Columns ----
const dynamicColumns = computed(() => {
  const cols: any[] = [
    { title: 'UUID', key: 'uuid', dataIndex: 'uuid', width: 220, fixed: 'left', ellipsis: true }
  ]
  // 根据类属性动态生成列
  const classProps = store.properties.filter(p =>
    !props.classType || store.classes.find(c => c.localName === props.classType && c.id === p.domainClassId)
  )
  const propNames = new Set(classProps.map(p => p.localName))
  propNames.forEach(name => {
    cols.push({
      title: name,
      key: name,
      dataIndex: name,
      width: 150,
      ellipsis: true
    })
  })
  cols.push({ title: '操作', key: 'action', width: 120, fixed: 'right' })
  return cols
})

const tableScrollX = computed(() => Math.max(dynamicColumns.value.length * 150, 600))
const tableScrollY = computed(() => 'calc(100vh - 340px)')

const detailColumns = computed(() => dynamicColumns.value.filter(c => c.key !== 'action'))

// ---- Methods ----

async function loadData() {
  if (!props.graphId) return
  loading.value = true
  try {
    const result = await graphApi.getClassInstances(props.graphId, props.classType || '', {
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined
    })
    dataSource.value = result.data ?? []
    total.value = result.total ?? dataSource.value.length
  } catch (e: any) {
    console.error('[InstanceDataTable] loadData failed', e)
    dataSource.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  refreshing.value = true
  await loadData()
  refreshing.value = false
  message.success('已刷新')
}

function handleTableChange(pagination: any) {
  page.value = pagination.current
  pageSize.value = pagination.pageSize
  loadData()
}

function handleSelectionChange(keys: string[]) {
  selectedRowKeys.value = keys
}

function startCellEdit(record: any, column: any) {
  if (column.key === 'uuid' || column.key === 'action') return
  editingKey.value = record[rowKey]
  Object.assign(editingRecord, record)
}

async function handleCellSave(record: any) {
  editingKey.value = ''
  try {
    await graphApi.updateNode(props.graphId, record[rowKey], {
      name: record.name,
      properties: editingRecord
    })
    message.success('已保存')
    loadData()
  } catch (e: any) {
    message.error(e.message || '保存失败')
  }
}

function handleAdd() {
  emit('edit-instance', { type: props.classType || '', name: '', properties: {} })
}

async function handleQuickExport() {
  const data = dataSource.value
  if (data.length === 0) {
    message.warning('无数据可导出')
    return
  }
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `instances-${props.classType || 'all'}-${Date.now()}.json`
  a.click()
  URL.revokeObjectURL(url)
  message.success(`已导出 ${data.length} 条记录`)
}

async function handleBatchDelete() {
  if (selectedRowKeys.value.length === 0) return
  if (!confirm(`确定删除选中的 ${selectedRowKeys.value.length} 条数据？`)) return
  let success = 0
  for (const uuid of selectedRowKeys.value) {
    try {
      await graphApi.deleteNode(props.graphId, uuid)
      success++
    } catch (e) { /* skip */ }
  }
  message.success(`已删除 ${success} 条`)
  selectedRowKeys.value = []
  loadData()
}

async function handleDelete(record: any) {
  try {
    await graphApi.deleteNode(props.graphId, record[rowKey])
    message.success('已删除')
    loadData()
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

function handleView(record: any) {
  selectedRecord.value = record
  detailDrawerVisible.value = true
}

function getCellText(record: any, column: any) {
  const val = record[column.dataIndex]
  if (val === null || val === undefined) return '-'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}

function truncateUuid(uuid: string) {
  if (!uuid) return '-'
  return uuid.length > 16 ? uuid.slice(0, 16) + '...' : uuid
}

async function copyUuid(uuid: string) {
  if (!uuid) return
  try {
    await navigator.clipboard.writeText(uuid)
    message.success('UUID已复制')
  } catch {
    message.warning('复制失败')
  }
}

// ---- Context Menu ----
function handleRowClick(_record: any) {
  // Reserved for future row click behavior
}

function handleContextMenu(e: MouseEvent, _record: any) {
  contextMenu.visible = true
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.record = _record
}

function handleCtxAction({ key }: { key: string }) {
  contextMenu.visible = false
  const record = contextMenu.record
  if (!record) return
  switch (key) {
    case 'view': handleView(record); break
    case 'edit': emit('edit-instance', record); break
    case 'copy-uuid': copyUuid(record?.uuid); break
    case 'delete': handleDelete(record); break
  }
}

function handleClickOutside() {
  if (contextMenu.visible) contextMenu.visible = false
}

// ---- Debounce ----
let searchTimer: any
function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadData(), 300)
}

watch(() => props.classType, () => { page.value = 1; loadData() })
watch(() => props.graphId, () => { page.value = 1; loadData() })
onMounted(() => {
  loadData()
  document.addEventListener('click', handleClickOutside)
})
</script>

<style scoped lang="less">
.instance-data-table {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  .table-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #161b22;
    border-bottom: 1px solid #30363d;
    flex-shrink: 0;
  }

  .table-stats {
    padding: 8px 16px;
    background: #161b22;
    border-bottom: 1px solid #21262d;
    flex-shrink: 0;

    .stat-label { font-size: 12px; color: #8b949e; }
    .stat-value { font-size: 13px; font-weight: 600; color: #e6edf3; }
  }

  .table-container {
    flex: 1;
    overflow: auto;
    padding: 0 16px 16px;
  }

  .cell-display {
    cursor: text;
    padding: 2px 4px;
    border-radius: 3px;
    min-height: 24px;
    transition: background 0.15s;

    &:hover { background: rgba(88, 166, 255, 0.1); }
  }

  .cell-editor {
    padding: 0;
  }

  .uuid-text {
    font-family: monospace;
    font-size: 12px;
    color: #8b949e;
  }
}

.import-steps {
  margin-bottom: 24px;
}

.import-step-content {
  min-height: 200px;
}
</style>
