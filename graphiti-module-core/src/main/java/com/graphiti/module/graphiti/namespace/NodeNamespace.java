package com.graphiti.module.graphiti.namespace;

import com.graphiti.module.graphiti.namespace.node.EntityNodeNamespace;
import com.graphiti.module.graphiti.namespace.node.EpisodeNodeNamespace;
import com.graphiti.module.graphiti.namespace.node.CommunityNodeNamespace;
import com.graphiti.module.graphiti.service.CommunityService;
import com.graphiti.module.graphiti.service.EdgeService;
import com.graphiti.module.graphiti.service.EmbedderService;
import com.graphiti.module.graphiti.service.EpisodeService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.NodeService;
import lombok.Getter;

/**
 * 节点命名空间根
 * 对应 Python: graphiti.nodes
 *
 * <p>聚合 entity、episode、community 三个子命名空间。
 * Spring 初始化时自动注入依赖，子空间通过构造函数传递共享的服务实例。
 */
@Getter
public class NodeNamespace {

    private final EntityNodeNamespace entity;
    private final EpisodeNodeNamespace episode;
    private final CommunityNodeNamespace community;

    public NodeNamespace(
            NodeService nodeService,
            EmbedderService embedderService,
            GraphNeo4jService graphNeo4jService,
            EpisodeService episodeService,
            CommunityService communityService) {
        this.entity = new EntityNodeNamespace(nodeService, embedderService, graphNeo4jService);
        this.episode = new EpisodeNodeNamespace(episodeService, graphNeo4jService);
        this.community = new CommunityNodeNamespace(communityService);
    }
}
