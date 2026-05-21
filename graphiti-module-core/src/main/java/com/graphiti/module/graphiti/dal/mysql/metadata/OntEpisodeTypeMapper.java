package com.graphiti.module.graphiti.dal.mysql.metadata;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntEpisodeTypeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntEpisodeTypeMapper extends BaseMapper<OntEpisodeTypeDO> {

    @Select("SELECT id, definition_id, type_code, type_name, type_name_en, " +
            "process_type, stage_label, stage_level, is_review_stage, " +
            "legal_process, court_level, is_trial_stage, " +
            "description, sort_order, metadata, status, created_at, updated_at " +
            "FROM ont_episode_type WHERE definition_id = #{definitionId} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEpisodeTypeDO> selectActiveByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} ORDER BY sort_order")
    List<OntEpisodeTypeDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} AND legal_process = #{legalProcess} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEpisodeTypeDO> selectByLegalProcess(@Param("definitionId") Long definitionId, @Param("legalProcess") String legalProcess);

    @Select("SELECT id, definition_id, type_code, type_name, type_name_en, " +
            "process_type, stage_label, stage_level, is_review_stage, " +
            "legal_process, court_level, is_trial_stage, " +
            "description, sort_order, metadata, status, created_at, updated_at " +
            "FROM ont_episode_type WHERE definition_id = #{definitionId} AND process_type = #{processType} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEpisodeTypeDO> selectByProcessType(@Param("definitionId") Long definitionId, @Param("processType") String processType);

    @Select("SELECT id, definition_id, type_code, type_name, type_name_en, " +
            "process_type, stage_label, stage_level, is_review_stage, " +
            "legal_process, court_level, is_trial_stage, " +
            "description, sort_order, metadata, status, created_at, updated_at " +
            "FROM ont_episode_type WHERE definition_id = #{definitionId} AND type_code = #{typeCode} LIMIT 1")
    OntEpisodeTypeDO selectByTypeCode(@Param("definitionId") Long definitionId, @Param("typeCode") String typeCode);
}
