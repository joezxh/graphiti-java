package com.graphiti.module.graphiti.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提示词版本 DO
 * 记录提示词的历史版本，用于版本管理和回滚
 */
@Data
@TableName("prompt_version")
public class PromptVersionDO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属模板ID
     */
    private Long templateId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 用户提示词模板
     */
    private String userPromptTemplate;

    /**
     * 响应格式
     */
    private String responseFormat;

    /**
     * 版本描述
     */
    private String description;

    /**
     * 是否为当前活跃版本
     */
    private Boolean active = false;

    /**
     * 创建人
     */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
