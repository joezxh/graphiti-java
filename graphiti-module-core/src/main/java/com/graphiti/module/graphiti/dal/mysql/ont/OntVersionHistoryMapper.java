package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntVersionHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntVersionHistoryMapper extends BaseMapper<OntVersionHistoryDO> {

    @Select("SELECT * FROM ont_version_history WHERE definition_id = #{definitionId} ORDER BY changed_at DESC")
    List<OntVersionHistoryDO> selectByDefinitionId(@Param("definitionId") Long definitionId);
}
