package com.ontograph.module.graphiti.config;

import com.ontograph.module.graphiti.service.EmbedderService;
import com.ontograph.module.graphiti.service.LlmClientService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderContractTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.ai.model.mistralai.autoconfigure.MistralAiChatAutoConfiguration.class,
                    org.springframework.ai.model.mistralai.autoconfigure.MistralAiEmbeddingAutoConfiguration.class))
            .withUserConfiguration(AiProviderTestConfiguration.class)
            .withPropertyValues(
                    "spring.ai.model.chat=mistral",
                    "spring.ai.model.embedding=mistral",
                    "spring.ai.mistralai.api-key=test-api-key",
                    "graphiti.ai.llm-provider=mistral",
                    "graphiti.ai.embedding-provider=mistral");

    private final ApplicationContextRunner anthropicContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration.class))
            .withUserConfiguration(AiProviderTestConfiguration.class)
            .withPropertyValues(
                    "spring.ai.model.chat=anthropic",
                    "spring.ai.anthropic.api-key=test-api-key",
                    "graphiti.ai.llm-provider=anthropic",
                    "graphiti.ai.embedding-provider=none");

    private final ApplicationContextRunner defaultSelectionContextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.ai.model.mistralai.autoconfigure.MistralAiChatAutoConfiguration.class,
                    org.springframework.ai.model.mistralai.autoconfigure.MistralAiEmbeddingAutoConfiguration.class))
            .withPropertyValues(
                    "spring.ai.model.chat=openai",
                    "spring.ai.model.embedding=openai");

    @Test
    void mistralSelectionCreatesMatchingGraphitiAdapters() {
        contextRunner.run(context -> {
            assertThat(context.getBeansOfType(LlmClientService.class).values())
                    .extracting(LlmClientService::getProvider)
                    .containsExactly("mistral");
            assertThat(context.getBeansOfType(EmbedderService.class).values())
                    .extracting(EmbedderService::getProvider)
                    .containsExactly("mistral");
        });
    }

    @Test
    void anthropicSelectionCreatesMatchingGraphitiAdapter() {
        anthropicContextRunner.run(context -> assertThat(context.getBeansOfType(LlmClientService.class).values())
                .extracting(LlmClientService::getProvider)
                .containsExactly("anthropic"));
    }

    @Test
    void openAiSelectionDoesNotInitializeMistralModels() {
        defaultSelectionContextRunner.run(context -> {
            assertThat(context).doesNotHaveBean(org.springframework.ai.mistralai.MistralAiChatModel.class);
            assertThat(context).doesNotHaveBean(org.springframework.ai.mistralai.MistralAiEmbeddingModel.class);
        });
    }
}
