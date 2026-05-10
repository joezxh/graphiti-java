package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.GraphDriverService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import lombok.RequiredArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Neo4j 驱动适配器
 * 将 GraphNeo4jService 适配为 GraphDriverService 接口
 */
@Service
@RequiredArgsConstructor
public class Neo4jDriverAdapter implements GraphDriverService {

    private final GraphNeo4jService graphNeo4jService;
    private final Driver neo4jDriver;

    @Override
    public Map<String, Object> createNode(String graphId, String uuid, String name, String type,
                                          String summary, float[] embedding, Map<String, Object> properties) {
        return graphNeo4jService.createEntityNode(graphId, uuid, name, type, summary, embedding, properties);
    }

    @Override
    public Map<String, Object> createEdge(String graphId, String edgeUuid, String sourceUuid, String targetUuid,
                                          String type, String fact, float[] embedding, Map<String, Object> properties) {
        return graphNeo4jService.createRelationship(graphId, edgeUuid, sourceUuid, targetUuid, type, fact, embedding, properties);
    }

    @Override
    public void deleteNode(String graphId, String uuid) {
        String cypher = "MATCH (n:Entity {group_id: $group_id, uuid: $uuid}) DETACH DELETE n";
        try (var session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("group_id", graphId, "uuid", uuid));
        }
    }

    @Override
    public void deleteEdge(String graphId, String uuid) {
        graphNeo4jService.deleteEdge(graphId, uuid);
    }

    @Override
    public Map<String, Object> getNode(String graphId, String uuid) {
        return graphNeo4jService.getNodeByUuid(uuid);
    }

    @Override
    public Map<String, Object> getEdge(String graphId, String uuid) {
        return graphNeo4jService.getEdgeByUuid(graphId, uuid);
    }

    @Override
    public List<Map<String, Object>> searchNodes(String graphId, String query, int limit) {
        return graphNeo4jService.searchNodesByFulltext(graphId, query, limit);
    }

    @Override
    public List<Map<String, Object>> searchEdges(String graphId, String query, int limit) {
        return graphNeo4jService.searchEdgesByFulltext(graphId, query, limit);
    }

    @Override
    public List<Map<String, Object>> searchNodesByVector(String graphId, float[] embedding, int limit) {
        return graphNeo4jService.searchNodesByVector(graphId, embedding, limit);
    }

    @Override
    public List<Map<String, Object>> searchEdgesByVector(String graphId, float[] embedding, int limit) {
        return graphNeo4jService.searchEdgesByVector(graphId, embedding, limit);
    }

    @Override
    public String getDriverName() {
        return "neo4j";
    }
}
