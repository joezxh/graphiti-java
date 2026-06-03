package com.ontograph.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Pipeline 搜索结果响应 VO
 *
 * <p>参考 Python：graphiti_core/search/search_config_recipes.py:SearchResults
 */
@Data
@Schema(description = "Pipeline 搜索结果")
public class SearchPipelineRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "边搜索结果")
    private List<FactResultVO> edges;

    @Schema(description = "边重排分数")
    private List<Double> edgeScores;

    @Schema(description = "节点搜索结果")
    private List<NodeResultVO> nodes;

    @Schema(description = "节点重排分数")
    private List<Double> nodeScores;

    @Schema(description = "Episode 搜索结果")
    private List<EpisodeResultVO> episodes;

    @Schema(description = "Episode 重排分数")
    private List<Double> episodeScores;

    @Schema(description = "社区节点搜索结果")
    private List<CommunityResultVO> communities;

    @Schema(description = "社区节点重排分数")
    private List<Double> communityScores;

    @Schema(description = "执行耗时（毫秒）")
    private Long elapsedMs;

    @Data
    @Schema(description = "Episode 搜索结果项")
    public static class EpisodeResultVO implements Serializable {
        private String uuid;
        private String name;
        private String content;
        private String source;
        private String sourceDescription;
        private Double score;
    }

    @Data
    @Schema(description = "社区节点搜索结果项")
    public static class CommunityResultVO implements Serializable {
        private String uuid;
        private String name;
        private String summary;
        private Double score;
    }
}
