package com.ontograph.module.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.module.system.dal.dataobject.MenuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 系统菜单 Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<MenuDO> {
    
    /**
     * 统计指定父菜单的子菜单数量
     * @param parentId 父菜单ID
     * @return 子菜单数量
     */
    @Select("SELECT COUNT(*) FROM sys_menu WHERE parent_id = #{parentId} AND deleted = false")
    long countByParentId(@Param("parentId") Long parentId);
}
