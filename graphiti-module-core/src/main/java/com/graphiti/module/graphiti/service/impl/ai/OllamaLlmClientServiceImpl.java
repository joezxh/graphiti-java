package com.graphiti.module.graphiti.service.impl.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ollama LLM 客户端服务实现
 * 依赖 Spring AI 自动配置的 OllamaChatModel
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "graphiti.ai", name = "llm-provider", havingValue = "ollama")
public class OllamaLlmClientServiceImpl implements LlmClientService {

    private final OllamaChatModel chatModel;
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
            log.error("Ollama chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama chat failed: " + e.getMessage(), e);
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
            log.error("Ollama chat with system prompt failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama chat failed: " + e.getMessage(), e);
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
            log.error("Ollama structured chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama structured chat failed: " + e.getMessage(), e);
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
            log.error("Ollama structured chat with system prompt failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ollama structured chat failed: " + e.getMessage(), e);
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
        return "ollama";
    }
}
