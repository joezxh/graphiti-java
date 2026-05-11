package com.graphiti.module.graphiti.namespace;

import com.graphiti.module.graphiti.service.CustomInstructionService;
import com.graphiti.module.graphiti.vo.custom_instruction.CreateCustomInstructionReqVO;
import com.graphiti.module.graphiti.vo.custom_instruction.CustomInstructionRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

/**
 * 自定义抽取指令命名空间
 * 对应 Python: graphiti.custom_instructions
 *
 * <p>管理 LLM 实体/关系抽取时的自定义指令，
 * 支持按图谱隔离和全局共享两种模式。
 */
@Slf4j
@RequiredArgsConstructor
public class CustomInstructionNamespace {

    private final CustomInstructionService customInstructionService;

    /**
     * 获取自定义指令（含全局指令）
     */
    public List<CustomInstructionRespVO> get(String graphId) {
        log.debug("CustomInstructionNamespace.get: graphId={}", graphId);
        return customInstructionService.getInstructions(graphId);
    }

    /**
     * 创建自定义指令
     */
    public CustomInstructionRespVO create(CreateCustomInstructionReqVO reqVO) {
        log.info("CustomInstructionNamespace.create: graphId={}", reqVO.getGraphId());
        return customInstructionService.createInstruction(reqVO);
    }

    /**
     * 删除自定义指令
     */
    public void delete(Long id) {
        log.info("CustomInstructionNamespace.delete: id={}", id);
        customInstructionService.deleteInstruction(id);
    }
}
