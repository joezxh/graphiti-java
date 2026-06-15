package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.service.CrossEncoderRerankerService;
import com.ontograph.module.graphiti.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Cross-Encoder 重排服务实现
 *
 * <p>参考 Python 实现：graphiti_core/cross_encoder/openai_reranker_client.py
 *
 * <p>策略：
 * <ul>
 *   <li>构建 True/False 二分类 Prompt</li>
 *   <li>批量并发调用 LLM（带信号量控制）</li>
 *   <li>解析 LLM 回复，按分数降序排列</li>
 * </ul>
 *
 * <p>Prompt 模板：
 * <pre>
 * You are an expert tasked with determining whether the passage is relevant
 * to the query.
 * Respond with "True" if PASSAGE is relevant to QUERY and "False" otherwise.
 * &lt;PASSAGE&gt;...&lt;/PASSAGE&gt;
 * &lt;QUERY&gt;...&lt;/QUERY&gt;
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrossEncoderRerankerServiceImpl implements CrossEncoderRerankerService {

    private final LlmClientService llmClientService;

    @Value("${graphiti.search.cross-encoder.max-concurrency:10}")
    private int maxConcurrency = 10;

    @Value("${graphiti.search.cross-encoder.batch-size:20}")
    private int batchSize = 20;

    private final Executor executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2);

    private static final String SYSTEM_PROMPT =
            "You are an expert tasked with determining whether the passage is relevant to the query. " +
            "You must respond with ONLY the word \"True\" or \"False\".";

    @Override
    public List<String> rankEdges(String query, Map<String, String> facts, int limit) {
        if (facts == null || facts.isEmpty()) {
            return List.of();
        }

        // 批量评分
        List<Map.Entry<String, Double>> scored = rankBatch(query, new ArrayList<>(facts.values()), maxConcurrency);

        // 建立 fact -> uuid 的反向映射
        Map<String, String> reverseMap = new HashMap<>();
        for (Map.Entry<String, String> entry : facts.entrySet()) {
            reverseMap.put(entry.getValue(), entry.getKey());
        }

        // 按分数降序，取前 limit 个
        return scored.stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> reverseMap.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> rankNodes(String query, Map<String, String> nodeNames,
                                 Map<String, String> nodeSummaries, int limit) {
        if (nodeNames == null || nodeNames.isEmpty()) {
            return List.of();
        }

        // 拼接 name + summary 作为 passage
        List<String> passages = new ArrayList<>();
        List<String> uuids = new ArrayList<>();
        for (Map.Entry<String, String> entry : nodeNames.entrySet()) {
            String uuid = entry.getKey();
            String name = entry.getValue();
            String summary = nodeSummaries != null ? nodeSummaries.getOrDefault(uuid, "") : "";
            String passage = (name != null ? name : "") + " " + (summary != null ? summary : "");
            passages.add(passage.trim());
            uuids.add(uuid);
        }

        List<Map.Entry<String, Double>> scored = rankBatch(query, passages, maxConcurrency);

        // passage index -> uuid
        Map<String, String> passageToUuid = new HashMap<>();
        for (int i = 0; i < passages.size(); i++) {
            passageToUuid.put(passages.get(i), uuids.get(i));
        }

        return scored.stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> passageToUuid.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map.Entry<String, Double>> rankBatch(String query, List<String> passages,
                                                     int concurrency) {
        if (passages == null || passages.isEmpty()) {
            return List.of();
        }

        Semaphore semaphore = new Semaphore(concurrency);
        List<CompletableFuture<Map.Entry<String, Double>>> futures = new ArrayList<>();

        for (String passage : passages) {
            CompletableFuture<Map.Entry<String, Double>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                    try {
                        double score = scoreSingle(query, passage);
                        return Map.entry(passage, score);
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return Map.entry(passage, 0.0);
                }
            }, executor);
            futures.add(future);
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    @Override
    public double scoreSingle(String query, String passage) {
        if (passage == null || passage.isBlank()) {
            return 0.0;
        }

        String userPrompt = buildPrompt(query, passage);

        try {
            String response = llmClientService.chat(SYSTEM_PROMPT, userPrompt);
            String trimmed = response != null ? response.trim().toLowerCase() : "";

            // 解析 True/False
            if (trimmed.startsWith("true")) {
                return 1.0;
            } else if (trimmed.startsWith("false")) {
                return 0.0;
            } else {
                // fallback: 解析数字
                try {
                    double num = Double.parseDouble(trimmed.replaceAll("[^0-9.]", ""));
                    return Math.max(0.0, Math.min(1.0, num / 100.0));
                } catch (NumberFormatException e) {
                    log.warn("无法解析 Cross-Encoder 响应: {}", response);
                    return 0.5;  // 中立分数
                }
            }
        } catch (Exception e) {
            log.warn("Cross-Encoder 打分失败: {}", e.getMessage());
            return 0.0;
        }
    }

    private String buildPrompt(String query, String passage) {
        return String.format(
            "Respond with \"True\" if PASSAGE is relevant to QUERY and \"False\" otherwise.\n\n" +
            "<PASSAGE>%s</PASSAGE>\n" +
            "<QUERY>%s</QUERY>",
            passage.length() > 2000 ? passage.substring(0, 2000) : passage,
            query
        );
    }
}
