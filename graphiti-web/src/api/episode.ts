import request from './request'

// Episode list item
export interface EpisodeListItem {
  uuid: string
  name: string
  groupId: string
  source: string
  sourceDescription?: string
  content?: string
  createdAt?: string
  validAt?: string
  entityEdges?: string[]
}

// Episode detail response
export interface EpisodeDetailResp extends EpisodeListItem {
  properties?: Record<string, any>
}

// Episode mentions response
export interface EpisodeMentions {
  nodes: Array<{
    uuid: string
    name: string
    type: string | null
  }>
  edges: Array<{
    uuid: string
    fact: string
    sourceNodeUuid: string
    targetNodeUuid: string
  }>
}

export const episodeApi = {
  /**
   * 获取 Episode 列表
   * 后端: GET /graph/episode/list/{graphId}?skip=&limit=
   */
  async list(graphId: string, skip?: number, limit?: number): Promise<EpisodeListItem[]> {
    const data = await request.get<{ episodes: EpisodeListItem[]; totalCount: number; rowCount: number }>(
      `/graph/episode/list/${graphId}`,
      { params: { skip: skip || 0, limit: limit || 20 } }
    )
    return data?.episodes ?? []
  },

  /**
   * 获取 Episode 详情
   * 后端: GET /graph/episode/{graphId}/{episodeUuid}
   */
  async get(graphId: string, episodeUuid: string): Promise<EpisodeDetailResp> {
    return request.get(`/graph/episode/${graphId}/${episodeUuid}`)
  },

  /**
   * 获取 Episode 提及的节点和边
   * 后端: GET /graph/episode/{graphId}/{episodeUuid}/mentions
   */
  async getMentions(graphId: string, episodeUuid: string): Promise<EpisodeMentions> {
    return request.get(`/graph/episode/${graphId}/${episodeUuid}/mentions`)
  },

  /**
   * 创建 Episode
   * 后端: POST /graph/episode/{graphId}
   */
  async create(graphId: string, data: {
    name?: string
    source: string
    sourceDescription?: string
    content?: string
    createdAt?: string
  }): Promise<EpisodeDetailResp> {
    return request.post(`/graph/episode/${graphId}`, data)
  },

  /**
   * 删除 Episode
   * 后端: DELETE /graph/episode/{graphId}/{episodeUuid}
   */
  async delete(graphId: string, episodeUuid: string): Promise<void> {
    return request.delete(`/graph/episode/${graphId}/${episodeUuid}`)
  }
}

export default episodeApi
