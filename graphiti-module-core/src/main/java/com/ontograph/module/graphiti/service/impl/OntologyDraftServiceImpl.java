package com.ontograph.module.graphiti.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ontograph.common.exception.BusinessException;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntDraftDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import com.ontograph.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntDraftMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntPropertyMapper;
import com.ontograph.module.graphiti.service.OntologyClassService;
import com.ontograph.module.graphiti.service.OntologyDraftService;
import com.ontograph.module.graphiti.service.OntologyPropertyService;
import com.ontograph.module.graphiti.vo.business.GenerateOntologyRespVO;
import com.ontograph.module.graphiti.vo.business.OntDraftVO;
import com.ontograph.module.graphiti.vo.ontology.OntClassVO;
import com.ontograph.module.graphiti.vo.ontology.OntDefinitionVO;
import com.ontograph.module.graphiti.vo.ontology.OntPropertyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyDraftServiceImpl implements OntologyDraftService {

    private final OntDraftMapper draftMapper;
    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntologyClassService ontologyClassService;
    private final OntologyPropertyService ontologyPropertyService;
    private final ObjectMapper objectMapper;

    @Override
    public List<OntDraftVO> listDrafts(String graphId) {
        List<OntDraftDO> drafts = draftMapper.selectByGraphId(graphId);
        return drafts.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public OntDraftVO getDraft(Long draftId) {
        OntDraftDO draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException(2000, "草稿不存在");
        }
        return toVO(draft);
    }

    @Override
    public GenerateOntologyRespVO getDraftContent(Long draftId) {
        OntDraftDO draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException(2000, "草稿不存在");
        }
        if (draft.getGeneratedInfo() == null) {
            throw new BusinessException(2005, "草稿中无生成内容");
        }
        try {
            return objectMapper.readValue(draft.getGeneratedInfo(), GenerateOntologyRespVO.class);
        } catch (Exception e) {
            log.error("解析草稿内容失败: draftId={}", draftId, e);
            throw new BusinessException(2002, "解析草稿内容失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void applyDraft(String graphId, Long draftId) {
        OntDraftDO draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException(2000, "草稿不存在");
        }
        if ("APPLIED".equals(draft.getStatus())) {
            throw new BusinessException(2001, "草稿已应用，不能重复应用");
        }

        try {
            GenerateOntologyRespVO content = objectMapper.readValue(
                draft.getGeneratedInfo(), GenerateOntologyRespVO.class);

            // 1. 创建或获取本体定义
            GenerateOntologyRespVO.OntologyDefinitionVO llmDef = content.getDefinition();
            OntDefinitionVO defVO = OntDefinitionVO.builder()
                .namespace(llmDef.getNamespace())
                .name(llmDef.getName())
                .version(llmDef.getVersion())
                .description(llmDef.getDescription())
                .status("ACTIVE")
                .build();
            OntDefinitionVO createdDef;
            try {
                createdDef = ontologyClassService.createDefinition(graphId, defVO);
            } catch (Exception e) {
                // 如果定义已存在，尝试获取现有定义
                createdDef = ontologyClassService.getDefinition(graphId);
                if (createdDef == null) {
                    throw e;
                }
                log.info("本体定义已存在，使用现有定义: graphId={}", graphId);
            }

            // 2. 创建类
            if (content.getClasses() != null) {
                for (GenerateOntologyRespVO.OntologyClassVO cls : content.getClasses()) {
                    try {
                        OntClassVO classVO = new OntClassVO();
                        classVO.setLocalName(cls.getLocalName());
                        classVO.setClassUri(cls.getClassUri());
                        classVO.setDescription(cls.getDescription());
                        classVO.setExample(cls.getExample());
                        classVO.setDomainHint(cls.getDomainHint());
                        ontologyClassService.createClass(graphId, classVO);
                    } catch (Exception e) {
                        log.warn("创建类失败（可能已存在）: {}", cls.getLocalName(), e);
                    }
                }
            }

            // 3. 创建属性
            if (content.getProperties() != null) {
                List<OntClassVO> existingClasses = ontologyClassService.listClasses(graphId);
                Map<String, Long> classNameToId = existingClasses.stream()
                    .collect(Collectors.toMap(OntClassVO::getLocalName, OntClassVO::getId));

                for (GenerateOntologyRespVO.OntologyPropertyVO prop : content.getProperties()) {
                    try {
                        OntPropertyVO propVO = new OntPropertyVO();
                        propVO.setLocalName(prop.getLocalName());
                        propVO.setPropertyType(prop.getPropertyType());
                        propVO.setDescription(prop.getDescription());
                        propVO.setRangeDataType(prop.getRangeDataType());
                        propVO.setIsRequired(prop.getIsRequired());
                        propVO.setIsMultiple(prop.getIsMultiple());
                        propVO.setDomainClassId(classNameToId.get(prop.getDomainClass()));
                        propVO.setRangeClassId(classNameToId.get(prop.getRangeClass()));
                        ontologyPropertyService.createProperty(graphId, propVO);
                    } catch (Exception e) {
                        log.warn("创建属性失败（可能已存在）: {}", prop.getLocalName(), e);
                    }
                }
            }

            // 4. 更新草稿状态
            draft.setStatus("APPLIED");
            draft.setUpdatedAt(LocalDateTime.now());
            draftMapper.updateById(draft);

        } catch (Exception e) {
            log.error("应用草稿失败: draftId={}", draftId, e);
            throw new BusinessException(2002, "应用草稿失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void approveDraft(Long draftId) {
        OntDraftDO draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException(2000, "草稿不存在");
        }
        if (!"PENDING".equals(draft.getStatus())) {
            throw new BusinessException(2008, "只有待审核状态的草稿可以审核");
        }
        draft.setStatus("APPROVED");
        draft.setUpdatedAt(LocalDateTime.now());
        draftMapper.updateById(draft);
    }

    @Override
    @Transactional
    public void rejectDraft(Long draftId) {
        OntDraftDO draft = draftMapper.selectById(draftId);
        if (draft == null) {
            throw new BusinessException(2000, "草稿不存在");
        }
        if (!"PENDING".equals(draft.getStatus())) {
            throw new BusinessException(2008, "只有待审核状态的草稿可以审核");
        }
        draft.setStatus("REJECTED");
        draft.setUpdatedAt(LocalDateTime.now());
        draftMapper.updateById(draft);
    }

    @Override
    @Transactional
    public void deleteDraft(String graphId, Long draftId) {
        draftMapper.deleteById(draftId);
    }

    private OntDraftVO toVO(OntDraftDO draft) {
        OntDraftVO vo = new OntDraftVO();
        vo.setId(draft.getId());
        vo.setGraphId(draft.getGraphId());
        vo.setDraftName(draft.getDraftName());
        vo.setDraftType(draft.getDraftType());
        vo.setStatus(draft.getStatus());
        vo.setCreatedBy(draft.getCreatedBy());
        vo.setCreatedAt(draft.getCreatedAt());
        vo.setUpdatedAt(draft.getUpdatedAt());

        if (draft.getMockData() != null && !draft.getMockData().isBlank()) {
            vo.setHasMockData(true);
            try {
                var data = objectMapper.readValue(draft.getMockData(), Map.class);
                var entities = (List<?>) data.get("entities");
                var relationships = (List<?>) data.get("relationships");
                vo.setMockEntityCount(entities != null ? entities.size() : 0);
                vo.setMockRelationCount(relationships != null ? relationships.size() : 0);
            } catch (Exception e) {
                log.warn("解析模拟数据统计失败: draftId={}", draft.getId(), e);
            }
        }

        return vo;
    }
}
