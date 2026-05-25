package com.graphiti.module.graphiti.service.metadata;

import com.graphiti.module.graphiti.vo.metadata.*;
import java.util.List;

/**
 * 本体元数据服务接口
 * 统一管理 ont_community_type、ont_episode_type、ont_entity_category、ont_relationship_meta 四张元数据表
 */
public interface OntMetadataService {

    // ==================== Community Type ====================
    Long createCommunityType(OntCommunityTypeReqVO reqVO);
    void updateCommunityType(Long id, OntCommunityTypeReqVO reqVO);
    void deleteCommunityType(Long id);
    OntCommunityTypeRespVO getCommunityTypeById(Long id);
    List<OntCommunityTypeRespVO> listCommunityTypes(Long definitionId);
    List<OntCommunityTypeRespVO> listCommunityTypesByCategory(Long definitionId, String category);
    List<OntCommunityTypeRespVO> getCommunityTypeTree(Long definitionId);

    // ==================== Episode Type ====================
    Long createEpisodeType(OntEpisodeTypeReqVO reqVO);
    void updateEpisodeType(Long id, OntEpisodeTypeReqVO reqVO);
    void deleteEpisodeType(Long id);
    void deleteEpisodeType(String graphId, Long id);
    OntEpisodeTypeRespVO getEpisodeTypeById(Long id);
    List<OntEpisodeTypeRespVO> listEpisodeTypes(Long definitionId);
    List<OntEpisodeTypeRespVO> listEpisodeTypesByProcessType(Long definitionId, String processType);
    List<OntEpisodeTypeRespVO> getEpisodeTypeTree(Long definitionId);
    EpisodeTypeDeleteCheckVO checkDeleteEpisodeType(String graphId, Long id);
    void reorderEpisodeTypes(List<EpisodeTypeReorderItemVO> items);
    EpisodeTypeImportResultVO importEpisodeTypes(Long definitionId, List<OntEpisodeTypeReqVO> items);
    int batchCreateEpisodeTypes(Long definitionId, List<OntEpisodeTypeReqVO> reqVOs);

    // ==================== Entity Category ====================
    Long createEntityCategory(OntEntityCategoryReqVO reqVO);
    void updateEntityCategory(Long id, OntEntityCategoryReqVO reqVO);
    void deleteEntityCategory(Long id);
    OntEntityCategoryRespVO getEntityCategoryById(Long id);
    List<OntEntityCategoryRespVO> listEntityCategories(Long definitionId);
    List<OntEntityCategoryRespVO> getEntityCategoryTree(Long definitionId);

    // ==================== Relationship Meta ====================
    Long createRelationshipMeta(OntRelationshipMetaReqVO reqVO);
    void updateRelationshipMeta(Long id, OntRelationshipMetaReqVO reqVO);
    void deleteRelationshipMeta(Long id);
    OntRelationshipMetaRespVO getRelationshipMetaById(Long id);
    List<OntRelationshipMetaRespVO> listRelationshipMetas(Long definitionId);
    int batchCreateRelationshipMetas(Long definitionId, List<OntRelationshipMetaReqVO> reqVOs);
}
