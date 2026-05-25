package com.graphiti.module.graphiti.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 自定义抽取指令 DO
 */
@Data
@TableName("custom_instruction")
public class CustomInstructionDO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 图谱ID（可选，为 null 时表示全局指令）
     */
    private String graphId;

    /**
     * 指令内容（LLM 抽取时的额外提示词）
     */
    private String instruction;

    /**
     * 启用状态
     */
    private Boolean enabled = true;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
