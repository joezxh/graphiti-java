package com.ontograph;

import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiImageAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.mistralai.autoconfigure.MistralAiChatAutoConfiguration;
import org.springframework.ai.model.mistralai.autoconfigure.MistralAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.mistralai.autoconfigure.MistralAiModerationAutoConfiguration;
import org.springframework.ai.model.mistralai.autoconfigure.MistralAiOcrAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OntoGraph 知识图谱后端服务启动类
 */
@SpringBootApplication(
        scanBasePackages = "com.graphiti",
        exclude = {
                // Anthropic
                AnthropicChatAutoConfiguration.class,
                // Azure OpenAI
                AzureOpenAiChatAutoConfiguration.class,
                AzureOpenAiEmbeddingAutoConfiguration.class,
                AzureOpenAiImageAutoConfiguration.class,
                AzureOpenAiAudioTranscriptionAutoConfiguration.class,
                // Mistral AI
                MistralAiModerationAutoConfiguration.class,
                MistralAiOcrAutoConfiguration.class,
                MistralAiChatAutoConfiguration.class,
                MistralAiEmbeddingAutoConfiguration.class
        }
)
public class GraphitiApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraphitiApplication.class, args);
    }
}
