package com.morel.greenhouse.shared.api;

import java.time.OffsetDateTime;

public record ApiResult<T>(
        int code,
        String message,
        T data,
        OffsetDateTime timestamp
) {
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "success", data, OffsetDateTime.now());
    }

    public static ApiResult<Void> ok() {
        return new ApiResult<>(0, "success", null, OffsetDateTime.now());
    }

    public static ApiResult<Void> fail(int code, String message) {
        return new ApiResult<>(code, message, null, OffsetDateTime.now());
    }
}
