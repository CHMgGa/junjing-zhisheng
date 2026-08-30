package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateDeviceRequest(
        @NotNull Long greenhouseId,
        @NotBlank String name,
        @NotBlank String category,
        @NotBlank String status,
        String location,
        String remark,
        Boolean autoMode,
        @NotNull @Min(0) @Max(100) Integer healthScore
) {
}
