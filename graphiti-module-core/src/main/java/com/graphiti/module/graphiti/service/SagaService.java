package com.graphiti.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * Saga管理服务接口
 */
public interface SagaService {

    /**
     * 创建Saga
     */
    Map<String, Object> createSaga(String graphId, String name, List<String> episodeUuids);

    /**
     * 获取Saga列表
     */
    List<Map<String, Object>> listSagas(String graphId);

    /**
     * 获取Saga的Episode链
     */
    List<Map<String, Object>> getSagaEpisodes(String sagaUuid);
}
