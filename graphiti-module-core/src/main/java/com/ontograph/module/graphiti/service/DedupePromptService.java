package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.dedup.EdgeDedupeResultVO;
import com.graphiti.module.graphiti.vo.dedup.NodeDedupeResultVO;

import java.util.List;
import java.util.Map;

/**
 * 去重提示词服务接口
 *
 * <p>提供基于 LLM 的实体去重和边去重功能，从数据库中的提示词模板获取提示词。
 *
 * <p>对应 Python 文件：
 * <ul>
 *   <li>dedupe_nodes.py - 节点去重（node, nodes, node_list 函数）</li>
 *   <li>dedupe_edges.py - 边去重（resolve_edge 函数）</li>
 * </ul>
 */
public interface DedupePromptService {

    // ========== 节点去重（单实体 vs 现有实体）==========

    /**
     * 单实体去重
     *
     * <p>将新提取的单个实体与现有实体进行去重比较
     *
     * <p>对应 Python: dedupe_nodes.node(context)
     *
     * @param context 去重上下文，包含：
     *                - previousEpisodes: 历史上下文
     *                - episodeContent: 当前消息内容
     *                - extractedNode: 新提取的实体
     *                - entityTypeDescription: 实体类型描述
     *                - existingNodes: 现有实体列表
     * @return 去重结果
     */
    NodeDedupeResultVO.NodeDuplicate deduplicateSingleNode(Map<String, Object> context);

    // ========== 节点去重（批量实体 vs 现有实体）==========

    /**
     * 批量实体去重
     *
     * <p>将新提取的多个实体与现有实体进行去重比较
     *
     * <p>对应 Python: dedupe_nodes.nodes(context)
     *
     * @param context 去重上下文，包含：
     *                - previousEpisodes: 历史上下文
     *                - episodeContent: 当前消息内容
     *                - extractedNodes: 新提取的实体列表
     *                - existingNodes: 现有实体列表
     * @return 去重结果
     */
    NodeDedupeResultVO deduplicateNodes(Map<String, Object> context);

    // ========== 节点分组去重 ==========

    /**
     * 节点列表分组去重
     *
     * <p>对节点列表进行去重分组，将重复节点归为一组
     *
     * <p>对应 Python: dedupe_nodes.node_list(context)
     *
     * @param nodes 待分组的节点列表
     * @return 分组结果，每个分组包含 uuids 和综合 summary
     */
    List<Map<String, Object>> groupDuplicateNodes(List<Map<String, Object>> nodes);

    // ========== 边去重 ==========

    /**
     * 边去重
     *
     * <p>检测新边与现有边是否重复或矛盾
     *
     * <p>对应 Python: dedupe_edges.resolve_edge(context)
     *
     * @param context 去重上下文，包含：
     *                - existingEdges: 现有边列表
     *                - edgeInvalidationCandidates: 边失效候选列表
     *                - newEdge: 新边
     * @return 去重结果，包含 duplicateFacts 和 contradictedFacts
     */
    EdgeDedupeResultVO deduplicateEdge(Map<String, Object> context);

    // ========== 辅助方法 ==========

    /**
     * 将节点列表转换为候选格式
     *
     * @param nodes 节点列表
     * @return 转换后的候选列表
     */
    List<Map<String, Object>> convertToNodeCandidates(List<Map<String, Object>> nodes);

    /**
     * 将边列表转换为事实格式
     *
     * @param edges 边列表
     * @return 转换后的事实列表
     */
    List<Map<String, Object>> convertToEdgeFacts(List<Map<String, Object>> edges);
}
