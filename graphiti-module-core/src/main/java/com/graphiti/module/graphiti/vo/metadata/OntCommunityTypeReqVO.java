package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 社区类型请求 VO（创建/更新共用）
 */
@Data
@Schema(description = "创建/更新社区类型请求")
public class OntCommunityTypeReqVO {

    @Schema(description = "主键ID（更新时必需）")
    private Long id;

    @Schema(description = "本体定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long definitionId;

    @Schema(description = "类型代码，如 DOMAIN_CIVIL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类型代码不能为空")
    private String typeCode;

    @Schema(description = "类型名称，如 民商事", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    @Schema(description = "英文名称")
    private String typeNameEn;

    @Schema(description = "分类维度: domain|region|scenario", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "分类维度不能为空")
    private String category;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "父类型代码（层级嵌套）")
    private String parentTypeCode;

    @Schema(description = "排序值")
    private Integer sortOrder = 0;

    @Schema(description = "区域/管辖区: REGION_CN|REGION_US|REGION_EU|REGION_ROOT")
    private String region;

    @Schema(description = "应用场景: SCENARIO_JUDICIAL|SCENARIO_COMPLIANCE|SCENARIO_RISK|SCENARIO_ROOT")
    private String scenarioType;

    @Schema(description = "关联的图数据库社区节点 uuid")
    private String communityUuid;

    @Schema(description = "图谱 ID")
    private String graphId;

    @Schema(description = "元数据 JSON（{icon, color, displayPriority}）")
    private String metadata;

    @Schema(description = "状态: ACTIVE|INACTIVE")
    private String status = "ACTIVE";
}
