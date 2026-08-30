package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.port.GreenhouseRepository;
import com.morel.greenhouse.application.dto.AlertDetail;
import com.morel.greenhouse.domain.alert.AlertLevel;
import com.morel.greenhouse.domain.alert.AlertStatus;
import com.morel.greenhouse.domain.alert.GreenhouseAlert;
import com.morel.greenhouse.domain.device.Device;
import com.morel.greenhouse.domain.device.DeviceStatus;
import com.morel.greenhouse.domain.greenhouse.Greenhouse;
import com.morel.greenhouse.domain.greenhouse.GreenhouseStatus;
import com.morel.greenhouse.domain.telemetry.TelemetrySnapshot;
import com.morel.greenhouse.domain.traceability.TraceabilityRecord;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GreenhouseQueryServiceTest {
    private GreenhouseRepository repository;
    private JdbcTemplate jdbcTemplate;
    private GreenhouseQueryService service;

    @BeforeEach
    void setUp() {
        repository = mock(GreenhouseRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new GreenhouseQueryService(repository, jdbcTemplate);
    }

    @Test
    void overviewReturnsEmptySummaryWhenUserHasNoGreenhouse() {
        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of());

        var overview = service.getOverview(null, farmer());

        assertTrue(overview.greenhouses().isEmpty());
        assertEquals(0, overview.productionSummary().activeGreenhouseCount());
        assertEquals("-", overview.productionSummary().qualityGrade());
    }

    @Test
    void overviewAggregatesTelemetryDevicesAlertsAndBatchCount() {
        Greenhouse greenhouse = greenhouse(1L, GreenhouseStatus.ONLINE);
        TelemetrySnapshot telemetry = telemetry(1L);
        Device running = new Device(10L, 1L, "fan", "FAN", DeviceStatus.RUNNING, "north", "", true, 96);
        GreenhouseAlert open = new GreenhouseAlert(20L, 1L, "hot", "too hot", AlertLevel.CRITICAL, AlertStatus.OPEN, LocalDateTime.now());
        GreenhouseAlert resolved = new GreenhouseAlert(21L, 1L, "old", "done", AlertLevel.INFO, AlertStatus.RESOLVED, LocalDateTime.now());

        when(repository.findGreenhouses()).thenReturn(List.of(greenhouse));
        when(repository.findCurrentTelemetry(1L)).thenReturn(Optional.of(telemetry));
        when(repository.findDevices(1L)).thenReturn(List.of(running));
        when(repository.findAlerts(1L)).thenReturn(List.of(open, resolved));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(3L);

        var overview = service.getOverview(null, admin());

        assertSame(telemetry, overview.currentTelemetry());
        assertEquals(1, overview.devices().size());
        assertEquals(List.of(open), overview.activeAlerts());
        assertEquals(1, overview.productionSummary().activeGreenhouseCount());
        assertEquals(1, overview.productionSummary().runningDeviceCount());
        assertEquals(1, overview.productionSummary().unresolvedAlertCount());
        assertEquals(3, overview.productionSummary().batchCount());
    }

    @Test
    void listGreenhousesDevicesAlertsAndTraceabilityUseResolvedVisibility() {
        Greenhouse greenhouse = greenhouse(1L, GreenhouseStatus.ONLINE);
        Device device = new Device(10L, 1L, "fan", "FAN", DeviceStatus.STOPPED, "north", "", false, 90);
        GreenhouseAlert alert = new GreenhouseAlert(20L, 1L, "hot", "too hot", AlertLevel.WARNING, AlertStatus.OPEN, LocalDateTime.now());
        TraceabilityRecord record = new TraceabilityRecord(30L, 1L, "B-1", "plant", "admin", LocalDate.now(), "ok");
        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of(greenhouse));
        when(repository.findDevices(1L)).thenReturn(List.of(device));
        when(repository.findAlerts(1L)).thenReturn(List.of(alert));
        when(repository.findTraceabilityRecords(1L)).thenReturn(List.of(record));

        assertEquals(List.of(greenhouse), service.listGreenhouses(farmer()));
        assertEquals(List.of(device), service.listDevices(null, farmer()));
        assertEquals(List.of(alert), service.listAlerts(1L, farmer()));
        assertEquals(List.of(record), service.listTraceabilityRecords(1L, farmer()));
    }

    @Test
    void telemetryRejectsInvisibleGreenhouse() {
        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of(greenhouse(1L, GreenhouseStatus.ONLINE)));

        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.getTelemetry(99L, farmer())).getCode());
    }

    @Test
    void telemetryRejectsMissingDataAndMissingBinding() {
        when(repository.findGreenhouses()).thenReturn(List.of(greenhouse(1L, GreenhouseStatus.ONLINE)));
        when(repository.findCurrentTelemetry(1L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.getTelemetry(1L, admin())).getCode());

        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.listDevices(null, farmer())).getCode());
    }

    @Test
    void alertDetailsCoverAdminFarmerEmptySingleAndMergedQueries() {
        AlertDetail detail = alertDetail(1L);
        when(repository.findAlertDetails(99L)).thenReturn(List.of(detail));
        assertEquals(List.of(detail), service.listAlertDetails(99L, admin()));

        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of());
        assertTrue(service.listAlertDetails(null, farmer()).isEmpty());

        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of(greenhouse(1L, GreenhouseStatus.ONLINE), greenhouse(2L, GreenhouseStatus.WARNING)));
        when(repository.findAlertDetails(1L)).thenReturn(List.of(alertDetail(1L)));
        when(repository.findAlertDetails(2L)).thenReturn(List.of(alertDetail(2L)));
        assertEquals(1L, service.listAlertDetails(1L, farmer()).get(0).greenhouseId());
        assertEquals(2, service.listAlertDetails(null, farmer()).size());
    }

    @Test
    void listBatchesBuildsFiltersForVisibleGreenhouses() {
        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of(greenhouse(1L, GreenhouseStatus.ONLINE), greenhouse(2L, GreenhouseStatus.WARNING)));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
                .thenReturn(List.of(Map.of("batch_no", "B-001")));

        List<Map<String, Object>> batches = service.listBatches(null, null, "B", "2026-01-01", "2026-12-31", farmer());

        assertEquals(1, batches.size());
        verify(jdbcTemplate).queryForList(anyString(), any(Object[].class));
    }

    @Test
    void listBatchesReturnsEmptyOrRejectsInvisibleSpecificGreenhouse() {
        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of());
        assertTrue(service.listBatches(null, null, null, null, null, farmer()).isEmpty());

        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of(greenhouse(1L, GreenhouseStatus.ONLINE)));
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.listBatches(null, 99L, null, null, null, farmer())).getCode());
    }

    @Test
    void listBatchesUsesSpecificGreenhouseFilter() {
        when(repository.findGreenhouses()).thenReturn(List.of(greenhouse(1L, GreenhouseStatus.ONLINE)));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("batch_no", "B-001")));

        assertEquals(1, service.listBatches(null, 1L, null, null, null, admin()).size());
    }

    @Test
    void batchDetailRejectsInvisibleFarmerBatch() {
        when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of(Map.of("greenhouse_id", 99L)));
        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of(greenhouse(1L, GreenhouseStatus.ONLINE)));

        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.batchDetail(5L, farmer())).getCode());
    }

    @Test
    void batchDetailReturnsBatchAndEventsOrNotFound() {
        when(jdbcTemplate.queryForList(anyString(), eq(404L))).thenReturn(List.of());
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.batchDetail(404L, admin())).getCode());

        Map<String, Object> batch = Map.of("id", 5L, "greenhouse_id", 1L);
        Map<String, Object> event = Map.of("id", 8L, "batch_id", 5L);
        when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of(batch), List.of(event));

        Map<String, Object> detail = service.batchDetail(5L, admin());

        assertEquals(batch, detail.get("batch"));
        assertEquals(List.of(event), detail.get("events"));
    }

    @Test
    void batchDetailAllowsFarmerWhenBatchGreenhouseIsVisible() {
        Map<String, Object> batch = Map.of("id", 5L, "greenhouse_id", 1L);
        Map<String, Object> event = Map.of("id", 8L, "batch_id", 5L);
        when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of(batch), List.of(event));
        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of(greenhouse(1L, GreenhouseStatus.ONLINE)));

        assertEquals(batch, service.batchDetail(5L, farmer()).get("batch"));
    }

    @Test
    void batchCountReturnsZeroForEmptyGreenhouseIds() throws Exception {
        var method = GreenhouseQueryService.class.getDeclaredMethod("batchCount", List.class);
        method.setAccessible(true);

        assertEquals(0, method.invoke(service, List.of()));
    }

    @Test
    void overviewCountsOfflineAndStoppedBranchesAndNullBatchCount() {
        Greenhouse online = greenhouse(1L, GreenhouseStatus.ONLINE);
        Greenhouse offline = greenhouse(2L, GreenhouseStatus.OFFLINE);
        Device running = new Device(10L, 1L, "fan", "FAN", DeviceStatus.RUNNING, "north", "", true, 96);
        Device stopped = new Device(11L, 1L, "lamp", "LIGHT", DeviceStatus.STOPPED, "south", "", true, 80);
        GreenhouseAlert open = new GreenhouseAlert(20L, 1L, "hot", "too hot", AlertLevel.CRITICAL, AlertStatus.OPEN, LocalDateTime.now());

        when(repository.findGreenhouses()).thenReturn(List.of(online, offline));
        when(repository.findCurrentTelemetry(2L)).thenReturn(Optional.of(telemetry(2L)));
        when(repository.findDevices(2L)).thenReturn(List.of(running, stopped));
        when(repository.findAlerts(2L)).thenReturn(List.of(open));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(null);

        var overview = service.getOverview(2L, null);

        assertEquals(1, overview.productionSummary().activeGreenhouseCount());
        assertEquals(1, overview.productionSummary().runningDeviceCount());
        assertEquals(0, overview.productionSummary().batchCount());
    }

    @Test
    void listBatchesCoversAdminFarmerFilterAndBlankFilters() {
        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of(greenhouse(1L, GreenhouseStatus.ONLINE)));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("batch_no", "B-002")));

        assertEquals(1, service.listBatches(99L, null, " ", "", null, farmer()).size());

        when(repository.findGreenhousesByOwner(7L)).thenReturn(List.of(greenhouse(1L, GreenhouseStatus.ONLINE)));
        assertEquals(1, service.listBatches(null, 1L, " B-002 ", null, " 2026-12-31 ", farmer()).size());
    }

    @Test
    void listBatchesAdminCanLimitByFarmerOwner() {
        when(repository.findGreenhousesByOwner(22L)).thenReturn(List.of(greenhouse(6L, GreenhouseStatus.WARNING)));
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(Map.of("greenhouse_id", 6L)));

        assertEquals(1, service.listBatches(22L, null, null, null, null, admin()).size());
    }

    private CurrentUser admin() {
        return new CurrentUser(1L, "admin1", "ADMIN");
    }

    private CurrentUser farmer() {
        return new CurrentUser(7L, "farmer", "FARMER");
    }

    private Greenhouse greenhouse(Long id, GreenhouseStatus status) {
        return new Greenhouse(id, 7L, "G" + id, "east", status, 12.5, "fruiting");
    }

    private TelemetrySnapshot telemetry(Long greenhouseId) {
        return new TelemetrySnapshot(greenhouseId, 21.5, 82.0, 18.0, 62.0, 6.8, 3300, 720, LocalDateTime.now());
    }

    private AlertDetail alertDetail(Long greenhouseId) {
        return new AlertDetail(1L, greenhouseId, "G" + greenhouseId, "east", 7L, "farmer",
                null, null, "hot", "too hot", "WARNING", "OPEN", LocalDateTime.now(),
                null, null, null, null);
    }
}
