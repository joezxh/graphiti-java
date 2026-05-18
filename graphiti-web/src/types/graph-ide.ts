/**
 * Graph IDE 类型定义
 * Graph IDE Type Definitions
 */

/**
 * 图谱可视化节点
 */
export interface GraphIDENode {
  uuid: string
  name: string
  type: string
  x?: number
  y?: number
  properties?: Record<string, any>
  summary?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 图谱可视化边
 */
export interface GraphIDEEdge {
  uuid: string
  source: string
  target: string
  type: string
  fact?: string
  properties?: Record<string, any>
}

/**
 * 图谱可视化数据
 */
export interface GraphVisualizationData {
  nodes: GraphIDENode[]
  edges: GraphIDEEdge[]
  pagination?: {
    page: number
    pageSize: number
    total: number
    totalPages: number
  }
  aggregations?: {
    byClass: Array<{ type: string; count: number }>
  }
}

/**
 * Schema 类定义
 */
export interface SchemaClass {
  id: number
  definitionId: number
  classUri: string
  localName: string
  description?: string
  parentClassIds: number[]
  propertyCount: number
  properties?: SchemaProperty[]
}

/**
 * Schema 属性定义
 */
export interface SchemaProperty {
  id: number
  definitionId: number
  localName: string
  propertyType: 'DATATYPE' | 'OBJECT' | 'ANNOTATION' | 'TRANSITIVE' | 'SYMMETRIC' | 'FUNCTIONAL'
  rangeDataType?: string
  domainClassId?: number
  rangeClassId?: number
  isRequired: boolean
  isMultiple: boolean
  defaultValue?: any
  allowedValues?: any[]
  pattern?: string
  minValue?: number
  maxValue?: number
  minCardinality?: number
  maxCardinality?: number
  description?: string
}

/**
 * 级联编辑筛选条件
 */
export interface CascadeFilter {
  classType: string
  conditions: PropertyCondition[]
  logic: 'AND' | 'OR'
}

/**
 * 属性条件
 */
export interface PropertyCondition {
  propertyName: string
  operator: CascadeOperator
  value: any
}

/**
 * 级联操作符
 */
export type CascadeOperator =
  | 'eq'
  | 'ne'
  | 'gt'
  | 'lt'
  | 'gte'
  | 'lte'
  | 'contains'
  | 'not_contains'
  | 'in'
  | 'not_in'
  | 'is_null'
  | 'is_not_null'

/**
 * 级联编辑预览响应
 */
export interface CascadePreviewResponse {
  totalMatch: number
  distribution: Array<{
    groupBy: string
    value: string
    count: number
  }>
}

/**
 * 级联编辑请求
 */
export interface CascadeExecuteRequest {
  classType: string
  conditions: PropertyCondition[]
  logic: 'AND' | 'OR'
  updates: Record<string, any>
}

/**
 * 级联编辑响应
 */
export interface CascadeExecuteResponse {
  success: boolean
  affectedCount: number
  failedCount: number
  errors: string[]
}

/**
 * Schema 变更验证请求
 */
export interface SchemaChangeValidationRequest {
  type: 'CREATE_CLASS' | 'UPDATE_CLASS' | 'DELETE_CLASS' | 'CREATE_PROPERTY' | 'UPDATE_PROPERTY' | 'DELETE_PROPERTY'
  classId?: number
  propertyId?: number
  changes?: Record<string, { old: any; new: any }>
}

/**
 * Schema 变更验证响应
 */
export interface SchemaChangeValidationResponse {
  compatible: boolean
  affectedNodes: number
  violations: Array<{
    nodeUuid: string
    propertyName?: string
    reason: string
  }>
}

/**
 * 布局类型
 */
export type LayoutType = 'force' | 'grid' | 'dagre' | 'concentric'

/**
 * 编辑工具
 */
export type EditTool = 'select' | 'pan' | 'add-node' | 'add-edge'

/**
 * 侧边栏标签
 */
export type SidebarTab = 'explorer' | 'schema'

/**
 * 详情面板标签
 */
export type DetailPanelTab = 'info' | 'properties' | 'relations' | 'instances'

/**
 * 类实例数据
 */
export interface ClassInstance {
  uuid: string
  name: string
  type: string
  properties?: Record<string, any>
  summary?: string
  createdAt?: string
  updatedAt?: string
  // 用于高亮状态
  inCanvas?: boolean
}

/**
 * 类树节点（用于 UI）
 */
export interface ClassTreeNode {
  id: number
  name: string
  propertyCount: number
  children?: ClassTreeNode[]
  expanded?: boolean
  selected?: boolean
}

/**
 * 节点关联关系
 */
export interface NodeRelation {
  id: string
  type: string
  targetId: string
  targetName: string
}
