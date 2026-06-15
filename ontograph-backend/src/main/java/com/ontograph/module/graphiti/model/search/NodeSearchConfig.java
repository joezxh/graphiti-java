package com.ontograph.module.graphiti.model.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 节点搜索配置
 *
 * <p>参考 Python 实现：graphiti_core/search/search_config_recipes.py:NodeSearchConfig
 */
@Data
@Schema(description = "节点搜索配置")
public class NodeSearchConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索方法列表")
    private java.util.List<NodeSearchMethod> searchMethods =
            java.util.List.of(NodeSearchMethod.bm25, NodeSearchMethod.cosine_similarity);

    @Schema(description = "重排策略")
    private RerankerType reranker = RerankerType.rrf;

    @Schema(description = "向量相似度最小分数阈值", example = "0.6")
    private double simMinScore = 0.6;

    @Schema(description = "MMR 多样性参数", example = "0.5")
    private double mmrLambda = 0.5;

    @Schema(description = "BFS 最大深度", example = "2")
    private int bfsMaxDepth = 2;

    // ==================== 预定义配置 ====================

    public static NodeSearchConfig hybridRrf() {
        NodeSearchConfig config = new NodeSearchConfig();
        config.setSearchMethods(java.util.List.of(
                NodeSearchMethod.bm25,
                NodeSearchMethod.cosine_similarity));
        config.setReranker(RerankerType.rrf);
        return config;
    }

    public static NodeSearchConfig hybridCrossEncoder() {
        NodeSearchConfig config = new NodeSearchConfig();
        config.setSearchMethods(java.util.List.of(
                NodeSearchMethod.bm25,
                NodeSearchMethod.cosine_similarity,
                NodeSearchMethod.bfs));
        config.setReranker(RerankerType.cross_encoder);
        return config;
    }
}
