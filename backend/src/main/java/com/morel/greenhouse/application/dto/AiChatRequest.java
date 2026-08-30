package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank String question,
        Long greenhouseId
) {
}
