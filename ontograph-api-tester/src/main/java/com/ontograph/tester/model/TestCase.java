package com.ontograph.tester.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个测试用例
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    /**
     * 用例唯一标识
     */
    private String id;

    /**
     * 用例名称
     */
    private String name;

    /**
     * 所属模块
     */
    private String module;

    /**
     * 所属阶段
     */
    private TestPhase phase;

    /**
     * 测试的端点
     */
    private TestEndpoint endpoint;

    /**
     * 前置条件（依赖的其他测试用例ID）
     */
    private List<String> prerequisites;

    /**
     * HTTP方法
     */
    private TestEndpoint.HttpMethod method;

    /**
     * 请求路径（支持模板变量替换）
     */
    private String path;

    /**
     * 查询参数
     */
    @Builder.Default
    private Map<String, Object> queryParams = new HashMap<>();

    /**
     * 路径参数值
     */
    @Builder.Default
    private Map<String, String> pathParams = new HashMap<>();

    /**
     * 请求头
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * 请求体（JSON字符串或Map）
     */
    private Object requestBody;

    /**
     * 断言列表
     */
    @Builder.Default
    private List<Assertion> assertions = new ArrayList<>();

    /**
     * 测试阶段
     */
    public enum TestPhase {
        /**
         * 前置阶段：认证、初始化
         */
        SETUP,
        /**
         * 创建阶段：新增资源
         */
        CREATE,
        /**
         * 查询阶段：读取资源
         */
        READ,
        /**
         * 更新阶段：修改资源
         */
        UPDATE,
        /**
         * 删除阶段：删除资源
         */
        DELETE,
        /**
         * 清理阶段：测试后数据清理
         */
        CLEANUP,
        /**
         * 独立测试：不依赖CRUD流程
         */
        INDEPENDENT
    }

    /**
     * 断言定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Assertion {
        /**
         * 断言类型
         */
        private AssertionType type;

        /**
         * JSON路径表达式（用于提取响应字段）
         */
        private String jsonPath;

        /**
         * 期望值
         */
        private Object expectedValue;

        /**
         * 比较运算符
         */
        private ComparisonOperator operator;

        /**
         * 自定义错误消息
         */
        private String message;

        /**
         * 断言类型枚举
         */
        public enum AssertionType {
            STATUS_CODE,      // HTTP状态码
            JSON_PATH,        // JSON字段值
            JSON_PATH_EXISTS, // JSON字段存在
            JSON_PATH_NOT_NULL, // JSON字段非空
            HEADER_EXISTS,    // 响应头存在
            HEADER_VALUE,     // 响应头值
            RESPONSE_TIME,    // 响应时间
            BODY_NOT_EMPTY,   // 响应体非空
            BODY_CONTAINS,    // 响应体包含
            CUSTOM           // 自定义验证
        }

        /**
         * 比较运算符
         */
        public enum ComparisonOperator {
            EQUALS,
            NOT_EQUALS,
            GREATER_THAN,
            LESS_THAN,
            GREATER_THAN_OR_EQUALS,
            LESS_THAN_OR_EQUALS,
            CONTAINS,
            NOT_CONTAINS,
            MATCHES,
            IN
        }
    }

    /**
     * 创建快速断言的静态方法
     */
    public static Assertion statusCode(int expected) {
        return Assertion.builder()
                .type(Assertion.AssertionType.STATUS_CODE)
                .expectedValue(expected)
                .operator(Assertion.ComparisonOperator.EQUALS)
                .build();
    }

    public static Assertion jsonPathEquals(String jsonPath, Object expected) {
        return Assertion.builder()
                .type(Assertion.AssertionType.JSON_PATH)
                .jsonPath(jsonPath)
                .expectedValue(expected)
                .operator(Assertion.ComparisonOperator.EQUALS)
                .build();
    }

    public static Assertion jsonPathNotNull(String jsonPath) {
        return Assertion.builder()
                .type(Assertion.AssertionType.JSON_PATH_NOT_NULL)
                .jsonPath(jsonPath)
                .build();
    }

    public static Assertion responseTimeLessThan(long ms) {
        return Assertion.builder()
                .type(Assertion.AssertionType.RESPONSE_TIME)
                .expectedValue(ms)
                .operator(Assertion.ComparisonOperator.LESS_THAN)
                .build();
    }

    public static Assertion bodyContains(String text) {
        return Assertion.builder()
                .type(Assertion.AssertionType.BODY_CONTAINS)
                .expectedValue(text)
                .operator(Assertion.ComparisonOperator.CONTAINS)
                .build();
    }
}
