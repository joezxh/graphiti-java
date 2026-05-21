package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntConstraintDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntConstraintMapper extends BaseMapper<OntConstraintDO> {

    @Select("SELECT * FROM ont_constraint WHERE definition_id = #{definitionId}")
    List<OntConstraintDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_constraint WHERE class_id = #{classId}")
    List<OntConstraintDO> selectByClassId(@Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM ont_constraint WHERE definition_id = #{definitionId}")
    long countByDefinitionId(@Param("definitionId") Long definitionId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ont_constraint WHERE definition_id = #{definitionId}")
    int deleteByDefinitionId(@Param("definitionId") Long definitionId);
}
