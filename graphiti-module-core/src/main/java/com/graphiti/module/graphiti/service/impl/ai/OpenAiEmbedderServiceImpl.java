package com.graphiti.module.graphiti.service.impl.ai;

import com.graphiti.module.graphiti.service.EmbedderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI 嵌入向量服务实现
 * 依赖 Spring AI 自动配置的 OpenAiEmbeddingModel
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "graphiti.ai", name = "embedding-provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiEmbedderServiceImpl implements EmbedderService {

    private final OpenAiEmbeddingModel embeddingModel;

    @Override
    public float[] embed(String text) {
        try {
            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(List.of(text), null)
            );
            return response.getResults().get(0).getOutput();
        } catch (Exception e) {
            log.error("OpenAI embedding failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        try {
            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(texts, null)
            );
            return response.getResults().stream()
                    .map(Embedding::getOutput)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("OpenAI batch embedding failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI batch embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimensions() {
        return 1536;
    }

    @Override
    public String getProvider() {
        return "openai";
    }
}
