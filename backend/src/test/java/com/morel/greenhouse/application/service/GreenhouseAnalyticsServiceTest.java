package com.morel.greenhouse.application.service;

import com.morel.greenhouse.application.dto.ChartValue;
import com.morel.greenhouse.application.dto.GreenhouseAnalytics;
import com.morel.greenhouse.application.dto.TelemetryTrendPoint;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GreenhouseAnalyticsServiceTest {
    private JdbcTemplate jdbcTemplate;
    private GreenhouseAnalyticsService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new GreenhouseAnalyticsService(jdbcTemplate);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void analyticsUsesFirstAdminGreenhouseAndCompletesGroups() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class))).thenReturn(List.of(1L));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L), eq(24))).thenReturn((List) variedTrend());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn((List) List.of(new ChartValue("RUNNING", 2)));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn((List) List.of(new ChartValue("G1", 12.5)));

        GreenhouseAnalytics analytics = service.analytics(null, 999, admin());

        assertEquals(6, analytics.telemetryTrend().size());
        assertEquals(3, analytics.deviceStatus().size());
        assertEquals(2, analytics.deviceStatus().get(0).value());
        assertTrue(analytics.alertLevel().size() >= 3);
        assertTrue(analytics.alertStatus().size() >= 3);
        assertEquals("G1", analytics.greenhouseAreas().get(0).name());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void analyticsAllowsOwnedFarmerGreenhouseAndBuildsSyntheticTrend() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(2L), eq(7L))).thenReturn(1);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2L), eq(72))).thenReturn((List) List.of(flatPoint()));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2L))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("telemetry_snapshot")) {
                return List.of(flatPoint());
            }
            return List.of(new ChartValue("RUNNING", 1));
        });
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(7L))).thenReturn((List) List.of(new ChartValue("G2", 9.0)));

        GreenhouseAnalytics analytics = service.analytics(2L, 72, farmer());

        assertEquals(24, analytics.telemetryTrend().size());
        assertEquals(3, analytics.deviceStatus().size());
        assertEquals("G2", analytics.greenhouseAreas().get(0).name());
        assertTrue(analytics.telemetryTrend().stream().map(TelemetryTrendPoint::airTemperature).distinct().count() > 1);
    }

    @Test
    void analyticsRejectsUnauthorizedFarmer() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(2L), eq(7L))).thenReturn(0);

        assertEquals(403, assertThrows(BusinessException.class, () -> service.analytics(2L, 24, farmer())).getCode());
    }

    @Test
    void analyticsRejectsWhenNoVisibleGreenhouse() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class))).thenReturn(List.of());

        assertEquals(404, assertThrows(BusinessException.class, () -> service.analytics(null, 24, admin())).getCode());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void analyticsUsesFarmerDefaultGreenhouseAndReturnsEmptySyntheticTrendWhenNoLatestTelemetry() {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), eq(7L))).thenReturn(List.of(2L));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2L), eq(24))).thenReturn((List) List.of(flatPoint()));
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2L))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("telemetry_snapshot")) {
                return List.of();
            }
            return List.of(new ChartValue("RUNNING", 1));
        });
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(7L))).thenReturn((List) List.of(new ChartValue("G2", 9.0)));

        GreenhouseAnalytics analytics = service.analytics(null, null, farmer());

        assertEquals(0, analytics.telemetryTrend().size());
        assertEquals("G2", analytics.greenhouseAreas().get(0).name());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void analyticsRowMappersReadResultSetValues() throws Exception {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(3L), eq(24))).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(telemetryRow(), 0));
        });
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(3L))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            RowMapper mapper = invocation.getArgument(1);
            if (sql.contains("telemetry_snapshot")) {
                return List.of(mapper.mapRow(telemetryRow(), 0));
            }
            return List.of(mapper.mapRow(chartRow(2), 0));
        });
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(chartRow(12.5), 0));
        });

        GreenhouseAnalytics analytics = service.analytics(3L, 24, admin());

        assertEquals(24, analytics.telemetryTrend().size());
        assertEquals(12.5, analytics.greenhouseAreas().get(0).value());
    }

    private List<TelemetryTrendPoint> variedTrend() {
        LocalDateTime start = LocalDateTime.of(2026, 1, 1, 8, 0);
        return List.of(
                point(start, 20, 60, 18, 50, 6.5, 420, 1000),
                point(start.plusHours(1), 21, 61, 19, 51, 6.6, 430, 1100),
                point(start.plusHours(2), 22, 62, 20, 52, 6.7, 440, 1200),
                point(start.plusHours(3), 23, 63, 21, 53, 6.8, 450, 1300),
                point(start.plusHours(4), 24, 64, 22, 54, 6.9, 460, 1400),
                point(start.plusHours(5), 25, 65, 23, 55, 7.0, 470, 1500)
        );
    }

    private TelemetryTrendPoint flatPoint() {
        return point(LocalDateTime.of(2026, 1, 1, 8, 0), 25, 60, 20, 55, 6.8, 500, 1600);
    }

    private TelemetryTrendPoint point(LocalDateTime time, double airT, double airH, double soilT,
                                      double soilH, double ph, int co2, int light) {
        return new TelemetryTrendPoint(time, airT, airH, soilT, soilH, ph, co2, light);
    }

    private ResultSet telemetryRow() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getTimestamp("collected_at")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2026, 1, 1, 8, 0)));
        when(rs.getDouble("air_temperature")).thenReturn(25.0);
        when(rs.getDouble("air_humidity")).thenReturn(60.0);
        when(rs.getDouble("soil_temperature")).thenReturn(20.0);
        when(rs.getDouble("soil_humidity")).thenReturn(55.0);
        when(rs.getDouble("ph_value")).thenReturn(6.8);
        when(rs.getInt("co2_ppm")).thenReturn(500);
        when(rs.getInt("light_lux")).thenReturn(1600);
        return rs;
    }

    private ResultSet chartRow(Number value) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("name")).thenReturn("RUNNING");
        when(rs.getInt("value")).thenReturn(value.intValue());
        when(rs.getDouble("value")).thenReturn(value.doubleValue());
        return rs;
    }

    private CurrentUser admin() {
        return new CurrentUser(1L, "admin1", "ADMIN");
    }

    private CurrentUser farmer() {
        return new CurrentUser(7L, "farmer", "FARMER");
    }
}
