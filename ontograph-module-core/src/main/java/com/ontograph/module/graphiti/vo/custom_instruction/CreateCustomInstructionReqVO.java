package com.ontograph.module.graphiti.vo.custom_instruction;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/**
 * 创建自定义指令请求 VO
 */
@Data
@Schema(description = "创建自定义指令请求")
public class CreateCustomInstructionReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "指令内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "指令内容不能为空")
    private String instruction;

    @Schema(description = "图谱ID（可选，为 null 时表示全局指令）")
    private String graphId;
}
