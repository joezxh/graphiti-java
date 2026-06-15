package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.custom_instruction.CreateCustomInstructionReqVO;
import com.ontograph.module.graphiti.vo.custom_instruction.CustomInstructionRespVO;
import java.util.List;

/**
 * 自定义抽取指令服务接口
 */
public interface CustomInstructionService {

    /**
     * 获取图谱的自定义指令（含全局指令）
     * @param graphId 图谱ID
     * @return 指令列表
     */
    List<CustomInstructionRespVO> getInstructions(String graphId);

    /**
     * 创建自定义抽取指令
     * @param reqVO 创建请求
     * @return 创建的指令
     */
    CustomInstructionRespVO createInstruction(CreateCustomInstructionReqVO reqVO);

    /**
     * 删除自定义抽取指令
     * @param id 指令ID
     */
    void deleteInstruction(Long id);
}
