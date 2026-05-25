package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.DataQualityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据质量服务实现
 * 提供节点去重、边去重、实体解析等数据清洗能力
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataQualityServiceImpl implements DataQualityService {

    private final Driver neo4jDriver;

    @Override
    public Map<String, Object> deduplicateNodes(String graphId) {
        // 查找同名同类型的重复节点，保留最新创建的，其余标记为重复
        String findDups =
            "MATCH (n:Entity {graph_id: $graph_id}) " +
            "WITH n.name as name, n.type as type, collect(n) as nodes " +
            "WHERE size(nodes) > 1 " +
            "RETURN name, type, nodes";

        int mergedCount = 0;
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(findDups, Values.parameters("graph_id", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                List<org.neo4j.driver.types.Node> nodes = record.get("nodes").asList(v -> v.asNode());
                if (nodes.size() > 1) {
                    // 保留第一个，将其余节点的关系转移到保留节点
                    String keepUuid = nodes.get(0).get("uuid").asString();
                    for (int i = 1; i < nodes.size(); i++) {
                        String dupUuid = nodes.get(i).get("uuid").asString();
                        mergeNodeRelationships(graphId, keepUuid, dupUuid);
                        deleteDuplicateNode(graphId, dupUuid);
                        mergedCount++;
                    }
                }
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("mergedCount", mergedCount);
        report.put("message", "节点去重完成");
        return report;
    }

    @Override
    public Map<String, Object> deduplicateEdges(String graphId) {
        // 查找相同源目标+类型的重复边，合并为一个
        String findDups =
            "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO]->(b:Entity {graph_id: $graph_id}) " +
            "WITH a.uuid as src, b.uuid as tgt, r.type as type, collect(r) as edges " +
            "WHERE size(edges) > 1 " +
            "RETURN src, tgt, type, edges";

        int mergedCount = 0;
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(findDups, Values.parameters("graph_id", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                List<org.neo4j.driver.types.Relationship> edges = record.get("edges").asList(v -> v.asRelationship());
                if (edges.size() > 1) {
                    // 保留第一条边，删除其余
                    for (int i = 1; i < edges.size(); i++) {
                        String dupUuid = edges.get(i).get("uuid").asString();
                        deleteDuplicateEdge(graphId, dupUuid);
                        mergedCount++;
                    }
                }
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("mergedCount", mergedCount);
        report.put("message", "边去重完成");
        return report;
    }

    @Override
    public Map<String, Object> resolveEntities(String graphId) {
        // 简化的实体解析：基于 Levenshtein 距离（使用 CONTAINS 近似）
        // 查找名称相似度高的节点，标记为同一实体
        String findSimilar =
            "MATCH (n:Entity {graph_id: $graph_id}) " +
            "MATCH (m:Entity {graph_id: $graph_id}) " +
            "WHERE n <> m AND n.name <> m.name AND (" +
            "  n.name CONTAINS m.name OR m.name CONTAINS n.name" +
            ") " +
            "RETURN n.uuid as n1, n.name as name1, m.uuid as n2, m.name as name2";

        int resolvedCount = 0;
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(findSimilar, Values.parameters("graph_id", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                String n1 = record.get("n1").asString();
                String n2 = record.get("n2").asString();
                // 创建 SAME_AS 关系标记实体解析
                String mergeCypher =
                    "MATCH (a:Entity {graph_id: $graph_id, uuid: $n1}) " +
                    "MATCH (b:Entity {graph_id: $graph_id, uuid: $n2}) " +
                    "MERGE (a)-[:SAME_AS]->(b)";
                session.run(mergeCypher, Values.parameters("graph_id", graphId, "n1", n1, "n2", n2));
                resolvedCount++;
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("resolvedCount", resolvedCount);
        report.put("message", "实体解析完成");
        return report;
    }

    @Override
    public List<Map<String, Object>> findOrphanNodes(String graphId) {
        String cypher =
            "MATCH (n:Entity {graph_id: $graph_id}) " +
            "WHERE NOT (n)-[:RELATES_TO]-() " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type";

        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId));
            while (result.hasNext()) {
                results.add(result.next().asMap());
            }
        }
        return results;
    }

    @Override
    public Map<String, Object> fixOrphanNodes(String graphId, boolean deleteOrphans) {
        List<Map<String, Object>> orphans = findOrphanNodes(graphId);
        int fixedCount = 0;

        try (Session session = neo4jDriver.session()) {
            for (Map<String, Object> orphan : orphans) {
                String uuid = (String) orphan.get("uuid");
                if (deleteOrphans) {
                    String deleteCypher =
                        "MATCH (n:Entity {graph_id: $graph_id, uuid: $uuid}) DELETE n";
                    session.run(deleteCypher, Values.parameters("graph_id", graphId, "uuid", uuid));
                } else {
                    // 添加一个自环标记为孤立节点
                    String selfLoopCypher =
                        "MATCH (n:Entity {graph_id: $graph_id, uuid: $uuid}) " +
                        "CREATE (n)-[:RELATES_TO {uuid: $relUuid, type: 'ISOLATED', fact: '孤立节点'}]->(n)";
                    session.run(selfLoopCypher, Values.parameters(
                        "graph_id", graphId,
                        "uuid", uuid,
                        "relUuid", java.util.UUID.randomUUID().toString().replace("-", "")
                    ));
                }
                fixedCount++;
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("fixedCount", fixedCount);
        report.put("deleteMode", deleteOrphans);
        report.put("message", deleteOrphans ? "已删除孤立节点" : "已为孤立节点添加自环");
        return report;
    }

    // ==================== 私有方法 ====================

    private void mergeNodeRelationships(String graphId, String keepUuid, String dupUuid) {
        // 将重复节点的所有关系转移到保留节点
        String transferCypher =
            "MATCH (dup:Entity {graph_id: $graph_id, uuid: $dupUuid}) " +
            "MATCH (keep:Entity {graph_id: $graph_id, uuid: $keepUuid}) " +
            "MATCH (dup)-[r:RELATES_TO]->(other:Entity {graph_id: $graph_id}) " +
            "WHERE other <> dup " +
            "CREATE (keep)-[nr:RELATES_TO]->(other) SET nr = properties(r) " +
            "DELETE r";
        try (Session session = neo4jDriver.session()) {
            session.run(transferCypher, Values.parameters("graph_id", graphId, "dupUuid", dupUuid, "keepUuid", keepUuid));
        }

        String transferIncomingCypher =
            "MATCH (dup:Entity {graph_id: $graph_id, uuid: $dupUuid}) " +
            "MATCH (keep:Entity {graph_id: $graph_id, uuid: $keepUuid}) " +
            "MATCH (other:Entity {graph_id: $graph_id})-[r:RELATES_TO]->(dup) " +
            "WHERE other <> dup " +
            "CREATE (other)-[nr:RELATES_TO]->(keep) SET nr = properties(r) " +
            "DELETE r";
        try (Session session = neo4jDriver.session()) {
            session.run(transferIncomingCypher, Values.parameters("graph_id", graphId, "dupUuid", dupUuid, "keepUuid", keepUuid));
        }
    }

    private void deleteDuplicateNode(String graphId, String uuid) {
        String cypher = "MATCH (n:Entity {graph_id: $graph_id, uuid: $uuid}) DETACH DELETE n";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId, "uuid", uuid));
        }
    }

    private void deleteDuplicateEdge(String graphId, String uuid) {
        String cypher = "MATCH ()-[r:RELATES_TO {uuid: $uuid}]->() DELETE r";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("uuid", uuid));
        }
    }
}
