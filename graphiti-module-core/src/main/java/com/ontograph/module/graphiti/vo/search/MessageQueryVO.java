package com.ontograph.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/**
 * 消息查询 VO（用于对话历史）
 */
@Data
@Schema(description = "消息查询")
public class MessageQueryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息角色（system/user/assistant）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "角色不能为空")
    private String role;

    @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "内容不能为空")
    private String content;
}
