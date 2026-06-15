package com.ontograph.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ont_definition")
public class OntDefinitionDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("graph_id")
    private String graphId;

    private String namespace;
    private String name;
    private String version;
    private String status;        // ACTIVE / DEPRECATED / ARCHIVED
    private String description;

    @TableField("parent_version_id")
    private Long parentVersionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private String createdBy;
}
