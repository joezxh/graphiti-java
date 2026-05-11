package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.DataImportService;
import com.graphiti.module.graphiti.vo.imports.AddDataBatchReqVO;
import com.graphiti.module.graphiti.vo.imports.AddDataReqVO;
import com.graphiti.module.graphiti.vo.imports.AddMessagesReqVO;
import com.graphiti.module.graphiti.vo.imports.FactTripleReqVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 数据导入控制器
 */
@Tag(name = "数据导入", description = "知识图谱数据导入相关接口")
@RestController
@RequestMapping("/api/v1/graph/data")
@Validated
@Slf4j
public class DataImportController {

    @Resource
    private DataImportService dataImportService;

    @PostMapping("/add")
    @Operation(summary = "添加单条数据", description = "添加单条数据并自动提取实体和关系", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> addData(@Valid @RequestBody AddDataReqVO reqVO) {
        dataImportService.addData(reqVO);
        return CommonResult.success(true);
    }

    @PostMapping("/batch")
    @Operation(summary = "批量添加数据", description = "批量导入数据到图谱", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> addDataBatch(@Valid @RequestBody AddDataBatchReqVO reqVO) {
        dataImportService.addDataBatch(reqVO);
        return CommonResult.success(true);
    }

    @PostMapping("/messages")
    @Operation(summary = "添加消息", description = "添加对话历史消息到图谱", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> addMessages(@Valid @RequestBody AddMessagesReqVO reqVO) {
        dataImportService.addMessages(reqVO);
        return CommonResult.success(true);
    }

    @PostMapping("/fact-triple")
    @Operation(summary = "添加事实三元组", description = "直接添加事实三元组到图谱", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> addFactTriple(@Valid @RequestBody FactTripleReqVO reqVO) {
        dataImportService.addFactTriple(reqVO);
        return CommonResult.success(true);
    }

    @PostMapping("/entity-node")
    @Operation(summary = "添加实体节点", description = "直接写入实体节点，不经过LLM提取", 
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Boolean> addEntityNode(
            @RequestParam @Parameter(description = "图谱ID", required = true) String graphId,
            @RequestBody @Valid java.util.Map<String, Object> nodeData) {
        dataImportService.addEntityNode(graphId, nodeData);
        return CommonResult.success(true);
    }
}
