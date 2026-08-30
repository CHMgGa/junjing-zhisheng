package com.morel.greenhouse.application.dto;

import com.morel.greenhouse.domain.alert.GreenhouseAlert;
import com.morel.greenhouse.domain.device.Device;
import com.morel.greenhouse.domain.greenhouse.Greenhouse;
import com.morel.greenhouse.domain.telemetry.TelemetrySnapshot;

import java.util.List;

public record DashboardOverview(
        List<Greenhouse> greenhouses,
        TelemetrySnapshot currentTelemetry,
        List<Device> devices,
        List<GreenhouseAlert> activeAlerts,
        ProductionSummary productionSummary
) {
}
