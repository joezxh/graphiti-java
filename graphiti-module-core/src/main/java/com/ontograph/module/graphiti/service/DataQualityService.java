package com.graphiti.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * 数据质量服务接口
 * 提供节点去重、边去重、实体解析等数据清洗能力
 */
public interface DataQualityService {

    /**
     * 检测并合并重复节点（基于名称相似度）
     * @param graphId 图谱ID
     * @return 合并报告
     */
    Map<String, Object> deduplicateNodes(String graphId);

    /**
     * 检测并合并重复边（相同源目标+类型）
     * @param graphId 图谱ID
     * @return 合并报告
     */
    Map<String, Object> deduplicateEdges(String graphId);

    /**
     * 实体解析：将指代同一实体的不同名称归一化
     * @param graphId 图谱ID
     * @return 解析报告
     */
    Map<String, Object> resolveEntities(String graphId);

    /**
     * 获取孤立节点（无边连接的节点）
     * @param graphId 图谱ID
     * @return 孤立节点列表
     */
    List<Map<String, Object>> findOrphanNodes(String graphId);

    /**
     * 修复孤立节点（删除或添加自环）
     * @param graphId 图谱ID
     * @param deleteOrphans 是否删除孤立节点
     * @return 修复报告
     */
    Map<String, Object> fixOrphanNodes(String graphId, boolean deleteOrphans);
}
