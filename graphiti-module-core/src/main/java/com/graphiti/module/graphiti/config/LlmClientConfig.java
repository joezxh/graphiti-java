package com.graphiti.module.graphiti.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 客户端配置类
 * <p>支持多 Provider 切换：OpenAI、阿里云通义千问、Ollama</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "graphiti.llm")
public class LlmClientConfig {

    /**
     * 当前使用的 LLM 提供商：openai | qwen | ollama
     */
    private String provider = "openai";

    /**
     * OpenAI 配置
     */
    private OpenAIConfig openai = new OpenAIConfig();

    /**
     * 阿里云通义千问配置
     */
    private QwenConfig qwen = new QwenConfig();

    /**
     * Ollama 本地模型配置
     */
    private OllamaConfig ollama = new OllamaConfig();

    @Data
    public static class OpenAIConfig {
        private String apiKey;
        private String model = "gpt-4o";
        private double temperature = 0.2;
    }

    @Data
    public static class QwenConfig {
        private String apiKey;
        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String model = "qwen-turbo";
        private double temperature = 0.2;
    }

    @Data
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama3";
        private double temperature = 0.2;
    }
}
