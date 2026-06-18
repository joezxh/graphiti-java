package com.ontograph.module.system.service.impl;

import com.ontograph.common.exception.BusinessException;
import com.ontograph.framework.security.jwt.JwtTokenProvider;
import com.ontograph.module.system.dto.LoginRequest;
import com.ontograph.module.system.dto.LoginResponse;
import com.ontograph.module.system.service.AuthService;
import com.ontograph.module.system.dal.dataobject.MenuDO;
import com.ontograph.module.system.dal.dataobject.UserDO;
import com.ontograph.module.system.dal.mysql.MenuMapper;
import com.ontograph.module.system.dal.mysql.UserMapper;
import com.ontograph.module.system.dal.mysql.RoleMenuMapper;
import com.ontograph.module.system.dal.mysql.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 认证服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final MenuMapper menuMapper;
    private final UserMapper userMapper;
    private final RoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;
    
    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("[AuthService] 登录请求 - username: {}, password: {}", request.getUsername(), request.getPassword());
        // 认证用户名和密码
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword())
        );
        
        // 生成 JWT Token
        String token = jwtTokenProvider.generateToken(auth);
        
        // 查询用户详细信息
        UserDO user = userMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, request.getUsername())
                .eq(UserDO::getDeleted, false)
        ).stream().findFirst().orElse(null);
        
        // 构造响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn(86400L); // 24小时
        if (user != null) {
            LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
            userInfo.setUsername(user.getUsername());
            userInfo.setNickname(user.getNickname());
            userInfo.setEmail(user.getEmail());
            response.setUserInfo(userInfo);
        }
        return response;
    }
    
    @Override
    public LoginResponse.UserInfo getUserInfo() {
        // 从 Security 上下文获取认证信息
        Authentication auth = SecurityContextHolder.getContext()
                                  .getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(401, "未认证");
        }
        
        // 构造用户信息
        LoginResponse.UserInfo info = new LoginResponse.UserInfo();
        info.setUsername(auth.getName());
        
        // 查询数据库补充昵称等信息
        UserDO user = userMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, auth.getName())
                .eq(UserDO::getDeleted, false)
        ).stream().findFirst().orElse(null);
        if (user != null) {
            info.setNickname(user.getNickname());
            info.setEmail(user.getEmail());
        }
        return info;
    }
    
    @Override
    public void logout() {
        // 清除 Security 上下文（JWT 无状态，客户端需删除 Token）
        SecurityContextHolder.clearContext();
    }

    @Override
    public List<MenuDO> getUserMenus() {
        // 从 Security 上下文获取认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(401, "未认证");
        }

        String username = auth.getName();
        log.info("[AuthService] 获取用户菜单 - username: {}", username);

        // 获取用户信息
        UserDO user = userMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, username)
                .eq(UserDO::getDeleted, false)
        ).stream().findFirst().orElseThrow(() -> new BusinessException(404, "用户不存在"));

        // 获取用户关联的角色
        List<Long> roleIds = getUserRoleIds(user.getId());

        // 如果是超级管理员（role_id = 1），返回所有启用的菜单
        if (roleIds.contains(1L)) {
            log.info("[AuthService] 用户 {} 是超级管理员，返回所有菜单", username);
            List<MenuDO> allMenus = menuMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MenuDO>()
                    .eq(MenuDO::getDeleted, false)
                    .eq(MenuDO::getStatus, 1)
                    .orderByAsc(MenuDO::getSort)
            );
            return buildMenuTree(allMenus, 0L);
        }

        // 普通用户：获取其角色关联的所有菜单
        Set<Long> menuIds = new HashSet<>();
        for (Long roleId : roleIds) {
            List<Long> roleMenuIds = roleMenuMapper.selectMenuIdsByRoleId(roleId);
            menuIds.addAll(roleMenuIds);
        }

        if (menuIds.isEmpty()) {
            log.info("[AuthService] 用户 {} 没有任何菜单权限", username);
            return new ArrayList<>();
        }

        // 获取所有启用的菜单并过滤
        List<MenuDO> allMenus = menuMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MenuDO>()
                .eq(MenuDO::getDeleted, false)
                .eq(MenuDO::getStatus, 1)
                .orderByAsc(MenuDO::getSort)
        );

        // 过滤出用户有权限的菜单
        List<MenuDO> userMenus = allMenus.stream()
            .filter(m -> menuIds.contains(m.getId()))
            .collect(Collectors.toList());

        // 构建菜单树
        return buildMenuTree(userMenus, 0L);
    }

    /**
     * 获取用户关联的角色ID列表
     */
    private List<Long> getUserRoleIds(Long userId) {
        // 从 sys_user_role 表查询用户关联的角色
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds != null && !roleIds.isEmpty()) {
            return roleIds;
        }
        return new ArrayList<>();
    }

    /**
     * 构建菜单树
     */
    private List<MenuDO> buildMenuTree(List<MenuDO> allMenus, Long parentId) {
        return allMenus.stream()
            .filter(m -> m.getParentId() != null && m.getParentId().equals(parentId))
            .peek(m -> m.setChildren(buildMenuTree(allMenus, m.getId())))
            .collect(Collectors.toList());
    }
}
