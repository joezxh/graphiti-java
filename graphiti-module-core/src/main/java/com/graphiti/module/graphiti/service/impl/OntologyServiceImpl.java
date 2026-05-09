package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.OntologyDO;
import com.graphiti.module.graphiti.dal.mysql.OntologyMapper;
import com.graphiti.module.graphiti.service.OntologyService;
import com.graphiti.module.graphiti.vo.ontology.OntologyRespVO;
import com.graphiti.module.graphiti.vo.ontology.SetOntologyReqVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 本体管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyServiceImpl implements OntologyService {
    
    private final OntologyMapper ontologyMapper;
    
    @Override
    public OntologyRespVO getOntology(String graphId) {
        OntologyDO entity = getOntologyByGraphId(graphId);
        return convertToOntologyRespVO(entity);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OntologyRespVO setOntology(String graphId, SetOntologyReqVO reqVO) {
        // 查询是否已存在本体定义
        LambdaQueryWrapper<OntologyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OntologyDO::getGraphId, graphId);
        wrapper.eq(OntologyDO::getDeleted, false);
        OntologyDO entity = ontologyMapper.selectOne(wrapper);
        
        if (entity == null) {
            // 新建
            entity = new OntologyDO();
            entity.setGraphId(graphId);
            entity.setEntities(reqVO.getEntities());
            entity.setEdges(reqVO.getEdges());
            entity.setIsDefault(reqVO.getIsDefault());
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            entity.setDeleted(false);
            ontologyMapper.insert(entity);
        } else {
            // 更新
            entity.setEntities(reqVO.getEntities());
            entity.setEdges(reqVO.getEdges());
            entity.setIsDefault(reqVO.getIsDefault());
            entity.setUpdateTime(LocalDateTime.now());
            ontologyMapper.updateById(entity);
        }
        
        return convertToOntologyRespVO(entity);
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 根据 graphId 查询本体定义
     * @param graphId 图谱ID
     * @return OntologyDO
     * @throws BusinessException 如果本体未定义
     */
    private OntologyDO getOntologyByGraphId(String graphId) {
        LambdaQueryWrapper<OntologyDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OntologyDO::getGraphId, graphId);
        wrapper.eq(OntologyDO::getDeleted, false);
        OntologyDO entity = ontologyMapper.selectOne(wrapper);
        if (entity == null) {
            throw new BusinessException(1002, "本体未定义");
        }
        return entity;
    }
    
    /**
     * 转换为 OntologyRespVO
     * @param entity OntologyDO
     * @return OntologyRespVO
     */
    private OntologyRespVO convertToOntologyRespVO(OntologyDO entity) {
        OntologyRespVO respVO = new OntologyRespVO();
        respVO.setGraphId(entity.getGraphId());
        respVO.setEntities(entity.getEntities());
        respVO.setEdges(entity.getEdges());
        respVO.setIsDefault(entity.getIsDefault());
        respVO.setCreatedAt(entity.getCreateTime());
        respVO.setUpdatedAt(entity.getUpdateTime());
        return respVO;
    }
}
