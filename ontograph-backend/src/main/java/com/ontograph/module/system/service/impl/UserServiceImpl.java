package com.ontograph.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ontograph.common.exception.BusinessException;
import com.ontograph.module.system.dal.dataobject.UserDO;
import com.ontograph.module.system.dal.mysql.UserMapper;
import com.ontograph.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

    @Override
    public String resetPassword(Long userId) {
        UserDO user = getUser(userId);
        if (user == null || user.getDeleted()) {
            throw new BusinessException(2004, "用户不存在");
        }

        // 生成 8 位随机密码
        String newPassword = generateRandomPassword(8);
        
        // 加密并更新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        log.info("重置用户密码成功：userId={}, username={}", userId, user.getUsername());
        return newPassword;
    }

    /**
     * 生成随机密码
     * @param length 密码长度
     * @return 随机密码
     */
    private String generateRandomPassword(int length) {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*";
        String all = upper + lower + digits + special;
        
        Random random = new Random();
        StringBuilder password = new StringBuilder();
        
        // 确保至少包含一个大写字母、一个小写字母、一个数字和一个特殊字符
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));
        
        // 剩余位随机
        for (int i = password.length(); i < length; i++) {
            password.append(all.charAt(random.nextInt(all.length())));
        }
        
        // 打乱顺序
        char[] chars = password.toString().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int j = random.nextInt(chars.length);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        
        return new String(chars);
    }
}
