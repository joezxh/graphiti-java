package com.ontograph.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ontograph.common.exception.BusinessException;
import com.ontograph.module.system.dal.dataobject.MenuDO;
import com.ontograph.module.system.dal.mysql.MenuMapper;
import com.ontograph.module.system.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;

    @Override
    public Long createMenu(MenuDO menuDO) {
        // 检查权限标识是否已存在
        MenuDO existingMenu = getMenuByPermission(menuDO.getPermission());
        if (existingMenu != null) {
            throw new BusinessException(2003, "权限标识已存在");
        }

        menuDO.setCreateTime(LocalDateTime.now());
        menuDO.setUpdateTime(LocalDateTime.now());
        menuDO.setDeleted(false);

        menuMapper.insert(menuDO);
        log.info("创建菜单成功：menuId={}, name={}", menuDO.getId(), menuDO.getName());
        return menuDO.getId();
    }

    @Override
    public void updateMenu(MenuDO menuDO) {
        menuDO.setUpdateTime(LocalDateTime.now());
        menuMapper.updateById(menuDO);
        log.info("更新菜单成功：menuId={}", menuDO.getId());
    }

    @Override
    public void deleteMenu(Long menuId) {
        // 检查菜单是否存在
        MenuDO menu = menuMapper.selectById(menuId);
        if (menu == null) {
            throw new BusinessException(3001, "菜单不存在");
        }
        
        // P0 修复: 检查是否有子菜单
        long childCount = menuMapper.countByParentId(menuId);
        if (childCount > 0) {
            throw new BusinessException(3002, "该菜单下存在 " + childCount + " 个子菜单，无法删除");
        }
        
        // 使用 MyBatis-Plus 的逻辑删除(会自动设置 deleted=true)
        menuMapper.deleteById(menuId);
        log.info("删除菜单成功：menuId={}, name={}", menuId, menu.getName());
    }

    @Override
    public MenuDO getMenu(Long menuId) {
        return menuMapper.selectById(menuId);
    }

    @Override
    public MenuDO getMenuByPermission(String permission) {
        return menuMapper.selectList(
            new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getPermission, permission)
                .eq(MenuDO::getDeleted, false)
        ).stream().findFirst().orElse(null);
    }

    @Override
    public List<MenuDO> listMenus() {
        return menuMapper.selectList(
            new LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getDeleted, false)
                .orderByAsc(MenuDO::getSort)
        );
    }
}
