/**
 * 本体元数据管理 API
 * 对应后端 OntMetadataController，管辖 ont_community_type、ont_episode_type、
 * ont_entity_category、ont_relationship_meta 四张元数据表
 */
import request from './request'

// ==================== Types ====================

export interface OntCommunityTypeVO {
  id: number
  definitionId: number
  typeCode: string
  typeName: string
  typeNameEn?: string
  category?: string
  description?: string
  parentTypeCode?: string
  sortOrder?: number
  metadata?: string
  status?: string
  createdAt?: string
  updatedAt?: string
}

export interface OntEpisodeTypeVO {
  id: number
  definitionId: number
  typeCode: string
  typeName: string
  typeNameEn?: string
  legalProcess?: string
  stageLabel?: string
  // 通用化字段
  processType?: string
  stageLevel?: string   // 替换 courtLevel
  isReviewStage?: boolean  // 替换 isTrialStage
  // 向后兼容旧字段
  courtLevel?: string
  isTrialStage?: boolean
  description?: string
  sortOrder?: number
  metadata?: string
  status?: string
  createdAt?: string
  updatedAt?: string
}

export interface OntEntityCategoryVO {
  id: number
  definitionId: number
  categoryCode: string
  categoryName: string
  categoryLevel: number
  parentCategoryCode?: string
  entityTypeScope?: string
  defaultAttributes?: string
  description?: string
  sortOrder?: number
  metadata?: string
  status?: string
  children?: OntEntityCategoryVO[]
  createdAt?: string
  updatedAt?: string
}

export interface OntRelationshipMetaVO {
  id: number
  definitionId: number
  relationshipType: string
  relationshipName: string
  relationshipNameEn?: string
  sourceEntityTypes?: string
  targetEntityTypes?: string
  isDirectional?: boolean
  isTransitive?: boolean
  multiplicity?: string
  defaultWeight?: number
  validityPeriod?: string
  description?: string
  exampleCypher?: string
  sortOrder?: number
  metadata?: string
  status?: string
  createdAt?: string
  updatedAt?: string
}

// ==================== Community Type API ====================

export const communityTypeApi = {
  list: (graphId: string, definitionId: number, category?: string) =>
    request.get<OntCommunityTypeVO[]>(`/ontology/${graphId}/community-types`, {
      params: { definitionId, ...(category ? { category } : {}) }
    }),

  getTree: (graphId: string, definitionId: number) =>
    request.get<OntCommunityTypeVO[]>(`/ontology/${graphId}/community-types/tree`, {
      params: { definitionId }
    }),

  get: (graphId: string, id: number) =>
    request.get<OntCommunityTypeVO>(`/ontology/${graphId}/community-types/${id}`),

  create: (graphId: string, data: Partial<OntCommunityTypeVO>) =>
    request.post<number>(`/ontology/${graphId}/community-types`, data),

  update: (graphId: string, id: number, data: Partial<OntCommunityTypeVO>) =>
    request.put(`/ontology/${graphId}/community-types/${id}`, data),

  delete: (graphId: string, id: number) =>
    request.delete(`/ontology/${graphId}/community-types/${id}`),
}

// ==================== Episode Type API ====================

export const episodeTypeApi = {
  list: (graphId: string, definitionId: number, legalProcess?: string) =>
    request.get<OntEpisodeTypeVO[]>(`/ontology/${graphId}/episode-types`, {
      params: { definitionId, ...(legalProcess ? { legalProcess } : {}) }
    }),

  get: (graphId: string, id: number) =>
    request.get<OntEpisodeTypeVO>(`/ontology/${graphId}/episode-types/${id}`),

  create: (graphId: string, data: Partial<OntEpisodeTypeVO>) =>
    request.post<number>(`/ontology/${graphId}/episode-types`, data),

  batchCreate: (graphId: string, definitionId: number, data: Partial<OntEpisodeTypeVO>[]) =>
    request.post<number>(`/ontology/${graphId}/episode-types/batch`, data, {
      params: { definitionId }
    }),

  update: (graphId: string, id: number, data: Partial<OntEpisodeTypeVO>) =>
    request.put(`/ontology/${graphId}/episode-types/${id}`, data),

  delete: (graphId: string, id: number) =>
    request.delete(`/ontology/${graphId}/episode-types/${id}`),
}

// ==================== Entity Category API ====================

export const entityCategoryApi = {
  list: (graphId: string, definitionId: number) =>
    request.get<OntEntityCategoryVO[]>(`/ontology/${graphId}/entity-categories`, {
      params: { definitionId }
    }),

  getTree: (graphId: string, definitionId: number) =>
    request.get<OntEntityCategoryVO[]>(`/ontology/${graphId}/entity-categories/tree`, {
      params: { definitionId }
    }),

  get: (graphId: string, id: number) =>
    request.get<OntEntityCategoryVO>(`/ontology/${graphId}/entity-categories/${id}`),

  create: (graphId: string, data: Partial<OntEntityCategoryVO>) =>
    request.post<number>(`/ontology/${graphId}/entity-categories`, data),

  update: (graphId: string, id: number, data: Partial<OntEntityCategoryVO>) =>
    request.put(`/ontology/${graphId}/entity-categories/${id}`, data),

  delete: (graphId: string, id: number) =>
    request.delete(`/ontology/${graphId}/entity-categories/${id}`),
}

// ==================== Relationship Meta API ====================

export const relationshipMetaApi = {
  list: (graphId: string, definitionId: number) =>
    request.get<OntRelationshipMetaVO[]>(`/ontology/${graphId}/relationship-meta`, {
      params: { definitionId }
    }),

  get: (graphId: string, id: number) =>
    request.get<OntRelationshipMetaVO>(`/ontology/${graphId}/relationship-meta/${id}`),

  create: (graphId: string, data: Partial<OntRelationshipMetaVO>) =>
    request.post<number>(`/ontology/${graphId}/relationship-meta`, data),

  batchCreate: (graphId: string, definitionId: number, data: Partial<OntRelationshipMetaVO>[]) =>
    request.post<number>(`/ontology/${graphId}/relationship-meta/batch`, data, {
      params: { definitionId }
    }),

  update: (graphId: string, id: number, data: Partial<OntRelationshipMetaVO>) =>
    request.put(`/ontology/${graphId}/relationship-meta/${id}`, data),

  delete: (graphId: string, id: number) =>
    request.delete(`/ontology/${graphId}/relationship-meta/${id}`),
}
