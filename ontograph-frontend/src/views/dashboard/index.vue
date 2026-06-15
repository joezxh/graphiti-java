<template>
  <div class="dashboard-page">
    <!-- 页面标题区 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <ThunderboltOutlined class="title-icon" />
          {{ $t('dashboard.title') }}
        </h2>
        <span class="page-subtitle">{{ $t('dashboard.subtitle') }}</span>
      </div>
      <div class="header-actions">
        <a-button @click="refresh" :loading="loading">
          <template #icon><ReloadOutlined /></template>
          {{ $t('common.refresh') }}
        </a-button>
      </div>
    </div>

    <!-- Stats 卡片区 -->
    <div class="stats-grid">
      <StatsCard
        :icon="ShareAltOutlined"
        :label="$t('dashboard.totalGraphs')"
        :value="stats.totalGraphs"
        icon-bg="rgba(94, 106, 210, 0.12)"
        icon-color="#5e6ad2"
        value-color="#5e6ad2"
        :trend="stats.graphTrend"
        :trend-period="$t('dashboard.thisWeek')"
        @click="goToGraphList"
      />
      <StatsCard
        :icon="EnvironmentOutlined"
        :label="$t('dashboard.totalNodes')"
        :value="stats.totalNodes"
        icon-bg="rgba(0, 212, 255, 0.12)"
        icon-color="#00d4ff"
        value-color="#00d4ff"
        :trend="stats.nodeTrend"
        :trend-period="$t('dashboard.thisWeek')"
        @click="goToGraphDetail"
      />
      <StatsCard
        :icon="ApiOutlined"
        :label="$t('dashboard.totalEdges')"
        :value="stats.totalEdges"
        icon-bg="rgba(39, 166, 68, 0.12)"
        icon-color="#27a644"
        value-color="#27a644"
        :trend="stats.edgeTrend"
        :trend-period="$t('dashboard.thisWeek')"
        @click="goToGraphDetail"
      />
      <StatsCard
        :icon="FileTextOutlined"
        :label="$t('dashboard.totalEpisodes')"
        :value="stats.totalEpisodes"
        icon-bg="rgba(255, 140, 0, 0.12)"
        icon-color="#ff8c00"
        value-color="#ff8c00"
        :trend="stats.episodeTrend"
        :trend-period="$t('dashboard.thisWeek')"
        @click="goToInferenceLog"
      />
    </div>

    <!-- 快捷操作区 -->
    <div class="quick-actions-section">
      <h3 class="section-title">
        <BulbOutlined class="section-icon" />
        {{ $t('dashboard.quickActions') }}
      </h3>
      <div class="action-grid">
        <div class="action-card" @click="handleCreateGraph">
          <div class="action-icon" style="background: rgba(94, 106, 210, 0.12); color: #5e6ad2;">
            <PlusOutlined />
          </div>
          <div class="action-text">
            <span class="action-title">{{ $t('dashboard.createGraph') }}</span>
            <span class="action-desc">{{ $t('dashboard.createGraphDesc') }}</span>
          </div>
          <RightOutlined class="action-arrow" />
        </div>

        <div class="action-card" @click="handleImportData">
          <div class="action-icon" style="background: rgba(0, 212, 255, 0.12); color: #00d4ff;">
            <UploadOutlined />
          </div>
          <div class="action-text">
            <span class="action-title">{{ $t('dashboard.importData') }}</span>
            <span class="action-desc">{{ $t('dashboard.importDataDesc') }}</span>
          </div>
          <RightOutlined class="action-arrow" />
        </div>

        <div class="action-card" @click="handleViewInferenceLog">
          <div class="action-icon" style="background: rgba(255, 140, 0, 0.12); color: #ff8c00;">
            <UnorderedListOutlined />
          </div>
          <div class="action-text">
            <span class="action-title">{{ $t('dashboard.viewInferenceLog') }}</span>
            <span class="action-desc">{{ $t('dashboard.viewInferenceLogDesc') }}</span>
          </div>
          <RightOutlined class="action-arrow" />
        </div>

        <div class="action-card" @click="handleOntologyManage">
          <div class="action-icon" style="background: rgba(39, 166, 68, 0.12); color: #27a644;">
            <SettingOutlined />
          </div>
          <div class="action-text">
            <span class="action-title">{{ $t('dashboard.ontologyManagement') }}</span>
            <span class="action-desc">{{ $t('dashboard.ontologyManagementDesc') }}</span>
          </div>
          <RightOutlined class="action-arrow" />
        </div>
      </div>
    </div>

    <!-- 最近图谱列表 -->
    <div class="recent-graphs-section">
      <div class="section-header">
        <h3 class="section-title">
          <ClockCircleOutlined class="section-icon" />
          {{ $t('dashboard.recentGraphs') }}
        </h3>
        <a-button type="link" @click="goToGraphList">
          {{ $t('common.viewAll') }} <RightOutlined />
        </a-button>
      </div>

      <div v-if="recentGraphs.length > 0" class="graph-list">
        <div
          v-for="graph in recentGraphs"
          :key="graph.graphId"
          class="graph-item"
          @click="goToDetail(graph.graphId)"
        >
          <div class="graph-icon-wrap">
            <ShareAltOutlined class="graph-icon" />
          </div>
          <div class="graph-info">
            <span class="graph-name">{{ graph.name }}</span>
            <span class="graph-desc">{{ graph.description || $t('common.noDescription') }}</span>
          </div>
          <div class="graph-stats">
            <div class="graph-stat">
              <EnvironmentOutlined />
              <span>{{ graph.nodeCount ?? 0 }}</span>
            </div>
            <div class="graph-stat">
              <ApiOutlined />
              <span>{{ graph.edgeCount ?? 0 }}</span>
            </div>
          </div>
          <div class="graph-time">
            {{ formatTime(graph.createdAt) }}
          </div>
        </div>
      </div>

      <a-empty v-else :description="$t('dashboard.noGraphs')">
        <template #image>
          <div style="font-size: 48px; color: #5e6ad2;">
            <InboxOutlined />
          </div>
        </template>
      </a-empty>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ThunderboltOutlined,
  ReloadOutlined,
  ShareAltOutlined,
  EnvironmentOutlined,
  ApiOutlined,
  FileTextOutlined,
  BulbOutlined,
  PlusOutlined,
  UploadOutlined,
  UnorderedListOutlined,
  SettingOutlined,
  RightOutlined,
  ClockCircleOutlined,
  InboxOutlined
} from '@ant-design/icons-vue'
import StatsCard from '@/components/StatsCard/index.vue'
import { getGraphStats, listGraphs } from '@/api/graph'
import type { GraphStats } from '@/api/graph'

