/**
 * 法律知识图谱 LLM 提取 API
 * Legal Knowledge Graph LLM Extraction API
 */

import request from '@/api/request'
// import type { AxiosPromise } from 'axios' // 未使用

const BASE_URL = '/api/v1/graph/legal/extract'

/** JSON 预览响应 */
export interface JsonPreviewResp {
  fileName: string
  fileSize: number
  fieldCount: number
  fieldTree: Record<string, any>
  sampleData: Record<string, any>
  contentPreview: string
}

/** 字段映射项 */
export interface FieldMappingItem {
  jsonPath: string
  ontField: string
  ontEntity: string
}

/** 提取结果 - 案件 */
export interface ExtractedCaseVO {
  caseName?: string
  caseNumber?: string
  caseType?: string
  caseStatus?: string
  filingDate?: string
  closedDate?: string
  amountInDispute?: number
  summary?: string
  description?: string
  uuid?: string
}

/** 提取结果 - 当事人 */
export interface ExtractedPartyVO {
  name?: string
  partyType?: string
  idNumber?: string
  role?: string
  address?: string
  contact?: string
  isEnterprise?: boolean
  uuid?: string
}

/** 提取结果 - 法院 */
export interface ExtractedCourtVO {
  name?: string
  level?: string
  location?: string
  jurisdiction?: string
  parentCourt?: string
  uuid?: string
}

/** 提取结果 - 法官 */
export interface ExtractedJudgeVO {
  name?: string
  title?: string
  courtName?: string
  specialty?: string
  uuid?: string
}

/** 提取结果 - 法律条文 */
export interface ExtractedProvisionVO {
  provisionId?: string
  articleNumber?: string
  content?: string
  lawName?: string
  lawType?: string
  effectiveDate?: string
  keywords?: string
  uuid?: string
}

/** 提取结果 - 律师 */
export interface ExtractedLawyerVO {
  name?: string
  licenseNumber?: string
  firmName?: string
  specialty?: string
  contact?: string
  uuid?: string
}

/** 提取结果 - 证据 */
export interface ExtractedEvidenceVO {
  evidenceNumber?: string
  evidenceType?: string
  content?: string
  submittedBy?: string
  submissionDate?: string
  purpose?: string
  uuid?: string
}

/** 提取结果 - 裁判文书 */
export interface ExtractedJudgmentVO {
  documentNumber?: string
  documentType?: string
  issueDate?: string
  mainContent?: string
  judgmentResult?: string
  legalBasis?: string
  uuid?: string
}

/** LLM 提取结果 */
export interface LegalExtractResultVO {
  cases?: ExtractedCaseVO[]
  parties?: ExtractedPartyVO[]
  courts?: ExtractedCourtVO[]
  judges?: ExtractedJudgeVO[]
  provisions?: ExtractedProvisionVO[]
  lawyers?: ExtractedLawyerVO[]
  evidences?: ExtractedEvidenceVO[]
  judgments?: ExtractedJudgmentVO[]
  errors?: string[]
  sourceFileName?: string
  totalNodes?: number
  totalEdges?: number
}

/** 本体字段定义 */
export interface OntologyFieldsResp {
  [entityType: string]: {
    displayName: string
    fields: Record<string, string>
  }
}

/**
 * 预览 JSON 文件字段结构
 * POST /api/v1/graph/legal/extract/preview
 */
export async function previewJsonFile(file: File): Promise<JsonPreviewResp> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<JsonPreviewResp>(`${BASE_URL}/preview`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 提取法律知识图谱（仅提取，不保存）
 * POST /api/v1/graph/legal/extract
 */
export async function extractLegalKG(
  file: File,
  graphId: string,
  fieldMapping: Record<string, string>
): Promise<LegalExtractResultVO> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('graphId', graphId)
  formData.append('fieldMapping', JSON.stringify(fieldMapping))
  return request.post<LegalExtractResultVO>(BASE_URL, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 提取并保存法律知识图谱
 * POST /api/v1/graph/legal/extract/save
 */
export async function extractAndSaveLegalKG(
  file: File,
  graphId: string,
  fieldMapping: Record<string, string>
): Promise<LegalExtractResultVO> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('graphId', graphId)
  formData.append('fieldMapping', JSON.stringify(fieldMapping))
  return request.post<LegalExtractResultVO>(`${BASE_URL}/save`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/**
 * 获取本体字段列表
 * GET /api/v1/graph/legal/extract/ontology-fields
 */
export async function getOntologyFields(): Promise<OntologyFieldsResp> {
  return request.get<OntologyFieldsResp>(`${BASE_URL}/ontology-fields`)
}
