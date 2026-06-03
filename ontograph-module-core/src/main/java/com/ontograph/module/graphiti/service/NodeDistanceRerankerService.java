package com.ontograph.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * Node Distance 重排服务
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py:node_distance_reranker()
 *
 * <p>基于图中最短路径距离对结果重排，距离越近分数越高。
 * 分数公式：score = 1 / (distance + 1)
 */
public interface NodeDistanceRerankerService {

    /**
     * 按节点距离重排边
     *
     * @param candidateUuids 候选边 UUID 列表
     * @param centerNodeUuid 中心节点 UUID
     * @param uuidSourceMap 边 UUID -> 源节点 UUID 的映射
     * @param limit 返回数量
     * @return 按距离分数降序排列的 UUID 列表
     */
    List<String> rerankEdgesByDistance(
            List<String> candidateUuids,
            String centerNodeUuid,
            Map<String, String> uuidSourceMap,
            int limit);

    /**
     * 按节点距离重排节点
     *
     * @param candidateUuids 候选节点 UUID 列表
     * @param centerNodeUuid 中心节点 UUID
     * @param limit 返回数量
     * @return 按距离分数降序排列的 UUID 列表
     */
    List<String> rerankNodesByDistance(
            List<String> candidateUuids,
            String centerNodeUuid,
            int limit);
}
