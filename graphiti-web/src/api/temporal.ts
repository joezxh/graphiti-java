import request from './request'

// Temporal fact (a fact valid at a specific time)
export interface TemporalFact {
  sourceNode: {
    uuid: string
    name: string
    type: string | null
    label: string
    properties?: Record<string, any>
  }
  targetNode: {
    uuid: string
    name: string
    type: string | null
    label: string
    properties?: Record<string, any>
  }
  edge: {
    uuid: string
    name: string | null
    fact: string
    sourceNodeUuid: string
    targetNodeUuid: string
    createdAt?: string
    validAt?: string
    invalidAt?: string
  }
  validAt: string
  invalidAt?: string
}

// Entity version history item
export interface EntityHistoryItem {
  uuid: string
  name: string
  type: string | null
  label: string
  properties: Record<string, any>
  validAt?: string
  invalidAt?: string
  createdAt?: string
}

// Graph state at a given time
export interface GraphStateAtTime {
  nodes: Array<{
    uuid: string
    name: string
    type: string | null
    label: string
    properties?: Record<string, any>
  }>
  edges: Array<{
    uuid: string
    name: string | null
    fact: string
    sourceNodeUuid: string
    targetNodeUuid: string
  }>
}

export const temporalApi = {
  /**
   * 获取当前有效的所有事实
   * 后端: GET /graph/{graphId}/temporal/facts/current
   */
  getCurrentFacts(graphId: string): Promise<TemporalFact[]> {
    return request.get(`/graph/${graphId}/temporal/facts/current`)
  },

  /**
   * 获取指定时间点有效的所有事实
   * 后端: GET /graph/{graphId}/temporal/facts/at/{referenceTime}
   * @param referenceTime Unix timestamp in milliseconds
   */
  getFactsAt(graphId: string, referenceTime: number): Promise<TemporalFact[]> {
    return request.get(`/graph/${graphId}/temporal/facts/at/${referenceTime}`)
  },

  /**
   * 获取指定时间点的关系图谱
   * 后端: GET /graph/{graphId}/temporal/relationships/at/{referenceTime}
   */
  getRelationshipsAt(graphId: string, referenceTime: number): Promise<GraphStateAtTime> {
    return request.get(`/graph/${graphId}/temporal/relationships/at/${referenceTime}`)
  },

  /**
   * 获取实体的版本历史
   * 后端: GET /graph/{graphId}/temporal/history/{entityName}
   */
  getEntityHistory(graphId: string, entityName: string): Promise<EntityHistoryItem[]> {
    return request.get(`/graph/${graphId}/temporal/history/${encodeURIComponent(entityName)}`)
  },

  /**
   * 批量使事实失效
   * 后端: POST /graph/{graphId}/temporal/facts/invalidate
   */
  invalidateFacts(graphId: string, edgeUuids: string[]): Promise<void> {
    return request.post(`/graph/${graphId}/temporal/facts/invalidate`, { uuids: edgeUuids })
  },

  /**
   * 获取图谱在指定时间的状态
   * 后端: GET /graph/{graphId}/history?time={timestamp}
   */
  getGraphStateAt(graphId: string, time: number): Promise<GraphStateAtTime> {
    return request.get(`/graph/${graphId}/history`, {
      params: { time }
    })
  }
}

export default temporalApi
