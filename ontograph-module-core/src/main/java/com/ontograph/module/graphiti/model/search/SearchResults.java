package com.ontograph.module.graphiti.model.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 搜索结果聚合模型（对齐 Python 的 SearchResults）
 *
 * <p>参考 Python 实现：graphiti_core/search/search_config_recipes.py:SearchResults
 */
@Data
@Schema(description = "搜索结果聚合模型")
public class SearchResults implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "边搜索结果")
    private List<EdgeResult> edges;

    @Schema(description = "边重排分数（与 edges 一一对应）")
    private List<Double> edgeRerankerScores;

    @Schema(description = "节点搜索结果")
    private List<NodeResult> nodes;

    @Schema(description = "节点重排分数")
    private List<Double> nodeRerankerScores;

    @Schema(description = "Episode 搜索结果")
    private List<EpisodeResult> episodes;

    @Schema(description = "Episode 重排分数")
    private List<Double> episodeRerankerScores;

    @Schema(description = "社区节点搜索结果")
    private List<CommunityResult> communities;

    @Schema(description = "社区节点重排分数")
    private List<Double> communityRerankerScores;

    // ==================== 内部结果类型 ====================

    @Data
    @Schema(description = "边搜索结果项")
    public static class EdgeResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String uuid;
        private String name;
        private String fact;
        private String sourceNodeUuid;
        private String targetNodeUuid;
        private String groupId;
        private String type;
        private Double score;
        private List<String> episodeUuids;
    }

    @Data
    @Schema(description = "节点搜索结果项")
    public static class NodeResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String uuid;
        private String name;
        private List<String> labels;
        private String summary;
        private Double score;
    }

    @Data
    @Schema(description = "Episode 搜索结果项")
    public static class EpisodeResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String uuid;
        private String name;
        private String content;
        private String source;
        private String sourceDescription;
        private Double score;
    }

    @Data
    @Schema(description = "社区节点搜索结果项")
    public static class CommunityResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String uuid;
        private String name;
        private String summary;
        private Double score;
    }

    // ==================== 便捷方法 ====================

    public static SearchResults empty() {
        SearchResults results = new SearchResults();
        results.setEdges(List.of());
        results.setEdgeRerankerScores(List.of());
        results.setNodes(List.of());
        results.setNodeRerankerScores(List.of());
        results.setEpisodes(List.of());
        results.setEpisodeRerankerScores(List.of());
        results.setCommunities(List.of());
        results.setCommunityRerankerScores(List.of());
        return results;
    }

    public boolean isEmpty() {
        return (edges == null || edges.isEmpty())
                && (nodes == null || nodes.isEmpty())
                && (episodes == null || episodes.isEmpty())
                && (communities == null || communities.isEmpty());
    }
}
