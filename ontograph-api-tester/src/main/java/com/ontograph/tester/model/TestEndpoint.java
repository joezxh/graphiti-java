package com.ontograph.tester.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 测试端点元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestEndpoint {

    /**
     * 端点唯一标识 (模块名:HTTP方法:路径)
     */
    private String id;

    /**
     * 所属模块
     */
    private String module;

    /**
     * API标签（来自Swagger）
     */
    private String tag;

    /**
     * HTTP方法
     */
    private HttpMethod method;

    /**
     * API路径
     */
    private String path;

    /**
     * 端点描述
     */
    private String description;

    /**
     * 需要的认证级别
     */
    private AuthLevel authLevel;

    /**
     * 请求头
     */
    private Map<String, String> headers;

    /**
     * 路径参数定义
     */
    private List<ParamDefinition> pathParams;

    /**
     * 查询参数定义
     */
    private List<ParamDefinition> queryParams;

    /**
     * 请求体定义（JSON Schema）
     */
    private String requestBodySchema;

    /**
     * 是否需要测试（通过过滤规则确定）
     */
    private boolean enabled;

    /**
     * 是否为自动发现的端点
     */
    private boolean autoDiscovered;

    /**
     * HTTP方法枚举
     */
    public enum HttpMethod {
        GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
    }

    /**
     * 认证级别
     */
    public enum AuthLevel {
        NONE,       // 不需要认证
        OPTIONAL,   // 可选认证
        REQUIRED    // 必须认证
    }

    /**
     * 参数定义
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParamDefinition {
        private String name;
        private String type;
        private boolean required;
        private String description;
        private Object defaultValue;
        private String example;
    }

    public String getFullPath(String baseUrl) {
        return baseUrl + path;
    }
}
