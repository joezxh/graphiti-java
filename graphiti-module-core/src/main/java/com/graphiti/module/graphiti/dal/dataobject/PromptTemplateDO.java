package com.graphiti.module.graphiti.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提示词模板 DO
 * 参考 tianque-ai 的 BasePromptTemplateDO 设计
 */
@Data
@TableName("prompt_template")
public class PromptTemplateDO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 模板编码（唯一标识，如：LEGAL_ENTITY_EXTRACT, LEGAL_EDGE_EXTRACT）
     */
    private String code;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板描述
     */
    private String description;

    /**
     * 模板类型：entity_extract-实体抽取, edge_extract-关系抽取, dedupe-去重, summary-摘要
     */
    private String type;

    /**
     * 系统提示词（System Prompt）
     */
    private String systemPrompt;

    /**
     * 用户提示词模板（User Prompt，支持变量占位符 {variable}）
     */
    private String userPromptTemplate;

    /**
     * 响应格式定义（JSON Schema 或格式说明）
     */
    private String responseFormat;

    /**
     * 是否启用
     */
    private Boolean enabled = true;

    /**
     * 所属模型（可选）
     */
    private String model;

    /**
     * 排序值
     */
    private Integer sort = 0;

    /**
     * 标签（JSON数组格式，用于分类）
     */
    private String tags;

    /**
     * 额外配置（JSON格式）
     */
    private String extraConfig;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 更新人ID
     */
    private Long updatedBy;
}
