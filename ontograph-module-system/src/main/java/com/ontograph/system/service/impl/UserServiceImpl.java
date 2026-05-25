package com.ontograph.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ontograph.common.exception.BusinessException;
import com.ontograph.system.dal.dataobject.UserDO;
import com.ontograph.system.dal.mysql.UserMapper;
import com.ontograph.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Override
    public Long getUserIdByUsername(String username) {
        UserDO user = getUserByUsername(username);
        return user != null ? user.getId() : null;
    }

    @Override
    public Map<String, Object> listUsers(Integer pageNo, Integer pageSize, String username, String nickname, Integer status) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getDeleted, false);
        if (username != null && !username.isBlank()) {
            wrapper.like(UserDO::getUsername, username);
        }
        if (nickname != null && !nickname.isBlank()) {
            wrapper.like(UserDO::getNickname, nickname);
        }
        if (status != null) {
            wrapper.eq(UserDO::getStatus, status);
        }
        wrapper.orderByDesc(UserDO::getCreateTime);
        Page<UserDO> page = new Page<>(pageNo, pageSize);
        Page<UserDO> result = userMapper.selectPage(page, wrapper);
        Map<String, Object> resp = new HashMap<>();
        resp.put("list", result.getRecords());
        resp.put("total", result.getTotal());
        resp.put("pageNum", pageNo);
        resp.put("pageSize", pageSize);
        return resp;
    }
}
