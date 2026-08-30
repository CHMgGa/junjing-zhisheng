package com.morel.greenhouse.domain.telemetry;

import java.time.LocalDateTime;

public record TelemetrySnapshot(
        Long greenhouseId,
        double airTemperature,
        double airHumidity,
        double soilTemperature,
        double soilHumidity,
        double phValue,
        int lightLux,
        int co2Ppm,
        LocalDateTime collectedAt
) {
}
