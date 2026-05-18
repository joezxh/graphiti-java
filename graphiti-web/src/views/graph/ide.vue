<template>
  <div class="graph-ide-container">
    <!-- Header -->
    <header class="ide-header">
      <div class="header-left">
        <div class="logo">
          <svg viewBox="0 0 32 32" fill="none" width="28" height="28">
            <circle cx="16" cy="16" r="14" stroke="url(#logoGradient)" stroke-width="2"/>
            <circle cx="10" cy="12" r="3" fill="#58a6ff"/>
            <circle cx="22" cy="12" r="3" fill="#a371f7"/>
            <circle cx="16" cy="22" r="3" fill="#3fb950"/>
            <defs>
              <linearGradient id="logoGradient" x1="0" y1="0" x2="32" y2="32">
                <stop offset="0%" stop-color="#58a6ff"/>
                <stop offset="100%" stop-color="#a371f7"/>
              </linearGradient>
            </defs>
          </svg>
          <span class="logo-text">Graphiti</span>
        </div>
        
        <div class="breadcrumb">
          <span class="breadcrumb-item" @click="$router.push('/graph/ide')">图谱管理</span>
          <span class="breadcrumb-sep">/</span>
          <a-select
            v-if="!graphId"
            v-model:value="selectedGraphId"
            placeholder="请选择图谱"
            style="width: 160px; margin-left: 8px;"
            :loading="loadingGraphs"
            @change="handleGraphChange"
          >
            <a-select-option v-for="g in graphList" :key="g.graphId" :value="g.graphId">
              {{ g.name }}
            </a-select-option>
          </a-select>
          <span v-else class="breadcrumb-current">{{ graphData?.name || '加载中...' }}</span>
        </div>
      </div>

      <div class="header-actions">
        <a-button class="action-btn" @click="handleSync">
          <template #icon><SyncOutlined :spin="syncing" /></template>
          同步
        </a-button>
      </div>
    </header>

    <!-- Main Content -->
    <div class="ide-main">
      <!-- Sidebar -->
      <aside class="ide-sidebar">
        <div class="sidebar-header">
          <div class="sidebar-tabs">
            <button
              class="sidebar-tab"
              :class="{ active: sidebarTab === 'explorer' }"
              @click="sidebarTab = 'explorer'"
            >
              资源
            </button>
            <button
              class="sidebar-tab"
              :class="{ active: sidebarTab === 'schema' }"
              @click="sidebarTab = 'schema'"
            >
              Schema
            </button>
          </div>
        </div>

        <div class="sidebar-content">
          <!-- Explorer Tab -->
          <div v-if="sidebarTab === 'explorer'" class="explorer-tree">
            <div class="tree-section">
              <div class="tree-node tree-node-root" @click="toggleTree('graph')">
                <span class="tree-icon">{{ isTreeExpanded('graph') ? '📂' : '📁' }}</span>
                <span class="tree-label">{{ graphData?.name || '图谱' }}</span>
                <span class="tree-badge">{{ formatNumber(graphData?.nodeCount || 0) }}</span>
              </div>
              
              <div v-show="isTreeExpanded('graph')" class="tree-children">
                <!-- Ontology -->
                <div class="tree-node" @click="toggleTree('ontology')">
                  <span class="tree-icon">{{ isTreeExpanded('ontology') ? '📂' : '📁' }}</span>
                  <span class="tree-label">本体定义</span>
                </div>
                
                <div v-show="isTreeExpanded('ontology')" class="tree-children">
                  <div class="tree-node" @click="toggleTree('classes')">
                    <span class="tree-icon">{{ isTreeExpanded('classes') ? '📂' : '📁' }}</span>
                    <span class="tree-label">类</span>
                    <span class="tree-badge">{{ schemaClasses.length }}</span>
                  </div>
                  
                  <div v-show="isTreeExpanded('classes')" class="tree-children">
                    <div
                      v-for="cls in schemaClasses"
                      :key="cls.id"
                      class="tree-node tree-node-leaf"
                      :class="{ selected: selectedClass?.id === cls.id }"
                      @click="selectClass(cls)"
                    >
                      <span class="tree-icon type-icon">◉</span>
                      <span class="tree-label">{{ cls.localName }}</span>
                      <span class="tree-badge">{{ cls.propertyCount }}</span>
                    </div>
                  </div>
                  
                  <div class="tree-node tree-node-leaf">
                    <span class="tree-icon type-icon" style="color: #a371f7;">◈</span>
                    <span class="tree-label">关系类型</span>
                    <span class="tree-badge">{{ graphData?.edgeCount ? 1 : 0 }}</span>
                  </div>
                </div>
                
                <div class="tree-node tree-node-leaf">
                  <span class="tree-icon type-icon" style="color: #3fb950;">◉</span>
                  <span class="tree-label">实例数据</span>
                  <span class="tree-badge">{{ formatNumber(graphData?.nodeCount || 0) }}</span>
                </div>
                
                <div class="tree-node tree-node-leaf">
                  <span class="tree-icon type-icon" style="color: #d29922;">◆</span>
                  <span class="tree-label">边</span>
                  <span class="tree-badge">{{ formatNumber(graphData?.edgeCount || 0) }}</span>
                </div>
                
                <div class="tree-node tree-node-leaf">
                  <span class="tree-icon type-icon" style="color: #f85149;">◇</span>
                  <span class="tree-label">事件流</span>
                  <span class="tree-badge">{{ graphData?.episodeCount || 0 }}</span>
                </div>
                
                <div class="tree-node tree-node-leaf">
                  <span class="tree-icon type-icon" style="color: #8b949e;">○</span>
                  <span class="tree-label">社区</span>
                  <span class="tree-badge">{{ graphData?.communityCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Schema Tab -->
          <div v-if="sidebarTab === 'schema'" class="schema-tree">
            <div class="search-box">
              <a-input
                v-model:value="schemaSearch"
                placeholder="搜索类..."
                size="small"
                allow-clear
              >
                <template #prefix><SearchOutlined /></template>
              </a-input>
            </div>
            
            <div class="class-list">
              <div
                v-for="cls in filteredSchemaClasses"
                :key="cls.id"
                class="class-item"
                :class="{ selected: selectedClass?.id === cls.id }"
                @click="selectClass(cls)"
              >
                <div class="class-icon" :style="{ background: getClassColor(cls.localName) }">
                  {{ cls.localName.charAt(0) }}
                </div>
                <div class="class-info">
                  <div class="class-name">{{ cls.localName }}</div>
                  <div class="class-count">{{ cls.propertyCount }} 个属性</div>
                </div>
              </div>
              
              <div v-if="filteredSchemaClasses.length === 0" class="empty-tip">
                暂无类定义
              </div>
            </div>
            
            <a-button type="link" class="add-class-btn" @click="showSchemaEditor = true">
              <template #icon><PlusOutlined /></template>
              添加类
            </a-button>
          </div>
        </div>
      </aside>

      <!-- Canvas Area -->
      <div class="ide-canvas">
        <!-- Toolbar -->
        <div class="canvas-toolbar">
          <div class="toolbar-group">
            <a-tooltip title="添加节点">
              <a-button type="text" size="small" @click="addNode">
                <template #icon><PlusOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="添加边">
              <a-button type="text" size="small" @click="addEdge">
                <template #icon><LinkOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-divider type="vertical" />
            <a-tooltip title="选择">
              <a-button
                type="text"
                size="small"
                :class="{ active: currentTool === 'select' }"
                @click="currentTool = 'select'"
              >
                <template #icon><AimOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="平移">
              <a-button
                type="text"
                size="small"
                :class="{ active: currentTool === 'pan' }"
                @click="currentTool = 'pan'"
              >
                <template #icon><DragOutlined /></template>
              </a-button>
            </a-tooltip>
          </div>

          <div class="toolbar-separator" />

          <div class="layout-selector">
            <a-tooltip
              v-for="layout in layouts"
              :key="layout.key"
              :title="layout.label"
            >
              <a-button
                size="small"
                :class="{ active: currentLayout === layout.key }"
                @click="currentLayout = layout.key"
              >
                <template #icon><component :is="layout.icon" style="color: #fff" /></template>
                <span style="color: #fff">{{ layout.label }}</span>
              </a-button>
            </a-tooltip>
          </div>

          <div class="toolbar-separator" />

          <div class="toolbar-group">
            <a-tooltip title="小地图">
              <a-button
                type="text"
                size="small"
                :class="{ active: showMinimap }"
                @click="showMinimap = !showMinimap"
              >
                <template #icon><BorderOutlined style="color: #fff" /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="聚合视图">
              <a-button
                type="text"
                size="small"
                :class="{ active: aggregationMode }"
                @click="aggregationMode = !aggregationMode"
              >
                <template #icon><ApartmentOutlined style="color: #fff" /></template>
              </a-button>
            </a-tooltip>
          </div>

          <div class="toolbar-spacer" />

          <div class="toolbar-group">
            <a-input-search
              v-model:value="searchKeyword"
              placeholder="搜索节点..."
              size="small"
              style="width: 180px"
              @search="handleSearch"
            />
          </div>

          <a-button type="primary" @click="showCascadeModal = true">
            级联编辑
          </a-button>
        </div>

        <!-- Canvas -->
        <div class="canvas-wrapper">
          <GraphCanvas
            ref="graphCanvasRef"
            :graph-id="graphId"
            :nodes="nodes"
            :edges="edges"
            :layout="currentLayout"
            :tool="currentTool"
            :show-minimap="showMinimap"
            :aggregation-mode="aggregationMode"
            :selected-node="selectedNode"
            @node-click="handleNodeClick"
            @node-dblclick="handleNodeDblClick"
            @node-contextmenu="handleNodeContextMenu"
          />
        </div>
      </div>

      <!-- Right Panel -->
      <aside class="ide-panel" :class="{ collapsed: !showPanel }">
        <template v-if="selectedNode || selectedClass">
          <div class="panel-header">
            <span class="panel-title">
              {{ selectedNode ? '节点详情' : '类详情' }}
            </span>
            <a-button type="text" size="small" @click="closePanel">
              <template #icon><CloseOutlined /></template>
            </a-button>
          </div>

          <div class="panel-tabs">
            <div
              v-for="tab in detailTabs"
              :key="tab.key"
              class="panel-tab"
              :class="{ active: currentDetailTab === tab.key }"
              @click="currentDetailTab = tab.key"
            >
              {{ tab.label }}
            </div>
          </div>

          <div class="panel-content">
            <!-- Info Tab -->
            <div v-if="currentDetailTab === 'info'" class="detail-section">
              <div class="section-title">基本信息</div>
              <div class="info-list">
                <div class="info-row">
                  <span class="info-label">名称</span>
                  <span class="info-value">{{ selectedNode?.name || selectedClass?.localName || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">类型</span>
                  <a-tag :color="getNodeColor(selectedNode?.type || selectedClass?.localName || '')">
                    {{ selectedNode?.type || selectedClass?.localName }}
                  </a-tag>
                </div>
                <div v-if="selectedNode?.summary" class="info-row">
                  <span class="info-label">摘要</span>
                  <span class="info-value">{{ selectedNode.summary }}</span>
                </div>
                <div v-if="selectedClass?.classUri" class="info-row">
                  <span class="info-label">URI</span>
                  <span class="info-value uri">{{ selectedClass.classUri }}</span>
                </div>
                <div v-if="selectedClass?.description" class="info-row">
                  <span class="info-label">描述</span>
                  <span class="info-value">{{ selectedClass.description }}</span>
                </div>
                <div class="info-row">
                  <span class="info-label">属性数</span>
                  <span class="info-value">{{ selectedClass?.propertyCount || 0 }}</span>
                </div>
                <div v-if="selectedNode?.createdAt" class="info-row">
                  <span class="info-label">创建时间</span>
                  <span class="info-value">{{ selectedNode.createdAt }}</span>
                </div>
              </div>
            </div>

            <!-- Properties Tab -->
            <div v-if="currentDetailTab === 'properties'" class="detail-section">
              <div class="section-title">属性列表</div>
              <div class="property-list">
                <div
                  v-for="(value, key) in displayProperties"
                  :key="key"
                  class="property-item"
                >
                  <span class="property-key">{{ key }}</span>
                  <span class="property-value">{{ formatValue(value) }}</span>
                </div>
                <div v-if="Object.keys(displayProperties).length === 0" class="empty-tip">
                  暂无属性
                </div>
              </div>
              <a-button
                v-if="selectedNode"
                type="link"
                class="add-property-btn"
                @click="showPropertyForm = true"
              >
                <template #icon><PlusOutlined /></template>
                添加属性
              </a-button>
            </div>

            <!-- Relations Tab -->
            <div v-if="currentDetailTab === 'relations' && selectedNode" class="detail-section">
              <div class="section-title">关联关系</div>
              <div class="relation-list">
                <div
                  v-for="rel in nodeRelations"
                  :key="rel.id"
                  class="relation-item"
                  @click="navigateToNode(rel.targetId)"
                >
                  <a-tag color="purple">{{ rel.type }}</a-tag>
                  <span class="relation-name">{{ rel.targetName }}</span>
                  <RightOutlined class="relation-arrow" />
                </div>
                <div v-if="nodeRelations.length === 0" class="empty-tip">
                  暂无关联关系
                </div>
              </div>
            </div>

            <!-- Instances Tab -->
            <div v-if="currentDetailTab === 'instances' && selectedClass" class="detail-section">
              <div class="section-title">类实例</div>
              
              <div class="instance-toolbar">
                <a-input-search
                  v-model:value="instanceSearchKeyword"
                  placeholder="搜索实例名称"
                  style="width: 100%"
                  @search="handleInstanceSearch"
                  @change="handleInstanceSearchChange"
                />
              </div>

              <div class="instance-list">
                <div
                  v-for="instance in classInstances"
                  :key="instance.uuid"
                  class="instance-item"
                  :class="{ 
                    'selected': selectedClassInstance?.uuid === instance.uuid,
                    'in-canvas': instance.inCanvas
                  }"
                  @click="handleSelectInstance(instance)"
                >
                  <div class="instance-info">
                    <div class="instance-name">{{ instance.name }}</div>
                    <div class="instance-meta">
                      <a-tag size="small">{{ instance.type }}</a-tag>
                      <span v-if="instance.summary" class="instance-summary">
                        {{ instance.summary.substring(0, 30) }}{{ instance.summary.length > 30 ? '...' : '' }}
                      </span>
                    </div>
                  </div>
                  <div class="instance-actions">
                    <a-tooltip title="在画布中定位">
                      <a-button 
                        type="text" 
                        size="small"
                        @click.stop="locateInstanceInCanvas(instance)"
                      >
                        <template #icon><AimOutlined /></template>
                      </a-button>
                    </a-tooltip>
                    <a-tooltip title="添加到画布">
                      <a-button 
                        type="text" 
                        size="small"
                        @click.stop="addInstanceToCanvas(instance)"
                      >
                        <template #icon><PlusOutlined /></template>
                      </a-button>
                    </a-tooltip>
                  </div>
                </div>
                
                <div v-if="instanceLoading" class="instance-loading">
                  <a-spin size="small" />
                  <span>加载中...</span>
                </div>
                
                <div v-if="!instanceLoading && classInstances.length === 0" class="empty-tip">
                  暂无实例数据
                </div>
              </div>

              <div v-if="classInstanceTotal > instancePageSize" class="instance-pagination">
                <a-pagination
                  v-model:current="instancePage"
                  :page-size="instancePageSize"
                  :total="classInstanceTotal"
                  size="small"
                  @change="handleInstancePageChange"
                />
              </div>
            </div>
          </div>

          <div class="panel-footer">
            <a-button
              v-if="selectedNode"
              type="primary"
              block
              @click="editSelectedNode"
            >
              编辑
            </a-button>
            <a-button
              v-if="selectedClass"
              type="primary"
              block
              @click="showSchemaEditor = true"
            >
              编辑 Schema
            </a-button>
            <a-popconfirm
              title="确定要删除吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="deleteSelected"
            >
              <a-button danger block>
                删除
              </a-button>
            </a-popconfirm>
          </div>
        </template>

        <div v-else class="panel-empty">
          <InboxOutlined class="empty-icon" />
          <div class="empty-title">选择节点或类</div>
          <div class="empty-desc">点击图谱中的节点或左侧资源管理器中的类查看详情</div>
        </div>
      </aside>
    </div>

    <!-- Status Bar -->
    <footer class="ide-status">
      <div class="status-item">
        <span class="status-dot success" />
        <span>Neo4j 已连接</span>
      </div>
      <div class="status-item">
        节点: <strong>{{ formatNumber(graphData?.nodeCount || 0) }}</strong>
      </div>
      <div class="status-item">
        边: <strong>{{ formatNumber(graphData?.edgeCount || 0) }}</strong>
      </div>
      <div class="status-item">
        类: <strong>{{ schemaClasses.length }}</strong>
      </div>
      <div class="status-item">
        事件: <strong>{{ graphData?.episodeCount || 0 }}</strong>
      </div>
      <div class="status-spacer" />
      <div class="status-item">
        显示: {{ nodes.length }} / {{ formatNumber(graphData?.nodeCount || 0) }}
      </div>
    </footer>

    <!-- Context Menu -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click.stop
    >
      <div class="context-menu-item" @click="viewNodeDetails">
        <EyeOutlined /> 查看详情
      </div>
      <div class="context-menu-item" @click="editNodeContext">
        <EditOutlined /> 编辑属性
      </div>
      <div class="context-menu-separator" />
      <div class="context-menu-item" @click="expandNeighbors">
        <ExpandOutlined /> 展开邻居
      </div>
      <div class="context-menu-item" @click="addRelationContext">
        <LinkOutlined /> 添加关联
      </div>
      <div class="context-menu-separator" />
      <div class="context-menu-item danger" @click="deleteNodeContext">
        <DeleteOutlined /> 删除节点
      </div>
    </div>

    <!-- Modals -->
    <CascadeEditModal
      v-model:visible="showCascadeModal"
      :graph-id="graphId"
      :classes="schemaClasses"
      @success="handleCascadeSuccess"
    />

    <NodeEditModal
      v-model:visible="showNodeEditModal"
      :graph-id="graphId"
      :node="editingNode"
      :classes="schemaClasses"
      @success="handleNodeEditSuccess"
    />

    <SchemaEditorModal
      v-model:visible="showSchemaEditor"
      :graph-id="graphId"
      :selected-class="selectedClass"
      :classes="schemaClasses"
      @success="handleSchemaSuccess"
    />

    <AddEdgeModal
      v-if="addingEdgeSource"
      v-model:visible="showAddEdgeModal"
      :graph-id="graphId"
      :source-node="addingEdgeSource"
      :nodes="nodes"
      @success="handleAddEdgeSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  SyncOutlined,
  SettingOutlined,
  SearchOutlined,
  PlusOutlined,
  LinkOutlined,
  AimOutlined,
  DragOutlined,
  ApartmentOutlined,
  CloseOutlined,
  RightOutlined,
  EyeOutlined,
  EditOutlined,
  ExpandOutlined,
  DeleteOutlined,
  InboxOutlined,
  BorderOutlined,
  NodeIndexOutlined,
  AppstoreOutlined,
  UnorderedListOutlined,
  ClusterOutlined
} from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import type {
  GraphMetadata,
  GraphIDENode,
  GraphIDEEdge,
  SchemaClass,
  ClassInstance,
  LayoutType,
  EditTool,
  SidebarTab
} from '@/api/graph'
import type { DetailPanelTab } from '@/types/graph-ide'
import GraphCanvas from '@/components/Graph/GraphCanvas.vue'
import CascadeEditModal from '@/components/Graph/CascadeEditModal.vue'
import NodeEditModal from '@/components/Graph/NodeEditModal.vue'
import SchemaEditorModal from '@/components/Graph/SchemaEditorModal.vue'
import AddEdgeModal from '@/components/Graph/AddEdgeModal.vue'

const route = useRoute()
const router = useRouter()

// Route params
const graphId = computed(() => route.params.id as string)
const selectedGraphId = ref<string>('')
const graphList = ref<Array<{ graphId: string; name: string }>>([])
const loadingGraphs = ref(false)

// Computed effective graph ID
const effectiveGraphId = computed(() => graphId.value || selectedGraphId.value)

// State
const loading = ref(false)
const syncing = ref(false)
const showSettings = ref(false)

// Sidebar
const sidebarTab = ref<SidebarTab>('explorer')
const schemaSearch = ref('')
const expandedTrees = reactive<Record<string, boolean>>({
  graph: true,
  ontology: true,
  classes: true
})

// Canvas
const graphCanvasRef = ref()
const currentTool = ref<EditTool>('select')
const currentLayout = ref<LayoutType>('force')
const showMinimap = ref(true)
const aggregationMode = ref(false)
const searchKeyword = ref('')

// Panels
const showPanel = ref(false)
const selectedNode = ref<GraphIDENode | null>(null)
const selectedClass = ref<SchemaClass | null>(null)
const currentDetailTab = ref<DetailPanelTab>('info')

// Context Menu
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  node: null as GraphIDENode | null
})

