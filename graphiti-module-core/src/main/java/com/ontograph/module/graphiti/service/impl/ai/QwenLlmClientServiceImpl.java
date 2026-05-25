package com.ontograph.module.graphiti.service.impl.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.module.graphiti.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通义千问(Qwen) LLM 客户端服务实现
 * 使用 OpenAI 兼容 API 调用 Qwen 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "graphiti.ai", name = "llm-provider", havingValue = "qwen")
public class QwenLlmClientServiceImpl implements LlmClientService {

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
            log.error("Qwen chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("Qwen chat failed: " + e.getMessage(), e);
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
            log.error("Qwen chat with system prompt failed: {}", e.getMessage(), e);
            throw new RuntimeException("Qwen chat failed: " + e.getMessage(), e);
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
            log.error("Qwen structured chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("Qwen structured chat failed: " + e.getMessage(), e);
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
            log.error("Qwen structured chat with system prompt failed: {}", e.getMessage(), e);
            throw new RuntimeException("Qwen structured chat failed: " + e.getMessage(), e);
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
        return "qwen";
    }
}
