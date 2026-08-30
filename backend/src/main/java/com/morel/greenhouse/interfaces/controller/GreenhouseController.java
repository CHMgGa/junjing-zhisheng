package com.morel.greenhouse.interfaces.controller;

import com.morel.greenhouse.application.dto.AlertCommandRequest;
import com.morel.greenhouse.application.dto.AlertDetail;
import com.morel.greenhouse.application.dto.CreateBatchEventRequest;
import com.morel.greenhouse.application.dto.CreateBatchRequest;
import com.morel.greenhouse.application.dto.CreateDeviceRequest;
import com.morel.greenhouse.application.dto.CreateGreenhouseRequest;
import com.morel.greenhouse.application.dto.DashboardOverview;
import com.morel.greenhouse.application.dto.DeviceCommandRequest;
import com.morel.greenhouse.application.dto.GreenhouseAnalytics;
import com.morel.greenhouse.application.dto.HandleAlertRequest;
import com.morel.greenhouse.application.dto.UpdateDeviceRequest;
import com.morel.greenhouse.application.dto.UpdateGreenhouseRequest;
import com.morel.greenhouse.application.service.DeviceCommandService;
import com.morel.greenhouse.application.service.GreenhouseAnalyticsService;
import com.morel.greenhouse.application.service.GreenhouseManagementService;
import com.morel.greenhouse.application.service.GreenhouseQueryService;
import com.morel.greenhouse.domain.alert.GreenhouseAlert;
import com.morel.greenhouse.domain.device.Device;
import com.morel.greenhouse.domain.greenhouse.Greenhouse;
import com.morel.greenhouse.domain.telemetry.TelemetrySnapshot;
import com.morel.greenhouse.domain.traceability.TraceabilityRecord;
import com.morel.greenhouse.shared.api.ApiResult;
import com.morel.greenhouse.shared.security.ApiSecurityInterceptor;
import com.morel.greenhouse.shared.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/greenhouses")
public class GreenhouseController {
    private final GreenhouseQueryService queryService;
    private final DeviceCommandService deviceCommandService;
    private final GreenhouseManagementService managementService;
    private final GreenhouseAnalyticsService analyticsService;

