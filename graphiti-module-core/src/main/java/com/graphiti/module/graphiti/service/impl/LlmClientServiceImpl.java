package com.graphiti.module.graphiti.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.module.graphiti.service.LlmClientService;
import com.graphiti.module.graphiti.vo.llm.ExtractedEntityVO;
import com.graphiti.module.graphiti.vo.llm.ExtractedRelationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClientServiceImpl implements LlmClientService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public List<ExtractedEntityVO> extractEntities(String text, List<String> entityTypes) {
        try {
            String prompt = loadPrompt("prompts/extract_entities.txt")
                    .replace("{entityTypes}", String.join(", ", entityTypes))
                    .replace("{text}", text);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return objectMapper.readValue(response, new TypeReference<List<ExtractedEntityVO>>() {});
        } catch (Exception e) {
            log.error("实体提取失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<ExtractedRelationVO> extractRelations(String text, List<ExtractedEntityVO> entities) {
        try {
            String prompt = loadPrompt("prompts/extract_relations.txt")
                    .replace("{entities}", objectMapper.writeValueAsString(entities))
                    .replace("{text}", text);

            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return objectMapper.readValue(response, new TypeReference<List<ExtractedRelationVO>>() {});
        } catch (Exception e) {
            log.error("关系提取失败: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public String generateSummary(String content) {
        try {
            String prompt = "请为以下内容生成简洁的摘要（不超过100字）:\n\n" + content;
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("摘要生成失败: {}", e.getMessage());
            return content.substring(0, Math.min(content.length(), 100));
        }
    }

    @Override
    public String generateCommunitySummary(List<String> nodeSummaries) {
        try {
            String prompt = "请为以下社区成员生成社区摘要:\n\n" + String.join("\n", nodeSummaries);
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("社区摘要生成失败: {}", e.getMessage());
            return String.join(", ", nodeSummaries).substring(0, Math.min(200, String.join(", ", nodeSummaries).length()));
        }
    }

    private String loadPrompt(String path) throws IOException {
        return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
