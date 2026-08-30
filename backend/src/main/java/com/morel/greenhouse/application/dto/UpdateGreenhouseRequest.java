package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateGreenhouseRequest(
        @NotBlank String name,
        String location,
        @NotBlank String status,
        @NotNull Double area,
        String cropStage,
        Long ownerUserId
) {
}
