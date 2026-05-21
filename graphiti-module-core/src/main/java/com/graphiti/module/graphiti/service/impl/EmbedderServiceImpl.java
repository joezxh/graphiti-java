package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.EmbedderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbedderServiceImpl implements EmbedderService {

    private final EmbeddingModel embeddingModel;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[getDimensions()];
        }
        try {
            EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(List.of(text), null));
            float[] embedding = response.getResults().get(0).getOutput();
            return embedding;
        } catch (Exception e) {
            log.error("生成嵌入向量失败: {}", e.getMessage());
            return new float[getDimensions()];
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>();
        for (String text : texts) {
            results.add(embed(text));
        }
        return results;
    }

    @Override
    public double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public int getDimensions() {
        return 1536; // OpenAI text-embedding-3-small
    }
}
