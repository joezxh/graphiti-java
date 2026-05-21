package com.graphiti.module.graphiti.service.metadata;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntCommunityTypeDO;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntEpisodeTypeDO;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntEntityCategoryDO;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntRelationshipMetaDO;
import com.graphiti.module.graphiti.dal.mysql.metadata.OntCommunityTypeMapper;
import com.graphiti.module.graphiti.dal.mysql.metadata.OntEpisodeTypeMapper;
import com.graphiti.module.graphiti.dal.mysql.metadata.OntEntityCategoryMapper;
import com.graphiti.module.graphiti.dal.mysql.metadata.OntRelationshipMetaMapper;
import com.graphiti.module.graphiti.vo.metadata.*;
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
public class OntMetadataServiceImpl implements OntMetadataService {

    private final OntCommunityTypeMapper communityTypeMapper;
    private final OntEpisodeTypeMapper episodeTypeMapper;
    private final OntEntityCategoryMapper entityCategoryMapper;
    private final OntRelationshipMetaMapper relationshipMetaMapper;

    // ==================== Community Type ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCommunityType(OntCommunityTypeReqVO reqVO) {
        OntCommunityTypeDO entity = new OntCommunityTypeDO();
        copyCommunityType(reqVO, entity);
        communityTypeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCommunityType(Long id, OntCommunityTypeReqVO reqVO) {
        OntCommunityTypeDO entity = communityTypeMapper.selectById(id);
        if (entity == null) throw new RuntimeException("社区类型不存在: " + id);
        copyCommunityType(reqVO, entity);
        communityTypeMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommunityType(Long id) {
        communityTypeMapper.deleteById(id);
    }

    @Override
    public OntCommunityTypeRespVO getCommunityTypeById(Long id) {
        return toCommunityTypeRespVO(communityTypeMapper.selectById(id));
    }

    @Override
    public List<OntCommunityTypeRespVO> listCommunityTypes(Long definitionId) {
        return communityTypeMapper.selectByDefinitionId(definitionId)
                .stream().map(this::toCommunityTypeRespVO).collect(Collectors.toList());
    }

    @Override
    public List<OntCommunityTypeRespVO> listCommunityTypesByCategory(Long definitionId, String category) {
        return communityTypeMapper.selectByCategory(definitionId, category)
                .stream().map(this::toCommunityTypeRespVO).collect(Collectors.toList());
    }

    @Override
    public List<OntCommunityTypeRespVO> getCommunityTypeTree(Long definitionId) {
        List<OntCommunityTypeDO> roots = communityTypeMapper.selectRootTypes(definitionId);
        return roots.stream().map(this::buildCommunityTypeNode).collect(Collectors.toList());
    }

    private OntCommunityTypeRespVO buildCommunityTypeNode(OntCommunityTypeDO root) {
        OntCommunityTypeRespVO node = toCommunityTypeRespVO(root);
        List<OntCommunityTypeDO> children = communityTypeMapper.selectByParentType(root.getDefinitionId(), root.getTypeCode());
        if (children != null && !children.isEmpty()) {
            node.setMetadata(children.stream()
                    .map(this::toCommunityTypeRespVO)
                    .collect(Collectors.toList()).toString());
        }
        return node;
    }

    private void copyCommunityType(OntCommunityTypeReqVO reqVO, OntCommunityTypeDO entity) {
        if (reqVO.getDefinitionId() != null) entity.setDefinitionId(reqVO.getDefinitionId());
        if (reqVO.getTypeCode() != null) entity.setTypeCode(reqVO.getTypeCode());
        if (reqVO.getTypeName() != null) entity.setTypeName(reqVO.getTypeName());
        if (reqVO.getTypeNameEn() != null) entity.setTypeNameEn(reqVO.getTypeNameEn());
        if (reqVO.getCategory() != null) entity.setCategory(reqVO.getCategory());
        if (reqVO.getDescription() != null) entity.setDescription(reqVO.getDescription());
        if (reqVO.getParentTypeCode() != null) entity.setParentTypeCode(reqVO.getParentTypeCode());
        if (reqVO.getSortOrder() != null) entity.setSortOrder(reqVO.getSortOrder());
        if (reqVO.getRegion() != null) entity.setRegion(reqVO.getRegion());
        if (reqVO.getScenarioType() != null) entity.setScenarioType(reqVO.getScenarioType());
        if (reqVO.getCommunityUuid() != null) entity.setCommunityUuid(reqVO.getCommunityUuid());
        if (reqVO.getGraphId() != null) entity.setGraphId(reqVO.getGraphId());
        if (reqVO.getMetadata() != null) entity.setMetadata(reqVO.getMetadata());
        if (reqVO.getStatus() != null) entity.setStatus(reqVO.getStatus());
    }

    private OntCommunityTypeRespVO toCommunityTypeRespVO(OntCommunityTypeDO entity) {
        if (entity == null) return null;
        return OntCommunityTypeRespVO.builder()
                .id(entity.getId())
                .definitionId(entity.getDefinitionId())
                .typeCode(entity.getTypeCode())
                .typeName(entity.getTypeName())
                .typeNameEn(entity.getTypeNameEn())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .parentTypeCode(entity.getParentTypeCode())
                .sortOrder(entity.getSortOrder())
                .region(entity.getRegion())
                .scenarioType(entity.getScenarioType())
                .communityUuid(entity.getCommunityUuid())
                .graphId(entity.getGraphId())
                .metadata(entity.getMetadata())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ==================== Episode Type ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEpisodeType(OntEpisodeTypeReqVO reqVO) {
        OntEpisodeTypeDO entity = new OntEpisodeTypeDO();
        copyEpisodeType(reqVO, entity);
        episodeTypeMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEpisodeType(Long id, OntEpisodeTypeReqVO reqVO) {
        OntEpisodeTypeDO entity = episodeTypeMapper.selectById(id);
        if (entity == null) throw new RuntimeException("剧集类型不存在: " + id);
        copyEpisodeType(reqVO, entity);
        episodeTypeMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEpisodeType(Long id) {
        episodeTypeMapper.deleteById(id);
    }

    @Override
    public OntEpisodeTypeRespVO getEpisodeTypeById(Long id) {
        return toEpisodeTypeRespVO(episodeTypeMapper.selectById(id));
    }

    @Override
    public List<OntEpisodeTypeRespVO> listEpisodeTypes(Long definitionId) {
        return episodeTypeMapper.selectByDefinitionId(definitionId)
                .stream().map(this::toEpisodeTypeRespVO).collect(Collectors.toList());
    }

    @Override
    public List<OntEpisodeTypeRespVO> listEpisodeTypesByProcess(Long definitionId, String legalProcess) {
        return episodeTypeMapper.selectByLegalProcess(definitionId, legalProcess)
                .stream().map(this::toEpisodeTypeRespVO).collect(Collectors.toList());
    }

    @Override
    public List<OntEpisodeTypeRespVO> listEpisodeTypesByProcessType(Long definitionId, String processType) {
        return episodeTypeMapper.selectByProcessType(definitionId, processType)
                .stream().map(this::toEpisodeTypeRespVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateEpisodeTypes(Long definitionId, List<OntEpisodeTypeReqVO> reqVOs) {
        int count = 0;
        for (OntEpisodeTypeReqVO req : reqVOs) {
            req.setDefinitionId(definitionId);
            createEpisodeType(req);
            count++;
        }
        return count;
    }

    private void copyEpisodeType(OntEpisodeTypeReqVO reqVO, OntEpisodeTypeDO entity) {
        if (reqVO.getDefinitionId() != null) entity.setDefinitionId(reqVO.getDefinitionId());
        if (reqVO.getTypeCode() != null) entity.setTypeCode(reqVO.getTypeCode());
        if (reqVO.getTypeName() != null) entity.setTypeName(reqVO.getTypeName());
        if (reqVO.getTypeNameEn() != null) entity.setTypeNameEn(reqVO.getTypeNameEn());
        if (reqVO.getStageLabel() != null) entity.setStageLabel(reqVO.getStageLabel());
        if (reqVO.getDescription() != null) entity.setDescription(reqVO.getDescription());
        if (reqVO.getSortOrder() != null) entity.setSortOrder(reqVO.getSortOrder());
        if (reqVO.getMetadata() != null) entity.setMetadata(reqVO.getMetadata());
        if (reqVO.getStatus() != null) entity.setStatus(reqVO.getStatus());
        // 通用化字段
        if (reqVO.getProcessType() != null) entity.setProcessType(reqVO.getProcessType());
        if (reqVO.getStageLevel() != null) entity.setStageLevel(reqVO.getStageLevel());
        if (reqVO.getIsReviewStage() != null) entity.setIsReviewStage(reqVO.getIsReviewStage());
        // 向后兼容旧字段
        if (reqVO.getLegalProcess() != null) entity.setLegalProcess(reqVO.getLegalProcess());
        if (reqVO.getCourtLevel() != null) entity.setCourtLevel(reqVO.getCourtLevel());
        if (reqVO.getIsTrialStage() != null) entity.setIsTrialStage(reqVO.getIsTrialStage());
    }

    private OntEpisodeTypeRespVO toEpisodeTypeRespVO(OntEpisodeTypeDO entity) {
        if (entity == null) return null;
        return OntEpisodeTypeRespVO.builder()
                .id(entity.getId())
                .definitionId(entity.getDefinitionId())
                .typeCode(entity.getTypeCode())
                .typeName(entity.getTypeName())
                .typeNameEn(entity.getTypeNameEn())
                .stageLabel(entity.getStageLabel())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .metadata(entity.getMetadata())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                // 通用化字段
                .processType(entity.getProcessType())
                .stageLevel(entity.getStageLevel())
                .isReviewStage(entity.getIsReviewStage())
                // 向后兼容旧字段
                .legalProcess(entity.getLegalProcess())
                .courtLevel(entity.getCourtLevel())
                .isTrialStage(entity.getIsTrialStage())
                .build();
    }

    // ==================== Entity Category ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEntityCategory(OntEntityCategoryReqVO reqVO) {
        OntEntityCategoryDO entity = new OntEntityCategoryDO();
        copyEntityCategory(reqVO, entity);
        entityCategoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEntityCategory(Long id, OntEntityCategoryReqVO reqVO) {
        OntEntityCategoryDO entity = entityCategoryMapper.selectById(id);
        if (entity == null) throw new RuntimeException("实体分类不存在: " + id);
        copyEntityCategory(reqVO, entity);
        entityCategoryMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEntityCategory(Long id) {
        entityCategoryMapper.deleteById(id);
    }

    @Override
    public OntEntityCategoryRespVO getEntityCategoryById(Long id) {
        return toEntityCategoryRespVO(entityCategoryMapper.selectById(id));
    }

    @Override
    public List<OntEntityCategoryRespVO> listEntityCategories(Long definitionId) {
        return entityCategoryMapper.selectByDefinitionId(definitionId)
                .stream().map(this::toEntityCategoryRespVO).collect(Collectors.toList());
    }

    @Override
    public List<OntEntityCategoryRespVO> getEntityCategoryTree(Long definitionId) {
        List<OntEntityCategoryDO> roots = entityCategoryMapper.selectRootCategories(definitionId);
        return roots.stream().map(this::buildEntityCategoryNode).collect(Collectors.toList());
    }

    private OntEntityCategoryRespVO buildEntityCategoryNode(OntEntityCategoryDO root) {
        OntEntityCategoryRespVO node = toEntityCategoryRespVO(root);
        List<OntEntityCategoryDO> children = entityCategoryMapper.selectByParentCode(root.getDefinitionId(), root.getCategoryCode());
        if (children != null && !children.isEmpty()) {
            node.setChildren(children.stream().map(this::buildEntityCategoryNode).collect(Collectors.toList()));
        }
        return node;
    }

    private void copyEntityCategory(OntEntityCategoryReqVO reqVO, OntEntityCategoryDO entity) {
        if (reqVO.getDefinitionId() != null) entity.setDefinitionId(reqVO.getDefinitionId());
        if (reqVO.getCategoryCode() != null) entity.setCategoryCode(reqVO.getCategoryCode());
        if (reqVO.getCategoryName() != null) entity.setCategoryName(reqVO.getCategoryName());
        if (reqVO.getCategoryLevel() != null) entity.setCategoryLevel(reqVO.getCategoryLevel());
        if (reqVO.getParentCategoryCode() != null) entity.setParentCategoryCode(reqVO.getParentCategoryCode());
        if (reqVO.getEntityTypeScope() != null) entity.setEntityTypeScope(reqVO.getEntityTypeScope());
        if (reqVO.getDefaultAttributes() != null) entity.setDefaultAttributes(reqVO.getDefaultAttributes());
        if (reqVO.getDescription() != null) entity.setDescription(reqVO.getDescription());
        if (reqVO.getSortOrder() != null) entity.setSortOrder(reqVO.getSortOrder());
        if (reqVO.getMetadata() != null) entity.setMetadata(reqVO.getMetadata());
        if (reqVO.getStatus() != null) entity.setStatus(reqVO.getStatus());
    }

    private OntEntityCategoryRespVO toEntityCategoryRespVO(OntEntityCategoryDO entity) {
        if (entity == null) return null;
        return OntEntityCategoryRespVO.builder()
                .id(entity.getId())
                .definitionId(entity.getDefinitionId())
                .categoryCode(entity.getCategoryCode())
                .categoryName(entity.getCategoryName())
                .categoryLevel(entity.getCategoryLevel())
                .parentCategoryCode(entity.getParentCategoryCode())
                .entityTypeScope(entity.getEntityTypeScope())
                .defaultAttributes(entity.getDefaultAttributes())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .metadata(entity.getMetadata())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // ==================== Relationship Meta ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRelationshipMeta(OntRelationshipMetaReqVO reqVO) {
        OntRelationshipMetaDO entity = new OntRelationshipMetaDO();
        copyRelationshipMeta(reqVO, entity);
        relationshipMetaMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRelationshipMeta(Long id, OntRelationshipMetaReqVO reqVO) {
        OntRelationshipMetaDO entity = relationshipMetaMapper.selectById(id);
        if (entity == null) throw new RuntimeException("关系元数据不存在: " + id);
        copyRelationshipMeta(reqVO, entity);
        relationshipMetaMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRelationshipMeta(Long id) {
        relationshipMetaMapper.deleteById(id);
    }

    @Override
    public OntRelationshipMetaRespVO getRelationshipMetaById(Long id) {
        return toRelationshipMetaRespVO(relationshipMetaMapper.selectById(id));
    }

    @Override
    public List<OntRelationshipMetaRespVO> listRelationshipMetas(Long definitionId) {
        return relationshipMetaMapper.selectByDefinitionId(definitionId)
                .stream().map(this::toRelationshipMetaRespVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateRelationshipMetas(Long definitionId, List<OntRelationshipMetaReqVO> reqVOs) {
        int count = 0;
        for (OntRelationshipMetaReqVO req : reqVOs) {
            req.setDefinitionId(definitionId);
            createRelationshipMeta(req);
            count++;
        }
        return count;
    }

    private void copyRelationshipMeta(OntRelationshipMetaReqVO reqVO, OntRelationshipMetaDO entity) {
        if (reqVO.getDefinitionId() != null) entity.setDefinitionId(reqVO.getDefinitionId());
        if (reqVO.getRelationshipType() != null) entity.setRelationshipType(reqVO.getRelationshipType());
        if (reqVO.getRelationshipName() != null) entity.setRelationshipName(reqVO.getRelationshipName());
        if (reqVO.getRelationshipNameEn() != null) entity.setRelationshipNameEn(reqVO.getRelationshipNameEn());
        if (reqVO.getSourceEntityTypes() != null) entity.setSourceEntityTypes(reqVO.getSourceEntityTypes());
        if (reqVO.getTargetEntityTypes() != null) entity.setTargetEntityTypes(reqVO.getTargetEntityTypes());
        if (reqVO.getIsDirectional() != null) entity.setIsDirectional(reqVO.getIsDirectional());
        if (reqVO.getIsTransitive() != null) entity.setIsTransitive(reqVO.getIsTransitive());
        if (reqVO.getMultiplicity() != null) entity.setMultiplicity(reqVO.getMultiplicity());
        if (reqVO.getDefaultWeight() != null) entity.setDefaultWeight(reqVO.getDefaultWeight());
        if (reqVO.getValidityPeriod() != null) entity.setValidityPeriod(reqVO.getValidityPeriod());
        if (reqVO.getDescription() != null) entity.setDescription(reqVO.getDescription());
        if (reqVO.getExampleCypher() != null) entity.setExampleCypher(reqVO.getExampleCypher());
        if (reqVO.getSortOrder() != null) entity.setSortOrder(reqVO.getSortOrder());
        if (reqVO.getMetadata() != null) entity.setMetadata(reqVO.getMetadata());
        if (reqVO.getStatus() != null) entity.setStatus(reqVO.getStatus());
    }

    private OntRelationshipMetaRespVO toRelationshipMetaRespVO(OntRelationshipMetaDO entity) {
        if (entity == null) return null;
        return OntRelationshipMetaRespVO.builder()
                .id(entity.getId())
                .definitionId(entity.getDefinitionId())
                .relationshipType(entity.getRelationshipType())
                .relationshipName(entity.getRelationshipName())
                .relationshipNameEn(entity.getRelationshipNameEn())
                .sourceEntityTypes(entity.getSourceEntityTypes())
                .targetEntityTypes(entity.getTargetEntityTypes())
                .isDirectional(entity.getIsDirectional())
                .isTransitive(entity.getIsTransitive())
                .multiplicity(entity.getMultiplicity())
                .defaultWeight(entity.getDefaultWeight())
                .validityPeriod(entity.getValidityPeriod())
                .description(entity.getDescription())
                .exampleCypher(entity.getExampleCypher())
                .sortOrder(entity.getSortOrder())
                .metadata(entity.getMetadata())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
