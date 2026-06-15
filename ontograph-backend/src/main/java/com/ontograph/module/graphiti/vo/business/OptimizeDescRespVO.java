package com.ontograph.module.graphiti.vo.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 优化描述响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "优化描述响应")
public class OptimizeDescRespVO {

    @Schema(description = "原始描述")
    private String original;

    @Schema(description = "优化版本列表")
    private List<OptimizationVO> optimizations;

    @Schema(description = "批量优化结果")
    private List<BatchOptimizeResult> batchResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "优化版本")
    public static class OptimizationVO {
        @Schema(description = "版本标识: v1, v2, v3")
        private String version;

        @Schema(description = "优化后的描述")
        private String description;

        @Schema(description = "优化亮点")
        private List<String> highlights;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "批量优化结果项")
    public static class BatchOptimizeResult {
        private String id;
        private String original;
        private List<OptimizationVO> optimizations;
    }
}
