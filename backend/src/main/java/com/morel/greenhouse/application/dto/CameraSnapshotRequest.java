package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotNull;

public record CameraSnapshotRequest(
        @NotNull Long greenhouseId,
        Long deviceId,
        String imageUrl,
        String imageBase64,
        String sourceType
) {
}
