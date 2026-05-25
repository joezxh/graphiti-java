package com.ontograph.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * 图数据库驱动抽象接口
 * 屏蔽底层图数据库（Neo4j / 其他）的实现差异
 */
public interface GraphDriverService {

    /**
     * 创建实体节点
     */
    Map<String, Object> createNode(String graphId, String uuid, String name, String type,
                                     String summary, float[] embedding, Map<String, Object> properties);

    /**
     * 创建关系边
     */
    Map<String, Object> createEdge(String graphId, String edgeUuid, String sourceUuid, String targetUuid,
                                     String type, String fact, float[] embedding, Map<String, Object> properties);

    /**
     * 删除节点
     */
    void deleteNode(String graphId, String uuid);

    /**
     * 删除边
     */
    void deleteEdge(String graphId, String uuid);

    /**
     * 查询节点
     */
    Map<String, Object> getNode(String graphId, String uuid);

    /**
     * 查询边
     */
    Map<String, Object> getEdge(String graphId, String uuid);

    /**
     * 搜索节点（全文）
     */
    List<Map<String, Object>> searchNodes(String graphId, String query, int limit);

    /**
     * 搜索边（全文）
     */
    List<Map<String, Object>> searchEdges(String graphId, String query, int limit);

    /**
     * 向量搜索节点
     */
    List<Map<String, Object>> searchNodesByVector(String graphId, float[] embedding, int limit);

    /**
     * 向量搜索边
     */
    List<Map<String, Object>> searchEdgesByVector(String graphId, float[] embedding, int limit);

    /**
     * 获取驱动名称
     */
    String getDriverName();
}
