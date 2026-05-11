/**
 * 图谱数据转换工具函数
 * 将后端图谱数据转换为 ECharts 力导向图所需的格式
 */

/** 后端节点数据接口 */
export interface BackendNode {
  uuid: string
  name: string
  type: string | null
  label?: string
  properties?: Record<string, any>
  [key: string]: any
}

/** 后端边数据接口 */
export interface BackendEdge {
  uuid: string
  source: string
  target: string
  type: string
  weight?: number
  properties?: Record<string, any>
  [key: string]: any
}

/** ECharts 节点数据接口 */
export interface EChartsNode {
  id: string
  name: string
  value: string
  category: number
  symbolSize: number
  itemStyle: {
    color: string
    borderColor: string
    borderWidth: number
    shadowBlur: number
    shadowColor: string
  }
  label: {
    show: boolean
    color: string
    fontSize: number
  }
  data: BackendNode
}

/** ECharts 边数据接口 */
export interface EChartsEdge {
  id: string
  source: string
  target: string
  value: string
  lineStyle: {
    width: number
    color: string
    curveness: number
    opacity: number
    type?: string
  }
  label?: {
    show: boolean
    formatter: string
    fontSize: number
    color: string
  }
  data: BackendEdge
}

/** 节点类型配置 */
interface NodeTypeConfig {
  color: string
  borderColor: string
  category: number
  symbol?: string
}

/** 图谱颜色配置（深色科技风） */
const NODE_TYPE_CONFIG: Record<string, NodeTypeConfig> = {
  // Entity 类型 - 主色系
  entity: {
    color: '#5e6ad2',
    borderColor: '#7b7ff0',
    category: 0,
    symbol: 'circle'
  },
  // Episode 类型 - 青色系
  episode: {
    color: '#00d4ff',
    borderColor: '#33ddff',
    category: 1,
    symbol: 'diamond'
  },
  // Event 类型 - 橙色系
  event: {
    color: '#ff8c00',
    borderColor: '#ffa033',
    category: 2,
    symbol: 'rect'
  },
  // 默认类型
  default: {
    color: '#8a8f98',
    borderColor: '#a0a5ad',
    category: 3,
    symbol: 'circle'
  }
}

/** 图谱深色主题背景 */
export const GRAPH_BACKGROUND_COLOR = '#010102'

/** 力导向图布局配置 */
export const FORCE_LAYOUT_CONFIG = {
  repulsion: 800,
  edgeLength: 120,
  gravity: 0.1,
  friction: 0.6,
  layoutAnimation: true
}

/**
 * 根据节点类型获取配置
 */
function getNodeTypeConfig(type: string | null | undefined): NodeTypeConfig {
  if (!type) return NODE_TYPE_CONFIG.default
  const lowerType = type.toLowerCase()
  if (lowerType.includes('entity')) return NODE_TYPE_CONFIG.entity
  if (lowerType.includes('episode')) return NODE_TYPE_CONFIG.episode
  if (lowerType.includes('event')) return NODE_TYPE_CONFIG.event
  return NODE_TYPE_CONFIG.default
}

/**
 * 计算节点大小（基于度中心性）
 */
function calculateNodeSize(node: BackendNode, allEdges: BackendEdge[]): number {
  const uuid = node.uuid
  const degree = allEdges.filter(e => e.source === uuid || e.target === uuid).length
  // 基础大小 30，每多一条边增加 5，最大 80
  return Math.min(30 + degree * 5, 80)
}

/**
 * 将后端节点数据转换为 ECharts 节点格式
 */
export function transformNodes(
  nodes: BackendNode[],
  edges: BackendEdge[]
): EChartsNode[] {
  return nodes.map(node => {
    const config = getNodeTypeConfig(node.type)
    const size = calculateNodeSize(node, edges)
    
    return {
      id: node.uuid,
      name: node.name || node.uuid.substring(0, 8),
      value: node.type,
      category: config.category,
      symbol: config.symbol,
      symbolSize: size,
      itemStyle: {
        color: config.color,
        borderColor: config.borderColor,
        borderWidth: 2,
        shadowBlur: 10,
        shadowColor: config.color + '66' // 40% 透明度
      },
      label: {
        show: true,
        color: '#f7f8f8',
        fontSize: 12
      },
      data: node
    }
  })
}

