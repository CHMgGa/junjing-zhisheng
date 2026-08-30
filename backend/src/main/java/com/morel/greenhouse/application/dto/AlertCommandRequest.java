package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AlertCommandRequest(
        Long deviceId,
        @NotBlank String command,
        String note,
        Boolean notifyFarmer
) {
}
