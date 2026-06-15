import request from './request'

// Edge filter parameters
export interface EdgeFilter {
  graphId?: string
  edgeTypes?: string[]
  sourceNodeTypes?: string[]
  targetNodeTypes?: string[]
  skip?: number
  limit?: number
}

// Edge creation parameters
export interface CreateEdgeReq {
  sourceNodeUuid: string
  targetNodeUuid: string
  type?: string
  name?: string
  fact?: string
  properties?: Record<string, any>
}

// Edge update parameters
export interface UpdateEdgeReq {
  name?: string
  fact?: string
  properties?: Record<string, any>
}

// Edge detail response
export interface EdgeDetailResp {
  uuid: string
  name: string | null
  fact: string
  sourceNodeUuid: string
  targetNodeUuid: string
  groupId: string
  createdAt?: string
  validAt?: string
  invalidAt?: string
  expiredAt?: string
  episodes?: string[]
  properties?: Record<string, any>
}

// Edge list item
export interface EdgeListItem {
  uuid: string
  name: string | null
  fact: string
  sourceNodeUuid: string
  targetNodeUuid: string
  groupId: string
  createdAt?: string
  episodes?: string[]
}

export const edgeApi = {
  /**
   * 获取边列表
   * 后端: POST /graph/edge/list/{graphId}
   */
  list(graphId: string, filter?: EdgeFilter): Promise<EdgeListItem[]> {
    return request.post(`/graph/edge/list/${graphId}`, filter || {})
  },

  /**
   * 获取边详情
   * 后端: GET /graph/edge/{graphId}/{edgeUuid}
   */
  get(graphId: string, edgeUuid: string): Promise<EdgeDetailResp> {
    return request.get(`/graph/edge/${graphId}/${edgeUuid}`)
  },

  /**
   * 创建边
   * 后端: POST /graph/edge/{graphId}
   */
  create(graphId: string, data: CreateEdgeReq): Promise<EdgeDetailResp> {
    return request.post(`/graph/edge/${graphId}`, data)
  },

  /**
   * 更新边
   * 后端: PUT /graph/edge/{graphId}/{edgeUuid}
   */
  update(graphId: string, edgeUuid: string, data: UpdateEdgeReq): Promise<EdgeDetailResp> {
    return request.put(`/graph/edge/${graphId}/${edgeUuid}`, data)
  },

  /**
   * 删除边
   * 后端: DELETE /graph/edge/{graphId}/{edgeUuid}
   */
  delete(graphId: string, edgeUuid: string): Promise<boolean> {
    return request.delete(`/graph/edge/${graphId}/${edgeUuid}`)
  },

  /**
   * 获取两个节点之间的所有边（双向）
   * 后端: GET /graph/edge/between/{sourceUuid}/{targetUuid}
   */
  between(sourceUuid: string, targetUuid: string): Promise<EdgeListItem[]> {
    return request.get(`/graph/edge/between/${sourceUuid}/${targetUuid}`)
  }
}

export default edgeApi
