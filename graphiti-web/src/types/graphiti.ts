// ============================================================
// Shared TypeScript type definitions for OntoGraph
// Aligned with backend Java VO/DO structures
// ============================================================

// ----- Graph -----

export interface Graph {
  graphId: string
  name: string
  description?: string
  nodeCount?: number
  edgeCount?: number
  episodeCount?: number
  createdAt?: string
  updatedAt?: string
  // 兼容旧代码
  id?: string
}

export interface GraphStats {
  totalGraphs: number
  totalNodes: number
  totalEdges: number
  totalEpisodes: number
  nodeTrend?: number
  edgeTrend?: number
  episodeTrend?: number
}

export interface CreateGraphReq {
  name: string
  description?: string
}

// ----- Node -----

/** 后端节点数据（BackendNode） */
export interface BackendNode {
  uuid: string
  name: string
  type: string | null
  label?: string
  properties?: Record<string, any>
  createdAt?: string
  updatedAt?: string
  [key: string]: any
}

/** 节点列表响应项 */
export interface NodeListItem {
  uuid: string
  name: string
  type: string | null
  label: string
  properties: Record<string, any>
  createdAt?: string
  updatedAt?: string
}

/** NodeFilter 查询参数 */
export interface NodeFilter {
  graphId: string
  name?: string
  type?: string
  label?: string
  skip?: number
  limit?: number
}

/** 创建节点请求 */
export interface CreateNodeReq {
  name: string
  type?: string
  properties?: Record<string, any>
}

/** 更新节点请求 */
export interface UpdateNodeReq {
  name?: string
  type?: string
  properties?: Record<string, any>
}

// ----- Edge -----

/** 后端边数据（BackendEdge） */
export interface BackendEdge {
  uuid: string
  name?: string
  fact: string
  sourceNodeUuid: string
  targetNodeUuid: string
  groupId: string
  createdAt?: string
  validAt?: string
  invalidAt?: string
  expiredAt?: string
  episodes?: string[]
  properties?: Record<string, any>
  [key: string]: any
}

/** 边列表响应项 */
export interface EdgeListItem {
  uuid: string
  name: string | null
  fact: string
  sourceNodeUuid: string
  targetNodeUuid: string
  groupId: string
  createdAt?: string
  episodes?: string[]
  properties?: Record<string, any>
}

/** EdgeFilter 查询参数 */
export interface EdgeFilter {
  graphId?: string
  edgeTypes?: string[]
  sourceNodeTypes?: string[]
  targetNodeTypes?: string[]
  skip?: number
  limit?: number
}

/** 创建边请求 */
export interface CreateEdgeReq {
  sourceNodeUuid: string
  targetNodeUuid: string
  name?: string
  fact?: string
  properties?: Record<string, any>
}

/** 更新边请求 */
export interface UpdateEdgeReq {
  name?: string
  fact?: string
  properties?: Record<string, any>
}

// ----- Episode -----

export interface Episode {
  uuid: string
  name: string
  groupId: string
  source: string
  sourceDescription?: string
  content?: string
  createdAt?: string
  validAt?: string
  entityEdges?: string[]
  properties?: Record<string, any>
}

export interface EpisodeMentions {
  nodes: Array<{
    uuid: string
    name: string
    type: string | null
  }>
  edges: Array<{
    uuid: string
    fact: string
    sourceNodeUuid: string
    targetNodeUuid: string
  }>
}

// ----- Search -----

export interface SearchFilter {
  field: string
  operator: 'eq' | 'ne' | 'gt' | 'gte' | 'lt' | 'lte' | 'contains' | 'in'
  value: any
}

export interface SearchParams {
  query: string
  mode: 'semantic' | 'structured' | 'hybrid' | 'bm25' | 'vector' | 'bfs' | 'memory'
  graphId?: string
  filters?: SearchFilter[]
  limit?: number
  depth?: number
}

export interface SearchResult {
  id: string
  type: 'node' | 'edge'
  name: string
  entityType?: string
  relationType?: string
  properties: Record<string, any>
  score: number
  source?: string
  target?: string
  highlight?: Record<string, string[]>
}

// ----- Temporal -----

export interface TemporalFact {
  sourceNode: BackendNode
  targetNode: BackendNode
  edge: BackendEdge
  validAt: string
  invalidAt?: string
}

export interface EntityHistoryItem {
  uuid: string
  name: string
  type: string | null
  label: string
  properties: Record<string, any>
  validAt?: string
  invalidAt?: string
  createdAt?: string
}

export interface GraphStateAtTime {
  nodes: BackendNode[]
  edges: BackendEdge[]
}

// ----- Ontology -----

export interface FieldSchema {
  type: string
  description?: string
  required?: boolean
}

export interface EntityTypeSchema {
  description?: string
  fields: Record<string, FieldSchema>
}

export interface EdgeTypeSchema {
  description?: string
  sourceTypes: string[]
  targetTypes: string[]
  fields: Record<string, FieldSchema>
}

export interface Ontology {
  graphId: string
  entities: Record<string, EntityTypeSchema>
  edges: Record<string, EdgeTypeSchema>
}

export interface ValidationResult {
  valid: boolean
  errors: Array<{
    type: 'node' | 'edge'
    uuid: string
    message: string
  }>
}

// ----- Custom Instruction -----

export interface CustomInstruction {
  id: string
  instruction: string
  graphId?: string
  userId?: string
  createdAt?: string
}

export interface CreateCustomInstructionReq {
  instruction: string
  graphId?: string
  userId?: string
}

// ----- Community -----

export interface Community {
  id: string
  name: string
  nodeCount: number
  edgeCount: number
  nodes: string[]
  edges: string[]
}

// ----- Data Import/Export -----

export interface ImportDataReq {
  format: 'json' | 'csv' | 'triple'
  data: any
}

export interface ImportTask {
  id: string
  graphId: string
  format: 'json' | 'csv' | 'triple'
  status: 'pending' | 'processing' | 'completed' | 'failed'
  totalRows: number
  processedRows: number
  errorMessage?: string
  createdAt: string
  completedAt?: string
}

export interface ExportTask {
  id: string
  graphId: string
  format: 'json' | 'csv' | 'triple'
  status: 'pending' | 'processing' | 'completed' | 'failed'
  fileName?: string
  fileSize?: number
  createdAt: string
  completedAt?: string
}

// ----- ECharts Visualization -----

export interface EChartsNode {
  id: string
  name: string
  type?: string
  label?: string
  category: number
  value?: number | string
  symbolSize?: number
  itemStyle?: Record<string, any>
  labelConfig?: {
    show?: boolean
    color?: string
    fontSize?: number
  }
  data?: BackendNode
}

export interface EChartsEdge {
  id: string
  source: string
  target: string
  value?: string | number
  lineStyle?: Record<string, any>
  label?: {
    show?: boolean
    formatter?: string
    fontSize?: number
    color?: string
  }
  data?: BackendEdge
}
