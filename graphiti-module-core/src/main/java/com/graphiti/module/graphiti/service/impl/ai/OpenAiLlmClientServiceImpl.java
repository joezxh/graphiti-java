package com.graphiti.module.graphiti.service.impl.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAI LLM 客户端服务实现
 * 支持私有化部署（通过 spring.ai.openai.base-url 配置自定义地址）
 *
 * <p>私有化部署配置示例：
 * <pre>
 * spring:
 *   ai:
 *     openai:
 *       api-key: any-key
 *       base-url: http://your-private-deployment:8000/v1  # vLLM / LM Studio / LocalAI
 *       chat:
 *         options:
 *           model: gpt-4o
 *           temperature: 0.2
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "graphiti.ai", name = "llm-provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiLlmClientServiceImpl implements LlmClientService {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    @Override
    public String chat(String prompt) {
        try {
            ChatClient chatClient = ChatClient.create(chatModel);
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("OpenAI chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        try {
            ChatClient chatClient = ChatClient.create(chatModel);
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("OpenAI chat with system prompt failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T chat(String prompt, Class<T> responseType) {
        try {
            ChatClient chatClient = ChatClient.create(chatModel);
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(responseType);
        } catch (Exception e) {
            log.error("OpenAI structured chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI structured chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T chat(String systemPrompt, String userPrompt, Class<T> responseType) {
        try {
            ChatClient chatClient = ChatClient.create(chatModel);
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(responseType);
        } catch (Exception e) {
            log.error("OpenAI structured chat with system prompt failed: {}", e.getMessage(), e);
            throw new RuntimeException("OpenAI structured chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> chatBatch(List<String> prompts) {
        return prompts.stream()
                .map(this::chat)
                .collect(Collectors.toList());
    }

    @Override
    public String getProvider() {
        return "openai";
    }
}
