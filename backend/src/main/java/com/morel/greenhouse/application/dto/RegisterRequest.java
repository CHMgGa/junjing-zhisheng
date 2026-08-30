package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String confirmPassword,
        @NotBlank String phone,
        @NotBlank @Email String email,
        String displayName,
        String gender,
        @NotBlank String verificationCode
) {
}
