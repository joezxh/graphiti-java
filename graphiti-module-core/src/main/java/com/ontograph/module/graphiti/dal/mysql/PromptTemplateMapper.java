package com.ontograph.module.graphiti.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.module.graphiti.dal.dataobject.PromptTemplateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 提示词模板 Mapper
 */
@Mapper
public interface PromptTemplateMapper extends BaseMapper<PromptTemplateDO> {

    /**
     * 根据类型查询启用的模板
     */
    @Select("SELECT * FROM prompt_template WHERE type = #{type} AND enabled = true ORDER BY sort ASC")
    List<PromptTemplateDO> selectByType(@Param("type") String type);

    /**
     * 根据编码查询模板
     */
    @Select("SELECT * FROM prompt_template WHERE code = #{code} AND enabled = true LIMIT 1")
    PromptTemplateDO selectByCode(@Param("code") String code);

    /**
     * 查询所有启用的模板
     */
    @Select("SELECT * FROM prompt_template WHERE enabled = true ORDER BY type, sort ASC")
    List<PromptTemplateDO> selectAllEnabled();

    /**
     * 根据标签查询模板
     */
    @Select("SELECT * FROM prompt_template WHERE FIND_IN_SET(#{tag}, tags) > 0 AND enabled = true ORDER BY sort ASC")
    List<PromptTemplateDO> selectByTag(@Param("tag") String tag);
}
