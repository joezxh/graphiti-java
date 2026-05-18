<template>
  <div ref="containerRef" class="scifi-graph-container">
    <!-- 背景网格 -->
    <div class="graph-grid-bg"></div>

    <!-- SVG 图谱区域 -->
    <svg ref="svgRef" class="scifi-graph-svg" :height="height">
      <defs>
        <!-- 节点发光滤镜 -->
        <filter id="scifiNodeGlow" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="4" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
        <!-- 节点发光滤镜（强） -->
        <filter id="scifiNodeGlowStrong" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="8" result="blur" />
          <feMerge>
            <feMergeNode in="blur" />
            <feMergeNode in="blur" />
            <feMergeNode in="blur" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
        <!-- 边流动渐变 -->
        <linearGradient id="scifiEdgeGradient" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#00f0ff" stop-opacity="0.9" />
          <stop offset="50%" stop-color="#bf5fff" stop-opacity="1" />
          <stop offset="100%" stop-color="#00f0ff" stop-opacity="0.9" />
        </linearGradient>
        <!-- 边发光渐变（关系） -->
        <linearGradient id="scifiEdgeGradientRel" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#ffe066" stop-opacity="0.9" />
          <stop offset="50%" stop-color="#00ffcc" stop-opacity="1" />
          <stop offset="100%" stop-color="#ffe066" stop-opacity="0.9" />
        </linearGradient>
        <!-- 节点渐变（类） -->
        <radialGradient id="scifiNodeClass" cx="30%" cy="30%">
          <stop offset="0%" stop-color="#00f0ff" stop-opacity="1" />
          <stop offset="70%" stop-color="#005566" stop-opacity="0.9" />
          <stop offset="100%" stop-color="#003344" stop-opacity="0.8" />
        </radialGradient>
        <!-- 节点渐变（属性） -->
        <radialGradient id="scifiNodeProperty" cx="30%" cy="30%">
          <stop offset="0%" stop-color="#bf5fff" stop-opacity="1" />
          <stop offset="70%" stop-color="#550088" stop-opacity="0.9" />
          <stop offset="100%" stop-color="#330055" stop-opacity="0.8" />
        </radialGradient>
        <!-- 节点渐变（实体） -->
        <radialGradient id="scifiNodeEntity" cx="30%" cy="30%">
          <stop offset="0%" stop-color="#00ffcc" stop-opacity="1" />
          <stop offset="70%" stop-color="#005544" stop-opacity="0.9" />
          <stop offset="100%" stop-color="#003322" stop-opacity="0.8" />
        </radialGradient>
        <!-- 箭头标记 -->
        <marker id="scifiArrowhead" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="#00f0ff" opacity="0.7" />
        </marker>
        <marker id="scifiArrowheadPurple" markerWidth="10" markerHeight="7" refX="10" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="#bf5fff" opacity="0.7" />
        </marker>
      </defs>

      <!-- 边层 -->
      <g class="edges-layer">
        <g v-for="edge in renderedEdges" :key="edge.id" class="edge-group">
          <!-- 边发光轨迹 -->
          <path
            :d="getEdgePath(edge)"
            class="scifi-edge-glow"
            :stroke="getEdgeColor(edge.type)"
            stroke-width="4"
            fill="none"
            :opacity="0.2"
          />
          <!-- 边主体 -->
          <path
            :d="getEdgePath(edge)"
            class="scifi-edge"
            :class="{ 'flowing-edge': isFlowEdge(edge.type) }"
            :stroke="getEdgeColor(edge.type)"
            stroke-width="1.5"
            fill="none"
            :marker-end="getMarker(edge.type)"
          />
          <!-- 边标签 -->
          <text
            v-if="showEdgeLabels && edge.label"
            :x="getEdgeMidPoint(edge).x"
            :y="getEdgeMidPoint(edge).y"
            class="edge-label"
            fill="rgba(0, 240, 255, 0.6)"
            font-size="10"
            text-anchor="middle"
          >
            {{ edge.label }}
          </text>
        </g>
      </g>

      <!-- 节点层 -->
      <g class="nodes-layer">
        <g
          v-for="node in renderedNodes"
          :key="node.id"
          class="node-group"
          :transform="`translate(${node.x || 0}, ${node.y || 0})`"
          @mouseenter="onNodeHover(node, $event)"
          @mouseleave="onNodeLeave"
          @click="onNodeClick(node)"
        >
          <!-- 外圈脉冲环 -->
          <circle
            r="30"
            class="node-pulse-ring"
            :fill="getNodeColor(node.type)"
            :style="{ animationDelay: `${(hashString(node.id) % 20) * 0.1}s` }"
          />
          <!-- 节点主体 -->
          <circle
            r="18"
            :fill="getNodeGradient(node.type)"
            :stroke="getNodeColor(node.type)"
            stroke-width="2"
            filter="url(#scifiNodeGlow)"
            class="node-body"
          />
          <!-- 节点图标 -->
          <text
            y="5"
            text-anchor="middle"
            font-size="14"
            fill="white"
            class="node-icon"
          >
            {{ getNodeIcon(node.type) }}
          </text>
          <!-- 节点标签 -->
          <text
            y="35"
            text-anchor="middle"
            font-size="11"
            fill="#e8f4f8"
            class="node-label"
          >
            {{ node.label }}
          </text>
        </g>
      </g>
    </svg>

    <!-- 节点详情悬浮面板 -->
    <transition name="panel-fade">
      <NodeDetailPanel
        v-if="hoveredNode && showPanel"
        :node="hoveredNode"
        :position="panelPosition"
        :edges="getNodeEdges(hoveredNode.id)"
        class="node-detail-float"
      />
    </transition>

    <!-- 图例 -->
    <div class="graph-legend glass-panel">
      <div class="legend-title">图例</div>
      <div v-for="type in legendTypes" :key="type.value" class="legend-item">
        <span class="legend-dot" :style="{ background: type.color, boxShadow: `0 0 6px ${type.color}` }"></span>
        <span class="legend-text">{{ type.label }}</span>
      </div>
    </div>

    <!-- 控制栏 -->
    <div class="graph-controls glass-panel">
      <a-tooltip title="放大">
        <a-button size="small" @click="zoomIn">
          <PlusOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="缩小">
        <a-button size="small" @click="zoomOut">
          <MinusOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="重置视图">
        <a-button size="small" @click="resetView">
          <AimOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip :title="showEdgeLabels ? '隐藏边标签' : '显示边标签'">
        <a-button size="small" @click="toggleLabels">
          <TagOutlined />
        </a-button>
      </a-tooltip>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { PlusOutlined, MinusOutlined, AimOutlined, TagOutlined } from '@ant-design/icons-vue'
