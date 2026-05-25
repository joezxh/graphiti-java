package com.ontograph.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntMappingDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OntMappingMapper extends BaseMapper<OntMappingDO> {

    @Select("SELECT * FROM ont_mapping WHERE definition_id = #{definitionId} ORDER BY confidence DESC NULLS LAST")
    List<OntMappingDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_mapping WHERE definition_id = #{definitionId} AND source_ontology = #{sourceOntology}")
    List<OntMappingDO> selectBySourceOntology(@Param("definitionId") Long definitionId,
                                              @Param("sourceOntology") String sourceOntology);

    @Select("SELECT COUNT(*) FROM ont_mapping WHERE definition_id = #{definitionId}")
    long countByDefinitionId(@Param("definitionId") Long definitionId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ont_mapping WHERE definition_id = #{definitionId}")
    int deleteByDefinitionId(@Param("definitionId") Long definitionId);
}
