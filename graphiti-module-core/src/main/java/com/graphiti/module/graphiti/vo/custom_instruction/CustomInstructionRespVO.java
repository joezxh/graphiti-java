package com.graphiti.module.graphiti.vo.custom_instruction;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 自定义指令响应 VO
 */
@Data
@Schema(description = "自定义指令响应")
public class CustomInstructionRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "指令ID")
    private Long id;

    @Schema(description = "图谱ID（null 表示全局指令）")
    private String graphId;

    @Schema(description = "指令内容")
    private String instruction;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
