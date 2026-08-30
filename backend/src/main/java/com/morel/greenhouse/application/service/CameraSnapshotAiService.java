package com.morel.greenhouse.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morel.greenhouse.application.dto.CameraSnapshotRequest;
import com.morel.greenhouse.domain.telemetry.TelemetrySnapshot;
import com.morel.greenhouse.infrastructure.ai.AiServiceClient;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CameraSnapshotAiService {
    private static final Logger log = LoggerFactory.getLogger(CameraSnapshotAiService.class);

    private final JdbcTemplate jdbcTemplate;
    private final GreenhouseQueryService greenhouseQueryService;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper;
    private final boolean scheduledEnabled;

    public CameraSnapshotAiService(
            JdbcTemplate jdbcTemplate,
            GreenhouseQueryService greenhouseQueryService,
            AiServiceClient aiServiceClient,
            ObjectMapper objectMapper,
            @Value("${greenhouse.ai.scheduled-analysis-enabled:true}") boolean scheduledEnabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.greenhouseQueryService = greenhouseQueryService;
        this.aiServiceClient = aiServiceClient;
        this.objectMapper = objectMapper;
        this.scheduledEnabled = scheduledEnabled;
    }

    @Transactional
    public Long submitSnapshot(CameraSnapshotRequest request, CurrentUser currentUser) {
        if ((request.imageBase64() == null || request.imageBase64().isBlank())
                && (request.imageUrl() == null || request.imageUrl().isBlank())) {
            throw new BusinessException(400, "请上传抓拍图片或提供图片地址");
        }
        greenhouseQueryService.getTelemetry(request.greenhouseId(), currentUser);
        jdbcTemplate.update("""
                INSERT INTO greenhouse_camera_snapshot(greenhouse_id, device_id, image_url, image_base64, source_type)
                VALUES (?, ?, ?, ?, ?)
                """,
                request.greenhouseId(),
                request.deviceId(),
                emptyToNull(request.imageUrl()),
                emptyToNull(request.imageBase64()),
                request.sourceType() == null || request.sourceType().isBlank() ? "MANUAL" : request.sourceType().trim().toUpperCase()
        );
        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM greenhouse_camera_snapshot
                WHERE greenhouse_id = ?
                ORDER BY id DESC
                LIMIT 1
                """, Long.class, request.greenhouseId());
    }

    public List<Map<String, Object>> latestSnapshots(Long greenhouseId, CurrentUser currentUser) {
        greenhouseQueryService.getTelemetry(greenhouseId, currentUser);
        return jdbcTemplate.queryForList("""
                SELECT id, greenhouse_id, device_id, image_url, source_type, ai_status, ai_result_json, ai_error,
                       captured_at, analyzed_at
                FROM greenhouse_camera_snapshot
                WHERE greenhouse_id = ? AND deleted = FALSE
                ORDER BY captured_at DESC, id DESC
                LIMIT 10
                """, greenhouseId);
    }

    @Scheduled(fixedDelayString = "${greenhouse.ai.scheduled-analysis-fixed-delay-ms:300000}", initialDelay = 30000)
    public void analyzePendingSnapshots() {
        if (!scheduledEnabled) {
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT s.id, s.greenhouse_id, s.image_base64, g.name AS greenhouse_name, g.owner_user_id
                FROM greenhouse_camera_snapshot s
                JOIN greenhouse g ON g.id = s.greenhouse_id
                WHERE s.deleted = FALSE
                  AND s.ai_status = 'PENDING'
                  AND s.image_base64 IS NOT NULL
                ORDER BY s.captured_at
                LIMIT 5
                """);
        for (Map<String, Object> row : rows) {
            analyzeOne(row);
        }
    }

    @Transactional
    public void analyzeOne(Map<String, Object> row) {
        Long snapshotId = longValue(row.get("id"));
        Long greenhouseId = longValue(row.get("greenhouse_id"));
        try {
            jdbcTemplate.update("""
                    UPDATE greenhouse_camera_snapshot
                    SET ai_status = 'ANALYZING', updated_at = CURRENT_TIMESTAMP
                    WHERE id = ? AND ai_status = 'PENDING'
                    """, snapshotId);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("question", "请对摄像头定时抓拍的羊肚菌图片进行病虫害、长势和环境风险诊断。");
            payload.put("greenhouse_id", greenhouseId);
            payload.put("image_base64", stripDataUrl(String.valueOf(row.get("image_base64"))));
            payload.put("image_filename", "camera-snapshot-" + snapshotId + ".jpg");
            payload.put("environment", environment(greenhouseId, String.valueOf(row.get("greenhouse_name"))));
            Map<String, Object> result = aiServiceClient.visionDiagnosis(payload);
            jdbcTemplate.update("""
                    UPDATE greenhouse_camera_snapshot
                    SET ai_status = 'DONE', ai_result_json = ?, analyzed_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """, toJson(result), snapshotId);
            createSuggestionIfNeeded(row, result);
        } catch (RuntimeException ex) {
            log.warn("AI camera snapshot analysis failed, snapshotId={}", snapshotId, ex);
            jdbcTemplate.update("""
                    UPDATE greenhouse_camera_snapshot
                    SET ai_status = 'FAILED', ai_error = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """, trim(ex.getMessage(), 900), snapshotId);
        }
    }

    private void createSuggestionIfNeeded(Map<String, Object> row, Map<String, Object> result) {
        String riskLevel = stringValue(result.get("risk_level"));
        if (!"HIGH".equalsIgnoreCase(riskLevel) && !"MEDIUM".equalsIgnoreCase(riskLevel)) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO ai_suggestion(farmer_user_id, greenhouse_id, snapshot_id, title, content, risk_level, source_type)
                VALUES (?, ?, ?, ?, ?, ?, 'CAMERA_AUTO')
                """,
                longValue(row.get("owner_user_id")),
                longValue(row.get("greenhouse_id")),
                longValue(row.get("id")),
                "摄像头抓拍 AI 风险预警",
                stringValue(result.get("answer")),
                riskLevel.toUpperCase()
        );
    }

    private Map<String, Object> environment(Long greenhouseId, String greenhouseName) {
        TelemetrySnapshot telemetry = jdbcTemplate.queryForObject("""
                SELECT greenhouse_id, air_temperature, air_humidity, soil_temperature, soil_humidity, ph_value,
                       light_lux, co2_ppm, collected_at
                FROM telemetry_snapshot
                WHERE greenhouse_id = ?
                ORDER BY collected_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> new TelemetrySnapshot(
                        rs.getLong("greenhouse_id"),
                        rs.getDouble("air_temperature"),
                        rs.getDouble("air_humidity"),
                        rs.getDouble("soil_temperature"),
                        rs.getDouble("soil_humidity"),
                        rs.getDouble("ph_value"),
                        rs.getInt("light_lux"),
                        rs.getInt("co2_ppm"),
                        rs.getTimestamp("collected_at").toLocalDateTime()
                ),
                greenhouseId
        );
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("greenhouse_id", telemetry.greenhouseId());
        data.put("greenhouse_name", greenhouseName);
        data.put("air_temperature", telemetry.airTemperature());
        data.put("air_humidity", telemetry.airHumidity());
        data.put("soil_temperature", telemetry.soilTemperature());
        data.put("soil_humidity", telemetry.soilHumidity());
        data.put("ph_value", telemetry.phValue());
        data.put("co2_ppm", telemetry.co2Ppm());
        data.put("light_lux", telemetry.lightLux());
        return data;
    }

    private String stripDataUrl(String imageBase64) {
        int comma = imageBase64.indexOf(',');
        return comma >= 0 ? imageBase64.substring(comma + 1) : imageBase64;
    }

    private String toJson(Object value) {
        try {
            return value == null ? null : objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long longValue(Object value) {
        return value == null ? null : Long.valueOf(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
