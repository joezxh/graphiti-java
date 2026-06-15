package com.ontograph.module.graphiti.model.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 社区节点搜索配置
 *
 * <p>参考 Python 实现：graphiti_core/search/search_config_recipes.py:CommunitySearchConfig
 */
@Data
@Schema(description = "社区节点搜索配置")
public class CommunitySearchConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索方法列表")
    private java.util.List<CommunitySearchMethod> searchMethods =
            java.util.List.of(CommunitySearchMethod.bm25, CommunitySearchMethod.cosine_similarity);

    @Schema(description = "重排策略")
    private RerankerType reranker = RerankerType.rrf;

    @Schema(description = "向量相似度最小分数阈值", example = "0.6")
    private double simMinScore = 0.6;

    @Schema(description = "MMR 多样性参数", example = "0.5")
    private double mmrLambda = 0.5;

    public static CommunitySearchConfig hybridRrf() {
        CommunitySearchConfig config = new CommunitySearchConfig();
        config.setSearchMethods(java.util.List.of(
                CommunitySearchMethod.bm25,
                CommunitySearchMethod.cosine_similarity));
        config.setReranker(RerankerType.rrf);
        return config;
    }

    public static CommunitySearchConfig crossEncoder() {
        CommunitySearchConfig config = new CommunitySearchConfig();
        config.setSearchMethods(java.util.List.of(
                CommunitySearchMethod.bm25,
                CommunitySearchMethod.cosine_similarity));
        config.setReranker(RerankerType.cross_encoder);
        return config;
    }
}
