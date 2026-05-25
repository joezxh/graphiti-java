package com.ontograph.module.graphiti.vo.prompt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 提示词模板 VO
 */
@Data
@Schema(description = "提示词模板")
public class PromptTemplateVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "模板编码")
    @NotBlank(message = "模板编码不能为空")
    private String code;

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    private String name;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "模板类型：entity_extract-实体抽取, edge_extract-关系抽取, dedupe-去重, summary-摘要")
    @NotBlank(message = "模板类型不能为空")
    private String type;

    @Schema(description = "系统提示词")
    @NotBlank(message = "系统提示词不能为空")
    private String systemPrompt;

    @Schema(description = "用户提示词模板")
    @NotBlank(message = "用户提示词模板不能为空")
    private String userPromptTemplate;

    @Schema(description = "响应格式定义")
    private String responseFormat;

    @Schema(description = "是否启用")
    private Boolean enabled = true;

    @Schema(description = "所属模型")
    private String model;

    @Schema(description = "排序值")
    private Integer sort = 0;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "额外配置JSON")
    private String extraConfig;

    @Schema(description = "变量列表")
    private List<PromptVariableVO> variables;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
