package com.morel.greenhouse.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Supplies smooth demonstration telemetry only while a greenhouse has stopped receiving real data.
 */
@Service
@Profile("mock")
public class DemoTelemetryService {
    private static final int HISTORY_SAMPLES = 96;

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final long staleAfterSeconds;

    public DemoTelemetryService(
            JdbcTemplate jdbcTemplate,
            @Value("${greenhouse.demo-telemetry.enabled:true}") boolean enabled,
            @Value("${greenhouse.demo-telemetry.stale-after-seconds:90}") long staleAfterSeconds
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
        this.staleAfterSeconds = staleAfterSeconds;
    }

    @Scheduled(fixedDelayString = "${greenhouse.demo-telemetry.fixed-delay-ms:60000}", initialDelay = 15_000)
    @Transactional
    public void generateWhenStale() {
        if (!enabled) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<Long> greenhouseIds = jdbcTemplate.queryForList(
                "SELECT id FROM greenhouse WHERE deleted = FALSE ORDER BY id", Long.class);
        for (Long greenhouseId : greenhouseIds) {
            bootstrapHistory(greenhouseId, now);
            LocalDateTime latest = latestCollectedAt(greenhouseId);
            if (latest == null || latest.isBefore(now.minusSeconds(staleAfterSeconds))) {
                insertSnapshot(greenhouseId, now, phase(now, greenhouseId));
            }
        }
    }

    private void bootstrapHistory(Long greenhouseId, LocalDateTime now) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM telemetry_snapshot WHERE greenhouse_id = ?", Integer.class, greenhouseId);
        if (count != null && count >= HISTORY_SAMPLES) {
            return;
        }
        for (int index = 0; index < HISTORY_SAMPLES; index++) {
            LocalDateTime collectedAt = now.minusMinutes((long) (HISTORY_SAMPLES - index) * 15L);
            insertSnapshot(greenhouseId, collectedAt, index);
        }
    }

    private LocalDateTime latestCollectedAt(Long greenhouseId) {
        return jdbcTemplate.query("""
                SELECT collected_at
                FROM telemetry_snapshot
                WHERE greenhouse_id = ?
                ORDER BY collected_at DESC, id DESC
                LIMIT 1
                """, rs -> rs.next() ? rs.getTimestamp("collected_at").toLocalDateTime() : null, greenhouseId);
    }

    private void insertSnapshot(Long greenhouseId, LocalDateTime collectedAt, int phase) {
        TelemetryPoint point = pointFor(greenhouseId, phase);
        jdbcTemplate.update("""
                INSERT INTO telemetry_snapshot(
                    greenhouse_id, temperature, humidity, air_temperature, air_humidity,
                    soil_temperature, soil_humidity, ph_value, light_lux, co2_ppm, soil_moisture, collected_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                greenhouseId, point.airTemperature(), point.airHumidity(), point.airTemperature(), point.airHumidity(),
                point.soilTemperature(), point.soilHumidity(), point.phValue(), point.lightLux(), point.co2Ppm(),
                point.soilHumidity(), Timestamp.valueOf(collectedAt));
    }

    private int phase(LocalDateTime now, Long greenhouseId) {
        return (int) (now.getHour() * 60L + now.getMinute() + greenhouseId * 19L);
    }

    private TelemetryPoint pointFor(Long greenhouseId, int phase) {
        double wave = Math.sin((phase + greenhouseId * 13D) / 8D);
        double shortWave = Math.cos((phase + greenhouseId * 7D) / 3D);
        double greenhouseOffset = Math.max(0, greenhouseId - 1) * 0.35D;
        return new TelemetryPoint(
                round(20.8D + greenhouseOffset + wave * 1.15D),
                round(81D - greenhouseOffset * 3D + shortWave * 3.2D),
                round(19.7D + greenhouseOffset + wave * 0.65D),
                round(62D - greenhouseOffset * 1.5D + shortWave * 2.4D),
                round(6.7D + wave * 0.12D),
                (int) Math.round(4000D + Math.max(0D, wave) * 900D + shortWave * 120D),
                (int) Math.round(700D + greenhouseOffset * 80D + shortWave * 95D)
        );
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private record TelemetryPoint(
            double airTemperature,
            double airHumidity,
            double soilTemperature,
            double soilHumidity,
            double phValue,
            int lightLux,
            int co2Ppm
    ) {
    }
}
