package com.ontograph.module.graphiti.vo.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 优化描述请求 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "优化描述请求")
public class OptimizeDescReqVO {

    @Schema(description = "原始描述")
    private String originalDescription;

    @Schema(description = "上下文（可选），如所属类名、属性名等")
    private String context;

    @Schema(description = "语言: zh | en")
    @Builder.Default
    private String language = "zh";

    @Schema(description = "批量优化时的描述列表")
    private List<OptimizeItem> batchItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizeItem {
        private String id;
        private String originalDescription;
        private String context;
    }
}
