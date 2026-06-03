package com.ontograph.tester.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.tester.client.ApiHttpClient;
import com.ontograph.tester.config.TesterConfig;
import com.ontograph.tester.model.HttpResponse;
import com.ontograph.tester.model.TestEndpoint;
import com.ontograph.tester.model.TestResult;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OpenAPI规范自动发现服务
 * 从/v3/api-docs端点获取API规范并转换为测试端点列表
 */
@Slf4j
public class OpenApiDiscoveryService {

    private final TesterConfig config;
    private final ApiHttpClient httpClient;
    private final ObjectMapper mapper;

    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final Set<String> SECURED_PATHS = Set.of(
            "/api/v1/graph", "/api/v1/nodes", "/api/v1/ontology",
            "/api/v1/prompt", "/api/v1/admin"
    );
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth", "/v3/api-docs", "/swagger-ui",
            "/error", "/actuator"
    );

    public OpenApiDiscoveryService(TesterConfig config, ApiHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
        this.mapper = new ObjectMapper();
    }

    /**
     * 从OpenAPI规范发现所有端点
     */
    public List<TestEndpoint> discoverEndpoints() {
        String apiDocsUrl = config.getServer().getBaseUrl() + config.getServer().getApiDocsPath();
        log.info("从 {} 发现API端点...", apiDocsUrl);

        List<TestEndpoint> endpoints = new ArrayList<>();
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json");

            HttpResponse response = httpClient.get(apiDocsUrl, headers);
            int statusCode = response.getStatusCode();
            if (statusCode != 200) {
                log.warn("无法获取OpenAPI规范，状态码: {}", statusCode);
                return endpoints;
            }

            String json = response.getBody();
            if (json == null || json.isEmpty()) {
                log.warn("OpenAPI规范返回空响应");
                return endpoints;
            }
            JsonNode root = mapper.readTree(json);

            String title = "";
            if (root.has("info") && root.get("info").has("title")) {
                title = root.get("info").get("title").asText();
            }
            log.info("API标题: {}", title);

            // 解析paths
            if (root.has("paths")) {
                JsonNode paths = root.get("paths");
                endpoints.addAll(parsePaths(paths, title));
            }

            // 解析webhooks（如果有）
            if (root.has("webhooks")) {
                JsonNode webhooks = root.get("webhooks");
                // 暂不支持webhook测试
            }
        } catch (Exception e) {
            log.error("发现端点失败: {}", e.getMessage());
        }

        log.info("共发现 {} 个端点", endpoints.size());
        return endpoints;
    }

    private List<TestEndpoint> parsePaths(JsonNode paths, String apiTitle) {
        List<TestEndpoint> endpoints = new ArrayList<>();

        paths.fieldNames().forEachRemaining(pathStr -> {
            JsonNode pathNode = paths.get(pathStr);
            if (!pathNode.isObject()) return;

            // 提取模块名（从路径）
            String module = extractModuleName(pathStr);

            // 解析每个HTTP方法
            Iterator<Map.Entry<String, JsonNode>> methods = pathNode.fields();
            while (methods.hasNext()) {
                Map.Entry<String, JsonNode> entry = methods.next();
                String methodStr = entry.getKey().toUpperCase();
                if (!isValidHttpMethod(methodStr)) continue;

                JsonNode operation = entry.getValue();
                if (!operation.isObject()) continue;

                try {
                    TestEndpoint endpoint = parseOperation(pathStr, methodStr, operation, module);
                    if (endpoint != null) {
                        endpoints.add(endpoint);
                    }
                } catch (Exception e) {
                    log.warn("解析端点 {} {} 失败: {}", methodStr, pathStr, e.getMessage());
                }
            }
        });

        return endpoints;
    }

    private TestEndpoint parseOperation(String path, String method, JsonNode operation, String module) {
        // 跳过 OPTIONS, HEAD 等不常用的方法
        if (method.equals("OPTIONS") || method.equals("HEAD")) {
            return null;
        }

        String endpointId = buildEndpointId(module, method, path);
        String description = "";
        String tag = module;

        if (operation.has("summary")) {
            description = operation.get("summary").asText();
        } else if (operation.has("description")) {
            description = operation.get("description").asText();
            if (description.length() > 200) {
                description = description.substring(0, 200);
            }
        }

        if (operation.has("tags") && operation.get("tags").isArray() && !operation.get("tags").isEmpty()) {
            tag = operation.get("tags").get(0).asText();
        }

        // 判断认证级别
        TestEndpoint.AuthLevel authLevel = determineAuthLevel(path, operation);

        // 解析参数
        List<TestEndpoint.ParamDefinition> pathParams = new ArrayList<>();
        List<TestEndpoint.ParamDefinition> queryParams = new ArrayList<>();
        if (operation.has("parameters")) {
            JsonNode params = operation.get("parameters");
            for (JsonNode param : params) {
                TestEndpoint.ParamDefinition pd = parseParameter(param);
                if (pd != null) {
                    if (isPathParam(param)) {
                        pathParams.add(pd);
                    } else {
                        queryParams.add(pd);
                    }
                }
            }
        }

        // 解析requestBody schema
        String requestBodySchema = "";
        if (operation.has("requestBody")) {
            requestBodySchema = extractRequestBodySchema(operation.get("requestBody"));
        }

        // 判断是否启用
        boolean enabled = isModuleEnabled(module);

        return TestEndpoint.builder()
                .id(endpointId)
                .module(module)
                .tag(tag)
                .method(TestEndpoint.HttpMethod.valueOf(method))
                .path(path)
                .description(description)
                .authLevel(authLevel)
                .pathParams(pathParams)
                .queryParams(queryParams)
                .requestBodySchema(requestBodySchema)
                .enabled(enabled)
                .autoDiscovered(true)
                .build();
    }

    private TestEndpoint.ParamDefinition parseParameter(JsonNode param) {
        String name = param.has("name") ? param.get("name").asText() : null;
        if (name == null) return null;

        String type = param.has("schema") && param.get("schema").has("type")
                ? param.get("schema").get("type").asText() : "string";
        boolean required = param.has("required") && param.asBoolean();
        String description = param.has("description") ? param.get("description").asText() : "";
        Object example = null;
        Object defaultValue = null;

        if (param.has("schema")) {
            JsonNode schema = param.get("schema");
            if (schema.has("example")) {
                example = schema.get("example").asText();
            }
            if (schema.has("default")) {
                defaultValue = schema.get("default").asText();
            }
        }

        return TestEndpoint.ParamDefinition.builder()
                .name(name)
                .type(type)
                .required(required)
                .description(description)
                .example(example != null ? String.valueOf(example) : null)
                .defaultValue(defaultValue != null ? String.valueOf(defaultValue) : null)
                .build();
    }

    private boolean isPathParam(JsonNode param) {
        return param.has("in") && "path".equals(param.get("in").asText());
    }

    private String extractRequestBodySchema(JsonNode requestBody) {
        if (!requestBody.has("content")) return "";
        JsonNode content = requestBody.get("content");
        Iterator<String> fields = content.fieldNames();
        while (fields.hasNext()) {
            String mediaType = fields.next();
            if (mediaType.contains("json")) {
                JsonNode jsonContent = content.get(mediaType);
                if (jsonContent.has("schema")) {
                    return jsonContent.get("schema").toString();
                }
            }
        }
        return "";
    }

    private TestEndpoint.AuthLevel determineAuthLevel(String path, JsonNode operation) {
        // 公开路径
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath)) {
                return TestEndpoint.AuthLevel.NONE;
            }
        }

        // 检查是否有security标记
        if (operation.has("security")) {
            JsonNode security = operation.get("security");
            if (security.isArray() && !security.isEmpty()) {
                return TestEndpoint.AuthLevel.REQUIRED;
            }
        }

        // 安全路径默认需要认证
        for (String securedPath : SECURED_PATHS) {
            if (path.startsWith(securedPath)) {
                return TestEndpoint.AuthLevel.REQUIRED;
            }
        }

        return TestEndpoint.AuthLevel.REQUIRED;
    }

    private boolean isModuleEnabled(String module) {
        TesterConfig.ModulesConfig m = config.getModules();
        String lower = module.toLowerCase();
        return switch (lower) {
            case "认证管理", "auth" -> m.isAuth();
            case "图谱管理", "graph" -> m.isGraph();
            case "节点管理", "node" -> m.isNode();
            case "边管理", "edge" -> m.isEdge();
            case "剧集管理", "episode" -> m.isEpisode();
            case "搜索管理", "search" -> m.isSearch();
            case "搜索管道", "searchpipeline", "search-pipeline" -> m.isSearchPipeline();
            case "数据导入", "datatypeimport", "data-import" -> m.isDataImport();
            case "本体管理", "ontology" -> m.isOntology();
            case "提示管理", "prompt" -> m.isPrompt();
            case "法律图谱", "legal" -> m.isLegal();
            case "业务信息", "businessinfo", "business-info" -> m.isBusinessInfo();
            case "系统管理", "system" -> m.isSystem();
            default -> true;
        };
    }

    private String extractModuleName(String path) {
        if (path.startsWith("/api/v1/auth")) return "auth";
        if (path.startsWith("/api/v1/graph") || path.startsWith("/api/v1/graphiti")) return "graph";
        if (path.startsWith("/api/v1/nodes")) return "node";
        if (path.startsWith("/api/v1/graph/edge")) return "edge";
        if (path.startsWith("/api/v1/graph/episode")) return "episode";
        if (path.startsWith("/api/v1/graph/search")) return "search";
        if (path.startsWith("/api/v1/graph/data")) return "dataImport";
        if (path.startsWith("/api/v1/ontology")) return "ontology";
        if (path.startsWith("/api/v1/prompt")) return "prompt";
        if (path.startsWith("/api/v1/graph/legal")) return "legal";
        if (path.startsWith("/api/v1/business-info")) return "businessInfo";
        if (path.startsWith("/api/v1/admin")) return "system";
        return "other";
    }

    private boolean isValidHttpMethod(String method) {
        return method.matches("GET|POST|PUT|DELETE|PATCH");
    }

    private String buildEndpointId(String module, String method, String path) {
        return String.format("%s:%s:%s", module, method, path);
    }

    /**
     * 从发现结果中筛选特定模块的端点
     */
    public List<TestEndpoint> filterByModule(List<TestEndpoint> endpoints, String module) {
        return endpoints.stream()
                .filter(e -> e.getModule().equalsIgnoreCase(module))
                .toList();
    }

    /**
     * 获取已启用端点的统计
     */
    public Map<String, Long> getEndpointStats(List<TestEndpoint> endpoints) {
        return endpoints.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        TestEndpoint::getModule,
                        java.util.stream.Collectors.counting()
                ));
    }
}
