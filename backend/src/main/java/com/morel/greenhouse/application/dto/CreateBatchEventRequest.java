package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBatchEventRequest(
        @NotBlank String eventCode,
        @NotBlank String eventTitle,
        String eventStatus,
        String description,
        String imageUrl
) {
}
