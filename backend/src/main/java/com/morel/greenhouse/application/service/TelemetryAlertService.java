package com.morel.greenhouse.application.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TelemetryAlertService {
    private final JdbcTemplate jdbcTemplate;

    public TelemetryAlertService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void evaluate(Long greenhouseId, Map<String, Object> telemetry) {
        if (greenhouseId == null || telemetry == null || telemetry.isEmpty()) {
            return;
        }
        List<Map<String, Object>> rules = jdbcTemplate.queryForList("""
                SELECT id, rule_name, metric_key, operator, threshold_value, level, description
                FROM alert_rule
                WHERE enabled = TRUE
                  AND deleted = FALSE
                  AND (greenhouse_id IS NULL OR greenhouse_id = ?)
                ORDER BY greenhouse_id IS NULL, greenhouse_id, id
                """, greenhouseId);
        for (Map<String, Object> rule : rules) {
            String metricKey = stringValue(rule.get("metric_key"));
            Double value = metricValue(telemetry, metricKey);
            if (value == null) {
                continue;
            }
            double threshold = doubleValue(rule.get("threshold_value"));
            if (matches(value, stringValue(rule.get("operator")), threshold)) {
                createAlertIfAbsent(greenhouseId, rule, value, threshold);
            }
        }
    }

    private void createAlertIfAbsent(Long greenhouseId, Map<String, Object> rule, double value, double threshold) {
        Long ruleId = longValue(rule.get("id"));
        Integer exists = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM greenhouse_alert
                WHERE greenhouse_id = ?
                  AND rule_id = ?
                  AND status <> 'RESOLVED'
                  AND deleted = FALSE
                """, Integer.class, greenhouseId, ruleId);
        if (exists != null && exists > 0) {
            return;
        }

        Map<String, Object> context = alertContext(greenhouseId);
        String metricName = metricName(stringValue(rule.get("metric_key")));
        String operatorText = operatorText(stringValue(rule.get("operator")));
        String ruleName = stringValue(rule.get("rule_name"));
        String level = stringValue(rule.get("level"), "WARNING");
        String title = ruleName == null || ruleName.isBlank() ? metricName + "异常" : ruleName;
        String description = "%s当前值 %.2f，%s阈值 %.2f，请及时检查%s。".formatted(
                metricName,
                value,
                operatorText,
                threshold,
                stringValue(context.get("greenhouse_name"), "大棚")
        );

        jdbcTemplate.update("""
                INSERT INTO greenhouse_alert(greenhouse_id, rule_id, title, description, level, status, created_by)
                VALUES (?, ?, ?, ?, ?, 'OPEN', 'iot-auto')
                """, greenhouseId, ruleId, title, description, level);

    }

    private Map<String, Object> alertContext(Long greenhouseId) {
        return jdbcTemplate.queryForList("""
                SELECT g.id, g.name AS greenhouse_name, COALESCE(g.owner_user_id, b.farmer_user_id) AS owner_user_id
                FROM greenhouse g
                LEFT JOIN farmer_greenhouse_binding b ON b.greenhouse_id = g.id AND b.deleted = FALSE
                WHERE g.id = ? AND g.deleted = FALSE
                """, greenhouseId).stream().findFirst().orElseGet(HashMap::new);
    }

    private boolean matches(double value, String operator, double threshold) {
        return switch (operator == null ? "" : operator.toUpperCase()) {
            case "GT", ">" -> value > threshold;
            case "GTE", ">=" -> value >= threshold;
            case "LT", "<" -> value < threshold;
            case "LTE", "<=" -> value <= threshold;
            case "EQ", "=" -> Double.compare(value, threshold) == 0;
            default -> false;
        };
    }

    private Double metricValue(Map<String, Object> telemetry, String metricKey) {
        Object raw = telemetry.get(metricKey);
        if (raw == null) {
            raw = switch (metricKey == null ? "" : metricKey) {
                case "temperature" -> telemetry.get("air_temperature");
                case "humidity" -> telemetry.get("air_humidity");
                case "soil_moisture" -> telemetry.get("soil_humidity");
                default -> null;
            };
        }
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.valueOf(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String metricName(String metricKey) {
        return switch (metricKey == null ? "" : metricKey) {
            case "temperature", "air_temperature" -> "空气温度";
            case "humidity", "air_humidity" -> "空气湿度";
            case "soil_temperature" -> "土壤温度";
            case "soil_humidity", "soil_moisture" -> "土壤湿度";
            case "ph_value" -> "pH值";
            case "co2_ppm" -> "二氧化碳";
            case "light_lux" -> "光照强度";
            default -> metricKey;
        };
    }

    private String operatorText(String operator) {
        return switch (operator == null ? "" : operator.toUpperCase()) {
            case "GT", "GTE", ">", ">=" -> "超过";
            case "LT", "LTE", "<", "<=" -> "低于";
            case "EQ", "=" -> "等于";
            default -> "触发";
        };
    }

    private String stringValue(Object value) {
        return stringValue(value, null);
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private double doubleValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.doubleValue();
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }
}
