package com.ontograph.module.graphiti.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Positive;

/**
 * Graphiti AI 配置属性
 * 读取 application-dev.yml 中 graphiti.ai.* 配置
 */
@Data
@Validated
@ConfigurationProperties(prefix = "graphiti.ai")
public class GraphitiAiProperties {

    /**
     * LLM 提供商: openai | qwen | ollama | anthropic | mistral
     */
    private String llmProvider = "openai";

    /**
     * Embedding 提供商: openai | qwen | ollama | mistral
     */
    private String embeddingProvider = "openai";

    /**
     * Rerank 提供商: openai | custom
     */
    private String rerankProvider = "custom";

    /**
     * OpenAI 配置
     */
    private ProviderConfig openai = new ProviderConfig();

    /**
     * Qwen 通义千问配置
     */
    private ProviderConfig qwen = new ProviderConfig();

    /**
     * Ollama 配置
     */
    private ProviderConfig ollama = new ProviderConfig();

    /**
     * Anthropic 配置
     */
    private ProviderConfig anthropic = new ProviderConfig();

    /**
     * Mistral 配置
     */
    private ProviderConfig mistral = new ProviderConfig();

    /**
     * DeepSeek 配置
     */
    private ProviderConfig deepseek = new ProviderConfig();

    /**
     * Groq 配置
     */
    private ProviderConfig groq = new ProviderConfig();

    /**
     * Fireworks AI 配置
     */
    private ProviderConfig fireworks = new ProviderConfig();

    /**
     * Nebius AI 配置
     */
    private ProviderConfig nebius = new ProviderConfig();

    /**
     * Hyperbolic 配置
     */
    private ProviderConfig hyperbolic = new ProviderConfig();

    /**
     * Together AI 配置
     */
    private ProviderConfig together = new ProviderConfig();

    /**
     * SiliconFlow 配置
     */
    private ProviderConfig siliconflow = new ProviderConfig();

    /**
     * Voyage AI 配置
     */
    private ProviderConfig voyage = new ProviderConfig();

    /**
     * 通用的 Provider 配置
     */
    @Data
    public static class ProviderConfig {
        /**
         * 模型名称
         */
        private String model;

        /**
         * Base URL (用于私有化部署)
         */
        private String baseUrl;

        /**
         * 温度参数
         */
        private Double temperature = 0.2;

        /**
         * 最大 token 数
         */
        @Positive
        private Integer maxTokens = 2048;

        /**
         * Embedding 模型名称
         */
        private String embeddingModel;

        /**
         * Rerank 模型名称
         */
        private String rerankModel;
    }
}
