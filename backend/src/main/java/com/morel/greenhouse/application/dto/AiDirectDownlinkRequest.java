package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiDirectDownlinkRequest(
        @NotNull Long greenhouseId,
        @NotBlank String title,
        @NotBlank String content,
        String riskLevel,
        String note
) {
}