const router = useRouter()

const loading = ref(false)
const recentGraphs = ref<any[]>([])

const stats = reactive<GraphStats & { graphTrend?: number }>({
  totalGraphs: 0,
  totalNodes: 0,
  totalEdges: 0,
  totalEpisodes: 0,
  nodeTrend: 12,
  edgeTrend: 8,
  episodeTrend: 15,
  graphTrend: 5
})

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return ''
  try {
    return new Date(time).toLocaleDateString('zh-CN')
  } catch {
    return time
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 并行请求统计数据和图谱列表
    const [statsRes, listRes] = await Promise.allSettled([
      getGraphStats(),
      listGraphs()
    ])

    // 处理统计数据
    if (statsRes.status === 'fulfilled') {
      Object.assign(stats, statsRes.value)
    }

    // 处理图谱列表
    if (listRes.status === 'fulfilled') {
      recentGraphs.value = (listRes.value as any).graphs || (listRes.value as any) || []
    }
  } catch (err) {
    console.warn('Dashboard data load failed', err)
    message.warning('dashboard.partialLoadFailed')
  } finally {
    loading.value = false
  }
}

const refresh = () => loadData()

// 导航方法
const goToGraphList = () => {
  router.push('/graph/list')
}

const goToGraphDetail = (graphId?: string) => {
  const id = graphId || recentGraphs.value[0]?.graphId
  if (id) {
    router.push(`/graph/ide/${id}`)
  } else {
    message.info('dashboard.pleaseCreateFirst')
  }
}

const goToDetail = (graphId: string) => {
  router.push(`/graph/ide/${graphId}`)
}

const goToInferenceLog = () => {
  router.push('/inference')
}

const handleCreateGraph = () => {
  router.push('/graph/list')
}

const handleImportData = () => {
  router.push('/data/import')
}

const handleViewInferenceLog = () => {
  router.push('/inference')
}

const handleOntologyManage = () => {
  router.push('/ontology')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="less">
.dashboard-page {
  padding: 24px;
  min-height: 100%;
  background: #010102;
}

/* 页面标题区 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  color: #f7f8f8;
}

.title-icon {
  font-size: 26px;
  color: #5e6ad2;
}

.page-subtitle {
  font-size: 14px;
  color: #8a8f98;
}

/* Stats 卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

/* 快捷操作区 */
.quick-actions-section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #f7f8f8;
}

.section-icon {
  font-size: 18px;
  color: #5e6ad2;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.action-card {
  background: #0f1011;
  border: 1px solid #23252a;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.2, 0.65, 0.2, 1);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(120deg, rgba(94, 106, 210, 0.06) 0%, transparent 50%);
    opacity: 0;
    transition: opacity 0.25s ease;
  }
  
  &:hover {
    border-color: #34343a;
    transform: translateY(-2px);
    
    &::before {
      opacity: 1;
    }
    
    .action-arrow {
      transform: translateX(2px);
      color: #5e6ad2;
    }
  }
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.action-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.action-title {
  font-size: 15px;
  font-weight: 600;
  color: #f7f8f8;
}

.action-desc {
  font-size: 12px;
  color: #8a8f98;
  margin-top: 2px;
}

.action-arrow {
  color: #8a8f98;
  font-size: 14px;
  transition: all 0.25s ease;
}

/* 最近图谱列表 */
.recent-graphs-section {
  background: #0f1011;
  border: 1px solid #23252a;
  border-radius: 12px;
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  .section-title {
    margin: 0;
  }
}

.graph-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.graph-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s ease;
  
  &:hover {
    background: #141516;
  }
}

.graph-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #5e6ad2, #828fff);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.graph-icon {
  font-size: 18px;
  color: #fff;
}

.graph-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.graph-name {
  font-size: 14px;
  font-weight: 600;
  color: #f7f8f8;
}

.graph-desc {
  font-size: 12px;
  color: #8a8f98;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.graph-stats {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.graph-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #8a8f98;
  
  .anticon {
    font-size: 14px;
  }
}

.graph-time {
  font-size: 12px;
  color: #8a8f98;
  flex-shrink: 0;
}

// 修复空状态文字颜色
:deep(.ant-empty) {
  .ant-empty-description {
    color: #f7f8f8 !important;
  }
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .action-grid {
    grid-template-columns: 1fr;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
