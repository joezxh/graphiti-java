package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 剧集类型请求 VO（创建/更新共用）
 */
@Data
@Schema(description = "创建/更新剧集类型请求")
public class OntEpisodeTypeReqVO {

    @Schema(description = "主键ID（更新时必需）")
    private Long id;

    @Schema(description = "本体定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long definitionId;

    @Schema(description = "类型代码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类型代码不能为空")
    private String typeCode;

    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    @Schema(description = "英文名称")
    private String typeNameEn;

    // ========== 通用化字段 ==========
    @Schema(description = "业务流程类型：business_process|workflow|lifecycle")
    private String processType;

    @Schema(description = "阶段级别（通用，可配置。法律领域：一审/二审/再审；其他领域：可自定义）")
    private String stageLevel;

    @Schema(description = "是否审查/评议阶段")
    private Boolean isReviewStage;

    // ========== 向后兼容旧字段（Phase 3 迁移完成后删除）==========
    @Schema(description = "[向后兼容] 法律程序: litigation|mediation|arbitration|execution")
    private String legalProcess;

    @Schema(description = "阶段标签: 立案|庭审|调解|判决|执行")
    private String stageLabel;

    @Schema(description = "[向后兼容] 审级: 一审|二审|再审|死刑复核")
    private String courtLevel;

    @Schema(description = "[向后兼容] 是否审判阶段")
    private Boolean isTrialStage = false;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序值")
    private Integer sortOrder = 0;

    @Schema(description = "元数据 JSON")
    private String metadata;

    @Schema(description = "状态: ACTIVE|INACTIVE")
    private String status = "ACTIVE";
}
