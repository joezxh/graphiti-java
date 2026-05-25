package com.ontograph.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ont_class_inheritance")
public class OntClassInheritanceDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("class_id")
    private Long classId;

    @TableField("parent_class_id")
    private Long parentClassId;

    @TableField("definition_id")
    private Long definitionId;

    private Integer distance;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
