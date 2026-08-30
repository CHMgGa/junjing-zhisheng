package com.morel.greenhouse.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morel.greenhouse.application.dto.AiChatRequest;
import com.morel.greenhouse.application.dto.AiDiagnosisRequest;
import com.morel.greenhouse.application.dto.AiDirectDownlinkRequest;
import com.morel.greenhouse.domain.telemetry.TelemetrySnapshot;
import com.morel.greenhouse.infrastructure.ai.AiServiceClient;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiAssistantServiceTest {

    private final AiServiceClient aiServiceClient = mock(AiServiceClient.class);
    private final GreenhouseQueryService greenhouseQueryService = mock(GreenhouseQueryService.class);
    private final UserAccountService userAccountService = mock(UserAccountService.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final ObjectMapper objectMapper = spy(new ObjectMapper());
    private final AiAssistantService service = new AiAssistantService(
            aiServiceClient, greenhouseQueryService, userAccountService, jdbcTemplate, objectMapper);

    private final CurrentUser admin = new CurrentUser(1L, "admin", "ADMIN");
    private final CurrentUser farmer = new CurrentUser(2L, "farmer", "FARMER");

    @Test
    void chatBuildsEnvironmentPersistsMessagesAndReturnsConversationId() {
        when(greenhouseQueryService.getTelemetry(7L, admin)).thenReturn(snapshot(7L));
        when(jdbcTemplate.queryForObject(eq("SELECT name FROM greenhouse WHERE id = ?"), eq(String.class), eq(7L)))
                .thenReturn("A棚");
        when(aiServiceClient.chat(any())).thenReturn(new java.util.LinkedHashMap<>(Map.of("answer", "ok")));
        when(jdbcTemplate.queryForObject(contains("SELECT id FROM ai_conversation"), eq(Long.class), eq(1L)))
                .thenReturn(99L);

        Map<String, Object> result = service.chat(new AiChatRequest("怎么调温？", 7L), admin);

        assertThat(result).containsEntry("conversationId", 99L).containsEntry("answer", "ok");
        verify(aiServiceClient).chat(argThat(payload -> {
            Map<?, ?> environment = (Map<?, ?>) payload.get("environment");
            return payload.get("question").equals("怎么调温？")
                    && environment.get("greenhouse_name").equals("A棚")
                    && environment.get("air_temperature").equals(25.5);
        }));
        verify(jdbcTemplate, times(3)).update(anyString(), any(Object[].class));
    }

    @Test
    void diagnoseNormalizesAnswerStripsImageAndCreatesSuggestionForRisk() {
        when(greenhouseQueryService.getTelemetry(8L, admin)).thenReturn(snapshot(8L));
        when(jdbcTemplate.queryForObject(eq("SELECT name FROM greenhouse WHERE id = ?"), eq(String.class), eq(8L)))
                .thenReturn("B棚");
        when(jdbcTemplate.queryForObject(contains("SELECT id FROM ai_conversation"), eq(Long.class), eq(1L)))
                .thenReturn(100L);
        when(jdbcTemplate.queryForList(eq("SELECT owner_user_id FROM greenhouse WHERE id = ?"), eq(Long.class), eq(8L)))
                .thenReturn(List.of(22L));
        when(aiServiceClient.visionDiagnosis(any())).thenReturn(new java.util.LinkedHashMap<>(Map.of(
                "diagnosis", "叶片异常",
                "risk_level", "medium",
                "references", List.of("r1"),
                "expert_trace", Map.of("step", 1)
        )));

        Map<String, Object> result = service.diagnose(
                new AiDiagnosisRequest(" ", 8L, "data:image/png;base64,abc123", null), admin);

        assertThat(result).containsEntry("answer", "叶片异常").containsEntry("conversationId", 100L);
        verify(aiServiceClient).visionDiagnosis(argThat(payload -> "abc123".equals(payload.get("image_base64"))));
        verify(jdbcTemplate, times(4)).update(anyString(), any(Object[].class));
    }

    @Test
    void adminOnlySuggestionOperationsCoverSuccessAndFailureBranches() {
        assertThatThrownBy(() -> service.rebuildIndex(farmer)).isInstanceOf(BusinessException.class);
        when(aiServiceClient.rebuildIndex()).thenReturn(Map.of("status", "ok"));
        assertThat(service.rebuildIndex(admin)).containsEntry("status", "ok");

        assertThatThrownBy(() -> service.suggestions(farmer)).isInstanceOf(BusinessException.class);
        when(jdbcTemplate.queryForList(contains("FROM ai_suggestion s"))).thenReturn(List.of(Map.of("id", 1L)));
        assertThat(service.suggestions(admin)).hasSize(1);

        when(jdbcTemplate.queryForList(contains("SELECT *"), eq(5L)))
                .thenReturn(List.of(Map.of("farmer_user_id", 3L, "title", "T", "content", "C")));
        when(jdbcTemplate.update(contains("SET status = 'DOWNLINKED'"), eq(1L), eq(5L))).thenReturn(1);
        service.downlinkSuggestion(5L, " note ", admin);
        verify(userAccountService).sendSystemMessage(eq(3L), eq(1L), eq(1L), eq(3L), contains("note"));

        when(jdbcTemplate.update(contains("SET status = 'DISCARDED'"), eq(6L))).thenReturn(0);
        assertThatThrownBy(() -> service.discardSuggestion(6L, "", admin)).isInstanceOf(BusinessException.class);
    }

    @Test
    void directDownlinkValidatesOwnerAndSerializesJsonFailuresGracefully() throws JsonProcessingException {
        when(jdbcTemplate.queryForList(eq("SELECT owner_user_id FROM greenhouse WHERE id = ?"), eq(Long.class), eq(10L)))
                .thenReturn(List.of());
        assertThatThrownBy(() -> service.directDownlinkSuggestion(
                new AiDirectDownlinkRequest(10L, "T", "C", null, null), admin))
                .isInstanceOf(BusinessException.class);

        when(jdbcTemplate.queryForList(eq("SELECT owner_user_id FROM greenhouse WHERE id = ?"), eq(Long.class), eq(11L)))
                .thenReturn(List.of(33L));
        service.directDownlinkSuggestion(new AiDirectDownlinkRequest(11L, " T ", " C ", "high", "N"), admin);
        verify(jdbcTemplate).update(contains("INSERT INTO ai_suggestion"), eq(33L), eq(11L), eq("T"), eq("C"), eq("HIGH"), eq(1L));
        verify(userAccountService).sendSystemMessage(eq(33L), eq(1L), eq(1L), eq(33L), contains("N"));

        doThrow(new JsonProcessingException("bad") {
        }).when(objectMapper).writeValueAsString(any());
        when(greenhouseQueryService.getTelemetry(12L, farmer)).thenReturn(snapshot(12L));
        when(jdbcTemplate.queryForObject(eq("SELECT name FROM greenhouse WHERE id = ?"), eq(String.class), eq(12L)))
                .thenReturn("C棚");
        when(jdbcTemplate.queryForObject(contains("SELECT id FROM ai_conversation"), eq(Long.class), eq(2L)))
                .thenReturn(101L);
        when(aiServiceClient.visionDiagnosis(any())).thenReturn(new java.util.LinkedHashMap<>(Map.of(
                "answer", "",
                "risk_level", "LOW",
                "references", List.of("x")
        )));
        service.diagnose(new AiDiagnosisRequest("q", 12L, "raw", "x.jpg"), farmer);
    }

    private TelemetrySnapshot snapshot(Long greenhouseId) {
        return new TelemetrySnapshot(greenhouseId, 25.5, 70.0, 22.0, 55.0, 6.8, 1200, 430, LocalDateTime.now());
    }
}
