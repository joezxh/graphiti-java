package com.ontograph.system.service;

import com.ontograph.system.dal.dataobject.SystemConfigDO;
import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {

    /**
     * 分页查询配置列表
     */
    Map<String, Object> listConfigs(Integer pageNo, Integer pageSize, String configKey,
                                    String configName, String groupName, Integer status);

    /**
     * 获取所有配置（全量）
     */
    List<SystemConfigDO> listAllConfigs();

    /**
     * 获取配置详情
     */
    SystemConfigDO getConfig(Long id);

    /**
     * 根据 key 获取配置
     */
    SystemConfigDO getConfigByKey(String key);

    /**
     * 创建配置
     */
    Long createConfig(SystemConfigDO configDO);

    /**
     * 更新配置
     */
    void updateConfig(SystemConfigDO configDO);

    /**
     * 删除配置
     */
    void deleteConfig(Long id);
}
