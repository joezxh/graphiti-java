package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.dto.CommunityCreateContext;

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

    /**
     * 删除单个社区
     * @param graphId 图谱ID
     * @param communityUuid 社区UUID
     */
    void deleteCommunity(String graphId, String communityUuid);

    /**
     * 获取社区层级树（用于 IDE 左侧树形菜单）
     * 按 domain_type 一级分组，community_type 二级分组，每组返回 count 和 children
     * @param graphId 图谱ID
     * @param dimension 维度过滤（可选：domain|region|scenario）
     * @return 分层树形结构的社区列表
     */
    List<Map<String, Object>> getCommunityHierarchy(String graphId, String dimension);

    /**
     * 获取单个社区详情
     * @param graphId 图谱ID
     * @param communityUuid 社区UUID
     * @return 社区完整信息（含成员节点列表）
     */
    Map<String, Object> getCommunityDetail(String graphId, String communityUuid);

    /**
     * 创建社区节点
     * @param graphId 图谱ID
     * @param body 社区信息
     * @return 创建的社区
     */
    Map<String, Object> createCommunity(String graphId, Map<String, Object> body);

    /**
     * 更新社区节点
     * @param graphId 图谱ID
     * @param communityUuid 社区UUID
     * @param body 更新信息
     * @return 更新后的社区
     */
    Map<String, Object> updateCommunity(String graphId, String communityUuid, Map<String, Object> body);

    /**
     * 使用领域推断上下文创建社区
     * @param graphId 图谱ID
     * @param context 社区创建上下文（含 LLM 推断的 domain/region/scenarioType）
     * @return 创建结果
     */
    Map<String, Object> createCommunity(String graphId, CommunityCreateContext context);
}
