package com.morel.greenhouse.shared.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResultTest {

    @Test
    void okWrapsDataWithSuccessMetadata() {
        ApiResult<Map<String, String>> result = ApiResult.ok(Map.of("status", "UP"));

        assertEquals(0, result.code());
        assertEquals("success", result.message());
        assertEquals("UP", result.data().get("status"));
        assertNotNull(result.timestamp());
    }

    @Test
    void emptyOkUsesNullData() {
        ApiResult<Void> result = ApiResult.ok();

        assertEquals(0, result.code());
        assertEquals("success", result.message());
        assertNull(result.data());
        assertNotNull(result.timestamp());
    }

    @Test
    void failUsesProvidedCodeAndMessage() {
        ApiResult<Void> result = ApiResult.fail(404, "not found");

        assertEquals(404, result.code());
        assertEquals("not found", result.message());
        assertNull(result.data());
        assertNotNull(result.timestamp());
    }
}
