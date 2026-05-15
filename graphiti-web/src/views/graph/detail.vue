<template>
  <div class="graph-detail-container">
    <!-- 左侧：图谱视图 -->
    <div class="graph-view-section">
      <div class="section-header">
        <div class="header-left">
          <h3 class="section-title">{{ $t("graphDetail.visualization") || "Graph Visualization" }}</h3>
          <div class="filter-tags">
            <a-tag
              v-for="type in nodeTypes"
              :key="type"
              :color="activeFilterType === type ? 'blue' : 'default'"
              class="filter-tag"
              @click="toggleTypeFilter(type)"
            >
              {{ type }}
            </a-tag>
          </div>
        </div>
        <GraphToolbar
          :show-labels="showLabels"
          @update:showLabels="showLabels = $event"
          @refresh="loadGraphData"
          @zoom-to-fit="handleZoomToFit"
          @layout-change="handleLayoutChange"
        />
      </div>
      
      <div class="graph-canvas">
        <ForceGraph
          v-if="nodes.length > 0"
          :graph-id="graphId"
          :nodes="nodes"
          :edges="edges"
          :categories="categories"
          :show-labels="showLabels"
          :layout="currentLayout"
          @node-click="handleNodeClick"
        />
        <div v-else class="empty-graph">
          <a-empty :description="$t('common.noData')" />
        </div>
      </div>
    </div>
    
    <!-- 右侧：详情面板 -->
    <div class="detail-panel">
      <!-- 图谱信息 -->
      <div class="panel-section">
        <div class="section-title">{{ $t("graphDetail.info") || "Graph Info" }}</div>
        <div class="info-list">
          <div class="info-item">
            <span class="info-label">{{ $t("common.name") }}</span>
            <span class="info-value">{{ graphData?.name || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ $t("common.description") }}</span>
            <span class="info-value">{{ graphData?.description || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">{{ $t("data.nodes") || "Nodes" }}</span>
            <a-tag color="blue">{{ nodes.length }}</a-tag>
          </div>
          <div class="info-item">
            <span class="info-label">{{ $t("data.edges") || "Edges" }}</span>
            <a-tag color="green">{{ edges.length }}</a-tag>
          </div>
        </div>
      </div>
      
      <!-- 快捷操作 -->
      <div class="panel-section">
        <div class="section-title">{{ $t("graphDetail.quickActions") || "Quick Actions" }}</div>
        <div class="action-buttons">
          <a-button size="small" @click="showImportModal">
            <template #icon><ImportOutlined /></template>
            {{ $t("data.import") }}
          </a-button>
          <a-button size="small" @click="handleExport">
            <template #icon><ExportOutlined /></template>
            {{ $t("data.export") }}
          </a-button>
          <a-button size="small" @click="handleBuildCommunity">
            <template #icon><ClusterOutlined /></template>
            {{ $t("community.build") || "Build Community" }}
          </a-button>
        </div>
      </div>
      
      <!-- 节点详情 -->
      <NodeDetail
        :visible="nodeDetailVisible"
        :node-data="selectedNode"
        @close="nodeDetailVisible = false"
        @view-edges="handleViewNodeEdges"
        @view-episodes="handleViewNodeEpisodes"
      />
    </div>
    
    <!-- 导入数据模态框 -->
    <a-modal
      v-model:open="importModalVisible"
      :title="$t('data.import')"
      @ok="handleImport"
    >
      <a-form layout="vertical">
        <a-form-item :label="$t('data.dataFormat')">
          <a-radio-group v-model:value="importFormat">
            <a-radio value="json">JSON</a-radio>
            <a-radio value="csv">CSV</a-radio>
            <a-radio value="triple">Triple</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item :label="$t('data.dataContent')">
          <a-textarea
            v-model:value="importContent"
            :placeholder="$t('form.enterValue')"
            :rows="10"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { ImportOutlined, ExportOutlined, ClusterOutlined } from '@ant-design/icons-vue'
import ForceGraph from '@/components/Graph/ForceGraph.vue'
import GraphToolbar from '@/components/Graph/GraphToolbar.vue'
import NodeDetail from '@/components/Graph/NodeDetail.vue'
import { graphApi, type Graph } from '@/api/graph'
import { nodeApi } from '@/api/node'
import type { EChartsNode, EChartsEdge } from '@/utils/graph'
import { transformGraphData } from '@/utils/graph'

const route = useRoute()
const { t } = useI18n()

// 状态
const graphId = ref<string>(route.params.id as string)
const graphData = ref<Graph | null>(null)
const nodes = ref<EChartsNode[]>([])
const edges = ref<EChartsEdge[]>([])
const categories = ref<Array<{ name: string; itemStyle: { color: string } }>>([])
const showLabels = ref(true)
const currentLayout = ref<'force' | 'circular' | 'tree'>('force')

// 节点详情
const selectedNode = ref<any>(null)
const nodeDetailVisible = ref(false)

// 节点类型过滤
const nodeTypes = ref<string[]>([])
const activeFilterType = ref<string | null>(null)
const allNodes = ref<EChartsNode[]>([])
const allEdges = ref<EChartsEdge[]>([])

// 导入数据
const importModalVisible = ref(false)
const importFormat = ref<'json' | 'csv' | 'triple'>('json')
const importContent = ref('')

// 加载图谱数据
const loadGraphData = async () => {
  try {
    // 获取图谱信息
    const graphRes = await graphApi.getDetail(graphId.value)
    graphData.value = graphRes
    
    // 获取节点和边
    const [nodesRes, edgesRes] = await Promise.all([
      graphApi.getNodes(graphId.value),
      graphApi.getEdges(graphId.value)
    ])
    
    // 转换数据格式
    const transformed = transformGraphData(nodesRes || [], edgesRes || [])
    allNodes.value = transformed.nodes
    allEdges.value = transformed.edges
    nodes.value = transformed.nodes
    edges.value = transformed.edges
    categories.value = transformed.categories
    // 提取节点类型
    const types = new Set<string>()
    for (const n of (nodesRes || [])) {
      if ((n as any).type) types.add((n as any).type)
      else if ((n as any).label) types.add((n as any).label)
    }
    nodeTypes.value = Array.from(types)
  } catch (error) {
    message.error(t('graphDetail.loadGraphFailed'))
  }
}

// 处理节点点击
const handleNodeClick = (nodeData: any) => {
  selectedNode.value = nodeData
  nodeDetailVisible.value = true
}

// 处理布局切换
const handleLayoutChange = (layout: string) => {
  currentLayout.value = layout as 'force' | 'circular' | 'tree'
}

// 处理缩放适配
const handleZoomToFit = () => {
  // ForceGraph 组件内部处理
  message.info(t('graphDetail.viewAdapted'))
}

// 显示导入模态框
const showImportModal = () => {
  importModalVisible.value = true
}

// 处理导入
const handleImport = async () => {
  if (!importContent.value) {
    message.warning(t('graphDetail.inputDataContent'))
    return
  }
  
  try {
    let data: any
    if (importFormat.value === 'json') {
      data = JSON.parse(importContent.value)
    } else {
      data = importContent.value
    }

    await graphApi.addData(graphId.value, {
      format: importFormat.value,
      data
    })
    
    message.success(t('graphDetail.importSuccess'))
    importModalVisible.value = false
    importContent.value = ''
    loadGraphData() // 重新加载数据
  } catch (error) {
    message.error(t('graphDetail.importFailed'))
  }
}

// 处理导出
const handleExport = async () => {
  try {
    const blob = await graphApi.exportData(graphId.value)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${graphData.value?.name || 'graph'}.json`
    link.click()
    window.URL.revokeObjectURL(url)
    message.success(t('graphDetail.exportSuccess'))
  } catch (error) {
    message.error(t('graphDetail.exportFailed'))
  }
}

// 构建社区
const handleBuildCommunity = async () => {
  try {
    await graphApi.buildCommunity(graphId.value)
    message.success(t('graphDetail.buildCommunitySuccess'))
  } catch (error) {
    message.error(t('graphDetail.buildCommunityFailed'))
  }
}

// 查看节点关联边
const handleViewNodeEdges = async (nodeData: any) => {
  message.loading(t('graphDetail.loadingEdges'))
  try {
    const edges = await nodeApi.getEdges(graphId.value, nodeData.uuid)
    message.success(t('graphDetail.nodeEdgesLoaded', { name: nodeData.name, count: edges.length }))
  } catch (err: any) {
    message.error(err.message || t('graphDetail.loadEdgesFailed'))
  }
}

// 查看节点事件
const handleViewNodeEpisodes = async (nodeData: any) => {
  message.loading(t('graphDetail.loadingEpisodes'))
  try {
    const episodes = await nodeApi.getEpisodes(graphId.value, nodeData.uuid)
    message.success(t('graphDetail.nodeEpisodesLoaded', { name: nodeData.name, count: episodes.length }))
  } catch (err: any) {
    message.error(err.message || t('graphDetail.loadEpisodesFailed'))
  }
}

const toggleTypeFilter = (type: string) => {
  if (activeFilterType.value === type) {
    activeFilterType.value = null
    nodes.value = allNodes.value
    edges.value = allEdges.value
  } else {
    activeFilterType.value = type
    const filteredNodeIds = new Set(
      allNodes.value.filter(n => ((n as any).type || (n as any).label) === type).map(n => n.id)
    )
    nodes.value = allNodes.value.filter(n => filteredNodeIds.has(n.id))
    edges.value = allEdges.value.filter(e => filteredNodeIds.has(e.source as string) && filteredNodeIds.has(e.target as string))
  }
}

onMounted(() => {
  loadGraphData()
})
</script>

<style scoped lang="less">
.graph-detail-container {
  display: flex;
  height: calc(100vh - 56px);
  background: #010102;
}

.graph-view-section {
  flex: 7;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #23252a;
  
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: rgba(15, 16, 17, 0.8);
    border-bottom: 1px solid #23252a;
    
    .section-title {
      font-size: 14px;
      font-weight: 600;
      color: #f7f8f8;
      margin: 0;
    }
  }
  
  .graph-canvas {
    flex: 1;
    position: relative;
    overflow: hidden;
    
    .empty-graph {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100%;
      
      :deep(.ant-empty-description) {
        color: #8a8f98;
      }
    }
  }
}

.detail-panel {
  flex: 3;
  max-width: 360px;
  overflow-y: auto;
  background: rgba(15, 16, 17, 0.8);
  padding: 16px;
  
  .panel-section {
    margin-bottom: 24px;
    
    .section-title {
      font-size: 12px;
      font-weight: 600;
      color: #8a8f98;
      text-transform: uppercase;
      margin-bottom: 12px;
    }
    
    .info-list {
      .info-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        
        .info-label {
          font-size: 12px;
          color: #8a8f98;
        }
        
        .info-value {
          font-size: 12px;
          color: #f7f8f8;
          max-width: 180px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
      }
    }
    
    .action-buttons {
      display: flex;
      flex-direction: column;
      gap: 8px;
      
      :deep(.ant-btn) {
        background: rgba(94, 106, 210, 0.1);
        border-color: rgba(94, 106, 210, 0.3);
        color: #f7f8f8 !important;  // 浅色文字，提高对比度
        
        &:hover {
          background: rgba(94, 106, 210, 0.2);
          border-color: #5e6ad2;
          color: #f7f8f8 !important;
        }
      }
    }
  }
}
</style>
