package com.ontograph.module.graphiti.util;

import com.ontograph.module.graphiti.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 二叉树合并摘要生成器
 *
 * <p>参考 Python 实现：graphiti_core/utils/maintenance/community_operations.py
 *
 * <p>算法：
 * <ol>
 *   <li>将摘要列表两两配对</li>
 *   <li>并行调用 LLM 合并每对摘要</li>
 *   <li>重复直到只剩一个摘要</li>
 * </ol>
 *
 * <p>复杂度：O(n log n) LLM 调用，而非 O(n)
 */
@Slf4j
public class BinaryTreeSummarizer {

    // 最大并发数
    private static final int MAX_CONCURRENCY = 10;

    // 最大摘要长度
    private static final int MAX_SUMMARY_CHARS = 1000;

    private final LlmClientService llmClientService;
    private final ExecutorService executorService;

    public BinaryTreeSummarizer(LlmClientService llmClientService) {
        this.llmClientService = llmClientService;
        this.executorService = Executors.newFixedThreadPool(MAX_CONCURRENCY);
    }

    /**
     * 使用二叉树合并策略生成社区摘要
     *
     * @param summaries 成员摘要列表
     * @return 最终摘要
     */
    public String summarize(List<String> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return "";
        }

        if (summaries.size() == 1) {
            return truncate(summaries.get(0));
        }

        List<String> currentSummaries = new ArrayList<>(summaries);

        while (currentSummaries.size() > 1) {
            List<String> nextSummaries = new ArrayList<>();
            List<CompletableFuture<String>> futures = new ArrayList<>();

            // 配对处理
            for (int i = 0; i < currentSummaries.size(); i += 2) {
                if (i + 1 < currentSummaries.size()) {
                    // 配对
                    String left = currentSummaries.get(i);
                    String right = currentSummaries.get(i + 1);
                    futures.add(CompletableFuture.supplyAsync(
                            () -> mergePair(left, right), executorService));
                } else {
                    // 奇数个，保留最后一个
                    nextSummaries.add(currentSummaries.get(i));
                }
            }

            // 等待所有合并完成
            for (CompletableFuture<String> future : futures) {
                try {
                    nextSummaries.add(future.join());
                } catch (Exception e) {
                    log.error("合并摘要失败：{}", e.getMessage());
                }
            }

            currentSummaries = nextSummaries;
        }

        return truncate(currentSummaries.isEmpty() ? "" : currentSummaries.get(0));
    }

    /**
     * 使用二叉树合并策略生成社区摘要（带上下文）
     *
     * @param summaries 成员摘要列表
     * @param context 额外上下文信息
     * @return 最终摘要
     */
    public String summarizeWithContext(List<String> summaries, String context) {
        if (summaries == null || summaries.isEmpty()) {
            return "";
        }

        if (summaries.size() == 1) {
            return truncate(summaries.get(0));
        }

        List<String> currentSummaries = new ArrayList<>(summaries);

        while (currentSummaries.size() > 1) {
            List<String> nextSummaries = new ArrayList<>();
            List<CompletableFuture<String>> futures = new ArrayList<>();

            for (int i = 0; i < currentSummaries.size(); i += 2) {
                if (i + 1 < currentSummaries.size()) {
                    String left = currentSummaries.get(i);
                    String right = currentSummaries.get(i + 1);
                    futures.add(CompletableFuture.supplyAsync(
                            () -> mergePairWithContext(left, right, context), executorService));
                } else {
                    nextSummaries.add(currentSummaries.get(i));
                }
            }

            for (CompletableFuture<String> future : futures) {
                try {
                    nextSummaries.add(future.join());
                } catch (Exception e) {
                    log.error("合并摘要失败：{}", e.getMessage());
                }
            }

            currentSummaries = nextSummaries;
        }

        return truncate(currentSummaries.isEmpty() ? "" : currentSummaries.get(0));
    }

    /**
     * 合并两个摘要
     */
    private String mergePair(String left, String right) {
        String prompt = buildMergePrompt(left, right);
        try {
            return llmClientService.chat(prompt);
        } catch (Exception e) {
            log.error("LLM 合并摘要失败：{}", e.getMessage());
            // 降级：简单拼接
            return truncate(left + "\n" + right);
        }
    }

    /**
     * 合并两个摘要（带上下文）
     */
    private String mergePairWithContext(String left, String right, String context) {
        String prompt = buildMergePromptWithContext(left, right, context);
        try {
            return llmClientService.chat(prompt);
        } catch (Exception e) {
            log.error("LLM 合并摘要失败：{}", e.getMessage());
            return truncate(left + "\n" + right);
        }
    }

    /**
     * 构建合并 prompt
     */
    private String buildMergePrompt(String left, String right) {
        return String.format("""
            请将以下两个摘要合并为一个连贯的摘要。

            摘要 1:
            %s

            摘要 2:
            %s

            要求：
            - 保留所有重要的实体名称、角色、地点、日期、数量等信息
            - 优先使用紧凑的事实性句子，而非模糊的主题性表述
            - 直接陈述内容，而非"提到"、"描述"等表述
            - 避免填充性动词：提到、描述、陈述、报告、指出、讨论、引用

            合并后的摘要：
            """, left, right);
    }

    /**
     * 构建合并 prompt（带上下文）
     */
    private String buildMergePromptWithContext(String left, String right, String context) {
        return String.format("""
            请将以下两个摘要合并为一个连贯的摘要。

            上下文信息：
            %s

            摘要 1:
            %s

            摘要 2:
            %s

            要求：
            - 保留所有重要的实体名称、角色、地点、日期、数量等信息
            - 优先使用紧凑的事实性句子，而非模糊的主题性表述
            - 直接陈述内容，而非"提到"、"描述"等表述
            - 避免填充性动词：提到、描述、陈述、报告、指出、讨论、引用

            合并后的摘要：
            """, context, left, right);
    }

    /**
     * 生成社区名称描述
     */
    public String generateCommunityName(String summary) {
        String prompt = String.format("""
            根据以下摘要生成一个简短的一句话描述，说明这是什么类型的信息汇总。

            摘要：
            %s

            要求：
            - 简洁明了，一句话
            - 描述社区的主题或类型
            - 不要使用"关于"、"涉及"等模糊词汇

            社区名称：
            """, summary);

        try {
            return llmClientService.chat(prompt);
        } catch (Exception e) {
            log.error("LLM 生成社区名称失败：{}", e.getMessage());
            return "社区摘要";
        }
    }

    /**
     * 截断摘要到最大长度
     */
    private String truncate(String text) {
        if (text == null) {
            return "";
        }

        if (text.length() <= MAX_SUMMARY_CHARS) {
            return text;
        }

        // 在句子边界截断
        int truncateIndex = text.lastIndexOf('.', MAX_SUMMARY_CHARS);
        if (truncateIndex > MAX_SUMMARY_CHARS / 2) {
            return text.substring(0, truncateIndex + 1);
        }

        // 否则在单词边界截断
        truncateIndex = text.lastIndexOf(' ', MAX_SUMMARY_CHARS);
        if (truncateIndex > MAX_SUMMARY_CHARS / 2) {
            return text.substring(0, truncateIndex) + "...";
        }

        return text.substring(0, MAX_SUMMARY_CHARS) + "...";
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
