<template>
  <div class="mock-data-graph-page">
    <div class="scan-line"></div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <a-button @click="goBack" type="text" class="back-btn">
          <LeftOutlined /> {{ $t('common.back') }}
        </a-button>
        <div class="title-area">
          <h1 class="page-title gradient-text">{{ $t('businessInfo.mockDataViewer') }}</h1>
          <p class="page-desc">{{ $t('businessInfo.mockDataViewerDesc') }}</p>
        </div>
      </div>
      <div class="header-right">
        <a-select
          v-model:value="selectedGraphId"
          :placeholder="$t('businessInfo.selectGraph')"
          style="width: 180px"
          @change="loadDrafts"
        >
          <a-select-option v-for="g in graphList" :key="g.graphId" :value="g.graphId">
            {{ g.name }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="selectedDraftId"
          :placeholder="$t('businessInfo.selectDraft')"
          style="width: 180px"
          @change="loadMockGraph"
        >
          <a-select-option v-for="d in draftList" :key="d.id" :value="d.id">
            {{ d.draftName }} ({{ d.mockEntityCount || 0 }})
          </a-select-option>
        </a-select>
        <a-button type="primary" @click="showExportModal = true">
          <DownloadOutlined /> {{ $t('common.export') }}
        </a-button>
      </div>
    </div>

    <!-- 统计概览 -->
    <div v-if="graphMeta" class="stats-overview glass-card fade-slide-up">
      <div class="stat-item">
        <div class="stat-icon" style="color: #00ffcc">◉</div>
        <div class="stat-content">
          <div class="stat-value" style="color: #00ffcc; text-shadow: 0 0 10px rgba(0, 255, 204, 0.6)">{{ graphMeta.nodeCount }}</div>
          <div class="stat-label">{{ $t('businessInfo.entityCount') }}</div>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon" style="color: #ffe066">◆</div>
        <div class="stat-content">
          <div class="stat-value" style="color: #ffe066; text-shadow: 0 0 10px rgba(255, 224, 102, 0.6)">{{ graphMeta.edgeCount }}</div>
          <div class="stat-label">{{ $t('businessInfo.relationCount') }}</div>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon" style="color: #bf5fff">◈</div>
        <div class="stat-content">
          <div class="stat-value" style="color: #bf5fff; text-shadow: 0 0 10px rgba(191, 95, 255, 0.6)">{{ graphMeta.entityTypeCount }}</div>
          <div class="stat-label">{{ $t('businessInfo.entityTypeCount') }}</div>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon" style="color: #ff3dcc">◇</div>
        <div class="stat-content">
          <div class="stat-value" style="color: #ff3dcc; text-shadow: 0 0 10px rgba(255, 61, 204, 0.6)">{{ graphMeta.relationTypeCount }}</div>
          <div class="stat-label">{{ $t('businessInfo.relationTypeCount') }}</div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧：图可视化 -->
      <div class="graph-panel glass-card">
        <div class="panel-header">
          <span class="panel-title">{{ $t('businessInfo.dataGraph') }}</span>
          <a-radio-group v-model:value="viewMode" size="small">
            <a-radio-button value="graph">{{ $t('businessInfo.graphView') }}</a-radio-button>
            <a-radio-button value="table">{{ $t('businessInfo.tableView') }}</a-radio-button>
          </a-radio-group>
        </div>
        <div class="graph-area">
          <a-spin :spinning="loading" tip="加载模拟数据...">
            <template v-if="viewMode === 'graph'">
              <SciFiGraph
                v-if="nodes.length > 0"
                :nodes="nodes"
                :edges="edges"
                :height="480"
                @node-click="onNodeClick"
              />
              <a-empty v-else-if="!loading" :description="$t('businessInfo.noMockData')" />
            </template>
            <template v-else>
              <a-tabs>
                <a-tab-pane key="entities" :tab="$t('businessInfo.entityList')">
                  <a-table
                    :columns="entityColumns"
                    :data-source="entities"
                    :pagination="{ pageSize: 10 }"
                    row-key="id"
                    size="small"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'type'">
                        <a-tag color="cyan">{{ record.type }}</a-tag>
                      </template>
                    </template>
                  </a-table>
                </a-tab-pane>
                <a-tab-pane key="relations" :tab="$t('businessInfo.relationList')">
                  <a-table
                    :columns="relationColumns"
                    :data-source="relations"
                    :pagination="{ pageSize: 10 }"
                    row-key="id"
                    size="small"
                  >
                    <template #bodyCell="{ column, record }">
                      <template v-if="column.key === 'type'">
                        <a-tag color="orange">{{ record.type }}</a-tag>
                      </template>
                    </template>
                  </a-table>
                </a-tab-pane>
              </a-tabs>
            </template>
          </a-spin>
        </div>
      </div>

      <!-- 右侧：类型统计 -->
      <div class="stats-panel">
        <div class="glass-card stat-card fade-slide-up">
          <div class="card-title">{{ $t('businessInfo.entityTypeDistribution') }}</div>
          <div class="entity-dist-list">
            <div v-for="(type, idx) in graphMeta?.entityTypes || []" :key="type" class="dist-item">
              <span class="dist-icon" :style="{ color: getEntityColor(idx) }">◉</span>
              <span class="dist-name">{{ type }}</span>
            </div>
          </div>
        </div>

        <div class="glass-card stat-card fade-slide-up" style="animation-delay: 0.1s">
          <div class="card-title">{{ $t('businessInfo.relationTypeDistribution') }}</div>
          <div class="relation-dist-list">
            <div v-for="(type, idx) in graphMeta?.relationTypes || []" :key="type" class="dist-item">
              <span class="dist-icon" :style="{ color: getRelationColor(idx) }">◆</span>
              <span class="dist-name">{{ type }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 导出弹窗 -->
    <a-modal
      v-model:open="showExportModal"
      :title="$t('businessInfo.exportData')"
      @ok="handleExport"
    >
      <a-form :model="exportForm" layout="vertical">
        <a-form-item label="Format">
          <a-radio-group v-model:value="exportForm.format">
            <a-radio value="JSON">JSON</a-radio>
            <a-radio value="CSV">CSV</a-radio>
            <a-radio value="N-TRIPLES">N-Triples</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LeftOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { graphApi } from '@/api/graph'
import { businessInfoApi, type GraphNodeVO, type GraphEdgeVO, type GraphMetaVO, type EntityVO, type RelationshipVO } from '@/api/business-info'
import SciFiGraph from '@/components/scifi/SciFiGraph.vue'

const { t } = useI18n()
const router = useRouter()

const selectedGraphId = ref('')
const selectedDraftId = ref<number | undefined>(undefined)
const graphList = ref<any[]>([])
const draftList = ref<any[]>([])
const loading = ref(false)
const viewMode = ref('graph')
const showExportModal = ref(false)
const nodes = ref<GraphNodeVO[]>([])
const edges = ref<GraphEdgeVO[]>([])
const graphMeta = ref<GraphMetaVO | null>(null)
const entities = ref<EntityVO[]>([])
const relations = ref<RelationshipVO[]>([])
const exportForm = ref({ format: 'JSON' })

const entityColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 120 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '类型', key: 'type' },
]
const relationColumns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 120 },
  { title: '源', dataIndex: 'source', key: 'source' },
  { title: '目标', dataIndex: 'target', key: 'target' },
  { title: '类型', key: 'type' },
]

