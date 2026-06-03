package com.ontograph.module.graphiti.service;

/**
 * SearchPipelineService 接口
 *
 * <p>搜索 Pipeline 核心编排服务
 *
 * <p>参考 Python 实现：graphiti_core/search/search.py:search()
 *
 * <p>职责：
 * <ul>
 *   <li>条件触发查询向量生成</li>
 *   <li>4 个 Scope 并行执行（CompletableFuture）</li>
 *   <li>各 Scope 内部方法并行执行</li>
 *   <li>Reranker 应用</li>
 *   <li>结果聚合</li>
 * </ul>
 */
public interface SearchPipelineService {

    /**
     * 搜索 Pipeline
     *
     * @param query 查询文本
     * @param graphId 图谱 ID
     * @param config 搜索配置
     * @param filters 过滤器
     * @param centerNodeUuid 中心节点 UUID（可选，用于 NodeDistance 重排）
     * @param bfsOriginUuids BFS 起始节点 UUID 列表（可选）
     * @return 搜索结果
     */
    com.ontograph.module.graphiti.model.search.SearchResults search(
            String query,
            String graphId,
            com.ontograph.module.graphiti.model.search.SearchConfig config,
            com.ontograph.module.graphiti.model.search.SearchFilters filters,
            String centerNodeUuid,
            java.util.List<String> bfsOriginUuids);

    /**
     * 对已有候选项进行重排
     *
     * <p>参考 Python：server/graph_service/routers/graph.py 的 /rerank 端点
     *
     * @param query 查询文本
     * @param reranker 重排策略（rrf, mmr, cross_encoder, node_distance, episode_mentions）
     * @param mmrLambda MMR lambda 参数
     * @param centerNodeUuid 中心节点 UUID（用于 NodeDistance）
     * @param limit 返回数量
     * @param edges 边候选项
     * @param nodes 节点候选项
     * @return 重排后的搜索结果
     */
    com.ontograph.module.graphiti.model.search.SearchResults rerank(
            String query,
            String reranker,
            Double mmrLambda,
            String centerNodeUuid,
            Integer limit,
            java.util.List<? extends com.ontograph.module.graphiti.model.search.SearchResults.EdgeResult> edges,
            java.util.List<? extends com.ontograph.module.graphiti.model.search.SearchResults.NodeResult> nodes);
}
