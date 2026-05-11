package com.graphiti.module.graphiti.namespace.node;

import com.graphiti.module.graphiti.service.CommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

/**
 * 社区节点命名空间
 * 对应 Python: graphiti.nodes.community
 *
 * <p>社区节点由社区发现算法（Louvain/Leiden）自动生成，
 * 聚合语义相近的实体节点。
 */
@Slf4j
@RequiredArgsConstructor
public class CommunityNodeNamespace {

    private final CommunityService communityService;

    /**
     * 构建社区（执行社区发现算法）
     */
    public Map<String, Object> build(String graphId) {
        log.debug("CommunityNodeNamespace.build: graphId={}", graphId);
        return communityService.buildCommunities(graphId);
    }

    /**
     * 获取社区列表
     */
    public List<Map<String, Object>> list(String graphId) {
        return communityService.listCommunities(graphId);
    }

    /**
     * 搜索社区
     */
    public List<Map<String, Object>> search(String graphId, String query) {
        return communityService.searchCommunities(graphId, query);
    }
}
