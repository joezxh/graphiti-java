package com.graphiti.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 获取记忆请求 VO（基于对话历史重建上下文）
 */
@Data
@Schema(description = "获取记忆请求")
public class GetMemoryReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "对话历史消息", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "消息列表不能为空")
    @Valid
    private List<MessageQueryVO> messages;

    @Schema(description = "限定的 graph_id 列表（图谱 ID）")
    private List<String> groupIds;

    @Schema(description = "最大返回事实数", example = "10")
    private Integer maxFacts = 10;
}