import type { GraphNodeVO, GraphEdgeVO } from '@/api/business-info'
import NodeDetailPanel from './NodeDetailPanel.vue'

const props = withDefaults(defineProps<{
  nodes: GraphNodeVO[]
  edges: GraphEdgeVO[]
  height?: number | string
}>(), {
  height: 600
})

const emit = defineEmits<{
  (e: 'node-click', node: GraphNodeVO): void
  (e: 'node-hover', node: GraphNodeVO, event: MouseEvent): void
  (e: 'node-leave'): void
}>()

// Refs
const svgRef = ref<SVGSVGElement>()
const containerRef = ref<HTMLDivElement>()

// State
const hoveredNode = ref<GraphNodeVO | null>(null)
const panelPosition = ref({ x: 0, y: 0 })
const showPanel = ref(false)
const showEdgeLabels = ref(true)
const transform = ref({ x: 0, y: 0, scale: 1 })

// 计算属性
const renderedNodes = computed(() => props.nodes)
const renderedEdges = computed(() => props.edges)

// 图例
const legendTypes = [
  { value: 'CLASS', label: '实体类型', color: '#00f0ff' },
  { value: 'PROPERTY', label: '属性', color: '#bf5fff' },
  { value: 'ENTITY', label: '数据实体', color: '#00ffcc' },
]

