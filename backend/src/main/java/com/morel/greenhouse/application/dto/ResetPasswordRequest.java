package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank String receiver,
        @NotBlank String verificationCode,
        @NotBlank String newPassword,
        @NotBlank String confirmPassword
) {
}
