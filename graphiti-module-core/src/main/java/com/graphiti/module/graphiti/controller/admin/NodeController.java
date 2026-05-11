package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.NodeService;
import com.graphiti.module.graphiti.vo.node.NodeFilterReqVO;
import com.graphiti.module.graphiti.vo.node.NodeInfoRespVO;
import com.graphiti.module.graphiti.vo.node.NodeListRespVO;
import com.graphiti.module.graphiti.vo.edge.EdgeListRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/**
 * 节点管理控制器
 * 提供节点的 CRUD 接口
 */
@Tag(name = "节点管理", description = "知识图谱节点的创建、查询、更新、删除等操作")
@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class NodeController {
    private final NodeService nodeService;
    /**
     * 获取节点列表（支持过滤和分页）
     * @param graphId 图谱ID（请求参数）
     * @param filterReqVO 过滤条件
     * @return CommonResult<List<NodeListRespVO>>
     */
    @Operation(summary = "获取节点列表", description = "获取指定图谱的节点列表，支持过滤条件", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/list")
    public CommonResult<List<NodeListRespVO>> list(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @Valid NodeFilterReqVO filterReqVO) {
        return CommonResult.success(nodeService.listNodes(graphId, filterReqVO));
    }
    /**
     * 获取节点详情
     * @param graphId 图谱ID
     * @param nodeUuid 节点UUID
     * @return CommonResult<NodeInfoRespVO>
     */
    @Operation(summary = "获取节点详情", description = "根据节点UUID获取节点详细信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{nodeUuid}")
    public CommonResult<NodeInfoRespVO> getDetail(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("nodeUuid") @Parameter(description = "节点UUID", required = true, example = "node-123") String nodeUuid) {
        return CommonResult.success(nodeService.getNodeDetail(graphId, nodeUuid));
    }
    /**
     * 创建节点
     * @param graphId 图谱ID
     * @param nodeData 节点数据（包含 name, type, properties）
     * @return CommonResult<NodeInfoRespVO>
     */
    @Operation(summary = "创建节点", description = "在指定图谱中创建新的实体节点", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/create")
    public CommonResult<NodeInfoRespVO> create(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody Map<String, Object> nodeData) {
        return CommonResult.success(nodeService.createNode(graphId, nodeData));
    }
    /**
     * 更新节点
     * @param graphId 图谱ID
     * @param nodeUuid 节点UUID
     * @param nodeData 更新的数据
     * @return CommonResult<NodeInfoRespVO>
     */
    @Operation(summary = "更新节点", description = "更新指定节点的属性信息", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PutMapping("/{nodeUuid}")
    public CommonResult<NodeInfoRespVO> update(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("nodeUuid") @Parameter(description = "节点UUID", required = true, example = "node-123") String nodeUuid,
            @RequestBody Map<String, Object> nodeData) {
        return CommonResult.success(nodeService.updateNode(graphId, nodeUuid, nodeData));
    }
    /**
     * 删除节点
     * @param graphId 图谱ID
     * @param nodeUuid 节点UUID
     * @return CommonResult<Void>
     */
    @Operation(summary = "删除节点", description = "删除指定的节点及其关联的边",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @DeleteMapping("/{nodeUuid}")
    public CommonResult<Void> delete(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("nodeUuid") @Parameter(description = "节点UUID", required = true, example = "node-123") String nodeUuid) {
        nodeService.deleteNode(graphId, nodeUuid);
        return CommonResult.success();
    }

    /**
     * 获取节点关联的边
     * @param graphId 图谱ID
     * @param nodeUuid 节点UUID
     * @param skip 跳过数量
     * @param limit 限制数量
     * @return CommonResult<List<EdgeListRespVO>>
     */
    @Operation(summary = "获取节点关联边", description = "获取指定节点关联的所有边（双向）",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{nodeUuid}/edges")
    public CommonResult<List<EdgeListRespVO>> getNodeEdges(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("nodeUuid") @Parameter(description = "节点UUID", required = true) String nodeUuid,
            @RequestParam(defaultValue = "0") @Parameter(description = "跳过数量") Long skip,
            @RequestParam(defaultValue = "20") @Parameter(description = "限制数量") Long limit) {
        return CommonResult.success(nodeService.getNodeEdges(graphId, nodeUuid, skip, limit));
    }

    /**
     * 获取节点关联的 Episode 列表
     * @param graphId 图谱ID
     * @param nodeUuid 节点UUID
     * @param skip 跳过数量
     * @param limit 限制数量
     * @return CommonResult<List<Map>>
     */
    @Operation(summary = "获取节点关联 Episode", description = "获取指定节点关联的 Episode 列表",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{nodeUuid}/episodes")
    public CommonResult<List<Map<String, Object>>> getNodeEpisodes(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("nodeUuid") @Parameter(description = "节点UUID", required = true) String nodeUuid,
            @RequestParam(defaultValue = "0") @Parameter(description = "跳过数量") Long skip,
            @RequestParam(defaultValue = "20") @Parameter(description = "限制数量") Long limit) {
        return CommonResult.success(nodeService.getNodeEpisodes(graphId, nodeUuid, skip, limit));
    }
}
