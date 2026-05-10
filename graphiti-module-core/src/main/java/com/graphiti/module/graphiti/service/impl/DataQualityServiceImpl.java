package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.DataQualityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityServiceImpl implements DataQualityService {

    private final Driver neo4jDriver;

    @Override
    public int deduplicateNodes(String graphId) {
        String cypher =
            "MATCH (n:Entity {group_id: $group_id}) " +
            "WITH n.name as name, n.type as type, collect(n) as nodes " +
            "WHERE size(nodes) > 1 " +
            "WITH nodes " +
            "UNWIND nodes[1..] as duplicate " +
            "DETACH DELETE duplicate";

        try (Session session = neo4jDriver.session()) {
            var result = session.run(cypher, Values.parameters("group_id", graphId));
            log.info("已去重图谱 {} 的节点", graphId);
            return result.consume().counters().nodesDeleted();
        }
    }

    @Override
    public int deduplicateEdges(String graphId) {
        String cypher =
            "MATCH (a:Entity {group_id: $group_id})-[r:RELATES_TO]->(b:Entity {group_id: $group_id}) " +
            "WITH a, b, r.type as type, collect(r) as edges " +
            "WHERE size(edges) > 1 " +
            "WITH edges " +
            "UNWIND edges[1..] as duplicate " +
            "DELETE duplicate";

        try (Session session = neo4jDriver.session()) {
            var result = session.run(cypher, Values.parameters("group_id", graphId));
            log.info("已去重图谱 {} 的边", graphId);
            return result.consume().counters().relationshipsDeleted();
        }
    }

    @Override
    public List<Map<String, Object>> resolveEntities(String graphId, List<String> entityNames) {
        String cypher =
            "MATCH (n:Entity {group_id: $group_id}) " +
            "WHERE n.name IN $entityNames " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type " +
            "ORDER BY n.name";

        try (Session session = neo4jDriver.session()) {
            var result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "entityNames", entityNames
            ));
            return result.list(record -> Map.of(
                "uuid", record.get("uuid").asString(),
                "name", record.get("name").asString(),
                "type", record.get("type").asString()
            ));
        }
    }
}
