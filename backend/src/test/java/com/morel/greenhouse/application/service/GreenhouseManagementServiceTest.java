package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.AlertCommandRequest;
import com.morel.greenhouse.application.dto.CreateBatchEventRequest;
import com.morel.greenhouse.application.dto.CreateBatchRequest;
import com.morel.greenhouse.application.dto.CreateDeviceRequest;
import com.morel.greenhouse.application.dto.CreateGreenhouseRequest;
import com.morel.greenhouse.application.dto.HandleAlertRequest;
import com.morel.greenhouse.application.dto.UpdateDeviceRequest;
import com.morel.greenhouse.application.dto.UpdateGreenhouseRequest;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GreenhouseManagementServiceTest {
    private JdbcTemplate jdbcTemplate;
    private UserAccountService userAccountService;
    private DeviceCommandService deviceCommandService;
    private GreenhouseManagementService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        userAccountService = mock(UserAccountService.class);
        deviceCommandService = mock(DeviceCommandService.class);
        service = new GreenhouseManagementService(jdbcTemplate, userAccountService, deviceCommandService);
    }

    @Test
    void createGreenhouseCreatesTelemetryAndBindingForOwner() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("G1"))).thenReturn(0);
        when(jdbcTemplate.queryForObject("SELECT id FROM greenhouse WHERE name = ? ORDER BY id DESC LIMIT 1", Long.class, "G1"))
                .thenReturn(10L);

        service.createGreenhouse(new CreateGreenhouseRequest("G1", "east", 12.5, "fruiting", 7L), admin());

        verify(jdbcTemplate).update(anyString(), eq(7L), eq("G1"), eq("east"), eq(12.5), eq("fruiting"));
        verify(jdbcTemplate).update(anyString(), eq(10L));
        verify(jdbcTemplate).update(anyString(), eq(7L), eq(10L), eq(10L));
    }

    @Test
    void createGreenhouseRejectsDuplicateName() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("G1"))).thenReturn(1);

        assertEquals(409, assertThrows(BusinessException.class,
                () -> service.createGreenhouse(new CreateGreenhouseRequest("G1", "east", 12.5, "fruiting", null), admin())).getCode());
    }

    @Test
    void createGreenhouseCoversNullOwnerAndFarmerOwnerBranches() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("NoOwner"))).thenReturn(0);
        when(jdbcTemplate.queryForObject("SELECT id FROM greenhouse WHERE name = ? ORDER BY id DESC LIMIT 1", Long.class, "NoOwner"))
                .thenReturn(11L);
        service.createGreenhouse(new CreateGreenhouseRequest("NoOwner", "east", 1.0, "", null), null);
        verify(jdbcTemplate, never()).update(anyString(), eq((Long) null), eq(11L), eq(11L));

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("FarmerOwner"))).thenReturn(0);
        when(jdbcTemplate.queryForObject("SELECT id FROM greenhouse WHERE name = ? ORDER BY id DESC LIMIT 1", Long.class, "FarmerOwner"))
                .thenReturn(12L);
        service.createGreenhouse(new CreateGreenhouseRequest("FarmerOwner", "west", 2.0, "seed", null), farmer());
        verify(jdbcTemplate).update(anyString(), eq(7L), eq(12L), eq(12L));
    }

    @Test
    void createBatchRequiresAdminAndCreatesInitialEvent() {
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.createBatch(batchRequest(), null)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.createBatch(batchRequest(), farmer())).getCode());
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse WHERE id = ? AND deleted = FALSE", Integer.class, 1L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM production_batch WHERE batch_no = ? AND deleted = FALSE", Integer.class, "B-1"))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject("SELECT id FROM production_batch WHERE batch_no = ?", Long.class, "B-1"))
                .thenReturn(20L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM production_batch WHERE id = ? AND deleted = FALSE", Integer.class, 20L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT COALESCE(MAX(sort_order), 0) FROM production_batch_event WHERE batch_id = ? AND deleted = FALSE", Integer.class, 20L))
                .thenReturn(0);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(20L))).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(20L))).thenReturn(30L);

        assertEquals(20L, service.createBatch(batchRequest(), admin()));
    }

    @Test
    void createBatchRejectsDuplicateAndEventUsesPreviousHash() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse WHERE id = ? AND deleted = FALSE", Integer.class, 1L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM production_batch WHERE batch_no = ? AND deleted = FALSE", Integer.class, "B-1"))
                .thenReturn(1);
        assertEquals(409, assertThrows(BusinessException.class, () -> service.createBatch(batchRequest(), admin())).getCode());

        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM production_batch WHERE id = ? AND deleted = FALSE", Integer.class, 20L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT COALESCE(MAX(sort_order), 0) FROM production_batch_event WHERE batch_id = ? AND deleted = FALSE", Integer.class, 20L))
                .thenReturn(null);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(20L))).thenReturn(List.of("previous"));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(20L))).thenReturn(31L);

        assertEquals(31L, service.createBatchEvent(20L,
                new CreateBatchEventRequest(" harvest ", " done ", "", " note ", ""),
                admin()));
    }

    @Test
    void createBatchEventRejectsAnonymousOrFarmer() {
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.createBatchEvent(20L, new CreateBatchEventRequest("A", "B", "DONE", "", null), null)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.createBatchEvent(20L, new CreateBatchEventRequest("A", "B", "DONE", "", null), farmer())).getCode());
    }

    @Test
    void updateAndDeleteGreenhouseCoverValidationBranches() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse WHERE id = ? AND deleted = FALSE", Integer.class, 1L))
                .thenReturn(1);
        service.updateGreenhouse(1L, new UpdateGreenhouseRequest("G2", "west", "warning", 20.0, "harvest", 7L));
        verify(jdbcTemplate).update(anyString(), eq(7L), eq("G2"), eq("west"), eq("WARNING"), eq(20.0), eq("harvest"), eq(1L));

        service.updateGreenhouse(1L, new UpdateGreenhouseRequest("G3", "north", "offline", 21.0, "seed", null));
        verify(jdbcTemplate).update(anyString(), eq((Long) null), eq("G3"), eq("north"), eq("OFFLINE"), eq(21.0), eq("seed"), eq(1L));

        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.updateGreenhouse(1L, new UpdateGreenhouseRequest("G2", "west", "bad", 20.0, "harvest", null))).getCode());

        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE greenhouse_id = ? AND status <> 'RESOLVED' AND deleted = FALSE", Integer.class, 1L))
                .thenReturn(0);
        service.deleteGreenhouse(1L);
        verify(jdbcTemplate).update("UPDATE greenhouse SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?", 1L);
    }

    @Test
    void greenhouseAndDeviceDeletesRejectOpenAlerts() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse WHERE id = ? AND deleted = FALSE", Integer.class, 1L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE greenhouse_id = ? AND status <> 'RESOLVED' AND deleted = FALSE", Integer.class, 1L))
                .thenReturn(2);
        assertEquals(409, assertThrows(BusinessException.class, () -> service.deleteGreenhouse(1L)).getCode());

        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Integer.class, 9L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT greenhouse_id FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Long.class, 9L))
                .thenReturn(1L);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(7L))).thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE device_id = ? AND status <> 'RESOLVED' AND deleted = FALSE", Integer.class, 9L))
                .thenReturn(1);
        assertEquals(409, assertThrows(BusinessException.class, () -> service.deleteDevice(9L, farmer())).getCode());
    }

    @Test
    void deviceManagementRequiresOwnedFarmerGreenhouse() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse WHERE id = ? AND deleted = FALSE", Integer.class, 1L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(7L))).thenReturn(1);

        service.createDevice(new CreateDeviceRequest(1L, "fan", "VENT", "north", "ok", true), farmer());
        verify(jdbcTemplate).update(anyString(), eq(1L), eq("fan"), eq("VENT"), eq("north"), eq("ok"), eq(true));

        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Integer.class, 9L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT greenhouse_id FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Long.class, 9L))
                .thenReturn(1L);
        service.updateDevice(9L, new UpdateDeviceRequest(1L, "fan2", "VENT", "running", "south", "", false, 88), farmer());
        verify(jdbcTemplate).update(anyString(), eq(1L), eq("fan2"), eq("VENT"), eq("RUNNING"), eq("south"), eq(""), eq(false), eq(88), eq(9L));

        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE device_id = ? AND status <> 'RESOLVED' AND deleted = FALSE", Integer.class, 9L))
                .thenReturn(0);
        service.deleteDevice(9L, farmer());
        verify(jdbcTemplate).update("UPDATE greenhouse_device SET deleted = TRUE, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?", 9L);
    }

    @Test
    void deviceManagementRejectsAnonymousAdminUnboundAndBadStatus() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse WHERE id = ? AND deleted = FALSE", Integer.class, 1L))
                .thenReturn(1);
        assertEquals(401, assertThrows(BusinessException.class,
                () -> service.createDevice(new CreateDeviceRequest(1L, "fan", "VENT", "", "", false), null)).getCode());
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.createDevice(new CreateDeviceRequest(1L, "fan", "VENT", "", "", false), admin())).getCode());
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(7L))).thenReturn(0);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.createDevice(new CreateDeviceRequest(1L, "fan", "VENT", "", "", false), farmer())).getCode());

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(7L))).thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Integer.class, 9L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject("SELECT greenhouse_id FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Long.class, 9L))
                .thenReturn(1L);
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.updateDevice(9L, new UpdateDeviceRequest(1L, "fan", "VENT", "bad", "", "", false, 1), farmer())).getCode());
    }

    @Test
    void handleAlertResolvesAndDispatchesDeviceCommand() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE id = ? AND deleted = FALSE", Integer.class, 5L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5L), eq(7L))).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5L), eq(9L))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of(alertContext()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);

        service.handleAlert(5L, new HandleAlertRequest("RESOLVED", "", 9L, "stop", "done"), farmer());

        verify(deviceCommandService).execute(eq(new com.morel.greenhouse.application.dto.DeviceCommandRequest(9L, "STOP", "done")), eq(farmer()));
        verify(userAccountService).sendSystemMessage(eq(7L), eq(1L), eq(7L), eq(1L), org.mockito.ArgumentMatchers.contains("done"));
    }

    @Test
    void handleAlertCoversValidationBranches() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE id = ? AND deleted = FALSE", Integer.class, 5L))
                .thenReturn(1);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.handleAlert(5L, new HandleAlertRequest("RESOLVED", "", null, null, "done"), admin())).getCode());

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5L), eq(7L))).thenReturn(1);
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.handleAlert(5L, new HandleAlertRequest("RESOLVED", "", null, null, null), farmer())).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.handleAlert(5L, new HandleAlertRequest("OPEN", "", null, null, "done"), farmer())).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.handleAlert(5L, new HandleAlertRequest("RESOLVED", "", null, null, " "), farmer())).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.handleAlert(5L, new HandleAlertRequest("RESOLVED", "", 9L, " ", "done"), farmer())).getCode());

        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5L), eq(9L))).thenReturn(0);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.handleAlert(5L, new HandleAlertRequest("RESOLVED", "worker", 9L, "stop", "done"), farmer())).getCode());
    }

    @Test
    void handleAlertCanResolveWithoutDeviceCommandUsingCustomHandler() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE id = ? AND deleted = FALSE", Integer.class, 5L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5L), eq(7L))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of(alertContextWithoutDevice()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1L);

        service.handleAlert(5L, new HandleAlertRequest("RESOLVED", "worker", null, null, "done"), farmer());

        verify(userAccountService).sendSystemMessage(eq(7L), eq(1L), eq(7L), eq(1L), org.mockito.ArgumentMatchers.contains("done"));
    }

    @Test
    void recordAlertCommandCanNotifyFarmer() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE id = ? AND deleted = FALSE", Integer.class, 5L))
                .thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of(alertContext()));

        service.recordAlertCommand(5L, new AlertCommandRequest(9L, "STOP", "inspect", true), admin());

        verify(userAccountService).sendSystemMessage(eq(7L), eq(1L), eq(1L), eq(7L), org.mockito.ArgumentMatchers.contains("STOP"));
    }

    @Test
    void recordAlertCommandCoversNoNotifyAnonymousAndMissingFarmer() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE id = ? AND deleted = FALSE", Integer.class, 5L))
                .thenReturn(1);

        service.recordAlertCommand(5L, new AlertCommandRequest(9L, "STOP", null, false), null);
        verify(userAccountService, never()).sendSystemMessage(eq(7L), eq(1L), eq(1L), eq(7L), anyString());

        when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of(Map.of(
                "title", "hot",
                "greenhouse_name", "G1",
                "greenhouse_location", "east"
        )));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.recordAlertCommand(5L, new AlertCommandRequest(9L, "STOP", null, true), null)).getCode());
    }

    @Test
    void alertVisibilityAndMissingResourcesThrowBusinessExceptions() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE id = ? AND deleted = FALSE", Integer.class, 5L))
                .thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(5L), eq(7L))).thenReturn(0);
        assertEquals(403, assertThrows(BusinessException.class,
                () -> service.handleAlert(5L, new HandleAlertRequest("RESOLVED", "", null, null, "done"), farmer())).getCode());

        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse WHERE id = ? AND deleted = FALSE", Integer.class, 99L))
                .thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.updateGreenhouse(99L, new UpdateGreenhouseRequest("G", "L", "ONLINE", 1.0, "", null))).getCode());

        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Integer.class, 98L))
                .thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.updateDevice(98L, new UpdateDeviceRequest(1L, "fan", "VENT", "RUNNING", "", "", false, 1), farmer())).getCode());

        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE id = ? AND deleted = FALSE", Integer.class, 97L))
                .thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.recordAlertCommand(97L, new AlertCommandRequest(null, "STOP", "", false), admin())).getCode());
    }

    @Test
    void recordAlertCommandThrowsWhenAlertContextMissing() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_alert WHERE id = ? AND deleted = FALSE", Integer.class, 5L))
                .thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of());

        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.recordAlertCommand(5L, new AlertCommandRequest(9L, "STOP", null, true), admin())).getCode());
    }

    @Test
    void privateHelpersCoverStatusResourceAndDefaultBranches() throws Exception {
        var normalizeGreenhouseStatus = GreenhouseManagementService.class.getDeclaredMethod("normalizeGreenhouseStatus", String.class);
        normalizeGreenhouseStatus.setAccessible(true);
        assertEquals("ONLINE", normalizeGreenhouseStatus.invoke(service, " online "));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> {
                    try {
                        normalizeGreenhouseStatus.invoke(service, "bad");
                    } catch (java.lang.reflect.InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }).getCode());

        var normalizeDeviceStatus = GreenhouseManagementService.class.getDeclaredMethod("normalizeDeviceStatus", String.class);
        normalizeDeviceStatus.setAccessible(true);
        assertEquals("RUNNING", normalizeDeviceStatus.invoke(service, " running "));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> {
                    try {
                        normalizeDeviceStatus.invoke(service, "bad");
                    } catch (java.lang.reflect.InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }).getCode());

        var stringValue = GreenhouseManagementService.class.getDeclaredMethod("stringValue", Object.class, String.class);
        stringValue.setAccessible(true);
        assertEquals("fallback", stringValue.invoke(service, new Object[]{null, "fallback"}));
        assertEquals("fallback", stringValue.invoke(service, " ", "fallback"));
        assertEquals("value", stringValue.invoke(service, "value", "fallback"));

        var blankToDefault = GreenhouseManagementService.class.getDeclaredMethod("blankToDefault", String.class, String.class);
        blankToDefault.setAccessible(true);
        assertEquals("fallback", blankToDefault.invoke(service, new Object[]{null, "fallback"}));
        assertEquals("fallback", blankToDefault.invoke(service, " ", "fallback"));
        assertEquals("value", blankToDefault.invoke(service, " value ", "fallback"));

        var emptyToNull = GreenhouseManagementService.class.getDeclaredMethod("emptyToNull", String.class);
        emptyToNull.setAccessible(true);
        assertEquals(null, emptyToNull.invoke(service, new Object[]{null}));
        assertEquals(null, emptyToNull.invoke(service, " "));
        assertEquals("img.png", emptyToNull.invoke(service, " img.png "));
    }

    @Test
    void privateResourceChecksCoverExistsNullAndZeroBranches() throws Exception {
        var ensureGreenhouseExists = GreenhouseManagementService.class.getDeclaredMethod("ensureGreenhouseExists", Long.class);
        ensureGreenhouseExists.setAccessible(true);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse WHERE id = ? AND deleted = FALSE", Integer.class, 1L))
                .thenReturn(1);
        ensureGreenhouseExists.invoke(service, 1L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse WHERE id = ? AND deleted = FALSE", Integer.class, 2L))
                .thenReturn(0);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> {
                    try {
                        ensureGreenhouseExists.invoke(service, 2L);
                    } catch (java.lang.reflect.InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }).getCode());

        var ensureDeviceExists = GreenhouseManagementService.class.getDeclaredMethod("ensureDeviceExists", Long.class);
        ensureDeviceExists.setAccessible(true);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM greenhouse_device WHERE id = ? AND deleted = FALSE", Integer.class, 3L))
                .thenReturn(0);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> {
                    try {
                        ensureDeviceExists.invoke(service, 3L);
                    } catch (java.lang.reflect.InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }).getCode());

        var ensureBatchExists = GreenhouseManagementService.class.getDeclaredMethod("ensureBatchExists", Long.class);
        ensureBatchExists.setAccessible(true);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM production_batch WHERE id = ? AND deleted = FALSE", Integer.class, 4L))
                .thenReturn(1);
        ensureBatchExists.invoke(service, 4L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM production_batch WHERE id = ? AND deleted = FALSE", Integer.class, 5L))
                .thenReturn(null);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> {
                    try {
                        ensureBatchExists.invoke(service, 5L);
                    } catch (java.lang.reflect.InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }).getCode());
    }

    private CreateBatchRequest batchRequest() {
        return new CreateBatchRequest(1L, "B-1", "Batch 1", "", "", "2026-01-01", "", "");
    }

    private Map<String, Object> alertContext() {
        return Map.of(
                "title", "hot",
                "greenhouse_name", "G1",
                "greenhouse_location", "east",
                "owner_user_id", 7L,
                "farmer_name", "farmer",
                "device_name", "fan"
        );
    }

    private Map<String, Object> alertContextWithoutDevice() {
        return Map.of(
                "title", "hot",
                "greenhouse_name", "G1",
                "greenhouse_location", "east",
                "owner_user_id", 7L,
                "farmer_name", "farmer"
        );
    }

    private CurrentUser admin() {
        return new CurrentUser(1L, "admin1", "ADMIN");
    }

    private CurrentUser farmer() {
        return new CurrentUser(7L, "farmer", "FARMER");
    }
}
