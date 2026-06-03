package com.ontograph.tester.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ontograph.tester.assertion.AssertionEngine;
import com.ontograph.tester.auth.JwtAuthManager;
import com.ontograph.tester.cleanup.TestDataCleanupService;
import com.ontograph.tester.client.ApiHttpClient;
import com.ontograph.tester.config.TesterConfig;
import com.ontograph.tester.discovery.ManualEndpointRegistry;
import com.ontograph.tester.discovery.OpenApiDiscoveryService;
import com.ontograph.tester.model.*;
import com.ontograph.tester.report.TestReporter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 测试运行器
 * 核心编排：按阶段顺序执行测试用例，收集结果，驱动报告生成
 */
@Slf4j
public class TestRunner {

    private final TesterConfig config;
    private final ApiHttpClient httpClient;
    private final JwtAuthManager authManager;
    private final AssertionEngine assertionEngine;
    private final ManualEndpointRegistry endpointRegistry;
    private final TestDataCleanupService cleanupService;
    private final ObjectMapper mapper;
    private final TestReporter reporter;

    private final Map<String, String> testContext = new ConcurrentHashMap<>();
    private final Map<String, String> createdResourceIds = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public TestRunner(TesterConfig config, ApiHttpClient httpClient, JwtAuthManager authManager,
                      ManualEndpointRegistry endpointRegistry, TestDataCleanupService cleanupService,
                      TestReporter reporter) {
        this.config = config;
        this.httpClient = httpClient;
        this.authManager = authManager;
        this.endpointRegistry = endpointRegistry;
        this.cleanupService = cleanupService;
        this.reporter = reporter;
        this.assertionEngine = new AssertionEngine(config.getTest().getMaxResponseBodyLength());
        this.mapper = createObjectMapper();

        int concurrency = config.getTest().getConcurrency();
        if (concurrency > 0) {
            this.executor = Executors.newFixedThreadPool(concurrency);
            log.info("测试将使用 {} 个并发线程执行", concurrency);
        } else {
            this.executor = null;
            log.info("测试将串行执行");
        }
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.findAndRegisterModules();
        m.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }

    /**
     * 执行完整测试流程
     */
    public TestReport run() {
        TestReport report = TestReport.builder()
                .generatedAt(LocalDateTime.now())
                .environment(detectEnvironment())
                .targetUrl(config.getServer().getBaseUrl())
                .build();

        try {
            // 0. 等待服务启动
            if (config.getTest().getStartupDelaySeconds() > 0) {
                log.info("等待 {} 秒后开始测试...",
                        config.getTest().getStartupDelaySeconds());
                Thread.sleep(config.getTest().getStartupDelaySeconds() * 1000L);
            }

            // 1. 前置验证：服务可达性检查
            if (!preflightCheck(report)) {
                reporter.onTestRunComplete(report);
                return report;
            }

            // 2. 初始化测试上下文
            initializeContext();

            // 3. 认证
            if (!authenticate(report)) {
                reporter.onTestRunComplete(report);
                return report;
            }

            // 4. 发现端点（如果启用）
            List<TestEndpoint> discoveredEndpoints = Collections.emptyList();
            if (config.getTest().isAutoDiscoverEnabled()) {
                discoveredEndpoints = discoverEndpoints();
            }

            // 5. 构建测试用例
            List<TestCase> allCases = buildTestCases(discoveredEndpoints);

            if (allCases.isEmpty()) {
                log.warn("没有找到任何测试用例");
                reporter.onTestRunComplete(report);
                return report;
            }

            log.info("共 {} 个测试用例待执行", allCases.size());

            // 6. 按阶段执行测试
            executeByPhase(allCases, report);

            // 7. 清理测试数据
            TestDataCleanupService.CleanupReport cleanupReport = cleanupService.executeCleanup();
            report.getResults().add(buildCleanupResult(cleanupReport));

            // 8. 计算报告统计
            report.calculateDuration();

            // 9. 登出
            try {
                authManager.logout();
            } catch (Exception ignored) {}

        } catch (Exception e) {
            log.error("测试执行异常: {}", e.getMessage(), e);
            TestResult errorResult = TestResult.builder()
                    .caseId("fatal_error")
                    .caseName("测试执行异常")
                    .status(TestResult.Status.ERROR)
                    .failureReason(e.getMessage())
                    .stackTrace(getStackTrace(e))
                    .build();
            report.addResult(errorResult);
        }

        // 最终报告
        reporter.onTestRunComplete(report);
        return report;
    }