// Modals
const showCascadeModal = ref(false)
const showNodeEditModal = ref(false)
const showSchemaEditor = ref(false)
const showAddEdgeModal = ref(false)
const showPropertyForm = ref(false)
const editingNode = ref<GraphIDENode | null>(null)
const addingEdgeSource = ref<GraphIDENode | null>(null)

// Data
const graphData = ref<GraphMetadata | null>(null)
const nodes = ref<GraphIDENode[]>([])
const edges = ref<GraphIDEEdge[]>([])
const schemaClasses = ref<SchemaClass[]>([])
const nodeRelations = ref<Array<{ id: string; type: string; targetId: string; targetName: string }>>([])

// Class instances
const classInstances = ref<ClassInstance[]>([])
const classInstanceTotal = ref(0)
const selectedClassInstance = ref<ClassInstance | null>(null)
const instanceLoading = ref(false)
const instancePage = ref(1)
const instancePageSize = ref(20)
const instanceSearchKeyword = ref('')

// Constants
// Constants - 布局类型说明:
// - force (力导向): 基于物理模拟的力导向算法，节点相互排斥，边像弹簧一样连接
// - grid (网格): 节点均匀分布在网格中，适合结构化数据展示
// - dagre (层次): 有向无环图布局，节点按层级从左到右或从上到下排列
// - concentric (同心): 节点按度数排列在同心圆上，中心节点最重要
const layouts = [
  { key: 'force' as LayoutType, label: '力导向', icon: NodeIndexOutlined },
  { key: 'grid' as LayoutType, label: '网格', icon: AppstoreOutlined },
  { key: 'dagre' as LayoutType, label: '层次', icon: UnorderedListOutlined },
  { key: 'concentric' as LayoutType, label: '同心', icon: ClusterOutlined }
]

