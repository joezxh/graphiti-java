package com.ontograph.module.graphiti.vo.extractor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 实体提取结果 VO
 */
@Data
@Schema(description = "实体提取结果")
public class ExtractedEntityVO {

    @Schema(description = "实体名称")
    private String name;

    @Schema(description = "实体类型")
    private String entityType;

    @Schema(description = "实体类型ID")
    private Integer entityTypeId;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "属性列表")
    private Map<String, Object> attributes;

    @Schema(description = "来源 episode 索引")
    private List<Integer> episodeIndices;

    @Schema(description = "置信度")
    private Double confidence;
}
