package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.GraphDriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class Neo4jDriverAdapter implements GraphDriverService {

    private final Driver neo4jDriver;

    @Override
    public Map<String, Object> createNode(String graphId, String uuid, String name, String type, Map<String, Object> properties) {
        String cypher = "CREATE (n:Entity {group_id: $group_id, uuid: $uuid, name: $name, type: $type}) SET n += $props RETURN n";

        try (Session session = neo4jDriver.session()) {
            var result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "uuid", uuid,
                "name", name,
                "type", type,
                "props", properties != null ? properties : new HashMap<>()
            ));
            return result.single().get("n").asMap();
        }
    }

    @Override
    public Map<String, Object> createEdge(String graphId, String sourceUuid, String targetUuid, String type, Map<String, Object> properties) {
        String cypher =
            "MATCH (a:Entity {group_id: $group_id, uuid: $source_uuid}) " +
            "MATCH (b:Entity {group_id: $group_id, uuid: $target_uuid}) " +
            "CREATE (a)-[r:RELATES_TO {type: $type}]->(b) SET r += $props RETURN r";

        try (Session session = neo4jDriver.session()) {
            var result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "source_uuid", sourceUuid,
                "target_uuid", targetUuid,
                "type", type,
                "props", properties != null ? properties : new HashMap<>()
            ));
            return result.single().get("r").asMap();
        }
    }

    @Override
    public List<Map<String, Object>> queryNodes(String graphId, String name, String type, int offset, int limit) {
        StringBuilder cypher = new StringBuilder("MATCH (n:Entity {group_id: $group_id}) ");
        if (name != null && !name.isEmpty()) {
            cypher.append("WHERE n.name CONTAINS $name ");
        }
        if (type != null && !type.isEmpty()) {
            cypher.append(name != null ? "AND " : "WHERE ");
            cypher.append("n.type = $type ");
        }
        cypher.append("RETURN n SKIP $offset LIMIT $limit");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("group_id", graphId);
        paramMap.put("offset", offset);
        paramMap.put("limit", limit);
        if (name != null) paramMap.put("name", name);
        if (type != null) paramMap.put("type", type);

        List<Map<String, Object>> nodes = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var result = session.run(cypher.toString(), Values.parameters(paramMap));
            while (result.hasNext()) {
                nodes.add(result.next().get("n").asMap());
            }
        }
        return nodes;
    }

    @Override
    public List<Map<String, Object>> queryEdges(String graphId, String sourceUuid, String targetUuid, String type, int offset, int limit) {
        String cypher =
            "MATCH (a:Entity {group_id: $group_id})-[r:RELATES_TO]->(b:Entity {group_id: $group_id}) " +
            "RETURN r, a.uuid as source_uuid, b.uuid as target_uuid SKIP $offset LIMIT $limit";

        List<Map<String, Object>> edges = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            var result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "offset", offset,
                "limit", limit
            ));
            while (result.hasNext()) {
                var record = result.next();
                Map<String, Object> edge = new HashMap<>(record.get("r").asMap());
                edge.put("sourceUuid", record.get("source_uuid").asString());
                edge.put("targetUuid", record.get("target_uuid").asString());
                edges.add(edge);
            }
        }
        return edges;
    }

    @Override
    public void clearGraph(String graphId) {
        String cypher = "MATCH (n:Entity {group_id: $group_id}) DETACH DELETE n";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("group_id", graphId));
            log.info("已清空图谱 {}", graphId);
        }
    }
}
