package com.ontograph.module.graphiti.vo.prompt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提示词测试请求 VO
 */
@Data
@Schema(description = "提示词测试请求")
public class PromptTestReqVO {

    @Schema(description = "模板ID")
    @NotBlank(message = "模板ID不能为空")
    private String templateId;

    @Schema(description = "输入内容（待提取的文本/JSON）")
    @NotBlank(message = "输入内容不能为空")
    private String inputContent;

    @Schema(description = "上下文内容（历史 episodes 等）")
    private String contextContent;

    @Schema(description = "自定义变量（JSON格式）")
    private String customVariables;

    @Schema(description = "数据源类型：text-文本, json-JSON, message-消息")
    private String sourceType = "text";

    @Schema(description = "模型名称（可选，默认使用模板配置）")
    private String model;

    @Schema(description = "温度参数")
    private Double temperature;

    @Schema(description = "最大 tokens")
    private Integer maxTokens;
}
