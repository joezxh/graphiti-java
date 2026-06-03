package com.ontograph.tester.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.tester.client.ApiHttpClient;
import com.ontograph.tester.config.TesterConfig;
import com.ontograph.tester.model.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JWT认证管理器
 * 处理登录、Token生命周期、自动刷新
 */
@Slf4j
public class JwtAuthManager {

    private final TesterConfig config;
    private final ApiHttpClient httpClient;
    private final ObjectMapper mapper;

    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private final AtomicReference<Instant> tokenExpiry = new AtomicReference<>();
    private volatile boolean authenticated = false;

    public JwtAuthManager(TesterConfig config, ApiHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        this.mapper = new ObjectMapper();
    }

    /**
     * 执行登录并获取Token
     */
    public boolean login() {
        String loginUrl = config.getServer().getBaseUrl() + config.getAuth().getLoginPath();

        Map<String, Object> loginBody = Map.of(
                "username", config.getAuth().getUsername(),
                "password", config.getAuth().getPassword()
        );

        try {
            log.info("正在登录用户: {}", config.getAuth().getUsername());
            HttpResponse response = httpClient.post(loginUrl, null, loginBody);

            if (response.isSuccess() && response.getCommonResultCode() != null
                    && response.getCommonResultCode() == 200) {
                Object data = response.getCommonResultData();
                if (data != null) {
                    String token = extractToken(data);
                    Long expiresIn = extractExpiresIn(data);

                    if (token != null && !token.isBlank()) {
                        accessToken.set(token);
                        if (expiresIn != null && expiresIn > 0) {
                            tokenExpiry.set(Instant.now().plusSeconds(expiresIn));
                        } else {
                            // 默认24小时
                            tokenExpiry.set(Instant.now().plusSeconds(86400));
                        }
                        authenticated = true;
                        log.info("登录成功，Token有效期至: {}", tokenExpiry.get());
                        return true;
                    }
                }
            }

            log.error("登录失败，状态码: {}, 响应: {}", response.getStatusCode(),
                    response.getTruncatedBody(200));
            return false;

        } catch (Exception e) {
            log.error("登录请求异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 确保Token有效（自动刷新）
     */
    public String ensureToken() {
        if (!authenticated || accessToken.get() == null) {
            if (!login()) {
                throw new IllegalStateException("无法获取有效的认证Token");
            }
        }

        // 检查是否需要刷新
        if (needsRefresh()) {
            log.info("Token即将过期，尝试刷新...");
            if (!login()) {
                throw new IllegalStateException("Token刷新失败");
            }
        }

        return accessToken.get();
    }

    /**
     * 判断Token是否需要刷新
     */
    private boolean needsRefresh() {
        Instant expiry = tokenExpiry.get();
        if (expiry == null) return true;

        long threshold = config.getAuth().getRefreshThresholdSeconds();
        return Instant.now().plusSeconds(threshold).isAfter(expiry);
    }

    /**
     * 获取带认证头的请求头
     */
    public Map<String, String> getAuthHeaders() {
        String token = ensureToken();
        String prefix = config.getAuth().getBearerPrefix();
        return Map.of("Authorization", prefix + token);
    }

    /**
     * 构建完整的认证头字符串
     */
    public String getAuthorizationHeader() {
        String token = ensureToken();
        return config.getAuth().getBearerPrefix() + token;
    }

    /**
     * 登出
     */
    public void logout() {
        String logoutUrl = config.getServer().getBaseUrl() + "/api/v1/auth/logout";
        try {
            httpClient.post(logoutUrl, getAuthHeaders(), null);
            log.info("已登出");
        } catch (Exception e) {
            log.debug("登出请求失败（可忽略）: {}", e.getMessage());
        } finally {
            accessToken.set(null);
            tokenExpiry.set(null);
            authenticated = false;
        }
    }

    /**
     * 是否已认证
     */
    public boolean isAuthenticated() {
        return authenticated && accessToken.get() != null;
    }

    private String extractToken(Object data) {
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object token = dataMap.get("token");
            if (token != null) return token.toString();
        }
        // 尝试直接从JSON字符串提取
        try {
            String json = mapper.writeValueAsString(data);
            return com.jayway.jsonpath.JsonPath.read(json, "$.token");
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractExpiresIn(Object data) {
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) data;
            Object expiresIn = dataMap.get("expiresIn");
            if (expiresIn instanceof Number) {
                return ((Number) expiresIn).longValue();
            }
        }
        try {
            String json = mapper.writeValueAsString(data);
            Object val = com.jayway.jsonpath.JsonPath.read(json, "$.expiresIn");
            if (val instanceof Number) {
                return ((Number) val).longValue();
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    /**
     * 重置认证状态（用于重新测试）
     */
    public void reset() {
        accessToken.set(null);
        tokenExpiry.set(null);
        authenticated = false;
    }
}
