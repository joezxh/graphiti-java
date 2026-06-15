package com.ontograph.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * 时序管理服务接口
 * 管理实体和关系的时间有效性
 *
 * <p>参考 Python 实现：graphiti_core/utils/maintenance/edge_operations.py
 *
 * <p>核心概念：
 * <ul>
 *   <li>valid_at: 事实开始有效的时间</li>
 *   <li>invalid_at: 事实失效的时间</li>
 *   <li>expired_at: 边被明确失效的时间戳</li>
 * </ul>
 */
public interface TemporalService {

    /**
     * 失效指定名称的实体（当新 Episode 中的实体与已有实体同名时自动触发）
     * @param graphId 图谱ID
     * @param entityNames 实体名称列表
     */
    void invalidateFacts(String graphId, List<String> entityNames);

    /**
     * 失效与指定节点相关的边
     * @param graphId 图谱ID
     * @param nodeUuids 节点UUID列表
     */
    void invalidateEdgesByNodes(String graphId, List<String> nodeUuids);

    /**
     * 解决边的矛盾冲突
     *
     * <p>根据时间信息判断新边是否使旧边失效：
     * <ul>
     *   <li>Case 1: 无时间重叠 → 不失效</li>
     *   <li>Case 2: 旧边 invalid_at <= 新边 valid_at → 不失效</li>
     *   <li>Case 3: 新边 valid_at 更晚 → 旧边失效</li>
     * </ul>
     *
     * @param graphId 图谱ID
     * @param newEdge 新边信息（包含 valid_at）
     * @param candidateEdges 候选旧边列表
     * @return 需要失效的旧边 UUID 列表
     */
    List<String> resolveEdgeContradictions(String graphId, Map<String, Object> newEdge,
                                          List<Map<String, Object>> candidateEdges);

    /**
     * 批量处理边的失效
     *
     * <p>根据 resolvedEdges 和 invalidationCandidates 批量更新边的 invalid_at 和 expired_at
     *
     * @param graphId 图谱ID
     * @param expiredEdges 需要失效的边 UUID 列表
     * @param expiredAt 失效时间戳（毫秒）
     */
    void expireEdges(String graphId, List<String> expiredEdges, long expiredAt);

    /**
     * 获取当前有效的节点列表
     * @param graphId 图谱ID
     * @return 有效节点列表
     */
    List<Map<String, Object>> getCurrentFacts(String graphId);

    /**
     * 获取指定时间点的有效节点列表
     * @param graphId 图谱ID
     * @param referenceTime 参考时间戳（毫秒）
     * @return 有效节点列表
     */
    List<Map<String, Object>> getFactsAtTime(String graphId, long referenceTime);

    /**
     * 获取指定时间点的有效边列表
     * @param graphId 图谱ID
     * @param referenceTime 参考时间戳（毫秒）
     * @return 有效边列表
     */
    List<Map<String, Object>> getRelationshipsAtTime(String graphId, long referenceTime);

    /**
     * 获取实体的历史版本
     * @param graphId 图谱ID
     * @param entityName 实体名称
     * @return 历史版本列表（按时间倒序）
     */
    List<Map<String, Object>> getFactHistory(String graphId, String entityName);
}