const detailTabs = [
  { key: 'info' as DetailPanelTab, label: '基本信息' },
  { key: 'properties' as DetailPanelTab, label: '属性' },
  { key: 'relations' as DetailPanelTab, label: '关系' },
  { key: 'instances' as DetailPanelTab, label: '实例' }
]

// Computed
const filteredSchemaClasses = computed(() => {
  if (!schemaSearch.value) return schemaClasses.value
  const keyword = schemaSearch.value.toLowerCase()
  return schemaClasses.value.filter(c =>
    c.localName.toLowerCase().includes(keyword)
  )
})

const displayProperties = computed(() => {
  if (selectedNode.value?.properties) return selectedNode.value.properties
  if (selectedClass.value?.properties) {
    const props: Record<string, any> = {}
    selectedClass.value.properties?.forEach(p => {
      props[p.localName] = p.rangeDataType || p.propertyType
    })
    return props
  }
  return {}
})

// Methods
const formatNumber = (num: number): string => {
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k'
  return num.toString()
}

const formatValue = (value: any): string => {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

const getNodeColor = (type: string): string => {
  const colors: Record<string, string> = {
    Person: '#58a6ff',
    Company: '#3fb950',
    Product: '#d29922',
    Order: '#a371f7',
    Location: '#f85149',
    Event: '#8b5cf6',
    Review: '#06b6d4',
    Category: '#84cc16'
  }
  return colors[type] || '#6e7681'
}

const getClassColor = (name: string): string => {
  return getNodeColor(name)
}

const toggleTree = (key: string) => {
  expandedTrees[key] = !expandedTrees[key]
}

const isTreeExpanded = (key: string): boolean => {
  return expandedTrees[key] === true
}

const selectClass = async (cls: SchemaClass) => {
  selectedClass.value = cls
  selectedNode.value = null
  showPanel.value = true
  currentDetailTab.value = 'info'
}

const handleNodeClick = (node: GraphIDENode) => {
  selectedNode.value = node
  selectedClass.value = null
  showPanel.value = true
  currentDetailTab.value = 'info'
  loadNodeRelations(node.uuid)
}

const handleNodeDblClick = (node: GraphIDENode) => {
  editingNode.value = node
  showNodeEditModal.value = true
}

const handleNodeContextMenu = (event: MouseEvent, node: GraphIDENode) => {
  contextMenu.visible = true
  contextMenu.x = event.clientX
  contextMenu.y = event.clientY
  contextMenu.node = node
}

const viewNodeDetails = () => {
  if (contextMenu.node) {
    handleNodeClick(contextMenu.node)
  }
  contextMenu.visible = false
}

const editNodeContext = () => {
  if (contextMenu.node) {
    editingNode.value = contextMenu.node
    showNodeEditModal.value = true
  }
  contextMenu.visible = false
}

const expandNeighbors = async () => {
  if (contextMenu.node) {
    try {
      const result = await graphApi.expandNeighbors(effectiveGraphId.value, contextMenu.node.uuid)
      nodes.value = [...nodes.value, ...result.nodes]
      edges.value = [...edges.value, ...result.edges]
      message.success(`已展开 ${result.nodes.length} 个邻居节点`)
    } catch (error) {
      message.error('展开邻居失败')
    }
  }
  contextMenu.visible = false
}

const addRelationContext = () => {
  if (contextMenu.node) {
    addingEdgeSource.value = contextMenu.node
    showAddEdgeModal.value = true
  }
  contextMenu.visible = false
}

const deleteNodeContext = async () => {
  if (contextMenu.node) {
    try {
      await graphApi.deleteNode(effectiveGraphId.value, contextMenu.node.uuid)
      nodes.value = nodes.value.filter(n => n.uuid !== contextMenu.node!.uuid)
      if (selectedNode.value?.uuid === contextMenu.node.uuid) {
        selectedNode.value = null
        showPanel.value = false
      }
      message.success('删除成功')
    } catch (error) {
      message.error('删除失败')
    }
  }
  contextMenu.visible = false
}

const loadNodeRelations = async (uuid: string) => {
  try {
    const detail = await graphApi.getNodeDetail(effectiveGraphId.value, uuid)
    nodeRelations.value = detail.relations || []
  } catch (error) {
    nodeRelations.value = []
  }
}

const closePanel = () => {
  showPanel.value = false
  selectedNode.value = null
  selectedClass.value = null
}

const editSelectedNode = () => {
  if (selectedNode.value) {
    editingNode.value = selectedNode.value
    showNodeEditModal.value = true
  }
}

const deleteSelected = async () => {
  if (selectedNode.value) {
    try {
      await graphApi.deleteNode(effectiveGraphId.value, selectedNode.value.uuid)
      nodes.value = nodes.value.filter(n => n.uuid !== selectedNode.value!.uuid)
      selectedNode.value = null
      showPanel.value = false
      message.success('删除成功')
    } catch (error) {
      message.error('删除失败')
    }
  }
}

const navigateToNode = async (uuid: string) => {
  const node = nodes.value.find(n => n.uuid === uuid)
  if (node) {
    handleNodeClick(node)
  } else {
    try {
      const detail = await graphApi.getNodeDetail(effectiveGraphId.value, uuid)
      handleNodeClick(detail)
    } catch (error) {
      message.error('加载节点失败')
    }
  }
}

const addNode = () => {
  editingNode.value = null
  showNodeEditModal.value = true
}

const addEdge = () => {
  if (selectedNode.value) {
    addingEdgeSource.value = selectedNode.value
  } else {
    message.info('请先选择一个源节点')
    return
  }
  showAddEdgeModal.value = true
}

const handleSearch = () => {
  // 实现搜索功能
  message.info(`搜索: ${searchKeyword.value}`)
}

const handleSync = async () => {
  syncing.value = true
  try {
    await loadAllData()
    message.success('同步完成')
  } catch (error) {
    message.error('同步失败')
  } finally {
    syncing.value = false
  }
}

const handleCascadeSuccess = () => {
  loadGraphData()
}

const handleNodeEditSuccess = (updatedNode: GraphIDENode) => {
  const index = nodes.value.findIndex(n => n.uuid === updatedNode.uuid)
  if (index >= 0) {
    nodes.value[index] = updatedNode
  }
  if (selectedNode.value?.uuid === updatedNode.uuid) {
    selectedNode.value = updatedNode
  }
}

const handleAddEdgeSuccess = (newEdge: GraphIDEEdge) => {
  edges.value.push(newEdge)
}

const handleSchemaSuccess = () => {
  loadSchemaClasses()
}

// Load data
const loadGraphList = async () => {
  loadingGraphs.value = true
  try {
    graphList.value = await graphApi.getList()
  } catch (error) {
    console.error('加载图谱列表失败:', error)
  } finally {
    loadingGraphs.value = false
  }
}

const loadGraphMetadata = async () => {
  if (!effectiveGraphId.value) return
  try {
    graphData.value = await graphApi.getGraphMetadata(effectiveGraphId.value)
  } catch (error) {
    console.error('加载图谱元数据失败:', error)
  }
}

const loadGraphData = async () => {
  if (!effectiveGraphId.value) return
  loading.value = true
  try {
    const data = await graphApi.getVisualization(effectiveGraphId.value, {
      layout: currentLayout.value,
      page: 1,
      pageSize: 500
    })
    nodes.value = data.nodes || []
    edges.value = data.edges || []
  } catch (error) {
    console.error('加载图谱数据失败:', error)
  } finally {
    loading.value = false
  }
}

const loadSchemaClasses = async () => {
  if (!effectiveGraphId.value) return
  try {
    schemaClasses.value = await graphApi.getSchemaClasses(effectiveGraphId.value)
  } catch (error) {
    console.error('加载类列表失败:', error)
  }
}

// Load class instances
const loadClassInstances = async () => {
  if (!effectiveGraphId.value || !selectedClass.value) return
  
  instanceLoading.value = true
  try {
    const classType = selectedClass.value.localName
    const result = await graphApi.getClassInstances(effectiveGraphId.value, classType, {
      page: instancePage.value,
      pageSize: instancePageSize.value,
      keyword: instanceSearchKeyword.value || undefined
    })
    
    // Mark instances that are currently in canvas
    classInstances.value = result.data.map(inst => ({
      ...inst,
      inCanvas: nodes.value.some(n => n.uuid === inst.uuid)
    }))
    classInstanceTotal.value = result.total
  } catch (error) {
    console.error('加载类实例失败:', error)
  } finally {
    instanceLoading.value = false
  }
}

// Handle instance search
const handleInstanceSearch = () => {
  instancePage.value = 1
  loadClassInstances()
}

const handleInstanceSearchChange = () => {
  // Debounce could be added here
  if (!instanceSearchKeyword.value) {
    handleInstanceSearch()
  }
}

// Handle instance page change
const handleInstancePageChange = (page: number) => {
  instancePage.value = page
  loadClassInstances()
}

// Handle instance selection
const handleSelectInstance = (instance: ClassInstance) => {
  // If instance is already selected, deselect it
  if (selectedClassInstance.value?.uuid === instance.uuid) {
    selectedClassInstance.value = null
    return
  }
  
  selectedClassInstance.value = instance
  
  // If the instance is in canvas, also select the corresponding node
  const nodeInCanvas = nodes.value.find(n => n.uuid === instance.uuid)
  if (nodeInCanvas) {
    handleNodeClick(nodeInCanvas)
  }
}

// Locate instance in canvas
const locateInstanceInCanvas = (instance: ClassInstance) => {
  // Find the node in canvas
  const node = nodes.value.find(n => n.uuid === instance.uuid)
  if (node) {
    // Select the node
    handleNodeClick(node)
  } else {
    // Add to canvas first
    addInstanceToCanvas(instance)
  }
}

// Add instance to canvas
const addInstanceToCanvas = (instance: ClassInstance) => {
  // Create a node from the instance
  const newNode: GraphIDENode = {
    uuid: instance.uuid,
    name: instance.name,
    type: instance.type,
    properties: instance.properties,
    summary: instance.summary,
    x: 100 + Math.random() * 400,
    y: 100 + Math.random() * 300
  }
  
  // Add to canvas nodes if not already there
  if (!nodes.value.some(n => n.uuid === instance.uuid)) {
    nodes.value.push(newNode)
    // Update inCanvas flag
    classInstances.value = classInstances.value.map(inst =>
      inst.uuid === instance.uuid ? { ...inst, inCanvas: true } : inst
    )
  }
}

// Handle graph selection change
const handleGraphChange = (value: string) => {
  router.push(`/graph/ide/${value}`)
}

const loadAllData = async () => {
  await Promise.all([
    loadGraphMetadata(),
    loadGraphData(),
    loadSchemaClasses()
  ])
}

// Hide context menu on click outside
const handleDocumentClick = (e: MouseEvent) => {
  if (contextMenu.visible) {
    contextMenu.visible = false
  }
}

// Lifecycle
onMounted(async () => {
  await loadGraphList()
  if (effectiveGraphId.value) {
    await loadAllData()
  }
  document.addEventListener('click', handleDocumentClick)
})

onUnmounted(() => {
  document.removeEventListener('click', handleDocumentClick)
})

// Watch route params changes
watch(() => route.params.id, async (newId) => {
  if (newId) {
    selectedGraphId.value = newId as string
    await loadAllData()
  }
})

// Watch layout changes
watch(currentLayout, () => {
  loadGraphData()
})

// Watch tab changes - load instances when switching to instances tab
watch(currentDetailTab, (newTab) => {
  if (newTab === 'instances' && selectedClass.value) {
    loadClassInstances()
  }
})
</script>

<style scoped lang="less">
.graph-ide-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #0d1117;
  color: #e6edf3;
  overflow: hidden;
}

