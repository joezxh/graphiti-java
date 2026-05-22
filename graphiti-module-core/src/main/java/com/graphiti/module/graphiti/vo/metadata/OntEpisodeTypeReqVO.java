package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 剧集类型请求 VO（创建/更新共用）
 */
@Data
@Schema(description = "创建/更新剧集类型请求 V5")
public class OntEpisodeTypeReqVO {

    @Schema(description = "主键ID（更新时必需）")
    private Long id;

    @Schema(description = "本体定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "本体定义ID不能为空")
    private Long definitionId;

    @Schema(description = "类型代码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类型代码不能为空")
    @Size(max = 50, message = "类型代码最多50字符")
    private String typeCode;

    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    @Schema(description = "英文名称")
    private String typeNameEn;

    // ========== 层级关系（V5 新增）==========
    @Schema(description = "父类型编码")
    private String parentTypeCode;

    @Schema(description = "层级深度（1-5）")
    @Min(value = 1, message = "层级最小为1")
    @Max(value = 5, message = "层级最大为5")
    private Integer level;

    // ========== 通用分类字段 ==========
    @Schema(description = "业务流程类型：business_process|workflow|lifecycle")
    private String processType;

    @Schema(description = "阶段标签：立案|庭审|调解|判决|执行")
    private String stageLabel;

    @Schema(description = "阶段级别（通用，可配置）")
    private String stageLevel;

    @Schema(description = "是否审查/评议阶段")
    private Boolean isReviewStage;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序值")
    private Integer sortOrder = 0;

    @Schema(description = "元数据 JSON")
    private String metadata;

    @Schema(description = "状态: ACTIVE|INACTIVE|DEPRECATED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ACTIVE|INACTIVE|DEPRECATED", message = "状态必须是 ACTIVE/INACTIVE/DEPRECATED")
    private String status = "ACTIVE";
}
