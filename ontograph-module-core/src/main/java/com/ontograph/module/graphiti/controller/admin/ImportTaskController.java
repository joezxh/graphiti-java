package com.ontograph.module.graphiti.controller.admin;

import com.ontograph.common.response.CommonResult;
import com.ontograph.module.graphiti.dal.repository.ImportTaskRepository;
import com.ontograph.module.graphiti.dto.batch.BulkImportTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Import Task", description = "Bulk import task status")
@RestController
@RequestMapping("/api/v1/graph/data/task")
public class ImportTaskController {

    @Resource
    private ImportTaskRepository importTaskRepository;

    @GetMapping("/{taskId}")
    @Operation(summary = "Get task status")
    public CommonResult<BulkImportTaskVO> getTask(@PathVariable String taskId) {
        BulkImportTaskVO task = importTaskRepository.getTask(taskId);
        if (task == null) {
            return CommonResult.error(404, "Task not found: " + taskId);
        }
        return CommonResult.success(task);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "Cancel task")
    public CommonResult<Void> cancelTask(@PathVariable String taskId) {
        log.info("Cancel task: taskId={}", taskId);
        return CommonResult.success(null);
    }
}
