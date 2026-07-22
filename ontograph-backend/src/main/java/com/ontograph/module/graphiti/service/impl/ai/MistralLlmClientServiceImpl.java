package com.ontograph.module.graphiti.service.impl.ai;

import com.ontograph.module.graphiti.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "graphiti.ai", name = "llm-provider", havingValue = "mistral")
public class MistralLlmClientServiceImpl implements LlmClientService {

    private final MistralAiChatModel chatModel;

    @Override
    public String chat(String prompt) {
        try {
            return chatClient().prompt().user(prompt).call().content();
        } catch (Exception e) {
            log.error("Mistral chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("Mistral chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        try {
            return chatClient().prompt().system(systemPrompt).user(userPrompt).call().content();
        } catch (Exception e) {
            log.error("Mistral chat with system prompt failed: {}", e.getMessage(), e);
            throw new RuntimeException("Mistral chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T chat(String prompt, Class<T> responseType) {
        try {
            return chatClient().prompt().user(prompt).call().entity(responseType);
        } catch (Exception e) {
            log.error("Mistral structured chat failed: {}", e.getMessage(), e);
            throw new RuntimeException("Mistral chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T chat(String systemPrompt, String userPrompt, Class<T> responseType) {
        try {
            return chatClient().prompt().system(systemPrompt).user(userPrompt).call().entity(responseType);
        } catch (Exception e) {
            log.error("Mistral structured chat with system prompt failed: {}", e.getMessage(), e);
            throw new RuntimeException("Mistral chat failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> chatBatch(List<String> prompts) {
        return prompts.stream().map(this::chat).toList();
    }

    @Override
    public String getProvider() {
        return "mistral";
    }

    private ChatClient chatClient() {
        return ChatClient.create(chatModel);
    }
}
