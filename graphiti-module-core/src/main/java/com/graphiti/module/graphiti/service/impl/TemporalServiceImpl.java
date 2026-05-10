package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.TemporalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 时序管理服务实现
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
}
