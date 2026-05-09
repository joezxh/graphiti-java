package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.EdgeService;
import com.graphiti.module.graphiti.service.GraphitiService;
import com.graphiti.module.graphiti.service.NodeService;
import com.graphiti.module.graphiti.vo.edge.EdgeFilterReqVO;
import com.graphiti.module.graphiti.vo.edge.EdgeListRespVO;
import com.graphiti.module.graphiti.vo.graph.CreateGraphReqVO;
import com.graphiti.module.graphiti.vo.graph.GraphInfoRespVO;
import com.graphiti.module.graphiti.vo.graph.GraphListRespVO;
import com.graphiti.module.graphiti.vo.graph.GraphStatsRespVO;
import com.graphiti.module.graphiti.vo.graph.UpdateGraphReqVO;
import com.graphiti.module.graphiti.vo.node.NodeFilterReqVO;
import com.graphiti.module.graphiti.vo.node.NodeListRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    /**
     * 创建图谱
     * @param reqVO CreateGraphReqVO
     * @return CommonResult<GraphInfoRespVO>
     */
    @Operation(summary = "创建图谱", description = "创建新的知识图谱", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/create")
    public CommonResult<GraphInfoRespVO> create(
            @Valid @RequestBody CreateGraphReqVO reqVO) {
        return CommonResult.success(graphitiService.createGraph(reqVO));
    }
    /**
     * 获取图谱列表
     * @return CommonResult<List<GraphListRespVO>>
     */
    @Operation(summary = "获取图谱列表", description = "获取所有知识图谱的列表", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/list")
    public CommonResult<List<GraphListRespVO>> list() {
        return CommonResult.success(graphitiService.listGraphs());
    }
    /**
     * 获取图谱详情
     * @param graphId 图谱ID
     * @return CommonResult<GraphInfoRespVO>
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
     * @param graphId 图谱ID
     * @param reqVO UpdateGraphReqVO
     * @return CommonResult<GraphInfoRespVO>
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
     * @param graphId 图谱ID
     * @return CommonResult<Void>
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
     * @param graphId 图谱ID
     * @return CommonResult<Void>
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
     * @return CommonResult<GraphStatsRespVO>
     */
    @Operation(summary = "获取图谱统计", description = "获取系统的图谱统计信息，包括图谱总数、节点数、边数等", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/stats")
    public CommonResult<GraphStatsRespVO> getStats() {
        return CommonResult.success(graphitiService.getGraphStats());
    }
    
    /**
     * 获取图谱的节点列表
     * @param graphId 图谱ID
     * @return CommonResult<List<NodeListRespVO>>
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
     * @param graphId 图谱ID
     * @return CommonResult<List<EdgeListRespVO>>
     */
    @Operation(summary = "获取边列表", description = "获取指定图谱的边列表", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/edges")
    public CommonResult<List<EdgeListRespVO>> getEdges(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        EdgeFilterReqVO filterReqVO = new EdgeFilterReqVO();
        return CommonResult.success(edgeService.listEdges(graphId, filterReqVO));
    }
}
