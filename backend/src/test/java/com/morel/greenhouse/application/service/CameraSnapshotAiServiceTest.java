package com.morel.greenhouse.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morel.greenhouse.application.dto.CameraSnapshotRequest;
import com.morel.greenhouse.domain.telemetry.TelemetrySnapshot;
import com.morel.greenhouse.infrastructure.ai.AiServiceClient;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CameraSnapshotAiServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final GreenhouseQueryService greenhouseQueryService = mock(GreenhouseQueryService.class);
    private final AiServiceClient aiServiceClient = mock(AiServiceClient.class);
    private final ObjectMapper objectMapper = spy(new ObjectMapper());
    private final CurrentUser user = new CurrentUser(2L, "farmer", "FARMER");

    @Test
    void submitSnapshotValidatesImagePersistsAndListsLatest() {
        CameraSnapshotAiService service = new CameraSnapshotAiService(jdbcTemplate, greenhouseQueryService, aiServiceClient, objectMapper, true);
        assertThatThrownBy(() -> service.submitSnapshot(new CameraSnapshotRequest(1L, null, " ", null, null), user))
                .isInstanceOf(BusinessException.class);

        when(greenhouseQueryService.getTelemetry(1L, user)).thenReturn(snapshot(1L));
        when(jdbcTemplate.queryForObject(contains("SELECT id"), eq(Long.class), eq(1L))).thenReturn(88L);
        Long id = service.submitSnapshot(new CameraSnapshotRequest(1L, 3L, " http://img ", " ", " camera "), user);
        assertThat(id).isEqualTo(88L);
        verify(jdbcTemplate).update(contains("INSERT INTO greenhouse_camera_snapshot"), eq(1L), eq(3L), eq("http://img"), isNull(), eq("CAMERA"));

        when(jdbcTemplate.queryForList(contains("FROM greenhouse_camera_snapshot"), eq(1L))).thenReturn(List.of(Map.of("id", 88L)));
        assertThat(service.latestSnapshots(1L, user)).hasSize(1);
    }

    @Test
    void analyzePendingSkipsWhenDisabledAndAnalyzesRowsWhenEnabled() throws Exception {
        CameraSnapshotAiService disabled = new CameraSnapshotAiService(jdbcTemplate, greenhouseQueryService, aiServiceClient, objectMapper, false);
        disabled.analyzePendingSnapshots();
        verifyNoInteractions(jdbcTemplate);

        CameraSnapshotAiService service = new CameraSnapshotAiService(jdbcTemplate, greenhouseQueryService, aiServiceClient, objectMapper, true);
        Map<String, Object> row = Map.of("id", 9L, "greenhouse_id", 4L, "image_base64", "data:x,abc", "greenhouse_name", "D棚", "owner_user_id", 44L);
        when(jdbcTemplate.queryForList(contains("ai_status = 'PENDING'"))).thenReturn(List.of(row));
        when(jdbcTemplate.queryForObject(contains("FROM telemetry_snapshot"), any(RowMapper.class), eq(4L)))
                .thenAnswer(invocation -> {
                    RowMapper<TelemetrySnapshot> mapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("greenhouse_id")).thenReturn(4L);
                    when(rs.getDouble("air_temperature")).thenReturn(24.0);
                    when(rs.getDouble("air_humidity")).thenReturn(65.0);
                    when(rs.getDouble("soil_temperature")).thenReturn(21.0);
                    when(rs.getDouble("soil_humidity")).thenReturn(53.0);
                    when(rs.getDouble("ph_value")).thenReturn(6.5);
                    when(rs.getInt("light_lux")).thenReturn(1000);
                    when(rs.getInt("co2_ppm")).thenReturn(410);
                    when(rs.getTimestamp("collected_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
                    return mapper.mapRow(rs, 0);
                });
        when(aiServiceClient.visionDiagnosis(any())).thenReturn(Map.of("answer", "风险", "risk_level", "HIGH"));

        service.analyzePendingSnapshots();

        verify(aiServiceClient).visionDiagnosis(argThat(payload -> "abc".equals(payload.get("image_base64"))));
        verify(jdbcTemplate).update(contains("SET ai_status = 'DONE'"), contains("\"风险\""), eq(9L));
        verify(jdbcTemplate).update(contains("INSERT INTO ai_suggestion"), eq(44L), eq(4L), eq(9L), anyString(), eq("风险"), eq("HIGH"));
    }

    @Test
    void analyzeOneMarksFailedAndHandlesJsonSerializationFailure() throws JsonProcessingException {
        CameraSnapshotAiService service = new CameraSnapshotAiService(jdbcTemplate, greenhouseQueryService, aiServiceClient, objectMapper, true);
        Map<String, Object> row = Map.of("id", 10L, "greenhouse_id", 4L, "image_base64", "abc", "greenhouse_name", "D棚");
        when(jdbcTemplate.queryForObject(contains("FROM telemetry_snapshot"), any(RowMapper.class), eq(4L)))
                .thenThrow(new RuntimeException("very long failure"));
        service.analyzeOne(row);
        verify(jdbcTemplate).update(contains("SET ai_status = 'FAILED'"), eq("very long failure"), eq(10L));

        doThrow(new JsonProcessingException("json") {
        }).when(objectMapper).writeValueAsString(any());
        when(jdbcTemplate.queryForObject(contains("FROM telemetry_snapshot"), any(RowMapper.class), eq(5L)))
                .thenReturn(snapshot(5L));
        when(aiServiceClient.visionDiagnosis(any())).thenReturn(Map.of("answer", "ok", "risk_level", "LOW"));
        service.analyzeOne(Map.of("id", 11L, "greenhouse_id", 5L, "image_base64", "abc", "greenhouse_name", "E棚"));
        verify(jdbcTemplate).update(contains("SET ai_status = 'DONE'"), isNull(), eq(11L));
    }

    private TelemetrySnapshot snapshot(Long greenhouseId) {
        return new TelemetrySnapshot(greenhouseId, 24.0, 65.0, 21.0, 53.0, 6.5, 1000, 410, LocalDateTime.now());
    }
}
