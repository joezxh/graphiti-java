package com.graphiti.module.graphiti.controller.admin;

import com.graphiti.common.response.CommonResult;
import com.graphiti.module.graphiti.dal.mysql.metadata.OntEpisodeTypeMapper;
import com.graphiti.module.graphiti.service.EpisodeService;
import com.graphiti.module.graphiti.service.metadata.OntMetadataService;
import com.graphiti.module.graphiti.vo.episode.EpisodeListRespVO;
import com.graphiti.module.graphiti.vo.metadata.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 本体元数据管理控制器
 * 提供 ont_community_type、ont_episode_type、ont_entity_category、ont_relationship_meta 四张元数据表的 CRUD 接口
 */
@Tag(name = "本体元数据管理", description = "本体元数据（社区类型、剧集类型、实体分类、关系元数据）的增删改查管理")
@RestController
@RequestMapping("/api/v1/ontology/{graphId}")
@RequiredArgsConstructor
@Slf4j
public class OntMetadataController {

    private final OntMetadataService ontMetadataService;
    private final EpisodeService episodeService;
    private final OntEpisodeTypeMapper episodeTypeMapper;

    // ==================== Community Type ====================

    @GetMapping("/community-types")
    @Operation(summary = "获取社区类型列表", description = "获取指定图谱的本体定义下的所有社区类型")
    public CommonResult<List<OntCommunityTypeRespVO>> listCommunityTypes(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId,
            @RequestParam(required = false) @Parameter(description = "分类维度") String category) {
        if (category != null && !category.isBlank()) {
            return CommonResult.success(ontMetadataService.listCommunityTypesByCategory(definitionId, category));
        }
        return CommonResult.success(ontMetadataService.listCommunityTypes(definitionId));
    }

    @GetMapping("/community-types/tree")
    @Operation(summary = "获取社区类型树", description = "获取社区类型的层级树结构")
    public CommonResult<List<OntCommunityTypeRespVO>> getCommunityTypeTree(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId) {
        return CommonResult.success(ontMetadataService.getCommunityTypeTree(definitionId));
    }

