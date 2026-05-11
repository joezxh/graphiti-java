package com.graphiti.module.graphiti.vo.extractor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 完整数据提取结果 VO
 */
@Data
@Schema(description = "完整数据提取结果")
public class DataExtractResultVO {

    @Schema(description = "提取到的实体列表")
    private List<ExtractedEntityVO> entities;

    @Schema(description = "提取到的关系列表")
    private List<ExtractedEdgeVO> edges;

    @Schema(description = "实体总数")
    private Integer entityCount;

    @Schema(description = "关系总数")
    private Integer edgeCount;

    @Schema(description = "实体类型统计")
    private Map<String, Integer> entityTypeStatistics;

    @Schema(description = "关系类型统计")
    private Map<String, Integer> edgeTypeStatistics;

    @Schema(description = "原始 LLM 响应")
    private String rawResponse;

    @Schema(description = "错误信息列表")
    private List<String> errors;

    @Schema(description = "处理耗时（毫秒）")
    private Long elapsedMs;
}
