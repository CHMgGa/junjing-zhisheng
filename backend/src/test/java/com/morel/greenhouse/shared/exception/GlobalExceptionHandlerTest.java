package com.morel.greenhouse.shared.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    @Test
    void businessExceptionUsesClientStatusOrFallsBackToBadRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var notFound = handler.handleBusinessException(new BusinessException(404, "missing"));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
        assertEquals(404, notFound.getBody().code());

        var serverLike = handler.handleBusinessException(new BusinessException(500, "bad"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, serverLike.getStatusCode());
        assertEquals(500, serverLike.getBody().code());

        var unknownStatus = handler.handleBusinessException(new BusinessException(799, "unknown"));
        assertEquals(HttpStatus.BAD_REQUEST, unknownStatus.getStatusCode());
        assertEquals(799, unknownStatus.getBody().code());
    }

    @Test
    void constraintAndUnexpectedExceptionsReturnApiResult() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        assertEquals(400, handler.handleConstraintViolationException(new ConstraintViolationException("invalid", null)).code());
        assertEquals(500, handler.handleUnexpectedException(new RuntimeException("boom")).code());
    }

    @Test
    void methodArgumentValidationUsesFirstFieldErrorOrDefaultMessage() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MethodParameter parameter = new MethodParameter(sampleMethod(), 0);
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "name", "must not be blank"));

        var fieldError = handler.handleValidationException(new MethodArgumentNotValidException(parameter, binding));
        assertEquals(400, fieldError.code());
        assertTrue(fieldError.message().contains("name"));

        BeanPropertyBindingResult empty = new BeanPropertyBindingResult(new Object(), "request");
        var fallback = handler.handleValidationException(new MethodArgumentNotValidException(parameter, empty));
        assertEquals("invalid request", fallback.message());
    }

    private Method sampleMethod() throws NoSuchMethodException {
        return GlobalExceptionHandlerTest.class.getDeclaredMethod("sample", String.class);
    }

    @SuppressWarnings("unused")
    private void sample(String value) {
    }
}
