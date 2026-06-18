package com.ontograph.framework.aspect;

import com.ontograph.module.system.dal.dataobject.OperationLogDO;
import com.ontograph.module.system.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面
 * 自动记录所有管理后台操作的日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    /**
     * 拦截所有 /admin/ 开头的 Controller 方法
     */
    @Around("execution(* com.ontograph.module.*.controller.*.*(..)) && @annotation(io.swagger.v3.oas.annotations.Operation)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        // 创建日志对象
        OperationLogDO logDO = new OperationLogDO();
        logDO.setCreateTime(LocalDateTime.now());
        
        try {
            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                logDO.setUsername(authentication.getName());
            }
            
            // 获取 IP 地址
            if (request != null) {
                String ip = getClientIp(request);
                logDO.setIp(ip);
                
                // 设置请求方法和路径
                logDO.setMethod(request.getMethod() + " " + request.getRequestURI());
                
                // 设置请求参数
                try {
                    Object[] args = joinPoint.getArgs();
                    if (args != null && args.length > 0) {
                        // 简单记录参数数量,避免日志过大
                        logDO.setParams("args_count=" + args.length);
                    }
                } catch (Exception e) {
                    logDO.setParams("参数解析失败");
                }
            }
            
            // 获取操作名称 (从方法注解或方法名)
            String operationName = extractOperationName(joinPoint);
            logDO.setOperation(operationName);
            
            // 执行目标方法
            Object result = joinPoint.proceed();
            
            // 记录成功
            logDO.setStatus(1);
            logDO.setDuration((int) (System.currentTimeMillis() - startTime));
            
            // 异步保存日志
            saveLogAsync(logDO);
            
            return result;
            
        } catch (Throwable e) {
            // 记录失败
            logDO.setStatus(0);
            logDO.setErrorMsg(e.getMessage() != null ? e.getMessage().substring(0, Math.min(500, e.getMessage().length())) : "未知错误");
            logDO.setDuration((int) (System.currentTimeMillis() - startTime));
            
            // 异步保存日志
            saveLogAsync(logDO);
            
            throw e;
        }
    }

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 对于通过多个代理的情况,第一个 IP 为客户端真实 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 提取操作名称
     */
    private String extractOperationName(ProceedingJoinPoint joinPoint) {
        // 从方法名提取
        String methodName = joinPoint.getSignature().getName();
        
        // 转换为中文操作名称
        if (methodName.startsWith("create") || methodName.startsWith("add")) {
            return "新增";
        } else if (methodName.startsWith("update") || methodName.startsWith("edit")) {
            return "修改";
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            return "删除";
        } else if (methodName.startsWith("get") || methodName.startsWith("list") || methodName.startsWith("query")) {
            return "查询";
        } else if (methodName.startsWith("export")) {
            return "导出";
        } else if (methodName.startsWith("import")) {
            return "导入";
        } else if (methodName.startsWith("clear")) {
            return "清空";
        }
        
        return methodName;
    }

    /**
     * 异步保存日志 (避免影响主流程性能)
     */
    private void saveLogAsync(OperationLogDO logDO) {
        try {
            operationLogService.saveLog(logDO);
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }
}
