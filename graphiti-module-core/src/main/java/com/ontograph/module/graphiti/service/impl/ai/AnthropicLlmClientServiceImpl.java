package com.graphiti.module.graphiti.service.impl.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Anthropic Claude LLM 客户端服务实现
 * 支持私有化部署（通过自定义 base-url 接入兼容 Anthropic API 的私有化模型）
 *
 * <p>配置示例（私有化部署）：
 * <pre>
 * spring:
 *   ai:
 *     anthropic:
 *       api-key: sk-your-api-key
 *       base-url: http://your-private-deployment:8080/v1  # 私有化部署地址
 *       chat:
 *         options:
 *           model: claude-3-sonnet-20240229
 *           temperature: 0.2
 *           max-tokens: 2048
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "graphiti.ai", name = "llm-provider", havingValue = "anthropic")
public class AnthropicLlmClientServiceImpl implements LlmClientService {

    private final AnthropicChatModel chatModel;
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
            log.error("Anthropic chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("Anthropic chat failed: " + e.getMessage(), e);
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
            log.error("Anthropic chat with system prompt failed: {}", e.getMessage(), e);
            throw new RuntimeException("Anthropic chat failed: " + e.getMessage(), e);
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
            log.error("Anthropic structured chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("Anthropic structured chat failed: " + e.getMessage(), e);
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
            log.error("Anthropic structured chat with system prompt failed: {}", e.getMessage(), e);
            throw new RuntimeException("Anthropic structured chat failed: " + e.getMessage(), e);
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
        return "anthropic";
    }
}