// Header
.ide-header {
  height: 56px;
  background: #161b22;
  border-bottom: 1px solid #30363d;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 24px;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 8px;

    .logo-text {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .breadcrumb {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;

    .breadcrumb-item {
      color: #8b949e;
      cursor: pointer;

      &:hover {
        color: #58a6ff;
      }
    }

    .breadcrumb-sep {
      color: #6e7681;
    }

    .breadcrumb-current {
      color: #e6edf3;
    }
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 12px;

    .action-btn {
      background: transparent;
      border: 1px solid #30363d;
      color: #ffffff !important;

      &:hover {
        border-color: #58a6ff;
        color: #58a6ff !important;
      }
    }

    .user-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: linear-gradient(135deg, #58a6ff, #a371f7);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
    }
  }
}

// Main Content
.ide-main {
  flex: 1;
  display: flex;
  overflow: hidden;
}

// Sidebar
.ide-sidebar {
  width: 260px;
  background: #161b22;
  border-right: 1px solid #30363d;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;

  .sidebar-header {
    padding: 12px;
    border-bottom: 1px solid #30363d;
  }

  .sidebar-tabs {
    display: flex;
    gap: 4px;
  }

  .sidebar-tab {
    flex: 1;
    padding: 8px;
    background: transparent;
    border: none;
    border-radius: 6px;
    color: #8b949e;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      color: #e6edf3;
    }

    &.active {
      background: #21262d;
      color: #e6edf3;
    }
  }

  .sidebar-content {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
  }
}

