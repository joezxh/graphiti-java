package com.ontograph.module.graphiti.vo.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 本体草稿 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "本体草稿信息")
public class OntDraftVO {

    @Schema(description = "草稿ID")
    private Long id;

    @Schema(description = "图谱ID")
    private String graphId;

    @Schema(description = "草稿名称")
    private String draftName;

    @Schema(description = "草稿类型: DRAFT | OPTIMIZED | GENERATED")
    private String draftType;

    @Schema(description = "状态: PENDING | APPROVED | REJECTED | APPLIED")
    private String status;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "包含模拟数据")
    private boolean hasMockData;

    @Schema(description = "实体数量（如果包含模拟数据）")
    private Integer mockEntityCount;

    @Schema(description = "关系数量（如果包含模拟数据）")
    private Integer mockRelationCount;
}
