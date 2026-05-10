package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.EmbedderService;
import com.graphiti.module.graphiti.service.RerankerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class RerankerServiceImpl implements RerankerService {

    @Override
    public List<Map<String, Object>> rrfRerank(List<List<Map<String, Object>>> resultLists, int k) {
        Map<String, Double> scores = new HashMap<>();
        Map<String, Map<String, Object>> items = new HashMap<>();

        for (List<Map<String, Object>> list : resultLists) {
            for (int i = 0; i < list.size(); i++) {
                String uuid = (String) list.get(i).get("uuid");
                double score = 1.0 / (k + i + 1);
                scores.merge(uuid, score, Double::sum);
                items.putIfAbsent(uuid, list.get(i));
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> item = new HashMap<>(items.get(e.getKey()));
                    item.put("score", e.getValue());
                    return item;
                })
                .toList();
    }

    @Override
    public List<Map<String, Object>> mmrRerank(List<Map<String, Object>> results, float[] queryEmbedding,
                                                double lambda, EmbedderService embedderService) {
        List<Map<String, Object>> selected = new ArrayList<>();
        Set<String> selectedUuids = new HashSet<>();

        while (selected.size() < results.size()) {
            Map<String, Object> bestItem = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (Map<String, Object> item : results) {
                String uuid = (String) item.get("uuid");
                if (selectedUuids.contains(uuid)) continue;

                double relevance = item.containsKey("similarity") ? 
                    ((Number) item.get("similarity")).doubleValue() : 0.5;

                double maxSim = 0;
                for (Map<String, Object> sel : selected) {
                    double sim = calculateNameSimilarity(
                        (String) item.get("name"), (String) sel.get("name"));
                    maxSim = Math.max(maxSim, sim);
                }

                double mmrScore = lambda * relevance - (1 - lambda) * maxSim;
                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    bestItem = item;
                }
            }

            if (bestItem == null) break;
            selected.add(bestItem);
            selectedUuids.add((String) bestItem.get("uuid"));
        }

        return selected;
    }

    private double calculateNameSimilarity(String a, String b) {
        if (a == null || b == null) return 0;
        Set<String> setA = new HashSet<>(Arrays.asList(a.toLowerCase().split("\\s+")));
        Set<String> setB = new HashSet<>(Arrays.asList(b.toLowerCase().split("\\s+")));
        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }
}
