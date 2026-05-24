<template>
  <div class="ontology-class-view">
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <span class="class-name">{{ schemaClass?.localName || '-' }}</span>
        <span class="instance-count">{{ total.toLocaleString() }} 个实例</span>
      </div>
      <div class="toolbar-center">
        <div class="tab-switcher">
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'graph' }"
            @click="activeTab = 'graph'"
          >
            图谱
          </button>
          <button
            class="tab-btn"
            :class="{ active: activeTab === 'list' }"
            @click="activeTab = 'list'"
          >
            实例列表
          </button>
        </div>
      </div>
      <div class="toolbar-right">
        <div class="pagination">
          <button
            class="page-btn"
            :disabled="page <= 1"
            @click="prevPage"
          >
            ◀
          </button>
          <span class="page-info">{{ page }} / {{ totalPages || 1 }}</span>
          <button
            class="page-btn"
            :disabled="page >= totalPages"
            @click="nextPage"
          >
            ▶
          </button>
        </div>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="content">
      <!-- 图谱 Tab -->
      <div v-show="activeTab === 'graph'" class="tab-content graph-tab">
        <GraphCanvas
          :graph-id="graphId"
          :nodes="graphNodes"
          :edges="graphEdges"
          :layout="'force'"
          :tool="'select'"
          :show-minimap="true"
          :selected-node="selectedNodeInGraph"
          @node-click="handleGraphNodeClick"
          @node-dblclick="handleGraphNodeDblclick"
        />
        <!-- 下一页按钮 -->
        <div v-if="page < totalPages" class="next-page-wrapper">
          <button class="next-page-btn" :disabled="loading" @click="nextPage">
            {{ loading ? '加载中...' : '下一页' }}
          </button>
        </div>
      </div>

      <!-- 实例列表 Tab -->
      <div v-show="activeTab === 'list'" class="tab-content list-tab">
        <InstanceDataTable
          :graph-id="graphId"
          :class-type="schemaClass?.localName"
          :page="page"
          :page-size="pageSize"
          @edit-instance="handleInstanceEdit"
          @page-change="handlePageChange"
          @row-click="handleInstanceRowClick"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { graphApi } from '@/api/graph'
import type { GraphIDENode, GraphIDEEdge, SchemaClass } from '@/api/graph'
import GraphCanvas from '@/components/Graph/GraphCanvas.vue'
import InstanceDataTable from './InstanceDataTable.vue'

interface Props {
  graphId: string
  schemaClass: SchemaClass | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'instance-click', node: GraphIDENode): void
  (e: 'instance-dblclick', node: GraphIDENode): void
}>()

// ---- State ----
const activeTab = ref<'graph' | 'list'>('graph')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value) || 1)

const graphNodes = ref<GraphIDENode[]>([])
const graphEdges = ref<GraphIDEEdge[]>([])
const loading = ref(false)
const selectedNodeInGraph = ref<GraphIDENode | null>(null)

// 去重用的 Map
const nodeMap = new Map<string, GraphIDENode>()
const edgeMap = new Map<string, GraphIDEEdge>()

// ---- Methods ----

async function loadData(append: boolean) {
  if (!props.graphId || !props.schemaClass?.localName) return

  loading.value = true
  try {
    const result = await graphApi.getEntitiesVisualizationByClass(
      props.graphId,
      props.schemaClass.localName,
      { page: page.value, pageSize: pageSize.value, depth: 2 }
    )

    // 更新分页信息
    if (result.pagination) {
      total.value = result.pagination.total
    }

    if (append) {
      // 追加模式：去重后拼接
      result.nodes.forEach(node => {
        if (!nodeMap.has(node.uuid)) {
          nodeMap.set(node.uuid, node)
          graphNodes.value.push(node)
        }
      })
      result.edges.forEach(edge => {
        if (!edgeMap.has(edge.uuid)) {
          edgeMap.set(edge.uuid, edge)
          graphEdges.value.push(edge)
        }
      })
    } else {
      // 替换模式：清空后重新填充
      nodeMap.clear()
      edgeMap.clear()
      graphNodes.value = []
      graphEdges.value = []

      result.nodes.forEach(node => {
        nodeMap.set(node.uuid, node)
        graphNodes.value.push(node)
      })
      result.edges.forEach(edge => {
        edgeMap.set(edge.uuid, edge)
        graphEdges.value.push(edge)
      })
    }
  } catch (e) {
    console.error('[OntologyClassView] loadData failed', e)
  } finally {
    loading.value = false
  }
}

