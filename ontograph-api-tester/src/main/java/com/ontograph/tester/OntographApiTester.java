package com.ontograph.tester;

import com.ontograph.tester.auth.JwtAuthManager;
import com.ontograph.tester.cleanup.TestDataCleanupService;
import com.ontograph.tester.client.ApiHttpClient;
import com.ontograph.tester.config.ConfigLoader;
import com.ontograph.tester.config.TesterConfig;
import com.ontograph.tester.discovery.ManualEndpointRegistry;
import com.ontograph.tester.model.TestReport;
import com.ontograph.tester.report.ConsoleReporter;
import com.ontograph.tester.report.HtmlReporter;
import com.ontograph.tester.report.MarkdownReporter;
import com.ontograph.tester.report.TestReporter;
import com.ontograph.tester.runner.TestRunner;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OntoGraph API Tester - 主入口
 *
 * 独立自动化API测试工具，用于全面验证OntoGraph后端所有RESTful接口。
 *
 * 启动方式:
 *   mvn compile exec:java -Dexec.mainClass="com.ontograph.tester.OntographApiTester"
 *   或打包后:
 *   java -jar ontograph-api-tester-1.0.0-SNAPSHOT.jar
 *
 * 环境变量:
 *   TEST_SERVER_URL       - 目标服务地址（如 http://localhost:8080）
 *   TEST_AUTH_USERNAME   - 登录用户名
 *   TEST_AUTH_PASSWORD   - 登录密码
 *   TEST_HTTP_TIMEOUT_MS - HTTP超时（毫秒）
 *   TEST_CONCURRENCY    - 并发数（0=串行）
 *   TEST_AUTO_CLEANUP   - 是否自动清理（true/false）
 *   TEST_REPORT_DIR     - 报告输出目录
 *   TEST_REPORT_MD      - 生成Markdown报告（true/false）
 *   TEST_REPORT_HTML    - 生成HTML报告（true/false）
 *   TEST_MODULES        - 启用的模块（逗号分隔）
 */
@Slf4j
public class OntographApiTester {

    public static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        printBanner();

        // 1. 加载配置
        TesterConfig config = loadConfig(args);

        // 2. 创建组件
        ApiHttpClient httpClient = new ApiHttpClient(config);
        JwtAuthManager authManager = new JwtAuthManager(config, httpClient);
        String testDataPrefix = config.getTest().getTestDataPrefix();
        ManualEndpointRegistry endpointRegistry = new ManualEndpointRegistry(testDataPrefix);

        // 3. 创建报告器
        List<TestReporter> reporters = createReporters(config);

        // 4. 启动所有报告器
        for (TestReporter r : reporters) {
            r.onTestRunStart();
        }

        // 5. 创建清理服务
        TestDataCleanupService cleanupService = new TestDataCleanupService(
                config, httpClient, ""); // authHeader稍后设置

        // 6. 创建测试运行器
        TestRunner runner = new TestRunner(
                config, httpClient, authManager,
                endpointRegistry, cleanupService,
                new CompositeReporter(reporters)
        );

        // 7. 执行测试
        TestReport report;
        try {
            log.info("开始执行API测试...");
            report = runner.run();
        } finally {
            runner.shutdown();
            httpClient.close();
        }

        // 8. 输出报告路径
        printReportPaths(reporters);

        // 9. 退出码（用于CI/CD集成）
        int exitCode = report.isAllPassed() ? 0 : 1;
        log.info("测试完成，退出码: {}", exitCode);
        System.exit(exitCode);
    }

    private static TesterConfig loadConfig(String[] args) {
        log.info("加载配置文件...");

        ConfigLoader loader = new ConfigLoader();
        TesterConfig config = loader.load(args);

        // 应用环境变量覆盖
        config.applyEnvironmentOverrides();

        // 应用命令行参数覆盖
        Map<String, String> argOverrides = ConfigLoader.parseArgs(args);
        ConfigLoader.applyOverrides(config, argOverrides);

        log.info("目标服务: {}", config.getServer().getBaseUrl());
        log.info("认证用户: {}", config.getAuth().getUsername());
        log.info("自动清理: {}", config.getTest().isAutoCleanup());
        log.info("并发数: {}", config.getTest().getConcurrency());
        log.info("报告目录: {}", config.getReport().getOutputDirectory());

        return config;
    }

    private static List<TestReporter> createReporters(TesterConfig config) {
        List<TestReporter> reporters = new ArrayList<>();

        // 控制台报告器
        reporters.add(new ConsoleReporter(config.getReport(), config.getServer().getBaseUrl()));

        // Markdown报告器
        if (config.getReport().getMarkdown().isEnabled()) {
            reporters.add(new MarkdownReporter(config.getReport()));
        }

        // HTML报告器
        if (config.getReport().getHtml().isEnabled()) {
            reporters.add(new HtmlReporter(config.getReport()));
        }

        return reporters;
    }

    private static void printReportPaths(List<TestReporter> reporters) {
        System.out.println();
        System.out.println("━━━ 报告输出 ━━━");
        for (TestReporter r : reporters) {
            String path = r.getReportPath();
            if (path != null) {
                System.out.println("  " + r.getClass().getSimpleName() + ": " + path);
            }
        }
    }

    private static void printBanner() {
        System.out.println("""

                ╔══════════════════════════════════════════════════════════╗
                ║                                                          ║
                ║         OntoGraph API Tester  v%s                       ║
                ║         独立自动化API测试工具                            ║
                ║                                                          ║
                ╚══════════════════════════════════════════════════════════╝
                """.formatted(VERSION));
    }

    /**
     * 复合报告器：将多个报告器组合，统一触发回调
     */
    private static class CompositeReporter implements TestReporter {

        private final List<TestReporter> delegates;

        CompositeReporter(List<TestReporter> delegates) {
            this.delegates = delegates;
        }

        @Override
        public void onTestRunStart() {
            for (TestReporter r : delegates) {
                r.onTestRunStart();
            }
        }

        @Override
        public void onPhaseStart(com.ontograph.tester.model.TestCase.TestPhase phase, int caseCount) {
            for (TestReporter r : delegates) {
                r.onPhaseStart(phase, caseCount);
            }
        }

        @Override
        public void onTestResult(com.ontograph.tester.model.TestResult result) {
            for (TestReporter r : delegates) {
                r.onTestResult(result);
            }
        }

        @Override
        public void onPhaseComplete(com.ontograph.tester.model.TestCase.TestPhase phase) {
            for (TestReporter r : delegates) {
                r.onPhaseComplete(phase);
            }
        }

        @Override
        public void onTestRunComplete(TestReport report) {
            for (TestReporter r : delegates) {
                r.onTestRunComplete(report);
            }
        }

        @Override
        public String getReportPath() {
            return delegates.isEmpty() ? null : delegates.get(0).getReportPath();
        }
    }
}
