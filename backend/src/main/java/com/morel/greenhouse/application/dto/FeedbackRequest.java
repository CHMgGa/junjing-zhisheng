package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;

public record FeedbackRequest(
        Long userId,
        @NotBlank String category,
        @NotBlank String content,
        String contact
) {
}
