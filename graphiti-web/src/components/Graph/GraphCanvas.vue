<template>
  <div class="graph-canvas-container" ref="containerRef">
    <div ref="chartRef" class="chart-wrapper" />
    
    <!-- Minimap -->
    <div v-if="showMinimap" class="minimap">
      <div class="minimap-header">小地图</div>
      <svg ref="minimapRef" class="minimap-svg" />
    </div>
    
    <!-- Zoom Controls -->
    <div class="zoom-controls">
      <div class="zoom-btn" @click="handleZoomIn">
        <PlusOutlined />
      </div>
      <div class="zoom-level">{{ Math.round(zoom * 100) }}%</div>
      <div class="zoom-btn" @click="handleZoomOut">
        <MinusOutlined />
      </div>
      <div class="zoom-btn" @click="handleZoomFit">
        <AimOutlined />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { PlusOutlined, MinusOutlined, AimOutlined } from '@ant-design/icons-vue'
import type { GraphIDENode, GraphIDEEdge, LayoutType, EditTool } from '@/api/graph'

interface Props {
  graphId: string
  nodes: GraphIDENode[]
  edges: GraphIDEEdge[]
  layout?: LayoutType
  tool?: EditTool
  showMinimap?: boolean
  aggregationMode?: boolean
  selectedNode?: GraphIDENode | null
}

const props = withDefaults(defineProps<Props>(), {
  layout: 'force',
  tool: 'select',
  showMinimap: true,
  aggregationMode: false,
  selectedNode: null
})

const emit = defineEmits<{
  (e: 'node-click', node: GraphIDENode): void
  (e: 'node-dblclick', node: GraphIDENode): void
  (e: 'node-contextmenu', event: MouseEvent, node: GraphIDENode): void
  (e: 'edge-click', edge: GraphIDEEdge): void
}>()

// Refs
const containerRef = ref<HTMLElement>()
const chartRef = ref<HTMLElement>()
const minimapRef = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null
let minimapInstance: echarts.ECharts | null = null
const zoom = ref(100)

// Node colors
const nodeColors: Record<string, string> = {
  Person: '#58a6ff',
  Company: '#3fb950',
  Product: '#d29922',
  Order: '#a371f7',
  Location: '#f85149',
  Event: '#8b5cf6',
  Review: '#06b6d4',
  Category: '#84cc16'
}

// Get node color
const getNodeColor = (type: string): string => {
  return nodeColors[type] || '#6e7681'
}

// Get categories from nodes
const categories = computed(() => {
  const types = [...new Set(props.nodes.map(n => n.type))]
  return types.map(type => ({
    name: type,
    itemStyle: { color: getNodeColor(type) }
  }))
})

// Transform nodes for ECharts
const transformNodes = () => {
  if (props.aggregationMode) {
    // Aggregation mode: group by type
    const typeCount: Record<string, number> = {}
    props.nodes.forEach(n => {
      typeCount[n.type] = (typeCount[n.type] || 0) + 1
    })
    
    const centerX = (containerRef.value?.clientWidth || 800) / 2
    const centerY = (containerRef.value?.clientHeight || 600) / 2
    const radius = Math.min(centerX, centerY) * 0.6
    
    return Object.entries(typeCount).map(([type, count], index) => {
      const angle = (index / Object.keys(typeCount).length) * 2 * Math.PI
      return {
        id: type,
        name: type,
        type: type,
        count: count,
        x: centerX + radius * Math.cos(angle),
        y: centerY + radius * Math.sin(angle),
        symbolSize: 60 + count / 10,
        category: type
      }
    })
  }
  
  return props.nodes.map(node => {
    const isSelected = props.selectedNode?.uuid === node.uuid
    return {
      id: node.uuid,
      name: node.name,
      type: node.type,
      x: node.x,
      y: node.y,
      properties: node.properties,
      summary: node.summary,
      symbolSize: isSelected ? 50 : 40,
      symbol: isSelected ? 'circle' : 'circle',
      category: node.type,
      itemStyle: {
        borderColor: isSelected ? '#58a6ff' : getNodeColor(node.type),
        borderWidth: isSelected ? 4 : 2,
        borderCap: 'round',
        borderJoin: 'round',
        shadowBlur: isSelected ? 25 : 0,
        shadowColor: isSelected ? 'rgba(88, 166, 255, 0.8)' : 'transparent',
        color: isSelected ? 'rgba(88, 166, 255, 0.3)' : getNodeColor(node.type)
      }
    }
  })
}

