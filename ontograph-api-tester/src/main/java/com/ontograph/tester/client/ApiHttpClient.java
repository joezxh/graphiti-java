package com.ontograph.tester.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ontograph.tester.config.TesterConfig;
import com.ontograph.tester.model.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * API HTTP客户端
 * 基于Apache HttpClient 5，支持连接池、重试、超时
 */
@Slf4j
public class ApiHttpClient {

    private final TesterConfig config;
    private final CloseableHttpClient httpClient;
    private final ObjectMapper mapper;
    private final HttpClientConfig clientConfig;

    public ApiHttpClient(TesterConfig config) {
        this.config = config;
        this.mapper = createObjectMapper();
        this.clientConfig = new HttpClientConfig(config);
        this.httpClient = buildHttpClient();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.findAndRegisterModules();
        m.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }

    private CloseableHttpClient buildHttpClient() {
        try {
            // 连接池
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(clientConfig.maxTotalConnections);
            connectionManager.setDefaultMaxPerRoute(clientConfig.maxPerRouteConnections);

            // 请求配置 - HttpClient 5.x uses Timeout
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(Timeout.of(clientConfig.connectTimeout, TimeUnit.MILLISECONDS))
                    .setResponseTimeout(Timeout.of(clientConfig.requestTimeout, TimeUnit.MILLISECONDS))
                    .setRedirectsEnabled(false) // 不自动重定向，由业务逻辑处理
                    .build();

            // 重试策略
            DefaultHttpRequestRetryStrategy retryStrategy = new DefaultHttpRequestRetryStrategy(
                    clientConfig.retryCount,
                    Timeout.of(clientConfig.retryInterval, TimeUnit.MILLISECONDS)
            );

            CloseableHttpClient client = org.apache.hc.client5.http.impl.classic.HttpClientBuilder.create()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .setRetryStrategy(retryStrategy)
                    .evictExpiredConnections()
                    .build();

            log.info("HTTP客户端初始化完成: 连接池={}, 重试={}", clientConfig.maxTotalConnections, clientConfig.retryCount);
            return client;

        } catch (Exception e) {
            log.error("创建HTTP客户端失败: {}", e.getMessage());
            throw new RuntimeException("无法创建HTTP客户端", e);
        }
    }

    /**
     * GET请求
     */
    public HttpResponse get(String url, Map<String, String> headers) {
        return execute(new HttpGet(url), headers, null);
    }

    public HttpResponse get(String url, Map<String, String> headers, Map<String, Object> queryParams) {
        String fullUrl = buildUrl(url, queryParams);
        return execute(new HttpGet(fullUrl), headers, null);
    }

    /**
     * POST请求
     */
    public HttpResponse post(String url, Map<String, String> headers, Object body) {
        return execute(new HttpPost(url), headers, body);
    }

    /**
     * PUT请求
     */
    public HttpResponse put(String url, Map<String, String> headers, Object body) {
        return execute(new HttpPut(url), headers, body);
    }

    /**
     * DELETE请求
     */
    public HttpResponse delete(String url, Map<String, String> headers) {
        return execute(new HttpDelete(url), headers, null);
    }

    /**
     * PATCH请求
     */
    public HttpResponse patch(String url, Map<String, String> headers, Object body) {
        return execute(new HttpPatch(url), headers, body);
    }

    /**
     * 执行HTTP请求
     */
    public HttpResponse execute(HttpUriRequestBase request, Map<String, String> headers, Object body) {
        long startTime = System.currentTimeMillis();

        // 设置默认头
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-Type", "application/json");

        // 设置自定义头
        if (headers != null) {
            headers.forEach(request::setHeader);
        }

        // 设置请求体
        if (body != null && request instanceof HttpEntityContainer) {
            try {
                String jsonBody = mapper.writeValueAsString(body);
                ((HttpEntityContainer) request).setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
            } catch (Exception e) {
                log.warn("序列化请求体失败: {}", e.getMessage());
            }
        }

        try {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return buildResponse(response, startTime);
            }
        } catch (IOException e) {
            String uriStr = "unknown";
            try {
                uriStr = request.getUri().toString();
            } catch (Exception ignored) {}
            log.error("HTTP请求失败 [{} {}]: {}", request.getMethod(), uriStr, e.getMessage());
            HttpResponse errorResponse = new HttpResponse();
            errorResponse.setStatusCode(0);
            errorResponse.setBody("Request failed: " + e.getMessage());
            errorResponse.setResponseTimeMs(System.currentTimeMillis() - startTime);
            return errorResponse;
        } catch (Exception e) {
            log.error("HTTP请求异常 [{}]: {}", request.getMethod(), e.getMessage());
            HttpResponse errorResponse = new HttpResponse();
            errorResponse.setStatusCode(0);
            errorResponse.setBody("Request failed: " + e.getMessage());
            errorResponse.setResponseTimeMs(System.currentTimeMillis() - startTime);
            return errorResponse;
        }
    }

    private HttpResponse buildResponse(CloseableHttpResponse response, long startTime) {
        HttpResponse result = new HttpResponse();

        result.setStatusCode(response.getCode());
        result.setStatusText(response.getReasonPhrase());

        // 响应头
        Map<String, String> respHeaders = new HashMap<>();
        Header[] allHeaders = response.getHeaders();
        for (Header h : allHeaders) {
            respHeaders.put(h.getName(), h.getValue());
        }
        result.setHeaders(respHeaders);

        // 响应体
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                result.setBody(body);
            }
        } catch (Exception e) {
            log.warn("读取响应体失败: {}", e.getMessage());
            result.setBody("");
        }

        result.setResponseTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * 构建带查询参数的URL
     */
    public String buildUrl(String baseUrl, Map<String, Object> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return baseUrl;
        }

        StringBuilder sb = new StringBuilder(baseUrl);
        boolean first = !baseUrl.contains("?");

        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            if (entry.getValue() != null) {
                sb.append(first ? "?" : "&");
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                sb.append("=");
                sb.append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
                first = false;
            }
        }
        return sb.toString();
    }

    /**
     * 序列化对象为JSON
     */
    public String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("序列化对象失败: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 反序列化JSON
     */
    public <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            log.warn("反序列化JSON失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 关闭客户端
     */
    public void close() {
        try {
            httpClient.close();
            log.info("HTTP客户端已关闭");
        } catch (IOException e) {
            log.warn("关闭HTTP客户端时出错: {}", e.getMessage());
        }
    }

    /**
     * HTTP客户端配置
     */
    public static class HttpClientConfig {
        final int connectTimeout;
        final int readTimeout;
        final int requestTimeout;
        final int maxTotalConnections;
        final int maxPerRouteConnections;
        final int retryCount;
        final int retryInterval;

        HttpClientConfig(TesterConfig config) {
            TesterConfig.HttpConfig http = config.getHttp();
            this.connectTimeout = http.getConnectTimeoutMs();
            this.readTimeout = http.getReadTimeoutMs();
            this.requestTimeout = http.getRequestTimeoutMs();
            this.maxTotalConnections = http.getMaxTotalConnections();
            this.maxPerRouteConnections = http.getMaxPerRouteConnections();
            this.retryCount = http.getRetryCount();
            this.retryInterval = http.getRetryIntervalMs();
        }
    }
}
