package com.ontograph.module.graphiti.dal.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 提示词变量 DO
 * 定义每个提示词模板支持的变量
 */
@Data
@TableName("prompt_variable")
public class PromptVariableDO implements Serializable {
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
     * 变量名称（对应提示词中的 {variable} 占位符）
     */
    private String variableName;

    /**
     * 变量描述
     */
    private String description;

    /**
     * 变量类型：string-字符串, list-列表, json-JSON对象, text-长文本
     */
    private String variableType;

    /**
     * 是否必需
     */
    private Boolean required = true;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 变量来源：context-上下文, static-静态值, llm-动态生成
     */
    private String source;

    /**
     * 验证规则（正则表达式或内置规则）
     */
    private String validationRule;

    /**
     * 排序值
     */
    private Integer sort = 0;

    /**
     * 备注
     */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
