package com.ontograph.module.graphiti.vo.extractor;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 批量实体提取结果 VO
 */
@Data
@Schema(description = "批量实体提取结果")
public class BatchExtractEntitiesVO {

    @Schema(description = "提取到的实体列表")
    private List<ExtractedEntityVO> entities;

    @Schema(description = "总实体数量")
    private Integer totalCount;

    @Schema(description = "按类型分组的统计")
    private java.util.Map<String, Integer> typeStatistics;
}
