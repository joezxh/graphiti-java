package com.graphiti.module.graphiti.vo.extractor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 关系提取结果 VO
 */
@Data
@Schema(description = "关系提取结果")
public class ExtractedEdgeVO {

    @Schema(description = "源实体名称")
    private String sourceEntityName;

    @Schema(description = "目标实体名称")
    private String targetEntityName;

    @Schema(description = "关系类型")
    private String relationType;

    @Schema(description = "事实描述")
    private String fact;

    @Schema(description = "生效时间")
    private LocalDateTime validAt;

    @Schema(description = "失效时间")
    private LocalDateTime invalidAt;

    @Schema(description = "来源 episode 索引")
    private java.util.List<Integer> episodeIndices;

    @Schema(description = "置信度")
    private Double confidence;

    @Schema(description = "属性")
    private java.util.Map<String, Object> attributes;
}
