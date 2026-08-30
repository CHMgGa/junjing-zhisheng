package com.morel.greenhouse.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelemetryAlertServiceTest {
    private JdbcTemplate jdbcTemplate;
    private TelemetryAlertService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new TelemetryAlertService(jdbcTemplate);
    }

    @Test
    void evaluateIgnoresMissingTelemetry() {
        service.evaluate(null, Map.of("temperature", 30));
        service.evaluate(1L, null);
        service.evaluate(1L, Map.of());

        verify(jdbcTemplate, never()).queryForList(anyString(), eq(1L));
    }

    @Test
    void evaluateCreatesAlertWhenRuleMatchesAliasMetric() {
        when(jdbcTemplate.queryForList(anyString(), eq(1L))).thenReturn(List.of(
                rule(11L, "High temperature", "temperature", "GT", BigDecimal.valueOf(30), "CRITICAL")
        )).thenReturn(List.of(context()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(11L))).thenReturn(0);

        service.evaluate(1L, Map.of("air_temperature", "31.5"));

        verify(jdbcTemplate).update(anyString(), eq(1L), eq(11L), eq("High temperature"), anyString(), eq("CRITICAL"));
    }

    @Test
    void evaluateSkipsExistingOpenAlertAndInvalidMetricValue() {
        when(jdbcTemplate.queryForList(anyString(), eq(1L))).thenReturn(List.of(
                rule(11L, "High humidity", "humidity", ">=", 80, "WARNING"),
                rule(12L, "Bad metric", "ph_value", "<", 5, "WARNING")
        ));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L), eq(11L))).thenReturn(1);

        service.evaluate(1L, Map.of("air_humidity", 80, "ph_value", "not-a-number"));

        verify(jdbcTemplate, never()).update(anyString(), eq(1L), eq(11L), anyString(), anyString(), anyString());
    }

    @Test
    void evaluateCoversOperatorsFallbackNamesAndNoAdminMessageBranch() {
        when(jdbcTemplate.queryForList(anyString(), eq(2L))).thenReturn(List.of(
                rule(1L, "", "soil_moisture", "LTE", 20, "WARNING"),
                rule(2L, "Equal co2", "co2_ppm", "=", 500, ""),
                rule(3L, "Unknown", "light_lux", "BAD", 0, "INFO")
        )).thenReturn(List.of(Map.of("greenhouse_name", "G2")));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(2L), eq(1L))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(2L), eq(2L))).thenReturn(0);

        service.evaluate(2L, Map.of("soil_humidity", 18, "co2_ppm", 500, "light_lux", 1));

        verify(jdbcTemplate).update(anyString(), eq(2L), eq(1L), anyString(), anyString(), eq("WARNING"));
        verify(jdbcTemplate).update(anyString(), eq(2L), eq(2L), eq("Equal co2"), anyString(), eq("WARNING"));
    }

    @Test
    void evaluateCoversRemainingOperatorsMetricsAndNullValues() {
        Map<String, Object> nullRuleName = new HashMap<>();
        nullRuleName.put("id", 21L);
        nullRuleName.put("rule_name", null);
        nullRuleName.put("metric_key", "soil_temperature");
        nullRuleName.put("operator", "<");
        nullRuleName.put("threshold_value", BigDecimal.valueOf(19));
        nullRuleName.put("level", "WARNING");

        Map<String, Object> nullMetric = new HashMap<>();
        nullMetric.put("id", 22L);
        nullMetric.put("rule_name", "Null metric");
        nullMetric.put("metric_key", null);
        nullMetric.put("operator", "GT");
        nullMetric.put("threshold_value", 1);
        nullMetric.put("level", "INFO");

        when(jdbcTemplate.queryForList(anyString(), eq(3L))).thenReturn(List.of(
                nullRuleName,
                rule(23L, "Air humidity low", "air_humidity", "LT", 40, "WARNING"),
                rule(24L, "Ph equal miss", "ph_value", "EQ", 7.2, "WARNING"),
                nullMetric
        )).thenReturn(List.of(context())).thenReturn(List.of(context()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(3L), eq(21L))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(3L), eq(23L))).thenReturn(0);

        Map<String, Object> telemetry = new HashMap<>();
        telemetry.put("soil_temperature", BigDecimal.valueOf(18.5));
        telemetry.put("air_humidity", 39);
        telemetry.put("ph_value", 7.0);
        service.evaluate(3L, telemetry);

        verify(jdbcTemplate).update(anyString(), eq(3L), eq(21L), anyString(), anyString(), eq("WARNING"));
        verify(jdbcTemplate).update(anyString(), eq(3L), eq(23L), eq("Air humidity low"), anyString(), eq("WARNING"));
    }

    @Test
    void evaluateCoversRemainingMetricNamesAndStringConversions() {
        when(jdbcTemplate.queryForList(anyString(), eq(4L))).thenReturn(List.of(
                rule("31", "Low ph", "ph_value", "GT", "6.0", "WARNING"),
                rule(32L, "Bright", "light_lux", ">", BigDecimal.valueOf(1000), "INFO"),
                rule(33L, "Custom", "leaf_wetness", "GT", "0.5", "WARNING")
        )).thenReturn(List.of(context())).thenReturn(List.of(context())).thenReturn(List.of(context()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(4L), eq(31L))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(4L), eq(32L))).thenReturn(0);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(4L), eq(33L))).thenReturn(0);

        service.evaluate(4L, Map.of("ph_value", "6.8", "light_lux", 1200, "leaf_wetness", "0.7"));

        verify(jdbcTemplate).update(anyString(), eq(4L), eq(31L), eq("Low ph"), anyString(), eq("WARNING"));
        verify(jdbcTemplate).update(anyString(), eq(4L), eq(32L), eq("Bright"), anyString(), eq("INFO"));
        verify(jdbcTemplate).update(anyString(), eq(4L), eq(33L), eq("Custom"), anyString(), eq("WARNING"));
    }

    @Test
    void privateOperatorTextDefaultIsCoveredForUnknownOperator() throws Exception {
        Method method = TelemetryAlertService.class.getDeclaredMethod("operatorText", String.class);
        method.setAccessible(true);

        org.junit.jupiter.api.Assertions.assertEquals("触发", method.invoke(service, "UNKNOWN"));
    }

    @Test
    void privateHelpersCoverRemainingSwitchAndFallbackBranches() throws Exception {
        Method matches = TelemetryAlertService.class.getDeclaredMethod("matches", double.class, String.class, double.class);
        matches.setAccessible(true);
        org.junit.jupiter.api.Assertions.assertEquals(false, matches.invoke(service, 1.0, null, 1.0));
        org.junit.jupiter.api.Assertions.assertEquals(false, matches.invoke(service, 1.0, "GT", 2.0));
        org.junit.jupiter.api.Assertions.assertEquals(true, matches.invoke(service, 2.0, "GTE", 2.0));
        org.junit.jupiter.api.Assertions.assertEquals(false, matches.invoke(service, 2.0, "LT", 1.0));
        org.junit.jupiter.api.Assertions.assertEquals(true, matches.invoke(service, 1.0, "LTE", 1.0));
        org.junit.jupiter.api.Assertions.assertEquals(false, matches.invoke(service, 1.0, "EQ", 2.0));

        Method metricValue = TelemetryAlertService.class.getDeclaredMethod("metricValue", Map.class, String.class);
        metricValue.setAccessible(true);
        org.junit.jupiter.api.Assertions.assertEquals(70.0, (double) metricValue.invoke(service, Map.of("humidity", 70), "humidity"));
        org.junit.jupiter.api.Assertions.assertEquals(null, metricValue.invoke(service, new Object[]{new HashMap<>(), null}));

        Method metricName = TelemetryAlertService.class.getDeclaredMethod("metricName", String.class);
        metricName.setAccessible(true);
        org.junit.jupiter.api.Assertions.assertEquals("空气温度", metricName.invoke(service, "air_temperature"));
        org.junit.jupiter.api.Assertions.assertEquals("空气湿度", metricName.invoke(service, "humidity"));

    }

    private Map<String, Object> rule(Object id, String name, String metric, String operator, Object threshold, String level) {
        return Map.of(
                "id", id,
                "rule_name", name,
                "metric_key", metric,
                "operator", operator,
                "threshold_value", threshold,
                "level", level
        );
    }

    private Map<String, Object> context() {
        return Map.of(
                "greenhouse_name", "G1",
                "owner_user_id", 7L
        );
    }
}
