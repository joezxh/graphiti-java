package com.graphiti.module.graphiti.namespace;

import com.graphiti.module.graphiti.namespace.graph.GraphMetadataNamespace;
import com.graphiti.module.graphiti.namespace.graph.OntologyNamespace;
import com.graphiti.module.graphiti.service.GraphitiService;
import com.graphiti.module.graphiti.service.OntologyService;
import lombok.Getter;

/**
 * 图谱命名空间根
 * 对应 Python: graphiti.graphs
 *
 * <p>聚合 metadata 和 ontology 两个子命名空间。
 */
@Getter
public class GraphNamespace {

    private final GraphMetadataNamespace metadata;
    private final OntologyNamespace ontology;

    public GraphNamespace(GraphitiService graphitiService, OntologyService ontologyService) {
        this.metadata = new GraphMetadataNamespace(graphitiService);
        this.ontology = new OntologyNamespace(ontologyService);
    }
}
