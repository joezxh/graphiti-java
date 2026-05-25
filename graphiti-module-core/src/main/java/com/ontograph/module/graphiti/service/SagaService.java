package com.ontograph.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * Saga 管理服务接口
 * 管理 Episode 之间的时序链关系
 */
public interface SagaService {

    /**
     * 构建 Saga 时序链（按 valid_at 排序的 Episode 链）
     * @param graphId 图谱ID
     * @return Saga 构建报告
     */
    Map<String, Object> buildSaga(String graphId);

    /**
     * 获取指定 Episode 的 Saga 上下文（前后相邻 Episode）
     * @param graphId 图谱ID
     * @param episodeUuid Episode UUID
     * @return Saga 上下文
     */
    Map<String, Object> getSagaContext(String graphId, String episodeUuid);

    /**
     * 获取 Saga 时间线（按顺序的 Episode 列表）
     * @param graphId 图谱ID
     * @return Episode 列表
     */
    List<Map<String, Object>> getSagaTimeline(String graphId);

    /**
     * 删除 Saga 链
     * @param graphId 图谱ID
     */
    void clearSaga(String graphId);
}
