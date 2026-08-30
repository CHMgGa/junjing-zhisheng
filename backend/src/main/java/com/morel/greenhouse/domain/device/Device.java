package com.morel.greenhouse.domain.device;

public record Device(
        Long id,
        Long greenhouseId,
        String name,
        String category,
        DeviceStatus status,
        String location,
        String remark,
        boolean autoMode,
        int healthScore
) {
}
