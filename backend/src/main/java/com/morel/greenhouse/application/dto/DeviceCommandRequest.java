package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceCommandRequest(
        @NotNull Long deviceId,
        @NotBlank String command,
        String value
) {
}
