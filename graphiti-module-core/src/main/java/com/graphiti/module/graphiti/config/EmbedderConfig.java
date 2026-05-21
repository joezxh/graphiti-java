package com.graphiti.module.graphiti.config;

import org.springframework.context.annotation.Configuration;

/**
 * 嵌入向量服务配置占位类
 * <p>Spring Boot Auto-Configuration 会根据 application.yml 中的 spring.ai.openai.api-key
 * 自动创建 EmbeddingClient 和 ChatClient Bean。</p>
 */
@Configuration
public class EmbedderConfig {
    // EmbeddingClient 和 ChatClient 由 Spring AI Auto-Configuration 自动注入
}
