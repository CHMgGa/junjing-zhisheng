package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGreenhouseRequest(
        @NotBlank String name,
        String location,
        @NotNull Double area,
        String cropStage,
        Long ownerUserId
) {
}
