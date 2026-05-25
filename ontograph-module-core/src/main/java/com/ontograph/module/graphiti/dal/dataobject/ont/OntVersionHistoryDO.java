package com.ontograph.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ont_version_history")
public class OntVersionHistoryDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("definition_id")
    private Long definitionId;

    private String version;
    private String changeType;     // CLASS_ADDED / PROPERTY_MODIFIED / CONSTRAINT_DELETED / ...
    private String entityType;     // CLASS / PROPERTY / CONSTRAINT / DEFINITION
    private Long entityId;
    private String beforeState;    // JSON string (TEXT column, serialized via ObjectMapper)
    private String afterState;     // JSON string (TEXT column, serialized via ObjectMapper)
    private String diffSummary;
    private String changedBy;

    @TableField(value = "changed_at", fill = FieldFill.INSERT)
    private LocalDateTime changedAt;
}
