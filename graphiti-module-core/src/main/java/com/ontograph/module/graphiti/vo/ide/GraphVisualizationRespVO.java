package com.graphiti.module.graphiti.vo.ide;

import lombok.Data;
import lombok.Builder;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 图谱可视化响应 VO
 */
@Data
@Builder
public class GraphVisualizationRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<NodeVO> nodes;
    private List<EdgeVO> edges;
    private PaginationVO pagination;
    private AggregationVO aggregations;

    @Data
    @Builder
    public static class NodeVO implements Serializable {
        private String uuid;
        private String name;
        private String type;
        private Double x;
        private Double y;
        private Map<String, Object> properties;
        private String summary;
        private String createdAt;
        private String updatedAt;
    }

    @Data
    @Builder
    public static class EdgeVO implements Serializable {
        private String uuid;
        private String source;
        private String target;
        private String type;
        private String fact;
        private Map<String, Object> properties;
    }

    @Data
    @Builder
    public static class PaginationVO implements Serializable {
        private Integer page;
        private Integer pageSize;
        private Long total;
        private Integer totalPages;
    }

    @Data
    @Builder
    public static class AggregationVO implements Serializable {
        private List<ClassCount> byClass;
    }

    @Data
    @Builder
    public static class ClassCount implements Serializable {
        private String type;
        private Long count;
    }
}
