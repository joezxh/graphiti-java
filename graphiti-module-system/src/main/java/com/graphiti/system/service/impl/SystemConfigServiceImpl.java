package com.graphiti.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.graphiti.common.exception.BusinessException;
import com.graphiti.system.dal.dataobject.SystemConfigDO;
import com.graphiti.system.dal.mysql.SystemConfigMapper;
import com.graphiti.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    @Override
    public Map<String, Object> listConfigs(Integer pageNo, Integer pageSize,
            String configKey, String configName, String groupName, Integer status) {
        LambdaQueryWrapper<SystemConfigDO> wrapper = buildQueryWrapper(
            configKey, configName, groupName, status);
        wrapper.orderByAsc(SystemConfigDO::getGroupName)
               .orderByAsc(SystemConfigDO::getSortNum);
        Page<SystemConfigDO> page = new Page<>(pageNo, pageSize);
        Page<SystemConfigDO> result = systemConfigMapper.selectPage(page, wrapper);
        Map<String, Object> resp = new HashMap<>();
        resp.put("list", result.getRecords());
        resp.put("total", result.getTotal());
        resp.put("pageNum", pageNo);
        resp.put("pageSize", pageSize);
        return resp;
    }

    @Override
    public List<SystemConfigDO> listAllConfigs() {
        LambdaQueryWrapper<SystemConfigDO> wrapper = buildQueryWrapper(null, null, null, null);
        wrapper.orderByAsc(SystemConfigDO::getGroupName)
               .orderByAsc(SystemConfigDO::getSortNum);
        return systemConfigMapper.selectList(wrapper);
    }

    @Override
    public SystemConfigDO getConfig(Long id) {
        SystemConfigDO config = systemConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(404, "配置不存在");
        }
        return config;
    }

    @Override
    public SystemConfigDO getConfigByKey(String key) {
        LambdaQueryWrapper<SystemConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigDO::getConfigKey, key)
               .eq(SystemConfigDO::getDeleted, false);
        return systemConfigMapper.selectList(wrapper).stream().findFirst().orElse(null);
    }

    @Override
    public Long createConfig(SystemConfigDO configDO) {
        // 检查 key 是否已存在
        SystemConfigDO existing = getConfigByKey(configDO.getConfigKey());
        if (existing != null) {
            throw new BusinessException(2004, "配置键已存在: " + configDO.getConfigKey());
        }
        configDO.setCreateTime(LocalDateTime.now());
        configDO.setUpdateTime(LocalDateTime.now());
        configDO.setDeleted(false);
        if (configDO.getStatus() == null) configDO.setStatus(1);
        if (configDO.getConfigType() == null) configDO.setConfigType(1);
        if (configDO.getSortNum() == null) configDO.setSortNum(0);
        systemConfigMapper.insert(configDO);
        log.info("创建系统配置：id={}, key={}", configDO.getId(), configDO.getConfigKey());
        return configDO.getId();
    }

    @Override
    public void updateConfig(SystemConfigDO configDO) {
        configDO.setUpdateTime(LocalDateTime.now());
        systemConfigMapper.updateById(configDO);
        log.info("更新系统配置：id={}", configDO.getId());
    }

    @Override
    public void deleteConfig(Long id) {
        SystemConfigDO config = new SystemConfigDO();
        config.setId(id);
        config.setDeleted(true);
        config.setUpdateTime(LocalDateTime.now());
        systemConfigMapper.updateById(config);
        log.info("删除系统配置：id={}", id);
    }

    private LambdaQueryWrapper<SystemConfigDO> buildQueryWrapper(
            String configKey, String configName, String groupName, Integer status) {
        LambdaQueryWrapper<SystemConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfigDO::getDeleted, false);
        if (configKey != null && !configKey.isBlank()) {
            wrapper.like(SystemConfigDO::getConfigKey, configKey);
        }
        if (configName != null && !configName.isBlank()) {
            wrapper.like(SystemConfigDO::getConfigName, configName);
        }
        if (groupName != null && !groupName.isBlank()) {
            wrapper.eq(SystemConfigDO::getGroupName, groupName);
        }
        if (status != null) {
            wrapper.eq(SystemConfigDO::getStatus, status);
        }
        return wrapper;
    }
}
