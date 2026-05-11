package com.graphiti.module.graphiti.namespace;

import com.graphiti.module.graphiti.namespace.edge.EntityEdgeNamespace;
import com.graphiti.module.graphiti.service.EdgeService;
import com.graphiti.module.graphiti.service.EmbedderService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import lombok.Getter;

/**
 * 边命名空间根
 * 对应 Python: graphiti.edges
 *
 * <p>聚合 entity、episodic、community、has_episode、next_episode 五个子命名空间。
 */
@Getter
public class EdgeNamespace {

    private final EntityEdgeNamespace entity;

    public EdgeNamespace(
            EdgeService edgeService,
            EmbedderService embedderService,
            GraphNeo4jService graphNeo4jService) {
        this.entity = new EntityEdgeNamespace(edgeService, embedderService, graphNeo4jService);
    }
}
