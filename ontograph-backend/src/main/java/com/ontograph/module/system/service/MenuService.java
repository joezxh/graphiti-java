package com.ontograph.module.system.service;

import com.ontograph.module.system.dal.dataobject.MenuDO;

/**
 * 菜单管理服务接口
 */
public interface MenuService {

    /**
     * 创建菜单
     * @param menuDO 菜单信息
     * @return 菜单ID
     */
    Long createMenu(MenuDO menuDO);

    /**
     * 更新菜单
     * @param menuDO 菜单信息
     */
    void updateMenu(MenuDO menuDO);

    /**
     * 删除菜单
     * @param menuId 菜单ID
     */
    void deleteMenu(Long menuId);

    /**
     * 获取菜单详情
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    MenuDO getMenu(Long menuId);

    /**
     * 根据权限标识获取菜单
     * @param permission 权限标识
     * @return 菜单信息
     */
    MenuDO getMenuByPermission(String permission);

    /**
     * 获取所有菜单列表（不分页）
     * @return 菜单列表
     */
    java.util.List<MenuDO> listMenus();
}
