package com.ontograph.module.system.dal.mysql;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ontograph.module.system.dal.dataobject.MenuDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统菜单 Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<MenuDO> {
}
