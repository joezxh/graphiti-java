package com.ontograph.module.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ontograph.module.system.dal.dataobject.OperationLogDO;
import com.ontograph.module.system.dal.mysql.OperationLogMapper;
import com.ontograph.module.system.service.OperationLogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    public void exportLogs(String username, String operation,
            Integer status, String startTime, String endTime, HttpServletResponse response) {
        // 查询数据
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
        List<OperationLogDO> logs = operationLogMapper.selectList(wrapper);

        // 创建 Excel 工作簿
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("操作日志");

            // 创建表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);

            // 创建表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "用户名", "操作", "请求方法", "IP地址", "状态", "耗时(ms)", "错误信息", "创建时间"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 填充数据
            for (int i = 0; i < logs.size(); i++) {
                OperationLogDO log = logs.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getUsername() != null ? log.getUsername() : "");
                row.createCell(2).setCellValue(log.getOperation() != null ? log.getOperation() : "");
                row.createCell(3).setCellValue(log.getMethod() != null ? log.getMethod() : "");
                row.createCell(4).setCellValue(log.getIp() != null ? log.getIp() : "");
                row.createCell(5).setCellValue(log.getStatus() == 1 ? "成功" : "失败");
                row.createCell(6).setCellValue(log.getDuration() != null ? log.getDuration() : 0);
                row.createCell(7).setCellValue(log.getErrorMsg() != null ? log.getErrorMsg() : "");
                row.createCell(8).setCellValue(log.getCreateTime() != null ? log.getCreateTime().toString() : "");
            }

            // 自动调整列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 设置最大列宽
                if (sheet.getColumnWidth(i) > 256 * 30) {
                    sheet.setColumnWidth(i, 256 * 30);
                }
            }

            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("operation-log.xlsx", StandardCharsets.UTF_8).toString();
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            // 写入响应输出流
            workbook.write(response.getOutputStream());
            response.flushBuffer();

            log.info("导出操作日志成功，共 {} 条记录", logs.size());
        } catch (IOException e) {
            log.error("导出操作日志失败", e);
            throw new RuntimeException("导出 Excel 失败", e);
        }
    }

    @Override
    public void saveLog(OperationLogDO logDO) {
        if (logDO.getCreateTime() == null) {
            logDO.setCreateTime(LocalDateTime.now());
        }
        operationLogMapper.insert(logDO);
    }
}
