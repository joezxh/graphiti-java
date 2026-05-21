/**
 * 本体可视化 — 基于 ECharts Graph
 * 支持继承树模式、关系图模式、完整模式
 */
<template>
  <div class="ontology-visualizer">
    <div class="viz-toolbar">
      <div class="toolbar-left">
        <a-space>
          <a-radio-group v-model:value="vizMode" button-style="solid" size="small">
            <a-radio-button value="inheritance">继承树</a-radio-button>
            <a-radio-button value="relation">关系图</a-radio-button>
            <a-radio-button value="full">完整模式</a-radio-button>
          </a-radio-group>
          <a-divider type="vertical" />
          <a-button size="small" @click="handleZoomIn"><template #icon><ZoomInOutlined /></template></a-button>
          <a-button size="small" @click="handleZoomOut"><template #icon><ZoomOutOutlined /></template></a-button>
          <a-button size="small" @click="handleFitView"><template #icon><AimOutlined /></template></a-button>
        </a-space>
      </div>
      <div class="toolbar-right">
        <a-input-search v-model:value="searchText" placeholder="搜索节点..." style="width: 180px" size="small" @search="handleSearch" />
        <a-button size="small" @click="handleExportImage"><template #icon><DownloadOutlined /></template>导出</a-button>
      </div>
    </div>
    <div ref="chartRef" class="chart-container" />
    <div class="chart-legend">
      <span class="legend-item"><span class="legend-dot" style="background: #58a6ff"></span>类</span>
      <span class="legend-item"><span class="legend-dot" style="background: #a371f7"></span>属性</span>
      <span class="legend-item"><span class="legend-dot" style="background: #3fb950"></span>继承关系</span>
      <span class="legend-item"><span class="legend-dot" style="background: #d29922"></span>属性关系</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { ZoomInOutlined, ZoomOutOutlined, AimOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import * as echarts from 'echarts'
import { useOntologyStore } from '@/store/modules/ontology'

defineProps<{ graphId: string; classId?: number }>()
const emit = defineEmits<{ (e: 'open-class', classId: number): void }>()

const store = useOntologyStore()
const chartRef = ref<HTMLDivElement>()
const vizMode = ref<'inheritance' | 'relation' | 'full'>('inheritance')
const searchText = ref('')
let chart: echarts.ECharts | null = null

function buildSeries(): any {
  const categories = [
    { name: '类', itemStyle: { color: '#58a6ff' } },
    { name: '属性', itemStyle: { color: '#a371f7' } },
    { name: '约束', itemStyle: { color: '#d29922' } }
  ]

  if (vizMode.value === 'inheritance') {
    // 继承树：仅展示类和继承关系，使用力导向布局让层次更清晰
    const classNodes = store.classes.map(cls => ({
      id: `class-${cls.id}`, name: cls.localName, category: 0, symbolSize: 55,
      itemStyle: { color: '#58a6ff', borderColor: '#30363d', borderWidth: 2 },
      label: { show: true, fontSize: 13, fontWeight: 'bold' }
    }))
    const edges = store.classes.filter(cls => cls.parentClassId).map(cls => ({
      source: `class-${cls.parentClassId}`, target: `class-${cls.id}`,
      lineStyle: { color: '#3fb950', width: 2 },
      label: { show: true, formatter: '继承', fontSize: 10, color: '#3fb950' }
    }))
    return {
      type: 'graph', layout: 'force', roam: true,
      data: classNodes, links: edges, categories,
      label: { show: true, position: 'bottom', fontSize: 12, color: '#e6edf3' },
      force: { repulsion: 300, edgeLength: [80, 150], gravity: 0.1 },
      lineStyle: { curveness: 0.2 }
    }
  }

  const allNodes: any[] = []
  const allLinks: any[] = []

  // 类节点
  store.classes.forEach(cls => {
    allNodes.push({
      id: `class-${cls.id}`, name: cls.localName, category: 0, symbolSize: 50,
      itemStyle: { color: '#58a6ff', borderColor: '#30363d', borderWidth: 2 }
    })
    if (cls.parentClassId) {
      allLinks.push({
        source: `class-${cls.parentClassId}`, target: `class-${cls.id}`,
        lineStyle: { color: '#3fb950', width: 2 },
        label: { show: vizMode.value === 'full', formatter: '继承', fontSize: 9, color: '#3fb950' }
      })
    }
  })

  // 属性节点 + 关系
  store.properties.forEach(prop => {
    allNodes.push({
      id: `prop-${prop.id}`, name: prop.localName, category: 1, symbolSize: 32,
      itemStyle: { color: '#a371f7', borderColor: '#30363d', borderWidth: 1 }
    })
    if (prop.domainClassId) {
      allLinks.push({
        source: `class-${prop.domainClassId}`, target: `prop-${prop.id}`,
        lineStyle: { color: '#d29922', width: 1, type: 'dashed' },
        label: { show: vizMode.value === 'full', formatter: 'domain', fontSize: 9, color: '#d29922' }
      })
    }
    if (prop.rangeClassId) {
      allLinks.push({
        source: `prop-${prop.id}`, target: `class-${prop.rangeClassId}`,
        lineStyle: { color: '#58a6ff', width: 1, type: 'dashed' },
        label: { show: vizMode.value === 'full', formatter: 'range', fontSize: 9, color: '#58a6ff' }
      })
    }
  })

  // 约束节点（仅完整模式）
  if (vizMode.value === 'full') {
    store.constraints.forEach(c => {
      allNodes.push({
        id: `constraint-${c.id}`, name: c.constraintType, category: 2, symbolSize: 24,
        itemStyle: { color: '#d29922', borderColor: '#30363d', borderWidth: 1 }
      })
      if (c.classId) {
        allLinks.push({
          source: `class-${c.classId}`, target: `constraint-${c.id}`,
          lineStyle: { color: '#f85149', width: 1, type: 'dotted' }
        })
      }
      if (c.propertyId) {
        allLinks.push({
          source: `prop-${c.propertyId}`, target: `constraint-${c.id}`,
          lineStyle: { color: '#f85149', width: 1, type: 'dotted' }
        })
      }
    })
  }

  return {
    type: 'graph', layout: vizMode.value === 'full' ? 'force' : 'circular',
    roam: true, nodeScaleRatio: 1.5,
    data: allNodes, links: allLinks, categories,
    label: { show: true, position: 'right', fontSize: 11, color: '#e6edf3' },
    force: { repulsion: 250, edgeLength: [60, 140], gravity: 0.08 },
    lineStyle: { curveness: 0.3, opacity: 0.7 }
  }
}

function initChart() {
  if (!chartRef.value) return
  if (chart) { chart.dispose(); chart = null }
  chart = echarts.init(chartRef.value)
  chart.setOption({ backgroundColor: '#0d1117', animation: true, animationDuration: 800, series: buildSeries() })
  chart.on('click', (params: any) => {
    if (!params.data?.id) return
    const id = params.data.id as string
    if (id.startsWith('class-')) emit('open-class', parseInt(id.replace('class-', '')))
  })
}

function updateChart() { chart?.setOption({ series: buildSeries() }, true) }
function handleZoomIn() { chart?.dispatchAction({ type: 'zoom', scaleX: 1.2, scaleY: 1.2 }) }
function handleZoomOut() { chart?.dispatchAction({ type: 'zoom', scaleX: 0.8, scaleY: 0.8 }) }
function handleFitView() { chart?.dispatchAction({ type: 'fitView' }) }
function handleSearch() {
  if (!chart || !searchText.value.trim()) return
  chart.dispatchAction({ type: 'highlight', query: { name: searchText.value.toLowerCase() } })
}
function handleExportImage() {
  if (!chart) return
  const url = chart.getDataURL({ type: 'png', pixelRatio: 2 })
  const a = document.createElement('a'); a.href = url
  a.download = `ontology-graph-${Date.now()}.png`; a.click()
  message.success('图片已导出')
}
function handleResize() { chart?.resize() }

watch(vizMode, () => updateChart())
onMounted(async () => { await nextTick(); initChart(); window.addEventListener('resize', handleResize) })
onBeforeUnmount(() => { window.removeEventListener('resize', handleResize); chart?.dispose() })
</script>

<style scoped lang="less">
.ontology-visualizer {
  display: flex; flex-direction: column; height: 100%; overflow: hidden;
  .viz-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; background: #161b22; border-bottom: 1px solid #30363d; flex-shrink: 0;
    .toolbar-right { display: flex; gap: 8px; align-items: center; }
  }
  .chart-container { flex: 1; min-height: 400px; background: #0d1117; }
  .chart-legend { display: flex; gap: 24px; align-items: center; padding: 8px 16px; background: #161b22; border-top: 1px solid #21262d; flex-shrink: 0;
    .legend-item { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #8b949e;
      .legend-dot { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }
    }
  }
}
</style>