// Explorer Tree
.explorer-tree {
  .tree-node {
    display: flex;
    align-items: center;
    padding: 6px 8px;
    border-radius: 6px;
    cursor: pointer;
    gap: 6px;
    font-size: 13px;
    transition: background 0.15s;

    &:hover {
      background: #21262d;
    }

    &.selected {
      background: rgba(88, 166, 255, 0.15);
      border: 1px solid rgba(88, 166, 255, 0.3);
    }

    .tree-icon {
      width: 16px;
      font-size: 12px;
      flex-shrink: 0;
    }

    .tree-label {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .tree-badge {
      font-size: 11px;
      color: #6e7681;
      background: #21262d;
      padding: 2px 6px;
      border-radius: 10px;
    }

    .type-icon {
      color: #58a6ff;
    }
  }

  .tree-children {
    padding-left: 20px;
  }
}

// Schema Tree
.schema-tree {
  .search-box {
    margin-bottom: 12px;
  }

  .class-list {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .class-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 10px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.15s;
    border: 1px solid transparent;

    &:hover {
      background: #21262d;
    }

    &.selected {
      background: rgba(88, 166, 255, 0.15);
      border-color: #58a6ff;
    }

    .class-icon {
      width: 28px;
      height: 28px;
      border-radius: 6px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 700;
      color: white;
      flex-shrink: 0;
    }

    .class-info {
      flex: 1;
      overflow: hidden;
    }

    .class-name {
      font-size: 13px;
      font-weight: 500;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .class-count {
      font-size: 11px;
      color: #6e7681;
    }
  }

  .add-class-btn {
    width: 100%;
    margin-top: 12px;
    color: #58a6ff;
  }
}

// Canvas
.ide-canvas {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .canvas-toolbar {
    height: 48px;
    background: #161b22;
    border-bottom: 1px solid #30363d;
    display: flex;
    align-items: center;
    padding: 0 12px;
    gap: 8px;
    flex-shrink: 0;
  }

  .toolbar-group {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .toolbar-separator {
    width: 1px;
    height: 24px;
    background: #30363d;
    margin: 0 8px;
  }

  .toolbar-spacer {
    flex: 1;
  }

  .layout-selector {
    display: flex;
    gap: 4px;

    .ant-btn {
      background: transparent;
      border: 1px solid #30363d;
      color: #8b949e;
      font-size: 12px;
      padding: 4px 12px;
      height: 28px;

      &:hover {
        border-color: #58a6ff;
        color: #58a6ff;
      }

      &.active {
        background: rgba(88, 166, 255, 0.15);
        border-color: #58a6ff;
        color: #58a6ff;
      }
    }
  }

  .canvas-wrapper {
    flex: 1;
    overflow: hidden;
    position: relative;
  }
}

// Right Panel
.ide-panel {
  width: 320px;
  background: #161b22;
  border-left: 1px solid #30363d;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition: width 0.2s;

  &.collapsed {
    width: 0;
    overflow: hidden;
  }

  .panel-header {
    padding: 12px 16px;
    border-bottom: 1px solid #30363d;
    display: flex;
    align-items: center;
    justify-content: space-between;

    .panel-title {
      font-size: 14px;
      font-weight: 600;
    }
  }

  .panel-tabs {
    display: flex;
    border-bottom: 1px solid #30363d;
  }

  .panel-tab {
    flex: 1;
    padding: 10px;
    font-size: 13px;
    color: #8b949e;
    cursor: pointer;
    text-align: center;
    border-bottom: 2px solid transparent;
    transition: all 0.15s;

    &:hover {
      color: #e6edf3;
    }

    &.active {
      color: #58a6ff;
      border-bottom-color: #58a6ff;
    }
  }

  .panel-content {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
  }

  .panel-footer {
    padding: 12px;
    border-top: 1px solid #30363d;
    display: flex;
    gap: 8px;
  }
}

.detail-section {
  .section-title {
    font-size: 12px;
    font-weight: 600;
    color: #6e7681;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 12px;
  }
}

.info-list {
  .info-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    padding: 8px 0;
    border-bottom: 1px solid #21262d;
    gap: 12px;

    &:last-child {
      border-bottom: none;
    }

    .info-label {
      font-size: 13px;
      color: #8b949e;
      flex-shrink: 0;
    }

    .info-value {
      font-size: 13px;
      color: #e6edf3;
      text-align: right;
      word-break: break-all;

      &.uri {
        font-size: 11px;
        color: #6e7681;
      }
    }
  }
}

.property-list {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .property-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    background: #21262d;
    border-radius: 6px;

    .property-key {
      font-size: 12px;
      color: #58a6ff;
      min-width: 60px;
    }

    .property-value {
      flex: 1;
      font-size: 12px;
      color: #e6edf3;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.relation-list {
  display: flex;
  flex-direction: column;
  gap: 6px;

  .relation-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
    background: #21262d;
    border-radius: 6px;
    cursor: pointer;
    transition: background 0.15s;

    &:hover {
      background: #30363d;
    }

    .relation-name {
      flex: 1;
      font-size: 12px;
      color: #e6edf3;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .relation-arrow {
      color: #6e7681;
      font-size: 10px;
    }
  }
}

.instance-toolbar {
  margin-bottom: 12px;
}

.instance-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 400px;
  overflow-y: auto;

  .instance-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 8px 10px;
    background: #161b22;
    border: 1px solid #30363d;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
      background: #1c2128;
      border-color: #58a6ff;
    }

    &.selected {
      background: #1c3a5f;
      border-color: #58a6ff;
    }

    &.in-canvas {
      .instance-name::before {
        content: '';
        display: inline-block;
        width: 6px;
        height: 6px;
        background: #3fb950;
        border-radius: 50%;
        margin-right: 6px;
      }
    }

    .instance-info {
      flex: 1;
      min-width: 0;
    }

    .instance-name {
      font-size: 13px;
      color: #e6edf3;
      font-weight: 500;
      margin-bottom: 4px;
    }

    .instance-meta {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .instance-summary {
      font-size: 11px;
      color: #6e7681;
    }

    .instance-actions {
      display: flex;
      gap: 4px;
      opacity: 0;
      transition: opacity 0.2s;

      .ant-btn {
        color: #8b949e;
        
        &:hover {
          color: #58a6ff;
        }
      }
    }

    &:hover .instance-actions {
      opacity: 1;
    }
  }

  .instance-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 16px;
    color: #6e7681;
  }
}

