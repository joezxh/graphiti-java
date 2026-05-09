<template>
  <div class="force-graph-container" ref="chartRef"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { EChartsNode, EChartsEdge } from '@/utils/graph'
import { generateForceGraphOption, generateTreeGraphOption } from '@/utils/graph'

// 类型别名（符合设计要求）
export type Node = EChartsNode
export type Edge = EChartsEdge

interface Props {
  graphId: string
  nodes: EChartsNode[]
  edges: EChartsEdge[]
  categories: Array<{ name: string; itemStyle: { color: string } }>
  showLabels?: boolean
  highlightNode?: string
  layout?: 'force' | 'circular' | 'tree'
}

const props = withDefaults(defineProps<Props>(), {
  showLabels: true,
  highlightNode: undefined,
  layout: 'force'
})

const emit = defineEmits<{
  (e: 'node-click', nodeData: any): void
  (e: 'edge-click', edgeData: any): void
}>()

const chartRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

// 初始化图表
const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value, null, {
    renderer: 'canvas'
  })
  
  // 点击事件
  chartInstance.on('click', (params: any) => {
    if (params.dataType === 'node') {
      emit('node-click', params.data.data)
    } else if (params.dataType === 'edge') {
      emit('edge-click', params.data.data)
    }
  })
  
  updateChart()
}

// 更新图表
const updateChart = () => {
  if (!chartInstance) return
  
  let option: any
  
  // 根据布局类型生成配置
  if (props.layout === 'force') {
    option = generateForceGraphOption(
      props.nodes,
      props.edges,
      props.categories,
      {
        showLabels: props.showLabels,
        highlightNode: props.highlightNode
      }
    )
  } else if (props.layout === 'tree') {
    option = generateTreeGraphOption(props.nodes, props.edges, props.categories)
  } else {
    // circular 布局使用 force 但设置圆形布局
    option = generateForceGraphOption(
      props.nodes,
      props.edges,
      props.categories,
      {
        showLabels: props.showLabels,
        highlightNode: props.highlightNode
      }
    )
    // 修改布局为环形
    if (option.series && option.series[0]) {
      option.series[0].layout = 'circular'
    }
  }
  
  chartInstance.setOption(option, true)
}

// 窗口 resize 处理
const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})

// 监听 props 变化
watch(
  () => [props.nodes, props.edges, props.showLabels, props.highlightNode],
  () => {
    nextTick(() => {
      updateChart()
    })
  },
  { deep: true }
)
</script>

<style scoped lang="less">
.force-graph-container {
  width: 100%;
  height: 100%;
  min-height: 500px;
  background: #010102;
}
</style>
