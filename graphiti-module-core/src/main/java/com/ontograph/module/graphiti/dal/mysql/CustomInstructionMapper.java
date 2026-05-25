package com.graphiti.module.graphiti.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.CustomInstructionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CustomInstructionMapper extends BaseMapper<CustomInstructionDO> {

    @Select("SELECT * FROM custom_instruction WHERE graph_id = #{graphId} OR graph_id IS NULL ORDER BY created_at DESC")
    List<CustomInstructionDO> selectByGraphId(@Param("graphId") String graphId);

    @Select("SELECT * FROM custom_instruction WHERE graph_id IS NULL ORDER BY created_at DESC")
    List<CustomInstructionDO> selectGlobal();
}