    public GreenhouseController(
            GreenhouseQueryService queryService,
            DeviceCommandService deviceCommandService,
            GreenhouseManagementService managementService,
            GreenhouseAnalyticsService analyticsService
    ) {
        this.queryService = queryService;
        this.deviceCommandService = deviceCommandService;
        this.managementService = managementService;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ApiResult<List<Greenhouse>> listGreenhouses(HttpServletRequest servletRequest) {
        return ApiResult.ok(queryService.listGreenhouses(currentUser(servletRequest)));
    }

    @PostMapping
    public ApiResult<Void> createGreenhouse(@Valid @RequestBody CreateGreenhouseRequest request, HttpServletRequest servletRequest) {
        managementService.createGreenhouse(request, currentUser(servletRequest));
        return ApiResult.ok();
    }

    @PutMapping("/{id}")
    public ApiResult<Void> updateGreenhouse(@PathVariable Long id, @Valid @RequestBody UpdateGreenhouseRequest request) {
        managementService.updateGreenhouse(id, request);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteGreenhouse(@PathVariable Long id) {
        managementService.deleteGreenhouse(id);
        return ApiResult.ok();
    }

    @GetMapping("/overview")
    public ApiResult<DashboardOverview> overview(@RequestParam(required = false) Long greenhouseId, HttpServletRequest servletRequest) {
        return ApiResult.ok(queryService.getOverview(greenhouseId, currentUser(servletRequest)));
    }

    @GetMapping("/analytics")
    public ApiResult<GreenhouseAnalytics> analytics(
            @RequestParam(required = false) Long greenhouseId,
            @RequestParam(required = false, defaultValue = "24") Integer rangeHours,
            HttpServletRequest servletRequest
    ) {
        return ApiResult.ok(analyticsService.analytics(greenhouseId, rangeHours, currentUser(servletRequest)));
    }

    @GetMapping("/telemetry")
    public ApiResult<TelemetrySnapshot> telemetry(@RequestParam(required = false) Long greenhouseId, HttpServletRequest servletRequest) {
        return ApiResult.ok(queryService.getTelemetry(greenhouseId, currentUser(servletRequest)));
    }

    @GetMapping("/devices")
    public ApiResult<List<Device>> devices(@RequestParam(required = false) Long greenhouseId, HttpServletRequest servletRequest) {
        return ApiResult.ok(queryService.listDevices(greenhouseId, currentUser(servletRequest)));
    }

    @PostMapping("/devices")
    public ApiResult<Void> createDevice(@Valid @RequestBody CreateDeviceRequest request, HttpServletRequest servletRequest) {
        managementService.createDevice(request, currentUser(servletRequest));
        return ApiResult.ok();
    }

    @PutMapping("/devices/{id}")
    public ApiResult<Void> updateDevice(@PathVariable Long id, @Valid @RequestBody UpdateDeviceRequest request, HttpServletRequest servletRequest) {
        managementService.updateDevice(id, request, currentUser(servletRequest));
        return ApiResult.ok();
    }

    @DeleteMapping("/devices/{id}")
    public ApiResult<Void> deleteDevice(@PathVariable Long id, HttpServletRequest servletRequest) {
        managementService.deleteDevice(id, currentUser(servletRequest));
        return ApiResult.ok();
    }

    @PostMapping("/devices/commands")
    public ApiResult<Void> command(@Valid @RequestBody DeviceCommandRequest request, HttpServletRequest servletRequest) {
        deviceCommandService.execute(request, currentUser(servletRequest));
        return ApiResult.ok();
    }

    @GetMapping("/alerts")
    public ApiResult<List<GreenhouseAlert>> alerts(@RequestParam(required = false) Long greenhouseId, HttpServletRequest servletRequest) {
        return ApiResult.ok(queryService.listAlerts(greenhouseId, currentUser(servletRequest)));
    }

    @GetMapping("/alerts/detail")
    public ApiResult<List<AlertDetail>> alertDetails(@RequestParam(required = false) Long greenhouseId, HttpServletRequest servletRequest) {
        return ApiResult.ok(queryService.listAlertDetails(greenhouseId, currentUser(servletRequest)));
    }

    @PostMapping("/alerts/{id}/handle")
    public ApiResult<Void> handleAlert(@PathVariable Long id, @Valid @RequestBody HandleAlertRequest request, HttpServletRequest servletRequest) {
        managementService.handleAlert(id, request, currentUser(servletRequest));
        return ApiResult.ok();
    }

    @PostMapping("/alerts/{id}/command")
    public ApiResult<Void> alertCommand(@PathVariable Long id, @Valid @RequestBody AlertCommandRequest request, HttpServletRequest servletRequest) {
        if (request.deviceId() != null) {
            deviceCommandService.execute(new DeviceCommandRequest(request.deviceId(), request.command(), request.note()), currentUser(servletRequest));
        }
        managementService.recordAlertCommand(id, request, currentUser(servletRequest));
        return ApiResult.ok();
    }

    @GetMapping("/traceability")
    public ApiResult<List<TraceabilityRecord>> traceability(@RequestParam(required = false) Long greenhouseId, HttpServletRequest servletRequest) {
        return ApiResult.ok(queryService.listTraceabilityRecords(greenhouseId, currentUser(servletRequest)));
    }

    @GetMapping("/batches")
    public ApiResult<List<Map<String, Object>>> batches(
            @RequestParam(required = false) Long farmerId,
            @RequestParam(required = false) Long greenhouseId,
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest servletRequest
    ) {
        return ApiResult.ok(queryService.listBatches(farmerId, greenhouseId, batchNo, startDate, endDate, currentUser(servletRequest)));
    }

    @GetMapping("/batches/{id}")
    public ApiResult<Map<String, Object>> batchDetail(@PathVariable Long id, HttpServletRequest servletRequest) {
        return ApiResult.ok(queryService.batchDetail(id, currentUser(servletRequest)));
    }

    @PostMapping("/batches")
    public ApiResult<Map<String, Object>> createBatch(@Valid @RequestBody CreateBatchRequest request, HttpServletRequest servletRequest) {
        Long id = managementService.createBatch(request, currentUser(servletRequest));
        return ApiResult.ok(Map.of("id", id));
    }

    @PostMapping("/batches/{id}/events")
    public ApiResult<Map<String, Object>> createBatchEvent(
            @PathVariable Long id,
            @Valid @RequestBody CreateBatchEventRequest request,
            HttpServletRequest servletRequest
    ) {
        Long eventId = managementService.createBatchEvent(id, request, currentUser(servletRequest));
        return ApiResult.ok(Map.of("id", eventId));
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute(ApiSecurityInterceptor.CURRENT_USER_ATTRIBUTE);
    }
}
