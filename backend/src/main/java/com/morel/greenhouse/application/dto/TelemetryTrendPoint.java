package com.morel.greenhouse.application.dto;

import java.time.LocalDateTime;

public record TelemetryTrendPoint(
        LocalDateTime collectedAt,
        double airTemperature,
        double airHumidity,
        double soilTemperature,
        double soilHumidity,
        double phValue,
        int co2Ppm,
        int lightLux
) {
}
