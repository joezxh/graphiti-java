package com.graphiti.module.graphiti.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntClassDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntVersionHistoryDO;
import com.graphiti.module.graphiti.dal.mysql.ont.OntClassMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.graphiti.module.graphiti.dal.mysql.ont.OntVersionHistoryMapper;
import com.graphiti.module.graphiti.service.OntologyClassService;
import com.graphiti.module.graphiti.vo.ontology.ClassHierarchyVO;
import com.graphiti.module.graphiti.vo.ontology.OntClassVO;
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
    private final OntVersionHistoryMapper versionHistoryMapper;
    private final ObjectMapper objectMapper;

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

        classMapper.insert(entity);
        recordHistory(defId, "CLASS_ADDED", "CLASS", entity.getId(),
            null, entity, "新增类: " + reqVO.getLocalName(), null);

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

        classMapper.updateById(existing);
        recordHistory(existing.getDefinitionId(), "CLASS_MODIFIED", "CLASS", classId,
            before, existing, "更新类: " + existing.getLocalName(), null);

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

        List<OntClassDO> allClasses = classMapper.selectByDefinitionId(defId);
        Map<Long, List<OntClassDO>> childrenMap = allClasses.stream()
            .filter(c -> c.getParentClassId() != null)
            .collect(Collectors.groupingBy(OntClassDO::getParentClassId));

        List<OntClassDO> roots = allClasses.stream()
            .filter(c -> c.getParentClassId() == null)
            .collect(Collectors.toList());

        return roots.stream().map(root -> buildHierarchy(root, childrenMap)).collect(Collectors.toList());
    }

    @Override
    public List<String> getDescendantClasses(String graphId, Long classId) {
        Long defId = resolveDefinitionId(graphId);
        if (defId == null) return List.of();
        Set<String> descendants = new HashSet<>();
        collectDescendants(defId, classId, descendants);
        return new ArrayList<>(descendants);
    }

    private Long resolveDefinitionId(String graphId) {
        LambdaQueryWrapper<OntDefinitionDO> w = new LambdaQueryWrapper<>();
        w.eq(OntDefinitionDO::getGraphId, graphId);
        w.eq(OntDefinitionDO::getStatus, "ACTIVE");
        w.last("LIMIT 1");
        OntDefinitionDO def = definitionMapper.selectOne(w);
        return def != null ? def.getId() : null;
    }

    private ClassHierarchyVO buildHierarchy(OntClassDO cls, Map<Long, List<OntClassDO>> childrenMap) {
        List<ClassHierarchyVO> childVOs = childrenMap.getOrDefault(cls.getId(), List.of())
            .stream().map(c -> buildHierarchy(c, childrenMap)).collect(Collectors.toList());
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