// 节点颜色
function getNodeColor(type: string): string {
  const colors: Record<string, string> = {
    CLASS: '#00f0ff',
    PROPERTY: '#bf5fff',
    ENTITY: '#00ffcc',
  }
  return colors[type] || '#00f0ff'
}

function getNodeGradient(type: string): string {
  const gradients: Record<string, string> = {
    CLASS: 'url(#scifiNodeClass)',
    PROPERTY: 'url(#scifiNodeProperty)',
    ENTITY: 'url(#scifiNodeEntity)',
  }
  return gradients[type] || 'url(#scifiNodeClass)'
}

function getNodeIcon(type: string): string {
  const icons: Record<string, string> = {
    CLASS: '\u25C8',
    PROPERTY: '\u25C6',
    ENTITY: '\u25C9',
  }
  return icons[type] || '\u25CF'
}

// 边颜色
function getEdgeColor(type: string): string {
  const colors: Record<string, string> = {
    INHERITS: '#bf5fff',
    HAS_PROPERTY: '#00f0ff',
    HAS_RANGE: '#00f0ff',
    RELATES_TO: '#ffe066',
    INSTANCE_OF: '#00ffcc',
  }
  return colors[type] || '#00f0ff'
}

function isFlowEdge(type: string): boolean {
  return ['RELATES_TO', 'INSTANCE_OF', 'HAS_RANGE'].includes(type)
}

function getMarker(type: string): string {
  if (type === 'INHERITS') return 'url(#scifiArrowheadPurple)'
  return 'url(#scifiArrowhead)'
}

// 边路径计算
function getEdgePath(edge: GraphEdgeVO): string {
  const source = renderedNodes.value.find(n => n.id === edge.source)
  const target = renderedNodes.value.find(n => n.id === edge.target)
  if (!source || !target) return ''
  const sx = source.x || 0, sy = source.y || 0
  const tx = target.x || 0, ty = target.y || 0
  const dx = tx - sx, dy = ty - sy
  const dr = Math.sqrt(dx * dx + dy * dy) * 0.5
  return `M${sx},${sy} Q${sx + dx * 0.5},${sy + dy * 0.5 - dr * 0.3} ${tx},${ty}`
}

function getEdgeMidPoint(edge: GraphEdgeVO): { x: number; y: number } {
  const source = renderedNodes.value.find(n => n.id === edge.source)
  const target = renderedNodes.value.find(n => n.id === edge.target)
  if (!source || !target) return { x: 0, y: 0 }
  return {
    x: ((source.x || 0) + (target.x || 0)) / 2,
    y: ((source.y || 0) + (target.y || 0)) / 2 - 10,
  }
}

function getNodeEdges(nodeId: string): GraphEdgeVO[] {
  return renderedEdges.value.filter(e => e.source === nodeId || e.target === nodeId)
}

// 交互
function onNodeHover(node: GraphNodeVO, event: MouseEvent) {
  hoveredNode.value = node
  showPanel.value = true
  const rect = containerRef.value?.getBoundingClientRect()
  if (rect) {
    panelPosition.value = {
      x: event.clientX - rect.left + 10,
      y: event.clientY - rect.top + 10,
    }
  }
  emit('node-hover', node, event)
}

function onNodeLeave() {
  hoveredNode.value = null
  showPanel.value = false
  emit('node-leave')
}

function onNodeClick(node: GraphNodeVO) {
  emit('node-click', node)
}

// 控制
function zoomIn() {
  transform.value.scale = Math.min(transform.value.scale * 1.2, 3)
  applyTransform()
}

function zoomOut() {
  transform.value.scale = Math.max(transform.value.scale / 1.2, 0.3)
  applyTransform()
}

