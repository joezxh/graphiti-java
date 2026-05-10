package com.graphiti.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * 数据质量保障服务接口
 */
public interface DataQualityService {

    /**
     * 去重节点
     */
    int deduplicateNodes(String graphId);

    /**
     * 去重边
     */
    int deduplicateEdges(String graphId);

    /**
     * 实体解析
     */
    List<Map<String, Object>> resolveEntities(String graphId, List<String> entityNames);
}
