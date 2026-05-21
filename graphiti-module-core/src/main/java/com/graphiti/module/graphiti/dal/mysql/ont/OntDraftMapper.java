package com.graphiti.module.graphiti.dal.mysql.ont;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.ont.OntDraftDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface OntDraftMapper extends BaseMapper<OntDraftDO> {

    @Select("SELECT * FROM ont_draft WHERE graph_id = #{graphId} ORDER BY created_at DESC")
    List<OntDraftDO> selectByGraphId(@Param("graphId") String graphId);

    @Select("SELECT * FROM ont_draft WHERE graph_id = #{graphId} AND status = #{status} ORDER BY created_at DESC")
    List<OntDraftDO> selectByGraphIdAndStatus(@Param("graphId") String graphId, @Param("status") String status);

    @Select("SELECT * FROM ont_draft WHERE graph_id = #{graphId} AND draft_type = #{draftType} ORDER BY created_at DESC")
    List<OntDraftDO> selectByGraphIdAndDraftType(@Param("graphId") String graphId, @Param("draftType") String draftType);

    @Select("SELECT COUNT(*) FROM ont_draft WHERE graph_id = #{graphId}")
    long countByGraphId(@Param("graphId") String graphId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM ont_draft WHERE graph_id = #{graphId}")
    int deleteByGraphId(@Param("graphId") String graphId);
}
