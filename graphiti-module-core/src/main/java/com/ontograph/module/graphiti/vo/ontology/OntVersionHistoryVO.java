package com.graphiti.module.graphiti.vo.ontology;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 本体版本历史 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "本体版本历史信息")
public class OntVersionHistoryVO {

    @Schema(description = "历史记录ID")
    private Long id;

    @Schema(description = "所属本体定义ID")
    private Long definitionId;

    @Schema(description = "版本号")
    private String version;

    @Schema(description = "变更类型: CLASS_ADDED/PROPERTY_MODIFIED/CONSTRAINT_DELETED/...")
    private String changeType;

    @Schema(description = "实体类型: CLASS/PROPERTY/CONSTRAINT/DEFINITION")
    private String entityType;

    @Schema(description = "实体ID")
    private Long entityId;

    @Schema(description = "变更前状态(JSON)")
    private String beforeState;

    @Schema(description = "变更后状态(JSON)")
    private String afterState;

    @Schema(description = "变更摘要")
    private String diffSummary;

    @Schema(description = "变更人")
    private String changedBy;

    @Schema(description = "变更时间")
    private LocalDateTime changedAt;
}
