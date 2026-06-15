/**
 * 法律知识图谱 V3.0.0 TypeScript 类型定义
 * 包含 Community、Episode、Entity、Relationship 的 V3 扩展类型
 *
 * V3.1.0 通用化改造：
 * - legal_domain → domainType
 * - jurisdiction → region
 * - practice_type → scenarioType
 * - legal_process → processType
 * - court_level → stageLevel
 * - is_trial_stage → isReviewStage
 */

// ========== Community V3 ==========

/** 社区节点 (V3.0.0 通用化) */
export interface CommunityV3 {
  uuid: string
  name: string
  communityType?: string
  /** V3.1.0 领域类型（通用化，原 legal_domain） */
  domainType?: string
  /** V3.1.0 区域/管辖区（通用化，原 jurisdiction） */
  region?: string
  /** V3.1.0 场景类型（通用化，原 practice_type） */
  scenarioType?: string
  subDomainType?: string
  parentCommunityUuid?: string
  summary?: string
  memberCount?: number
  keyProvisions?: string[]
  description?: string
  metadata?: CommunityMetadata
  createdAt?: string
  updatedAt?: string
  /** V3.0.0 向后兼容字段（已废弃） */
  legalDomain?: string
  jurisdiction?: string
  practiceType?: string
}

export interface CommunityMetadata {
  icon?: string
  color?: string
  displayPriority?: number
}

/** 社区类型元数据（来自 ont_community_type 表） */
export interface CommunityTypeMeta {
  id: number
  typeCode: string
  typeName: string
  typeNameEn?: string
  /** V3.1.0 分类维度：domain|region|scenario */
  category: 'domain' | 'region' | 'scenario'
  parentTypeCode?: string
  sortOrder: number
  metadata: CommunityMetadata
  description?: string
  /** V3.1.0 区域字段 */
  region?: string
  /** V3.1.0 场景字段 */
  scenarioType?: string
  communityUuid?: string
  graphId?: string
}

/** 社区树节点（用于 a-tree 组件） */
export interface CommunityTreeNode {
  key: string
  title: string
  icon?: string
  color?: string
  /** V3.1.0 通用化字段 */
  domainType?: string
  region?: string
  scenarioType?: string
  isLeaf?: boolean
  children?: CommunityTreeNode[]
  /** V3.0.0 向后兼容字段（已废弃） */
  legalDomain?: string
  jurisdiction?: string
  practiceType?: string
}

// ========== Episode V3 ==========

/** Episode 节点 (V3.0.0 通用化) */
export interface EpisodeV3 {
  uuid: string
  name: string
  episodeType?: string
  /** V3.1.0 业务流程类型（通用化，原 legal_process） */
  processType?: string
  stageLabel?: string
  /** V3.1.0 阶段级别（通用化，原 court_level） */
  stageLevel?: string | null
  /** V3.1.0 是否审查/评议阶段（通用化，原 is_trial_stage） */
  isReviewStage?: boolean
  caseId?: string
  startTime?: string
  endTime?: string
  content?: string
  source?: string
  sourceDescription?: string
  createdAt?: string
  groupId?: string
  /** V3.0.0 向后兼容字段（已废弃） */
  legalProcess?: string
  courtLevel?: string | null
  isTrialStage?: boolean
}

/** Episode 类型元数据（来自 ont_episode_type 表） */
export interface EpisodeTypeMeta {
  typeCode: string
  /** V3.1.0 业务流程类型（通用化） */
  processType?: string
  stageLabel?: string
  /** V3.1.0 阶段级别（通用化） */
  stageLevel?: string | null
  /** V3.1.0 是否审查/评议阶段（通用化） */
  isReviewStage?: boolean
  count: number
  /** V3.0.0 向后兼容字段（已废弃） */
  legalProcess?: string
  courtLevel?: string | null
  isTrialStage?: boolean
}

/** 业务流程类型分组（V3.1.0 通用化） */
export type ProcessTypeGroup = 'business_process' | 'lifecycle' | 'workflow' | 'mediation' | 'arbitration' | 'execution'

export const PROCESS_TYPE_LABELS: Record<ProcessTypeGroup, string> = {
  business_process: '业务流程',
  lifecycle: '生命周期',
  workflow: '工作流',
  mediation: '调解',
  arbitration: '仲裁',
  execution: '执行',
}

// ========== Entity V3 ==========

/** 实体节点 (V3.0.0) */
export interface EntityV3 {
  uuid: string
  name: string
  type: string
  category?: string
  categoryLevel?: number
  properties?: Record<string, any>
  summary?: string
  createdAt?: string
  updatedAt?: string
}

/** 实体分类元数据（来自 ont_entity_category 表） */
export interface EntityCategoryMeta {
  id: number
  categoryCode: string
  categoryName: string
  categoryLevel: number
  parentCategoryCode?: string
  entityTypeScope: string[]
  sortOrder: number
  description?: string
}

// ========== Relationship V3 ==========