function nextPage() {
  page.value++
  loadData(true)
}

function prevPage() {
  if (page.value <= 1) return
  page.value--
  loadData(false)
}

function handlePageChange(p: number, ps: number) {
  page.value = p
  pageSize.value = ps
  loadData(false)
}

function handleInstanceRowClick(record: GraphIDENode | any) {
  const node = graphNodes.value.find(n => n.uuid === record.uuid)
  if (node) {
    selectedNodeInGraph.value = node
    emit('instance-click', node)
  }
}

function handleGraphNodeClick(node: GraphIDENode) {
  selectedNodeInGraph.value = node
  emit('instance-click', node)
}

function handleGraphNodeDblclick(node: GraphIDENode) {
  selectedNodeInGraph.value = node
  emit('instance-dblclick', node)
}

function handleInstanceEdit(data: any) {
  // Forward to parent
}

// ---- Watch ----
watch(
  () => props.schemaClass,
  (newClass) => {
    if (newClass) {
      // 切换类：清空画布，重新加载第1页
      page.value = 1
      graphNodes.value = []
      graphEdges.value = []
      nodeMap.clear()
      edgeMap.clear()
      selectedNodeInGraph.value = null
      loadData(false)
    }
  },
  { deep: true }
)

onMounted(() => {
  if (props.schemaClass) {
    loadData(false)
  }
})
</script>

<style scoped lang="less">
.ontology-class-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0d1117;
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: #161b22;
  border-bottom: 1px solid #30363d;
  flex-shrink: 0;
  gap: 12px;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
    min-width: 0;

    .class-name {
      font-size: 14px;
      font-weight: 600;
      color: #58a6ff;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .instance-count {
      font-size: 12px;
      color: #8b949e;
      white-space: nowrap;
    }
  }

  .toolbar-center {
    flex-shrink: 0;
  }

  .toolbar-right {
    flex-shrink: 0;
  }
}

.tab-switcher {
  display: flex;
  background: #21262d;
  border: 1px solid #30363d;
  border-radius: 6px;
  overflow: hidden;

  .tab-btn {
    padding: 4px 12px;
    font-size: 12px;
    color: #8b949e;
    background: transparent;
    border: none;
    cursor: pointer;
    transition: all 0.15s;

    &:hover {
      color: #e6edf3;
    }

    &.active {
      background: #238636;
      color: #ffffff;
    }
  }
}

.pagination {
  display: flex;
  align-items: center;
  gap: 8px;

  .page-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    background: #21262d;
    border: 1px solid #30363d;
    border-radius: 4px;
    color: #8b949e;
    font-size: 12px;
    cursor: pointer;
    transition: all 0.15s;

    &:hover:not(:disabled) {
      background: #30363d;
      color: #e6edf3;
      border-color: #58a6ff;
    }

    &:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  }

  .page-info {
    font-size: 12px;
    color: #8b949e;
    min-width: 50px;
    text-align: center;
  }
}

.content {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.tab-content {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.graph-tab {
  position: relative;
  display: flex;
  flex-direction: column;
}

.list-tab {
  overflow: auto;
}

.next-page-wrapper {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
}

.next-page-btn {
  padding: 8px 24px;
  background: #238636;
  border: none;
  border-radius: 6px;
  color: #ffffff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);

  &:hover:not(:disabled) {
    background: #2ea043;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}
</style>
