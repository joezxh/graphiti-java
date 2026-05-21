package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * 剧集类型响应 VO
 */
@Data
@Builder
@Schema(description = "剧集类型响应")
public class OntEpisodeTypeRespVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "本体定义ID")
    private Long definitionId;
    @Schema(description = "类型代码")
    private String typeCode;
    @Schema(description = "类型名称")
    private String typeName;
    @Schema(description = "英文名称")
    private String typeNameEn;
    // ========== 通用化字段 ==========
    @Schema(description = "业务流程类型")
    private String processType;
    @Schema(description = "阶段级别")
    private String stageLevel;
    @Schema(description = "是否审查/评议阶段")
    private Boolean isReviewStage;
    // ========== 向后兼容旧字段（Phase 3 迁移完成后删除）==========
    @Schema(description = "[向后兼容] 法律程序")
    private String legalProcess;
    @Schema(description = "阶段标签")
    private String stageLabel;
    @Schema(description = "[向后兼容] 审级")
    private String courtLevel;
    @Schema(description = "[向后兼容] 是否审判阶段")
    private Boolean isTrialStage;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "排序值")
    private Integer sortOrder;
    @Schema(description = "元数据")
    private String metadata;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
