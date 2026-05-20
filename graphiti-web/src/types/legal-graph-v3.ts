/**
 * 法律知识图谱 V3.0.0 TypeScript 类型定义
 * 包含 Community、Episode、Entity、Relationship 的 V3 扩展类型
 */

// ========== Community V3 ==========

/** 社区节点 (V3.0.0) */
export interface CommunityV3 {
  uuid: string
  name: string
  communityType?: string
  legalDomain?: string
  jurisdiction?: string
  practiceType?: string
  parentCommunityUuid?: string
  summary?: string
  memberCount?: number
  keyProvisions?: string[]
  description?: string
  metadata?: CommunityMetadata
  createdAt?: string
  updatedAt?: string
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
  category: 'domain' | 'jurisdiction' | 'practice'
  parentTypeCode?: string
  sortOrder: number
  metadata: CommunityMetadata
  description?: string
}

/** 社区树节点（用于 a-tree 组件） */
export interface CommunityTreeNode {
  key: string
  title: string
  icon?: string
  color?: string
  legalDomain?: string
  jurisdiction?: string
  practiceType?: string
  isLeaf?: boolean
  children?: CommunityTreeNode[]
}

// ========== Episode V3 ==========

/** Episode 节点 (V3.0.0) */
export interface EpisodeV3 {
  uuid: string
  name: string
  episodeType?: string
  legalProcess?: string
  stageLabel?: string
  courtLevel?: string | null
  isTrialStage?: boolean
  caseId?: string
  startTime?: string
  endTime?: string
  content?: string
  source?: string
  sourceDescription?: string
  createdAt?: string
  groupId?: string
}

/** Episode 类型元数据（来自 ont_episode_type 表） */
export interface EpisodeTypeMeta {
  typeCode: string
  legalProcess?: string
  stageLabel?: string
  courtLevel?: string | null
  isTrialStage?: boolean
  count: number
}

/** 法律程序分组 */
export type LegalProcessGroup = 'litigation' | 'mediation' | 'arbitration' | 'execution'

export const LEGAL_PROCESS_LABELS: Record<LegalProcessGroup, string> = {
  litigation: '诉讼程序',
  mediation: '调解程序',
  arbitration: '仲裁程序',
  execution: '执行程序',
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

/** 法律领域色彩映射 */
export const LEGAL_DOMAIN_COLORS: Record<string, string> = {
  DOMAIN_CIVIL: '#2E7D32',
  DOMAIN_CRIMINAL: '#C62828',
  DOMAIN_ADMIN: '#6A1B9A',
  DOMAIN_IP: '#F57F17',
  DOMAIN_LABOR: '#00838F',
  DOMAIN_MEDIATION: '#AD1457',
  DOMAIN_EXECUTION: '#37474F',
  DOMAIN_ROOT: '#1565C0',
}

/** 司法管辖区色彩映射 */
export const JURISDICTION_COLORS: Record<string, string> = {
  JURISDICTION_CN: '#C62828',
  JURISDICTION_INTERNATIONAL: '#0277BD',
}

/** 应用场景色彩映射 */
export const PRACTICE_COLORS: Record<string, string> = {
  PRACTICE_JUDICIAL: '#1565C0',
  PRACTICE_ARBITRATION: '#0288D1',
  PRACTICE_MEDIATION: '#AD1457',
  PRACTICE_COMPLIANCE: '#558B2F',
}

/** Episode 类型色彩映射 */
export const EPISODE_TYPE_COLORS: Record<string, string> = {
  EP_FILING: '#42A5F5',
  EP_SERVING: '#64B5F6',
  EP_TRIAL_1ST: '#2E7D32',
  EP_JUDGMENT_1ST: '#1B5E20',
  EP_APPEAL: '#FFA726',
  EP_TRIAL_2ND: '#388E3C',
  EP_JUDGMENT_2ND: '#1B5E20',
  EP_RETRIAL: '#D32F2F',
  EP_EXECUTION: '#37474F',
  EP_MEDIATION_ACCEPT: '#EC407A',
  EP_MEDIATION_NEGOTIATION: '#F48FB1',
  EP_MEDIATION_AGREEMENT: '#CE93D8',
  EP_MEDIATION_CONFIRM: '#AB47BC',
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