    /**
     * 前置检查：验证服务可达
     */
    private boolean preflightCheck(TestReport report) {
        log.info("========== 前置检查：验证服务可达性 ==========");
        String baseUrl = config.getServer().getBaseUrl();
        String healthUrl = baseUrl + "/actuator/health";

        TestResult result = TestResult.builder()
                .caseId("preflight")
                .caseName("服务可达性检查")
                .module("system")
                .phase(TestCase.TestPhase.SETUP)
                .startTime(LocalDateTime.now())
                .requestUrl(healthUrl)
                .httpMethod("GET")
                .build();

        try {
            HttpResponse response = httpClient.get(healthUrl, null);
            result.setEndTime(LocalDateTime.now());
            result.setHttpStatusCode(response.getStatusCode());
            result.setResponseTimeMs(response.getResponseTimeMs());
            result.setResponseBody(response.getTruncatedBody(config.getTest().getMaxResponseBodyLength()));

            // 也检查API Docs
            if (response.getStatusCode() != 200) {
                String apiDocsUrl = baseUrl + config.getServer().getApiDocsPath();
                HttpResponse docsResponse = httpClient.get(apiDocsUrl, null);
                if (docsResponse.getStatusCode() == 200) {
                    result.setHttpStatusCode(200); // API Docs可达即可
                }
            }

            if (response.isSuccess() || response.getStatusCode() == 404) {
                result.setStatus(TestResult.Status.PASSED);
                log.info("服务可达: {}", baseUrl);
                report.addResult(result);
                return true;
            } else {
                result.setStatus(TestResult.Status.FAILED);
                result.setFailureReason(String.format("服务不可达，HTTP %d", response.getStatusCode()));
                log.error("服务不可达: {} -> HTTP {}", baseUrl, response.getStatusCode());
                report.addResult(result);
                return false;
            }
        } catch (Exception e) {
            result.setStatus(TestResult.Status.ERROR);
            result.setEndTime(LocalDateTime.now());
            result.setFailureReason("连接失败: " + e.getMessage());
            result.setStackTrace(getStackTrace(e));
            log.error("服务连接失败: {} -> {}", baseUrl, e.getMessage());
            report.addResult(result);
            return false;
        }
    }

    /**
     * 初始化测试上下文
     */
    private void initializeContext() {
        testContext.clear();
        testContext.putAll(endpointRegistry.buildTestContext());

        // 添加当前时间戳
        testContext.put("timestamp", String.valueOf(System.currentTimeMillis()));
        testContext.put("runId", UUID.randomUUID().toString().substring(0, 8));

        log.info("测试上下文已初始化，RunId: {}", testContext.get("runId"));
    }

    /**
     * 认证
     */
    private boolean authenticate(TestReport report) {
        log.info("========== 阶段 0：认证 ==========");

        if (!config.getModules().isAuth()) {
            log.info("认证模块已禁用，跳过");
            return true;
        }

        TestCase loginCase = endpointRegistry.getCasesByModule("auth").stream()
                .filter(c -> c.getId().contains("login"))
                .findFirst()
                .orElse(null);

        if (loginCase == null) {
            log.warn("未找到登录测试用例");
            return true;
        }

        // 手动登录（绕过通用测试执行，直接调用auth manager）
        boolean success = authManager.login();

        TestResult loginResult = TestResult.builder()
                .caseId("auth_login")
                .caseName("用户登录")
                .module("auth")
                .phase(TestCase.TestPhase.SETUP)
                .status(success ? TestResult.Status.PASSED : TestResult.Status.FAILED)
                .httpStatusCode(success ? 200 : 0)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .build();

        if (success) {
            // 验证token
            TestResult infoResult = executeTestCase(endpointRegistry.resolveTemplateVariables(loginCase, testContext));
            report.addResult(infoResult);

            // 提取用户信息中的token
            return infoResult.isSuccess();
        } else {
            loginResult.setFailureReason("登录失败，请检查用户名和密码");
            report.addResult(loginResult);
            return false;
        }
    }

