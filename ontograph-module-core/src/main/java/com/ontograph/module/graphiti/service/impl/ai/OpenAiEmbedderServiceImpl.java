package com.ontograph.module.graphiti.service.impl.ai;

import com.ontograph.module.graphiti.service.EmbedderService;
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
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Embedding input text must not be null or blank");
        }
        try {
            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(List.of(text), null)
            );
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                throw new IllegalStateException("Embedding response returned no results. "
                    + "Ensure the embedding model is correctly loaded in LM Studio "
                    + "(e.g. 'nomic-embed-text', 'bge-m3', 'qwen3-embedding') and is not a reranker model.");
            }
            return response.getResults().get(0).getOutput();
        } catch (NullPointerException e) {
            String msg = e.getMessage();
            log.error("OpenAI embedding failed with NullPointerException. "
                + "This typically means the embedding API returned null data. "
                + "Please verify: (1) the configured model is an EMBEDDING model, not a reranker; "
                + "(2) the model is loaded and ready in LM Studio (check the 'Embedding' tab); "
                + "(3) the base-url and model name in spring.ai.openai.embedding.options.model are correct. "
                + "Error: {}", msg, e);
            throw new RuntimeException(
                "OpenAI embedding failed: API returned null data. "
                + "Please check that a valid embedding model (e.g. 'nomic-embed-text', 'bge-m3', 'qwen3-embedding') "
                + "is loaded in LM Studio, not a reranker model.", e);
        } catch (Exception e) {
            String msg = e.getMessage();
            log.error("OpenAI embedding failed: {}", msg, e);
            throw new RuntimeException("OpenAI embedding failed: " + msg, e);
        }
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("Embedding input texts must not be null or empty");
        }
        try {
            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(texts, null)
            );
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                throw new IllegalStateException("Batch embedding response returned no results. "
                    + "Ensure the embedding model is correctly loaded in LM Studio "
                    + "(e.g. 'nomic-embed-text', 'bge-m3', 'qwen3-embedding') and is not a reranker model.");
            }
            return response.getResults().stream()
                    .map(Embedding::getOutput)
                    .collect(Collectors.toList());
        } catch (NullPointerException e) {
            String msg = e.getMessage();
            log.error("OpenAI batch embedding failed with NullPointerException. "
                + "This typically means the embedding API returned null data. "
                + "Please verify: (1) the configured model is an EMBEDDING model, not a reranker; "
                + "(2) the model is loaded and ready in LM Studio; "
                + "(3) the base-url and model name in spring.ai.openai.embedding.options.model are correct. "
                + "Error: {}", msg, e);
            throw new RuntimeException(
                "OpenAI batch embedding failed: API returned null data. "
                + "Please check that a valid embedding model is loaded in LM Studio, not a reranker model.", e);
        } catch (Exception e) {
            String msg = e.getMessage();
            log.error("OpenAI batch embedding failed: {}", msg, e);
            throw new RuntimeException("OpenAI batch embedding failed: " + msg, e);
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
