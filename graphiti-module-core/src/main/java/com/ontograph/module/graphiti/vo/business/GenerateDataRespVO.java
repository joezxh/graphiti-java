package com.graphiti.module.graphiti.vo.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 生成模拟数据响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "生成模拟数据响应")
public class GenerateDataRespVO {

    @Schema(description = "草稿ID")
    private Long draftId;

    @Schema(description = "实体列表")
    private List<EntityVO> entities;

    @Schema(description = "关系列表")
    private List<RelationshipVO> relationships;

    @Schema(description = "统计数据")
    private DataStatsVO stats;

    @Schema(description = "格式化的数据输出（JSON/CSV/N-Triples 字符串）")
    private String formattedData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityVO {
        private String id;
        private String name;
        private String type;
        private Map<String, Object> properties;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationshipVO {
        private String id;
        private String source;
        private String target;
        private String type;
        private String fact;
        private Map<String, Object> properties;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataStatsVO {
        private int totalEntities;
        private int totalRelationships;
        private int entityTypeCount;
        private int relationTypeCount;
        private List<String> entityTypes;
        private List<String> relationTypes;
    }
}
