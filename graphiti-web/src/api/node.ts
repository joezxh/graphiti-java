import request from './request'

// Node filter parameters
export interface NodeFilter {
  graphId: string
  name?: string
  type?: string
  label?: string
  skip?: number
  limit?: number
}

// Node creation parameters
export interface CreateNodeReq {
  name: string
  type?: string
  properties?: Record<string, any>
}

// Node update parameters
export interface UpdateNodeReq {
  name?: string
  type?: string
  properties?: Record<string, any>
}

// Node detail response
export interface NodeDetailResp {
  uuid: string
  name: string
  type: string | null
  label: string
  properties: Record<string, any>
  createdAt?: string
  updatedAt?: string
}

// Node list item (matches BackendNode)
export interface NodeListItem {
  uuid: string
  name: string
  type: string | null
  label: string
  properties: Record<string, any>
  createdAt?: string
}

export const nodeApi = {
  /**
   * 获取节点列表
   * 后端: GET /nodes/list?graphId=&name=&type=&skip=&limit=
   */
  list(params: NodeFilter): Promise<NodeListItem[]> {
    return request.get('/nodes/list', {
      params: {
        graphId: params.graphId,
        name: params.name || undefined,
        type: params.type || undefined,
        skip: params.skip || 0,
        limit: params.limit || 20
      }
    })
  },

  /**
   * 获取节点详情
   * 后端: GET /nodes/{nodeUuid}?graphId=
   */
  get(graphId: string, nodeUuid: string): Promise<NodeDetailResp> {
    return request.get(`/nodes/${nodeUuid}`, {
      params: { graphId }
    })
  },

  /**
   * 创建节点
   * 后端: POST /nodes/create?graphId=
   */
  create(graphId: string, data: CreateNodeReq): Promise<NodeDetailResp> {
    return request.post('/nodes/create', data, {
      params: { graphId }
    })
  },

  /**
   * 更新节点
   * 后端: PUT /nodes/{nodeUuid}?graphId=
   */
  update(graphId: string, nodeUuid: string, data: UpdateNodeReq): Promise<NodeDetailResp> {
    return request.put(`/nodes/${nodeUuid}`, data, {
      params: { graphId }
    })
  },

  /**
   * 删除节点
   * 后端: DELETE /nodes/{nodeUuid}?graphId=
   */
  delete(graphId: string, nodeUuid: string): Promise<void> {
    return request.delete(`/nodes/${nodeUuid}`, {
      params: { graphId }
    })
  },

  /**
   * 获取节点关联的边
   * 后端: GET /nodes/{nodeUuid}/edges?graphId=&skip=&limit=
   */
  getEdges(graphId: string, nodeUuid: string, skip?: number, limit?: number): Promise<any[]> {
    return request.get(`/nodes/${nodeUuid}/edges`, {
      params: { graphId, skip: skip || 0, limit: limit || 50 }
    })
  },

  /**
   * 获取节点关联的 Episode
   * 后端: GET /nodes/{nodeUuid}/episodes?graphId=&skip=&limit=
   */
  getEpisodes(graphId: string, nodeUuid: string, skip?: number, limit?: number): Promise<any[]> {
    return request.get(`/nodes/${nodeUuid}/episodes`, {
      params: { graphId, skip: skip || 0, limit: limit || 50 }
    })
  }
}

export default nodeApi