/**
 * 将后端边数据转换为 ECharts 边格式
 */
export function transformEdges(edges: BackendEdge[]): EChartsEdge[] {
  return edges.map(edge => {
    const weight = edge.weight || 1
    // 宽度与权重正相关，范围 1-10
    const width = Math.max(1, Math.min(weight * 2, 10))
    
    return {
      id: edge.uuid,
      source: edge.source,
      target: edge.target,
      value: edge.type,
      lineStyle: {
        width,
        color: '#5e6ad266', // 40% 透明度的主色
        curveness: 0.2,
        opacity: 0.6,
        type: edge.properties?.expired_at ? 'dashed' : 'solid'
      },
      label: {
        show: false,
        formatter: edge.type,
        fontSize: 10,
        color: '#8a8f98'
      },
      data: edge
    }
  })
}

/**
 * 转换完整图谱数据
 */
export function transformGraphData(
  nodes: BackendNode[],
  edges: BackendEdge[]
): {
  nodes: EChartsNode[]
  edges: EChartsEdge[]
  categories: Array<{ name: string; itemStyle: { color: string } }>
} {
  const transformedNodes = transformNodes(nodes, edges)
  const transformedEdges = transformEdges(edges)
  
  // 分类信息（用于图例）
  const categories = [
    { name: '实体 (Entity)', itemStyle: { color: NODE_TYPE_CONFIG.entity.color } },
    { name: '事件 (Episode)', itemStyle: { color: NODE_TYPE_CONFIG.episode.color } },
    { name: '事件 (Event)', itemStyle: { color: NODE_TYPE_CONFIG.event.color } },
    { name: '其他', itemStyle: { color: NODE_TYPE_CONFIG.default.color } }
  ]
  
  return {
    nodes: transformedNodes,
    edges: transformedEdges,
    categories
  }
}

/**
 * 生成 ECharts 力导向图配置项
 */
export function generateForceGraphOption(
  nodes: EChartsNode[],
  edges: EChartsEdge[],
  categories: Array<{ name: string; itemStyle: { color: string } }>,
  options?: {
    showLabels?: boolean
    highlightNode?: string
  }
): any {
  const { showLabels = true, highlightNode } = options || {}
  
  // 如果指定了高亮节点，调整样式
  const processedNodes = highlightNode
    ? nodes.map(n => ({
        ...n,
        itemStyle: {
          ...n.itemStyle,
          opacity: n.id === highlightNode || edges.some(e => 
            (e.source === highlightNode && e.target === n.id) ||
            (e.target === highlightNode && e.source === n.id)
          ) ? 1 : 0.2
        },
        label: {
          ...n.label,
          show: n.id === highlightNode || edges.some(e => 
            (e.source === highlightNode && e.target === n.id) ||
            (e.target === highlightNode && e.source === n.id)
          ) ? true : false
        }
      }))
    : nodes.map(n => ({ ...n, itemStyle: { ...n.itemStyle, opacity: 1 } }))
  
  return {
    backgroundColor: GRAPH_BACKGROUND_COLOR,
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 16, 17, 0.9)',
      borderColor: '#23252a',
      textStyle: { color: '#f7f8f8', fontSize: 12 },
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          const node = params.data.data as BackendNode
          return `
            <div style="padding: 8px;">
              <div style="font-weight: 600; margin-bottom: 4px;">${node.name || node.uuid}</div>
              <div style="color: #8a8f98; font-size: 11px;">UUID: ${node.uuid}</div>
              <div style="color: #8a8f98; font-size: 11px;">类型: ${node.type ?? '(无)'}</div>
            </div>
          `
        }
        if (params.dataType === 'edge') {
          const edge = params.data.data as BackendEdge
          return `
            <div style="padding: 8px;">
              <div style="font-weight: 600; margin-bottom: 4px;">${edge.type}</div>
              <div style="color: #8a8f98; font-size: 11px;">UUID: ${edge.uuid}</div>
            </div>
          `
        }
        return ''
      }
    },
    legend: {
      data: categories.map(c => c.name),
      textStyle: { color: '#8a8f98', fontSize: 12 },
      top: 10,
      right: 10,
      backgroundColor: 'rgba(15, 16, 17, 0.8)',
      borderColor: '#23252a',
      borderRadius: 8,
      padding: [8, 12]
    },
    animationDuration: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [
      {
        type: 'graph',
        layout: 'force',
        data: processedNodes,
        edges: processedEdges(highlightNode, edges),
        categories,
        roam: true, // 支持缩放和拖拽
        draggable: true,
        focusNodeAdjacency: true,
        force: FORCE_LAYOUT_CONFIG,
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: [0, 10],
        label: {
          show: showLabels,
          position: 'right',
          distance: 5,
          fontSize: 12,
          color: '#f7f8f8'
        },
        lineStyle: {
          opacity: 0.6,
          curveness: 0.2,
          width: 2
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: {
            width: 4,
            color: '#5e6ad2'
          }
        }
      }
    ]
  }
}

