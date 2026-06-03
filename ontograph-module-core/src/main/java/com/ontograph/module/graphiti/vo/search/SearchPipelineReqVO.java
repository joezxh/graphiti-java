package com.ontograph.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Pipeline 搜索请求 VO（对齐 Python 新版 SearchConfig）
 *
 * <p>参考 Python：graphiti_core/search/search_config_recipes.py:SearchConfig
 */
@Data
@Schema(description = "Pipeline 搜索请求")
public class SearchPipelineReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索查询文本", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;

    @Schema(description = "图谱 ID")
    private String graphId;

    @Schema(description = "边搜索配置")
    private EdgeSearchConfigVO edgeConfig;

    @Schema(description = "节点搜索配置")
    private NodeSearchConfigVO nodeConfig;

    @Schema(description = "Episode 搜索配置")
    private EpisodeSearchConfigVO episodeConfig;

    @Schema(description = "社区节点搜索配置")
    private CommunitySearchConfigVO communityConfig;

    @Schema(description = "每种方法返回的最大数量", example = "10")
    private Integer limit = 10;

    @Schema(description = "重排最小分数阈值", example = "0.0")
    private Double rerankerMinScore = 0.0;

    @Schema(description = "中心节点 UUID（用于 NodeDistance 重排）")
    private String centerNodeUuid;

    @Schema(description = "BFS 起始节点 UUID 列表")
    private List<String> bfsOriginUuids;

    @Schema(description = "是否启用缓存", example = "true")
    private Boolean enableCache = true;

    @Data
    @Schema(description = "边搜索配置")
    public static class EdgeSearchConfigVO implements Serializable {
        private List<String> searchMethods;  // bm25, cosine_similarity, bfs
        private String reranker;               // rrf, mmr, cross_encoder, node_distance, episode_mentions
        private Double simMinScore = 0.6;
        private Double mmrLambda = 0.5;
        private Integer bfsMaxDepth = 2;
    }

    @Data
    @Schema(description = "节点搜索配置")
    public static class NodeSearchConfigVO implements Serializable {
        private List<String> searchMethods;
        private String reranker;
        private Double simMinScore = 0.6;
        private Double mmrLambda = 0.5;
        private Integer bfsMaxDepth = 2;
    }

    @Data
    @Schema(description = "Episode 搜索配置")
    public static class EpisodeSearchConfigVO implements Serializable {
        private List<String> searchMethods;  // 仅 bm25
        private String reranker;
        private Double simMinScore = 0.6;
        private Double mmrLambda = 0.5;
    }

    @Data
    @Schema(description = "社区节点搜索配置")
    public static class CommunitySearchConfigVO implements Serializable {
        private List<String> searchMethods;  // bm25, cosine_similarity
        private String reranker;
        private Double simMinScore = 0.6;
        private Double mmrLambda = 0.5;
    }
}
