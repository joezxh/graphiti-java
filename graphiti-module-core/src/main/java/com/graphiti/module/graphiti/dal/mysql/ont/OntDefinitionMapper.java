package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OntDefinitionMapper extends BaseMapper<OntDefinitionDO> {
    
    /**
     * 根据 graphId 查询本体定义
     * @param graphId 图ID
     * @return 本体定义对象
     */
    @Select("SELECT * FROM ont_definition WHERE graph_id = #{graphId} LIMIT 1")
    OntDefinitionDO selectByGraphId(@Param("graphId") String graphId);
}
