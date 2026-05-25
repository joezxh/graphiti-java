package com.graphiti.module.graphiti.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.graphiti.module.graphiti.dal.dataobject.PromptVariableDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 提示词变量 Mapper
 */
@Mapper
public interface PromptVariableMapper extends BaseMapper<PromptVariableDO> {

    /**
     * 根据模板ID查询变量列表
     */
    @Select("SELECT * FROM prompt_variable WHERE template_id = #{templateId} ORDER BY sort ASC")
    List<PromptVariableDO> selectByTemplateId(@Param("templateId") Long templateId);

    /**
     * 查询模板必需的变量
     */
    @Select("SELECT * FROM prompt_variable WHERE template_id = #{templateId} AND required = true ORDER BY sort ASC")
    List<PromptVariableDO> selectRequiredByTemplateId(@Param("templateId") Long templateId);

    /**
     * 根据模板ID删除变量
     */
    @Delete("DELETE FROM prompt_variable WHERE template_id = #{templateId}")
    int deleteByTemplateId(@Param("templateId") Long templateId);
}
