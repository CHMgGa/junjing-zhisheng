package com.morel.greenhouse.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morel.greenhouse.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HuaweiIotIngestionServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private JdbcTemplate jdbcTemplate;
    private TelemetryAlertService telemetryAlertService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        telemetryAlertService = mock(TelemetryAlertService.class);
    }

    @Test
    void ingestStoresTelemetryFromMappedDeviceAndUpdatesDeviceStatus() throws Exception {
        HuaweiIotIngestionService service = new HuaweiIotIngestionService(
                jdbcTemplate, telemetryAlertService, "", "device-1:7");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(7L))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(7L))).thenReturn(List.of(Map.of(
                "soil_temperature", 18.0,
                "soil_humidity", "61.5",
                "ph_value", "bad",
                "co2_ppm", 780
        )));
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(7L))).thenReturn(null);

        Map<String, Object> result = service.ingest(objectMapper.readTree("""
                {
                  "device_id": "device-1",
                  "data": {
                    "Temp": "22.4",
                    "Humi": "83.2",
                    "Lumi": "60",
                    "LampST": "ON",
                    "Fengd": "3"
                  }
                }
                """));

        assertEquals(7L, result.get("greenhouse_id"));
        assertEquals(22.4, result.get("air_temperature"));
        assertEquals(4200, result.get("light_lux"));
        verify(telemetryAlertService).evaluate(eq(7L), any());
        verify(jdbcTemplate, times(2)).update(anyString(), eq("RUNNING"), eq(7L));
    }

    @Test
    void ingestUsesDeviceLookupAndUpdatesExistingTelemetry() throws Exception {
        HuaweiIotIngestionService service = new HuaweiIotIngestionService(
                jdbcTemplate, telemetryAlertService, "", "");
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq("serial-1"))).thenAnswer(invocation -> {
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getLong("greenhouse_id")).thenReturn(8L);
            return ((ResultSetExtractor<?>) invocation.getArgument(1)).extractData(rs);
        });
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(8L))).thenAnswer(invocation -> {
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(true);
            when(rs.getLong("id")).thenReturn(99L);
            return ((ResultSetExtractor<?>) invocation.getArgument(1)).extractData(rs);
        });
        when(jdbcTemplate.queryForList(anyString(), eq(8L))).thenReturn(List.of());

        Map<String, Object> result = service.ingest(objectMapper.readTree("""
                {
                  "notify_data": {
                    "header": {"device_id": "serial-1"},
                    "body": {"services": [{"properties": {
                      "temperature": 20.0,
                      "humidity": 80.0,
                      "light": 4300,
                      "soilTemperature": 18.8,
                      "soilHumidity": 60.1,
                      "ph": 6.6,
                      "co2": 755
                    }}]}
                  }
                }
                """));

        assertEquals(8L, result.get("greenhouse_id"));
        verify(jdbcTemplate).update(anyString(), eq(20.0), eq(80.0), eq(20.0), eq(80.0),
                eq(18.8), eq(60.1), eq(6.6), eq(4300), eq(755), eq(60.1), eq(99L));
    }

    @Test
    void ingestRejectsInvalidPayloadsAndMappings() throws Exception {
        HuaweiIotIngestionService noDefault = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "");
        assertEquals(400, assertThrows(BusinessException.class, () -> noDefault.ingest(null)).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> noDefault.ingest(objectMapper.readTree("{}"))).getCode());

        HuaweiIotIngestionService badMap = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "device-1:not-number");
        assertEquals(500, assertThrows(BusinessException.class,
                () -> badMap.ingest(objectMapper.readTree("{\"device_id\":\"device-1\",\"data\":{\"Temp\":1,\"Humi\":2}}"))).getCode());

        HuaweiIotIngestionService missingGreenhouse = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "device-1:99");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(99L))).thenReturn(0);
        assertEquals(404, assertThrows(BusinessException.class,
                () -> missingGreenhouse.ingest(objectMapper.readTree("{\"device_id\":\"device-1\",\"data\":{\"Temp\":1,\"Humi\":2}}"))).getCode());
    }

    @Test
    void ingestRejectsMissingDataAndInvalidNumbers() throws Exception {
        HuaweiIotIngestionService service = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "default-device", "");
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq("default-device"))).thenReturn(1L);
        when(jdbcTemplate.queryForList(anyString(), eq(1L))).thenReturn(List.of());

        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.ingest(objectMapper.readTree("{\"device_id\":\"default-device\"}"))).getCode());

        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.ingest(objectMapper.readTree("{\"device_id\":\"default-device\",\"data\":{\"Temp\":\"bad\",\"Humi\":80}}"))).getCode());
    }

    @Test
    void ingestRejectsUnknownDeviceAndMissingRequiredHumidity() throws Exception {
        HuaweiIotIngestionService service = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "other:1");
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq("device-404"))).thenReturn(null);

        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.ingest(objectMapper.readTree("{\"device_id\":\"device-404\",\"data\":{\"Temp\":21,\"Humi\":80}}"))).getCode());

        HuaweiIotIngestionService mapped = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "device-1:1");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L))).thenReturn(1);
        assertEquals(400, assertThrows(BusinessException.class,
                () -> mapped.ingest(objectMapper.readTree("{\"device_id\":\"device-1\",\"data\":{\"Temp\":21}}"))).getCode());
    }

    @Test
    void ingestAcceptsPropertiesPayloadAndStoppedDeviceStatuses() throws Exception {
        HuaweiIotIngestionService service = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "device-1:3");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(3L))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(3L))).thenReturn(List.of());
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(3L))).thenReturn(null);

        Map<String, Object> result = service.ingest(objectMapper.readTree("""
                {
                  "device_id": "device-1",
                  "properties": {
                    "temperature": 19,
                    "humidity": 70,
                    "lightLux": 3000,
                    "LampST": "OFF",
                    "fanLevel": 0
                  }
                }
                """));

        assertEquals(3L, result.get("greenhouse_id"));
        verify(jdbcTemplate, times(2)).update(anyString(), eq("STOPPED"), eq(3L));
    }

    @Test
    void privateHelpersCoverLightAndClampBranches() throws Exception {
        HuaweiIotIngestionService service = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "");

        var normalize = HuaweiIotIngestionService.class.getDeclaredMethod("normalizeLightLux", Double.class);
        normalize.setAccessible(true);
        assertEquals(4200, normalize.invoke(service, new Object[]{null}));
        assertEquals(700, normalize.invoke(service, 10.0));
        assertEquals(120, normalize.invoke(service, 120.0));

        var latest = HuaweiIotIngestionService.class.getDeclaredMethod("latestValue", Map.class, String.class, double.class);
        latest.setAccessible(true);
        assertEquals(1.5, (double) latest.invoke(service, Map.of("x", 1.5), "x", 2.0));
        assertEquals(2.0, (double) latest.invoke(service, Map.of("x", "bad"), "x", 2.0));
        assertEquals(2.0, (double) latest.invoke(service, Map.of(), "x", 2.0));

        var clamp = HuaweiIotIngestionService.class.getDeclaredMethod("clamp", double.class, double.class, double.class);
        clamp.setAccessible(true);
        assertEquals(1.0, (double) clamp.invoke(service, 0.0, 1.0, 3.0));
        assertEquals(3.0, (double) clamp.invoke(service, 4.0, 1.0, 3.0));
        assertEquals(2.0, (double) clamp.invoke(service, 2.0, 1.0, 3.0));
    }

    @Test
    void ingestUsesDefaultDeviceAndHandlesBlankDeviceStatusValues() throws Exception {
        HuaweiIotIngestionService service = new HuaweiIotIngestionService(
                jdbcTemplate, telemetryAlertService, "fallback-device", "");
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq("fallback-device"))).thenReturn(5L);
        when(jdbcTemplate.queryForList(anyString(), eq(5L))).thenReturn(List.of());
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(5L))).thenReturn(null);

        Map<String, Object> result = service.ingest(objectMapper.readTree("""
                {
                  "device_id": " ",
                  "data": {
                    "Temp": 22,
                    "Humi": 81,
                    "Lumi": 101,
                    "LampST": " ",
                    "Fengd": null
                  }
                }
                """));

        assertEquals("fallback-device", result.get("device_id"));
        assertEquals(101, result.get("light_lux"));
    }

    @Test
    void ingestCoversNullNodesAlternateDeviceKeysAndMissingNestedProperties() throws Exception {
        HuaweiIotIngestionService noDefault = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "");

        assertEquals(400, assertThrows(BusinessException.class,
                () -> noDefault.ingest(objectMapper.readTree("null"))).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> noDefault.ingest(objectMapper.readTree("""
                        {"notify_data":{"header":{"nodeId":""},"body":{"services":[]}}}
                        """))).getCode());

        HuaweiIotIngestionService mapped = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "node-2:12");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(12L))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(12L))).thenReturn(List.of());
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(12L))).thenReturn(null);

        Map<String, Object> result = mapped.ingest(objectMapper.readTree("""
                {
                  "nodeId": "node-2",
                  "data": {
                    "Temp": 23,
                    "Humi": 75,
                    "SoilTemp": null,
                    "SoilHumi": "",
                    "PH": " ",
                    "CO2": null
                  }
                }
                """));

        assertEquals(12L, result.get("greenhouse_id"));
    }

    @Test
    void privateHelpersCoverMissingNullAndInvalidTextBranches() throws Exception {
        HuaweiIotIngestionService service = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "");

        var firstText = HuaweiIotIngestionService.class.getDeclaredMethod("firstText", com.fasterxml.jackson.databind.JsonNode.class, String[].class);
        firstText.setAccessible(true);
        assertEquals(null, firstText.invoke(service, new Object[]{null, new String[]{"x"}}));
        assertEquals(null, firstText.invoke(service, objectMapper.readTree("{\"x\":null,\"y\":\" \"}"), new String[]{"x", "y"}));
        assertEquals("abc", firstText.invoke(service, objectMapper.readTree("{\"x\":\" abc \"}"), new String[]{"x"}));

        var optionalDouble = HuaweiIotIngestionService.class.getDeclaredMethod("optionalDouble", com.fasterxml.jackson.databind.JsonNode.class, String[].class);
        optionalDouble.setAccessible(true);
        assertEquals(null, optionalDouble.invoke(service, objectMapper.readTree("{}"), new String[]{"x"}));
        assertEquals(12.5, (double) optionalDouble.invoke(service, objectMapper.readTree("{\"x\":\"12.5\"}"), new String[]{"x"}));
        assertEquals(400, assertThrows(BusinessException.class,
                () -> {
                    try {
                        optionalDouble.invoke(service, objectMapper.readTree("{\"x\":\"bad\"}"), new String[]{"x"});
                    } catch (java.lang.reflect.InvocationTargetException exception) {
                        throw exception.getCause();
                    }
                }).getCode());

        var valueOrEstimate = HuaweiIotIngestionService.class.getDeclaredMethod("valueOrEstimate", Double.class, double.class);
        valueOrEstimate.setAccessible(true);
        assertEquals(9.0, (double) valueOrEstimate.invoke(service, 9.0, 2.0));
        assertEquals(2.0, (double) valueOrEstimate.invoke(service, new Object[]{null, 2.0}));
    }

    @Test
    void ingestExecutesExtractorFalseBranchesAndNullDataFallbacks() throws Exception {
        HuaweiIotIngestionService service = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "device-null:15");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(15L))).thenReturn(1);
        when(jdbcTemplate.queryForList(anyString(), eq(15L))).thenReturn(List.of());
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(15L))).thenAnswer(invocation -> {
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(false);
            return ((ResultSetExtractor<?>) invocation.getArgument(1)).extractData(rs);
        });

        Map<String, Object> result = service.ingest(objectMapper.readTree("""
                {
                  "device_id": "device-null",
                  "data": null,
                  "properties": {
                    "Temp": 24,
                    "Humi": 76,
                    "LampST": null
                  }
                }
                """));

        assertEquals(15L, result.get("greenhouse_id"));

        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.ingest(objectMapper.readTree("""
                        {
                          "device_id": "device-null",
                          "data": null,
                          "properties": null,
                          "notify_data": {"body": {"services": [{"properties": null}]}}
                        }
                        """))).getCode());
    }

    @Test
    void ingestExecutesDeviceLookupFalseExtractorBranch() throws Exception {
        HuaweiIotIngestionService service = new HuaweiIotIngestionService(jdbcTemplate, telemetryAlertService, "", "");
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq("missing-device"))).thenAnswer(invocation -> {
            ResultSet rs = mock(ResultSet.class);
            when(rs.next()).thenReturn(false);
            return ((ResultSetExtractor<?>) invocation.getArgument(1)).extractData(rs);
        });

        assertEquals(404, assertThrows(BusinessException.class,
                () -> service.ingest(objectMapper.readTree("""
                        {"device_id":"missing-device","data":{"Temp":21,"Humi":80}}
                        """))).getCode());
    }
}
