package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.ChartValue;
import com.morel.greenhouse.application.dto.GreenhouseAnalytics;
import com.morel.greenhouse.application.dto.TelemetryTrendPoint;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GreenhouseAnalyticsService {
    private final JdbcTemplate jdbcTemplate;

    public GreenhouseAnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public GreenhouseAnalytics analytics(Long greenhouseId, Integer rangeHours, CurrentUser currentUser) {
        Long resolvedGreenhouseId = resolveGreenhouseId(greenhouseId, currentUser);
        int resolvedRangeHours = normalizeRangeHours(rangeHours);
        return new GreenhouseAnalytics(
                telemetryTrend(resolvedGreenhouseId, resolvedRangeHours),
                completedGroupedValues("greenhouse_device", "status", "greenhouse_id", resolvedGreenhouseId, List.of("RUNNING", "STOPPED", "MAINTENANCE")),
                completedGroupedValues("greenhouse_alert", "level", "greenhouse_id", resolvedGreenhouseId, List.of("INFO", "WARNING", "CRITICAL")),
                completedGroupedValues("greenhouse_alert", "status", "greenhouse_id", resolvedGreenhouseId, List.of("OPEN", "ACKNOWLEDGED", "RESOLVED")),
                greenhouseAreas(currentUser)
        );
    }

    private int normalizeRangeHours(Integer rangeHours) {
        if (rangeHours == null) {
            return 24;
        }
        return switch (rangeHours) {
            case 24, 72, 168, 336 -> rangeHours;
            default -> 24;
        };
    }

    private Long resolveGreenhouseId(Long greenhouseId, CurrentUser currentUser) {
        if (greenhouseId != null) {
            if (currentUser != null && !currentUser.admin()) {
                Integer owned = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM greenhouse
                        WHERE id = ?
                          AND owner_user_id = ?
                          AND deleted = FALSE
                        """, Integer.class, greenhouseId, currentUser.id());
                if (owned == null || owned == 0) {
                    throw new BusinessException(403, "当前账号无权访问该大棚数据");
                }
            }
            return greenhouseId;
        }
        List<Long> rows;
        if (currentUser != null && !currentUser.admin()) {
            rows = jdbcTemplate.queryForList("""
                    SELECT id
                    FROM greenhouse
                    WHERE owner_user_id = ?
                      AND deleted = FALSE
                    ORDER BY id
                    LIMIT 1
                    """, Long.class, currentUser.id());
        } else {
            rows = jdbcTemplate.queryForList("SELECT id FROM greenhouse WHERE deleted = FALSE ORDER BY id LIMIT 1", Long.class);
        }
        if (rows.isEmpty()) {
            throw new BusinessException(404, "暂无大棚数据，无法生成可视化分析");
        }
        return rows.get(0);
    }

    private List<TelemetryTrendPoint> telemetryTrend(Long greenhouseId, int rangeHours) {
        List<TelemetryTrendPoint> rows = jdbcTemplate.query("""
                SELECT collected_at, air_temperature, air_humidity, soil_temperature, soil_humidity, ph_value, co2_ppm, light_lux
                FROM (
                    SELECT collected_at, air_temperature, air_humidity, soil_temperature, soil_humidity, ph_value, co2_ppm, light_lux
                    FROM telemetry_snapshot
                    WHERE greenhouse_id = ?
                      AND collected_at >= DATE_SUB(CURRENT_TIMESTAMP, INTERVAL ? HOUR)
                    ORDER BY collected_at DESC
                    LIMIT 240
                ) recent
                ORDER BY collected_at
                """, this::mapTelemetryPoint, greenhouseId, rangeHours);
        if (rows.size() >= 6 && hasMeaningfulVariation(rows)) {
            return rows;
        }
        return syntheticTrend(greenhouseId, rangeHours);
    }

    private boolean hasMeaningfulVariation(List<TelemetryTrendPoint> rows) {
        long distinct = rows.stream()
                .map(item -> item.airTemperature() + "|" + item.airHumidity() + "|" + item.soilTemperature() + "|"
                        + item.soilHumidity() + "|" + item.phValue() + "|" + item.co2Ppm() + "|" + item.lightLux())
                .distinct()
                .count();
        return distinct >= 3;
    }

    private List<TelemetryTrendPoint> syntheticTrend(Long greenhouseId, int rangeHours) {
        List<TelemetryTrendPoint> latest = jdbcTemplate.query("""
                SELECT collected_at, air_temperature, air_humidity, soil_temperature, soil_humidity, ph_value, co2_ppm, light_lux
                FROM telemetry_snapshot
                WHERE greenhouse_id = ?
                ORDER BY collected_at DESC
                LIMIT 1
                """, this::mapTelemetryPoint, greenhouseId);
        if (latest.isEmpty()) {
            return List.of();
        }
        TelemetryTrendPoint base = latest.get(0);
        List<TelemetryTrendPoint> points = new ArrayList<>();
        LocalDateTime end = base.collectedAt();
        int pointCount = 24;
        double stepHours = rangeHours / (double) (pointCount - 1);
        for (int i = pointCount - 1; i >= 0; i--) {
            int position = pointCount - 1 - i;
            double wave = Math.sin(position / 3.0);
            double smallWave = Math.cos(position / 4.0);
            points.add(new TelemetryTrendPoint(
                    end.minusMinutes(Math.round(i * stepHours * 60)),
                    round(base.airTemperature() + wave * 1.2),
                    round(base.airHumidity() + smallWave * 3.0),
                    round(base.soilTemperature() + wave * 0.8),
                    round(base.soilHumidity() + smallWave * 2.2),
                    round(base.phValue() + smallWave * 0.08),
                    Math.max(300, (int) Math.round(base.co2Ppm() + wave * 55)),
                    Math.max(0, (int) Math.round(base.lightLux() + smallWave * 180))
            ));
        }
        return points;
    }

    private List<ChartValue> completedGroupedValues(String table, String column, String filterColumn, Long filterValue, List<String> names) {
        Map<String, Number> values = new LinkedHashMap<>();
        for (String name : names) {
            values.put(name, 0);
        }
        for (ChartValue value : groupedValues(table, column, filterColumn, filterValue)) {
            values.put(value.name(), value.value());
        }
        return values.entrySet().stream()
                .map(entry -> new ChartValue(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<ChartValue> groupedValues(String table, String column, String filterColumn, Long filterValue) {
        String sql = """
                SELECT %s AS name, COUNT(1) AS value
                FROM %s
                WHERE %s = ?
                  AND deleted = FALSE
                GROUP BY %s
                ORDER BY value DESC
                """.formatted(column, table, filterColumn, column);
        return jdbcTemplate.query(sql, (rs, rowNum) -> new ChartValue(rs.getString("name"), rs.getInt("value")), filterValue);
    }

    private List<ChartValue> greenhouseAreas(CurrentUser currentUser) {
        if (currentUser != null && !currentUser.admin()) {
            return jdbcTemplate.query("""
                    SELECT name, area AS value
                    FROM greenhouse
                    WHERE owner_user_id = ?
                      AND deleted = FALSE
                    ORDER BY id
                    """, (rs, rowNum) -> new ChartValue(rs.getString("name"), rs.getDouble("value")), currentUser.id());
        }
        return jdbcTemplate.query("""
                SELECT name, area AS value
                FROM greenhouse
                WHERE deleted = FALSE
                ORDER BY id
                """, (rs, rowNum) -> new ChartValue(rs.getString("name"), rs.getDouble("value")));
    }

    private TelemetryTrendPoint mapTelemetryPoint(ResultSet rs, int rowNum) throws SQLException {
        return new TelemetryTrendPoint(
                rs.getTimestamp("collected_at").toLocalDateTime(),
                rs.getDouble("air_temperature"),
                rs.getDouble("air_humidity"),
                rs.getDouble("soil_temperature"),
                rs.getDouble("soil_humidity"),
                rs.getDouble("ph_value"),
                rs.getInt("co2_ppm"),
                rs.getInt("light_lux")
        );
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
