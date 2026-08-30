package com.morel.greenhouse.application.dto;

import java.util.List;

public record GreenhouseAnalytics(
        List<TelemetryTrendPoint> telemetryTrend,
        List<ChartValue> deviceStatus,
        List<ChartValue> alertLevel,
        List<ChartValue> alertStatus,
        List<ChartValue> greenhouseAreas
) {
}
