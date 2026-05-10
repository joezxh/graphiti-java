package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.temporal.TemporalEdgeVO;
import com.graphiti.module.graphiti.vo.temporal.TemporalNodeVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 时序事实管理服务接口
 */
public interface TemporalService {

    /**
     * 标记与指定实体相关的旧事实为失效
     */
    void invalidateFacts(String graphId, List<String> entityNames, LocalDateTime invalidAt);

    /**
     * 查询当前有效的节点（默认当前时间）
     */
    List<TemporalNodeVO> getValidNodes(String graphId);

    /**
     * 查询指定时间有效的节点
     */
    List<TemporalNodeVO> getValidNodesAt(String graphId, LocalDateTime referenceTime);

    /**
     * 查询指定时间有效的边
     */
    List<TemporalEdgeVO> getValidEdgesAt(String graphId, LocalDateTime referenceTime);

    /**
     * 获取实体的事实版本链
     */
    List<TemporalNodeVO> getFactVersions(String graphId, String entityName);
}
