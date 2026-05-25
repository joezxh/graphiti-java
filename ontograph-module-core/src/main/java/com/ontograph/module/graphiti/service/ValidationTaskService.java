package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.ontology.ValidationTaskVO;

import java.util.List;

public interface ValidationTaskService {

    /**
     * 提交异步完整性检查任务
     */
    String submitIntegrityCheck(String graphId, List<String> checkTypes);

    /**
     * 查询任务状态和结果
     */
    ValidationTaskVO getTaskStatus(String taskId);

    /**
     * 列出图谱的所有验证任务
     */
    List<ValidationTaskVO> listTasks(String graphId);
}
