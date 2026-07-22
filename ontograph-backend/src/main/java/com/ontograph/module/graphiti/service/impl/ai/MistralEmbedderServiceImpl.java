package com.ontograph.module.graphiti.service.impl.ai;

import com.ontograph.module.graphiti.service.EmbedderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.mistralai.MistralAiEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "graphiti.ai", name = "embedding-provider", havingValue = "mistral")
public class MistralEmbedderServiceImpl implements EmbedderService {

    private final MistralAiEmbeddingModel embeddingModel;

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Embedding input text must not be null or blank");
        }
        return embed(List.of(text)).getFirst();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("Embedding input texts must not be null or empty");
        }
        try {
            EmbeddingResponse response = embeddingModel.call(new EmbeddingRequest(texts, null));
            if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
                throw new IllegalStateException("Mistral embedding response returned no results");
            }
            return response.getResults().stream().map(Embedding::getOutput).toList();
        } catch (Exception e) {
            log.error("Mistral embedding failed: {}", e.getMessage(), e);
            throw new RuntimeException("Mistral embedding failed: " + e.getMessage(), e);
        }
    }

    @Override
    public int getDimensions() {
        return embeddingModel.dimensions();
    }

    @Override
    public String getProvider() {
        return "mistral";
    }
}
