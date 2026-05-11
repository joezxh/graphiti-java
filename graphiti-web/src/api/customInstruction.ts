import request from './request'

// Custom instruction item
export interface CustomInstruction {
  id: string
  instruction: string
  graphId?: string
  userId?: string
  createdAt?: string
}

// Create custom instruction request
export interface CreateCustomInstructionReq {
  instruction: string
  graphId?: string
  userId?: string
}

export const customInstructionApi = {
  /**
   * 获取自定义指令列表
   * 后端: GET /custom-instructions?graphId=
   */
  list(graphId?: string): Promise<CustomInstruction[]> {
    return request.get('/custom-instructions', {
      params: { graphId: graphId || undefined }
    })
  },

  /**
   * 创建自定义指令
   * 后端: POST /custom-instructions
   */
  create(data: CreateCustomInstructionReq): Promise<CustomInstruction> {
    return request.post('/custom-instructions', data)
  },

  /**
   * 删除自定义指令
   * 后端: DELETE /custom-instructions/{id}
   */
  delete(id: string): Promise<void> {
    return request.delete(`/custom-instructions/${id}`)
  }
}

export default customInstructionApi
