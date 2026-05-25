import request from './request'

// Prompt template types
export interface PromptTemplate {
  id: number
  code: string
  name: string
  description?: string
  type: string
  systemPrompt: string
  userPromptTemplate: string
  responseFormat?: string
  enabled: boolean
  model?: string
  sort: number
  tags?: string[]
  extraConfig?: string
  variables?: PromptVariable[]
  createdAt?: string
  updatedAt?: string
}

export interface PromptVariable {
  id: number
  templateId: number
  variableName: string
  description?: string
  variableType: string
  required: boolean
  defaultValue?: string
  source: string
  validationRule?: string
  sort: number
  remark?: string
}

export interface PromptVersion {
  id: number
  templateId: number
  version: number
  systemPrompt: string
  userPromptTemplate: string
  responseFormat?: string
  description?: string
  active: boolean
  createdAt?: string
}

export interface CreatePromptTemplateReq {
  code: string
  name: string
  description?: string
  type: string
  systemPrompt: string
  userPromptTemplate: string
  responseFormat?: string
  enabled?: boolean
  model?: string
  sort?: number
  tags?: string[]
  extraConfig?: string
  variables?: PromptVariable[]
}

export interface PromptTestReq {
  templateId: string
  inputContent: string
  contextContent?: string
  customVariables?: string
  sourceType?: string
  model?: string
  temperature?: number
  maxTokens?: number
}

export interface PromptTestResp {
  success: boolean
  rawResponse?: string
  parsedData?: string
  entityCount?: number
  edgeCount?: number
  errorMessage?: string
  tokenUsage?: {
    inputTokens?: number
    outputTokens?: number
    totalTokens?: number
  }
  elapsedMs?: number
  responseTime?: string
}

export interface GenerateSampleReq {
  templateId: string
  dataType: string
  count?: number
  scenario?: string
  format?: string
  additionalInstructions?: string
}

export interface SampleData {
  index: number
  content: string
  type: string
  domain?: string
  metadata?: string
}

export interface GenerateSampleResp {
  success: boolean
  samples?: SampleData[]
  errorMessage?: string
}

export const promptApi = {
  // ========== Template Management ==========

  /**
   * Get all templates
   * GET /prompt/templates
   */
  async list(): Promise<PromptTemplate[]> {
    return request.get('/prompt/templates')
  },

  /**
   * Get template by ID
   * GET /prompt/templates/{id}
   */
  async getById(id: number): Promise<PromptTemplate> {
    return request.get(`/prompt/templates/${id}`)
  },

  /**
   * Get template by code
   * GET /prompt/templates/code/{code}
   */
  async getByCode(code: string): Promise<PromptTemplate> {
    return request.get(`/prompt/templates/code/${code}`)
  },

  /**
   * Get templates by type
   * GET /prompt/templates/type/{type}
   */
  async listByType(type: string): Promise<PromptTemplate[]> {
    return request.get(`/prompt/templates/type/${type}`)
  },

  /**
   * Create template
   * POST /prompt/templates
   */
  async create(data: CreatePromptTemplateReq): Promise<PromptTemplate> {
    return request.post('/prompt/templates', data)
  },

  /**
   * Update template
   * PUT /prompt/templates/{id}
   */
  async update(id: number, data: CreatePromptTemplateReq): Promise<PromptTemplate> {
    return request.put(`/prompt/templates/${id}`, data)
  },

  /**
   * Delete template
   * DELETE /prompt/templates/{id}
   */
  async delete(id: number): Promise<void> {
    return request.delete(`/prompt/templates/${id}`)
  },

  /**
   * Toggle template enabled status
   * PUT /prompt/templates/{id}/toggle?enabled=
   */
  async toggle(id: number, enabled: boolean): Promise<void> {
    return request.put(`/prompt/templates/${id}/toggle`, null, {
      params: { enabled }
    })
  },

  // ========== Version Management ==========

  /**
   * Get version history
   * GET /prompt/templates/{id}/versions
   */
  async getVersionHistory(id: number): Promise<PromptVersion[]> {
    return request.get(`/prompt/templates/${id}/versions`)
  },

  /**
   * Create new version
   * POST /prompt/templates/{id}/versions
   */
  async createVersion(id: number, description?: string): Promise<void> {
    return request.post(`/prompt/templates/${id}/versions`, null, {
      params: { description }
    })
  },

  /**
   * Rollback to version
   * POST /prompt/templates/{id}/rollback?version=
   */
  async rollback(id: number, version: number): Promise<void> {
    return request.post(`/prompt/templates/${id}/rollback`, null, {
      params: { version }
    })
  },

  // ========== Render & Preview ==========

  /**
   * Render prompt with variables
   * POST /prompt/templates/{id}/render
   */
  async render(id: number, variables: Record<string, any>): Promise<{
    systemPrompt: string
    userPrompt: string
    responseFormat: string
  }> {
    return request.post(`/prompt/templates/${id}/render`, variables)
  },

  // ========== Template Types ==========

  /**
   * Get template type list
   * GET /prompt/types
   */
  async getTypes(): Promise<Array<{ value: string; label: string }>> {
    return request.get('/prompt/types')
  },

  // ========== Testing ==========

  /**
   * Execute prompt test (render only)
   * POST /prompt/test/execute
   */
  async testExecute(data: PromptTestReq): Promise<PromptTestResp> {
    return request.post('/prompt/test/execute', data)
  },

  /**
   * Execute prompt test with extraction
   * POST /prompt/test/extract
   */
  async testExtract(data: PromptTestReq): Promise<PromptTestResp> {
    return request.post('/prompt/test/extract', data)
  },

  /**
   * Generate sample data
   * POST /prompt/test/generate-sample
   */
  async generateSample(data: GenerateSampleReq): Promise<GenerateSampleResp> {
    return request.post('/prompt/test/generate-sample', data)
  }
}

export default promptApi
