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

/** 域规则 */
export interface DomainRuleVO {
  id?: number
  definitionId?: number
  ruleName: string
  ruleCode: string
  spelExpression: string
  applicableClassIds?: number[]
  severity?: string
  errorMessage?: string
  description?: string
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
  lastTestResult?: {
    passed: boolean
    testData: string
    result: any
    error: string | null
    testedAt: string
  }
}

/** 推理类型 */
export interface InferredTypeVO {
  className?: string
  classUri: string
  confidence?: number
  reason?: string
}

/** 验证任务 */
export interface ValidationTaskVO {
  taskId: string
  graphId: string
  status: string
  checkType?: string
  result?: any
  errorMessage?: string
  createdAt?: string
  completedAt?: string
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
    return request.get<OntologyFullVO>(`/ontology/${graphId}`)
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
   * 更新约束
   */
  async updateConstraint(graphId: string, constraintId: number, data: Partial<OntConstraintVO>): Promise<OntConstraintVO> {
    return request.put<OntConstraintVO>(`/ontology/${graphId}/constraints/${constraintId}`, data)
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
  },

  // ==================== 域规则管理 ====================

  /**
   * 列出域规则
   */
  async listDomainRules(graphId: string): Promise<DomainRuleVO[]> {
    return request.get<DomainRuleVO[]>(`/ontology/${graphId}/domain-rules`)
  },

  /**
   * 创建域规则
   */
  async createDomainRule(graphId: string, data: Partial<DomainRuleVO>): Promise<DomainRuleVO> {
    return request.post<DomainRuleVO>(`/ontology/${graphId}/domain-rules`, data)
  },

  /**
   * 更新域规则
   */
  async updateDomainRule(graphId: string, ruleId: number, data: Partial<DomainRuleVO>): Promise<DomainRuleVO> {
    return request.put<DomainRuleVO>(`/ontology/${graphId}/domain-rules/${ruleId}`, data)
  },

  /**
   * 删除域规则
   */
  async deleteDomainRule(graphId: string, ruleId: number): Promise<void> {
    return request.delete(`/ontology/${graphId}/domain-rules/${ruleId}`)
  },

  /**
   * 启用/禁用域规则
   */
  async toggleDomainRule(graphId: string, ruleId: number, enabled: boolean): Promise<void> {
    return request.patch(`/ontology/${graphId}/domain-rules/${ruleId}/toggle`, null, { params: { enabled } })
  },

  /**
   * 测试域规则
   */
  async testDomainRule(graphId: string, spelExpression: string, testProperties: Record<string, any>): Promise<{ passed: boolean; result: any; error: string | null }> {
    return request.post(`/ontology/${graphId}/domain-rules/test`, { spelExpression, testProperties })
  },

  // ==================== 图谱完整性检查（L6） ====================

  /**
   * 提交完整性检查
   */
  async submitIntegrityCheck(graphId: string, checkTypes?: string[]): Promise<{ taskId: string }> {
    return request.post(`/ontology/${graphId}/validate/integrity`, checkTypes ? { checkTypes } : {})
  },

  /**
   * 查询验证任务状态
   */
  async getValidationTaskStatus(taskId: string): Promise<ValidationTaskVO> {
    return request.get<ValidationTaskVO>(`/ontology/validate/tasks/${taskId}`)
  },

  /**
   * 列出验证任务
   */
  async listValidationTasks(graphId: string): Promise<ValidationTaskVO[]> {
    return request.get<ValidationTaskVO[]>(`/ontology/${graphId}/validate/tasks`)
  },

  // ==================== 推理类型推断 ====================

  /**
   * 推理类型推断
   */
  async inferTypes(graphId: string, properties: Record<string, any>): Promise<InferredTypeVO[]> {
    return request.post(`/ontology/${graphId}/reasoners/infer-types`, { properties })
  },

  /**
   * 获取属性 domain
   */
  async getPropertyDomains(graphId: string, propertyUri: string): Promise<string[]> {
    return request.get(`/ontology/${graphId}/properties/${encodeURIComponent(propertyUri)}/domains`)
  },

  /**
   * 获取属性 range
   */
  async getPropertyRanges(graphId: string, propertyUri: string): Promise<string[]> {
    return request.get(`/ontology/${graphId}/properties/${encodeURIComponent(propertyUri)}/ranges`)
  }
}

export default ontologyApi