// Transform edges for ECharts
const transformEdges = () => {
  if (props.aggregationMode) return []
  
  return props.edges.map(edge => ({
    id: edge.uuid,
    source: edge.source,
    target: edge.target,
    name: edge.type,
    type: edge.type,
    lineStyle: {
      color: '#6e7681',
      width: 1.5
    }
  }))
}

// Generate chart options
const generateOptions = () => {
  const nodes = transformNodes()
  const edges = transformEdges()
  
  const baseOptions: echarts.EChartsOption = {
    backgroundColor: '#0d1117',
    animation: true,
    animationDuration: 300,
    
    legend: {
      show: false
    },
    
    tooltip: {
      trigger: 'item',
      backgroundColor: '#161b22',
      borderColor: '#30363d',
      textStyle: {
        color: '#e6edf3'
      },
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          const node = params.data
          return `
            <div style="padding: 4px;">
              <div style="font-weight: 600; margin-bottom: 4px;">${node.name}</div>
              <div style="color: #8b949e; font-size: 12px;">类型: ${node.type}</div>
              ${node.count !== undefined ? `<div style="color: #8b949e; font-size: 12px;">数量: ${node.count}</div>` : ''}
            </div>
          `
        } else if (params.dataType === 'edge') {
          return `<div style="padding: 4px;">${params.data.name || params.data.type}</div>`
        }
        return ''
      }
    },
    
    series: [
      {
        type: 'graph',
        layout: props.layout === 'force' ? 'force' : 'none',
        roam: true,
        draggable: true,
        cursor: 'pointer',
        
        // Force layout specific options
        force: {
          initLayout: 'circular',
          repulsion: 100,
          gravity: 0.1,
          edgeLength: [80, 150],
          layoutAnimation: true
        },
        
        // Circular layout for concentric
        circular: {
          rotateLabel: false
        },
        
        // Node configuration
        nodeScaleRatio: 0.6,
        symbolKeepAspect: true,
        
        // Edge configuration
        edgeSymbol: ['circle', 'arrow'],
        edgeSymbolSize: [4, 8],
        
        // Label
        label: {
          show: true,
          position: 'bottom',
          distance: 6,
          formatter: (params: any) => {
            if (props.aggregationMode && params.data.count !== undefined) {
              return `{count|${params.data.count}}`
            }
            return `{name|${params.data.name}}`
          },
          rich: {
            name: {
              fontSize: 11,
              color: '#e6edf3',
              padding: [2, 0]
            },
            count: {
              fontSize: 12,
              color: '#e6edf3',
              fontWeight: 'bold',
              padding: [2, 4],
              backgroundColor: 'rgba(88, 166, 255, 0.2)',
              borderRadius: 4
            }
          }
        },
        
        // Emphasis
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 3,
            color: '#58a6ff'
          },
          nodeStyle: {
            shadowBlur: 20,
            shadowColor: 'rgba(88, 166, 255, 0.5)'
          }
        },
        
        // Categories
        categories: categories.value.map(cat => ({
          name: cat.name,
          itemStyle: cat.itemStyle
        })),
        
        // Data
        data: nodes,
        links: edges,
        
        // Line style
        lineStyle: {
          curveness: 0.1,
          opacity: 0.6
        }
      }
    ]
  }
  
  // Apply grid layout
  if (props.layout === 'grid') {
    const cols = Math.ceil(Math.sqrt(nodes.length))
    nodes.forEach((node, index) => {
      node.x = (index % cols) * 120 + 100
      node.y = Math.floor(index / cols) * 100 + 80
    })
    baseOptions.series![0].layout = 'none'
  }
  
  // Apply dagre layout (hierarchical)
  if (props.layout === 'dagre') {
    const levels: Record<string, number> = {}
    const children: Record<string, string[]> = {}
    
    // Build hierarchy from edges
    props.edges.forEach(edge => {
      if (!children[edge.source]) children[edge.source] = []
      children[edge.source].push(edge.target)
    })
    
    // Calculate levels
    const assignLevel = (nodeId: string, level: number) => {
      levels[nodeId] = Math.max(levels[nodeId] || 0, level)
      ;(children[nodeId] || []).forEach(child => assignLevel(child, level + 1))
    }
    
    nodes.forEach((node, i) => {
      if (!levels[node.id as string]) {
        assignLevel(node.id as string, 0)
      }
    })
    
    // Group by level
    const levelGroups: Record<number, typeof nodes> = {}
    nodes.forEach(node => {
      const level = levels[node.id as string] || 0
      if (!levelGroups[level]) levelGroups[level] = []
      levelGroups[level].push(node)
    })
    
    // Position nodes
    const maxLevel = Math.max(...Object.keys(levelGroups).map(Number))
    const startY = 80
    const endY = (containerRef.value?.clientHeight || 600) - 80
    const levelHeight = (endY - startY) / (maxLevel + 1)
    
    Object.entries(levelGroups).forEach(([level, levelNodes]) => {
      const startX = 100
      const endX = (containerRef.value?.clientWidth || 800) - 100
      const levelWidth = (endX - startX) / (levelNodes.length + 1)
      
      levelNodes.forEach((node, index) => {
        node.x = startX + levelWidth * (index + 1)
        node.y = startY + Number(level) * levelHeight
      })
    })
    
    baseOptions.series![0].layout = 'none'
  }
  
  return baseOptions
}

