package com.ontograph.tester.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单个测试结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {

    /**
     * 测试用例ID
     */
    private String caseId;

    /**
     * 测试用例名称
     */
    private String caseName;

    /**
     * 所属模块
     */
    private String module;

    /**
     * 测试阶段
     */
    private TestCase.TestPhase phase;

    /**
     * 测试状态
     */
    @Builder.Default
    private Status status = Status.PENDING;

    /**
     * HTTP状态码
     */
    private int httpStatusCode;

    /**
     * 响应时间（毫秒）
     */
    private long responseTimeMs;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * HTTP方法
     */
    private String httpMethod;

    /**
     * 请求头（JSON字符串）
     */
    private String requestHeaders;

    /**
     * 请求体（JSON字符串）
     */
    private String requestBody;

    /**
     * 响应头（JSON字符串）
     */
    private String responseHeaders;

    /**
     * 响应体（前4KB）
     */
    private String responseBody;

    /**
     * 完整响应体（用于调试）
     */
    private String fullResponseBody;

    /**
     * 断言结果列表
     */
    @Builder.Default
    private List<AssertionResult> assertionResults = new ArrayList<>();

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 异常堆栈
     */
    private String stackTrace;

    /**
     * 关联的资源ID（如创建的图谱ID、节点ID）
     */
    @Builder.Default
    private Map<String, String> resourceIds = new ConcurrentHashMap<>();

    /**
     * 测试状态枚举
     */
    public enum Status {
        PENDING("待执行"),
        RUNNING("执行中"),
        PASSED("通过"),
        FAILED("失败"),
        SKIPPED("跳过"),
        ERROR("错误");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 单个断言结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssertionResult {
        private String assertionType;
        private String jsonPath;
        private Object expected;
        private Object actual;
        private String operator;
        private boolean passed;
        private String message;
        private long elapsedMs;

        public static AssertionResult pass(String type, String jsonPath, Object expected, Object actual) {
            return AssertionResult.builder()
                    .assertionType(type)
                    .jsonPath(jsonPath)
                    .expected(expected)
                    .actual(actual)
                    .passed(true)
                    .build();
        }

        public static AssertionResult fail(String type, String jsonPath, Object expected, Object actual, String message) {
            return AssertionResult.builder()
                    .assertionType(type)
                    .jsonPath(jsonPath)
                    .expected(expected)
                    .actual(actual)
                    .passed(false)
                    .message(message)
                    .build();
        }
    }

    /**
     * 判断是否为成功
     */
    public boolean isSuccess() {
        return status == Status.PASSED;
    }

    /**
     * 获取持续时间描述
     */
    public String getDurationDescription() {
        if (startTime == null || endTime == null) {
            return "N/A";
        }
        long ms = java.time.Duration.between(startTime, endTime).toMillis();
        if (ms < 1000) {
            return ms + "ms";
        } else {
            return String.format("%.2fs", ms / 1000.0);
        }
    }

    /**
     * 存储资源ID
     */
    public void storeResourceId(String key, String value) {
        resourceIds.put(key, value);
    }

    /**
     * 获取资源ID
     */
    public String getResourceId(String key) {
        return resourceIds.get(key);
    }

    /**
     * 提取资源ID的静态方法（从响应JSON中提取）
     */
    public static String extractFromJsonPath(String json, String jsonPath) {
        if (json == null || jsonPath == null) {
            return null;
        }
        try {
            Object result = com.jayway.jsonpath.JsonPath.read(json, jsonPath);
            if (result == null) {
                return null;
            }
            if (result instanceof List) {
                List<?> list = (List<?>) result;
                return list.isEmpty() ? null : String.valueOf(list.get(0));
            }
            return String.valueOf(result);
        } catch (Exception e) {
            return null;
        }
    }
}
