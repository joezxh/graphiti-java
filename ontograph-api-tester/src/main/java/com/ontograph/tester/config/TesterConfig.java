package com.ontograph.tester.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API测试工具全局配置（从api-tester.yml加载）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TesterConfig {

    private ServerConfig server = new ServerConfig();
    private AuthConfig auth = new AuthConfig();
    private HttpConfig http = new HttpConfig();
    private TestConfig test = new TestConfig();
    private ReportConfig report = new ReportConfig();
    private ModulesConfig modules = new ModulesConfig();
    private ScenariosConfig scenarios = new ScenariosConfig();

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ServerConfig {
        private String baseUrl = "http://localhost:8080";
        private String apiDocsPath = "/v3/api-docs";
        private String swaggerUiPath = "/swagger-ui.html";
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthConfig {
        private String loginPath = "/api/v1/auth/login";
        private String username = "admin";
        private String password = "admin123";
        private String bearerPrefix = "Bearer ";
        private int refreshThresholdSeconds = 300;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HttpConfig {
        private int connectTimeoutMs = 30000;
        private int readTimeoutMs = 60000;
        private int requestTimeoutMs = 120000;
        private int maxTotalConnections = 50;
        private int maxPerRouteConnections = 10;
        private int retryCount = 3;
        private int retryIntervalMs = 1000;
        private List<String> retryableMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "DELETE"));
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TestConfig {
        private boolean autoDiscoverEnabled = true;
        private boolean manualEndpointsEnabled = true;
        private int concurrency = 0;
        private int startupDelaySeconds = 5;
        private boolean autoCleanup = true;
        private boolean cleanupFailContinue = true;
        private String testDataPrefix = "api_test_";
        private int maxResponseBodyLength = 4096;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReportConfig {
        private ConsoleConfig console = new ConsoleConfig();
        private MarkdownConfig markdown = new MarkdownConfig();
        private HtmlConfig html = new HtmlConfig();
        private String outputDirectory = "./test-reports";

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ConsoleConfig {
            private boolean colored = true;
            private boolean showStackTrace = true;
            private boolean showRequestResponse = false;
            private int progressRefreshMs = 500;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class MarkdownConfig {
            private boolean enabled = true;
            private String outputPath = "api-test-report.md";
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class HtmlConfig {
            private boolean enabled = true;
            private String outputPath = "api-test-report.html";
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModulesConfig {
        private boolean auth = true;
        private boolean graph = true;
        private boolean node = true;
        private boolean edge = true;
        private boolean episode = true;
        private boolean search = true;
        private boolean searchPipeline = true;
        private boolean dataImport = true;
        private boolean ontology = true;
        private boolean prompt = true;
        private boolean legal = true;
        private boolean businessInfo = true;
        private boolean system = false;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScenariosConfig {
        private CrudFlowConfig crudFlow = new CrudFlowConfig();
        private AuthFlowConfig authFlow = new AuthFlowConfig();
        private SearchFlowConfig searchFlow = new SearchFlowConfig();
        private ImportFlowConfig importFlow = new ImportFlowConfig();

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CrudFlowConfig {
            private boolean enabled = true;
            private int dataCount = 2;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class AuthFlowConfig {
            private boolean enabled = true;
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class SearchFlowConfig {
            private boolean enabled = true;
            private List<String> keywords = new ArrayList<>(List.of("测试", "知识图谱", "entity"));
        }

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class ImportFlowConfig {
            private boolean enabled = false;
        }
    }

    /**
     * 从环境变量覆盖配置
     */
    public void applyEnvironmentOverrides() {
        String baseUrl = System.getenv("TEST_SERVER_URL");
        if (baseUrl != null && !baseUrl.isBlank()) {
            server.setBaseUrl(baseUrl);
        }

        String username = System.getenv("TEST_AUTH_USERNAME");
        if (username != null && !username.isBlank()) {
            auth.setUsername(username);
        }

        String password = System.getenv("TEST_AUTH_PASSWORD");
        if (password != null && !password.isBlank()) {
            auth.setPassword(password);
        }

        String timeout = System.getenv("TEST_HTTP_TIMEOUT_MS");
        if (timeout != null && !timeout.isBlank()) {
            try {
                int t = Integer.parseInt(timeout);
                http.setConnectTimeoutMs(t);
                http.setReadTimeoutMs(t);
            } catch (NumberFormatException ignored) {}
        }

        String concurrency = System.getenv("TEST_CONCURRENCY");
        if (concurrency != null && !concurrency.isBlank()) {
            try {
                test.setConcurrency(Integer.parseInt(concurrency));
            } catch (NumberFormatException ignored) {}
        }

        String autoCleanup = System.getenv("TEST_AUTO_CLEANUP");
        if (autoCleanup != null && !autoCleanup.isBlank()) {
            test.setAutoCleanup(Boolean.parseBoolean(autoCleanup));
        }

        String outputDir = System.getenv("TEST_REPORT_DIR");
        if (outputDir != null && !outputDir.isBlank()) {
            report.setOutputDirectory(outputDir);
        }

        String enableMd = System.getenv("TEST_REPORT_MD");
        if (enableMd != null) {
            report.getMarkdown().setEnabled(Boolean.parseBoolean(enableMd));
        }

        String enableHtml = System.getenv("TEST_REPORT_HTML");
        if (enableHtml != null) {
            report.getHtml().setEnabled(Boolean.parseBoolean(enableHtml));
        }

        String enabledModules = System.getenv("TEST_MODULES");
        if (enabledModules != null && !enabledModules.isBlank()) {
            applyModulesFilter(enabledModules);
        }
    }

    private void applyModulesFilter(String enabledModules) {
        modules.setAuth(false);
        modules.setGraph(false);
        modules.setNode(false);
        modules.setEdge(false);
        modules.setEpisode(false);
        modules.setSearch(false);
        modules.setSearchPipeline(false);
        modules.setDataImport(false);
        modules.setOntology(false);
        modules.setPrompt(false);
        modules.setLegal(false);
        modules.setBusinessInfo(false);
        modules.setSystem(false);

        for (String m : enabledModules.split(",")) {
            String trimmed = m.trim().toLowerCase();
            switch (trimmed) {
                case "auth" -> modules.setAuth(true);
                case "graph" -> modules.setGraph(true);
                case "node" -> modules.setNode(true);
                case "edge" -> modules.setEdge(true);
                case "episode" -> modules.setEpisode(true);
                case "search" -> modules.setSearch(true);
                case "search-pipeline", "searchpipeline" -> modules.setSearchPipeline(true);
                case "data-import", "datatypeimport" -> modules.setDataImport(true);
                case "ontology" -> modules.setOntology(true);
                case "prompt" -> modules.setPrompt(true);
                case "legal" -> modules.setLegal(true);
                case "business-info", "businessinfo" -> modules.setBusinessInfo(true);
                case "system" -> modules.setSystem(true);
            }
        }
    }
}
