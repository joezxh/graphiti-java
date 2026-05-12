import request from './request'

// ============================================================
// 类型定义
// ============================================================

/** 本体定义 */
export interface OntDefinitionVO {
  id?: number
  graphId?: string
  namespace?: string
  name: string
  version?: string
  status?: string
  description?: string
  parentVersionId?: number
  createdBy?: string
  createdAt?: string
  updatedAt?: string
  classCount?: number
  propertyCount?: number
  constraintCount?: number
}

/** 本体类 */
export interface OntClassVO {
  id: number
  definitionId?: number
  classUri: string
  localName: string
  parentClassId?: number
  parentClassUri?: string
  equivalentTo?: any[]
  disjointWith?: any[]
  description?: string
  example?: string
  domainHint?: string
  metadata?: string
  createdAt?: string
  updatedAt?: string
}

/** 类层次树节点 */
export interface ClassHierarchyVO {
  id?: number
  classUri: string
  localName: string
  description?: string
  domainHint?: string
  children?: ClassHierarchyVO[]
}

/** 本体属性 */
export interface OntPropertyVO {
  id: number
  definitionId?: number
  propertyUri: string
  localName: string
  propertyType: 'DATATYPE' | 'OBJECT' | 'ANNOTATION' | 'TRANSITIVE' | 'SYMMETRIC' | 'FUNCTIONAL'
  domainClassId?: number
  rangeClassId?: number
  rangeDataType?: string
  minCardinality?: number
  maxCardinality?: number
  defaultValue?: string
  allowedValues?: any[]
  parentPropertyId?: number
  equivalentTo?: any[]
  inverseOfId?: number
  isRequired?: boolean
  isMultiple?: boolean
  pattern?: string
  minValue?: number
  maxValue?: number
  description?: string
  example?: string
  metadata?: string
  createdAt?: string
  updatedAt?: string
}

/** 本体约束 */
export interface OntConstraintVO {
  id: number
  definitionId?: number
  classId?: number
  propertyId?: number
  constraintType: string
  value: string
  errorMessage?: string
  severity?: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

/** 版本历史 */
export interface OntVersionHistoryVO {
  id: number
  definitionId?: number
  version: string
  changeType: string
  entityType: string
  entityId?: number
  beforeState?: string
  afterState?: string
  diffSummary?: string
  changedBy?: string
  changedAt?: string
}

/** 完整本体信息 */
export interface OntologyFullVO {
  definition: OntDefinitionVO | null
  classes: OntClassVO[]
  classHierarchy: ClassHierarchyVO[]
  properties: OntPropertyVO[]
  constraints: OntConstraintVO[]
}

// ============================================================
// API 定义
// ============================================================

export const ontologyApi = {
  // ==================== 本体定义 ====================

  /**
   * 获取本体定义
   */
  async getDefinition(graphId: string): Promise<OntDefinitionVO | null> {
    return request.get<OntDefinitionVO>(`/ontology/${graphId}/definition`)
  },

  /**
   * 创建本体定义
   */
  async createDefinition(graphId: string, data: Partial<OntDefinitionVO>): Promise<OntDefinitionVO> {
    return request.post<OntDefinitionVO>(`/ontology/${graphId}/definition`, data)
  },

  /**
   * 获取完整本体信息
   */
  async getFullOntology(graphId: string): Promise<OntologyFullVO> {
    const resp = await request.get<any>(`/ontology/${graphId}`)
    return resp || {
      definition: null,
      classes: [],
      classHierarchy: [],
      properties: [],
      constraints: []
    }
  },

  // ==================== 类管理 ====================

  /**
   * 获取类列表
   */
  async listClasses(graphId: string): Promise<OntClassVO[]> {
    return request.get<OntClassVO[]>(`/ontology/${graphId}/classes`)
  },

  /**
   * 获取类层次树
   */
  async getClassHierarchy(graphId: string): Promise<ClassHierarchyVO[]> {
    return request.get<ClassHierarchyVO[]>(`/ontology/${graphId}/classes/hierarchy`)
  },

  /**
   * 创建类
   */
  async createClass(graphId: string, data: Partial<OntClassVO>): Promise<OntClassVO> {
    return request.post<OntClassVO>(`/ontology/${graphId}/classes`, data)
  },

  /**
   * 更新类
   */
  async updateClass(graphId: string, classId: number, data: Partial<OntClassVO>): Promise<OntClassVO> {
    return request.put<OntClassVO>(`/ontology/${graphId}/classes/${classId}`, data)
  },

  /**
   * 删除类
   */
  async deleteClass(graphId: string, classId: number): Promise<void> {
    return request.delete(`/ontology/${graphId}/classes/${classId}`)
  },

  // ==================== 属性管理 ====================

  /**
   * 获取属性列表
   */
  async listProperties(graphId: string): Promise<OntPropertyVO[]> {
    return request.get<OntPropertyVO[]>(`/ontology/${graphId}/properties`)
  },

  /**
   * 创建属性
   */
  async createProperty(graphId: string, data: Partial<OntPropertyVO>): Promise<OntPropertyVO> {
    return request.post<OntPropertyVO>(`/ontology/${graphId}/properties`, data)
  },

  /**
   * 更新属性
   */
  async updateProperty(graphId: string, propertyId: number, data: Partial<OntPropertyVO>): Promise<OntPropertyVO> {
    return request.put<OntPropertyVO>(`/ontology/${graphId}/properties/${propertyId}`, data)
  },

  /**
   * 删除属性
   */
  async deleteProperty(graphId: string, propertyId: number): Promise<void> {
    return request.delete(`/ontology/${graphId}/properties/${propertyId}`)
  },

  // ==================== 约束管理 ====================

  /**
   * 获取约束列表
   */
  async listConstraints(graphId: string): Promise<OntConstraintVO[]> {
    return request.get<OntConstraintVO[]>(`/ontology/${graphId}/constraints`)
  },

  /**
   * 创建约束
   */
  async createConstraint(graphId: string, data: Partial<OntConstraintVO>): Promise<OntConstraintVO> {
    return request.post<OntConstraintVO>(`/ontology/${graphId}/constraints`, data)
  },

  /**
   * 删除约束
   */
  async deleteConstraint(graphId: string, constraintId: number): Promise<void> {
    return request.delete(`/ontology/${graphId}/constraints/${constraintId}`)
  },

  // ==================== 版本历史 ====================

  /**
   * 获取版本历史
   */
  async getVersionHistory(graphId: string): Promise<OntVersionHistoryVO[]> {
    return request.get<OntVersionHistoryVO[]>(`/ontology/${graphId}/history`)
  },

  // ==================== 其他 ====================

  /**
   * 批量验证
   */
  async validateBatch(graphId: string, data: any): Promise<any> {
    return request.post(`/ontology/${graphId}/validate/batch`, data)
  },

  /**
   * 一致性检查
   */
  async checkConsistency(graphId: string): Promise<any> {
    return request.get(`/ontology/${graphId}/consistency`)
  }
}

export default ontologyApi
