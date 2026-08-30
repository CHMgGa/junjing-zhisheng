package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeviceRequest(
        @NotNull Long greenhouseId,
        @NotBlank String name,
        @NotBlank String category,
        String location,
        String remark,
        Boolean autoMode
) {
}
