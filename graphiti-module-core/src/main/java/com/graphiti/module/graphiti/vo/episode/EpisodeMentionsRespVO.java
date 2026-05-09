package com.graphiti.module.graphiti.vo.episode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 事件提及的节点和边响应 VO
 */
@Data
@Schema(description = "事件提及的节点和边")
public class EpisodeMentionsRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "提及的节点列表")
    private List<EpisodeNodeVO> nodes;

    @Schema(description = "提及的边列表")
    private List<EpisodeEdgeVO> edges;

    /**
     * 事件提及的节点 VO
     */
    @Data
    @Schema(description = "事件提及的节点")
    public static class EpisodeNodeVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "节点 UUID")
        private String uuid;

        @Schema(description = "节点名称")
        private String name;

        @Schema(description = "节点类型")
        private String type;

        @Schema(description = "节点摘要")
        private String summary;
    }

    /**
     * 事件提及的边 VO
     */
    @Data
    @Schema(description = "事件提及的边")
    public static class EpisodeEdgeVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "边 UUID")
        private String uuid;

        @Schema(description = "源节点 UUID")
        private String sourceNodeUuid;

        @Schema(description = "目标节点 UUID")
        private String targetNodeUuid;

        @Schema(description = "关系类型")
        private String type;

        @Schema(description = "事实描述")
        private String fact;
    }
}
