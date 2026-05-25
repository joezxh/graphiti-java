package com.ontograph.module.graphiti.service.impl.ai;

import com.ontograph.module.graphiti.service.EmbedderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ollama 嵌入向量服务实现
 * 依赖 Spring AI 自动配置的 OllamaEmbeddingModel
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "graphiti.ai", name = "embedding-provider", havingValue = "ollama")
public class OllamaEmbedderServiceImpl implements EmbedderService {

    private final OllamaEmbeddingModel embeddingModel;

    @Override
    public float[] embed(String text) {
        try {
            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(List.of(text), null)
            );
            if (response.getResults() == null || response.getResults().isEmpty()) {
                throw new IllegalStateException("Ollama embedding response returned no results.");
            }
            return response.getResults().get(0).getOutput();
        } catch (IllegalStateException e) {
            log.error("Ollama embedding failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama embedding failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Ollama embedding failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        try {
            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(texts, null)
            );
            if (response.getResults() == null || response.getResults().isEmpty()) {
                throw new IllegalStateException("Ollama batch embedding response returned no results.");
            }
            return response.getResults().stream()
                    .map(Embedding::getOutput)
                    .collect(Collectors.toList());
        } catch (IllegalStateException e) {
            log.error("Ollama batch embedding failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama batch embedding failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Ollama batch embedding failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama batch embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimensions() {
        return 768;
    }

    @Override
    public String getProvider() {
        return "ollama";
    }
}
