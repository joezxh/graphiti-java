/**
 * 法律知识图谱批量导入 API
 * Legal Knowledge Graph Batch Import API
 */

import request from '@/api/request';
import type { AxiosPromise } from 'axios';
import { LEGAL_NODES, LEGAL_EDGES, LEGAL_GRAPH_ID } from './legal-kg-data';

const BASE_URL = '/api/v1/graph';

export interface LegalImportNodeVO {
  name: string;
  type: string;
  summary?: string;
  properties: Record<string, any>;
}

export interface LegalImportEdgeVO {
  sourceName: string;
  targetName: string;
  type: string;
  properties: Record<string, any>;
}

export interface LegalImportReqVO {
  graphId: string;
  nodes: LegalImportNodeVO[];
  edges: LegalImportEdgeVO[];
}

export interface LegalImportRespVO {
  graphId: string;
  nodeCount: number;
  edgeCount: number;
  nodeErrors: string[];
  edgeErrors: string[];
}

/**
 * 批量导入法律图谱数据（一次性导入所有节点和边）
 * POST /api/v1/graph/legal/import
 */
export function importLegalKG(data: LegalImportReqVO): AxiosPromise<LegalImportRespVO> {
  return request.post(`${BASE_URL}/legal/import`, data, {
    headers: { 'Content-Type': 'application/json' }
  });
}

/**
 * 导入法律节点
 * POST /api/v1/graph/legal/nodes
 */
export function importLegalNodes(graphId: string, nodes: LegalImportNodeVO[]): AxiosPromise<{ successCount: number; errors: string[] }> {
  return request.post(`${BASE_URL}/legal/nodes`, { graphId, nodes }, {
    headers: { 'Content-Type': 'application/json' }
  });
}

/**
 * 导入法律边
 * POST /api/v1/graph/legal/edges
 */
export function importLegalEdges(graphId: string, edges: LegalImportEdgeVO[]): AxiosPromise<{ successCount: number; errors: string[] }> {
  return request.post(`${BASE_URL}/legal/edges`, { graphId, edges }, {
    headers: { 'Content-Type': 'application/json' }
  });
}

/**
 * 导入商事调解条例法条节点
 * POST /api/v1/graph/legal/provisions
 */
export function importLegalProvisions(graphId: string): AxiosPromise<{ successCount: number }> {
  const provisionNodes = LEGAL_NODES.filter(n => n.type === 'LegalProvision')
    .map(n => ({ name: n.name, type: n.type, summary: n.summary, properties: n.properties }));
  return importLegalNodes(graphId, provisionNodes);
}

/**
 * 快速导入完整法律知识图谱数据
 * 自动包含所有预定义的节点和边
 */
export function importFullLegalKG(): AxiosPromise<LegalImportRespVO> {
  const nodes: LegalImportNodeVO[] = LEGAL_NODES.map(n => ({
    name: n.name,
    type: n.type,
    summary: n.summary,
    properties: n.properties
  }));

  return importLegalKG({
    graphId: LEGAL_GRAPH_ID,
    nodes,
    edges: []  // 边需要通过节点名称查找 UUID，API 侧处理
  });
}

/**
 * 设置法律本体定义
 * POST /api/v1/graph/ontology
 */
export function setLegalOntology(graphId: string, entities: any[], edges: any[]): AxiosPromise<void> {
  return request.post(`${BASE_URL}/ontology`, {
    graphId,
    entities: JSON.stringify(entities),
    edges: JSON.stringify(edges)
  }, {
    headers: { 'Content-Type': 'application/json' }
  });
}

/**
 * 创建法律图谱（包含本体定义）
 */
export async function createLegalGraph(): Promise<string> {
  const { default: graphApi } = await import('./graph');

  // 1. 创建图谱
  const graphId = LEGAL_GRAPH_ID;
  try {
    await graphApi.createGraph({
      graphId,
      name: '法律知识图谱',
      description: '基于典型案例、商事调解条例的法律领域知识图谱'
    });
  } catch (e: any) {
    // 图谱可能已存在，忽略
    console.warn('Graph may already exist:', e?.message);
  }

  // 2. 设置本体
  const { LEGAL_ENTITIES, LEGAL_EDGES } = await import('./legal-kg-data');
  await setLegalOntology(graphId, LEGAL_ENTITIES, LEGAL_EDGES);

  return graphId;
}
