package com.morel.greenhouse.domain.user;

public record OperatorProfile(
        Long id,
        String username,
        String displayName,
        String phone,
        String role,
        String organization
) {
}
