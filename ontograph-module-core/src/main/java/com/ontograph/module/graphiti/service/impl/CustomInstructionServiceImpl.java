package com.ontograph.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ontograph.module.graphiti.dal.dataobject.CustomInstructionDO;
import com.ontograph.module.graphiti.dal.mysql.CustomInstructionMapper;
import com.ontograph.module.graphiti.service.CustomInstructionService;
import com.ontograph.module.graphiti.vo.custom_instruction.CreateCustomInstructionReqVO;
import com.ontograph.module.graphiti.vo.custom_instruction.CustomInstructionRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义抽取指令服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomInstructionServiceImpl implements CustomInstructionService {

    private final CustomInstructionMapper customInstructionMapper;

    @Override
    public List<CustomInstructionRespVO> getInstructions(String graphId) {
        LambdaQueryWrapper<CustomInstructionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CustomInstructionDO::getGraphId, graphId)
               .or()
               .isNull(CustomInstructionDO::getGraphId);
        wrapper.eq(CustomInstructionDO::getEnabled, true);
        wrapper.orderByDesc(CustomInstructionDO::getCreatedAt);
        List<CustomInstructionDO> list = customInstructionMapper.selectList(wrapper);
        return list.stream().map(this::convertToResp).collect(Collectors.toList());
    }

    @Override
    public CustomInstructionRespVO createInstruction(CreateCustomInstructionReqVO reqVO) {
        CustomInstructionDO entity = new CustomInstructionDO();
        entity.setInstruction(reqVO.getInstruction());
        entity.setGraphId(reqVO.getGraphId());
        entity.setEnabled(true);
        customInstructionMapper.insert(entity);
        log.info("创建自定义抽取指令：graphId={}, id={}", reqVO.getGraphId(), entity.getId());
        return convertToResp(entity);
    }

    @Override
    public void deleteInstruction(Long id) {
        customInstructionMapper.deleteById(id);
        log.info("删除自定义抽取指令：id={}", id);
    }

    private CustomInstructionRespVO convertToResp(CustomInstructionDO entity) {
        CustomInstructionRespVO vo = new CustomInstructionRespVO();
        vo.setId(entity.getId());
        vo.setGraphId(entity.getGraphId());
        vo.setInstruction(entity.getInstruction());
        vo.setEnabled(entity.getEnabled());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
