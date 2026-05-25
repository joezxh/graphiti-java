package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.TemporalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 时序管理服务实现
 *
 * <p>参考 Python 实现：graphiti_core/utils/maintenance/edge_operations.py
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemporalServiceImpl implements TemporalService {

    private final GraphNeo4jService graphNeo4jService;

    @Override
    public void invalidateFacts(String graphId, List<String> entityNames) {
        if (entityNames == null || entityNames.isEmpty()) {
            return;
        }
        graphNeo4jService.invalidateNodesByName(graphId, entityNames);
    }

    @Override
    public void invalidateEdgesByNodes(String graphId, List<String> nodeUuids) {
        if (nodeUuids == null || nodeUuids.isEmpty()) {
            return;
        }
        graphNeo4jService.invalidateEdgesByNodes(graphId, nodeUuids);
    }

    @Override
    public List<String> resolveEdgeContradictions(String graphId, Map<String, Object> newEdge,
                                                List<Map<String, Object>> candidateEdges) {
        if (candidateEdges == null || candidateEdges.isEmpty()) {
            return new ArrayList<>();
        }

        // 解析新边的时间信息
        Long newValidAt = parseTimestamp(newEdge.get("valid_at"));
        if (newValidAt == null) {
            newValidAt = System.currentTimeMillis();
        }

        Long newInvalidAt = parseTimestamp(newEdge.get("invalid_at"));
        List<String> expiredEdgeUuids = new ArrayList<>();

        for (Map<String, Object> candidateEdge : candidateEdges) {
            String uuid = (String) candidateEdge.get("uuid");
            Long edgeInvalidAt = parseTimestamp(candidateEdge.get("invalid_at"));
            Long edgeValidAt = parseTimestamp(candidateEdge.get("valid_at"));

            // Case 1: 无时间重叠 → 不失效
            // 边已经无效了
            if (edgeInvalidAt != null && newValidAt != null && edgeInvalidAt <= newValidAt) {
                continue;
            }

            // Case 2: 新边在旧边失效之前就已经无效
            if (newInvalidAt != null && edgeValidAt != null && newInvalidAt <= edgeValidAt) {
                continue;
            }

            // Case 3: 新边 valid_at 更晚 → 旧边失效
            if (newValidAt != null && edgeValidAt != null && newValidAt > edgeValidAt) {
                expiredEdgeUuids.add(uuid);
                log.debug("边 {} 因时间冲突失效: new_valid_at={}, edge_valid_at={}",
                        uuid, newValidAt, edgeValidAt);
            }
        }

        log.info("边矛盾解决完成：需要失效 {} 条边", expiredEdgeUuids.size());
        return expiredEdgeUuids;
    }

    @Override
    public void expireEdges(String graphId, List<String> expiredEdges, long expiredAt) {
        if (expiredEdges == null || expiredEdges.isEmpty()) {
            return;
        }

        String cypher =
            "MATCH ()-[r:RELATES_TO]->() " +
            "WHERE r.graph_id = $graph_id AND r.uuid IN $uuids " +
            "SET r.invalid_at = $expiredAt, r.expired_at = $expiredAt";

        try (Session session = graphNeo4jService.getNeo4jDriver().session()) {
            session.run(cypher, Values.parameters(
                "graph_id", graphId,
                "uuids", expiredEdges,
                "expiredAt", expiredAt
            ));
            log.info("边失效完成：graphId={}, expiredCount={}, expiredAt={}",
                    graphId, expiredEdges.size(), expiredAt);
        }
    }

    @Override
    public List<Map<String, Object>> getCurrentFacts(String graphId) {
        return graphNeo4jService.getValidNodes(graphId);
    }

    @Override
    public List<Map<String, Object>> getFactsAtTime(String graphId, long referenceTime) {
        return graphNeo4jService.getValidNodesAt(graphId, referenceTime);
    }

    @Override
    public List<Map<String, Object>> getRelationshipsAtTime(String graphId, long referenceTime) {
        return graphNeo4jService.getValidEdgesAt(graphId, referenceTime);
    }

    @Override
    public List<Map<String, Object>> getFactHistory(String graphId, String entityName) {
        return graphNeo4jService.getFactVersions(graphId, entityName);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 解析时间戳（支持多种格式）
     */
    private Long parseTimestamp(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                // 尝试解析为 ISO 8601 格式
                try {
                    return java.time.Instant.parse((String) value).toEpochMilli();
                } catch (Exception ex) {
                    log.warn("无法解析时间戳：{}", value);
                    return null;
                }
            }
        }

        return null;
    }
}
