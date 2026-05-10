package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ont_property")
public class OntPropertyDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("property_uri")
    private String propertyUri;

    @TableField("local_name")
    private String localName;

    @TableField("property_type")
    private String propertyType;   // OBJECT / DATATYPE / ANNOTATION / TRANSITIVE / SYMMETRIC / FUNCTIONAL

    @TableField("domain_class_id")
    private Long domainClassId;

    @TableField("range_class_id")
    private Long rangeClassId;

    @TableField("range_data_type")
    private String rangeDataType;  // string / integer / float / boolean / date / json / ...

    @TableField("min_cardinality")
    private Integer minCardinality;

    @TableField("max_cardinality")
    private Integer maxCardinality;

    @TableField("default_value")
    private String defaultValue;

    @TableField("allowed_values")
    private String allowedValues;  // JSON array string

    @TableField("parent_property_id")
    private Long parentPropertyId;

    @TableField("equivalent_to")
    private String equivalentTo;   // JSON array string

    @TableField("inverse_of_id")
    private Long inverseOfId;

    @TableField("is_required")
    private Boolean isRequired;

    @TableField("is_multiple")
    private Boolean isMultiple;

    private String pattern;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String description;
    private String example;
    private String metadata;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
