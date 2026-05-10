package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.EmbedderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EmbedderServiceImpl implements EmbedderService {

    @Override
    public float[] embed(String text) {
        if (text == null || text.isEmpty()) {
            return new float[getDimensions()];
        }
        // TODO: 集成Spring AI EmbeddingClient
        log.warn("Embedder not fully implemented yet");
        return new float[getDimensions()];
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
