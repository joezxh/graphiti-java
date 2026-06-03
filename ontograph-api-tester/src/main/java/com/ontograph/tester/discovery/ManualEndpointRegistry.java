package com.ontograph.tester.discovery;

import com.ontograph.tester.model.TestCase;
import com.ontograph.tester.model.TestEndpoint;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 手动端点注册表
 * 为核心API模块提供预定义的测试用例
 */
@Slf4j
public class ManualEndpointRegistry {

    private final Map<String, List<TestCase>> registry = new LinkedHashMap<>();
    private final String testDataPrefix;
    private int testCounter = 0;

    public ManualEndpointRegistry(String testDataPrefix) {
        this.testDataPrefix = testDataPrefix;
        registerAll();
    }

    private void registerAll() {
        registerAuthEndpoints();
        registerGraphEndpoints();
        registerNodeEndpoints();
        registerEdgeEndpoints();
        registerEpisodeEndpoints();
        registerSearchEndpoints();
        registerSearchPipelineEndpoints();
        registerDataImportEndpoints();
        registerOntologyEndpoints();
        registerPromptEndpoints();
    }

    private String nextId() {
        return testDataPrefix + (++testCounter);
    }

    private String timestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    // ==================== 认证模块 ====================
    private void registerAuthEndpoints() {
        registry.put("auth", List.of(
                createCase("auth_login", "用户登录", TestCase.TestPhase.SETUP,
                        TestEndpoint.HttpMethod.POST, "/api/v1/auth/login", "auth",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .message("登录应返回200")
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH)
                                    .jsonPath("$.code")
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .message("业务码应为200")
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH_NOT_NULL)
                                    .jsonPath("$.data.token")
                                    .message("Token不应为空")
                                    .build());
                        },
                        Map.of("username", "{{username}}", "password", "{{password}}")
                ),
                createCase("auth_info", "获取用户信息", TestCase.TestPhase.INDEPENDENT,
                        TestEndpoint.HttpMethod.GET, "/api/v1/auth/info", "auth",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH)
                                    .jsonPath("$.code")
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("auth_logout", "退出登录", TestCase.TestPhase.CLEANUP,
                        TestEndpoint.HttpMethod.POST, "/api/v1/auth/logout", "auth",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                )
        ));
    }

    // ==================== 图谱管理模块 ====================
    private void registerGraphEndpoints() {
        registry.put("graph", List.of(
                // CREATE - 创建图谱
                createCase("graph_create", "创建图谱", TestCase.TestPhase.CREATE,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/create", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH)
                                    .jsonPath("$.code")
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH_NOT_NULL)
                                    .jsonPath("$.data.uuid")
                                    .message("图谱UUID不应为空")
                                    .build());
                        },
                        Map.of("name", "{{test_graph_name}}", "description", "{{test_graph_desc}}")
                ),
                // READ - 列表
                createCase("graph_list", "获取图谱列表", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/list", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH)
                                    .jsonPath("$.code")
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                // READ - 详情（动态路径，依赖创建结果）
                createCase("graph_detail", "获取图谱详情", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/{graphId}", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH)
                                    .jsonPath("$.code")
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                // UPDATE - 更新图谱
                createCase("graph_update", "更新图谱", TestCase.TestPhase.UPDATE,
                        TestEndpoint.HttpMethod.PUT, "/api/v1/graph/{graphId}", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH)
                                    .jsonPath("$.code")
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("name", "{{test_graph_name_updated}}", "description", "{{test_graph_desc_updated}}")
                ),
                // READ - 图谱统计
                createCase("graph_stats", "获取图谱统计", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/{graphId}/stats", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                // DELETE - 删除图谱
                createCase("graph_delete", "删除图谱", TestCase.TestPhase.DELETE,
                        TestEndpoint.HttpMethod.DELETE, "/api/v1/graph/{graphId}", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                // READ - 获取节点列表
                createCase("graph_nodes", "获取图谱节点列表", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/{graphId}/nodes", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                // READ - 获取边列表
                createCase("graph_edges", "获取图谱边列表", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/{graphId}/edges", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                // READ - 系统统计
                createCase("graph_system_stats", "获取系统统计", TestCase.TestPhase.INDEPENDENT,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/stats", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                // CREATE - 构建社区
                createCase("graph_communities_build", "构建社区", TestCase.TestPhase.CREATE,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/{graphId}/communities/build", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                // READ - 社区列表
                createCase("graph_communities_list", "获取社区列表", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/{graphId}/communities", "graph",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                )
        ));
    }

    // ==================== 节点管理模块 ====================
    private void registerNodeEndpoints() {
        registry.put("node", List.of(
                createCase("node_create", "创建节点", TestCase.TestPhase.CREATE,
                        TestEndpoint.HttpMethod.POST, "/api/v1/nodes/create", "node",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH_NOT_NULL)
                                    .jsonPath("$.data.uuid")
                                    .message("节点UUID不应为空")
                                    .build());
                        },
                        Map.of("name", "{{test_node_name}}", "type", "{{test_node_type}}", "graphId", "{{graphId}}")
                ),
                createCase("node_list", "获取节点列表", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/nodes/list", "node",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("node_detail", "获取节点详情", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/nodes/{nodeUuid}", "node",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("node_update", "更新节点", TestCase.TestPhase.UPDATE,
                        TestEndpoint.HttpMethod.PUT, "/api/v1/nodes/{nodeUuid}", "node",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("name", "{{test_node_name_updated}}")
                ),
                createCase("node_delete", "删除节点", TestCase.TestPhase.DELETE,
                        TestEndpoint.HttpMethod.DELETE, "/api/v1/nodes/{nodeUuid}", "node",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("node_edges", "获取节点关联边", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/nodes/{nodeUuid}/edges", "node",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("node_episodes", "获取节点关联剧集", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/nodes/{nodeUuid}/episodes", "node",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                )
        ));
    }

    // ==================== 边管理模块 ====================
    private void registerEdgeEndpoints() {
        registry.put("edge", List.of(
                createCase("edge_create", "创建边", TestCase.TestPhase.CREATE,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/edge/{graphId}", "edge",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH_NOT_NULL)
                                    .jsonPath("$.data.uuid")
                                    .build());
                        },
                        Map.of("sourceUuid", "{{sourceNodeUuid}}", "targetUuid", "{{targetNodeUuid}}",
                                "type", "{{test_edge_type}}")
                ),
                createCase("edge_list", "获取边列表", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/edge/list/{graphId}", "edge",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("edge_detail", "获取边详情", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/edge/{graphId}/{edgeUuid}", "edge",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("edge_update", "更新边", TestCase.TestPhase.UPDATE,
                        TestEndpoint.HttpMethod.PUT, "/api/v1/graph/edge/{graphId}/{edgeUuid}", "edge",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("type", "{{test_edge_type_updated}}")
                ),
                createCase("edge_delete", "删除边", TestCase.TestPhase.DELETE,
                        TestEndpoint.HttpMethod.DELETE, "/api/v1/graph/edge/{graphId}/{edgeUuid}", "edge",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("edge_between", "获取两节点间边", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/edge/between/{sourceUuid}/{targetUuid}", "edge",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                )
        ));
    }

    // ==================== 剧集管理模块 ====================
    private void registerEpisodeEndpoints() {
        registry.put("episode", List.of(
                createCase("episode_create", "创建剧集", TestCase.TestPhase.CREATE,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/episode/{graphId}", "episode",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.JSON_PATH_NOT_NULL)
                                    .jsonPath("$.data.uuid")
                                    .build());
                        },
                        Map.of("name", "{{test_episode_name}}", "content", "{{test_episode_content}}")
                ),
                createCase("episode_list", "获取剧集列表", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/episode/list/{graphId}", "episode",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("episode_detail", "获取剧集详情", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/episode/{graphId}/{episodeUuid}", "episode",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("episode_delete", "删除剧集", TestCase.TestPhase.DELETE,
                        TestEndpoint.HttpMethod.DELETE, "/api/v1/graph/episode/{graphId}/{episodeUuid}", "episode",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                )
        ));
    }

    // ==================== 搜索模块 ====================
    private void registerSearchEndpoints() {
        registry.put("search", List.of(
                createCase("search_global", "全局搜索", TestCase.TestPhase.INDEPENDENT,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/search/global", "search",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("query", "{{search_keyword}}", "limit", 10)
                ),
                createCase("search_graph", "图谱内搜索", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/search/graph/{graphId}", "search",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("query", "{{search_keyword}}", "limit", 10)
                ),
                createCase("search_hybrid", "混合搜索", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/search/hybrid/{graphId}", "search",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("query", "{{search_keyword}}")
                ),
                createCase("search_semantic", "语义搜索", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/search/semantic/{graphId}", "search",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("query", "{{search_keyword}}")
                ),
                createCase("search_bfs", "BFS图搜索", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/search/bfs/{graphId}", "search",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("startNodeId", "{{startNodeId}}", "maxDepth", 3)
                ),
                createCase("search_memory", "记忆搜索", TestCase.TestPhase.INDEPENDENT,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/search/memory", "search",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("query", "{{search_keyword}}")
                )
        ));
    }

    // ==================== 搜索管道模块 ====================
    private void registerSearchPipelineEndpoints() {
        registry.put("searchPipeline", List.of(
                createCase("search_pipeline_search", "搜索管道执行", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/search/pipeline/search", "searchPipeline",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("query", "{{search_keyword}}", "graphId", "{{graphId}}")
                ),
                createCase("search_pipeline_rerank", "结果重排", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/search/pipeline/rerank", "searchPipeline",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of()
                )
        ));
    }

    // ==================== 数据导入模块 ====================
    private void registerDataImportEndpoints() {
        registry.put("dataImport", List.of(
                createCase("data_add", "添加单条数据", TestCase.TestPhase.CREATE,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/data/add", "dataImport",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("graphId", "{{graphId}}", "type", "entity", "name", "{{test_entity_name}}")
                ),
                createCase("data_messages", "批量添加消息", TestCase.TestPhase.CREATE,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/data/messages", "dataImport",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("graphId", "{{graphId}}")
                ),
                createCase("data_fact_triple", "添加事实三元组", TestCase.TestPhase.CREATE,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/data/fact-triple", "dataImport",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("graphId", "{{graphId}}", "subject", "{{test_subject}}",
                                "predicate", "{{test_predicate}}", "object", "{{test_object}}")
                ),
                createCase("data_extract_text", "文本抽取", TestCase.TestPhase.CREATE,
                        TestEndpoint.HttpMethod.POST, "/api/v1/graph/extract/text", "dataImport",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Map.of("graphId", "{{graphId}}", "text", "{{test_extract_text}}")
                ),
                createCase("data_extract_entity_types", "获取实体类型列表", TestCase.TestPhase.INDEPENDENT,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/extract/entity-types", "dataImport",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("data_extract_edge_types", "获取边类型列表", TestCase.TestPhase.INDEPENDENT,
                        TestEndpoint.HttpMethod.GET, "/api/v1/graph/extract/edge-types", "dataImport",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                )
        ));
    }

    // ==================== 本体管理模块 ====================
    private void registerOntologyEndpoints() {
        registry.put("ontology", List.of(
                createCase("ontology_definition", "获取本体定义", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/ontology/{graphId}/definition", "ontology",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("ontology_classes", "获取类列表", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/ontology/{graphId}/classes", "ontology",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("ontology_hierarchy", "获取类层次结构", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/ontology/{graphId}/classes/hierarchy", "ontology",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("ontology_properties", "获取属性列表", TestCase.TestPhase.READ,
                        TestEndpoint.HttpMethod.GET, "/api/v1/ontology/{graphId}/properties", "ontology",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("ontology_consistency", "一致性检查", TestCase.TestPhase.INDEPENDENT,
                        TestEndpoint.HttpMethod.GET, "/api/v1/ontology/{graphId}/consistency", "ontology",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                )
        ));
    }

    // ==================== 提示管理模块 ====================
    private void registerPromptEndpoints() {
        registry.put("prompt", List.of(
                createCase("prompt_list", "获取提示模板列表", TestCase.TestPhase.INDEPENDENT,
                        TestEndpoint.HttpMethod.GET, "/api/v1/prompt/templates", "prompt",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                ),
                createCase("prompt_types", "获取提示类型列表", TestCase.TestPhase.INDEPENDENT,
                        TestEndpoint.HttpMethod.GET, "/api/v1/prompt/types", "prompt",
                        tc -> {
                            tc.getAssertions().add(TestCase.Assertion.builder()
                                    .type(TestCase.Assertion.AssertionType.STATUS_CODE)
                                    .expectedValue(200)
                                    .operator(TestCase.Assertion.ComparisonOperator.EQUALS)
                                    .build());
                        },
                        Collections.emptyMap()
                )
        ));
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建测试用例
     */
    private TestCase createCase(String id, String name, TestCase.TestPhase phase,
                                TestEndpoint.HttpMethod method, String path, String module,
                                java.util.function.Consumer<TestCase> assertionConfigurer,
                                Map<String, Object> bodyParams) {
        String fullId = id + "_" + timestamp();

        TestCase tc = TestCase.builder()
                .id(fullId)
                .name(name)
                .module(module)
                .phase(phase)
                .method(method)
                .path(path)
                .queryParams(new java.util.HashMap<>())
                .pathParams(new java.util.HashMap<>())
                .headers(new java.util.HashMap<>())
                .assertions(new ArrayList<>())
                .build();

        // 设置请求体
        if (!bodyParams.isEmpty()) {
            tc.setRequestBody(new java.util.HashMap<>(bodyParams));
        }

        // 配置断言
        assertionConfigurer.accept(tc);

        return tc;
    }

    /**
     * 获取所有手动注册的用例
     */
    public List<TestCase> getAllCases() {
        return registry.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 按模块获取用例
     */
    public List<TestCase> getCasesByModule(String module) {
        return registry.getOrDefault(module, Collections.emptyList());
    }

    /**
     * 获取所有已注册的模块
     */
    public Set<String> getRegisteredModules() {
        return registry.keySet();
    }

    /**
     * 替换用例中的模板变量
     */
    public TestCase resolveTemplateVariables(TestCase tc, Map<String, String> context) {
        TestCase resolved = copyCase(tc);

        // 替换路径中的变量
        String resolvedPath = tc.getPath();
        for (Map.Entry<String, String> entry : context.entrySet()) {
            resolvedPath = resolvedPath.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        resolved.setPath(resolvedPath);

        // 替换请求体中的变量
        if (tc.getRequestBody() instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) tc.getRequestBody();
            Map<String, Object> resolvedBody = new HashMap<>();
            for (Map.Entry<String, Object> entry : body.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    String strValue = (String) value;
                    for (Map.Entry<String, String> ctx : context.entrySet()) {
                        strValue = strValue.replace("{{" + ctx.getKey() + "}}", ctx.getValue());
                    }
                    resolvedBody.put(entry.getKey(), strValue);
                } else {
                    resolvedBody.put(entry.getKey(), value);
                }
            }
            resolved.setRequestBody(resolvedBody);
        }

        return resolved;
    }

    private TestCase copyCase(TestCase tc) {
        return TestCase.builder()
                .id(tc.getId())
                .name(tc.getName())
                .module(tc.getModule())
                .phase(tc.getPhase())
                .endpoint(tc.getEndpoint())
                .prerequisites(new ArrayList<>(tc.getPrerequisites()))
                .method(tc.getMethod())
                .path(tc.getPath())
                .queryParams(new HashMap<>(tc.getQueryParams()))
                .pathParams(new HashMap<>(tc.getPathParams()))
                .headers(new HashMap<>(tc.getHeaders()))
                .requestBody(tc.getRequestBody() instanceof Map ? new HashMap<>((Map<?, ?>) tc.getRequestBody()) : tc.getRequestBody())
                .assertions(tc.getAssertions().stream()
                        .map(a -> TestCase.Assertion.builder()
                                .type(a.getType())
                                .jsonPath(a.getJsonPath())
                                .expectedValue(a.getExpectedValue())
                                .operator(a.getOperator())
                                .message(a.getMessage())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * 为CRUD流程创建上下文
     */
    public Map<String, String> buildTestContext() {
        String ts = timestamp();
        Map<String, String> ctx = new HashMap<>();
        ctx.put("username", System.getenv("TEST_AUTH_USERNAME") != null
                ? System.getenv("TEST_AUTH_USERNAME") : "admin");
        ctx.put("password", System.getenv("TEST_AUTH_PASSWORD") != null
                ? System.getenv("TEST_AUTH_PASSWORD") : "admin123");
        ctx.put("test_graph_name", testDataPrefix + "Graph_" + ts);
        ctx.put("test_graph_desc", testDataPrefix + "Graph Description_" + ts);
        ctx.put("test_graph_name_updated", testDataPrefix + "Graph_Updated_" + ts);
        ctx.put("test_graph_desc_updated", testDataPrefix + "Graph Description Updated_" + ts);
        ctx.put("test_node_name", testDataPrefix + "Node_" + ts);
        ctx.put("test_node_type", "TestEntity");
        ctx.put("test_node_name_updated", testDataPrefix + "Node_Updated_" + ts);
        ctx.put("test_edge_type", "testRelation");
        ctx.put("test_edge_type_updated", "testRelationUpdated");
        ctx.put("test_episode_name", testDataPrefix + "Episode_" + ts);
        ctx.put("test_episode_content", testDataPrefix + "Episode content for testing_" + ts);
        ctx.put("search_keyword", "测试");
        ctx.put("test_entity_name", testDataPrefix + "Entity_" + ts);
        ctx.put("test_subject", testDataPrefix + "Subject_" + ts);
        ctx.put("test_predicate", "relatedTo");
        ctx.put("test_object", testDataPrefix + "Object_" + ts);
        ctx.put("test_extract_text", "这是用于实体抽取测试的文本内容。包含：实体A和实体B之间的关系。");
        return ctx;
    }
}
