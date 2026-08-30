package com.morel.greenhouse.shared.security;

import com.morel.greenhouse.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;

@Component
public class ApiSecurityInterceptor implements HandlerInterceptor {
    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final JdbcTemplate jdbcTemplate;

    public ApiSecurityInterceptor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || publicEndpoint(request)) {
            return true;
        }
        CurrentUser currentUser = resolveCurrentUser(request);
        request.setAttribute(CURRENT_USER_ATTRIBUTE, currentUser);
        MDC.put("user", currentUser.username());
        enforceRole(request, currentUser);
        return true;
    }

    private boolean publicEndpoint(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/v1/")) {
            return true;
        }
        return uri.startsWith("/api/v1/auth/")
                || uri.startsWith("/api/v1/health")
                || uri.startsWith("/api/v1/iot/huawei/")
                || uri.startsWith("/swagger-ui")
                || uri.startsWith("/v3/api-docs");
    }

    private CurrentUser resolveCurrentUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(401, "请先登录");
        }
        String token = header.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new BusinessException(401, "登录凭证不能为空");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT s.user_id, s.username, s.role_code
                FROM app_session s
                JOIN app_user u ON u.id = s.user_id
                WHERE s.token = ?
                  AND s.revoked = FALSE
                  AND s.expires_at > CURRENT_TIMESTAMP
                  AND u.enabled = TRUE
                  AND u.deleted = FALSE
                """, token);
        if (rows.isEmpty()) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
        Map<String, Object> row = rows.get(0);
        return new CurrentUser(
                Long.valueOf(String.valueOf(row.get("user_id"))),
                String.valueOf(row.get("username")),
                String.valueOf(row.get("role_code"))
        );
    }

    private void enforceRole(HttpServletRequest request, CurrentUser currentUser) {
        if (adminOnly(request) && !currentUser.admin()) {
            throw new BusinessException(403, "没有管理员权限");
        }
        if (profileEndpoint(request) && !currentUser.admin()) {
            Long requestedUserId = extractProfileUserId(request.getRequestURI());
            if (requestedUserId != null && !requestedUserId.equals(currentUser.id())) {
                throw new BusinessException(403, "只能访问自己的个人资料");
            }
        }
    }

    private boolean adminOnly(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        return ("GET".equals(method) && uri.equals("/api/v1/users"))
                || ("POST".equals(method) && uri.equals("/api/v1/users"))
                || ("PUT".equals(method) && uri.matches("/api/v1/users/\\d+"))
                || ("DELETE".equals(method) && uri.matches("/api/v1/users/\\d+"))
                || ("GET".equals(method) && uri.matches("/api/v1/users/\\d+/greenhouses"))
                || ("POST".equals(method) && uri.matches("/api/v1/users/\\d+/greenhouses"))
                || ("DELETE".equals(method) && uri.matches("/api/v1/users/\\d+/greenhouses/\\d+"))
                || ("GET".equals(method) && uri.equals("/api/v1/users/feedback"))
                || ("PUT".equals(method) && uri.matches("/api/v1/greenhouses/\\d+"))
                || ("DELETE".equals(method) && uri.matches("/api/v1/greenhouses/\\d+"))
                || ("POST".equals(method) && uri.equals("/api/v1/greenhouses/batches"))
                || ("POST".equals(method) && uri.matches("/api/v1/greenhouses/batches/\\d+/events"))
                || ("POST".equals(method) && uri.matches("/api/v1/greenhouses/alerts/\\d+/command"));
    }

    private boolean profileEndpoint(HttpServletRequest request) {
        return request.getRequestURI().matches("/api/v1/users/\\d+/profile");
    }

    private Long extractProfileUserId(String uri) {
        String[] parts = uri.split("/");
        if (parts.length < 5) {
            return null;
        }
        try {
            return Long.valueOf(parts[4]);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
