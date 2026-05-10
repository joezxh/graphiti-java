package com.graphiti.module.graphiti.service;

import java.util.List;
import java.util.Map;

/**
 * 社区发现服务接口
 */
public interface CommunityService {

    /**
     * 构建社区
     * @param graphId 图谱ID
     * @return 构建结果
     */
    Map<String, Object> buildCommunities(String graphId);

    /**
     * 获取社区列表
     * @param graphId 图谱ID
     * @return 社区列表
     */
    List<Map<String, Object>> listCommunities(String graphId);

    /**
     * 搜索社区
     * @param graphId 图谱ID
     * @param query 搜索关键词
     * @return 社区列表
     */
    List<Map<String, Object>> searchCommunities(String graphId, String query);

    /**
     * 删除所有社区
     * @param graphId 图谱ID
     */
    void removeCommunities(String graphId);
}
