package com.graphiti.module.graphiti.vo.prompt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提示词变量 VO
 */
@Data
@Schema(description = "提示词变量")
public class PromptVariableVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "所属模板ID")
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    @Schema(description = "变量名称")
    @NotBlank(message = "变量名称不能为空")
    private String variableName;

    @Schema(description = "变量描述")
    private String description;

    @Schema(description = "变量类型：string-字符串, list-列表, json-JSON对象, text-长文本")
    private String variableType = "string";

    @Schema(description = "是否必需")
    private Boolean required = true;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "变量来源：context-上下文, static-静态值, llm-动态生成")
    private String source = "context";

    @Schema(description = "验证规则")
    private String validationRule;

    @Schema(description = "排序值")
    private Integer sort = 0;

    @Schema(description = "备注")
    private String remark;
}
