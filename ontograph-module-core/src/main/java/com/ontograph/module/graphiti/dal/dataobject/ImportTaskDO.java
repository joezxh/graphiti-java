package com.ontograph.module.graphiti.dal.dataobject;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ImportTaskDO {
    private String taskId;
    private String graphId;
    private Integer totalItems;
    private Integer processedItems;
    private Integer failedItems;
    private Integer entitiesCreated;
    private Integer relationsCreated;
    private String status;
    private String errorDetails;
    private Long durationMs;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
