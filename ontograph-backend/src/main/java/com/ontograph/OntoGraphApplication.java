package com.ontograph;

import org.springframework.ai.model.mistralai.autoconfigure.MistralAiModerationAutoConfiguration;
import org.springframework.ai.model.mistralai.autoconfigure.MistralAiOcrAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OntoGraph 知识图谱后端服务启动类
 */
@SpringBootApplication(
        scanBasePackages = "com.ontograph",
        exclude = {
                // No Graphiti adapter exposes these capabilities yet. Their auto-configurations
                // default to Mistral and require an API key even when chat/embedding use OpenAI.
                MistralAiModerationAutoConfiguration.class,
                MistralAiOcrAutoConfiguration.class
        }
)
public class OntoGraphApplication {
    public static void main(String[] args) {
        SpringApplication.run(OntoGraphApplication.class, args);
    }
}
