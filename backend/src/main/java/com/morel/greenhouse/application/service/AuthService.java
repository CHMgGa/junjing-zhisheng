package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.LoginRequest;
import com.morel.greenhouse.application.dto.RegisterRequest;
import com.morel.greenhouse.application.dto.ResetPasswordRequest;
import com.morel.greenhouse.application.dto.VerificationCodeRequest;
import com.morel.greenhouse.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SESSION_HOURS = 24;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final VerificationDeliveryService deliveryService;
    private final int codeTtlMinutes;
    private final int minSendIntervalSeconds;
    private final int maxSendPerHour;
    private final int maxIpSendPerHour;
    private final int cleanupRetentionHours;

    public AuthService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            VerificationDeliveryService deliveryService,
            @Value("${greenhouse.verification.code-ttl-minutes:5}") int codeTtlMinutes,
            @Value("${greenhouse.verification.min-send-interval-seconds:60}") int minSendIntervalSeconds,
            @Value("${greenhouse.verification.max-send-per-hour:5}") int maxSendPerHour,
            @Value("${greenhouse.verification.max-ip-send-per-hour:20}") int maxIpSendPerHour,
            @Value("${greenhouse.verification.cleanup-retention-hours:24}") int cleanupRetentionHours
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.deliveryService = deliveryService;
        this.codeTtlMinutes = Math.max(codeTtlMinutes, 1);
        this.minSendIntervalSeconds = Math.max(minSendIntervalSeconds, 10);
        this.maxSendPerHour = Math.max(maxSendPerHour, 1);
        this.maxIpSendPerHour = Math.max(maxIpSendPerHour, 1);
        this.cleanupRetentionHours = Math.max(cleanupRetentionHours, 1);
    }

    public Map<String, Object> login(LoginRequest request, String clientIp) {
        if (request.username() == null || request.username().isBlank()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException(400, "密码不能为空");
        }
        Map<String, Object> user = findUserByUsername(request.username());
        assertEnabled(user);
        String stored = stripPrefix((String) user.get("password_hash"));
        if (!passwordEncoder.matches(request.password(), stored)) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        jdbcTemplate.update("UPDATE app_user SET last_login_ip = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", clientIp, user.get("id"));
        log.info("User logged in: username={}, role={}, ip={}", user.get("username"), user.get("role_code"), clientIp);
        return authPayload(user, clientIp);
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (request.username().toLowerCase().startsWith("admin")) {
            throw new BusinessException(400, "农户用户名不能以 admin 开头");
        }
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }
        verifyCode(request.email(), "REGISTER", request.verificationCode());
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM app_user WHERE deleted = FALSE AND (username = ? OR phone = ? OR email = ?)",
                Integer.class,
                request.username(),
                request.phone(),
                request.email()
        );
        if (exists != null && exists > 0) {
            throw new BusinessException(409, "用户名、手机号或邮箱已存在");
        }
        String gender = normalizeGender(request.gender());
        jdbcTemplate.update("""
                INSERT INTO app_user(username, password_hash, role_code, phone, email, display_name, avatar_url, gender, bio, created_by)
                VALUES (?, ?, 'FARMER', ?, ?, ?, ?, ?, '', 'self_register')
                """,
                request.username(),
                "{bcrypt}" + passwordEncoder.encode(request.password()),
                request.phone(),
                request.email(),
                request.displayName() == null || request.displayName().isBlank() ? "" : request.displayName(),
                DefaultAvatarResolver.resolve("FARMER", gender),
                gender
        );
        jdbcTemplate.update("""
                INSERT INTO auth_user_role(user_id, role_id)
                SELECT u.id, r.id
                FROM app_user u, auth_role r
                WHERE u.username = ? AND r.role_code = 'FARMER'
                  AND NOT EXISTS (SELECT 1 FROM auth_user_role ur WHERE ur.user_id = u.id AND ur.role_id = r.id)
                """, request.username());
        log.info("Farmer account registered: username={}, phone={}, email={}", request.username(), request.phone(), request.email());
        return authPayload(findUserByUsername(request.username()), "");
    }

    public Map<String, String> sendCode(VerificationCodeRequest request, String clientIp) {
        if (request.receiver() == null || request.receiver().isBlank()) {
            throw new BusinessException(400, "邮箱不能为空");
        }
        if (!request.receiver().contains("@")) {
            throw new BusinessException(400, "验证码只能发送到邮箱");
        }
        cleanupExpiredCodes();
        enforceSendLimits(request.receiver(), request.scene(), clientIp);
        String code = String.valueOf(100000 + RANDOM.nextInt(900000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(codeTtlMinutes);
        jdbcTemplate.update("""
                INSERT INTO verification_code(receiver, scene, code, client_ip, expires_at)
                VALUES (?, ?, ?, ?, ?)
                """, request.receiver(), request.scene(), code, clientIp, expiresAt);
        Long codeId = jdbcTemplate.queryForObject("""
                SELECT id FROM verification_code
                WHERE receiver = ? AND scene = ? AND code = ?
                ORDER BY created_at DESC LIMIT 1
                """, Long.class, request.receiver(), request.scene(), code);
        VerificationDeliveryService.DeliveryResult delivery = deliveryService.deliverEmail(
                request.receiver(),
                request.scene(),
                code,
                codeTtlMinutes
        );
        String deliveryStatus = delivery.success() ? "SENT" : "FAILED";
        jdbcTemplate.update("""
                UPDATE verification_code
                SET delivery_status = ?, delivery_message = ?, sent_at = CASE WHEN ? = 'SENT' THEN CURRENT_TIMESTAMP ELSE sent_at END
                WHERE id = ?
                """, deliveryStatus, delivery.message(), deliveryStatus, codeId);
        writeVerificationSendLog(request.receiver(), request.scene(), clientIp, delivery);
        log.info("Email verification code generated: receiver={}, scene={}, deliveryMode={}",
                request.receiver(), request.scene(), delivery.devMode() ? "DEV" : "REAL");
        if (!delivery.success()) {
            throw new BusinessException(500, delivery.message());
        }
        Map<String, String> result = new HashMap<>();
        result.put("message", delivery.message() + "，有效期 " + codeTtlMinutes + " 分钟");
        result.put("deliveryMode", delivery.devMode() ? "DEV" : "REAL");
        if (delivery.devMode()) {
            result.put("devCode", code);
        }
        return result;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.receiver().contains("@")) {
            throw new BusinessException(400, "请使用邮箱重置密码");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }
        verifyCode(request.receiver(), "RESET_PASSWORD", request.verificationCode());
        int updated = jdbcTemplate.update("""
                UPDATE app_user SET password_hash = ?, updated_at = CURRENT_TIMESTAMP
                WHERE email = ? AND deleted = FALSE
                """, "{bcrypt}" + passwordEncoder.encode(request.newPassword()), request.receiver());
        if (updated == 0) {
            throw new BusinessException(404, "未找到绑定该邮箱的用户");
        }
        log.info("Password reset completed for email={}", request.receiver());
    }

    private void verifyCode(String receiver, String scene, String code) {
        if (receiver == null || receiver.isBlank() || code == null || code.isBlank()) {
            throw new BusinessException(400, "验证码不能为空");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id FROM verification_code
                WHERE receiver = ? AND scene = ? AND code = ? AND used = FALSE AND expires_at > CURRENT_TIMESTAMP
                  AND delivery_status IN ('SENT', 'PENDING')
                ORDER BY created_at DESC LIMIT 1
                """, receiver, scene, code);
        if (rows.isEmpty()) {
            throw new BusinessException(400, "验证码无效或已过期");
        }
        jdbcTemplate.update("UPDATE verification_code SET used = TRUE WHERE id = ?", rows.get(0).get("id"));
    }

    private void cleanupExpiredCodes() {
        int removed = jdbcTemplate.update("""
                DELETE FROM verification_code
                WHERE expires_at < ?
                """, LocalDateTime.now().minusHours(cleanupRetentionHours));
        if (removed > 0) {
            log.info("Expired verification codes cleaned: count={}", removed);
        }
    }

    private void enforceSendLimits(String receiver, String scene, String clientIp) {
        Long recent = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM verification_send_log
                WHERE receiver = ? AND scene = ? AND created_at > ?
                """, Long.class, receiver, scene, LocalDateTime.now().minusSeconds(minSendIntervalSeconds));
        if (recent != null && recent > 0) {
            writeVerificationBlockedLog(receiver, scene, clientIp, "验证码发送过于频繁");
            throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
        }
        Long receiverHourly = jdbcTemplate.queryForObject("""
                SELECT COUNT(1) FROM verification_send_log
                WHERE receiver = ? AND scene = ? AND created_at > ?
                """, Long.class, receiver, scene, LocalDateTime.now().minusHours(1));
        if (receiverHourly != null && receiverHourly >= maxSendPerHour) {
            writeVerificationBlockedLog(receiver, scene, clientIp, "邮箱小时发送次数超限");
            throw new BusinessException(429, "该邮箱验证码发送次数过多，请 1 小时后再试");
        }
        if (clientIp != null && !clientIp.isBlank()) {
            Long ipHourly = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1) FROM verification_send_log
                    WHERE client_ip = ? AND created_at > ?
                    """, Long.class, clientIp, LocalDateTime.now().minusHours(1));
            if (ipHourly != null && ipHourly >= maxIpSendPerHour) {
                writeVerificationBlockedLog(receiver, scene, clientIp, "IP 小时发送次数超限");
                throw new BusinessException(429, "当前网络环境验证码请求过多，请稍后再试");
            }
        }
    }

    private void writeVerificationBlockedLog(String receiver, String scene, String clientIp, String reason) {
        jdbcTemplate.update("""
                INSERT INTO verification_send_log(receiver, scene, channel, client_ip, delivery_mode, status, retry_count, provider_message, error_message, request_trace_id)
                VALUES (?, ?, 'EMAIL', ?, 'BLOCKED', 'BLOCKED', 0, ?, ?, ?)
                """,
                receiver,
                scene,
                clientIp,
                "验证码发送请求被系统限流",
                reason,
                MDC.get("traceId")
        );
    }

    private void writeVerificationSendLog(
            String receiver,
            String scene,
            String clientIp,
            VerificationDeliveryService.DeliveryResult delivery
    ) {
        jdbcTemplate.update("""
                INSERT INTO verification_send_log(receiver, scene, channel, client_ip, delivery_mode, status, retry_count, provider_message, error_message, request_trace_id)
                VALUES (?, ?, 'EMAIL', ?, ?, ?, ?, ?, ?, ?)
                """,
                receiver,
                scene,
                clientIp,
                delivery.devMode() ? "DEV" : "REAL",
                delivery.success() ? "SUCCESS" : "FAILED",
                delivery.retryCount(),
                delivery.message(),
                delivery.errorMessage(),
                MDC.get("traceId")
        );
    }

    private Map<String, Object> findUserByUsername(String username) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM app_user WHERE username = ? AND deleted = FALSE", username);
        return rows.stream().findFirst().orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));
    }

    private Map<String, Object> authPayload(Map<String, Object> user, String clientIp) {
        String token = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO app_session(token, user_id, username, role_code, client_ip, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                token,
                user.get("id"),
                user.get("username"),
                user.get("role_code"),
                clientIp,
                LocalDateTime.now().plusHours(SESSION_HOURS)
        );
        return Map.of(
                "token", token,
                "profile", Map.of(
                        "id", user.get("id"),
                        "username", user.get("username"),
                        "displayName", value(user.get("display_name")),
                        "phone", value(user.get("phone")),
                        "email", value(user.get("email")),
                        "role", user.get("role_code"),
                        "avatarUrl", value(user.get("avatar_url")),
                        "gender", value(user.get("gender")),
                        "bio", value(user.get("bio")),
                        "lastLoginIp", value(user.get("last_login_ip"))
                )
        );
    }

    private void assertEnabled(Map<String, Object> user) {
        Object enabled = user.get("enabled");
        if (enabled instanceof Boolean bool && !bool) {
            throw new BusinessException(403, "账号已被禁用");
        }
    }

    private String stripPrefix(String passwordHash) {
        return passwordHash == null ? "" : passwordHash.replace("{bcrypt}", "");
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String normalizeGender(String gender) {
        String normalized = gender == null ? "MALE" : gender.trim().toUpperCase();
        return "FEMALE".equals(normalized) ? "FEMALE" : "MALE";
    }
}
