package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ont_constraint")
public class OntConstraintDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    @TableField("class_id")
    private Long classId;

    @TableField("property_id")
    private Long propertyId;

    @TableField("constraint_type")
    private String constraintType;  // CARDINALITY / PATTERN / RANGE / ENUM / NOT_NULL / CUSTOM_SPARQL

    @TableField("value")
    private String value;          // JSON string: { "min": 1, "max": 5 } or { "pattern": "^[A-Z].*" }

    @TableField("error_message")
    private String errorMessage;

    private String severity;       // ERROR / WARNING / INFO
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
