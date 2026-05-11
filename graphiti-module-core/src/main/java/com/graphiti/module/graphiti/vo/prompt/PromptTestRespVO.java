package com.graphiti.module.graphiti.vo.prompt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 提示词测试响应 VO
 */
@Data
@Schema(description = "提示词测试响应")
public class PromptTestRespVO {

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "原始响应内容")
    private String rawResponse;

    @Schema(description = "解析后的结构化数据（JSON格式）")
    private String parsedData;

    @Schema(description = "提取到的实体数量")
    private Integer entityCount;

    @Schema(description = "提取到的关系数量")
    private Integer edgeCount;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "Token 消耗统计")
    private TokenUsage tokenUsage;

    @Schema(description = "响应耗时（毫秒）")
    private Long elapsedMs;

    @Schema(description = "响应时间")
    private LocalDateTime responseTime;

    @Data
    @Schema(description = "Token 使用统计")
    public static class TokenUsage {
        @Schema(description = "输入 tokens")
        private Integer inputTokens;

        @Schema(description = "输出 tokens")
        private Integer outputTokens;

        @Schema(description = "总 tokens")
        private Integer totalTokens;
    }
}