/** 关系 (V3.0.0) */
export interface RelationshipV3 {
  uuid: string
  source: string
  target: string
  type: string
  name?: string
  isDirectional?: boolean
  defaultWeight?: number
  properties?: Record<string, any>
  fact?: string
  createdAt?: string
}

/** 关系元数据（来自 ont_relationship_meta 表或动态查询） */
export interface RelationshipMeta {
  relationshipType: string
  sourceType?: string
  targetType?: string
  count?: number
  defaultWeight?: number
  description?: string
  sampleFact?: string
}

// ========== 颜色体系 (V3) ==========

/** 领域类型色彩映射（V3.1.0 通用化） */
export const DOMAIN_TYPE_COLORS: Record<string, string> = {
  DOMAIN_ROOT: '#37474F',
  // 法律领域
  DOMAIN_LEGAL: '#2E7D32',
  DOMAIN_CIVIL: '#2E7D32',
  DOMAIN_CRIMINAL: '#C62828',
  DOMAIN_ADMIN: '#6A1B9A',
  DOMAIN_IP: '#F57F17',
  DOMAIN_LABOR: '#00838F',
  DOMAIN_MEDIATION: '#AD1457',
  DOMAIN_EXECUTION: '#37474F',
  // 金融领域
  DOMAIN_FINANCE: '#1565C0',
  DOMAIN_BANKING: '#0277BD',
  DOMAIN_SECURITIES: '#00838F',
  DOMAIN_INSURANCE: '#2E7D32',
  DOMAIN_RISK: '#D84315',
  // 企业管理领域
  DOMAIN_ENTERPRISE: '#6A1B9A',
  DOMAIN_HR: '#6A1B9A',
  DOMAIN_FINANCE_MGMT: '#0277BD',
  DOMAIN_COMPLIANCE: '#F57F17',
  DOMAIN_GOVERNANCE: '#37474F',
  // 医疗领域
  DOMAIN_MEDICAL: '#C62828',
  DOMAIN_CLINICAL: '#C62828',
  DOMAIN_DRUG: '#6A1B9A',
  DOMAIN_PUBLIC_HEALTH: '#00838F',
  // 社会治理领域
  DOMAIN_SOCIAL_GOV: '#E65100',
}

/** 向后兼容别名 */
export const LEGAL_DOMAIN_COLORS: Record<string, string> = DOMAIN_TYPE_COLORS

/** 区域色彩映射（V3.1.0） */
export const REGION_COLORS: Record<string, string> = {
  REGION_ROOT: '#78909C',
  REGION_CN: '#C62828',
  REGION_US: '#1565C0',
  REGION_EU: '#1565C0',
  REGION_INTERNATIONAL: '#78909C',
}

/** 向后兼容别名 */
export const JURISDICTION_COLORS: Record<string, string> = REGION_COLORS

/** 场景类型色彩映射（V3.1.0） */
export const SCENARIO_TYPE_COLORS: Record<string, string> = {
  SCENARIO_ROOT: '#78909C',
  SCENARIO_JUDICIAL: '#2E7D32',
  SCENARIO_COMPLIANCE: '#F57F17',
  SCENARIO_RISK: '#D84315',
  SCENARIO_LIFECYCLE: '#00838F',
  SCENARIO_LAW_REGULATE: '#5D4037',
  SCENARIO_FEEDBACK: '#AD1457',
  SCENARIO_GOVERNANCE: '#E65100',
  SCENARIO_PREVENTION: '#37474F',
}

/** 向后兼容别名 */
export const PRACTICE_COLORS: Record<string, string> = SCENARIO_TYPE_COLORS

/** Episode 类型色彩映射 */
export const EPISODE_TYPE_COLORS: Record<string, string> = {
  EP_INITIATION: '#42A5F5',
  EP_FILING: '#42A5F5',
  EP_SERVING: '#64B5F6',
  EP_EVALUATION: '#FFA726',
  EP_TRIAL: '#2E7D32',
  EP_TRIAL_1ST: '#2E7D32',
  EP_JUDGMENT: '#1B5E20',
  EP_JUDGMENT_1ST: '#1B5E20',
  EP_APPEAL: '#FFA726',
  EP_TRIAL_2ND: '#388E3C',
  EP_JUDGMENT_2ND: '#1B5E20',
  EP_RETRIAL: '#D32F2F',
  EP_EXECUTION: '#37474F',
  EP_MEDIATION: '#EC407A',
  EP_MEDIATION_ACCEPT: '#EC407A',
  EP_MEDIATION_NEGOTIATION: '#F48FB1',
  EP_MEDIATION_AGREEMENT: '#CE93D8',
  EP_MEDIATION_CONFIRM: '#AB47BC',
  EP_WORKFLOW_START: '#0288D1',
  EP_WORKFLOW_NODE: '#039BE5',
  EP_WORKFLOW_END: '#0277BD',
  EP_REPORT_RECEIVE: '#5E35B1',
  EP_TRIAGE_ASSESS: '#7E57C2',
  EP_COORDINATION: '#9575CD',
  EP_FEEDBACK: '#AD1457',
  EP_FOLLOW_UP: '#C2185B',
  EP_CLOSE: '#880E4F',
}