// Initialize chart
const initChart = () => {
  if (!chartRef.value) return
  
  chartInstance = echarts.init(chartRef.value, null, {
    renderer: 'canvas'
  })
  
  // Event handlers
  chartInstance.on('click', (params: any) => {
    if (params.dataType === 'node') {
      const node = props.nodes.find(n => n.uuid === params.data.id)
      if (node) emit('node-click', node)
    } else if (params.dataType === 'edge') {
      emit('edge-click', params.data as GraphIDEEdge)
    }
  })

  chartInstance.on('dblclick', (params: any) => {
    if (params.dataType === 'node') {
      const node = props.nodes.find(n => n.uuid === params.data.id)
      if (node) emit('node-dblclick', node)
    }
  })

  chartInstance.getZr().on('contextmenu', (params: any) => {
    if (params.target) {
      const pointInPixel = [params.offsetX, params.offsetY]
      const pointInGrid = chartInstance!.convertFromPixel('grid', pointInPixel)
      
      // Find node at this point
      const data = chartInstance!.getOption() as any
      const nodes = data.series[0].data
      const node = nodes.find((n: any) => {
        if (n.x === undefined || n.y === undefined) return false
        const dx = n.x - pointInGrid[0]
        const dy = n.y - pointInGrid[1]
        return Math.sqrt(dx * dx + dy * dy) < 30
      })
      
      if (node) {
        const fullNode = props.nodes.find(n => n.uuid === node.id)
        if (fullNode) emit('node-contextmenu', params.event as MouseEvent, fullNode)
      }
    }
  })
  
  // Track zoom level
  chartInstance.on('datazoom', () => {
    const option = chartInstance!.getOption()
    if (option.dataZoom && option.dataZoom[0]) {
      zoom.value = (option.dataZoom[0] as any).end || 100
    }
  })
  
  updateChart()
}

// Initialize minimap
const initMinimap = () => {
  if (!minimapRef.value) return
  
  minimapInstance = echarts.init(minimapRef.value)
  
  const minimapOptions: echarts.EChartsOption = {
    backgroundColor: '#0d1117',
    series: [
      {
        type: 'graph',
        layout: 'force',
        animation: false,
        roam: false,
        data: transformNodes().map(n => ({
          ...n,
          symbolSize: 3,
          category: n.type
        })),
        links: transformEdges(),
        lineStyle: {
          width: 0.5,
          opacity: 0.3
        }
      }
    ]
  }
  
  minimapInstance.setOption(minimapOptions)
}

