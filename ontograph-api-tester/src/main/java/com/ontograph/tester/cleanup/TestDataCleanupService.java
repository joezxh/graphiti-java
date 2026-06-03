package com.ontograph.tester.cleanup;

import com.ontograph.tester.client.ApiHttpClient;
import com.ontograph.tester.config.TesterConfig;
import com.ontograph.tester.model.HttpResponse;
import com.ontograph.tester.model.TestCase;
import com.ontograph.tester.model.TestResult;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 测试数据清理器
 * 在测试结束后清理创建的测试数据（按倒序执行删除操作）
 */
@Slf4j
public class TestDataCleanupService {

    private final TesterConfig config;
    private final ApiHttpClient httpClient;
    private final String authHeader;

    /**
     * 需要清理的资源记录
     */
    private final Map<String, List<CleanupResource>> resources = new ConcurrentHashMap<>();

    public TestDataCleanupService(TesterConfig config, ApiHttpClient httpClient, String authHeader) {
        this.config = config;
        this.httpClient = httpClient;
        this.authHeader = authHeader;
    }

    /**
     * 记录需要清理的资源
     */
    public void registerResource(String phase, CleanupResource resource) {
        resources.computeIfAbsent(phase, k -> new ArrayList<>()).add(resource);
        log.debug("注册清理资源: {} {}", resource.type, resource.id);
    }

    /**
     * 注册图谱资源
     */
    public void registerGraphResource(String graphId) {
        if (graphId == null || graphId.isBlank()) return;
        registerResource("graph", new CleanupResource("graph", graphId, "/api/v1/graph/" + graphId));
    }

    /**
     * 注册节点资源
     */
    public void registerNodeResource(String nodeUuid) {
        if (nodeUuid == null || nodeUuid.isBlank()) return;
        registerResource("node", new CleanupResource("node", nodeUuid, "/api/v1/nodes/" + nodeUuid));
    }

    /**
     * 注册边资源
     */
    public void registerEdgeResource(String graphId, String edgeUuid) {
        if (edgeUuid == null || edgeUuid.isBlank()) return;
        String path = "/api/v1/graph/edge/" + graphId + "/" + edgeUuid;
        registerResource("edge", new CleanupResource("edge", edgeUuid, path));
    }

    /**
     * 注册剧集资源
     */
    public void registerEpisodeResource(String graphId, String episodeUuid) {
        if (episodeUuid == null || episodeUuid.isBlank()) return;
        String path = "/api/v1/graph/episode/" + graphId + "/" + episodeUuid;
        registerResource("episode", new CleanupResource("episode", episodeUuid, path));
    }

    /**
     * 从TestResult中自动提取资源ID
     */
    public void autoExtractAndRegister(TestResult result, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return;

        // 尝试提取各种资源ID
        String[] patterns = {
                "\"uuid\":\"([^\"]+)\"",
                "\"graphId\":\"([^\"]+)\"",
                "\"graph_uuid\":\"([^\"]+)\"",
                "\"id\":\"([^\"]+)\"",
                "\"taskId\":\"([^\"]+)\""
        };

        Set<String> found = new HashSet<>();
        for (String pattern : patterns) {
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                var matcher = p.matcher(responseBody);
                while (matcher.find()) {
                    String id = matcher.group(1);
                    if (id != null && !id.isBlank() && id.length() > 10) {
                        found.add(id);
                    }
                }
            } catch (Exception ignored) {}
        }