.instance-pagination {
  margin-top: 12px;
  display: flex;
  justify-content: center;
}

.empty-tip {
  text-align: center;
  padding: 24px;
  color: #6e7681;
  font-size: 13px;
}

.add-property-btn {
  width: 100%;
  margin-top: 12px;
  color: #58a6ff;
}

.panel-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;

  .empty-icon {
    font-size: 48px;
    color: #30363d;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 16px;
    font-weight: 500;
    color: #e6edf3;
    margin-bottom: 8px;
  }

  .empty-desc {
    font-size: 13px;
    color: #6e7681;
  }
}

// Status Bar
.ide-status {
  height: 28px;
  background: #161b22;
  border-top: 1px solid #30363d;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 20px;
  font-size: 12px;
  color: #8b949e;
  flex-shrink: 0;

  .status-item {
    display: flex;
    align-items: center;
    gap: 6px;

    strong {
      color: #e6edf3;
    }
  }

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;

    &.success {
      background: #3fb950;
    }

    &.warning {
      background: #d29922;
    }

    &.error {
      background: #f85149;
    }
  }

  .status-spacer {
    flex: 1;
  }
}

// Context Menu
.context-menu {
  position: fixed;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 8px;
  padding: 4px;
  min-width: 160px;
  z-index: 1000;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);

  .context-menu-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 4px;
    font-size: 13px;
    color: #e6edf3;
    cursor: pointer;
    transition: background 0.1s;

    &:hover {
      background: #21262d;
    }

    &.danger {
      color: #f85149;
    }
  }

  .context-menu-separator {
    height: 1px;
    background: #30363d;
    margin: 4px 0;
  }
}

// Override Ant Design
:deep(.ant-btn-text) {
  color: #8b949e;

  &:hover {
    background: #21262d;
    color: #e6edf3;
  }

  &.active {
    color: #58a6ff;
    background: rgba(88, 166, 255, 0.15);
  }
}

:deep(.ant-input) {
  background: #21262d;
  border-color: #30363d;
  color: #e6edf3;

  &::placeholder {
    color: #6e7681;
  }

  &:hover, &:focus {
    border-color: #58a6ff;
  }
}

:deep(.ant-input-search-button) {
  background: #21262d !important;
  border-color: #30363d !important;

  .anticon {
    color: #8b949e;
  }
}

:deep(.ant-tag) {
  background: #21262d;
  border-color: #30363d;
  color: #e6edf3;
}

:deep(.ant-popconfirm) {
  .ant-btn {
    background: transparent;
    border: 1px solid #30363d;
    color: #8b949e;
  }

  .ant-btn-dangerous {
    background: transparent;
    border-color: #f85149;
    color: #f85149;
  }
}
</style>
