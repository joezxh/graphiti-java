/**
 * 业务信息管理 API 客户端
 * Business Info Management API Client
 */

import request from '@/api/request'

// ============================================================
// 类型定义
// ============================================================

/** 图节点 */
export interface GraphNodeVO {
  id: string
  label: string
  type: 'CLASS' | 'PROPERTY' | 'ENTITY'
  category?: string
  color?: string
  description?: string
  example?: string
  data?: Record<string, any>
  x?: number
  y?: number
}

/** 图边 */
export interface GraphEdgeVO {
  id: string
  source: string
  target: string
  label?: string
  type: 'INHERITS' | 'HAS_PROPERTY' | 'HAS_RANGE' | 'RELATES_TO' | 'INSTANCE_OF'
  color?: string
  data?: Record<string, any>
}

/** 图元数据 */
export interface GraphMetaVO {
  nodeCount: number
  edgeCount: number
  entityTypeCount: number
  relationTypeCount: number
  entityTypes: string[]
  relationTypes: string[]
  graphId: string
  ontologyName?: string
  ontologyVersion?: string
}

/** 本体图数据 */
export interface OntologyGraphVO {
  nodes: GraphNodeVO[]
  edges: GraphEdgeVO[]
  meta: GraphMetaVO
}

/** 生成本体请求 */
export interface GenerateOntologyReqVO {
  draftName?: string
  businessScenario?: string
  domainHint?: string
  userInput: string
  namespace?: string
  version?: string
  saveAsDraft?: boolean
}

/** 生成本体响应 */
export interface GenerateOntologyRespVO {
  draftId?: number
  definition?: {
    name: string
    namespace: string
    version: string
    description: string
  }
  classes?: OntologyClassVO[]
  properties?: OntologyPropertyVO[]
  relationships?: OntologyRelationshipVO[]
  status?: string
  generatedAt?: string
}

export interface OntologyClassVO {
  localName: string
  classUri?: string
  parentClass?: string
  description?: string
  example?: string
  domainHint?: string
}

export interface OntologyPropertyVO {
  localName: string
  propertyUri?: string
  propertyType: string
  domainClass?: string
  rangeClass?: string
  rangeDataType?: string
  isRequired?: boolean
  isMultiple?: boolean
  description?: string
}

export interface OntologyRelationshipVO {
  sourceClass: string
  targetClass: string
  relationshipType: string
  description?: string
}

/** 优化描述请求 */
export interface OptimizeDescReqVO {
  originalDescription: string
  context?: string
  language?: string
  batchItems?: OptimizeItem[]
}

export interface OptimizeItem {
  id: string
  originalDescription: string
  context?: string
}

/** 优化描述响应 */
export interface OptimizeDescRespVO {
  original?: string
  optimizations?: OptimizationVO[]
  batchResults?: BatchOptimizeResult[]
}

export interface OptimizationVO {
  version: string
  description: string
  highlights?: string[]
}

export interface BatchOptimizeResult {
  id: string
  original: string
  optimizations: OptimizationVO[]
}

/** 生成数据请求 */
export interface GenerateDataReqVO {
  count?: number
  format?: 'JSON' | 'CSV' | 'N-TRIPLES'
  entityTypes?: string[]
  relationTypes?: string[]
  includeProperties?: boolean
  diversity?: 'low' | 'medium' | 'high'
}

/** 生成数据响应 */
export interface GenerateDataRespVO {
  draftId?: number
  entities?: EntityVO[]
  relationships?: RelationshipVO[]
  stats?: DataStatsVO
  formattedData?: string
}

export interface EntityVO {
  id: string
  name: string
  type: string
  properties?: Record<string, any>
}

export interface RelationshipVO {
  id: string
  source: string
  target: string
  type: string
  fact?: string
  properties?: Record<string, any>
}

export interface DataStatsVO {
  totalEntities: number
  totalRelationships: number
  entityTypeCount: number
  relationTypeCount: number
  entityTypes?: string[]
  relationTypes?: string[]
}

/** 草稿信息 */
export interface OntDraftVO {
  id: number
  graphId: string
  draftName: string
  draftType: 'DRAFT' | 'OPTIMIZED' | 'GENERATED'
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'APPLIED'
  createdBy?: string
  createdAt?: string
  updatedAt?: string
  hasMockData?: boolean
  mockEntityCount?: number
  mockRelationCount?: number
}

// ============================================================
// API 定义
// ============================================================

export const businessInfoApi = {
  // --- Feature 1: 本体定义生成 ---
  generateOntology: (graphId: string, data: GenerateOntologyReqVO) =>
    request.post<GenerateOntologyRespVO>(`/business-info/${graphId}/generate`, data),

  // --- Feature 2: 描述优化 ---
  optimizeDescription: (data: OptimizeDescReqVO) =>
    request.post<OptimizeDescRespVO>('/business-info/optimize', data),

  optimizeBatch: (data: OptimizeDescReqVO) =>
    request.post<OptimizeDescRespVO>('/business-info/optimize/batch', data),

  // --- Feature 3: 数据模拟生成 ---
  generateMockData: (graphId: string, data: GenerateDataReqVO) =>
    request.post<GenerateDataRespVO>(`/business-info/${graphId}/mock-data`, data),

  generateMockDataFromDraft: (graphId: string, draftId: number, data: GenerateDataReqVO) =>
    request.post<GenerateDataRespVO>(`/business-info/${graphId}/mock-data/from-draft/${draftId}`, data),

  // --- 草稿管理 ---
  listDrafts: (graphId: string) =>
    request.get<OntDraftVO[]>(`/business-info/${graphId}/drafts`),

  getDraftContent: (graphId: string, draftId: number) =>
    request.get<GenerateOntologyRespVO>(`/business-info/${graphId}/drafts/${draftId}`),

  saveDraft: (graphId: string, data: GenerateOntologyReqVO) =>
    request.post(`/business-info/${graphId}/drafts`, data),

  applyDraft: (graphId: string, draftId: number) =>
    request.post(`/business-info/${graphId}/drafts/${draftId}/apply`),

  approveDraft: (draftId: number) =>
    request.post(`/business-info/drafts/${draftId}/approve`),

  rejectDraft: (draftId: number) =>
    request.post(`/business-info/drafts/${draftId}/reject`),

  deleteDraft: (graphId: string, draftId: number) =>
    request.delete(`/business-info/${graphId}/drafts/${draftId}`),

  // --- 元数据查看 ---
  getOntologyGraph: (graphId: string) =>
    request.get<OntologyGraphVO>(`/business-info/${graphId}/metadata/graph`),

  getMockDataGraph: (graphId: string, draftId: number) =>
    request.get<OntologyGraphVO>(`/business-info/${graphId}/mock-graph/${draftId}`),

  getGraphStats: (graphId: string) =>
    request.get<Record<string, any>>(`/business-info/${graphId}/stats`),

  getMockDataStats: (graphId: string, draftId: number) =>
    request.get<Record<string, any>>(`/business-info/${graphId}/mock-stats/${draftId}`),
}

export default businessInfoApi
