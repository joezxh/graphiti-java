package com.ontograph.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ont_mapping")
public class OntMappingDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("source_ontology")
    private String sourceOntology;

    @TableField("source_type")
    private String sourceType;

    @TableField("mapped_class_uri")
    private String mappedClassUri;

    @TableField("mapping_type")
    private String mappingType;

    private BigDecimal confidence;

    private String metadata;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
