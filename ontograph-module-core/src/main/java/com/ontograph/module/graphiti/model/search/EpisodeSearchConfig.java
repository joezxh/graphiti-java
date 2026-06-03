package com.ontograph.module.graphiti.model.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * Episode 搜索配置
 *
 * <p>参考 Python 实现：graphiti_core/search/search_config_recipes.py:EpisodeSearchConfig
 * <p>Note: Episode 仅支持 BM25 全文搜索，不涉及向量操作
 */
@Data
@Schema(description = "Episode 搜索配置")
public class EpisodeSearchConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索方法列表（仅支持 BM25）")
    private java.util.List<EpisodeSearchMethod> searchMethods =
            java.util.List.of(EpisodeSearchMethod.bm25);

    @Schema(description = "重排策略")
    private RerankerType reranker = RerankerType.rrf;

    @Schema(description = "向量相似度最小分数阈值（Episode 不使用，此处占位）", example = "0.6")
    private double simMinScore = 0.6;

    @Schema(description = "MMR 多样性参数", example = "0.5")
    private double mmrLambda = 0.5;

    @Schema(description = "BFS 最大深度", example = "2")
    private int bfsMaxDepth = 2;

    public static EpisodeSearchConfig bm25Only() {
        EpisodeSearchConfig config = new EpisodeSearchConfig();
        config.setSearchMethods(java.util.List.of(EpisodeSearchMethod.bm25));
        config.setReranker(RerankerType.rrf);
        return config;
    }

    public static EpisodeSearchConfig crossEncoder() {
        EpisodeSearchConfig config = new EpisodeSearchConfig();
        config.setSearchMethods(java.util.List.of(EpisodeSearchMethod.bm25));
        config.setReranker(RerankerType.cross_encoder);
        return config;
    }
}
