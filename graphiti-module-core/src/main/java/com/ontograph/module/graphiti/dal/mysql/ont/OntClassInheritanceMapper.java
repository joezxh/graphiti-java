package com.ontograph.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntClassInheritanceDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OntClassInheritanceMapper extends BaseMapper<OntClassInheritanceDO> {

    @Select("SELECT * FROM ont_class_inheritance WHERE definition_id = #{definitionId}")
    List<OntClassInheritanceDO> selectByDefinitionId(@Param("definitionId") Long definitionId);

    @Select("SELECT * FROM ont_class_inheritance WHERE class_id = #{classId}")
    List<OntClassInheritanceDO> selectByClassId(@Param("classId") Long classId);

    @Select("SELECT * FROM ont_class_inheritance WHERE parent_class_id = #{parentClassId}")
    List<OntClassInheritanceDO> selectByParentClassId(@Param("parentClassId") Long parentClassId);

    @Select("SELECT COUNT(*) FROM ont_class_inheritance WHERE definition_id = #{definitionId}")
    long countByDefinitionId(@Param("definitionId") Long definitionId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ont_class_inheritance WHERE definition_id = #{definitionId}")
    int deleteByDefinitionId(@Param("definitionId") Long definitionId);
}
