package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.OntologyService;
import com.graphiti.module.graphiti.service.OntologyValidationService;
import com.graphiti.module.graphiti.vo.ontology.BatchValidationReqVO;
import com.graphiti.module.graphiti.vo.ontology.BatchValidationRespVO;
import com.graphiti.module.graphiti.vo.ontology.OntologyRespVO;
import com.graphiti.module.graphiti.vo.ontology.SetOntologyReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 本体管理控制器
 * 提供本体定义的获取和设置接口
 */
@Tag(name = "本体管理", description = "知识图谱本体定义的查询和设置")
@RestController
@RequestMapping("/api/v1/ontology")
@RequiredArgsConstructor
public class OntologyController {
    private final OntologyService ontologyService;
    private final OntologyValidationService ontologyValidationService;
    /**
     * 获取指定图谱的本体定义
     * @param graphId 图谱ID
     * @return CommonResult<OntologyRespVO>
     */
    @Operation(summary = "获取本体定义", description = "获取指定图谱的本体定义（实体类型和关系类型）", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @GetMapping("/{graphId}")
    public CommonResult<OntologyRespVO> getOntology(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true, example = "graph-123") String graphId) {
        return CommonResult.success(ontologyService.getOntology(graphId));
    }
    /**
     * 设置指定图谱的本体定义
     * @param graphId 图谱ID
     * @param reqVO SetOntologyReqVO
     * @return CommonResult<OntologyRespVO>
     */
    @Operation(summary = "设置本体定义", description = "设置或更新指定图谱的本体定义", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}")
    public CommonResult<OntologyRespVO> setOntology(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true, example = "graph-123") String graphId,
            @Valid @RequestBody SetOntologyReqVO reqVO) {
        return CommonResult.success(ontologyService.setOntology(graphId, reqVO));
    }

    @Operation(summary = "批量本体验证", description = "对请求中的节点与边批量执行本体验证",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    @PostMapping("/{graphId}/validate/batch")
    public CommonResult<BatchValidationRespVO> validateBatch(
            @PathVariable("graphId") @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody BatchValidationReqVO reqVO) {
        return CommonResult.success(ontologyValidationService.validateBatch(graphId, reqVO));
    }
}
