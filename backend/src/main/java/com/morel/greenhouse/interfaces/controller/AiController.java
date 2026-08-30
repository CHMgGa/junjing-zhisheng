package com.morel.greenhouse.interfaces.controller;

import com.morel.greenhouse.application.dto.AiChatRequest;
import com.morel.greenhouse.application.dto.AiDiagnosisRequest;
import com.morel.greenhouse.application.dto.AiDirectDownlinkRequest;
import com.morel.greenhouse.application.dto.AiSuggestionDownlinkRequest;
import com.morel.greenhouse.application.dto.CameraSnapshotRequest;
import com.morel.greenhouse.application.service.AiAssistantService;
import com.morel.greenhouse.application.service.CameraSnapshotAiService;
import com.morel.greenhouse.shared.api.ApiResult;
import com.morel.greenhouse.shared.security.ApiSecurityInterceptor;
import com.morel.greenhouse.shared.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiAssistantService aiAssistantService;
    private final CameraSnapshotAiService cameraSnapshotAiService;

    public AiController(AiAssistantService aiAssistantService, CameraSnapshotAiService cameraSnapshotAiService) {
        this.aiAssistantService = aiAssistantService;
        this.cameraSnapshotAiService = cameraSnapshotAiService;
    }

    @PostMapping("/chat")
    public ApiResult<Map<String, Object>> chat(@Valid @RequestBody AiChatRequest request, HttpServletRequest servletRequest) {
        return ApiResult.ok(aiAssistantService.chat(request, currentUser(servletRequest)));
    }

    @PostMapping("/diagnosis")
    public ApiResult<Map<String, Object>> diagnosis(@Valid @RequestBody AiDiagnosisRequest request, HttpServletRequest servletRequest) {
        return ApiResult.ok(aiAssistantService.diagnose(request, currentUser(servletRequest)));
    }

    @PostMapping("/knowledge/rebuild")
    public ApiResult<Map<String, Object>> rebuildKnowledge(HttpServletRequest servletRequest) {
        return ApiResult.ok(aiAssistantService.rebuildIndex(currentUser(servletRequest)));
    }

    @PostMapping("/camera-snapshots")
    public ApiResult<Map<String, Object>> submitCameraSnapshot(
            @Valid @RequestBody CameraSnapshotRequest request,
            HttpServletRequest servletRequest
    ) {
        Long snapshotId = cameraSnapshotAiService.submitSnapshot(request, currentUser(servletRequest));
        return ApiResult.ok(Map.of("snapshotId", snapshotId));
    }

    @GetMapping("/camera-snapshots")
    public ApiResult<List<Map<String, Object>>> cameraSnapshots(
            @RequestParam Long greenhouseId,
            HttpServletRequest servletRequest
    ) {
        return ApiResult.ok(cameraSnapshotAiService.latestSnapshots(greenhouseId, currentUser(servletRequest)));
    }

    @GetMapping("/suggestions")
    public ApiResult<List<Map<String, Object>>> suggestions(HttpServletRequest servletRequest) {
        return ApiResult.ok(aiAssistantService.suggestions(currentUser(servletRequest)));
    }

    @PostMapping("/suggestions/{id}/downlink")
    public ApiResult<Void> downlinkSuggestion(
            @PathVariable Long id,
            @RequestBody AiSuggestionDownlinkRequest request,
            HttpServletRequest servletRequest
    ) {
        aiAssistantService.downlinkSuggestion(id, request.note(), currentUser(servletRequest));
        return ApiResult.ok();
    }

    @PostMapping("/suggestions/{id}/discard")
    public ApiResult<Void> discardSuggestion(
            @PathVariable Long id,
            @RequestBody AiSuggestionDownlinkRequest request,
            HttpServletRequest servletRequest
    ) {
        aiAssistantService.discardSuggestion(id, request.note(), currentUser(servletRequest));
        return ApiResult.ok();
    }

    @PostMapping("/suggestions/direct-downlink")
    public ApiResult<Void> directDownlinkSuggestion(
            @Valid @RequestBody AiDirectDownlinkRequest request,
            HttpServletRequest servletRequest
    ) {
        aiAssistantService.directDownlinkSuggestion(request, currentUser(servletRequest));
        return ApiResult.ok();
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        return (CurrentUser) request.getAttribute(ApiSecurityInterceptor.CURRENT_USER_ATTRIBUTE);
    }
}
