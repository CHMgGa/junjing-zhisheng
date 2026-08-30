package com.morel.greenhouse.application.port;

import com.morel.greenhouse.domain.alert.GreenhouseAlert;
import com.morel.greenhouse.domain.device.Device;
import com.morel.greenhouse.domain.greenhouse.Greenhouse;
import com.morel.greenhouse.domain.telemetry.TelemetrySnapshot;
import com.morel.greenhouse.domain.traceability.TraceabilityRecord;
import com.morel.greenhouse.domain.user.OperatorProfile;
import com.morel.greenhouse.application.dto.AlertDetail;

import java.util.List;
import java.util.Optional;

public interface GreenhouseRepository {
    List<Greenhouse> findGreenhouses();

    List<Greenhouse> findGreenhousesByOwner(Long ownerUserId);

    Optional<TelemetrySnapshot> findCurrentTelemetry(Long greenhouseId);

    List<Device> findDevices(Long greenhouseId);

    List<GreenhouseAlert> findAlerts(Long greenhouseId);

    List<AlertDetail> findAlertDetails(Long greenhouseId);

    List<TraceabilityRecord> findTraceabilityRecords(Long greenhouseId);

    Optional<OperatorProfile> findOperator(String username);
}
