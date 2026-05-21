package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 关系元数据响应 VO
 */
@Data
@Builder
@Schema(description = "关系元数据响应")
public class OntRelationshipMetaRespVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "本体定义ID")
    private Long definitionId;
    @Schema(description = "关系类型")
    private String relationshipType;
    @Schema(description = "关系名称")
    private String relationshipName;
    @Schema(description = "英文名称")
    private String relationshipNameEn;
    @Schema(description = "源实体类型")
    private String sourceEntityTypes;
    @Schema(description = "目标实体类型")
    private String targetEntityTypes;
    @Schema(description = "是否有向")
    private Boolean isDirectional;
    @Schema(description = "是否可传递")
    private Boolean isTransitive;
    @Schema(description = "多重性")
    private String multiplicity;
    @Schema(description = "默认权重")
    private BigDecimal defaultWeight;
    @Schema(description = "有效期")
    private String validityPeriod;
    @Schema(description = "描述")
    private String description;
    @Schema(description = "示例 Cypher")
    private String exampleCypher;
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