function resetView() {
  transform.value = { x: 0, y: 0, scale: 1 }
  applyTransform()
}

function toggleLabels() {
  showEdgeLabels.value = !showEdgeLabels.value
}

function applyTransform() {
  if (svgRef.value) {
    svgRef.value.style.transform = `translate(${transform.value.x}px, ${transform.value.y}px) scale(${transform.value.scale})`
  }
}

// 简单哈希
function hashString(str: string): number {
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i)
    hash = ((hash << 5) - hash) + char
    hash = hash & hash
  }
  return Math.abs(hash)
}

// D3 力导向布局
let resizeObserver: ResizeObserver | null = null

function initLayout() {
  if (!props.nodes.length) return
  const width = containerRef.value?.clientWidth || 800
  const height = props.height as number || 600

  // 圆形布局
  const centerX = width / 2
  const centerY = height / 2
  const radius = Math.min(width, height) * 0.35

  props.nodes.forEach((node, i) => {
    if (!node.x) {
      const angle = (2 * Math.PI * i) / props.nodes.length
      node.x = centerX + radius * Math.cos(angle) + (Math.random() - 0.5) * 80
      node.y = centerY + radius * Math.sin(angle) + (Math.random() - 0.5) * 80
    }
  })
}

onMounted(() => {
  nextTick(() => {
    initLayout()
    resizeObserver = new ResizeObserver(() => {
      initLayout()
    })
    if (containerRef.value) {
      resizeObserver.observe(containerRef.value)
    }
  })
})

onUnmounted(() => {
  resizeObserver?.disconnect()
})

watch(() => props.nodes, () => {
  nextTick(() => initLayout())
}, { deep: true })
</script>

<style scoped lang="less">
@import '@/assets/styles/scifi-variables.less';
@import '@/assets/styles/scifi-glass.less';
@import '@/assets/styles/scifi-animation.less';

.scifi-graph-container {
  position: relative;
  width: 100%;
  height: v-bind('typeof height === "number" ? height + "px" : height');
  background: @bg-deep;
  border-radius: 16px;
  overflow: hidden;
}

.graph-grid-bg {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(0, 240, 255, 0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0, 240, 255, 0.025) 1px, transparent 1px);
  background-size: 30px 30px;
  pointer-events: none;
}

.scifi-graph-svg {
  width: 100%;
  cursor: grab;
  transition: transform 0.3s ease;

  &:active {
    cursor: grabbing;
  }
}

.node-group {
  cursor: pointer;
  transition: transform 0.15s ease;

  &:hover .node-body {
    filter: url(#scifiNodeGlowStrong);
    transform: scale(1.1);
  }
}

.node-pulse-ring {
  opacity: 0;
  animation: nodePulseRing 2.5s ease-out infinite;
  transform-origin: center;
}

.scifi-edge {
  stroke-opacity: 0.6;
  transition: stroke-opacity 0.2s, stroke-width 0.2s;

  &:hover {
    stroke-opacity: 1;
    stroke-width: 2.5;
  }
}

.flowing-edge {
  stroke-dasharray: 6 3;
  animation: edgeFlow 2s linear infinite;
}

.edge-label {
  pointer-events: none;
  text-shadow: 0 0 4px rgba(0, 0, 0, 0.8);
}

.node-detail-float {
  position: absolute;
  z-index: 100;
  pointer-events: none;
}

.graph-legend {
  position: absolute;
  bottom: 16px;
  left: 16px;
  padding: 12px 16px;
  min-width: 120px;

  .legend-title {
    font-size: 12px;
    color: @text-dim;
    margin-bottom: 8px;
    text-transform: uppercase;
    letter-spacing: 1px;
  }

  .legend-item {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 6px;
  }

  .legend-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  .legend-text {
    color: @text-secondary;
    font-size: 12px;
  }
}

.graph-controls {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

// 过渡动画
.panel-fade-enter-active,
.panel-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.panel-fade-enter-from,
.panel-fade-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
</style>