/**
 * 处理边的高亮状态
 */
function processedEdges(
  highlightNode: string | undefined,
  edges: EChartsEdge[]
): EChartsEdge[] {
  if (!highlightNode) return edges
  
  return edges.map(e => ({
    ...e,
    lineStyle: {
      ...e.lineStyle,
      opacity: (e.source === highlightNode || e.target === highlightNode) ? 1 : 0.1,
      width: (e.source === highlightNode || e.target === highlightNode) ? e.lineStyle.width * 1.5 : e.lineStyle.width
    }
  }))
}

/**
 * 生成 ECharts 树图配置项
 */
export function generateTreeGraphOption(
  nodes: EChartsNode[],
  edges: EChartsEdge[],
  _categories: Array<{ name: string; itemStyle: { color: string } }>,
  options?: {
    showLabels?: boolean
    highlightNode?: string
  }
): any {
  const { showLabels = true, highlightNode: _highlightNode } = options || {}
  
  // 构建树结构（以第一个节点为根）
  const rootNode = nodes[0]
  if (!rootNode) return {}
  
  // 构建邻接表
  const adjacency = new Map<string, string[]>()
  edges.forEach(edge => {
    if (!adjacency.has(edge.source)) adjacency.set(edge.source, [])
    adjacency.get(edge.source)!.push(edge.target)
  })
  
  // 递归构建树数据
  function buildTreeNode(nodeId: string, depth: number = 0): any {
    const node = nodes.find(n => n.id === nodeId)
    if (!node || depth > 10) return null
    
    const children = (adjacency.get(nodeId) || [])
      .map(childId => buildTreeNode(childId, depth + 1))
      .filter(Boolean)
    
    return {
      name: node.name,
      value: node.value,
      itemStyle: node.itemStyle,
      label: node.label,
      children: children.length > 0 ? children : undefined
    }
  }
  
  const treeData = buildTreeNode(rootNode.id)
  
  return {
    backgroundColor: GRAPH_BACKGROUND_COLOR,
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 16, 17, 0.9)',
      borderColor: '#23252a',
      textStyle: { color: '#f7f8f8', fontSize: 12 }
    },
    series: [
      {
        type: 'tree',
        data: [treeData],
        top: '10%',
        left: '10%',
        bottom: '10%',
        right: '20%',
        symbolSize: 20,
        label: {
          show: showLabels,
          position: 'left',
          color: '#f7f8f8',
          fontSize: 12
        },
        leaves: {
          label: {
            show: showLabels,
            position: 'right',
            color: '#f7f8f8'
          }
        },
        emphasis: {
          focus: 'descendant'
        },
        expandAndCollapse: true,
        animationDuration: 400,
        animationDurationUpdate: 400
      }
    ]
  }
}