/** 关系类型色彩映射 */
export const RELATIONSHIP_COLORS: Record<string, string> = {
  HAS_COMMUNITY: '#1565C0',
  PARENT_OF: '#5E35B1',
  MENTIONS: '#0288D1',
  NEXT_EPISODE: '#00838F',
  CITES: '#2E7D32',
  INVOLVES: '#F57F17',
  BELONGS_TO: '#6A1B9A',
  PRECEDES: '#AD1457',
  REPRESENTS: '#00838F',
  PRESIDES_OVER: '#C62828',
  PARTY_OF: '#6D4C41',
  SUBSTANTIATES: '#546E7A',
  AFFIRMED_BY: '#AD1457',
  RELATES_TO: '#78909C',
}

// ========== V3.1.0 通用化常量 ==========

/** 领域类型选项（用于前端下拉） */
export const DOMAIN_TYPE_OPTIONS = [
  { value: 'DOMAIN_ROOT', label: '知识领域', color: '#37474F' },
  { value: 'DOMAIN_LEGAL', label: '法律', color: '#2E7D32' },
  { value: 'DOMAIN_CIVIL', label: '民商事', color: '#2E7D32' },
  { value: 'DOMAIN_CRIMINAL', label: '刑事法律', color: '#C62828' },
  { value: 'DOMAIN_ADMIN', label: '行政法律', color: '#6A1B9A' },
  { value: 'DOMAIN_IP', label: '知识产权', color: '#F57F17' },
  { value: 'DOMAIN_LABOR', label: '劳动法律', color: '#00838F' },
  { value: 'DOMAIN_FINANCE', label: '金融', color: '#1565C0' },
  { value: 'DOMAIN_BANKING', label: '银行与信贷', color: '#0277BD' },
  { value: 'DOMAIN_SECURITIES', label: '证券与投资', color: '#00838F' },
  { value: 'DOMAIN_INSURANCE', label: '保险业务', color: '#2E7D32' },
  { value: 'DOMAIN_RISK', label: '风险管控', color: '#D84315' },
  { value: 'DOMAIN_ENTERPRISE', label: '企业管理', color: '#6A1B9A' },
  { value: 'DOMAIN_HR', label: '人力资源', color: '#6A1B9A' },
  { value: 'DOMAIN_FINANCE_MGMT', label: '财务管理', color: '#0277BD' },
  { value: 'DOMAIN_COMPLIANCE', label: '企业合规', color: '#F57F17' },
  { value: 'DOMAIN_GOVERNANCE', label: '公司治理', color: '#37474F' },
  { value: 'DOMAIN_MEDICAL', label: '医疗', color: '#C62828' },
  { value: 'DOMAIN_CLINICAL', label: '临床诊疗', color: '#C62828' },
  { value: 'DOMAIN_DRUG', label: '药品与器械', color: '#6A1B9A' },
  { value: 'DOMAIN_PUBLIC_HEALTH', label: '公共卫生', color: '#00838F' },
  { value: 'DOMAIN_SOCIAL_GOV', label: '社会综合治理', color: '#E65100' },
]

/** 区域选项（用于前端下拉） */
export const REGION_OPTIONS = [
  { value: 'REGION_ROOT', label: '全球/通用', color: '#78909C' },
  { value: 'REGION_CN', label: '中国', color: '#C62828' },
  { value: 'REGION_US', label: '美国', color: '#1565C0' },
  { value: 'REGION_EU', label: '欧洲', color: '#1565C0' },
  { value: 'REGION_INTERNATIONAL', label: '国际组织', color: '#78909C' },
]

/** 场景类型选项（用于前端下拉） */
export const SCENARIO_TYPE_OPTIONS = [
  { value: 'SCENARIO_ROOT', label: '通用场景', color: '#78909C' },
  { value: 'SCENARIO_JUDICIAL', label: '司法实践', color: '#2E7D32' },
  { value: 'SCENARIO_COMPLIANCE', label: '合规管理', color: '#F57F17' },
  { value: 'SCENARIO_RISK', label: '风险管控', color: '#D84315' },
  { value: 'SCENARIO_LIFECYCLE', label: '生命周期', color: '#00838F' },
  { value: 'SCENARIO_LAW_REGULATE', label: '依法调解', color: '#5D4037' },
  { value: 'SCENARIO_FEEDBACK', label: '反馈处置', color: '#AD1457' },
  { value: 'SCENARIO_GOVERNANCE', label: '综合治理', color: '#E65100' },
  { value: 'SCENARIO_PREVENTION', label: '预防预警', color: '#37474F' },
]

/** 业务流程类型选项（用于前端下拉） */
export const PROCESS_TYPE_OPTIONS = [
  { value: 'business_process', label: '业务流程' },
  { value: 'lifecycle', label: '生命周期' },
  { value: 'workflow', label: '工作流' },
  { value: 'mediation', label: '调解' },
  { value: 'arbitration', label: '仲裁' },
  { value: 'execution', label: '执行' },
]
