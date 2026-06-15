package com.ontograph.module.graphiti.vo.ontology;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 本体约束 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "本体约束信息")
public class OntConstraintVO {

    @Schema(description = "约束ID")
    private Long id;

    @Schema(description = "所属本体定义ID")
    private Long definitionId;

    @Schema(description = "关联的类ID")
    private Long classId;

    @Schema(description = "关联的属性ID")
    private Long propertyId;

    @Schema(description = "约束类型: CARDINALITY/PATTERN/RANGE/ENUM/NOT_NULL/CUSTOM_SPARQL/UNIQUE/LENGTH")
    private String constraintType;

    @Schema(description = "约束值(JSON)")
    private String value;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "严重级别: ERROR/WARNING/INFO")
    private String severity;

    @Schema(description = "约束描述")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
