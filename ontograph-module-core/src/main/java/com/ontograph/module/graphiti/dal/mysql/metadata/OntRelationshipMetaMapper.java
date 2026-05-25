package com.ontograph.module.graphiti.dal.mysql.metadata;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.module.graphiti.dal.dataobject.metadata.OntRelationshipMetaDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntRelationshipMetaMapper extends BaseMapper<OntRelationshipMetaDO> {

    @Select("SELECT * FROM ont_relationship_meta WHERE definition_id = #{definitionId} AND status = 'ACTIVE' ORDER BY sort_order")
    List<OntRelationshipMetaDO> selectActiveByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_relationship_meta WHERE definition_id = #{definitionId} ORDER BY sort_order")
    List<OntRelationshipMetaDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_relationship_meta WHERE definition_id = #{definitionId} AND relationship_type = #{relationshipType} LIMIT 1")
    OntRelationshipMetaDO selectByType(@Param("definitionId") Long definitionId, @Param("relationshipType") String relationshipType);

    @Select("SELECT COUNT(*) FROM ont_relationship_meta WHERE definition_id = #{definitionId}")
    long countByDefinitionId(@Param("definitionId") Long definitionId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ont_relationship_meta WHERE definition_id = #{definitionId}")
    int deleteByDefinitionId(@Param("definitionId") Long definitionId);
}
