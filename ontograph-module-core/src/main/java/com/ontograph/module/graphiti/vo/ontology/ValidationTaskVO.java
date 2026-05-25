package com.ontograph.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationTaskVO {
    private String taskId;
    private String graphId;
    private String status;        // PENDING / RUNNING / COMPLETED / FAILED
    private String checkType;
    private GraphIntegrityResultVO result;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
