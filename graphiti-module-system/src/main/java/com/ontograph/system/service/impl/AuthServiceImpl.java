package com.graphiti.system.service.impl;

import com.graphiti.common.exception.BusinessException;
import com.graphiti.framework.security.jwt.JwtTokenProvider;
import com.graphiti.system.dto.LoginRequest;
import com.graphiti.system.dto.LoginResponse;
import com.graphiti.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    
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
        
        // 构造响应
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setExpiresIn(86400L); // 24小时
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
        return info;
    }
    
    @Override
    public void logout() {
        // 清除 Security 上下文（JWT 无状态，客户端需删除 Token）
        SecurityContextHolder.clearContext();
    }
}
