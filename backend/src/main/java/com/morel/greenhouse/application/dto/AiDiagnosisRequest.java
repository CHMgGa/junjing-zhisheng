package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AiDiagnosisRequest(
        String question,
        Long greenhouseId,
        @NotBlank String imageBase64,
        String imageFilename
) {
}
