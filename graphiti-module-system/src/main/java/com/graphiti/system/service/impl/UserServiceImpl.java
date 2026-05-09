package com.graphiti.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.system.dal.dataobject.UserDO;
import com.graphiti.system.dal.mysql.UserMapper;
import com.graphiti.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public Long createUser(UserDO userDO) {
        // 检查用户名是否已存在
        UserDO existingUser = getUserByUsername(userDO.getUsername());
        if (existingUser != null) {
            throw new BusinessException(2001, "用户名已存在");
        }

        // 加密密码
        if (userDO.getPassword() != null) {
            userDO.setPassword(passwordEncoder.encode(userDO.getPassword()));
        }

        userDO.setCreateTime(LocalDateTime.now());
        userDO.setUpdateTime(LocalDateTime.now());
        userDO.setDeleted(false);

        userMapper.insert(userDO);
        log.info("创建用户成功：userId={}, username={}", userDO.getId(), userDO.getUsername());
        return userDO.getId();
    }

    @Override
    public void updateUser(UserDO userDO) {
        userDO.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(userDO);
        log.info("更新用户成功：userId={}", userDO.getId());
    }

    @Override
    public void deleteUser(Long userId) {
        UserDO userDO = new UserDO();
        userDO.setId(userId);
        userDO.setDeleted(true);
        userDO.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(userDO);
        log.info("删除用户成功：userId={}", userId);
    }

    @Override
    public UserDO getUser(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public UserDO getUserByUsername(String username) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, username)
                .eq(UserDO::getDeleted, false)
        );
    }
}
