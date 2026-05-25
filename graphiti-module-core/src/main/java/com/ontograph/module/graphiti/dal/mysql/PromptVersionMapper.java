package com.ontograph.module.graphiti.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.module.graphiti.dal.dataobject.PromptVersionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Optional;

/**
 * 提示词版本 Mapper
 */
@Mapper
public interface PromptVersionMapper extends BaseMapper<PromptVersionDO> {

    /**
     * 根据模板ID查询最新版本
     */
    @Select("SELECT * FROM prompt_version WHERE template_id = #{templateId} ORDER BY version DESC LIMIT 1")
    Optional<PromptVersionDO> selectLatestByTemplateId(@Param("templateId") Long templateId);

    /**
     * 根据模板ID查询所有版本
     */
    @Select("SELECT * FROM prompt_version WHERE template_id = #{templateId} ORDER BY version DESC")
    List<PromptVersionDO> selectByTemplateId(@Param("templateId") Long templateId);

    /**
     * 根据模板ID和版本号查询
     */
    @Select("SELECT * FROM prompt_version WHERE template_id = #{templateId} AND version = #{version} LIMIT 1")
    Optional<PromptVersionDO> selectByTemplateIdAndVersion(@Param("templateId") Long templateId, @Param("version") Integer version);

    /**
     * 查询模板的活跃版本
     */
    @Select("SELECT * FROM prompt_version WHERE template_id = #{templateId} AND active = true LIMIT 1")
    Optional<PromptVersionDO> selectActiveByTemplateId(@Param("templateId") Long templateId);

    /**
     * 根据模板ID删除版本
     */
    @Delete("DELETE FROM prompt_version WHERE template_id = #{templateId}")
    int deleteByTemplateId(@Param("templateId") Long templateId);
}