        for (String id : found) {
            if (result.getCaseId().contains("graph") && !result.getCaseId().contains("detail")) {
                registerGraphResource(id);
            } else if (result.getCaseId().contains("node")) {
                registerNodeResource(id);
            } else if (result.getCaseId().contains("edge")) {
                registerEdgeResource(null, id);
            } else if (result.getCaseId().contains("episode")) {
                registerEpisodeResource(null, id);
            }
        }
    }

    /**
     * 执行清理
     */
    public CleanupReport executeCleanup() {
        if (!config.getTest().isAutoCleanup()) {
            log.info("自动清理已禁用，跳过清理");
            return CleanupReport.skipped();
        }

        CleanupReport report = new CleanupReport();
        report.startTime = LocalDateTime.now();

        log.info("========== 开始清理测试数据 ==========");

        // 删除顺序：episode -> edge -> node -> graph
        List<String> deleteOrder = List.of("episode", "edge", "node", "graph");

        for (String phase : deleteOrder) {
            List<CleanupResource> phaseResources = resources.get(phase);
            if (phaseResources == null || phaseResources.isEmpty()) {
                continue;
            }

            log.info("清理 {} 类型资源 (共 {} 个)...", phase, phaseResources.size());

            for (CleanupResource resource : phaseResources) {
                CleanupResult result = deleteResource(resource);
                report.addResult(result);

                if (result.success) {
                    report.successCount++;
                } else {
                    report.failCount++;
                    if (!config.getTest().isCleanupFailContinue()) {
                        log.error("清理失败且配置为不继续，停止清理");
                        break;
                    }
                }
            }
        }

        report.endTime = LocalDateTime.now();
        report.totalDurationMs = java.time.Duration.between(report.startTime, report.endTime).toMillis();

        log.info("========== 清理完成: 成功={}, 失败={}, 耗时={}ms ==========",
                report.successCount, report.failCount, report.totalDurationMs);

        return report;
    }

    private CleanupResult deleteResource(CleanupResource resource) {
        CleanupResult result = new CleanupResult();
        result.resourceType = resource.type;
        result.resourceId = resource.id;

        long start = System.currentTimeMillis();

        try {
            Map<String, String> headers = Map.of(
                    "Authorization", authHeader,
                    "Content-Type", "application/json"
            );

            // 根据资源类型选择删除URL和方式
            HttpResponse response;
            if (resource.type.equals("graph")) {
                response = httpClient.delete(resource.deleteUrl, headers);
            } else if (resource.type.equals("node")) {
                response = httpClient.delete(resource.deleteUrl, headers);
            } else if (resource.type.equals("edge") || resource.type.equals("episode")) {
                response = httpClient.delete(resource.deleteUrl, headers);
            } else {
                // 默认DELETE
                response = httpClient.delete(resource.deleteUrl, headers);
            }

            result.statusCode = response.getStatusCode();
            result.responseTimeMs = System.currentTimeMillis() - start;

            // 成功：2xx 或 404（资源不存在）
            result.success = response.isSuccess() || response.getStatusCode() == 404;

            if (result.success) {
                log.debug("清理成功: {} {}", resource.type, resource.id);
            } else {
                result.errorMessage = String.format("HTTP %d", response.getStatusCode());
                log.warn("清理失败: {} {} -> HTTP {}", resource.type, resource.id, response.getStatusCode());
            }

        } catch (Exception e) {
            result.success = false;
            result.errorMessage = e.getMessage();
            result.responseTimeMs = System.currentTimeMillis() - start;
            log.warn("清理异常: {} {} -> {}", resource.type, resource.id, e.getMessage());
        }

        return result;
    }

    /**
     * 清理资源记录
     */
    public void clear() {
        resources.clear();
    }

    /**
     * 获取清理统计
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, List<CleanupResource>> entry : resources.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().size());
        }
        return stats;
    }

    /**
     * 清理资源描述
     */
    public record CleanupResource(String type, String id, String deleteUrl) {}

    /**
     * 单个清理结果
     */
    public static class CleanupResult {
        public String resourceType;
        public String resourceId;
        public boolean success;
        public int statusCode;
        public long responseTimeMs;
        public String errorMessage;

        @Override
        public String toString() {
            return String.format("%s[%s]: %s (HTTP %d, %dms)",
                    resourceType, resourceId, success ? "OK" : "FAIL", statusCode, responseTimeMs);
        }
    }

    /**
     * 清理报告
     */
    public static class CleanupReport {
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public long totalDurationMs;
        public int successCount = 0;
        public int failCount = 0;
        public List<CleanupResult> results = new ArrayList<>();
        public boolean skipped = false;

        public void addResult(CleanupResult result) {
            results.add(result);
        }

        public static CleanupReport skipped() {
            CleanupReport r = new CleanupReport();
            r.skipped = true;
            return r;
        }

        public String getSummary() {
            if (skipped) return "Cleanup skipped (disabled)";
            return String.format("Cleanup: %d success, %d failed, %dms total",
                    successCount, failCount, totalDurationMs);
        }
    }
}
