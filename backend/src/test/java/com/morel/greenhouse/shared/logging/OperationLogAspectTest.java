package com.morel.greenhouse.shared.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogAspectTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void writeOperationLogPersistsSuccessAndFailure() throws Throwable {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        OperationLogAspect aspect = new OperationLogAspect(jdbcTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users");
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        MDC.put("traceId", "trace-1");
        MDC.put("user", "admin1");

        ProceedingJoinPoint success = joinPoint("UserController", "createUser");
        when(success.proceed()).thenReturn("ok");
        assertEquals("ok", aspect.writeOperationLog(success));
        verify(jdbcTemplate).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        ProceedingJoinPoint failure = joinPoint("UserController", "deleteUser");
        when(failure.proceed()).thenThrow(new RuntimeException("boom"));
        assertEquals("boom", assertThrows(RuntimeException.class, () -> aspect.writeOperationLog(failure)).getMessage());
    }

    @Test
    void writeOperationLogHandlesMissingRequestAndPersistenceFailure() throws Throwable {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        doThrow(new RuntimeException("db down")).when(jdbcTemplate)
                .update(anyString(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        OperationLogAspect aspect = new OperationLogAspect(jdbcTemplate);

        ProceedingJoinPoint joinPoint = joinPoint("HealthController", "health");
        when(joinPoint.proceed()).thenReturn("healthy");

        assertEquals("healthy", aspect.writeOperationLog(joinPoint));
    }

    private ProceedingJoinPoint joinPoint(String typeName, String methodName) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        Class<?> declaringType = "HealthController".equals(typeName) ? DummyHealthController.class : DummyUserController.class;
        when(signature.getDeclaringType()).thenReturn((Class) declaringType);
        when(signature.getName()).thenReturn(methodName);
        when(joinPoint.getSignature()).thenReturn(signature);
        return joinPoint;
    }

    private static class DummyUserController {
    }

    private static class DummyHealthController {
    }
}
