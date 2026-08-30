package com.morel.greenhouse.shared.security;

import com.morel.greenhouse.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiSecurityInterceptorTest {

    @Test
    void publicEndpointsAndOptionsPassWithoutDatabaseLookup() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ApiSecurityInterceptor interceptor = new ApiSecurityInterceptor(jdbcTemplate);

        assertTrue(interceptor.preHandle(request("OPTIONS", "/api/v1/users"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("GET", "/assets/app.js"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("GET", "/api/v1/auth/login"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("GET", "/api/v1/health"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("POST", "/api/v1/iot/huawei/telemetry"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("GET", "/swagger-ui/index.html"), new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("GET", "/v3/api-docs"), new MockHttpServletResponse(), new Object()));

        verify(jdbcTemplate, never()).queryForList(anyString(), eq("token"));
    }

    @Test
    void rejectsMissingBlankAndExpiredTokens() {
        ApiSecurityInterceptor interceptor = new ApiSecurityInterceptor(mock(JdbcTemplate.class));

        assertEquals(401, assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request("GET", "/api/v1/greenhouses"), new MockHttpServletResponse(), new Object())).getCode());

        MockHttpServletRequest wrongScheme = request("GET", "/api/v1/greenhouses");
        wrongScheme.addHeader("Authorization", "Basic token");
        assertEquals(401, assertThrows(BusinessException.class,
                () -> interceptor.preHandle(wrongScheme, new MockHttpServletResponse(), new Object())).getCode());

        MockHttpServletRequest blank = request("GET", "/api/v1/greenhouses");
        blank.addHeader("Authorization", "Bearer   ");
        assertEquals(401, assertThrows(BusinessException.class,
                () -> interceptor.preHandle(blank, new MockHttpServletResponse(), new Object())).getCode());

        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ApiSecurityInterceptor withDb = new ApiSecurityInterceptor(jdbcTemplate);
        MockHttpServletRequest expired = request("GET", "/api/v1/greenhouses");
        expired.addHeader("Authorization", "Bearer token");
        when(jdbcTemplate.queryForList(anyString(), eq("token"))).thenReturn(List.of());
        assertEquals(401, assertThrows(BusinessException.class,
                () -> withDb.preHandle(expired, new MockHttpServletResponse(), new Object())).getCode());
    }

    @Test
    void resolvesUserAndEnforcesAdminAndProfileRules() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ApiSecurityInterceptor interceptor = new ApiSecurityInterceptor(jdbcTemplate);
        when(jdbcTemplate.queryForList(anyString(), eq("farmer-token"))).thenReturn(List.of(Map.of(
                "user_id", 7L,
                "username", "farmer",
                "role_code", "FARMER"
        )));
        when(jdbcTemplate.queryForList(anyString(), eq("admin-token"))).thenReturn(List.of(Map.of(
                "user_id", 1L,
                "username", "admin1",
                "role_code", "ADMIN"
        )));

        MockHttpServletRequest allowed = authorized("GET", "/api/v1/users/7/profile", "farmer-token");
        assertTrue(interceptor.preHandle(allowed, new MockHttpServletResponse(), new Object()));
        CurrentUser currentUser = (CurrentUser) allowed.getAttribute(ApiSecurityInterceptor.CURRENT_USER_ATTRIBUTE);
        assertEquals("farmer", currentUser.username());
        assertEquals("farmer", MDC.get("user"));

        assertEquals(403, assertThrows(BusinessException.class,
                () -> interceptor.preHandle(authorized("GET", "/api/v1/users", "farmer-token"), new MockHttpServletResponse(), new Object())).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> interceptor.preHandle(authorized("GET", "/api/v1/users/8/profile", "farmer-token"), new MockHttpServletResponse(), new Object())).getCode());

        assertTrue(interceptor.preHandle(authorized("POST", "/api/v1/greenhouses/batches/3/events", "admin-token"),
                new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(authorized("GET", "/api/v1/users/not-a-number/profile", "farmer-token"),
                new MockHttpServletResponse(), new Object()));
    }

    @Test
    void adminOnlyEndpointMatrixAllowsAdminAndRejectsFarmer() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ApiSecurityInterceptor interceptor = new ApiSecurityInterceptor(jdbcTemplate);
        when(jdbcTemplate.queryForList(anyString(), eq("admin-token"))).thenReturn(List.of(Map.of(
                "user_id", 1L,
                "username", "admin1",
                "role_code", "ADMIN"
        )));
        when(jdbcTemplate.queryForList(anyString(), eq("farmer-token"))).thenReturn(List.of(Map.of(
                "user_id", 7L,
                "username", "farmer",
                "role_code", "FARMER"
        )));

        String[][] endpoints = {
                {"POST", "/api/v1/users"},
                {"PUT", "/api/v1/users/8"},
                {"DELETE", "/api/v1/users/8"},
                {"GET", "/api/v1/users/8/greenhouses"},
                {"POST", "/api/v1/users/8/greenhouses"},
                {"DELETE", "/api/v1/users/8/greenhouses/3"},
                {"GET", "/api/v1/users/feedback"},
                {"PUT", "/api/v1/greenhouses/2"},
                {"DELETE", "/api/v1/greenhouses/2"},
                {"POST", "/api/v1/greenhouses/batches"},
                {"POST", "/api/v1/greenhouses/batches/3/events"},
                {"POST", "/api/v1/greenhouses/alerts/4/command"}
        };

        for (String[] endpoint : endpoints) {
            assertTrue(interceptor.preHandle(authorized(endpoint[0], endpoint[1], "admin-token"),
                    new MockHttpServletResponse(), new Object()));
            assertEquals(403, assertThrows(BusinessException.class,
                    () -> interceptor.preHandle(authorized(endpoint[0], endpoint[1], "farmer-token"),
                            new MockHttpServletResponse(), new Object())).getCode());
        }

        assertTrue(interceptor.preHandle(authorized("GET", "/api/v1/users/8/profile", "admin-token"),
                new MockHttpServletResponse(), new Object()));
        Method extract = ApiSecurityInterceptor.class.getDeclaredMethod("extractProfileUserId", String.class);
        extract.setAccessible(true);
        assertEquals(null, extract.invoke(interceptor, "/short"));
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }

    private MockHttpServletRequest authorized(String method, String uri, String token) {
        MockHttpServletRequest request = request(method, uri);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
