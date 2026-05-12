package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.service.CustomInstructionService;
import com.graphiti.module.graphiti.vo.custom_instruction.CreateCustomInstructionReqVO;
import com.graphiti.module.graphiti.vo.custom_instruction.CustomInstructionRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 自定义抽取指令控制器
 */
@Tag(name = "自定义抽取指令", description = "管理 LLM 实体/关系抽取时的自定义指令")
@RestController
@RequestMapping("/api/v1/custom-instructions")
@RequiredArgsConstructor
public class CustomInstructionController {

    private final CustomInstructionService customInstructionService;

    @GetMapping
    @Operation(summary = "获取自定义指令", description = "获取指定图谱的自定义抽取指令（含全局指令）",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<List<CustomInstructionRespVO>> getInstructions(
            @RequestParam(name = "graphId", required = false) String graphId) {
        // 处理空字符串情况
        String effectiveGraphId = (graphId != null && graphId.isBlank()) ? null : graphId;
        return CommonResult.success(customInstructionService.getInstructions(effectiveGraphId));
    }

    @PostMapping
    @Operation(summary = "创建自定义指令", description = "创建新的自定义抽取指令",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<CustomInstructionRespVO> create(
            @Valid @RequestBody CreateCustomInstructionReqVO reqVO) {
        return CommonResult.success(customInstructionService.createInstruction(reqVO));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除自定义指令", description = "删除指定的自定义抽取指令",
               security = {@SecurityRequirement(name = "Bearer Authentication")})
    public CommonResult<Void> delete(
            @PathVariable("id") @Parameter(description = "指令ID", required = true) Long id) {
        customInstructionService.deleteInstruction(id);
        return CommonResult.success();
    }
}
