package com.graphiti.module.graphiti.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Graphiti AI 配置属性
 * 支持多 Provider 切换：OpenAI / 通义千问(Qwen) / Ollama
 */
@Data
@Component
@ConfigurationProperties(prefix = "graphiti.ai")
public class GraphitiAiProperties {

    /**
     * LLM Provider: openai | qwen | ollama
     */
    private String llmProvider = "openai";

    /**
     * Embedding Provider: openai | qwen | ollama
     */
    private String embeddingProvider = "openai";

    /**
     * OpenAI 配置
     */
    private ProviderConfig openai = new ProviderConfig();

    /**
     * 通义千问(Qwen) 配置
     */
    private ProviderConfig qwen = new ProviderConfig();

    /**
     * Ollama 配置
     */
    private OllamaConfig ollama = new OllamaConfig();

    @Data
    public static class ProviderConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
        private Double temperature = 0.2;
        private Integer maxTokens = 2048;
    }

    @Data
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String chatModel = "llama3";
        private String embeddingModel = "nomic-embed-text";
        private Double temperature = 0.2;
    }
}
