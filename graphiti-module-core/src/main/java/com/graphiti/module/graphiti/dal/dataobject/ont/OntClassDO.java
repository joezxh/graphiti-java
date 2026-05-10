package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ont_class")
public class OntClassDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("class_uri")
    private String classUri;

    @TableField("local_name")
    private String localName;

    @TableField("parent_class_id")
    private Long parentClassId;

    @TableField("equivalent_to")
    private String equivalentTo;   // JSON array string: ["uri1", "uri2"]

    @TableField("disjoint_with")
    private String disjointWith;   // JSON array string: [1, 2, 3]

    private String description;
    private String example;

    @TableField("domain_hint")
    private String domainHint;    // FINANCIAL / MEDICAL / ECOMMERCE / KNOWLEDGE

    @TableField("metadata")
    private String metadata;      // JSON string (TEXT column, serialized via ObjectMapper)

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
