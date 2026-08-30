package com.morel.greenhouse.shared.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BusinessExceptionTest {

    @Test
    void storesCodeAndMessage() {
        BusinessException exception = new BusinessException(400, "invalid request");

        assertEquals(400, exception.getCode());
        assertEquals("invalid request", exception.getMessage());
    }
}
