package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificationCodeRequest(
        @NotBlank String receiver,
        @NotBlank String scene,
        String captchaToken
) {
}
