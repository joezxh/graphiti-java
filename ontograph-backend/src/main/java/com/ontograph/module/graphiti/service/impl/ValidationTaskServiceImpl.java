package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.service.ValidationTaskService;
import com.ontograph.module.graphiti.service.validator.GraphIntegrityValidator;
import com.ontograph.module.graphiti.vo.ontology.GraphIntegrityResultVO;
import com.ontograph.module.graphiti.vo.ontology.ValidationTaskVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValidationTaskServiceImpl implements ValidationTaskService {

    private final GraphIntegrityValidator integrityValidator;

    // 内存任务存储（生产环境建议用 Redis 或数据库）
    private final Map<String, ValidationTaskVO> taskStore = new ConcurrentHashMap<>();

    @Override
    public String submitIntegrityCheck(String graphId, List<String> checkTypes) {
        String taskId = UUID.randomUUID().toString();
        
        ValidationTaskVO task = ValidationTaskVO.builder()
                .taskId(taskId)
                .graphId(graphId)
                .status("PENDING")
                .checkType(String.join(",", checkTypes != null ? checkTypes : List.of("ALL")))
                .createdAt(LocalDateTime.now())
                .build();
        
        taskStore.put(taskId, task);
        log.info("提交完整性检查任务: taskId={}, graphId={}, checkTypes={}", taskId, graphId, checkTypes);
        
        // 异步执行
        executeIntegrityCheckAsync(taskId, graphId, checkTypes);
        
        return taskId;
    }

    @Override
    public ValidationTaskVO getTaskStatus(String taskId) {
        ValidationTaskVO task = taskStore.get(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在: taskId=" + taskId);
        }
        return task;
    }

    @Override
    public List<ValidationTaskVO> listTasks(String graphId) {
        return taskStore.values().stream()
                .filter(t -> t.getGraphId().equals(graphId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    @Async
    protected void executeIntegrityCheckAsync(String taskId, String graphId, List<String> checkTypes) {
        ValidationTaskVO task = taskStore.get(taskId);
        if (task == null) {
            log.error("任务不存在: taskId={}", taskId);
            return;
        }

        try {
            // 更新状态为运行中
            task.setStatus("RUNNING");
            log.info("开始执行完整性检查: taskId={}, graphId={}", taskId, graphId);

            // 执行验证
            List<GraphIntegrityResultVO> results = integrityValidator.validate(graphId, checkTypes);

            // 判断是否全部通过
            boolean allPassed = results.stream().allMatch(GraphIntegrityResultVO::isPassed);
            int totalViolations = results.stream().mapToInt(GraphIntegrityResultVO::getViolationCount).sum();

            // 更新任务结果
            task.setStatus("COMPLETED");
            task.setResult(GraphIntegrityResultVO.builder()
                    .passed(allPassed)
                    .checkType("AGGREGATED")
                    .violationCount(totalViolations)
                    .violations(results.stream()
                            .flatMap(r -> r.getViolations().stream())
                            .toList())
                    .build());
            task.setCompletedAt(LocalDateTime.now());

            log.info("完整性检查完成: taskId={}, passed={}, violations={}", taskId, allPassed, totalViolations);

        } catch (Exception e) {
            log.error("完整性检查失败: taskId={}, error={}", taskId, e.getMessage(), e);
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
        }
    }
}