    /**
     * 发现端点
     */
    private List<TestEndpoint> discoverEndpoints() {
        log.info("========== 端点发现 ==========");
        try {
            OpenApiDiscoveryService discovery = new OpenApiDiscoveryService(config, httpClient);
            List<TestEndpoint> endpoints = discovery.discoverEndpoints();
            log.info("发现 {} 个API端点", endpoints.size());
            return endpoints;
        } catch (Exception e) {
            log.warn("端点发现失败（不影响测试）: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 构建测试用例
     */
    private List<TestCase> buildTestCases(List<TestEndpoint> discoveredEndpoints) {
        List<TestCase> cases = new ArrayList<>();

        // 添加手动注册的用例
        if (config.getTest().isManualEndpointsEnabled()) {
            for (TestCase tc : endpointRegistry.getAllCases()) {
                String module = tc.getModule();
                if (!isModuleEnabled(module)) {
                    continue;
                }
                cases.add(tc);
            }
        }

        // 添加自动发现的用例
        if (config.getTest().isAutoDiscoverEnabled() && !discoveredEndpoints.isEmpty()) {
            for (TestEndpoint ep : discoveredEndpoints) {
                if (!ep.isEnabled()) continue;

                // 跳过已经在手动注册中覆盖的端点
                if (isEndpointCoveredManually(ep, cases)) continue;

                // 将自动发现的端点转换为用例（基础状态码检查）
                TestCase autoCase = convertEndpointToCase(ep);
                if (autoCase != null) {
                    cases.add(autoCase);
                }
            }
        }

        // 按阶段排序
        return cases.stream()
                .sorted(Comparator.comparing(tc -> {
                    TestCase.TestPhase p = tc.getPhase();
                    return switch (p) {
                        case SETUP -> 0;
                        case CREATE -> 1;
                        case READ -> 2;
                        case UPDATE -> 3;
                        case DELETE -> 4;
                        case CLEANUP -> 5;
                        case INDEPENDENT -> 6;
                    };
                }))
                .collect(Collectors.toList());
    }

    private boolean isModuleEnabled(String module) {
        TesterConfig.ModulesConfig m = config.getModules();
        return switch (module.toLowerCase()) {
            case "auth" -> m.isAuth();
            case "graph" -> m.isGraph();
            case "node" -> m.isNode();
            case "edge" -> m.isEdge();
            case "episode" -> m.isEpisode();
            case "search" -> m.isSearch();
            case "searchpipeline" -> m.isSearchPipeline();
            case "dataimport" -> m.isDataImport();
            case "ontology" -> m.isOntology();
            case "prompt" -> m.isPrompt();
            case "legal" -> m.isLegal();
            case "businessinfo" -> m.isBusinessInfo();
            case "system" -> m.isSystem();
            default -> true;
        };
    }

    private boolean isEndpointCoveredManually(TestEndpoint ep, List<TestCase> manualCases) {
        for (TestCase tc : manualCases) {
            if (tc.getPath().equals(ep.getPath()) && tc.getMethod() == ep.getMethod()) {
                return true;
            }
        }
        return false;
    }

    private TestCase convertEndpointToCase(TestEndpoint ep) {
        TestCase tc = TestCase.builder()
                .id("auto_" + ep.getId())
                .name(ep.getDescription() != null ? ep.getDescription() : ep.getPath())
                .module(ep.getModule())
                .phase(TestCase.TestPhase.INDEPENDENT)
                .endpoint(ep)
                .method(ep.getMethod())
                .path(ep.getPath())
                .pathParams(new HashMap<>())
                .queryParams(new HashMap<>())
                .headers(new HashMap<>())
                .assertions(new ArrayList<>())
                .build();

        // 基础断言：状态码
        tc.getAssertions().add(TestCase.Assertion.builder()
                .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                .expectedValue(200)
                .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                .message("自动发现端点状态码应为200")
                .build());

        // 可选：添加JSON Path断言
        tc.getAssertions().add(TestCase.Assertion.builder()
                .type(TestCase.Assertion.AssertionType.JSON_PATH)
                .jsonPath("$.code")
                .expectedValue(200)
                .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                .message("业务码应为200")
                .build());

        return tc;
    }

    /**
     * 按阶段执行测试
     */
    private void executeByPhase(List<TestCase> allCases, TestReport report) {
        List<TestCase.TestPhase> phases = List.of(
                TestCase.TestPhase.SETUP,
                TestCase.TestPhase.CREATE,
                TestCase.TestPhase.READ,
                TestCase.TestPhase.UPDATE,
                TestCase.TestPhase.DELETE,
                TestCase.TestPhase.INDEPENDENT
        );

        for (TestCase.TestPhase phase : phases) {
            List<TestCase> phaseCases = allCases.stream()
                    .filter(c -> c.getPhase() == phase)
                    .toList();

            if (phaseCases.isEmpty()) continue;

            log.info("========== 阶段 {}：{} ({}个用例) ==========",
                    phase.ordinal(), getPhaseName(phase), phaseCases.size());

            reporter.onPhaseStart(phase, phaseCases.size());

            for (TestCase tc : phaseCases) {
                TestResult result = executeTestCase(tc);
                report.addResult(result);

                // 从响应中提取资源ID
                extractAndStoreResourceIds(tc, result);

                reporter.onTestResult(result);
            }

            reporter.onPhaseComplete(phase);
        }
    }

    /**
     * 执行单个测试用例
     */
    public TestResult executeTestCase(TestCase tc) {
        TestResult result = TestResult.builder()
                .caseId(tc.getId())
                .caseName(tc.getName())
                .module(tc.getModule())
                .phase(tc.getPhase())
                .httpMethod(tc.getMethod().name())
                .startTime(LocalDateTime.now())
                .build();

        // 构建请求
        String resolvedPath = resolvePath(tc.getPath());
        String fullUrl = config.getServer().getBaseUrl() + resolvedPath;
        result.setRequestUrl(fullUrl);

        try {
            // 构建认证头
            Map<String, String> headers = new HashMap<>();
            if (tc.getEndpoint() != null && tc.getEndpoint().getAuthLevel() == TestEndpoint.AuthLevel.REQUIRED
                    || tc.getEndpoint() == null) {
                headers.put("Authorization", authManager.getAuthorizationHeader());
            }

            // 合并自定义头
            if (tc.getHeaders() != null) {
                headers.putAll(tc.getHeaders());
            }

            // 序列化请求体
            String requestBody = null;
            if (tc.getRequestBody() != null) {
                Object resolvedBody = resolveRequestBody(tc.getRequestBody());
                requestBody = mapper.writeValueAsString(resolvedBody);
                result.setRequestBody(requestBody);
            }

            // 构建查询参数（从tc中合并resolved值）
            Map<String, Object> allQueryParams = new HashMap<>();
            if (tc.getQueryParams() != null) {
                allQueryParams.putAll(tc.getQueryParams());
            }

            // 执行请求
            HttpResponse response;
            switch (tc.getMethod()) {
                case GET -> response = httpClient.get(fullUrl, headers, allQueryParams);
                case POST -> response = httpClient.post(fullUrl, headers, tc.getRequestBody() != null
                        ? resolveRequestBody(tc.getRequestBody()) : null);
                case PUT -> response = httpClient.put(fullUrl, headers, tc.getRequestBody() != null
                        ? resolveRequestBody(tc.getRequestBody()) : null);
                case DELETE -> response = httpClient.delete(fullUrl, headers);
                case PATCH -> response = httpClient.patch(fullUrl, headers, tc.getRequestBody() != null
                        ? resolveRequestBody(tc.getRequestBody()) : null);
                default -> {
                    response = httpClient.get(fullUrl, headers);
                }
            }

            result.setEndTime(LocalDateTime.now());
            result.setHttpStatusCode(response.getStatusCode());
            result.setResponseTimeMs(response.getResponseTimeMs());
            result.setResponseBody(response.getTruncatedBody(config.getTest().getMaxResponseBodyLength()));
            result.setFullResponseBody(response.getBody());

            // 执行断言
            List<TestResult.AssertionResult> assertionResults = assertionEngine.assertAll(response, tc.getAssertions());
            result.setAssertionResults(assertionResults);

            // 判断结果
            boolean passed = response.isSuccess()
                    && assertionEngine.allPassed(assertionResults)
                    && (response.getCommonResultCode() == null || response.getCommonResultCode() == 200);

            result.setStatus(passed ? TestResult.Status.PASSED : TestResult.Status.FAILED);

            if (!passed && !assertionEngine.getFailed(assertionResults).isEmpty()) {
                TestResult.AssertionResult firstFail = assertionEngine.getFailed(assertionResults).get(0);
                result.setFailureReason(firstFail.getMessage());
            } else if (!response.isSuccess()) {
                result.setFailureReason(String.format("HTTP %d", response.getStatusCode()));
            }

        } catch (Exception e) {
            result.setEndTime(LocalDateTime.now());
            result.setStatus(TestResult.Status.ERROR);
            result.setFailureReason(e.getMessage());
            result.setStackTrace(getStackTrace(e));
            log.error("测试用例 {} 执行异常: {}", tc.getId(), e.getMessage());
        }

        return result;
    }

    /**
     * 解析路径中的模板变量
     */
    private String resolvePath(String path) {
        String resolved = path;
        for (Map.Entry<String, String> entry : createdResourceIds.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        for (Map.Entry<String, String> entry : testContext.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return resolved;
    }

    /**
     * 解析请求体中的模板变量
     */
    @SuppressWarnings("unchecked")
    private Object resolveRequestBody(Object body) {
        if (body instanceof Map) {
            Map<String, Object> resolved = new HashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) body).entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    String strVal = (String) value;
                    strVal = resolveString(strVal);
                    resolved.put(entry.getKey(), strVal);
                } else {
                    resolved.put(entry.getKey(), entry.getValue());
                }
            }
            return resolved;
        }
        return body;
    }

    private String resolveString(String str) {
        if (str == null) return null;
        String resolved = str;
        for (Map.Entry<String, String> entry : createdResourceIds.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        for (Map.Entry<String, String> entry : testContext.entrySet()) {
            resolved = resolved.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return resolved;
    }

    /**
     * 从测试结果中提取并存储资源ID
     */
    private void extractAndStoreResourceIds(TestCase tc, TestResult result) {
        String body = result.getFullResponseBody();
        if (body == null || body.isBlank()) return;

        String uuid = TestResult.extractFromJsonPath(body, "$.data.uuid");
        if (uuid != null && !uuid.isBlank()) {
            if (tc.getId().contains("graph_create") || tc.getId().contains("graph_create")) {
                createdResourceIds.put("graphId", uuid);
                cleanupService.registerGraphResource(uuid);
            } else if (tc.getId().contains("node_create")) {
                createdResourceIds.put("nodeUuid", uuid);
                createdResourceIds.put("nodeId", uuid);
                cleanupService.registerNodeResource(uuid);
            } else if (tc.getId().contains("edge_create")) {
                createdResourceIds.put("edgeUuid", uuid);
            } else if (tc.getId().contains("episode_create")) {
                createdResourceIds.put("episodeUuid", uuid);
            }
        }

        String graphId = TestResult.extractFromJsonPath(body, "$.data.graphId");
        if (graphId != null && !graphId.isBlank() && createdResourceIds.get("graphId") == null) {
            createdResourceIds.put("graphId", graphId);
        }
    }

    /**
     * 生成环境描述
     */
    private String detectEnvironment() {
        String baseUrl = config.getServer().getBaseUrl();
        if (baseUrl.contains("localhost")) return "local";
        if (baseUrl.contains("dev")) return "dev";
        if (baseUrl.contains("staging")) return "staging";
        if (baseUrl.contains("prod")) return "production";
        return "unknown";
    }

    private String getPhaseName(TestCase.TestPhase phase) {
        return switch (phase) {
            case SETUP -> "认证与准备";
            case CREATE -> "创建资源";
            case READ -> "查询资源";
            case UPDATE -> "更新资源";
            case DELETE -> "删除资源";
            case CLEANUP -> "清理数据";
            case INDEPENDENT -> "独立测试";
        };
    }

    private TestResult buildCleanupResult(TestDataCleanupService.CleanupReport cleanupReport) {
        return TestResult.builder()
                .caseId("cleanup")
                .caseName("测试数据清理")
                .module("system")
                .phase(TestCase.TestPhase.CLEANUP)
                .status(cleanupReport.failCount == 0 ? TestResult.Status.PASSED
                        : cleanupReport.successCount > 0 ? TestResult.Status.PASSED
                        : TestResult.Status.FAILED)
                .startTime(cleanupReport.startTime)
                .endTime(cleanupReport.endTime)
                .responseTimeMs(cleanupReport.totalDurationMs)
                .build();
    }

    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        String trace = sw.toString();
        // 截断到前20行
        String[] lines = trace.split("\n");
        if (lines.length > 20) {
            trace = String.join("\n", Arrays.copyOfRange(lines, 0, 20)) + "\n... [truncated]";
        }
        return trace;
    }

    /**
     * 关闭执行器
     */
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
