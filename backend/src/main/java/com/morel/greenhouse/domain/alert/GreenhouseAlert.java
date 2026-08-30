package com.morel.greenhouse.domain.alert;

import java.time.LocalDateTime;

public record GreenhouseAlert(
        Long id,
        Long greenhouseId,
        String title,
        String description,
        AlertLevel level,
        AlertStatus status,
        LocalDateTime occurredAt
) {
}
