package com.graphiti.module.graphiti.vo.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 添加消息请求 VO（对话历史写入图谱）
 */
@Data
@Schema(description = "添加消息请求")
public class AddMessagesReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标图谱 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "图谱 ID 不能为空")
    private String graphId;

    @Schema(description = "消息列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "消息列表不能为空")
    private List<MessageItemVO> messages;

    @Schema(description = "是否更新社区")
    private Boolean updateCommunities = false;

    /**
     * 消息项 VO
     */
    @Data
    @Schema(description = "消息项")
    public static class MessageItemVO implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "消息角色（system/user/assistant）", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "角色不能为空")
        private String role;

        @Schema(description = "消息内容", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "内容不能为空")
        private String content;
    }
}
