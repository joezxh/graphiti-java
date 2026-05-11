package com.graphiti;

import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Graphiti 知识图谱后端服务启动类
 */
@SpringBootApplication(
        scanBasePackages = "com.graphiti",
        exclude = {AnthropicChatAutoConfiguration.class}
)
public class GraphitiApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraphitiApplication.class, args);
    }
}
