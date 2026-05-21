package com.graphiti.module.graphiti.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.common.response.CommonResult;
import com.graphiti.framework.security.util.UserContext;
import com.graphiti.module.graphiti.service.CommunityService;
import com.graphiti.module.graphiti.service.EdgeService;
import com.graphiti.module.graphiti.service.GraphitiService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.NodeService;
import com.graphiti.module.graphiti.service.SearchService;
import com.graphiti.module.graphiti.service.TemporalService;
import com.graphiti.module.graphiti.vo.edge.EdgeFilterReqVO;
import com.graphiti.module.graphiti.vo.edge.EdgeListRespVO;
import com.graphiti.module.graphiti.vo.graph.CreateGraphReqVO;
import com.graphiti.module.graphiti.vo.graph.GraphInfoRespVO;
import com.graphiti.module.graphiti.vo.graph.GraphListRespVO;
import com.graphiti.module.graphiti.vo.graph.GraphStatsRespVO;
import com.graphiti.module.graphiti.vo.graph.UpdateGraphReqVO;
import com.graphiti.module.graphiti.vo.node.NodeFilterReqVO;
import com.graphiti.module.graphiti.vo.node.NodeListRespVO;
import com.graphiti.module.graphiti.vo.search.SearchQueryReqVO;
import com.graphiti.module.graphiti.vo.search.SearchResultsRespVO;
import com.graphiti.system.dal.dataobject.OperationLogDO;
import com.graphiti.system.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 图谱管理控制器
 * 提供图谱的 CRUD 接口
 */
@Tag(name = "图谱管理", description = "知识图谱的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/v1/graph")
@RequiredArgsConstructor
@Slf4j
public class GraphitiController {
    private final GraphitiService graphitiService;
    private final NodeService nodeService;
    private final EdgeService edgeService;
    private final CommunityService communityService;
    private final TemporalService temporalService;
    private final SearchService searchService;
    private final GraphNeo4jService graphNeo4jService;
    private final OperationLogService operationLogService;
    private final UserContext userContext;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 记录图谱操作日志
     */
    private void saveGraphOpLog(String operation, String method, String graphId,
                                 Object params, int status, String errorMsg, long startTime) {
        try {
            OperationLogDO logDO = new OperationLogDO();
            logDO.setUsername(userContext.getCurrentUsername());
            logDO.setOperation(operation);
            logDO.setMethod(method);

            Map<String, Object> paramMap = new HashMap<>();
            if (graphId != null) {
                paramMap.put("graphId", graphId);
            }
            if (params != null) {
                paramMap.put("detail", params);
            }
            logDO.setParams(objectMapper.writeValueAsString(paramMap));
            logDO.setStatus(status);
            logDO.setErrorMsg(errorMsg);
            logDO.setDuration((int) (System.currentTimeMillis() - startTime));
            logDO.setCreateTime(LocalDateTime.now());
            operationLogService.saveLog(logDO);
        } catch (Exception e) {
            log.error("记录图谱操作日志失败: operation={}, graphId={}", operation, graphId, e);
        }
    }

