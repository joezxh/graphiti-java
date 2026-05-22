package com.graphiti.module.graphiti.dal.mysql.metadata;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.metadata.OntEpisodeTypeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntEpisodeTypeMapper extends BaseMapper<OntEpisodeTypeDO> {

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} ORDER BY sort_order, level")
    List<OntEpisodeTypeDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} AND parent_type_code = #{parentTypeCode} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEpisodeTypeDO> selectByParentTypeCode(@Param("definitionId") Long definitionId, @Param("parentTypeCode") String parentTypeCode);

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} AND parent_type_code IS NULL AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntEpisodeTypeDO> selectRootTypes(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_episode_type WHERE definition_id = #{definitionId} AND type_code = #{typeCode} LIMIT 1")
    OntEpisodeTypeDO selectByTypeCode(@Param("definitionId") Long definitionId, @Param("typeCode") String typeCode);

    @Select("SELECT COUNT(*) FROM ont_episode_type WHERE definition_id = #{definitionId}")
    long countByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT COUNT(*) FROM episode WHERE graph_id = #{graphId} AND episode_type = #{typeCode}")
    long countEpisodeInstances(@Param("graphId") String graphId, @Param("typeCode") String typeCode);

    int batchUpdateSortOrder(@Param("list") List<OntEpisodeTypeDO> types);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ont_episode_type WHERE definition_id = #{definitionId}")
    int deleteByDefinitionId(@Param("definitionId") Long definitionId);
}
