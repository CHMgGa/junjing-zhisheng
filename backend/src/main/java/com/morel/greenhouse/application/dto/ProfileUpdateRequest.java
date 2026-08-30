package com.morel.greenhouse.application.dto;

public record ProfileUpdateRequest(
        String username,
        String phone,
        String email,
        String displayName,
        String avatarUrl,
        String gender,
        String bio,
        Boolean allowAdminDelete
) {
}
