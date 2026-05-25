package com.ontograph.module.graphiti.dal.dataobject.metadata;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体分类层次表
 * 定义法律实体的层级分类体系，与 ont_class 一一映射并扩展法律专项分类
 */
@Data
@TableName("ont_entity_category")
public class OntEntityCategoryDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("category_code")
    private String categoryCode;

    @TableField("category_name")
    private String categoryName;

    @TableField("category_level")
    private Integer categoryLevel;

    @TableField("parent_category_code")
    private String parentCategoryCode;

    @TableField("entity_type_scope")
    private String entityTypeScope;

    @TableField("default_attributes")
    private String defaultAttributes;

    private String description;

    @TableField("sort_order")
    private Integer sortOrder;

    private String metadata;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
