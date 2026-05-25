package com.graphiti.module.graphiti.dal.dataobject.metadata;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 关系类型元数据表
 * 定义预置关系类型的语义属性和约束，用于前端类型选择和后端关系推理
 */
@Data
@TableName("ont_relationship_meta")
public class OntRelationshipMetaDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("relationship_type")
    private String relationshipType;

    @TableField("relationship_name")
    private String relationshipName;

    @TableField("relationship_name_en")
    private String relationshipNameEn;

    @TableField("source_entity_types")
    private String sourceEntityTypes;

    @TableField("target_entity_types")
    private String targetEntityTypes;

    @TableField("is_directional")
    private Boolean isDirectional;

    @TableField("is_transitive")
    private Boolean isTransitive;

    private String multiplicity;

    @TableField("default_weight")
    private BigDecimal defaultWeight;

    @TableField("validity_period")
    private String validityPeriod;

    private String description;

    @TableField("example_cypher")
    private String exampleCypher;

    @TableField("sort_order")
    private Integer sortOrder;

    private String metadata;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
