package com.ontograph.module.graphiti.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.module.graphiti.vo.llm.ExtractEntitiesResultVO;
import com.ontograph.module.graphiti.vo.llm.ExtractRelationsResultVO;
import com.ontograph.module.graphiti.vo.llm.ExtractedEntityVO;
import com.ontograph.module.graphiti.vo.llm.ExtractedRelationVO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * LLM 客户端服务接口
 * 提供统一的 LLM 调用能力，屏蔽底层 Provider 差异
 */
public interface LlmClientService {

    /**
     * 发送单轮对话请求
     *
     * @param prompt 用户提示词
     * @return LLM 回复文本
     */
    String chat(String prompt);

    /**
     * 发送带系统提示词的对话请求
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @return LLM 回复文本
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 发送对话请求，返回结构化对象（JSON 解析）
     *
     * @param prompt      用户提示词
     * @param responseType 返回类型
     * @param <T>         泛型类型
     * @return 解析后的对象
     */
    <T> T chat(String prompt, Class<T> responseType);

    /**
     * 发送带系统提示词的对话请求，返回结构化对象
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param responseType 返回类型
     * @param <T>          泛型类型
     * @return 解析后的对象
     */
    <T> T chat(String systemPrompt, String userPrompt, Class<T> responseType);

    /**
     * 批量发送对话请求
     *
     * @param prompts 提示词列表
     * @return 回复文本列表
     */
    List<String> chatBatch(List<String> prompts);

    /**
     * 带信号量控制的并发批量对话请求
     *
     * @param prompts 提示词列表
     * @param maxConcurrency 最大并发数
     * @return 回复文本列表
     */
    List<String> chatBatchAsync(List<String> prompts, int maxConcurrency);

    /**
     * 获取当前使用的 Provider 名称
     *
     * @return Provider 名称
     */
    String getProvider();

    /**
     * 生成内容摘要
     *
     * @param content 原始内容
     * @return 摘要文本
     */
    default String generateSummary(String content) {
        try {
            String prompt = "请为以下内容生成简洁的摘要（不超过100字）:\n\n" + content;
            return chat(prompt);
        } catch (Exception e) {
            return content.substring(0, Math.min(content.length(), 100));
        }
    }

    /**
     * 生成社区摘要
     *
     * @param nodeSummaries 社区成员摘要列表
     * @return 社区摘要文本
     */
    default String generateCommunitySummary(List<String> nodeSummaries) {
        try {
            String content = String.join("\n", nodeSummaries);
            String prompt = "请为以下社区成员生成社区摘要:\n\n" + content;
            return chat(prompt);
        } catch (Exception e) {
            return String.join(", ", nodeSummaries)
                .substring(0, Math.min(200, String.join(", ", nodeSummaries).length()));
        }
    }

    /**
     * 从文本中提取实体
     *
     * @param text 输入文本
     * @return 提取的实体列表
     */
    default List<ExtractedEntityVO> extractEntities(String text) {
        try {
            String promptTemplate = loadPrompt("prompts/extract_entities.txt");
            String prompt = promptTemplate.replace("{{text}}", text != null ? text : "");
            String response = chat(prompt);
            ObjectMapper mapper = new ObjectMapper();
            ExtractEntitiesResultVO result = mapper.readValue(response, ExtractEntitiesResultVO.class);
            return result.getEntities() != null ? result.getEntities() : List.of();
        } catch (Exception e) {
            System.err.println("实体提取失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 从文本中提取实体之间的关系
     *
     * @param text 输入文本
     * @return 提取的关系列表
     */
    default List<ExtractedRelationVO> extractRelations(String text) {
        try {
            String promptTemplate = loadPrompt("prompts/extract_relations.txt");
            String prompt = promptTemplate.replace("{{text}}", text != null ? text : "");
            String response = chat(prompt);
            ObjectMapper mapper = new ObjectMapper();
            ExtractRelationsResultVO result = mapper.readValue(response, ExtractRelationsResultVO.class);
            return result.getRelations() != null ? result.getRelations() : List.of();
        } catch (Exception e) {
            System.err.println("关系提取失败: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 加载 classpath 下的 prompt 模板
     */
    private static String loadPrompt(String path) {
        try (var is = LlmClientService.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Prompt file not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("加载 prompt 模板失败: " + path, e);
        }
    }
}
