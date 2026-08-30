package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.DeviceCommandRequest;
import com.morel.greenhouse.application.port.HardwareGateway;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceCommandServiceTest {
    private HardwareGateway hardwareGateway;
    private JdbcTemplate jdbcTemplate;
    private DeviceCommandService service;

    @BeforeEach
    void setUp() {
        hardwareGateway = mock(HardwareGateway.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new DeviceCommandService(hardwareGateway, jdbcTemplate);
    }

    @Test
    void adminCanStartDeviceAndStatusIsUpdated() {
        DeviceCommandRequest request = new DeviceCommandRequest(10L, "START", null);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(1);

        service.execute(request, new CurrentUser(1L, "admin", "ADMIN"));

        verify(hardwareGateway).dispatchDeviceCommand(request);
        verify(jdbcTemplate).update(anyString(), eq("RUNNING"), eq("START"), eq(10L));
    }

    @Test
    void farmerCanStopOwnedDevice() {
        DeviceCommandRequest request = new DeviceCommandRequest(10L, "STOP", null);
        CurrentUser farmer = new CurrentUser(3L, "farmer", "FARMER");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(10L), eq(3L), eq(3L))).thenReturn(1);

        service.execute(request, farmer);

        verify(hardwareGateway).dispatchDeviceCommand(request);
        verify(jdbcTemplate).update(anyString(), eq("STOPPED"), eq("STOP"), eq(10L));
    }

    @Test
    void adminCanPutDeviceIntoMaintenance() {
        DeviceCommandRequest request = new DeviceCommandRequest(10L, "MAINTENANCE", "inspect");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(1);

        service.execute(request, new CurrentUser(1L, "admin", "ADMIN"));

        verify(hardwareGateway).dispatchDeviceCommand(request);
        verify(jdbcTemplate).update(anyString(), eq("MAINTENANCE"), eq("MAINTENANCE"), eq(10L));
    }

    @Test
    void commandRequiresLoginAndExistingDevice() {
        DeviceCommandRequest request = new DeviceCommandRequest(10L, "START", null);

        assertEquals(401, assertThrows(BusinessException.class,
                () -> service.execute(request, null)).getCode());

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(0);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.execute(request, new CurrentUser(1L, "admin", "ADMIN"))).getCode());
        verify(hardwareGateway, never()).dispatchDeviceCommand(request);
    }

    @Test
    void farmerCannotCommandOtherGreenhouseDevice() {
        DeviceCommandRequest request = new DeviceCommandRequest(10L, "START", null);
        CurrentUser farmer = new CurrentUser(3L, "farmer", "FARMER");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(10L), eq(3L), eq(3L))).thenReturn(0);

        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.execute(request, farmer)).getCode());
        verify(hardwareGateway, never()).dispatchDeviceCommand(request);
    }

    @Test
    void unknownCommandIsDispatchedWithoutLocalStatusUpdate() {
        DeviceCommandRequest request = new DeviceCommandRequest(10L, "PING", null);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(10L))).thenReturn(1);

        service.execute(request, new CurrentUser(1L, "admin", "ADMIN"));

        verify(hardwareGateway).dispatchDeviceCommand(request);
        verify(jdbcTemplate, never()).update(anyString(), eq("PING"), eq(10L));
    }
}
