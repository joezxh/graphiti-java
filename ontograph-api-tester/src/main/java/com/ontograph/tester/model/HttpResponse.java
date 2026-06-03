package com.ontograph.tester.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 原始HTTP响应封装
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpResponse {

    /**
     * HTTP状态码
     */
    private int statusCode;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 响应头
     */
    @Builder.Default
    private java.util.Map<String, String> headers = new java.util.HashMap<>();

    /**
     * 响应体（原始字符串）
     */
    private String body;

    /**
     * 响应时间（毫秒）
     */
    private long responseTimeMs;

    /**
     * 是否成功（2xx）
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * 是否是客户端错误
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * 是否是服务端错误
     */
    public boolean isServerError() {
        return statusCode >= 500;
    }

    /**
     * 获取Content-Type
     */
    public String getContentType() {
        return headers.getOrDefault("Content-Type", "");
    }

    /**
     * 获取响应体的JSON对象
     */
    public Object getBodyAsJson() {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(body, Object.class);
        } catch (Exception e) {
            return body;
        }
    }

    /**
     * 获取CommonResult的data字段
     */
    public Object getCommonResultData() {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return com.jayway.jsonpath.JsonPath.read(body, "$.data");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取CommonResult的code字段
     */
    public Integer getCommonResultCode() {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return com.jayway.jsonpath.JsonPath.read(body, "$.code");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取CommonResult的message字段
     */
    public String getCommonResultMessage() {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return com.jayway.jsonpath.JsonPath.read(body, "$.message");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取截断的响应体（用于报告）
     */
    public String getTruncatedBody(int maxLength) {
        if (body == null) return null;
        if (body.length() <= maxLength) return body;
        return body.substring(0, maxLength) + "... [truncated]";
    }
}
