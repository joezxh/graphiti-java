package com.ontograph.module.graphiti.dto.batch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Bulk import task status VO
 */
@Data
@Builder
@Schema(description = "批量导入任务状态")
public class BulkImportTaskVO {

    @Schema(description = "任务 ID")
    private String taskId;

    @Schema(description = "任务状态")
    private String status;

    @Schema(description = "总条目数")
    private Integer totalItems;

    @Schema(description = "已处理条目数")
    private Integer processedItems;

    @Schema(description = "失败条目数")
    private Integer failedItems;

    @Schema(description = "创建的实体数量")
    private Integer entitiesCreated;

    @Schema(description = "创建的关系数量")
    private Integer relationsCreated;

    @Schema(description = "耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "错误详情")
    private List<String> errorDetails;
}