    /**
     * 创建图谱
     */
    @Operation(summary = "创建图谱", description = "创建新的知识图谱",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/create")
    public CommonResult<GraphInfoRespVO> create(
            @Valid @RequestBody CreateGraphReqVO reqVO) {
        long start = System.currentTimeMillis();
        try {
            GraphInfoRespVO result = graphitiService.createGraph(reqVO);
            saveGraphOpLog("创建图谱", "POST /graph/create", result.getGraphId(),
                           Map.of("graphName", reqVO.getName()), 1, null, start);
            return CommonResult.success(result);
        } catch (Exception e) {
            saveGraphOpLog("创建图谱", "POST /graph/create", null,
                           Map.of("graphName", reqVO.getName()), 0, e.getMessage(), start);
            throw e;
        }
    }

    /**
     * 获取图谱列表（分页）
     */
    @Operation(summary = "获取图谱列表", description = "获取所有知识图谱的列表（含分页信息）",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/list")
    public CommonResult<GraphListRespVO> list(
            @RequestParam(value = "limit", required = false) @Parameter(description = "限制数量", example = "100") Long limit,
            @RequestParam(value = "offset", required = false) @Parameter(description = "偏移量", example = "0") Long offset) {
        return CommonResult.success(graphitiService.listGraphs(limit, offset));
    }

    /**
     * 获取图谱详情
     */
    @Operation(summary = "获取图谱详情", description = "根据图谱ID获取图谱详细信息",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}")
    public CommonResult<GraphInfoRespVO> getDetail(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true, example = "abc123") String graphId) {
        return CommonResult.success(graphitiService.getGraph(graphId));
    }

    /**
     * 更新图谱信息
     */
    @Operation(summary = "更新图谱", description = "更新图谱的基本信息",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PutMapping("/{graphId}")
    public CommonResult<GraphInfoRespVO> update(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true, example = "abc123") String graphId,
            @Valid @RequestBody UpdateGraphReqVO reqVO) {
        long start = System.currentTimeMillis();
        try {
            GraphInfoRespVO result = graphitiService.updateGraph(graphId, reqVO);
            saveGraphOpLog("更新图谱", "PUT /graph/{graphId}", graphId,
                           Map.of("graphName", reqVO.getName()), 1, null, start);
            return CommonResult.success(result);
        } catch (Exception e) {
            saveGraphOpLog("更新图谱", "PUT /graph/{graphId}", graphId,
                           Map.of("graphName", reqVO.getName()), 0, e.getMessage(), start);
            throw e;
        }
    }

    /**
     * 删除图谱
     */
    @Operation(summary = "删除图谱", description = "逻辑删除指定的图谱",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @DeleteMapping("/{graphId}")
    public CommonResult<Void> delete(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true, example = "abc123") String graphId) {
        long start = System.currentTimeMillis();
        try {
            graphitiService.deleteGraph(graphId);
            saveGraphOpLog("删除图谱", "DELETE /graph/{graphId}", graphId, null, 1, null, start);
            return CommonResult.success();
        } catch (Exception e) {
            saveGraphOpLog("删除图谱", "DELETE /graph/{graphId}", graphId, null, 0, e.getMessage(), start);
            throw e;
        }
    }

    /**
     * 清空图谱数据
     */
    @Operation(summary = "清空图谱数据", description = "清空图谱中的所有数据，但保留图谱元数据",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/clear")
    public CommonResult<Void> clear(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true, example = "abc123") String graphId) {
        long start = System.currentTimeMillis();
        try {
            graphitiService.clearGraph(graphId);
            saveGraphOpLog("清空图谱数据", "POST /graph/{graphId}/clear", graphId, null, 1, null, start);
            return CommonResult.success();
        } catch (Exception e) {
            saveGraphOpLog("清空图谱数据", "POST /graph/{graphId}/clear", graphId, null, 0, e.getMessage(), start);
            throw e;
        }
    }

    /**
     * 获取图谱统计信息
     */
    @Operation(summary = "获取图谱统计", description = "获取系统的图谱统计信息，包括图谱总数、节点数、边数等",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/stats")
    public CommonResult<GraphStatsRespVO> getStats() {
        return CommonResult.success(graphitiService.getGraphStats());
    }

    /**
     * 获取指定图谱的统计信息
     */
    @Operation(summary = "获取图谱统计", description = "获取指定图谱的节点数、边数统计",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/stats")
    public CommonResult<Map<String, Long>> getGraphStats(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(graphNeo4jService.getGraphStats(graphId));
    }

    /**
     * 获取图谱的节点列表
     */
    @Operation(summary = "获取节点列表", description = "获取指定图谱的节点列表",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/nodes")
    public CommonResult<List<NodeListRespVO>> getNodes(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        NodeFilterReqVO filterReqVO = new NodeFilterReqVO();
        return CommonResult.success(nodeService.listNodes(graphId, filterReqVO));
    }

    /**
     * 获取图谱的边列表
     */
    @Operation(summary = "获取边列表", description = "获取指定图谱的边列表",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/edges")
    public CommonResult<List<EdgeListRespVO>> getEdges(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        EdgeFilterReqVO filterReqVO = new EdgeFilterReqVO();
        return CommonResult.success(edgeService.listEdges(graphId, filterReqVO));
    }

    // ==================== 社区管理 ====================

    @Operation(summary = "构建社区", description = "对指定图谱执行社区发现算法",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/communities/build")
    public CommonResult<Map<String, Object>> buildCommunities(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> result = communityService.buildCommunities(graphId);
            saveGraphOpLog("构建社区", "POST /graph/{graphId}/communities/build", graphId, null, 1, null, start);
            return CommonResult.success(result);
        } catch (Exception e) {
            saveGraphOpLog("构建社区", "POST /graph/{graphId}/communities/build", graphId, null, 0, e.getMessage(), start);
            throw e;
        }
    }

    @Operation(summary = "获取社区列表", description = "获取指定图谱的社区列表",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/communities")
    public CommonResult<List<Map<String, Object>>> listCommunities(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(communityService.listCommunities(graphId));
    }

    @Operation(summary = "搜索社区", description = "按名称或摘要搜索社区",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/communities/search")
    public CommonResult<List<Map<String, Object>>> searchCommunities(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam("query") @Parameter(description = "搜索关键词") String query) {
        return CommonResult.success(communityService.searchCommunities(graphId, query));
    }

    @Operation(summary = "删除社区", description = "根据 UUID 删除指定的社区节点",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @DeleteMapping("/{graphId}/communities/{communityUuid}")
    public CommonResult<Boolean> deleteCommunity(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("communityUuid") @Parameter(description = "社区UUID", required = true) String communityUuid) {
        communityService.deleteCommunity(graphId, communityUuid);
        return CommonResult.success(true);
    }

    @Operation(summary = "获取社区列表", description = "分页获取图谱中的社区节点列表",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/communities/list")
    public CommonResult<Map<String, Object>> listCommunities(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam(value = "domain", required = false) @Parameter(description = "法律领域过滤") String domain,
            @RequestParam(value = "type", required = false) @Parameter(description = "社区类型过滤") String type,
            @RequestParam(value = "keyword", required = false) @Parameter(description = "名称关键词") String keyword,
            @RequestParam(value = "skip", defaultValue = "0") @Parameter(description = "跳过数量") int skip,
            @RequestParam(value = "limit", defaultValue = "20") @Parameter(description = "返回数量") int limit) {
        List<Map<String, Object>> all = communityService.listCommunities(graphId);
        // 过滤
        Stream<Map<String, Object>> stream = all.stream();
        if (domain != null && !domain.isEmpty()) {
            stream = stream.filter(c -> domain.equals(c.get("domainType")) || domain.equals(c.get("legalDomain")));
        }
        if (type != null && !type.isEmpty()) {
            stream = stream.filter(c -> type.equals(c.get("communityType")));
        }
        if (keyword != null && !keyword.isEmpty()) {
            String kw = keyword.toLowerCase();
            stream = stream.filter(c -> {
                String name = String.valueOf(c.getOrDefault("name", ""));
                String summary = String.valueOf(c.getOrDefault("summary", ""));
                return name.toLowerCase().contains(kw) || summary.toLowerCase().contains(kw);
            });
        }
        List<Map<String, Object>> filtered = stream.toList();
        int total = filtered.size();
        int end = Math.min(skip + limit, total);
        List<Map<String, Object>> page = skip < total ? filtered.subList(skip, end) : List.of();
        return CommonResult.success(Map.of("communities", page, "totalCount", total));
    }

    @Operation(summary = "创建社区", description = "在指定图谱中创建新的社区节点",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/communities")
    public CommonResult<Map<String, Object>> createCommunity(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody @Parameter(description = "社区信息") Map<String, Object> body) {
        Map<String, Object> community = communityService.createCommunity(graphId, body);
        return CommonResult.success(community);
    }

    @Operation(summary = "更新社区", description = "更新指定社区节点的信息",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PutMapping("/{graphId}/communities/{communityUuid}")
    public CommonResult<Map<String, Object>> updateCommunity(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("communityUuid") @Parameter(description = "社区UUID", required = true) String communityUuid,
            @RequestBody @Parameter(description = "更新信息") Map<String, Object> body) {
        Map<String, Object> community = communityService.updateCommunity(graphId, communityUuid, body);
        return CommonResult.success(community);
    }

    // ==================== 克隆与导出 ====================

    @Operation(summary = "克隆图谱", description = "克隆指定图谱",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/clone")
    public CommonResult<GraphInfoRespVO> cloneGraph(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        long start = System.currentTimeMillis();
        try {
            GraphInfoRespVO result = graphitiService.cloneGraph(graphId);
            saveGraphOpLog("克隆图谱", "POST /graph/{graphId}/clone", graphId,
                           Map.of("newGraphId", result.getGraphId()), 1, null, start);
            return CommonResult.success(result);
        } catch (Exception e) {
            saveGraphOpLog("克隆图谱", "POST /graph/{graphId}/clone", graphId, null, 0, e.getMessage(), start);
            throw e;
        }
    }

    @Operation(summary = "导出图谱", description = "导出指定图谱数据",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/export")
    public CommonResult<Map<String, Object>> exportGraph(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> result = graphitiService.exportGraph(graphId);
            int nodeCount = result.containsKey("nodes") ? ((List<?>) result.get("nodes")).size() : 0;
            int edgeCount = result.containsKey("edges") ? ((List<?>) result.get("edges")).size() : 0;
            saveGraphOpLog("导出图谱", "GET /graph/{graphId}/export", graphId,
                           Map.of("nodeCount", nodeCount, "edgeCount", edgeCount), 1, null, start);
            return CommonResult.success(result);
        } catch (Exception e) {
            saveGraphOpLog("导出图谱", "GET /graph/{graphId}/export", graphId, null, 0, e.getMessage(), start);
            throw e;
        }
    }

    // ==================== 图谱搜索 ====================

    @Operation(summary = "图谱搜索", description = "在指定图谱中进行搜索（POST body 形式）",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/search")
    public CommonResult<SearchResultsRespVO> searchGraph(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @Valid @RequestBody SearchQueryReqVO reqVO) {
        return CommonResult.success(searchService.searchGraph(graphId, reqVO));
    }

    // ==================== 历史状态查询 ====================

    @Operation(summary = "历史状态查询", description = "查询指定时间点的图谱状态",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/history")
    public CommonResult<Map<String, Object>> getHistory(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestParam("time") @Parameter(description = "查询时间戳（毫秒）") long time) {
        Map<String, Object> result = new HashMap<>();
        result.put("nodes", temporalService.getFactsAtTime(graphId, time));
        result.put("edges", temporalService.getRelationshipsAtTime(graphId, time));
        return CommonResult.success(result);
    }
}
