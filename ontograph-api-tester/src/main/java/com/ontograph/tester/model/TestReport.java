package com.ontograph.tester.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 测试报告聚合类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestReport {

    /**
     * 报告生成时间
     */
    private LocalDateTime generatedAt;

    /**
     * 测试环境
     */
    private String environment;

    /**
     * 目标服务URL
     */
    private String targetUrl;

    /**
     * 总测试用例数
     */
    private int totalCases;

    /**
     * 通过数
     */
    @Builder.Default
    private int passed = 0;

    /**
     * 失败数
     */
    @Builder.Default
    private int failed = 0;

    /**
     * 跳过数
     */
    @Builder.Default
    private int skipped = 0;

    /**
     * 错误数
     */
    @Builder.Default
    private int error = 0;

    /**
     * 总耗时（毫秒）
     */
    private long totalDurationMs;

    /**
     * 平均响应时间（毫秒）
     */
    private long avgResponseTimeMs;

    /**
     * 所有测试结果
     */
    @Builder.Default
    private List<TestResult> results = new ArrayList<>();

    /**
     * 按模块分组的结果
     */
    @Builder.Default
    private Map<String, ModuleSummary> moduleSummaries = new ConcurrentHashMap<>();

    /**
     * 按阶段分组的结果
     */
    @Builder.Default
    private Map<TestCase.TestPhase, List<TestResult>> phaseResults = new ConcurrentHashMap<>();

    /**
     * 按状态分组的结果
     */
    @Builder.Default
    private Map<TestResult.Status, List<TestResult>> statusResults = new ConcurrentHashMap<>();

    /**
     * 失败和错误的详情
     */
    @Builder.Default
    private List<TestResult> failures = new ArrayList<>();

    /**
     * 模块摘要
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleSummary {
        private String moduleName;
        private int total;
        private int passed;
        private int failed;
        private int skipped;
        private int error;
        private double passRate;
        private long avgResponseTimeMs;
        private long maxResponseTimeMs;
        private long minResponseTimeMs;
    }

    /**
     * 添加测试结果
     */
    public void addResult(TestResult result) {
        results.add(result);
        totalCases++;

        switch (result.getStatus()) {
            case PASSED -> passed++;
            case FAILED -> {
                failed++;
                failures.add(result);
            }
            case SKIPPED -> skipped++;
            case ERROR -> {
                error++;
                failures.add(result);
            }
        }

        // 更新按状态分组
        statusResults.computeIfAbsent(result.getStatus(), k -> new ArrayList<>()).add(result);

        // 更新按模块分组
        if (result.getModule() != null) {
            moduleSummaries.computeIfAbsent(result.getModule(), k -> ModuleSummary.builder()
                    .moduleName(result.getModule())
                    .total(0).passed(0).failed(0).skipped(0).error(0)
                    .build());
            updateModuleSummary(result);
        }

        // 更新按阶段分组
        phaseResults.computeIfAbsent(result.getPhase(), k -> new ArrayList<>()).add(result);
    }

    private void updateModuleSummary(TestResult result) {
        ModuleSummary summary = moduleSummaries.get(result.getModule());
        summary.setTotal(summary.getTotal() + 1);
        switch (result.getStatus()) {
            case PASSED -> summary.setPassed(summary.getPassed() + 1);
            case FAILED -> summary.setFailed(summary.getFailed() + 1);
            case SKIPPED -> summary.setSkipped(summary.getSkipped() + 1);
            case ERROR -> summary.setError(summary.getError() + 1);
        }
        long responseTime = result.getResponseTimeMs();
        long currentAvg = summary.getAvgResponseTimeMs();
        int count = summary.getTotal();
        summary.setAvgResponseTimeMs((currentAvg * (count - 1) + responseTime) / count);
        summary.setMaxResponseTimeMs(Math.max(summary.getMaxResponseTimeMs(), responseTime));
        if (summary.getMinResponseTimeMs() == 0) {
            summary.setMinResponseTimeMs(responseTime);
        } else {
            summary.setMinResponseTimeMs(Math.min(summary.getMinResponseTimeMs(), responseTime));
        }
        summary.setPassRate(summary.getTotal() > 0 ? (summary.getPassed() * 100.0 / summary.getTotal()) : 0);
    }

    /**
     * 计算总耗时
     */
    public void calculateDuration() {
        if (results.isEmpty()) {
            totalDurationMs = 0;
            avgResponseTimeMs = 0;
            return;
        }
        LocalDateTime earliest = results.stream()
                .map(TestResult::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime latest = results.stream()
                .map(TestResult::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (earliest != null && latest != null) {
            totalDurationMs = java.time.Duration.between(earliest, latest).toMillis();
        }
        avgResponseTimeMs = (long) results.stream()
                .filter(r -> r.getResponseTimeMs() > 0)
                .mapToLong(TestResult::getResponseTimeMs)
                .summaryStatistics()
                .getAverage();
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        if (totalCases == 0) return 0;
        return (passed * 100.0) / totalCases;
    }

    /**
     * 获取通过率字符串
     */
    public String getPassRateString() {
        return String.format("%.2f%%", getSuccessRate());
    }

    /**
     * 是否全部通过
     */
    public boolean isAllPassed() {
        return failed == 0 && error == 0;
    }

    /**
     * 获取执行时间描述
     */
    public String getDurationString() {
        if (totalDurationMs < 1000) {
            return totalDurationMs + "ms";
        } else if (totalDurationMs < 60000) {
            return String.format("%.2fs", totalDurationMs / 1000.0);
        } else {
            return String.format("%.2fmin", totalDurationMs / 60000.0);
        }
    }

    /**
     * 获取时间戳字符串
     */
    public String getTimestampString() {
        return generatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 摘要信息
     */
    public String getSummary() {
        return String.format(
                "Total: %d | Passed: %d | Failed: %d | Skipped: %d | Error: %d | Duration: %s | Pass Rate: %s",
                totalCases, passed, failed, skipped, error, getDurationString(), getPassRateString()
        );
    }
}
