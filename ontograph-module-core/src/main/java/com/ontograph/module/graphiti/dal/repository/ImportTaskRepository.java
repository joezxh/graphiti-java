package com.ontograph.module.graphiti.dal.repository;

import com.ontograph.module.graphiti.dal.dataobject.ImportTaskDO;
import com.ontograph.module.graphiti.dto.batch.BulkImportResult;
import com.ontograph.module.graphiti.dto.batch.BulkImportTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class ImportTaskRepository {

    private final Map<String, ImportTaskDO> store = new ConcurrentHashMap<>();

    public void save(String taskId, String graphId, int totalItems) {
        ImportTaskDO task = new ImportTaskDO();
        task.setTaskId(taskId);
        task.setGraphId(graphId);
        task.setTotalItems(totalItems);
        task.setProcessedItems(0);
        task.setFailedItems(0);
        task.setEntitiesCreated(0);
        task.setRelationsCreated(0);
        task.setStatus("PROCESSING");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        store.put(taskId, task);
        log.info("Import task saved: taskId={}, graphId={}, totalItems={}", taskId, graphId, totalItems);
    }

    public void updateResult(String taskId, BulkImportResult result) {
        ImportTaskDO task = store.get(taskId);
        if (task == null) return;
        task.setProcessedItems(result.getProcessedItems());
        task.setFailedItems(result.getFailedItems());
        task.setEntitiesCreated(result.getEntitiesCreated());
        task.setRelationsCreated(result.getRelationsCreated());
        task.setStatus("COMPLETED");
        task.setDurationMs(result.getDurationMs());
        task.setErrorDetails(result.getErrorDetails() != null
            ? String.join("; ", result.getErrorDetails()) : null);
        task.setUpdateTime(LocalDateTime.now());
        log.info("Import task result updated: taskId={}, status=COMPLETED", taskId);
    }

    public void updateFailed(String taskId, String errorMessage) {
        ImportTaskDO task = store.get(taskId);
        if (task == null) return;
        task.setStatus("FAILED");
        task.setErrorDetails(errorMessage);
        task.setUpdateTime(LocalDateTime.now());
        log.error("Import task failed: taskId={}, error={}", taskId, errorMessage);
    }

    public BulkImportTaskVO getTask(String taskId) {
        ImportTaskDO task = store.get(taskId);
        if (task == null) return null;
        return BulkImportTaskVO.builder()
            .taskId(task.getTaskId())
            .status(task.getStatus())
            .totalItems(task.getTotalItems())
            .processedItems(task.getProcessedItems())
            .failedItems(task.getFailedItems())
            .entitiesCreated(task.getEntitiesCreated())
            .relationsCreated(task.getRelationsCreated())
            .durationMs(task.getDurationMs())
            .errorDetails(task.getErrorDetails() != null
                ? List.of(task.getErrorDetails().split(";")) : List.of())
            .build();
    }
}