async function loadGraphs() {
  try {
    graphList.value = await graphApi.getList() as any[] || []
    if (graphList.value.length > 0) {
      selectedGraphId.value = graphList.value[0].graphId
      await loadDrafts()
    }
  } catch (e) {
    console.error('加载图谱列表失败', e)
  }
}

async function loadDrafts() {
  if (!selectedGraphId.value) return
  try {
    draftList.value = await businessInfoApi.listDrafts(selectedGraphId.value) as any[] || []
    if (draftList.value.length > 0) {
      selectedDraftId.value = draftList.value[0].id
      await loadMockGraph()
    } else {
      nodes.value = []
      edges.value = []
      graphMeta.value = null
    }
  } catch (e) {
    console.error('加载草稿列表失败', e)
  }
}

async function loadMockGraph() {
  if (!selectedGraphId.value || !selectedDraftId.value) return
  loading.value = true
  try {
    const resp = await businessInfoApi.getMockDataGraph(selectedGraphId.value, selectedDraftId.value) as any
    const respNodes: GraphNodeVO[] = (resp.nodes || []) as GraphNodeVO[]
    const respEdges: GraphEdgeVO[] = (resp.edges || []) as GraphEdgeVO[]
    nodes.value = respNodes
    edges.value = respEdges
    graphMeta.value = resp.meta || null
    // 填充实体和关系列表
    entities.value = respNodes.filter(n => n.type === 'ENTITY').map(n => ({
      id: n.id,
      name: n.label,
      type: n.category || 'Unknown',
    }))
    relations.value = respEdges.map(e => ({
      id: e.id,
      source: e.source,
      target: e.target,
      type: e.type,
    }))
  } catch (e: any) {
    if (e?.code !== 2007) {
      message.error(e?.message || t('common.loadFailed'))
    }
    nodes.value = []
    edges.value = []
    graphMeta.value = null
  } finally {
    loading.value = false
  }
}

