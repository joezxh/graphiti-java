package com.ontograph.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ontograph.common.exception.BusinessException;
import com.ontograph.system.dal.dataobject.MenuDO;
import com.ontograph.system.dal.mysql.MenuMapper;
import com.ontograph.system.service.MenuService;
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
        MenuDO menuDO = new MenuDO();
        menuDO.setId(menuId);
        menuDO.setDeleted(true);
        menuDO.setUpdateTime(LocalDateTime.now());
        menuMapper.updateById(menuDO);
        log.info("删除菜单成功：menuId={}", menuId);
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
