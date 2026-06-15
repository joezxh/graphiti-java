package com.ontograph.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ontograph.common.exception.BusinessException;
import com.ontograph.system.dal.dataobject.RoleDO;
import com.ontograph.system.dal.mysql.RoleMapper;
import com.ontograph.system.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    @Override
    public Long createRole(RoleDO roleDO) {
        // 检查角色编码是否已存在
        RoleDO existingRole = getRoleByCode(roleDO.getCode());
        if (existingRole != null) {
            throw new BusinessException(2002, "角色编码已存在");
        }

        roleDO.setCreateTime(LocalDateTime.now());
        roleDO.setUpdateTime(LocalDateTime.now());
        roleDO.setDeleted(false);

        roleMapper.insert(roleDO);
        log.info("创建角色成功：roleId={}, name={}", roleDO.getId(), roleDO.getName());
        return roleDO.getId();
    }

    @Override
    public void updateRole(RoleDO roleDO) {
        roleDO.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(roleDO);
        log.info("更新角色成功：roleId={}", roleDO.getId());
    }

    @Override
    public void deleteRole(Long roleId) {
        RoleDO roleDO = new RoleDO();
        roleDO.setId(roleId);
        roleDO.setDeleted(true);
        roleDO.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(roleDO);
        log.info("删除角色成功：roleId={}", roleId);
    }

    @Override
    public RoleDO getRole(Long roleId) {
        return roleMapper.selectById(roleId);
    }

    @Override
    public RoleDO getRoleByCode(String code) {
        return roleMapper.selectList(
            new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getCode, code)
                .eq(RoleDO::getDeleted, false)
        ).stream().findFirst().orElse(null);
    }

    @Override
    public List<RoleDO> listRoles() {
        return roleMapper.selectList(
            new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getDeleted, false)
                .orderByDesc(RoleDO::getCreateTime)
        );
    }
}
