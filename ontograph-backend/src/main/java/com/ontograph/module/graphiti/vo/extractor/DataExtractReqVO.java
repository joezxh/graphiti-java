package com.ontograph.module.graphiti.vo.extractor;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 数据提取请求 VO
 */
@Data
@Schema(description = "数据提取请求")
public class DataExtractReqVO {

    @Schema(description = "图谱ID")
    @NotBlank(message = "图谱ID不能为空")
    private String graphId;

    @Schema(description = "输入内容（文本或JSON）")
    @NotBlank(message = "输入内容不能为空")
    private String content;

    @Schema(description = "数据源类型：text-文本, json-JSON, message-消息")
    private String sourceType = "text";

    @Schema(description = "数据源描述")
    private String sourceDescription;

    @Schema(description = "实体类型配置（JSON格式）")
    private String entityTypesConfig;

    @Schema(description = "关系类型配置（JSON格式）")
    private String edgeTypesConfig;

    @Schema(description = "自定义提取指令")
    private String customInstructions;

    @Schema(description = "历史 Episodes（用于上下文）")
    private List<EpisodeContext> previousEpisodes;

    @Schema(description = "参考时间（用于解析相对时间）")
    private LocalDateTime referenceTime;

    @Schema(description = "是否仅提取实体（不提取关系）")
    private Boolean entityOnly = false;

    @Schema(description = "是否仅提取关系（需要先提供实体）")
    private Boolean edgeOnly = false;

    @Schema(description = "已有实体列表（用于关系提取）")
    private List<ExtractedEntityVO> existingEntities;

    @Schema(description = "提示词模板ID（可选）")
    private Long promptTemplateId;

    @Schema(description = "额外变量（JSON格式）")
    private Map<String, String> extraVariables;

    @Data
    @Schema(description = "Episode 上下文")
    public static class EpisodeContext {
        @Schema(description = "Episode 内容")
        private String content;

        @Schema(description = "时间戳")
        private LocalDateTime timestamp;

        @Schema(description = "来源类型")
        private String sourceType;
    }
}