    @GetMapping("/community-types/{id}")
    @Operation(summary = "获取社区类型详情")
    public CommonResult<OntCommunityTypeRespVO> getCommunityType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id) {
        return CommonResult.success(ontMetadataService.getCommunityTypeById(id));
    }

    @PostMapping("/community-types")
    @Operation(summary = "创建社区类型")
    public CommonResult<Long> createCommunityType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid OntCommunityTypeReqVO reqVO) {
        return CommonResult.success(ontMetadataService.createCommunityType(reqVO));
    }

    @PutMapping("/community-types/{id}")
    @Operation(summary = "更新社区类型")
    public CommonResult<Void> updateCommunityType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id,
            @RequestBody @Valid OntCommunityTypeReqVO reqVO) {
        ontMetadataService.updateCommunityType(id, reqVO);
        return CommonResult.success();
    }

    @DeleteMapping("/community-types/{id}")
    @Operation(summary = "删除社区类型")
    public CommonResult<Void> deleteCommunityType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id) {
        ontMetadataService.deleteCommunityType(id);
        return CommonResult.success();
    }

    // ==================== Episode Type ====================

    @GetMapping("/episode-types")
    @Operation(summary = "获取剧集类型列表", description = "获取指定图谱的本体定义下的所有剧集类型")
    public CommonResult<List<OntEpisodeTypeRespVO>> listEpisodeTypes(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId,
            @RequestParam(required = false) @Parameter(description = "业务流程类型") String processType) {
        if (processType != null && !processType.isBlank()) {
            return CommonResult.success(ontMetadataService.listEpisodeTypesByProcessType(definitionId, processType));
        }
        return CommonResult.success(ontMetadataService.listEpisodeTypes(definitionId));
    }

    @GetMapping("/episode-types/tree")
    @Operation(summary = "获取剧集类型树", description = "获取层级化的剧集类型树")
    public CommonResult<List<OntEpisodeTypeRespVO>> getEpisodeTypeTree(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId) {
        return CommonResult.success(ontMetadataService.getEpisodeTypeTree(definitionId));
    }

    @GetMapping("/episode-types/{id}")
    @Operation(summary = "获取剧集类型详情")
    public CommonResult<OntEpisodeTypeRespVO> getEpisodeType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id) {
        OntEpisodeTypeRespVO vo = ontMetadataService.getEpisodeTypeById(id);
        if (vo != null) {
            vo.setInstanceCount(episodeTypeMapper.countEpisodeInstances(graphId, vo.getTypeCode()));
        }
        return CommonResult.success(vo);
    }

    @PostMapping("/episode-types")
    @Operation(summary = "创建剧集类型")
    public CommonResult<Long> createEpisodeType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid OntEpisodeTypeReqVO reqVO) {
        return CommonResult.success(ontMetadataService.createEpisodeType(reqVO));
    }

    @PostMapping("/episode-types/batch")
    @Operation(summary = "批量创建剧集类型")
    public CommonResult<Integer> batchCreateEpisodeTypes(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId,
            @RequestBody @Valid List<OntEpisodeTypeReqVO> reqVOs) {
        return CommonResult.success(ontMetadataService.batchCreateEpisodeTypes(definitionId, reqVOs));
    }

    @PutMapping("/episode-types/{id}")
    @Operation(summary = "更新剧集类型")
    public CommonResult<Void> updateEpisodeType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id,
            @RequestBody @Valid OntEpisodeTypeReqVO reqVO) {
        ontMetadataService.updateEpisodeType(id, reqVO);
        return CommonResult.success();
    }

    @GetMapping("/episode-types/{id}/delete-check")
    @Operation(summary = "检查剧集类型是否可以删除")
    public CommonResult<EpisodeTypeDeleteCheckVO> checkDeleteEpisodeType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id) {
        return CommonResult.success(ontMetadataService.checkDeleteEpisodeType(graphId, id));
    }

    @DeleteMapping("/episode-types/{id}")
    @Operation(summary = "删除剧集类型")
    public CommonResult<Void> deleteEpisodeType(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id) {
        ontMetadataService.deleteEpisodeType(graphId, id);
        return CommonResult.success();
    }

    @PostMapping("/episode-types/reorder")
    @Operation(summary = "批量更新剧集类型排序")
    public CommonResult<Void> reorderEpisodeTypes(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid List<EpisodeTypeReorderItemVO> items) {
        ontMetadataService.reorderEpisodeTypes(items);
        return CommonResult.success();
    }

    @PostMapping("/episode-types/import")
    @Operation(summary = "批量导入剧集类型")
    public CommonResult<EpisodeTypeImportResultVO> importEpisodeTypes(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId,
            @RequestBody @Valid List<OntEpisodeTypeReqVO> items) {
        return CommonResult.success(ontMetadataService.importEpisodeTypes(definitionId, items));
    }

    @GetMapping("/episode-types/{id}/instances")
    @Operation(summary = "获取剧集类型下的实例列表")
    public CommonResult<EpisodeListRespVO> getEpisodeTypeInstances(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "类型ID") Long id,
            @RequestParam(defaultValue = "1") @Parameter(description = "页码") Integer page,
            @RequestParam(defaultValue = "20") @Parameter(description = "每页数量") Integer pageSize,
            @RequestParam(required = false) @Parameter(description = "搜索关键词") String keyword) {
        // TODO: 按类型过滤的实例列表查询，暂用全量列表
        EpisodeListRespVO result = episodeService.listEpisodes(graphId, pageSize, (page - 1) * pageSize);
        return CommonResult.success(result);
    }

    // ==================== Entity Category ====================

    @GetMapping("/entity-categories")
    @Operation(summary = "获取实体分类列表", description = "获取指定图谱的本体定义下的所有实体分类")
    public CommonResult<List<OntEntityCategoryRespVO>> listEntityCategories(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId) {
        return CommonResult.success(ontMetadataService.listEntityCategories(definitionId));
    }

    @GetMapping("/entity-categories/tree")
    @Operation(summary = "获取实体分类树", description = "获取实体分类的层级树结构")
    public CommonResult<List<OntEntityCategoryRespVO>> getEntityCategoryTree(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId) {
        return CommonResult.success(ontMetadataService.getEntityCategoryTree(definitionId));
    }

    @GetMapping("/entity-categories/{id}")
    @Operation(summary = "获取实体分类详情")
    public CommonResult<OntEntityCategoryRespVO> getEntityCategory(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "分类ID") Long id) {
        return CommonResult.success(ontMetadataService.getEntityCategoryById(id));
    }

    @PostMapping("/entity-categories")
    @Operation(summary = "创建实体分类")
    public CommonResult<Long> createEntityCategory(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid OntEntityCategoryReqVO reqVO) {
        return CommonResult.success(ontMetadataService.createEntityCategory(reqVO));
    }

    @PutMapping("/entity-categories/{id}")
    @Operation(summary = "更新实体分类")
    public CommonResult<Void> updateEntityCategory(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "分类ID") Long id,
            @RequestBody @Valid OntEntityCategoryReqVO reqVO) {
        ontMetadataService.updateEntityCategory(id, reqVO);
        return CommonResult.success();
    }

    @DeleteMapping("/entity-categories/{id}")
    @Operation(summary = "删除实体分类")
    public CommonResult<Void> deleteEntityCategory(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "分类ID") Long id) {
        ontMetadataService.deleteEntityCategory(id);
        return CommonResult.success();
    }

    // ==================== Relationship Meta ====================

    @GetMapping("/relationship-meta")
    @Operation(summary = "获取关系元数据列表", description = "获取指定图谱的本体定义下的所有关系元数据")
    public CommonResult<List<OntRelationshipMetaRespVO>> listRelationshipMetas(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId) {
        return CommonResult.success(ontMetadataService.listRelationshipMetas(definitionId));
    }

    @GetMapping("/relationship-meta/{id}")
    @Operation(summary = "获取关系元数据详情")
    public CommonResult<OntRelationshipMetaRespVO> getRelationshipMeta(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "元数据ID") Long id) {
        return CommonResult.success(ontMetadataService.getRelationshipMetaById(id));
    }

    @PostMapping("/relationship-meta")
    @Operation(summary = "创建关系元数据")
    public CommonResult<Long> createRelationshipMeta(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestBody @Valid OntRelationshipMetaReqVO reqVO) {
        return CommonResult.success(ontMetadataService.createRelationshipMeta(reqVO));
    }

    @PostMapping("/relationship-meta/batch")
    @Operation(summary = "批量创建关系元数据")
    public CommonResult<Integer> batchCreateRelationshipMetas(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @RequestParam @Parameter(description = "本体定义ID") Long definitionId,
            @RequestBody @Valid List<OntRelationshipMetaReqVO> reqVOs) {
        return CommonResult.success(ontMetadataService.batchCreateRelationshipMetas(definitionId, reqVOs));
    }

    @PutMapping("/relationship-meta/{id}")
    @Operation(summary = "更新关系元数据")
    public CommonResult<Void> updateRelationshipMeta(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "元数据ID") Long id,
            @RequestBody @Valid OntRelationshipMetaReqVO reqVO) {
        ontMetadataService.updateRelationshipMeta(id, reqVO);
        return CommonResult.success();
    }

    @DeleteMapping("/relationship-meta/{id}")
    @Operation(summary = "删除关系元数据")
    public CommonResult<Void> deleteRelationshipMeta(
            @PathVariable @Parameter(description = "图谱ID") String graphId,
            @PathVariable @Parameter(description = "元数据ID") Long id) {
        ontMetadataService.deleteRelationshipMeta(id);
        return CommonResult.success();
    }
}
