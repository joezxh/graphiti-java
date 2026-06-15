package com.ontograph.module.graphiti.model.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 搜索配置模型（对齐 Python graphiti 的 SearchConfig）
 *
 * <p>参考 Python 实现：graphiti_core/search/search_config_recipes.py
 *
 * <p>支持配置四种搜索 Scope（Edge/Node/Episode/Community），
 * 每个 Scope 可独立配置搜索方法和重排策略。
 *
 * <p>预定义配置：
 * <ul>
 *   <li>COMBINED_HYBRID_RRF: BM25 + Cosine + RRF 融合</li>
 *   <li>COMBINED_HYBRID_CROSS_ENCODER: BM25 + Cosine + BFS + Cross-Encoder</li>
 *   <li>EDGE_ONLY_RRF: 仅边搜索，BM25 + Cosine + RRF</li>
 * </ul>
 */
@Data
@Schema(description = "搜索配置模型")
public class SearchConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "边搜索配置")
    private EdgeSearchConfig edgeConfig;

    @Schema(description = "节点搜索配置")
    private NodeSearchConfig nodeConfig;

    @Schema(description = "Episode 搜索配置")
    private EpisodeSearchConfig episodeConfig;

    @Schema(description = "社区节点搜索配置")
    private CommunitySearchConfig communityConfig;

    @Schema(description = "每种方法返回的最大数量", example = "10")
    private int limit = 10;

    @Schema(description = "重排最小分数阈值", example = "0.0")
    private double rerankerMinScore = 0.0;

    // ==================== 预定义配置 ====================

    /**
     * 完整混合搜索配置（RRF 融合）
     * 等同于 Python 的 COMBINED_HYBRID_SEARCH_RRF
     */
    public static SearchConfig combinedHybridRrf() {
        SearchConfig config = new SearchConfig();
        config.setLimit(10);
        config.setEdgeConfig(EdgeSearchConfig.hybridRrf());
        config.setNodeConfig(NodeSearchConfig.hybridRrf());
        config.setEpisodeConfig(EpisodeSearchConfig.bm25Only());
        config.setCommunityConfig(CommunitySearchConfig.hybridRrf());
        return config;
    }

    /**
     * 完整混合搜索配置（Cross-Encoder 重排）
     * 等同于 Python 的 COMBINED_HYBRID_SEARCH_CROSS_ENCODER
     */
    public static SearchConfig combinedHybridCrossEncoder() {
        SearchConfig config = new SearchConfig();
        config.setLimit(10);
        config.setEdgeConfig(EdgeSearchConfig.hybridCrossEncoder());
        config.setNodeConfig(NodeSearchConfig.hybridCrossEncoder());
        config.setEpisodeConfig(EpisodeSearchConfig.crossEncoder());
        config.setCommunityConfig(CommunitySearchConfig.crossEncoder());
        return config;
    }

    /**
     * 仅边搜索配置（RRF 融合）
     * 等同于 Python 的 EDGE_HYBRID_SEARCH_RRF
     */
    public static SearchConfig edgeOnlyRrf() {
        SearchConfig config = new SearchConfig();
        config.setLimit(10);
        config.setEdgeConfig(EdgeSearchConfig.hybridRrf());
        return config;
    }

    // ==================== 便捷工厂方法 ====================

    /**
     * 从旧版 SearchConfigVO 创建新版 SearchConfig
     */
    public static SearchConfig fromVo(
            com.ontograph.module.graphiti.vo.search.SearchConfigVO vo) {
        if (vo == null) {
            return combinedHybridRrf();
        }

        SearchConfig config = new SearchConfig();
        config.setLimit(vo.getMaxFacts() != null ? vo.getMaxFacts() : 10);

        String mode = vo.getMode() != null ? vo.getMode() : "hybrid";

        // Edge and Node share the same config pattern
        EdgeSearchConfig edgeCfg = new EdgeSearchConfig();
        edgeCfg.setSimMinScore(0.6);
        edgeCfg.setMmrLambda(vo.getMmrLambda() != null ? vo.getMmrLambda() : 0.5);
        edgeCfg.setBfsMaxDepth(vo.getBfsDepth() != null ? vo.getBfsDepth() : 2);

        NodeSearchConfig nodeCfg = new NodeSearchConfig();
        nodeCfg.setSimMinScore(0.6);
        nodeCfg.setMmrLambda(vo.getMmrLambda() != null ? vo.getMmrLambda() : 0.5);
        nodeCfg.setBfsMaxDepth(vo.getBfsDepth() != null ? vo.getBfsDepth() : 2);

        switch (mode) {
            case "bm25" -> {
                edgeCfg.setSearchMethods(List.of(EdgeSearchMethod.bm25));
                edgeCfg.setReranker(RerankerType.rrf);
                nodeCfg.setSearchMethods(List.of(NodeSearchMethod.bm25));
                nodeCfg.setReranker(RerankerType.rrf);
            }
            case "vector" -> {
                edgeCfg.setSearchMethods(List.of(EdgeSearchMethod.cosine_similarity));
                edgeCfg.setReranker(RerankerType.rrf);
                nodeCfg.setSearchMethods(List.of(NodeSearchMethod.cosine_similarity));
                nodeCfg.setReranker(RerankerType.rrf);
            }
            case "bfs" -> {
                edgeCfg.setSearchMethods(List.of(EdgeSearchMethod.bfs));
                edgeCfg.setReranker(RerankerType.rrf);
                nodeCfg.setSearchMethods(List.of(NodeSearchMethod.bfs));
                nodeCfg.setReranker(RerankerType.rrf);
            }
            case "hybrid" -> {
                edgeCfg.setSearchMethods(List.of(
                        EdgeSearchMethod.bm25,
                        EdgeSearchMethod.cosine_similarity));
                edgeCfg.setReranker(Boolean.TRUE.equals(vo.getEnableMmr())
                        ? RerankerType.mmr : RerankerType.rrf);
                nodeCfg.setSearchMethods(List.of(
                        NodeSearchMethod.bm25,
                        NodeSearchMethod.cosine_similarity));
                nodeCfg.setReranker(Boolean.TRUE.equals(vo.getEnableMmr())
                        ? RerankerType.mmr : RerankerType.rrf);
            }
            default -> {
                edgeCfg.setSearchMethods(List.of(
                        EdgeSearchMethod.bm25,
                        EdgeSearchMethod.cosine_similarity));
                edgeCfg.setReranker(Boolean.TRUE.equals(vo.getEnableMmr())
                        ? RerankerType.mmr : RerankerType.rrf);
                nodeCfg.setSearchMethods(List.of(
                        NodeSearchMethod.bm25,
                        NodeSearchMethod.cosine_similarity));
                nodeCfg.setReranker(Boolean.TRUE.equals(vo.getEnableMmr())
                        ? RerankerType.mmr : RerankerType.rrf);
            }
        }

        config.setEdgeConfig(edgeCfg);
        config.setNodeConfig(nodeCfg);
        config.setEpisodeConfig(EpisodeSearchConfig.bm25Only());
        config.setCommunityConfig(CommunitySearchConfig.hybridRrf());

        return config;
    }

    // ==================== Builder ====================

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SearchConfig config = new SearchConfig();

        public Builder edgeConfig(EdgeSearchConfig edgeConfig) {
            config.setEdgeConfig(edgeConfig);
            return this;
        }

        public Builder nodeConfig(NodeSearchConfig nodeConfig) {
            config.setNodeConfig(nodeConfig);
            return this;
        }

        public Builder episodeConfig(EpisodeSearchConfig episodeConfig) {
            config.setEpisodeConfig(episodeConfig);
            return this;
        }

        public Builder communityConfig(CommunitySearchConfig communityConfig) {
            config.setCommunityConfig(communityConfig);
            return this;
        }

        public Builder limit(int limit) {
            config.setLimit(limit);
            return this;
        }

        public Builder rerankerMinScore(double rerankerMinScore) {
            config.setRerankerMinScore(rerankerMinScore);
            return this;
        }

        public SearchConfig build() {
            return config;
        }
    }
}
