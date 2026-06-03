package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.service.MmrRerankerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MMR (Maximal Marginal Relevance) 重排服务实现
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py:maximal_marginal_relevance()
 */
@Slf4j
@Service
public class MmrRerankerServiceImpl implements MmrRerankerService {

    private static final double MIN_SCORE = -2.0;

    @Override
    public List<String> mmrEdges(
            double[] queryVector,
            List<String> candidateUuids,
            Map<String, double[]> uuidEmbeddingMap,
            Map<String, String> uuidFactMap,
            double lambda,
            int limit) {

        if (candidateUuids == null || candidateUuids.isEmpty()) {
            return List.of();
        }

        // 归一化查询向量
        double[] normQuery = normalizeL2(queryVector);

        // 归一化候选向量
        Map<String, double[]> normEmbeddings = new HashMap<>();
        for (Map.Entry<String, double[]> entry : uuidEmbeddingMap.entrySet()) {
            double[] norm = normalizeL2(entry.getValue());
            normEmbeddings.put(entry.getKey(), norm);
        }

        // 构建相似度矩阵
        List<String> uuids = new ArrayList<>(candidateUuids);
        int n = uuids.size();
        double[][] similarityMatrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            double[] vecI = normEmbeddings.getOrDefault(uuids.get(i), new double[0]);
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    similarityMatrix[i][j] = 1.0;
                } else if (j < i) {
                    similarityMatrix[i][j] = similarityMatrix[j][i];
                } else {
                    double[] vecJ = normEmbeddings.getOrDefault(uuids.get(j), new double[0]);
                    similarityMatrix[i][j] = dot(normQuery, vecJ);  // 查询与候选的相似度
                }
            }
        }

        // 逐步选择，每次选择 MMR 分数最高的
        List<String> selected = new ArrayList<>();
        Set<Integer> selectedIndices = new HashSet<>();
        Set<Integer> remainingIndices = new HashSet<>();
        for (int i = 0; i < n; i++) remainingIndices.add(i);

        while (!remainingIndices.isEmpty() && selected.size() < limit) {
            String bestUuid = null;
            int bestIdx = -1;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (int idx : remainingIndices) {
                double relevance = dot(normQuery, normEmbeddings.getOrDefault(uuids.get(idx), new double[0]));

                double maxSimToSelected = 0;
                for (int selIdx : selectedIndices) {
                    maxSimToSelected = Math.max(maxSimToSelected, similarityMatrix[idx][selIdx]);
                }

                // MMR 公式：λ * sim(query, doc) - (1-λ) * max(sim(selected, doc))
                double mmrScore = lambda * relevance - (1 - lambda) * maxSimToSelected;

                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    bestUuid = uuids.get(idx);
                    bestIdx = idx;
                }
            }

            if (bestUuid != null && bestScore >= MIN_SCORE) {
                selected.add(bestUuid);
                selectedIndices.add(bestIdx);
                remainingIndices.remove(bestIdx);
            } else {
                break;
            }
        }

        return selected;
    }

    @Override
    public List<String> mmrNodes(
            double[] queryVector,
            List<String> candidateUuids,
            Map<String, double[]> uuidEmbeddingMap,
            Map<String, String> uuidNameMap,
            double lambda,
            int limit) {
        return mmrEdges(queryVector, candidateUuids, uuidEmbeddingMap, uuidNameMap, lambda, limit);
    }

    @Override
    public List<String> mmrByText(
            List<String> candidateUuids,
            Map<String, String> uuidTextMap,
            double lambda,
            int limit) {

        if (candidateUuids == null || candidateUuids.isEmpty()) {
            return List.of();
        }

        // 按 Jaccard 相似度初始化排序
        List<String> remaining = new ArrayList<>(candidateUuids);
        List<String> selected = new ArrayList<>();

        if (!remaining.isEmpty()) {
            // 选择第一个（最高 Jaccard 分数的）
            selected.add(remaining.remove(0));
        }

        while (!remaining.isEmpty() && selected.size() < limit) {
            String bestUuid = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            for (String uuid : remaining) {
                String text = uuidTextMap.getOrDefault(uuid, "");
                Set<String> textSet = tokenize(text);

                double maxSim = 0;
                for (String selUuid : selected) {
                    String selText = uuidTextMap.getOrDefault(selUuid, "");
                    maxSim = Math.max(maxSim, jaccard(tokenize(selText), textSet));
                }

                // λ * relevance(1.0) - (1-λ) * maxSim = λ - (1-λ) * maxSim
                double mmrScore = lambda - (1 - lambda) * maxSim;

                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    bestUuid = uuid;
                }
            }

            if (bestUuid != null) {
                selected.add(bestUuid);
                remaining.remove(bestUuid);
            } else {
                break;
            }
        }

        return selected;
    }

    @Override
    public double cosineSimilarity(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        return dot(a, b) / (norm(a) * norm(b) + 1e-10);
    }

    @Override
    public double[] normalizeL2(double[] vector) {
        if (vector == null || vector.length == 0) {
            return new double[0];
        }
        double n = norm(vector);
        if (n == 0) return vector.clone();
        double[] result = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = vector[i] / n;
        }
        return result;
    }

    private double dot(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    private double norm(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList(text.toLowerCase().split("\\s+")));
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 0.0;
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }
}
