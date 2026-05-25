package com.ontograph.module.graphiti.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.module.graphiti.dal.dataobject.GraphMetadataDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 图谱元数据 Mapper
 * 对应表：graphiti_graph_metadata
 */
@Mapper
public interface GraphMetadataMapper extends BaseMapper<GraphMetadataDO> {

    @Select("SELECT * FROM graphiti_graph_metadata WHERE graph_id = #{graphId} AND deleted = false LIMIT 1")
    GraphMetadataDO selectByGraphId(@Param("graphId") String graphId);
}
