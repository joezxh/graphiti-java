package com.ontograph.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 关系元数据请求 VO（创建/更新共用）
 */
@Data
@Schema(description = "创建/更新关系元数据请求")
public class OntRelationshipMetaReqVO {

    @Schema(description = "主键ID（更新时必需）")
    private Long id;

    @Schema(description = "本体定义ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long definitionId;

    @Schema(description = "关系类型代码（对应 Neo4j 关系类型）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "关系类型不能为空")
    private String relationshipType;

    @Schema(description = "关系中文名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "关系名称不能为空")
    private String relationshipName;

    @Schema(description = "英文名称")
    private String relationshipNameEn;

    @Schema(description = "源实体类型 JSON 数组")
    private String sourceEntityTypes;

    @Schema(description = "目标实体类型 JSON 数组")
    private String targetEntityTypes;

    @Schema(description = "是否有向")
    private Boolean isDirectional = true;

    @Schema(description = "是否可传递")
    private Boolean isTransitive = false;

    @Schema(description = "多重性: one-to-one|one-to-many|many-to-many")
    private String multiplicity = "many-to-many";

    @Schema(description = "默认权重（0.0000-1.0000）")
    private BigDecimal defaultWeight = new BigDecimal("1.0000");

    @Schema(description = "有效期 JSON")
    private String validityPeriod;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "示例 Cypher")
    private String exampleCypher;

    @Schema(description = "排序值")
    private Integer sortOrder = 0;

    @Schema(description = "元数据 JSON")
    private String metadata;

    @Schema(description = "状态: ACTIVE|INACTIVE")
    private String status = "ACTIVE";
}
