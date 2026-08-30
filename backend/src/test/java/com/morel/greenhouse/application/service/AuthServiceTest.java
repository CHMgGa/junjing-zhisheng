package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.LoginRequest;
import com.morel.greenhouse.application.dto.RegisterRequest;
import com.morel.greenhouse.application.dto.ResetPasswordRequest;
import com.morel.greenhouse.application.dto.VerificationCodeRequest;
import com.morel.greenhouse.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {
    private JdbcTemplate jdbcTemplate;
    private PasswordEncoder passwordEncoder;
    private VerificationDeliveryService deliveryService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        passwordEncoder = mock(PasswordEncoder.class);
        deliveryService = mock(VerificationDeliveryService.class);
        authService = new AuthService(jdbcTemplate, passwordEncoder, deliveryService, 5, 60, 5, 20, 24);
    }

    @Test
    void loginReturnsTokenAndProfileWhenPasswordMatches() {
        when(jdbcTemplate.queryForList(anyString(), eq("alice"))).thenReturn(List.of(userRow(true)));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);

        Map<String, Object> result = authService.login(new LoginRequest("alice", "secret"), "127.0.0.1");

        assertTrue(String.valueOf(result.get("token")).length() > 10);
        Map<?, ?> profile = (Map<?, ?>) result.get("profile");
        assertEquals("alice", profile.get("username"));
        assertEquals("FARMER", profile.get("role"));
        verify(jdbcTemplate).update("UPDATE app_user SET last_login_ip = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?", "127.0.0.1", 1L);
    }

    @Test
    void loginRejectsBlankUsernameAndBlankPassword() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest(null, "secret"), "ip")).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("", "secret"), "ip")).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("alice", null), "ip")).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("alice", " "), "ip")).getCode());
    }

    @Test
    void loginRejectsWrongPasswordAndDisabledUser() {
        when(jdbcTemplate.queryForList(anyString(), eq("alice"))).thenReturn(List.of(userRow(true)));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertEquals(401, assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("alice", "wrong"), "ip")).getCode());

        when(jdbcTemplate.queryForList(anyString(), eq("disabled"))).thenReturn(List.of(userRow(false)));
        assertEquals(403, assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("disabled", "secret"), "ip")).getCode());
    }

    @Test
    void loginRejectsUnknownUser() {
        when(jdbcTemplate.queryForList(anyString(), eq("missing"))).thenReturn(List.of());

        assertEquals(401, assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("missing", "secret"), "ip")).getCode());
    }

    @Test
    void registerRejectsAdminUsernameAndMismatchedPasswordBeforeDatabaseWrite() {
        RegisterRequest admin = new RegisterRequest("adminUser", "a", "a", "13800000000", "a@example.com", "Admin", "MALE", "123456");
        RegisterRequest mismatch = new RegisterRequest("farmer", "a", "b", "13800000000", "a@example.com", "Farmer", "MALE", "123456");

        assertEquals(400, assertThrows(BusinessException.class, () -> authService.register(admin)).getCode());
        assertEquals(400, assertThrows(BusinessException.class, () -> authService.register(mismatch)).getCode());
    }

    @Test
    void registerCreatesFarmerAccountWhenCodeIsValid() {
        RegisterRequest request = new RegisterRequest(
                "farmer001", "secret", "secret", "13800000000",
                "farmer@example.com", "Farmer", "FEMALE", "123456");
        when(jdbcTemplate.queryForList(anyString(), eq("farmer@example.com"), eq("REGISTER"), eq("123456")))
                .thenReturn(List.of(Map.of("id", 9L)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("farmer001"), eq("13800000000"), eq("farmer@example.com")))
                .thenReturn(0);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-new");
        when(jdbcTemplate.queryForList(anyString(), eq("farmer001"))).thenReturn(List.of(userRow(true)));

        Map<String, Object> result = authService.register(request);

        assertTrue(String.valueOf(result.get("token")).length() > 10);
        verify(jdbcTemplate).update("UPDATE verification_code SET used = TRUE WHERE id = ?", 9L);
        verify(jdbcTemplate).update(anyString(), eq("farmer001"), eq("{bcrypt}encoded-new"), eq("13800000000"),
                eq("farmer@example.com"), eq("Farmer"), eq(DefaultAvatarResolver.FEMALE_FARMER), eq("FEMALE"));
    }

    @Test
    void registerRejectsDuplicateAccount() {
        RegisterRequest request = new RegisterRequest(
                "farmer001", "secret", "secret", "13800000000",
                "farmer@example.com", "Farmer", "MALE", "123456");
        when(jdbcTemplate.queryForList(anyString(), eq("farmer@example.com"), eq("REGISTER"), eq("123456")))
                .thenReturn(List.of(Map.of("id", 9L)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("farmer001"), eq("13800000000"), eq("farmer@example.com")))
                .thenReturn(1);

        assertEquals(409, assertThrows(BusinessException.class, () -> authService.register(request)).getCode());
    }

    @Test
    void sendCodeRejectsBlankOrNonEmailReceiver() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> authService.sendCode(new VerificationCodeRequest("", "REGISTER", null), "ip")).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> authService.sendCode(new VerificationCodeRequest("not-email", "REGISTER", null), "ip")).getCode());
    }

    @Test
    void sendCodeReturnsDevCodeWhenDeliveryUsesDevMode() {
        when(jdbcTemplate.update(anyString(), any(java.time.LocalDateTime.class))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("farmer@example.com"), eq("REGISTER"), anyString()))
                .thenReturn(20L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("farmer@example.com"), eq("REGISTER"), any()))
                .thenReturn(0L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("127.0.0.1"), any()))
                .thenReturn(0L);
        when(deliveryService.deliverEmail(eq("farmer@example.com"), eq("REGISTER"), anyString(), eq(5)))
                .thenReturn(new VerificationDeliveryService.DeliveryResult("dev message", true, true, 0, ""));

        Map<String, String> result = authService.sendCode(new VerificationCodeRequest("farmer@example.com", "REGISTER", null), "127.0.0.1");

        assertEquals("DEV", result.get("deliveryMode"));
        assertTrue(result.containsKey("devCode"));
        verify(deliveryService).deliverEmail(eq("farmer@example.com"), eq("REGISTER"), anyString(), eq(5));
    }

    @Test
    void sendCodeThrowsWhenDeliveryFails() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("farmer@example.com"), eq("REGISTER"), anyString()))
                .thenReturn(0L, 0L, 20L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("127.0.0.1"), any()))
                .thenReturn(0L);
        when(deliveryService.deliverEmail(eq("farmer@example.com"), eq("REGISTER"), anyString(), eq(5)))
                .thenReturn(new VerificationDeliveryService.DeliveryResult("smtp down", false, false, 2, "smtp down"));

        assertEquals(500, assertThrows(BusinessException.class,
                () -> authService.sendCode(new VerificationCodeRequest("farmer@example.com", "REGISTER", null), "127.0.0.1")).getCode());
    }

    @Test
    void sendCodeRejectsTooFrequentRequests() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("farmer@example.com"), eq("REGISTER"), any()))
                .thenReturn(1L);

        assertEquals(429, assertThrows(BusinessException.class,
                () -> authService.sendCode(new VerificationCodeRequest("farmer@example.com", "REGISTER", null), "127.0.0.1")).getCode());
    }

    @Test
    void sendCodeRejectsHourlyReceiverAndIpLimits() {
        AuthService strictService = new AuthService(jdbcTemplate, passwordEncoder, deliveryService, 5, 60, 1, 1, 24);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("farmer@example.com"), eq("REGISTER"), any()))
                .thenReturn(0L, 1L);
        assertEquals(429, assertThrows(BusinessException.class,
                () -> strictService.sendCode(new VerificationCodeRequest("farmer@example.com", "REGISTER", null), "127.0.0.1")).getCode());

        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("other@example.com"), eq("REGISTER"), any()))
                .thenReturn(0L, 0L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("127.0.0.1"), any()))
                .thenReturn(1L);
        assertEquals(429, assertThrows(BusinessException.class,
                () -> strictService.sendCode(new VerificationCodeRequest("other@example.com", "REGISTER", null), "127.0.0.1")).getCode());
    }

    @Test
    void resetPasswordRejectsNonEmailAndMismatchedPassword() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("phone", "123456", "new", "new"))).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("a@example.com", "123456", "new", "other"))).getCode());
    }

    @Test
    void registerAndResetRejectMissingOrInvalidCode() {
        RegisterRequest blankCode = new RegisterRequest(
                "farmer002", "secret", "secret", "13800000000",
                "farmer@example.com", "Farmer", "MALE", " ");
        assertEquals(400, assertThrows(BusinessException.class, () -> authService.register(blankCode)).getCode());

        ResetPasswordRequest invalidCode = new ResetPasswordRequest("farmer@example.com", "000000", "newSecret", "newSecret");
        when(jdbcTemplate.queryForList(anyString(), eq("farmer@example.com"), eq("RESET_PASSWORD"), eq("000000")))
                .thenReturn(List.of());
        assertEquals(400, assertThrows(BusinessException.class, () -> authService.resetPassword(invalidCode)).getCode());
    }

    @Test
    void resetPasswordUpdatesPasswordWhenCodeIsValid() {
        ResetPasswordRequest request = new ResetPasswordRequest("farmer@example.com", "123456", "newSecret", "newSecret");
        when(jdbcTemplate.queryForList(anyString(), eq("farmer@example.com"), eq("RESET_PASSWORD"), eq("123456")))
                .thenReturn(List.of(Map.of("id", 8L)));
        when(passwordEncoder.encode("newSecret")).thenReturn("encoded-reset");
        when(jdbcTemplate.update(anyString(), eq("{bcrypt}encoded-reset"), eq("farmer@example.com"))).thenReturn(1);

        authService.resetPassword(request);

        verify(jdbcTemplate).update("UPDATE verification_code SET used = TRUE WHERE id = ?", 8L);
        verify(jdbcTemplate).update(anyString(), eq("{bcrypt}encoded-reset"), eq("farmer@example.com"));
    }

    @Test
    void resetPasswordReturnsNotFoundWhenEmailIsUnknown() {
        ResetPasswordRequest request = new ResetPasswordRequest("missing@example.com", "123456", "newSecret", "newSecret");
        when(jdbcTemplate.queryForList(anyString(), eq("missing@example.com"), eq("RESET_PASSWORD"), eq("123456")))
                .thenReturn(List.of(Map.of("id", 8L)));
        when(passwordEncoder.encode("newSecret")).thenReturn("encoded-reset");
        when(jdbcTemplate.update(anyString(), eq("{bcrypt}encoded-reset"), eq("missing@example.com"))).thenReturn(0);

        assertEquals(404, assertThrows(BusinessException.class, () -> authService.resetPassword(request)).getCode());
    }

    @Test
    void sendCodeReturnsRealModeWithoutDevCodeAndSkipsBlankIpLimit() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("real@example.com"), eq("REGISTER"), any()))
                .thenReturn(0L, 0L, 20L);
        when(deliveryService.deliverEmail(eq("real@example.com"), eq("REGISTER"), anyString(), eq(5)))
                .thenReturn(new VerificationDeliveryService.DeliveryResult("sent", false, true, 0, ""));

        Map<String, String> result = authService.sendCode(new VerificationCodeRequest("real@example.com", "REGISTER", null), " ");

        assertEquals("REAL", result.get("deliveryMode"));
        assertFalse(result.containsKey("devCode"));
    }

    @Test
    void registerUsesDefaultDisplayNameAndMaleAvatarForNullGender() {
        RegisterRequest request = new RegisterRequest(
                "farmerNull", "secret", "secret", "13800000001",
                "null@example.com", null, null, "123456");
        when(jdbcTemplate.queryForList(anyString(), eq("null@example.com"), eq("REGISTER"), eq("123456")))
                .thenReturn(List.of(Map.of("id", 10L)));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("farmerNull"), eq("13800000001"), eq("null@example.com")))
                .thenReturn(0);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-null");
        when(jdbcTemplate.queryForList(anyString(), eq("farmerNull"))).thenReturn(List.of(userRow(true)));

        authService.register(request);

        verify(jdbcTemplate).update(anyString(), eq("farmerNull"), eq("{bcrypt}encoded-null"), eq("13800000001"),
                eq("null@example.com"), eq(""), eq(DefaultAvatarResolver.MALE_FARMER), eq("MALE"));
    }

    @Test
    void privateHelpersCoverNullFallbackBranches() throws Exception {
        var stripPrefix = AuthService.class.getDeclaredMethod("stripPrefix", String.class);
        stripPrefix.setAccessible(true);
        assertEquals("", stripPrefix.invoke(authService, new Object[]{null}));
        assertEquals("hash", stripPrefix.invoke(authService, "{bcrypt}hash"));

        var value = AuthService.class.getDeclaredMethod("value", Object.class);
        value.setAccessible(true);
        assertEquals("", value.invoke(authService, new Object[]{null}));
        assertEquals("7", value.invoke(authService, 7));

        var normalizeGender = AuthService.class.getDeclaredMethod("normalizeGender", String.class);
        normalizeGender.setAccessible(true);
        assertEquals("MALE", normalizeGender.invoke(authService, new Object[]{null}));
        assertEquals("FEMALE", normalizeGender.invoke(authService, " female "));
        assertEquals("MALE", normalizeGender.invoke(authService, "other"));
    }

    private Map<String, Object> userRow(boolean enabled) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1L);
        row.put("username", "alice");
        row.put("password_hash", "{bcrypt}encoded");
        row.put("role_code", "FARMER");
        row.put("display_name", "Alice");
        row.put("phone", "13800000000");
        row.put("email", "alice@example.com");
        row.put("avatar_url", "/avatars/a.png");
        row.put("gender", "FEMALE");
        row.put("bio", "bio");
        row.put("last_login_ip", "");
        row.put("enabled", enabled);
        return row;
    }
}
