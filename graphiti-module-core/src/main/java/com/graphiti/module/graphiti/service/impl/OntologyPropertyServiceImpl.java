package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.ont.*;
import com.graphiti.module.graphiti.dal.mysql.ont.*;
import com.graphiti.module.graphiti.service.OntologyPropertyService;
import com.graphiti.module.graphiti.vo.ontology.OntPropertyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyPropertyServiceImpl implements OntologyPropertyService {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntConstraintMapper constraintMapper;
    private final OntVersionHistoryMapper versionHistoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public OntPropertyVO createProperty(String graphId, OntPropertyVO reqVO) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) throw new BusinessException(2002, "图谱未定义本体");

        String propUri = reqVO.getPropertyUri();
        if (propUri == null || propUri.isBlank()) {
            propUri = "http://graphiti.io/" + reqVO.getLocalName();
        }

        if (reqVO.getDomainClassId() != null && classMapper.selectById(reqVO.getDomainClassId()) == null) {
            throw new BusinessException(2004, "domainClassId 不存在: " + reqVO.getDomainClassId());
        }
        if (reqVO.getRangeClassId() != null && classMapper.selectById(reqVO.getRangeClassId()) == null) {
            throw new BusinessException(2004, "rangeClassId 不存在: " + reqVO.getRangeClassId());
        }

        OntPropertyDO entity = toEntity(reqVO, defId, propUri);
        propertyMapper.insert(entity);

        recordHistory(defId, "PROPERTY_ADDED", "PROPERTY", entity.getId(),
            null, entity, "新增属性: " + reqVO.getLocalName(), null);

        return toVO(entity);
    }

    @Override
    @Transactional
    public OntPropertyVO updateProperty(String graphId, Long propertyId, OntPropertyVO reqVO) {
        OntPropertyDO existing = propertyMapper.selectById(propertyId);
        if (existing == null) throw new BusinessException(1003, "属性不存在");

        OntPropertyDO before = cloneDO(existing);
        if (reqVO.getLocalName() != null) existing.setLocalName(reqVO.getLocalName());
        if (reqVO.getPropertyUri() != null) existing.setPropertyUri(reqVO.getPropertyUri());
        if (reqVO.getPropertyType() != null) existing.setPropertyType(reqVO.getPropertyType());
        if (reqVO.getDomainClassId() != null) existing.setDomainClassId(reqVO.getDomainClassId());
        if (reqVO.getRangeClassId() != null) existing.setRangeClassId(reqVO.getRangeClassId());
        if (reqVO.getRangeDataType() != null) existing.setRangeDataType(reqVO.getRangeDataType());
        if (reqVO.getIsRequired() != null) existing.setIsRequired(reqVO.getIsRequired());
        if (reqVO.getPattern() != null) existing.setPattern(reqVO.getPattern());
        if (reqVO.getDescription() != null) existing.setDescription(reqVO.getDescription());

        propertyMapper.updateById(existing);
        recordHistory(existing.getDefinitionId(), "PROPERTY_MODIFIED", "PROPERTY", propertyId,
            before, existing, "更新属性: " + existing.getLocalName(), null);

        return toVO(existing);
    }

    @Override
    @Transactional
    public void deleteProperty(String graphId, Long propertyId) {
        OntPropertyDO existing = propertyMapper.selectById(propertyId);
        if (existing == null) return;

        LambdaQueryWrapper<OntConstraintDO> cw = new LambdaQueryWrapper<>();
        cw.eq(OntConstraintDO::getPropertyId, propertyId);
        if (constraintMapper.selectCount(cw) > 0) {
            throw new BusinessException(2005, "无法删除：存在约束引用此属性");
        }

        recordHistory(existing.getDefinitionId(), "PROPERTY_DELETED", "PROPERTY", propertyId,
            existing, null, "删除属性: " + existing.getLocalName(), null);
        propertyMapper.deleteById(propertyId);
    }

    @Override
    public OntPropertyVO getProperty(String graphId, Long propertyId) {
        OntPropertyDO entity = propertyMapper.selectById(propertyId);
        if (entity == null) throw new BusinessException(1003, "属性不存在");
        return toVO(entity);
    }

    @Override
    public List<OntPropertyVO> listProperties(String graphId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();
        return propertyMapper.selectByDefinitionId(defId).stream()
            .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<OntPropertyVO> getPropertiesForClass(String graphId, Long classId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();
        return propertyMapper.selectByClassId(defId, classId).stream()
            .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<String> getPropertyAncestors(String graphId, Long propertyId) {
        List<String> ancestors = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return ancestors;
        collectPropertyAncestors(propertyId, ancestors, visited);
        return ancestors;
    }

    private Long resolveDefinitionId(String graphId) {
        LambdaQueryWrapper<OntDefinitionDO> w = new LambdaQueryWrapper<>();
        w.eq(OntDefinitionDO::getGraphId, graphId);
        w.eq(OntDefinitionDO::getStatus, "ACTIVE");
        w.last("LIMIT 1");
        OntDefinitionDO def = definitionMapper.selectOne(w);
        return def != null ? def.getId() : null;
    }

    private void collectPropertyAncestors(Long propId, List<String> result, Set<Long> visited) {
        if (visited.contains(propId)) return;
        visited.add(propId);
        OntPropertyDO prop = propertyMapper.selectById(propId);
        if (prop != null && prop.getParentPropertyId() != null) {
            OntPropertyDO parent = propertyMapper.selectById(prop.getParentPropertyId());
            if (parent != null) {
                result.add(parent.getLocalName());
                collectPropertyAncestors(parent.getId(), result, visited);
            }
        }
    }

    private OntPropertyVO toVO(OntPropertyDO entity) {
        OntPropertyVO vo = OntPropertyVO.builder()
            .id(entity.getId())
            .definitionId(entity.getDefinitionId())
            .propertyUri(entity.getPropertyUri())
            .localName(entity.getLocalName())
            .propertyType(entity.getPropertyType())
            .domainClassId(entity.getDomainClassId())
            .rangeClassId(entity.getRangeClassId())
            .rangeDataType(entity.getRangeDataType())
            .minCardinality(entity.getMinCardinality())
            .maxCardinality(entity.getMaxCardinality())
            .defaultValue(entity.getDefaultValue())
            .parentPropertyId(entity.getParentPropertyId())
            .inverseOfId(entity.getInverseOfId())
            .isRequired(entity.getIsRequired())
            .isMultiple(entity.getIsMultiple())
            .pattern(entity.getPattern())
            .minValue(entity.getMinValue())
            .maxValue(entity.getMaxValue())
            .description(entity.getDescription())
            .example(entity.getExample())
            .metadata(entity.getMetadata())
            .createdAt(entity.getCreatedAt())
            .build();

        if (entity.getAllowedValues() != null && !entity.getAllowedValues().isBlank()) {
            try {
                vo.setAllowedValues(objectMapper.readValue(entity.getAllowedValues(), List.class));
            } catch (Exception e) {
                log.warn("解析 allowedValues 失败", e);
            }
        }
        if (entity.getEquivalentTo() != null && !entity.getEquivalentTo().isBlank()) {
            try {
                vo.setEquivalentTo(objectMapper.readValue(entity.getEquivalentTo(), List.class));
            } catch (Exception e) {
                log.warn("解析 equivalentTo 失败", e);
            }
        }

        if (entity.getDomainClassId() != null) {
            OntClassDO domain = classMapper.selectById(entity.getDomainClassId());
            if (domain != null) vo.setDomainClassUri(domain.getClassUri());
        }
        if (entity.getRangeClassId() != null) {
            OntClassDO range = classMapper.selectById(entity.getRangeClassId());
            if (range != null) vo.setRangeClassUri(range.getClassUri());
        }
        if (entity.getParentPropertyId() != null) {
            OntPropertyDO parent = propertyMapper.selectById(entity.getParentPropertyId());
            if (parent != null) vo.setParentPropertyUri(parent.getPropertyUri());
        }
        if (entity.getInverseOfId() != null) {
            OntPropertyDO inverse = propertyMapper.selectById(entity.getInverseOfId());
            if (inverse != null) vo.setInverseOfUri(inverse.getPropertyUri());
        }
        return vo;
    }

    private OntPropertyDO toEntity(OntPropertyVO req, Long defId, String propUri) {
        OntPropertyDO entity = new OntPropertyDO();
        entity.setDefinitionId(defId);
        entity.setPropertyUri(propUri);
        entity.setLocalName(req.getLocalName());
        entity.setPropertyType(req.getPropertyType() != null ? req.getPropertyType() : "DATATYPE");
        entity.setDomainClassId(req.getDomainClassId());
        entity.setRangeClassId(req.getRangeClassId());
        entity.setRangeDataType(req.getRangeDataType());
        entity.setMinCardinality(req.getMinCardinality());
        entity.setMaxCardinality(req.getMaxCardinality());
        entity.setDefaultValue(req.getDefaultValue());
        if (req.getAllowedValues() != null) {
            try {
                entity.setAllowedValues(objectMapper.writeValueAsString(req.getAllowedValues()));
            } catch (Exception e) {
                log.warn("序列化 allowedValues 失败", e);
            }
        }
        entity.setParentPropertyId(req.getParentPropertyId());
        if (req.getEquivalentTo() != null) {
            try {
                entity.setEquivalentTo(objectMapper.writeValueAsString(req.getEquivalentTo()));
            } catch (Exception e) {
                log.warn("序列化 equivalentTo 失败", e);
            }
        }
        entity.setInverseOfId(req.getInverseOfId());
        entity.setIsRequired(req.getIsRequired() != null ? req.getIsRequired() : false);
        entity.setIsMultiple(req.getIsMultiple() != null ? req.getIsMultiple() : false);
        entity.setPattern(req.getPattern());
        entity.setMinValue(req.getMinValue());
        entity.setMaxValue(req.getMaxValue());
        entity.setDescription(req.getDescription());
        entity.setExample(req.getExample());
        entity.setMetadata(req.getMetadata());
        return entity;
    }

    private OntPropertyDO cloneDO(OntPropertyDO src) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(src), OntPropertyDO.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void recordHistory(Long defId, String changeType, String entityType,
            Long entityId, Object before, Object after, String diffSummary, String changedBy) {
        try {
            OntVersionHistoryDO history = new OntVersionHistoryDO();
            history.setDefinitionId(defId);
            history.setVersion("1.0.0");
            history.setChangeType(changeType);
            history.setEntityType(entityType);
            history.setEntityId(entityId);
            history.setBeforeState(before != null ? objectMapper.writeValueAsString(before) : null);
            history.setAfterState(after != null ? objectMapper.writeValueAsString(after) : null);
            history.setDiffSummary(diffSummary);
            history.setChangedBy(changedBy);
            versionHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("记录版本历史失败", e);
        }
    }
}
