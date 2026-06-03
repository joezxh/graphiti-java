package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.model.search.SearchResults.EdgeResult;
import com.ontograph.module.graphiti.model.search.SearchResults.EpisodeResult;
import com.ontograph.module.graphiti.model.search.SearchResults.NodeResult;
import com.ontograph.module.graphiti.model.search.SearchResults.CommunityResult;
import com.ontograph.module.graphiti.service.RrfRerankerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RRF (Reciprocal Rank Fusion) 重排服务实现
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py:rrf()
 */
@Slf4j
@Service
public class RrfRerankerServiceImpl implements RrfRerankerService {

    /** RRF 默认参数，对齐 Python graphiti */
    private static final int DEFAULT_K = 1;

    @Override
    public List<EdgeResult> rrfEdges(List<List<EdgeResult>> resultLists, int k) {
        if (resultLists == null || resultLists.isEmpty()) {
            return List.of();
        }

        Map<String, Double> scores = new HashMap<>();
        Map<String, EdgeResult> uuidMap = new HashMap<>();

        for (List<EdgeResult> result : resultLists) {
            for (int i = 0; i < result.size(); i++) {
                EdgeResult item = result.get(i);
                String uuid = item.getUuid();
                double rrfScore = 1.0 / (k + i + 1);
                scores.merge(uuid, rrfScore, Double::sum);
                uuidMap.putIfAbsent(uuid, item);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> {
                    EdgeResult result = uuidMap.get(e.getKey());
                    result.setScore(e.getValue());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<NodeResult> rrfNodes(List<List<NodeResult>> resultLists, int k) {
        if (resultLists == null || resultLists.isEmpty()) {
            return List.of();
        }

        Map<String, Double> scores = new HashMap<>();
        Map<String, NodeResult> uuidMap = new HashMap<>();

        for (List<NodeResult> result : resultLists) {
            for (int i = 0; i < result.size(); i++) {
                NodeResult item = result.get(i);
                String uuid = item.getUuid();
                double rrfScore = 1.0 / (k + i + 1);
                scores.merge(uuid, rrfScore, Double::sum);
                uuidMap.putIfAbsent(uuid, item);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> {
                    NodeResult result = uuidMap.get(e.getKey());
                    result.setScore(e.getValue());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<EpisodeResult> rrfEpisodes(List<List<EpisodeResult>> resultLists, int k) {
        if (resultLists == null || resultLists.isEmpty()) {
            return List.of();
        }

        Map<String, Double> scores = new HashMap<>();
        Map<String, EpisodeResult> uuidMap = new HashMap<>();

        for (List<EpisodeResult> result : resultLists) {
            for (int i = 0; i < result.size(); i++) {
                EpisodeResult item = result.get(i);
                String uuid = item.getUuid();
                double rrfScore = 1.0 / (k + i + 1);
                scores.merge(uuid, rrfScore, Double::sum);
                uuidMap.putIfAbsent(uuid, item);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> {
                    EpisodeResult result = uuidMap.get(e.getKey());
                    result.setScore(e.getValue());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CommunityResult> rrfCommunities(List<List<CommunityResult>> resultLists, int k) {
        if (resultLists == null || resultLists.isEmpty()) {
            return List.of();
        }

        Map<String, Double> scores = new HashMap<>();
        Map<String, CommunityResult> uuidMap = new HashMap<>();

        for (List<CommunityResult> result : resultLists) {
            for (int i = 0; i < result.size(); i++) {
                CommunityResult item = result.get(i);
                String uuid = item.getUuid();
                double rrfScore = 1.0 / (k + i + 1);
                scores.merge(uuid, rrfScore, Double::sum);
                uuidMap.putIfAbsent(uuid, item);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> {
                    CommunityResult result = uuidMap.get(e.getKey());
                    result.setScore(e.getValue());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<String> rrfUuids(List<List<String>> resultLists, int k) {
        if (resultLists == null || resultLists.isEmpty()) {
            return List.of();
        }

        Map<String, Double> scores = new HashMap<>();

        for (List<String> result : resultLists) {
            for (int i = 0; i < result.size(); i++) {
                String uuid = result.get(i);
                double rrfScore = 1.0 / (k + i + 1);
                scores.merge(uuid, rrfScore, Double::sum);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> rrfWeightedUuids(List<Map<String, Double>> uuidScorePairs, int k) {
        if (uuidScorePairs == null || uuidScorePairs.isEmpty()) {
            return List.of();
        }

        Map<String, Double> scores = new HashMap<>();

        for (Map<String, Double> pair : uuidScorePairs) {
            for (Map.Entry<String, Double> entry : pair.entrySet()) {
                String uuid = entry.getKey();
                Double score = entry.getValue();
                if (uuid != null && score != null) {
                    double weightedScore = score / (k + 1);  // 权重因子
                    scores.merge(uuid, weightedScore, Double::sum);
                }
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}
