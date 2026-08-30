package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveUserRequest(
        @NotBlank String username,
        String password,
        @NotBlank String roleCode,
        String phone,
        String email,
        String displayName,
        String gender,
        String bio,
        Boolean enabled
) {
}