// Update chart
const updateChart = () => {
  if (!chartInstance) return
  
  chartInstance.setOption(generateOptions(), true)
  
  // Update minimap
  if (minimapInstance) {
    minimapInstance.setOption({
      series: [{
        data: transformNodes().map(n => ({
          ...n,
          symbolSize: 3
        })),
        links: transformEdges()
      }]
    })
  }
}

// Handle resize
const handleResize = () => {
  chartInstance?.resize()
  minimapInstance?.resize()
}

// Zoom controls
const handleZoomIn = () => {
  chartInstance?.dispatchAction({
    type: 'dataZoom',
    start: 0,
    end: Math.max(10, zoom.value - 10)
  })
}

const handleZoomOut = () => {
  chartInstance?.dispatchAction({
    type: 'dataZoom',
    start: 0,
    end: Math.min(100, zoom.value + 10)
  })
}

const handleZoomFit = () => {
  chartInstance?.dispatchAction({
    type: 'dataZoom',
    start: 0,
    end: 100
  })
  zoom.value = 100
}

// Watch props changes
watch(
  () => [props.nodes, props.edges, props.layout, props.aggregationMode],
  () => {
    nextTick(() => {
      updateChart()
    })
  },
  { deep: true }
)

// Watch selected node changes to highlight it
watch(
  () => props.selectedNode,
  (newNode, oldNode) => {
    if (!chartInstance) return
    
    // Clear previous selection
    if (oldNode) {
      chartInstance.dispatchAction({
        type: 'downplay',
        dataIndex: props.nodes.findIndex(n => n.uuid === oldNode.uuid)
      })
    }
    
    // Highlight new selection
    if (newNode) {
      const nodeIndex = props.nodes.findIndex(n => n.uuid === newNode.uuid)
      if (nodeIndex >= 0) {
        chartInstance.dispatchAction({
          type: 'highlight',
          dataIndex: nodeIndex
        })
        
        // Also center the view on the selected node
        const option = chartInstance.getOption() as any
        if (option && option.series && option.series[0] && option.series[0].data) {
          const nodeData = option.series[0].data[nodeIndex]
          if (nodeData && nodeData.x !== undefined && nodeData.y !== undefined) {
            chartInstance.dispatchAction({
              type: 'showTip',
              seriesIndex: 0,
              dataIndex: nodeIndex
            })
          }
        }
      }
    }
  }
)

// Lifecycle
onMounted(() => {
  nextTick(() => {
    initChart()
    if (props.showMinimap) {
      initMinimap()
    }
  })
  
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  minimapInstance?.dispose()
})

// Expose methods
defineExpose({
  zoomIn: handleZoomIn,
  zoomOut: handleZoomOut,
  zoomFit: handleZoomFit,
  refresh: updateChart
})
</script>

<style scoped lang="less">
.graph-canvas-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: 
    radial-gradient(circle at 50% 50%, rgba(88, 166, 255, 0.03) 0%, transparent 50%),
    #0d1117;
  
  .chart-wrapper {
    width: 100%;
    height: 100%;
  }
  
  .minimap {
    position: absolute;
    left: 16px;
    bottom: 16px;
    width: 180px;
    background: rgba(22, 27, 34, 0.95);
    border: 1px solid #30363d;
    border-radius: 8px;
    overflow: hidden;
    
    .minimap-header {
      padding: 6px 10px;
      font-size: 11px;
      color: #6e7681;
      border-bottom: 1px solid #30363d;
    }
    
    .minimap-svg {
      width: 100%;
      height: 100px;
    }
  }
  
  .zoom-controls {
    position: absolute;
    right: 16px;
    bottom: 16px;
    display: flex;
    flex-direction: column;
    gap: 4px;
    align-items: center;
    
    .zoom-btn {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(22, 27, 34, 0.95);
      border: 1px solid #30363d;
      border-radius: 6px;
      color: #8b949e;
      cursor: pointer;
      transition: all 0.15s;
      
      &:hover {
        background: #21262d;
        color: #e6edf3;
        border-color: #58a6ff;
      }
    }
    
    .zoom-level {
      font-size: 10px;
      color: #6e7681;
      padding: 4px 0;
    }
  }
}
</style>
