package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntConstraintDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntPropertyDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntVersionHistoryDO;
import com.graphiti.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntConstraintMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntPropertyMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntVersionHistoryMapper;
import com.graphiti.module.graphiti.service.OntologyClassService;
import com.graphiti.module.graphiti.service.OntologyReasoner;
import com.graphiti.module.graphiti.vo.ontology.ClassHierarchyVO;
import com.graphiti.module.graphiti.vo.ontology.OntClassVO;
import com.graphiti.module.graphiti.vo.ontology.OntConstraintVO;
import com.graphiti.module.graphiti.vo.ontology.OntDefinitionVO;
import com.graphiti.module.graphiti.vo.ontology.OntologyFullVO;
import com.graphiti.module.graphiti.vo.ontology.OntPropertyVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyClassServiceImpl implements OntologyClassService {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntConstraintMapper constraintMapper;
    private final OntVersionHistoryMapper versionHistoryMapper;
    private final ObjectMapper objectMapper;
    private final OntologyReasoner reasoner;

    // ==================== 本体定义管理 ====================

    @Override
    @Transactional
    public OntDefinitionVO createDefinition(String graphId, OntDefinitionVO reqVO) {
        OntDefinitionDO entity = new OntDefinitionDO();
        entity.setGraphId(graphId);
        entity.setNamespace(reqVO.getNamespace() != null ? reqVO.getNamespace() : "http://graphiti.io/ontology");
        entity.setName(reqVO.getName());
        entity.setVersion(reqVO.getVersion() != null ? reqVO.getVersion() : "1.0.0");
        entity.setStatus("ACTIVE");
        entity.setDescription(reqVO.getDescription());
        entity.setCreatedBy(reqVO.getCreatedBy());
        definitionMapper.insert(entity);

        recordHistory(entity.getId(), "DEFINITION_CREATED", "DEFINITION", entity.getId(),
            null, entity, "创建本体定义: " + reqVO.getName(), reqVO.getCreatedBy());

        return toDefinitionVO(entity);
    }

    @Override
    public OntDefinitionVO getDefinition(String graphId) {
        OntDefinitionDO entity = resolveDefinition(graphId);
        if (entity == null) return null;
        return toDefinitionVO(entity);
    }

    @Override
    @Transactional
    public void rollbackVersion(String graphId, Long historyId) {
        OntVersionHistoryDO history = versionHistoryMapper.selectById(historyId);
        if (history == null) throw new BusinessException(1003, "版本历史记录不存在");
        if (history.getBeforeState() == null) throw new BusinessException(1004, "无法回滚：该记录没有保存变更前的状态");

        String entityType = history.getEntityType();
        Long defId = history.getDefinitionId();
        Long entityId = history.getEntityId();

        try {
            if ("CLASS".equals(entityType)) {
                OntClassDO restoreData = objectMapper.readValue(history.getBeforeState(), OntClassDO.class);
                restoreData.setId(entityId);
                restoreData.setDefinitionId(defId);
                classMapper.updateById(restoreData);
                recordHistory(defId, "CLASS_ROLLBACK", "CLASS", entityId,
                    null, restoreData, "回滚到历史版本: " + history.getVersion(), null);
            } else if ("PROPERTY".equals(entityType)) {
                OntPropertyDO restoreData = objectMapper.readValue(history.getBeforeState(), OntPropertyDO.class);
                restoreData.setId(entityId);
                restoreData.setDefinitionId(defId);
                propertyMapper.updateById(restoreData);
                recordHistory(defId, "PROPERTY_ROLLBACK", "PROPERTY", entityId,
                    null, restoreData, "回滚到历史版本: " + history.getVersion(), null);
            } else if ("CONSTRAINT".equals(entityType)) {
                OntConstraintDO restoreData = objectMapper.readValue(history.getBeforeState(), OntConstraintDO.class);
                restoreData.setId(entityId);
                restoreData.setDefinitionId(defId);
                constraintMapper.updateById(restoreData);
                recordHistory(defId, "CONSTRAINT_ROLLBACK", "CONSTRAINT", entityId,
                    null, restoreData, "回滚到历史版本: " + history.getVersion(), null);
            } else {
                throw new BusinessException(1005, "不支持的实体类型回滚: " + entityType);
            }
        } catch (Exception e) {
            throw new BusinessException(1006, "回滚失败: " + e.getMessage());
        }
    }

    @Override
    public OntologyFullVO getFullOntology(String graphId) {
        OntDefinitionDO definition = resolveDefinition(graphId);
        if (definition == null) {
            throw new BusinessException(1002, "本体未定义");
        }

        Long defId = definition.getId();

        List<OntClassVO> classes = classMapper.selectByDefinitionId(defId).stream()
            .map(this::toVO).collect(Collectors.toList());

        List<ClassHierarchyVO> hierarchy = buildClassHierarchy(defId);

        List<OntPropertyVO> properties = propertyMapper.selectByDefinitionId(defId).stream()
            .map(this::toPropertyVO).collect(Collectors.toList());

        List<OntConstraintVO> constraints = constraintMapper.selectByDefinitionId(defId).stream()
            .map(this::toConstraintVO).collect(Collectors.toList());

        return OntologyFullVO.builder()
            .definition(toDefinitionVO(definition))
            .classes(classes)
            .classHierarchy(hierarchy)
            .properties(properties)
            .constraints(constraints)
            .build();
    }

    // ==================== 类管理 ====================

    @Override
    @Transactional
    public OntClassVO createClass(String graphId, OntClassVO reqVO) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) {
            throw new BusinessException(2002, "图谱未定义本体，请先创建本体定义");
        }

        String classUri = reqVO.getClassUri();
        if (classUri == null || classUri.isBlank()) {
            classUri = "http://graphiti.io/" + reqVO.getLocalName();
        }

        OntClassDO entity = new OntClassDO();
        entity.setDefinitionId(defId);
        entity.setClassUri(classUri);
        entity.setLocalName(reqVO.getLocalName());
        entity.setParentClassId(reqVO.getParentClassId());
        entity.setDescription(reqVO.getDescription());
        entity.setExample(reqVO.getExample());
        entity.setDomainHint(reqVO.getDomainHint());
        entity.setMetadata(reqVO.getMetadata());
        if (reqVO.getEquivalentTo() != null && !reqVO.getEquivalentTo().isEmpty()) {
            try {
                entity.setEquivalentTo(objectMapper.writeValueAsString(reqVO.getEquivalentTo()));
            } catch (Exception e) {
                log.warn("序列化 equivalentTo 失败", e);
            }
        }
        if (reqVO.getDisjointWith() != null && !reqVO.getDisjointWith().isEmpty()) {
            try {
                entity.setDisjointWith(objectMapper.writeValueAsString(reqVO.getDisjointWith()));
            } catch (Exception e) {
                log.warn("序列化 disjointWith 失败", e);
            }
        }

        classMapper.insert(entity);
        recordHistory(defId, "CLASS_ADDED", "CLASS", entity.getId(),
            null, entity, "新增类: " + reqVO.getLocalName(), null);
        reasoner.shutdown(graphId); // 缓存失效

        return toVO(entity);
    }

    @Override
    @Transactional
    public OntClassVO updateClass(String graphId, Long classId, OntClassVO reqVO) {
        OntClassDO existing = classMapper.selectById(classId);
        if (existing == null) throw new BusinessException(1003, "类定义不存在");

        OntClassDO before = cloneDO(existing);

        if (reqVO.getLocalName() != null) existing.setLocalName(reqVO.getLocalName());
        if (reqVO.getClassUri() != null) existing.setClassUri(reqVO.getClassUri());
        if (reqVO.getDescription() != null) existing.setDescription(reqVO.getDescription());
        if (reqVO.getParentClassId() != null) existing.setParentClassId(reqVO.getParentClassId());
        if (reqVO.getDomainHint() != null) existing.setDomainHint(reqVO.getDomainHint());
        if (reqVO.getEquivalentTo() != null) {
            try {
                existing.setEquivalentTo(objectMapper.writeValueAsString(reqVO.getEquivalentTo()));
            } catch (Exception e) {
                log.warn("序列化 equivalentTo 失败", e);
            }
        }
        if (reqVO.getDisjointWith() != null) {
            try {
                existing.setDisjointWith(objectMapper.writeValueAsString(reqVO.getDisjointWith()));
            } catch (Exception e) {
                log.warn("序列化 disjointWith 失败", e);
            }
        }

        classMapper.updateById(existing);
        recordHistory(existing.getDefinitionId(), "CLASS_MODIFIED", "CLASS", classId,
            before, existing, "更新类: " + existing.getLocalName(), null);
        reasoner.shutdown(graphId); // 缓存失效

        return toVO(existing);
    }

    @Override
    @Transactional
    public void deleteClass(String graphId, Long classId) {
        OntClassDO existing = classMapper.selectById(classId);
        if (existing == null) return;

        LambdaQueryWrapper<OntClassDO> cw = new LambdaQueryWrapper<>();
        cw.eq(OntClassDO::getParentClassId, classId);
        if (classMapper.selectCount(cw) > 0) {
            throw new BusinessException(2003, "无法删除：该类存在子类型，请先删除子类型");
        }

        recordHistory(existing.getDefinitionId(), "CLASS_DELETED", "CLASS", classId,
            existing, null, "删除类: " + existing.getLocalName(), null);

        classMapper.deleteById(classId);
        reasoner.shutdown(graphId); // 缓存失效
    }

    @Override
    public OntClassVO getClass(String graphId, Long classId) {
        OntClassDO entity = classMapper.selectById(classId);
        if (entity == null) throw new BusinessException(1003, "类定义不存在");
        return toVO(entity);
    }

    @Override
    public List<OntClassVO> listClasses(String graphId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();
        return classMapper.selectByDefinitionId(defId).stream()
            .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ClassHierarchyVO> getClassHierarchy(String graphId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();
        return buildClassHierarchy(defId);
    }

    @Override
    public List<String> getDescendantClasses(String graphId, Long classId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();
        Set<String> descendants = new HashSet<>();
        collectDescendants(defId, classId, descendants);
        return new ArrayList<>(descendants);
    }

    // ==================== 私有方法 ====================

    private OntDefinitionDO resolveDefinition(String graphId) {
        LambdaQueryWrapper<OntDefinitionDO> w = new LambdaQueryWrapper<>();
        w.eq(OntDefinitionDO::getGraphId, graphId);
        w.eq(OntDefinitionDO::getStatus, "ACTIVE");
        w.last("LIMIT 1");
        return definitionMapper.selectOne(w);
    }

    private Long resolveDefinitionId(String graphId) {
        OntDefinitionDO def = resolveDefinition(graphId);
        return def != null ? def.getId() : null;
    }

    private List<ClassHierarchyVO> buildClassHierarchy(Long defId) {
        List<OntClassDO> allClasses = classMapper.selectByDefinitionId(defId);
        Map<Long, List<OntClassDO>> childrenMap = allClasses.stream()
            .filter(c -> c.getParentClassId() != null)
            .collect(Collectors.groupingBy(OntClassDO::getParentClassId));

        List<OntClassDO> roots = allClasses.stream()
            .filter(c -> c.getParentClassId() == null)
            .collect(Collectors.toList());

        return roots.stream().map(root -> buildHierarchyNode(root, childrenMap)).collect(Collectors.toList());
    }

    private ClassHierarchyVO buildHierarchyNode(OntClassDO cls, Map<Long, List<OntClassDO>> childrenMap) {
        List<ClassHierarchyVO> childVOs = childrenMap.getOrDefault(cls.getId(), List.of())
            .stream().map(c -> buildHierarchyNode(c, childrenMap)).collect(Collectors.toList());
        return ClassHierarchyVO.builder()
            .classUri(cls.getClassUri())
            .localName(cls.getLocalName())
            .description(cls.getDescription())
            .domainHint(cls.getDomainHint())
            .children(childVOs)
            .build();
    }

    private void collectDescendants(Long defId, Long classId, Set<String> result) {
        LambdaQueryWrapper<OntClassDO> w = new LambdaQueryWrapper<>();
        w.eq(OntClassDO::getDefinitionId, defId);
        w.eq(OntClassDO::getParentClassId, classId);
        List<OntClassDO> children = classMapper.selectList(w);
        for (OntClassDO child : children) {
            result.add(child.getLocalName());
            collectDescendants(defId, child.getId(), result);
        }
    }

    private OntDefinitionVO toDefinitionVO(OntDefinitionDO entity) {
        OntDefinitionVO vo = new OntDefinitionVO();
        vo.setId(entity.getId());
        vo.setGraphId(entity.getGraphId());
        vo.setNamespace(entity.getNamespace());
        vo.setName(entity.getName());
        vo.setVersion(entity.getVersion());
        vo.setStatus(entity.getStatus());
        vo.setDescription(entity.getDescription());
        vo.setParentVersionId(entity.getParentVersionId());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());

        // 统计数量
        vo.setClassCount(classMapper.selectByDefinitionId(entity.getId()).size());
        vo.setPropertyCount(propertyMapper.selectByDefinitionId(entity.getId()).size());
        vo.setConstraintCount(constraintMapper.selectByDefinitionId(entity.getId()).size());

        return vo;
    }

    private OntClassVO toVO(OntClassDO entity) {
        OntClassVO vo = new OntClassVO();
        vo.setId(entity.getId());
        vo.setDefinitionId(entity.getDefinitionId());
        vo.setClassUri(entity.getClassUri());
        vo.setLocalName(entity.getLocalName());
        vo.setParentClassId(entity.getParentClassId());
        vo.setDescription(entity.getDescription());
        vo.setExample(entity.getExample());
        vo.setDomainHint(entity.getDomainHint());
        vo.setMetadata(entity.getMetadata());
        vo.setCreatedAt(entity.getCreatedAt());
        if (entity.getEquivalentTo() != null && !entity.getEquivalentTo().isBlank()) {
            try {
                vo.setEquivalentTo(objectMapper.readValue(entity.getEquivalentTo(), List.class));
            } catch (Exception e) {
                log.warn("解析 equivalentTo 失败", e);
            }
        }
        if (entity.getDisjointWith() != null && !entity.getDisjointWith().isBlank()) {
            try {
                vo.setDisjointWith(objectMapper.readValue(entity.getDisjointWith(), List.class));
            } catch (Exception e) {
                log.warn("解析 disjointWith 失败", e);
            }
        }
        if (entity.getParentClassId() != null) {
            OntClassDO parent = classMapper.selectById(entity.getParentClassId());
            if (parent != null) vo.setParentClassUri(parent.getClassUri());
        }
        return vo;
    }

    private OntPropertyVO toPropertyVO(OntPropertyDO entity) {
        OntPropertyVO vo = new OntPropertyVO();
        vo.setId(entity.getId());
        vo.setDefinitionId(entity.getDefinitionId());
        vo.setPropertyUri(entity.getPropertyUri());
        vo.setLocalName(entity.getLocalName());
        vo.setPropertyType(entity.getPropertyType());
        vo.setDomainClassId(entity.getDomainClassId());
        vo.setRangeClassId(entity.getRangeClassId());
        vo.setRangeDataType(entity.getRangeDataType());
        vo.setIsRequired(entity.getIsRequired());
        vo.setIsMultiple(entity.getIsMultiple());
        vo.setMinCardinality(entity.getMinCardinality());
        vo.setMaxCardinality(entity.getMaxCardinality());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private OntConstraintVO toConstraintVO(OntConstraintDO entity) {
        OntConstraintVO vo = new OntConstraintVO();
        vo.setId(entity.getId());
        vo.setDefinitionId(entity.getDefinitionId());
        vo.setClassId(entity.getClassId());
        vo.setPropertyId(entity.getPropertyId());
        vo.setConstraintType(entity.getConstraintType());
        vo.setValue(entity.getValue());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setSeverity(entity.getSeverity());
        vo.setDescription(entity.getDescription());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private OntClassDO cloneDO(OntClassDO src) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(src), OntClassDO.class);
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
