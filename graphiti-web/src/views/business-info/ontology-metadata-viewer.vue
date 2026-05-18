<template>
  <div class="ontology-metadata-page">
    <!-- 扫描线背景装饰 -->
    <div class="scan-line"></div>

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <a-button @click="goBack" type="text" class="back-btn">
          <LeftOutlined /> {{ $t('common.back') }}
        </a-button>
        <div class="title-area">
          <h1 class="page-title gradient-text">{{ $t('businessInfo.ontologyViewer') }}</h1>
          <p class="page-desc">{{ $t('businessInfo.ontologyViewerDesc') }}</p>
        </div>
      </div>
      <div class="header-right">
        <a-select
          v-model:value="selectedGraphId"
          :placeholder="$t('businessInfo.selectGraph')"
          style="width: 220px"
          @change="loadOntologyGraph"
        >
          <a-select-option v-for="g in graphList" :key="g.graphId" :value="g.graphId">
            {{ g.name }}
          </a-select-option>
        </a-select>
        <a-button @click="loadOntologyGraph">
          <ReloadOutlined :spin="loading" /> {{ $t('common.refresh') }}
        </a-button>
      </div>
    </div>

    <!-- 统计概览 -->
    <div v-if="graphMeta" class="stats-overview glass-card fade-slide-up">
      <div class="stat-item">
        <div class="stat-icon" style="color: #00f0ff">◈</div>
        <div class="stat-content">
          <div class="stat-value glow-text-cyan">{{ graphMeta.nodeCount }}</div>
          <div class="stat-label">{{ $t('businessInfo.nodeCount') }}</div>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon" style="color: #ffe066">◆</div>
        <div class="stat-content">
          <div class="stat-value" style="color: #ffe066; text-shadow: 0 0 10px rgba(255, 224, 102, 0.6)">{{ graphMeta.edgeCount }}</div>
          <div class="stat-label">{{ $t('businessInfo.edgeCount') }}</div>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon" style="color: #bf5fff">◉</div>
        <div class="stat-content">
          <div class="stat-value" style="color: #bf5fff; text-shadow: 0 0 10px rgba(191, 95, 255, 0.6)">{{ graphMeta.entityTypeCount }}</div>
          <div class="stat-label">{{ $t('businessInfo.entityTypeCount') }}</div>
        </div>
      </div>
      <div class="stat-divider"></div>
      <div class="stat-item">
        <div class="stat-icon" style="color: #00ffcc">◈</div>
        <div class="stat-content">
          <div class="stat-value" style="color: #00ffcc; text-shadow: 0 0 10px rgba(0, 255, 204, 0.6)">{{ graphMeta.relationTypeCount }}</div>
          <div class="stat-label">{{ $t('businessInfo.relationTypeCount') }}</div>
        </div>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧：图可视化 -->
      <div class="graph-panel glass-card">
        <div class="panel-header">
          <span class="panel-title">{{ $t('businessInfo.ontologyGraph') }}</span>
        </div>
        <div class="graph-area">
          <a-spin :spinning="loading" tip="加载本体数据...">
            <SciFiGraph
              v-if="nodes.length > 0"
              :nodes="nodes"
              :edges="edges"
              :height="520"
              @node-click="onNodeClick"
            />
            <a-empty v-else-if="!loading" :description="$t('businessInfo.noOntologyData')" />
          </a-spin>
        </div>
      </div>

      <!-- 右侧：统计面板 -->
      <div class="stats-panel">
        <!-- 关系类型分布 -->
        <div class="glass-card stat-card fade-slide-up">
          <div class="card-title">{{ $t('businessInfo.relationTypeDistribution') }}</div>
          <div class="relation-list">
            <div v-for="rel in graphMeta?.relationTypes || []" :key="rel" class="relation-item">
              <span class="rel-name">{{ rel }}</span>
              <div class="rel-bar">
                <div class="rel-bar-fill" :style="{ width: '60%', background: 'linear-gradient(90deg, #00f0ff, #bf5fff)' }"></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 实体类型列表 -->
        <div class="glass-card stat-card fade-slide-up" style="animation-delay: 0.1s">
          <div class="card-title">{{ $t('businessInfo.entityTypeList') }}</div>
          <div class="entity-list">
            <div v-for="(entity, idx) in graphMeta?.entityTypes || []" :key="entity" class="entity-item">
              <span class="entity-icon" :style="{ color: getEntityColor(idx) }">◈</span>
              <span class="entity-name">{{ entity }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部：属性定义表 -->
    <div class="properties-section glass-card fade-slide-up" style="animation-delay: 0.2s">
      <div class="card-title">{{ $t('businessInfo.propertyDefinition') }}</div>
      <a-table
        :columns="propertyColumns"
        :data-source="properties"
        :pagination="{ pageSize: 10 }"
        :loading="loading"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'propertyType'">
            <a-tag :color="getPropertyTypeColor(record.propertyType)">{{ record.propertyType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'rangeDataType'">
            <a-tag color="cyan">{{ record.rangeDataType || '-' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'isRequired'">
            <a-tag :color="record.isRequired ? 'red' : 'default'">
              {{ record.isRequired ? $t('common.yes') : $t('common.no') }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { LeftOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { graphApi } from '@/api/graph'
import { ontologyApi } from '@/api/ontology'
import { businessInfoApi, type GraphNodeVO, type GraphEdgeVO, type GraphMetaVO } from '@/api/business-info'
import SciFiGraph from '@/components/scifi/SciFiGraph.vue'

const { t } = useI18n()
const router = useRouter()

const selectedGraphId = ref('')
const graphList = ref<any[]>([])
const loading = ref(false)
const nodes = ref<GraphNodeVO[]>([])
const edges = ref<GraphEdgeVO[]>([])
const graphMeta = ref<GraphMetaVO | null>(null)
const properties = ref<any[]>([])

const propertyColumns = [
  { title: '属性名', dataIndex: 'localName', key: 'localName' },
  { title: '类型', key: 'propertyType' },
  { title: '数据类型', key: 'rangeDataType' },
  { title: '必填', key: 'isRequired' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
]

async function loadGraphs() {
  try {
    graphList.value = await graphApi.getList() as any[] || []
    if (graphList.value.length > 0 && !selectedGraphId.value) {
      selectedGraphId.value = graphList.value[0].graphId
    }
    if (selectedGraphId.value) {
      await loadOntologyGraph()
    }
  } catch (e) {
    console.error('加载图谱列表失败', e)
  }
}

async function loadOntologyGraph() {
  if (!selectedGraphId.value) return
  loading.value = true
  try {
    const [graphResp, ontologyResp] = await Promise.all([
      businessInfoApi.getOntologyGraph(selectedGraphId.value),
      ontologyApi.getFullOntology(selectedGraphId.value),
    ])
    nodes.value = (graphResp as any).nodes || []
    edges.value = (graphResp as any).edges || []
    graphMeta.value = (graphResp as any).meta || null
    properties.value = (ontologyResp as any).properties || []
  } catch (e: any) {
    if (e?.code !== 1002) {
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
  message.info(`Clicked: ${node.label} (${node.type})`)
}

function goBack() {
  router.push('/ontology')
}

function getEntityColor(idx: number): string {
  const colors = ['#00f0ff', '#bf5fff', '#00ffcc', '#ffe066', '#ff3dcc']
  return colors[idx % colors.length]
}

function getPropertyTypeColor(type: string): string {
  const colors: Record<string, string> = {
    DATATYPE: 'blue',
    OBJECT: 'green',
    ANNOTATION: 'purple',
  }
  return colors[type] || 'default'
}

onMounted(() => {
  loadGraphs()
})
</script>

<style scoped lang="less">
@import '@/assets/styles/scifi-variables.less';
@import '@/assets/styles/scifi-glass.less';
@import '@/assets/styles/scifi-animation.less';

.ontology-metadata-page {
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
  background: linear-gradient(90deg, transparent, rgba(0, 240, 255, 0.15), transparent);
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

.back-btn {
  color: @text-secondary;
  font-size: 14px;
}

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

  .page-desc {
    font-size: 13px;
    color: @text-secondary;
    margin: 0;
  }
}

.header-right {
  display: flex;
  gap: 12px;
  align-items: center;
}

.stats-overview {
  display: flex;
  align-items: center;
  padding: 20px 32px;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.stat-icon {
  font-size: 28px;
  filter: drop-shadow(0 0 6px currentColor);
}

.stat-content {
  .stat-value {
    font-size: 28px;
    font-weight: 700;
  }

  .stat-label {
    font-size: 12px;
    color: @text-dim;
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-top: 2px;
  }
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: linear-gradient(to bottom, transparent, @glass-border, transparent);
  margin: 0 24px;
}

.main-content {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  position: relative;
  z-index: 1;
}

.graph-panel {
  flex: 2;
  min-height: 560px;

  .panel-header {
    padding: 16px 20px;
    border-bottom: 1px solid rgba(0, 240, 255, 0.1);
  }

  .panel-title {
    font-size: 14px;
    font-weight: 600;
    color: @neon-cyan;
    text-transform: uppercase;
    letter-spacing: 1px;
  }

  .graph-area {
    padding: 8px;
    min-height: 520px;
  }
}

.stats-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-card {
  padding: 20px;

  .card-title {
    font-size: 13px;
    font-weight: 600;
    color: @neon-cyan;
    text-transform: uppercase;
    letter-spacing: 1px;
    margin-bottom: 16px;
    padding-bottom: 8px;
    border-bottom: 1px solid rgba(0, 240, 255, 0.1);
  }
}

.relation-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.relation-item {
  display: flex;
  align-items: center;
  gap: 10px;

  .rel-name {
    width: 80px;
    font-size: 11px;
    color: @text-secondary;
    text-transform: uppercase;
  }

  .rel-bar {
    flex: 1;
    height: 4px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 2px;
    overflow: hidden;

    .rel-bar-fill {
      height: 100%;
      border-radius: 2px;
      transition: width 0.3s ease;
    }
  }
}

.entity-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.entity-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.2);

  .entity-icon {
    font-size: 14px;
    filter: drop-shadow(0 0 4px currentColor);
  }

  .entity-name {
    font-size: 13px;
    color: @text-primary;
  }
}

.properties-section {
  padding: 20px;
  position: relative;
  z-index: 1;
}
</style>
