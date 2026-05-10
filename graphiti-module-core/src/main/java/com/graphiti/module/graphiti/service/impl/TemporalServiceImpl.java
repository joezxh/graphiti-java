package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.TemporalService;
import com.graphiti.module.graphiti.vo.temporal.TemporalEdgeVO;
import com.graphiti.module.graphiti.vo.temporal.TemporalNodeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemporalServiceImpl implements TemporalService {

    private final Driver neo4jDriver;

    @Override
    public void invalidateFacts(String graphId, List<String> entityNames, LocalDateTime invalidAt) {
        String cypher =
            "MATCH (n:Entity {group_id: $group_id}) " +
            "WHERE n.name IN $entityNames AND (n.invalid_at IS NULL OR n.invalid_at > $invalid_at) " +
            "SET n.invalid_at = $invalid_at";

        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters(
                "group_id", graphId,
                "entityNames", entityNames,
                "invalid_at", invalidAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            ));
            log.info("已标记 {} 个实体的旧事实为失效", entityNames.size());
        }
    }

    @Override
    public List<TemporalNodeVO> getValidNodes(String graphId) {
        return getValidNodesAt(graphId, LocalDateTime.now());
    }

    @Override
    public List<TemporalNodeVO> getValidNodesAt(String graphId, LocalDateTime referenceTime) {
        String cypher =
            "MATCH (n:Entity {group_id: $group_id}) " +
            "WHERE n.valid_at <= $reference_time " +
            "  AND (n.invalid_at IS NULL OR n.invalid_at > $reference_time) " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, " +
            "       n.summary as summary, n.valid_at as valid_at, n.invalid_at as invalid_at, " +
            "       properties(n) as props " +
            "ORDER BY n.valid_at DESC";

        long refTime = referenceTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        List<TemporalNodeVO> nodes = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "reference_time", refTime
            ));
            while (result.hasNext()) {
                Record record = result.next();
                TemporalNodeVO node = new TemporalNodeVO();
                node.setUuid(record.get("uuid").asString());
                node.setName(record.get("name").asString());
                node.setType(record.get("type").asString());
                node.setSummary(record.get("summary").asString(null));
                node.setValidAt(millisToLocalDateTime(record.get("valid_at").asLong(0)));
                long invalidAt = record.get("invalid_at").asLong(0);
                if (invalidAt > 0) {
                    node.setInvalidAt(millisToLocalDateTime(invalidAt));
                }
                node.setProperties(record.get("props").asMap());
                nodes.add(node);
            }
        }
        return nodes;
    }

    @Override
    public List<TemporalEdgeVO> getValidEdgesAt(String graphId, LocalDateTime referenceTime) {
        String cypher =
            "MATCH (a:Entity {group_id: $group_id})-[r:RELATES_TO]->(b:Entity {group_id: $group_id}) " +
            "WHERE r.valid_at <= $reference_time " +
            "  AND (r.invalid_at IS NULL OR r.invalid_at > $reference_time) " +
            "RETURN r.uuid as uuid, a.uuid as source_uuid, b.uuid as target_uuid, " +
            "       r.type as type, r.fact as fact, r.valid_at as valid_at, r.invalid_at as invalid_at " +
            "ORDER BY r.valid_at DESC";

        long refTime = referenceTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        List<TemporalEdgeVO> edges = new ArrayList<>();

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "reference_time", refTime
            ));
            while (result.hasNext()) {
                Record record = result.next();
                TemporalEdgeVO edge = new TemporalEdgeVO();
                edge.setUuid(record.get("uuid").asString());
                edge.setSourceUuid(record.get("source_uuid").asString());
                edge.setTargetUuid(record.get("target_uuid").asString());
                edge.setType(record.get("type").asString());
                edge.setFact(record.get("fact").asString(null));
                edge.setValidAt(millisToLocalDateTime(record.get("valid_at").asLong(0)));
                long invalidAt = record.get("invalid_at").asLong(0);
                if (invalidAt > 0) {
                    edge.setInvalidAt(millisToLocalDateTime(invalidAt));
                }
                edges.add(edge);
            }
        }
        return edges;
    }

    @Override
    public List<TemporalNodeVO> getFactVersions(String graphId, String entityName) {
        String cypher =
            "MATCH (n:Entity {group_id: $group_id, name: $name}) " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, " +
            "       n.summary as summary, n.valid_at as valid_at, n.invalid_at as invalid_at " +
            "ORDER BY n.valid_at DESC";

        List<TemporalNodeVO> versions = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "name", entityName
            ));
            while (result.hasNext()) {
                Record record = result.next();
                TemporalNodeVO node = new TemporalNodeVO();
                node.setUuid(record.get("uuid").asString());
                node.setName(record.get("name").asString());
                node.setType(record.get("type").asString());
                node.setSummary(record.get("summary").asString(null));
                node.setValidAt(millisToLocalDateTime(record.get("valid_at").asLong(0)));
                long invalidAt = record.get("invalid_at").asLong(0);
                if (invalidAt > 0) {
                    node.setInvalidAt(millisToLocalDateTime(invalidAt));
                }
                versions.add(node);
            }
        }
        return versions;
    }

    private LocalDateTime millisToLocalDateTime(long millis) {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(millis),
            ZoneId.systemDefault()
        );
    }
}
