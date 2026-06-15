package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.service.GraphNeo4jService;
import com.ontograph.module.graphiti.service.NodeDistanceRerankerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Node Distance 重排服务实现
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py:node_distance_reranker()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NodeDistanceRerankerServiceImpl implements NodeDistanceRerankerService {

    private final GraphNeo4jService graphNeo4jService;

    @Override
    public List<String> rerankEdgesByDistance(
            List<String> candidateUuids,
            String centerNodeUuid,
            Map<String, String> uuidSourceMap,
            int limit) {

        if (candidateUuids == null || candidateUuids.isEmpty() || centerNodeUuid == null) {
            return List.of();
        }

        Map<String, Double> distanceScores = new HashMap<>();

        for (String edgeUuid : candidateUuids) {
            String sourceUuid = uuidSourceMap.get(edgeUuid);
            if (sourceUuid == null) {
                distanceScores.put(edgeUuid, 0.0);
                continue;
            }

            int distance = bfsDistance(centerNodeUuid, sourceUuid);
            // 分数 = 1 / (distance + 1)，距离越小分数越高
            double score = distance == Integer.MAX_VALUE ? 0 : 1.0 / (distance + 1);
            distanceScores.put(edgeUuid, score);
        }

        return distanceScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> rerankNodesByDistance(
            List<String> candidateUuids,
            String centerNodeUuid,
            int limit) {

        if (candidateUuids == null || candidateUuids.isEmpty() || centerNodeUuid == null) {
            return List.of();
        }

        Map<String, Double> distanceScores = new HashMap<>();

        for (String nodeUuid : candidateUuids) {
            int distance = bfsDistance(centerNodeUuid, nodeUuid);
            double score = distance == Integer.MAX_VALUE ? 0 : 1.0 / (distance + 1);
            distanceScores.put(nodeUuid, score);
        }

        return distanceScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int bfsDistance(String startUuid, String endUuid) {
        if (startUuid == null || endUuid == null) return Integer.MAX_VALUE;
        if (startUuid.equals(endUuid)) return 0;

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(startUuid);
        visited.add(startUuid);
        int distance = 0;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                String current = queue.poll();
                List<Map<String, Object>> neighbors = graphNeo4jService.getNodeEdges(current, 0, 50);
                for (Map<String, Object> edge : neighbors) {
                    String neighborUuid = (String) edge.get("target");
                    if (neighborUuid == null) neighborUuid = (String) edge.get("source");

                    if (neighborUuid != null && !visited.contains(neighborUuid)) {
                        if (neighborUuid.equals(endUuid)) {
                            return distance + 1;
                        }
                        visited.add(neighborUuid);
                        queue.offer(neighborUuid);
                    }
                }
            }
            distance++;
        }

        return Integer.MAX_VALUE;
    }
}
