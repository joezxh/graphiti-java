package com.ontograph.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户详情服务实现类
 * 实现 UserDetailsService 接口，用于 Spring Security 认证
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    // 注意：这里为了简化，暂时硬编码用户信息进行测试
    // 实际项目中应该从数据库中查询用户信息
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        log.info("[UserDetailsService] 加载用户: {}", username);
        // TODO：从数据库查询用户信息
        // 这里为了测试，硬编码一个 admin 用户
        if (!"admin".equals(username)) {
            throw new UsernameNotFoundException("用户不存在");
        }
        // 模拟从数据库查询的密码（admin123 的 BCrypt 加密结果）
        String password = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("admin123");
        log.info("[UserDetailsService] 返回密码哈希: {}", password);
        // 模拟用户权限
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        // 返回 UserDetails 实现
        return new User(username, password, authorities);
    }
}
