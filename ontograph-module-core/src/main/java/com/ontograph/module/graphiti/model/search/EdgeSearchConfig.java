package com.ontograph.module.graphiti.model.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 边搜索配置
 *
 * <p>参考 Python 实现：graphiti_core/search/search_config_recipes.py:EdgeSearchConfig
 */
@Data
@Schema(description = "边搜索配置")
public class EdgeSearchConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索方法列表", example = "[BM25, VECTOR]")
    private java.util.List<EdgeSearchMethod> searchMethods =
            java.util.List.of(EdgeSearchMethod.bm25, EdgeSearchMethod.cosine_similarity);

    @Schema(description = "重排策略", example = "RRF")
    private RerankerType reranker = RerankerType.rrf;

    @Schema(description = "向量相似度最小分数阈值", example = "0.6")
    private double simMinScore = 0.6;

    @Schema(description = "MMR 多样性参数（0=纯多样性, 1=纯相关性）", example = "0.5")
    private double mmrLambda = 0.5;

    @Schema(description = "BFS 最大深度", example = "2")
    private int bfsMaxDepth = 2;

    // ==================== 预定义配置 ====================

    public static EdgeSearchConfig hybridRrf() {
        EdgeSearchConfig config = new EdgeSearchConfig();
        config.setSearchMethods(java.util.List.of(
                EdgeSearchMethod.bm25,
                EdgeSearchMethod.cosine_similarity));
        config.setReranker(RerankerType.rrf);
        return config;
    }

    public static EdgeSearchConfig hybridMmr() {
        EdgeSearchConfig config = new EdgeSearchConfig();
        config.setSearchMethods(java.util.List.of(
                EdgeSearchMethod.bm25,
                EdgeSearchMethod.cosine_similarity));
        config.setReranker(RerankerType.mmr);
        config.setMmrLambda(0.5);
        return config;
    }

    public static EdgeSearchConfig hybridCrossEncoder() {
        EdgeSearchConfig config = new EdgeSearchConfig();
        config.setSearchMethods(java.util.List.of(
                EdgeSearchMethod.bm25,
                EdgeSearchMethod.cosine_similarity,
                EdgeSearchMethod.bfs));
        config.setReranker(RerankerType.cross_encoder);
        config.setMmrLambda(1.0);
        return config;
    }
}
