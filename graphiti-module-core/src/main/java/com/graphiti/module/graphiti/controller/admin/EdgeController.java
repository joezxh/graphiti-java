package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.EdgeService;
import com.graphiti.module.graphiti.vo.edge.EdgeFilterReqVO;
import com.graphiti.module.graphiti.vo.edge.EdgeInfoRespVO;
import com.graphiti.module.graphiti.vo.edge.EdgeListRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 边管理控制器
 */
@Tag(name = "边管理", description = "知识图谱关系边的管理接口")
@RestController
@RequestMapping("/admin/graphiti/edge")
@Validated
@Slf4j
public class EdgeController {

    @Resource
    private EdgeService edgeService;

    @PostMapping("/list/{graphId}")
    @Operation(summary = "获取边列表", description = "获取指定图谱的边列表，支持过滤和分页", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<List<EdgeListRespVO>> listEdges(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody(required = false) @Valid EdgeFilterReqVO filterReqVO) {
        List<EdgeListRespVO> list = edgeService.listEdges(graphId,
                filterReqVO != null ? filterReqVO : new EdgeFilterReqVO());
        return CommonResult.success(list);
    }

    @GetMapping("/{graphId}/{edgeUuid}")
    @Operation(summary = "获取边详情", description = "根据边UUID获取边的详细信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<EdgeInfoRespVO> getEdgeDetail(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("edgeUuid") @Parameter(description = "边UUID", required = true) String edgeUuid) {
        return CommonResult.success(edgeService.getEdgeDetail(graphId, edgeUuid));
    }

    @PostMapping("/{graphId}")
    @Operation(summary = "创建边", description = "在图谱中创建新的关系边", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<EdgeInfoRespVO> createEdge(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody @Valid Map<String, Object> edgeData) {
        return CommonResult.success(edgeService.createEdge(graphId, edgeData));
    }

    @PutMapping("/{graphId}/{edgeUuid}")
    @Operation(summary = "更新边", description = "更新指定边的属性信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<EdgeInfoRespVO> updateEdge(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("edgeUuid") @Parameter(description = "边UUID", required = true) String edgeUuid,
            @RequestBody @Valid Map<String, Object> edgeData) {
        return CommonResult.success(edgeService.updateEdge(graphId, edgeUuid, edgeData));
    }

    @DeleteMapping("/{graphId}/{edgeUuid}")
    @Operation(summary = "删除边", description = "删除指定的关系边", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> deleteEdge(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("edgeUuid") @Parameter(description = "边UUID", required = true) String edgeUuid) {
        edgeService.deleteEdge(graphId, edgeUuid);
        return CommonResult.success(true);
    }
}
