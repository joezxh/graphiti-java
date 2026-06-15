package com.ontograph.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Pipeline 搜索结果重排请求 VO
 *
 * <p>参考 Python：server/graph_service/routers/graph.py 的 /rerank 端点
 */
@Data
@Schema(description = "Pipeline 搜索结果重排请求")
public class RerankReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "查询文本（用于 Cross-Encoder/MMR 评分）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String query;

    @Schema(description = "图谱 ID")
    private String graphId;

    @Schema(description = "边候选项")
    private List<EdgeCandidateVO> edges;

    @Schema(description = "节点候选项")
    private List<NodeCandidateVO> nodes;

    @Schema(description = "重排策略", example = "rrf")
    private String reranker;

    @Schema(description = "MMR lambda 参数（0=纯多样性, 1=纯相关性）", example = "0.5")
    private Double mmrLambda = 0.5;

    @Schema(description = "中心节点 UUID（用于 NodeDistance 重排）")
    private String centerNodeUuid;

    @Schema(description = "返回数量上限", example = "10")
    private Integer limit = 10;

    @Data
    @Schema(description = "边候选项")
    public static class EdgeCandidateVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "边 UUID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String uuid;

        @Schema(description = "边名称")
        private String name;

        @Schema(description = "事实描述")
        private String fact;

        @Schema(description = "源节点 UUID")
        private String sourceNodeUuid;

        @Schema(description = "目标节点 UUID")
        private String targetNodeUuid;

        @Schema(description = "已有分数（可选）")
        private Double score;
    }

    @Data
    @Schema(description = "节点候选项")
    public static class NodeCandidateVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "节点 UUID", requiredMode = Schema.RequiredMode.REQUIRED)
        private String uuid;

        @Schema(description = "节点名称")
        private String name;

        @Schema(description = "节点摘要")
        private String summary;

        @Schema(description = "节点标签")
        private List<String> labels;

        @Schema(description = "已有分数（可选）")
        private Double score;
    }
}
