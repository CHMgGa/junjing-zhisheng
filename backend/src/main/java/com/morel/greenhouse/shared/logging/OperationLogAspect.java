package com.morel.greenhouse.shared.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Profile("mysql")
public class OperationLogAspect {
    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private final JdbcTemplate jdbcTemplate;

    public OperationLogAspect(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Around("within(com.morel.greenhouse.interfaces.controller..*)")
    public Object writeOperationLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        boolean success = false;
        String message = "OK";
        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Throwable throwable) {
            message = throwable.getMessage();
            throw throwable;
        } finally {
            persistLog(joinPoint, success, System.currentTimeMillis() - start, message);
        }
    }

    private void persistLog(ProceedingJoinPoint joinPoint, boolean success, long elapsed, String message) {
        HttpServletRequest request = currentRequest();
        String uri = request == null ? "" : request.getRequestURI();
        String method = request == null ? "" : request.getMethod();
        String ip = request == null ? "" : request.getRemoteAddr();
        String module = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String action = joinPoint.getSignature().getName();
        log.info("operation module={} action={} uri={} method={} success={} elapsedMs={} ip={}",
                module, action, uri, method, success, elapsed, ip);
        try {
            jdbcTemplate.update("""
                    INSERT INTO operation_log(trace_id, username, client_ip, module_name, action_name, request_uri, http_method, success, elapsed_ms, message)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    MDC.get("traceId"),
                    MDC.get("user"),
                    ip,
                    module,
                    action,
                    uri,
                    method,
                    success,
                    elapsed,
                    message == null ? "" : message
            );
        } catch (Exception exception) {
            log.warn("operation log persistence failed: {}", exception.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }
}
