package com.ontograph.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ontograph.common.exception.BusinessException;
import com.ontograph.system.dal.dataobject.OperationLogDO;
import com.ontograph.system.dal.mysql.OperationLogMapper;
import com.ontograph.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public Map<String, Object> listLogs(Integer pageNo, Integer pageSize,
            String username, String operation, Integer status,
            String startTime, String endTime) {
        LambdaQueryWrapper<OperationLogDO> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(OperationLogDO::getUsername, username);
        }
        if (operation != null && !operation.isBlank()) {
            wrapper.like(OperationLogDO::getOperation, operation);
        }
        if (status != null) {
            wrapper.eq(OperationLogDO::getStatus, status);
        }
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(OperationLogDO::getCreateTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(OperationLogDO::getCreateTime, LocalDateTime.parse(endTime));
        }
        wrapper.orderByDesc(OperationLogDO::getCreateTime);
        Page<OperationLogDO> page = new Page<>(pageNo, pageSize);
        Page<OperationLogDO> result = operationLogMapper.selectPage(page, wrapper);
        Map<String, Object> resp = new HashMap<>();
        resp.put("list", result.getRecords());
        resp.put("total", result.getTotal());
        resp.put("pageNum", pageNo);
        resp.put("pageSize", pageSize);
        return resp;
    }

    @Override
    public OperationLogDO getLog(Long id) {
        return operationLogMapper.selectById(id);
    }

    @Override
    public void deleteLog(Long id) {
        operationLogMapper.deleteById(id);
        log.info("删除操作日志：id={}", id);
    }

    @Override
    public void clearLogs() {
        operationLogMapper.delete(null);
        log.info("清空所有操作日志");
    }

    @Override
    public List<OperationLogDO> exportLogs(String username, String operation,
            Integer status, String startTime, String endTime) {
        LambdaQueryWrapper<OperationLogDO> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isBlank()) {
            wrapper.like(OperationLogDO::getUsername, username);
        }
        if (operation != null && !operation.isBlank()) {
            wrapper.like(OperationLogDO::getOperation, operation);
        }
        if (status != null) {
            wrapper.eq(OperationLogDO::getStatus, status);
        }
        if (startTime != null && !startTime.isBlank()) {
            wrapper.ge(OperationLogDO::getCreateTime, LocalDateTime.parse(startTime));
        }
        if (endTime != null && !endTime.isBlank()) {
            wrapper.le(OperationLogDO::getCreateTime, LocalDateTime.parse(endTime));
        }
        wrapper.orderByDesc(OperationLogDO::getCreateTime);
        return operationLogMapper.selectList(wrapper);
    }

    @Override
    public void saveLog(OperationLogDO logDO) {
        if (logDO.getCreateTime() == null) {
            logDO.setCreateTime(LocalDateTime.now());
        }
        operationLogMapper.insert(logDO);
    }
}
