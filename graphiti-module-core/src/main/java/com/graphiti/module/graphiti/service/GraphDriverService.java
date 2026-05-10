package com.graphiti.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * 图数据库驱动抽象接口
 */
public interface GraphDriverService {

    /**
     * 创建节点
     */
    Map<String, Object> createNode(String graphId, String uuid, String name, String type, Map<String, Object> properties);

    /**
     * 创建关系
     */
    Map<String, Object> createEdge(String graphId, String sourceUuid, String targetUuid, String type, Map<String, Object> properties);

    /**
     * 查询节点
     */
    List<Map<String, Object>> queryNodes(String graphId, String name, String type, int offset, int limit);

    /**
     * 查询关系
     */
    List<Map<String, Object>> queryEdges(String graphId, String sourceUuid, String targetUuid, String type, int offset, int limit);

    /**
     * 删除图谱数据
     */
    void clearGraph(String graphId);
}
