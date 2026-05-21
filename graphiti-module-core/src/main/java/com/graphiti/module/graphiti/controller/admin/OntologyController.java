package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.*;
import com.graphiti.module.graphiti.vo.ontology.*;
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
 * 本体管理控制器
 * 提供完整的本体定义 CRUD 操作，包括类、属性、约束、版本历史管理
 */
@Tag(name = "本体管理", description = "知识图谱本体定义的查询和设置")
@RestController
@RequestMapping("/api/v1/ontology")
@RequiredArgsConstructor
public class OntologyController {
    private final OntologyValidationService ontologyValidationService;
    private final OntologyClassService classService;
    private final OntologyPropertyService propertyService;
    private final OntologyReasoner reasoner;
    private final SchemaOrgImportService schemaOrgImportService;
    // ==================== 本体定义管理 ====================

    @Operation(summary = "获取本体定义", description = "获取指定图谱的活跃本体定义",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/definition")
    public CommonResult<OntDefinitionVO> getDefinition(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(classService.getDefinition(graphId));
    }

    @Operation(summary = "创建本体定义", description = "为图谱创建新的本体定义版本",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/definition")
    public CommonResult<OntDefinitionVO> createDefinition(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody OntDefinitionVO reqVO) {
        return CommonResult.success(classService.createDefinition(graphId, reqVO));
    }

    @Operation(summary = "获取本体完整信息", description = "获取图谱本体的完整信息（类、属性、约束）",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}")
    public CommonResult<OntologyFullVO> getOntology(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(classService.getFullOntology(graphId));
    }

    @Operation(summary = "批量本体验证", description = "对请求中的节点与边批量执行本体验证",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/validate/batch")
    public CommonResult<BatchValidationRespVO> validateBatch(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody BatchValidationReqVO reqVO) {
        return CommonResult.success(ontologyValidationService.validateBatch(graphId, reqVO));
    }

    // ==================== 类管理 ====================

    @Operation(summary = "列出所有类", description = "获取图谱下所有本体类定义（平铺）",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/classes")
    public CommonResult<List<OntClassVO>> listClasses(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(classService.listClasses(graphId));
    }

    @Operation(summary = "获取类层次树", description = "以树形结构返回类继承关系",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/classes/hierarchy")
    public CommonResult<List<ClassHierarchyVO>> getClassHierarchy(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(classService.getClassHierarchy(graphId));
    }

    @Operation(summary = "创建类定义", description = "在图谱下创建新的本体类",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/classes")
    public CommonResult<OntClassVO> createClass(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody OntClassVO reqVO) {
        return CommonResult.success(classService.createClass(graphId, reqVO));
    }

    @Operation(summary = "更新类定义", description = "更新指定类的定义信息",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PutMapping("/{graphId}/classes/{classId}")
    public CommonResult<OntClassVO> updateClass(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("classId") @Parameter(description = "类ID", required = true) Long classId,
            @RequestBody OntClassVO reqVO) {
        return CommonResult.success(classService.updateClass(graphId, classId, reqVO));
    }

    @Operation(summary = "删除类定义", description = "删除指定的本体类（若存在子类则拒绝）",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @DeleteMapping("/{graphId}/classes/{classId}")
    public CommonResult<Void> deleteClass(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("classId") @Parameter(description = "类ID", required = true) Long classId) {
        classService.deleteClass(graphId, classId);
        return CommonResult.success(null);
    }

    // ==================== 属性管理 ====================

    @Operation(summary = "列出所有属性", description = "获取图谱下所有本体属性定义",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/properties")
    public CommonResult<List<OntPropertyVO>> listProperties(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(propertyService.listProperties(graphId));
    }

    @Operation(summary = "创建属性定义", description = "在图谱下创建新的本体属性",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/properties")
    public CommonResult<OntPropertyVO> createProperty(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody OntPropertyVO reqVO) {
        return CommonResult.success(propertyService.createProperty(graphId, reqVO));
    }

    @Operation(summary = "更新属性定义", description = "更新指定的本体属性",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PutMapping("/{graphId}/properties/{propertyId}")
    public CommonResult<OntPropertyVO> updateProperty(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("propertyId") @Parameter(description = "属性ID", required = true) Long propertyId,
            @RequestBody OntPropertyVO reqVO) {
        return CommonResult.success(propertyService.updateProperty(graphId, propertyId, reqVO));
    }

    @Operation(summary = "删除属性定义", description = "删除指定的本体属性",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @DeleteMapping("/{graphId}/properties/{propertyId}")
    public CommonResult<Void> deleteProperty(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("propertyId") @Parameter(description = "属性ID", required = true) Long propertyId) {
        propertyService.deleteProperty(graphId, propertyId);
        return CommonResult.success(null);
    }

    // ==================== 约束管理 ====================

    @Operation(summary = "列出所有约束", description = "获取图谱下所有本体约束",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/constraints")
    public CommonResult<List<OntConstraintVO>> listConstraints(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(propertyService.listConstraints(graphId));
    }

    @Operation(summary = "创建约束", description = "创建新的本体约束",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/constraints")
    public CommonResult<OntConstraintVO> createConstraint(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody OntConstraintVO reqVO) {
        return CommonResult.success(propertyService.createConstraint(graphId, reqVO));
    }

    @Operation(summary = "更新约束", description = "更新指定的本体约束",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PutMapping("/{graphId}/constraints/{constraintId}")
    public CommonResult<OntConstraintVO> updateConstraint(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("constraintId") @Parameter(description = "约束ID", required = true) Long constraintId,
            @RequestBody OntConstraintVO reqVO) {
        return CommonResult.success(propertyService.updateConstraint(graphId, constraintId, reqVO));
    }

    @Operation(summary = "删除约束", description = "删除指定的本体约束",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @DeleteMapping("/{graphId}/constraints/{constraintId}")
    public CommonResult<Void> deleteConstraint(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("constraintId") @Parameter(description = "约束ID", required = true) Long constraintId) {
        propertyService.deleteConstraint(graphId, constraintId);
        return CommonResult.success(null);
    }

    // ==================== 版本历史 ====================

    @Operation(summary = "获取版本历史", description = "获取图谱本体的版本变更历史",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/history")
    public CommonResult<List<OntVersionHistoryVO>> getVersionHistory(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(propertyService.getVersionHistory(graphId));
    }

    @Operation(summary = "回滚版本", description = "根据版本历史记录回滚到指定版本",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/history/{historyId}/rollback")
    public CommonResult<Void> rollbackVersion(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @PathVariable("historyId") @Parameter(description = "历史记录ID", required = true) Long historyId) {
        classService.rollbackVersion(graphId, historyId);
        return CommonResult.success(null);
    }

    // ==================== Schema.org 导入导出 ====================

    @Operation(summary = "从 Schema.org 导入本体", description = "从 Schema.org CDN 导入指定领域的类与属性",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/import/schema-org")
    public CommonResult<Map<String, Integer>> importSchemaOrg(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody SchemaOrgImportReqVO reqVO) {
        return CommonResult.success(schemaOrgImportService.importFromSchemaOrg(graphId, reqVO));
    }

    // ==================== 推理引擎 ====================

    @Operation(summary = "推理机状态", description = "查看 OWL 2 RL 推理机是否已预热",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/reasoners/status")
    public CommonResult<Map<String, Object>> getReasonerStatus(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(Map.of(
            "warmedUp", reasoner.isWarmedUp(graphId),
            "graphId", graphId
        ));
    }

    @Operation(summary = "预热推理机", description = "将图谱本体加载到 Jena InfGraph",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/reasoners/warmup")
    public CommonResult<Void> warmUpReasoner(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        reasoner.warmUp(graphId);
        return CommonResult.success(null);
    }

    @Operation(summary = "一致性检查", description = "检查本体是否满足 OWL 2 RL 约束",
              security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}/consistency")
    public CommonResult<ConsistencyResultVO> checkConsistency(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId) {
        return CommonResult.success(reasoner.checkConsistency(graphId));
    }
}
