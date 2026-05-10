package com.graphiti.module.graphiti.service;

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
}