function onNodeClick(node: GraphNodeVO) {
  message.info(`Node: ${node.label} (${node.type})`)
}

function goBack() {
  router.push('/business-info')
}

function getEntityColor(idx: number): string {
  const colors = ['#00f0ff', '#bf5fff', '#00ffcc', '#ffe066', '#ff3dcc']
  return colors[idx % colors.length]
}

function getRelationColor(idx: number): string {
  const colors = ['#ffe066', '#00ffcc', '#ff3dcc', '#00f0ff', '#bf5fff']
  return colors[idx % colors.length]
}

function handleExport() {
  const data = JSON.stringify({ nodes: nodes.value, edges: edges.value }, null, 2)
  const blob = new Blob([data], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `mock-data-${selectedDraftId.value}.${exportForm.value.format.toLowerCase()}`
  a.click()
  URL.revokeObjectURL(url)
  showExportModal.value = false
  message.success(t('common.exportSuccess'))
}

onMounted(() => {
  loadGraphs()
})
</script>

<style scoped lang="less">
@import '@/assets/styles/scifi-variables.less';
@import '@/assets/styles/scifi-glass.less';
@import '@/assets/styles/scifi-animation.less';

.mock-data-graph-page {
  padding: 24px;
  min-height: 100vh;
  background: @bg-deepest;
  position: relative;
}

.scan-line {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(0, 255, 204, 0.12), transparent);
  animation: scanLine 8s linear infinite;
  pointer-events: none;
  z-index: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-btn { color: @text-secondary; font-size: 14px; }

.title-area {
  .page-title {
    font-size: 24px;
    font-weight: 700;
    margin: 0 0 4px 0;
    background: linear-gradient(90deg, @neon-cyan, @neon-purple, @neon-cyan);
    background-size: 200% 100%;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    animation: gradientText 3s ease infinite;
  }
  .page-desc { font-size: 13px; color: @text-secondary; margin: 0; }
}

.header-right { display: flex; gap: 12px; align-items: center; }

.stats-overview {
  display: flex;
  align-items: center;
  padding: 20px 32px;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.stat-item { display: flex; align-items: center; gap: 12px; flex: 1; }
.stat-icon { font-size: 28px; filter: drop-shadow(0 0 6px currentColor); }
.stat-content {
  .stat-value { font-size: 28px; font-weight: 700; }
  .stat-label { font-size: 12px; color: @text-dim; text-transform: uppercase; letter-spacing: 1px; margin-top: 2px; }
}
.stat-divider { width: 1px; height: 40px; background: linear-gradient(to bottom, transparent, @glass-border, transparent); margin: 0 24px; }

.main-content { display: flex; gap: 16px; position: relative; z-index: 1; }

.graph-panel {
  flex: 2;
  min-height: 540px;
  .panel-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid rgba(0, 240, 255, 0.1); }
  .panel-title { font-size: 14px; font-weight: 600; color: @neon-cyan; text-transform: uppercase; letter-spacing: 1px; }
  .graph-area { padding: 8px; min-height: 480px; }
}

.stats-panel { flex: 1; display: flex; flex-direction: column; gap: 16px; }

.stat-card {
  padding: 20px;
  .card-title { font-size: 13px; font-weight: 600; color: @neon-cyan; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 16px; padding-bottom: 8px; border-bottom: 1px solid rgba(0, 240, 255, 0.1); }
}

.entity-dist-list, .relation-dist-list { display: flex; flex-direction: column; gap: 8px; }
.dist-item { display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-radius: 6px; background: rgba(0, 0, 0, 0.2); }
.dist-icon { font-size: 14px; filter: drop-shadow(0 0 4px currentColor); }
.dist-name { font-size: 13px; color: @text-primary; }
</style>
