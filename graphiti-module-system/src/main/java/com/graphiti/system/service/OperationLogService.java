package com.graphiti.system.service;

import com.graphiti.system.dal.dataobject.OperationLogDO;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 分页查询日志列表
     */
    Map<String, Object> listLogs(Integer pageNo, Integer pageSize, String username,
                                  String operation, Integer status,
                                  String startTime, String endTime);

    /**
     * 获取日志详情
     */
    OperationLogDO getLog(Long id);

    /**
     * 删除单条日志
     */
    void deleteLog(Long id);

    /**
     * 清空所有日志
     */
    void clearLogs();

    /**
     * 导出所有日志
     */
    List<OperationLogDO> exportLogs(String username, String operation,
                                      Integer status, String startTime, String endTime);

    /**
     * 记录日志（供 AOP 拦截器调用）
     */
    void saveLog(OperationLogDO logDO);
}
