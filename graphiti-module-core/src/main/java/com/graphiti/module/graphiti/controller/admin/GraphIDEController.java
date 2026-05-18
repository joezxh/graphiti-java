package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.CascadeEditService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.GraphVisualizationService;
import com.graphiti.module.graphiti.service.SchemaManagementService;
import com.graphiti.module.graphiti.vo.ide.*;
import com.graphiti.module.graphiti.vo.node.NodeInfoRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Graph IDE 控制器
 * 提供图谱可视化、Schema编辑、级联编辑等接口
 */
@Tag(name = "Graph IDE", description = "图谱可视化与Schema编辑接口")
@RestController
@RequestMapping("/api/v1/graph")
@RequiredArgsConstructor
@Slf4j
public class GraphIDEController {

    private final GraphVisualizationService graphVisualizationService;
    private final SchemaManagementService schemaManagementService;
    private final CascadeEditService cascadeEditService;
    private final GraphNeo4jService graphNeo4jService;

    // ==================== 可视化接口 ====================

    @Operation(summary = "获取图谱可视化数据", description = "获取图谱的节点和边数据，用于前端可视化渲染")
    @GetMapping("/{graphId}/visualization")
    public CommonResult<GraphVisualizationRespVO> getVisualization(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam(required = false) String layout,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "500") Integer pageSize,
            @RequestParam(required = false) String classType,
            @RequestParam(required = false) String keyword) {
        return CommonResult.success(
                graphVisualizationService.getVisualizationData(graphId, layout, page, pageSize, classType, keyword)
        );
    }

    @Operation(summary = "获取图谱元数据", description = "获取图谱的统计信息")
    @GetMapping("/{graphId}/metadata")
    public CommonResult<Map<String, Object>> getMetadata(
            @PathVariable @Parameter(description = "图谱ID") String graphId) {
        return CommonResult.success(graphVisualizationService.getGraphMetadata(graphId));
    }

    @Operation(summary = "获取节点详情", description = "获取单个节点的详细信息和关联关系")
    @GetMapping("/{graphId}/nodes/{nodeUuid}")
    public CommonResult<NodeInfoRespVO> getNodeDetail(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "节点UUID") String nodeUuid) {
        return CommonResult.success(graphVisualizationService.getNodeDetail(graphId, nodeUuid));
    }

    @Operation(summary = "创建节点", description = "在图谱中创建新的实体节点")
    @PostMapping("/{graphId}/nodes")
    public CommonResult<NodeInfoRespVO> createNode(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody Map<String, Object> nodeData) {
        return CommonResult.success(graphVisualizationService.createNode(graphId, nodeData));
    }

    @Operation(summary = "更新节点", description = "更新指定节点的属性信息")
    @PutMapping("/{graphId}/nodes/{nodeUuid}")
    public CommonResult<NodeInfoRespVO> updateNode(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "节点UUID") String nodeUuid,
            @RequestBody Map<String, Object> nodeData) {
        return CommonResult.success(graphVisualizationService.updateNode(graphId, nodeUuid, nodeData));
    }

    @Operation(summary = "删除节点", description = "删除指定的实体节点及其关联的边")
    @DeleteMapping("/{graphId}/nodes/{nodeUuid}")
    public CommonResult<Void> deleteNode(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "节点UUID") String nodeUuid) {
        graphVisualizationService.deleteNode(graphId, nodeUuid);
        return CommonResult.success(null);
    }

    @Operation(summary = "创建边", description = "在两个节点之间创建关联关系")
    @PostMapping("/{graphId}/edges")
    public CommonResult<GraphVisualizationRespVO.EdgeVO> createEdge(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody Map<String, Object> edgeData) {
        return CommonResult.success(graphVisualizationService.createEdge(graphId, edgeData));
    }

    @Operation(summary = "展开邻居节点", description = "获取指定节点的直接邻居节点")
    @PostMapping("/{graphId}/nodes/{nodeUuid}/expand")
    public CommonResult<GraphVisualizationRespVO> expandNeighbors(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "节点UUID") String nodeUuid,
            @RequestBody(required = false) Map<String, Object> options) {
        return CommonResult.success(graphVisualizationService.expandNeighbors(graphId, nodeUuid, options));
    }

    // ==================== Schema 接口 ====================

    @Operation(summary = "获取类列表", description = "获取图谱本体定义中的所有类")
    @GetMapping("/{graphId}/ontology/classes")
    public CommonResult<List<SchemaClassRespVO>> getSchemaClasses(
            @PathVariable @Parameter(description = "图谱ID") String graphId) {
        return CommonResult.success(schemaManagementService.getClasses(graphId));
    }

    @Operation(summary = "获取类详情", description = "获取指定类的详细信息")
    @GetMapping("/{graphId}/ontology/classes/{classId}")
    public CommonResult<SchemaClassRespVO> getSchemaClassDetail(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类ID") Long classId) {
        return CommonResult.success(schemaManagementService.getClassDetail(graphId, classId));
    }

    @Operation(summary = "创建类", description = "创建新的本体类定义")
    @PostMapping("/{graphId}/ontology/classes")
    public CommonResult<SchemaClassRespVO> createSchemaClass(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody Map<String, Object> classData) {
        return CommonResult.success(schemaManagementService.createClass(graphId, classData));
    }

    @Operation(summary = "更新类", description = "更新本体类的定义")
    @PutMapping("/{graphId}/ontology/classes/{classId}")
    public CommonResult<SchemaClassRespVO> updateSchemaClass(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类ID") Long classId,
            @RequestBody Map<String, Object> classData) {
        return CommonResult.success(schemaManagementService.updateClass(graphId, classId, classData));
    }

    @Operation(summary = "删除类", description = "删除本体类定义")
    @DeleteMapping("/{graphId}/ontology/classes/{classId}")
    public CommonResult<Void> deleteSchemaClass(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类ID") Long classId) {
        schemaManagementService.deleteClass(graphId, classId);
        return CommonResult.success(null);
    }

    @Operation(summary = "获取类属性列表", description = "获取指定类的所有属性定义")
    @GetMapping("/{graphId}/ontology/classes/{classId}/properties")
    public CommonResult<List<SchemaPropertyRespVO>> getClassProperties(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类ID") Long classId) {
        return CommonResult.success(schemaManagementService.getClassProperties(graphId, classId));
    }

    @Operation(summary = "创建属性", description = "为指定类创建新的属性定义")
    @PostMapping("/{graphId}/ontology/classes/{classId}/properties")
    public CommonResult<SchemaPropertyRespVO> createClassProperty(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类ID") Long classId,
            @RequestBody Map<String, Object> propertyData) {
        return CommonResult.success(schemaManagementService.createProperty(graphId, classId, propertyData));
    }

    @Operation(summary = "更新属性", description = "更新属性定义")
    @PutMapping("/{graphId}/ontology/classes/{classId}/properties/{propertyId}")
    public CommonResult<SchemaPropertyRespVO> updateClassProperty(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类ID") Long classId,
            @PathVariable @Parameter(description = "属性ID") Long propertyId,
            @RequestBody Map<String, Object> propertyData) {
        return CommonResult.success(schemaManagementService.updateProperty(graphId, classId, propertyId, propertyData));
    }

    @Operation(summary = "删除属性", description = "删除属性定义")
    @DeleteMapping("/{graphId}/ontology/classes/{classId}/properties/{propertyId}")
    public CommonResult<Void> deleteClassProperty(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类ID") Long classId,
            @PathVariable @Parameter(description = "属性ID") Long propertyId) {
        schemaManagementService.deleteProperty(graphId, classId, propertyId);
        return CommonResult.success(null);
    }

    @Operation(summary = "验证 Schema 变更", description = "在执行 Schema 变更前验证其对现有数据的影响")
    @PostMapping("/{graphId}/ontology/validate-change")
    public CommonResult<SchemaChangeValidateRespVO> validateSchemaChange(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid SchemaChangeValidateReqVO request) {
        return CommonResult.success(schemaManagementService.validateSchemaChange(graphId, request));
    }

    // ==================== 类实例接口 ====================

    @Operation(summary = "获取类的实例数据列表", description = "从图数据库中获取指定类的所有实例节点，支持分页和搜索")
    @GetMapping("/{graphId}/instances")
    public CommonResult<Map<String, Object>> getClassInstances(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "类类型名称") String classType,
            @RequestParam(required = false, defaultValue = "1") @Parameter(description = "页码") Integer page,
            @RequestParam(required = false, defaultValue = "20") @Parameter(description = "每页数量") Integer pageSize,
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String keyword) {
        return CommonResult.success(
                schemaManagementService.getClassInstances(graphId, classType, page, pageSize, keyword)
        );
    }

    // ==================== 级联编辑接口 ====================

    @Operation(summary = "预览级联编辑影响范围", description = "计算筛选条件匹配的节点数量和分布")
    @PostMapping("/{graphId}/cascade/preview")
    public CommonResult<CascadePreviewRespVO> previewCascade(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid CascadeFilterReqVO filter) {
        return CommonResult.success(cascadeEditService.preview(graphId, filter));
    }

    @Operation(summary = "执行级联编辑", description = "批量更新符合条件的节点属性")
    @PostMapping("/{graphId}/cascade/execute")
    public CommonResult<CascadeExecuteRespVO> executeCascade(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid CascadeExecuteReqVO executeReq) {
        return CommonResult.success(cascadeEditService.execute(graphId, executeReq));
    }
}
