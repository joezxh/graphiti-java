package com.graphiti.module.graphiti.vo.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成模拟数据请求 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "生成模拟数据请求")
public class GenerateDataReqVO {

    @Schema(description = "数据规模（实体数量）")
    @Builder.Default
    private int count = 20;

    @Schema(description = "格式: JSON | CSV | N-TRIPLES")
    @Builder.Default
    private String format = "JSON";

    @Schema(description = "实体类型过滤（可选），指定只生成某些类型的实体")
    private String[] entityTypes;

    @Schema(description = "关系类型过滤（可选）")
    private String[] relationTypes;

    @Schema(description = "是否包含属性")
    @Builder.Default
    private boolean includeProperties = true;

    @Schema(description = "数据多样性级别: low | medium | high")
    @Builder.Default
    private String diversity = "medium";
}
