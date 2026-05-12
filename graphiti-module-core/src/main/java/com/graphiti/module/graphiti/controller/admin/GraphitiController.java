package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图谱管理控制器
 * 提供图谱的 CRUD 接口
 */
@Tag(name = "图谱管理", description = "知识图谱的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/v1/graph")
@RequiredArgsConstructor
public class GraphitiController {
    private final GraphitiService graphitiService;
    private final NodeService nodeService;
    private final EdgeService edgeService;
    private final CommunityService communityService;
    private final TemporalService temporalService;
    private final SearchService searchService;
    private final GraphNeo4jService graphNeo4jService;

    /**
     * 创建图谱
     */
    @Operation(summary = "创建图谱", description = "创建新的知识图谱",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/create")
    public CommonResult<GraphInfoRespVO> create(
            @Valid @RequestBody CreateGraphReqVO reqVO) {
        return CommonResult.success(graphitiService.createGraph(reqVO));
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
        return CommonResult.success(graphitiService.updateGraph(graphId, reqVO));
    }

    /**
     * 删除图谱
     */
    @Operation(summary = "删除图谱", description = "逻辑删除指定的图谱",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @DeleteMapping("/{graphId}")
    public CommonResult<Void> delete(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true, example = "abc123") String graphId) {
        graphitiService.deleteGraph(graphId);
        return CommonResult.success();
    }

    /**
     * 清空图谱数据
     */
    @Operation(summary = "清空图谱数据", description = "清空图谱中的所有数据，但保留图谱元数据",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/clear")
    public CommonResult<Void> clear(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true, example = "abc123") String graphId) {
        graphitiService.clearGraph(graphId);
        return CommonResult.success();
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
        return CommonResult.success(communityService.buildCommunities(graphId));
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

    // ==================== 克隆与导出 ====================

    @Operation(summary = "克隆图谱", description = "克隆指定图谱",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/clone")
    public CommonResult<GraphInfoRespVO> cloneGraph(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(graphitiService.cloneGraph(graphId));
    }

    @Operation(summary = "导出图谱", description = "导出指定图谱数据",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/export")
    public CommonResult<Map<String, Object>> exportGraph(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(graphitiService.exportGraph(graphId));
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
