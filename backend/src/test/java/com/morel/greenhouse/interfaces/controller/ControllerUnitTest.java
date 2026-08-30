package com.morel.greenhouse.interfaces.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morel.greenhouse.application.dto.AiChatRequest;
import com.morel.greenhouse.application.dto.AiDiagnosisRequest;
import com.morel.greenhouse.application.dto.AiDirectDownlinkRequest;
import com.morel.greenhouse.application.dto.AiSuggestionDownlinkRequest;
import com.morel.greenhouse.application.dto.AlertCommandRequest;
import com.morel.greenhouse.application.dto.BindGreenhousesRequest;
import com.morel.greenhouse.application.dto.CameraSnapshotRequest;
import com.morel.greenhouse.application.dto.CreateBatchEventRequest;
import com.morel.greenhouse.application.dto.CreateBatchRequest;
import com.morel.greenhouse.application.dto.CreateDeviceRequest;
import com.morel.greenhouse.application.dto.CreateGreenhouseRequest;
import com.morel.greenhouse.application.dto.DeviceCommandRequest;
import com.morel.greenhouse.application.dto.FeedbackMessageRequest;
import com.morel.greenhouse.application.dto.FeedbackRequest;
import com.morel.greenhouse.application.dto.HandleAlertRequest;
import com.morel.greenhouse.application.dto.LoginRequest;
import com.morel.greenhouse.application.dto.ProfileUpdateRequest;
import com.morel.greenhouse.application.dto.RegisterRequest;
import com.morel.greenhouse.application.dto.ResetPasswordRequest;
import com.morel.greenhouse.application.dto.SaveUserRequest;
import com.morel.greenhouse.application.dto.UpdateDeviceRequest;
import com.morel.greenhouse.application.dto.UpdateGreenhouseRequest;
import com.morel.greenhouse.application.dto.VerificationCodeRequest;
import com.morel.greenhouse.application.service.AiAssistantService;
import com.morel.greenhouse.application.service.AuthService;
import com.morel.greenhouse.application.service.CameraSnapshotAiService;
import com.morel.greenhouse.application.service.DeviceCommandService;
import com.morel.greenhouse.application.service.GreenhouseAnalyticsService;
import com.morel.greenhouse.application.service.GreenhouseManagementService;
import com.morel.greenhouse.application.service.GreenhouseQueryService;
import com.morel.greenhouse.application.service.HuaweiIotIngestionService;
import com.morel.greenhouse.application.service.HuaweiIotPullService;
import com.morel.greenhouse.application.service.UserAccountService;
import com.morel.greenhouse.shared.exception.BusinessException;
import com.morel.greenhouse.shared.security.ApiSecurityInterceptor;
import com.morel.greenhouse.shared.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ControllerUnitTest {

    @Test
    void authControllerDelegatesPoliciesAndClientIpBranches() {
        AuthService authService = mock(AuthService.class);
        AuthController controller = new AuthController(authService);
        MockHttpServletRequest forwarded = request();
        forwarded.addHeader("X-Forwarded-For", "8.8.8.8, 9.9.9.9");
        MockHttpServletRequest direct = request();
        direct.setRemoteAddr("10.0.0.8");

        when(authService.login(any(), eq("8.8.8.8"))).thenReturn(Map.of("token", "t"));
        when(authService.register(any())).thenReturn(Map.of("id", 1L));
        when(authService.sendCode(any(), eq("10.0.0.8"))).thenReturn(Map.of("mode", "DEV"));

        assertEquals("t", controller.login(new LoginRequest("u", "p"), forwarded).data().get("token"));
        assertEquals(1L, controller.register(new RegisterRequest("u", "p", "p", "13800000000", "u@example.com", "", "F", "123456")).data().get("id"));
        assertEquals("DEV", controller.sendCode(new VerificationCodeRequest("u@example.com", "REGISTER", null), direct).data().get("mode"));
        assertTrue(controller.resetPassword(new ResetPasswordRequest("u@example.com", "123456", "p", "p")).code() == 0);
        assertTrue(controller.policy("privacy").data().containsKey("content"));
        assertTrue(controller.policy("terms").data().containsKey("content"));
        assertEquals(404, assertThrows(BusinessException.class, () -> controller.policy("bad")).getCode());

        verify(authService).resetPassword(any());
    }

    @Test
    void greenhousesControllerDelegatesEveryEndpoint() {
        GreenhouseQueryService queryService = mock(GreenhouseQueryService.class);
        DeviceCommandService commandService = mock(DeviceCommandService.class);
        GreenhouseManagementService managementService = mock(GreenhouseManagementService.class);
        GreenhouseAnalyticsService analyticsService = mock(GreenhouseAnalyticsService.class);
        GreenhouseController controller = new GreenhouseController(queryService, commandService, managementService, analyticsService);
        MockHttpServletRequest request = request();

        when(queryService.listGreenhouses(any())).thenReturn(List.of());
        when(queryService.listDevices(eq(1L), any())).thenReturn(List.of());
        when(queryService.listAlerts(eq(1L), any())).thenReturn(List.of());
        when(queryService.listAlertDetails(eq(1L), any())).thenReturn(List.of());
        when(queryService.listTraceabilityRecords(eq(1L), any())).thenReturn(List.of());
        when(queryService.listBatches(eq(7L), eq(1L), eq("B"), eq("2026-01-01"), eq("2026-12-31"), any()))
                .thenReturn(List.of(Map.of("batch_no", "B")));
        when(queryService.batchDetail(eq(9L), any())).thenReturn(Map.of("id", 9L));
        when(managementService.createBatch(any(), any())).thenReturn(88L);
        when(managementService.createBatchEvent(eq(88L), any(), any())).thenReturn(99L);

        assertTrue(controller.listGreenhouses(request).code() == 0);
        assertTrue(controller.createGreenhouse(new CreateGreenhouseRequest("G", "east", 1.0, "seed", 7L), request).code() == 0);
        assertTrue(controller.updateGreenhouse(1L, new UpdateGreenhouseRequest("G", "east", "ONLINE", 1.0, "seed", 7L)).code() == 0);
        assertTrue(controller.deleteGreenhouse(1L).code() == 0);
        assertTrue(controller.overview(1L, request).code() == 0);
        assertTrue(controller.analytics(1L, 24, request).code() == 0);
        assertTrue(controller.telemetry(1L, request).code() == 0);
        assertTrue(controller.devices(1L, request).code() == 0);
        assertTrue(controller.createDevice(new CreateDeviceRequest(1L, "fan", "vent", "north", "", true), request).code() == 0);
        assertTrue(controller.updateDevice(2L, new UpdateDeviceRequest(1L, "fan", "vent", "RUNNING", "north", "", true, 90), request).code() == 0);
        assertTrue(controller.deleteDevice(2L, request).code() == 0);
        assertTrue(controller.command(new DeviceCommandRequest(2L, "START", "ON"), request).code() == 0);
        assertTrue(controller.alerts(1L, request).code() == 0);
        assertTrue(controller.alertDetails(1L, request).code() == 0);
        assertTrue(controller.handleAlert(3L, new HandleAlertRequest("RESOLVED", "farmer", null, null, "done"), request).code() == 0);
        assertTrue(controller.alertCommand(3L, new AlertCommandRequest(2L, "STOP", "note", true), request).code() == 0);
        assertTrue(controller.alertCommand(3L, new AlertCommandRequest(null, "STOP", "note", false), request).code() == 0);
        assertTrue(controller.traceability(1L, request).code() == 0);
        assertEquals(1, controller.batches(7L, 1L, "B", "2026-01-01", "2026-12-31", request).data().size());
        assertEquals(9L, controller.batchDetail(9L, request).data().get("id"));
        assertEquals(88L, controller.createBatch(new CreateBatchRequest(1L, "B", "Batch", "crop", "RUNNING", "2026-01-01", "", ""), request).data().get("id"));
        assertEquals(99L, controller.createBatchEvent(88L, new CreateBatchEventRequest("CODE", "title", "DONE", "", ""), request).data().get("id"));

        verify(commandService, times(2)).execute(any(DeviceCommandRequest.class), any(CurrentUser.class));
        verify(managementService, times(2)).recordAlertCommand(eq(3L), any(AlertCommandRequest.class), any(CurrentUser.class));
    }

    @Test
    void userControllerDelegatesAndResolvesClientIp() {
        UserAccountService userAccountService = mock(UserAccountService.class);
        UserController controller = new UserController(userAccountService);
        MockHttpServletRequest request = request();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "1.1.1.1, 2.2.2.2");
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", "farmer");

        when(userAccountService.profile(7L)).thenReturn(profile);
        when(userAccountService.updateProfile(eq(7L), any())).thenReturn(Map.of("username", "new"));
        when(userAccountService.users("a", "G")).thenReturn(List.of(Map.of("username", "a")));
        when(userAccountService.createUser(any())).thenReturn(8L);
        when(userAccountService.admins()).thenReturn(List.of(Map.of("username", "admin1")));
        when(userAccountService.farmerGreenhouseIds(7L)).thenReturn(List.of(Map.of("id", 1L)));
        when(userAccountService.feedbacks("bug", "OPEN")).thenReturn(List.of(Map.of("status", "OPEN")));
        when(userAccountService.feedbackConversations(any())).thenReturn(List.of(Map.of("conversation_id", 3L)));
        when(userAccountService.unreadFeedbackSummary(any())).thenReturn(Map.of("unreadCount", 1L));
        when(userAccountService.feedbackMessages(any(), eq(3L))).thenReturn(List.of(Map.of("content", "hi")));

        assertEquals("1.1.1.1", controller.profile(7L, request).data().get("realtimeIp"));
        MockHttpServletRequest noForwarded = request();
        noForwarded.setRemoteAddr("10.0.0.2");
        assertEquals("10.0.0.2", controller.profile(7L, noForwarded).data().get("realtimeIp"));
        assertEquals("new", controller.updateProfile(7L, new ProfileUpdateRequest("new", null, null, null, null, null, null, false)).data().get("username"));
        assertTrue(controller.feedback(new FeedbackRequest(7L, "bug", "help", "phone")).code() == 0);
        assertEquals(1, controller.users("a", "G").data().size());
        assertEquals(8L, controller.createUser(new SaveUserRequest("u", "p", "FARMER", "", "", "", "MALE", "", true)).data().get("id"));
        assertTrue(controller.updateUser(8L, new SaveUserRequest("u", "p", "FARMER", "", "", "", "MALE", "", true)).code() == 0);
        assertTrue(controller.deleteUser(8L, request).code() == 0);
        assertEquals("admin1", controller.admins().data().get(0).get("username"));
        assertEquals(1L, controller.farmerGreenhouses(7L).data().get(0).get("id"));
        assertTrue(controller.bindGreenhouses(7L, new BindGreenhousesRequest(List.of(1L)), request).code() == 0);
        assertTrue(controller.unbindGreenhouse(7L, 1L).code() == 0);
        assertEquals("OPEN", controller.feedbacks("bug", "OPEN").data().get(0).get("status"));
        assertEquals(3L, controller.feedbackConversations(request).data().get(0).get("conversation_id"));
        assertEquals(1L, controller.unreadFeedback(request).data().get("unreadCount"));
        assertEquals("hi", controller.feedbackMessages(3L, request).data().get(0).get("content"));
        assertTrue(controller.sendFeedbackMessage(new FeedbackMessageRequest(3L, null, null, "hi", null, null), request).code() == 0);
    }

    @Test
    void aiAndHuaweiControllersDelegateAndValidateToken() throws Exception {
        AiAssistantService aiAssistantService = mock(AiAssistantService.class);
        CameraSnapshotAiService cameraService = mock(CameraSnapshotAiService.class);
        AiController aiController = new AiController(aiAssistantService, cameraService);
        MockHttpServletRequest request = request();
        when(aiAssistantService.chat(any(), any())).thenReturn(new HashMap<>(Map.of("answer", "ok")));
        when(aiAssistantService.diagnose(any(), any())).thenReturn(new HashMap<>(Map.of("answer", "diag")));
        when(aiAssistantService.rebuildIndex(any())).thenReturn(Map.of("status", "ok"));
        when(cameraService.submitSnapshot(any(), any())).thenReturn(5L);
        when(cameraService.latestSnapshots(eq(1L), any())).thenReturn(List.of(Map.of("id", 5L)));
        when(aiAssistantService.suggestions(any())).thenReturn(List.of(Map.of("id", 9L)));

        assertEquals("ok", aiController.chat(new AiChatRequest("hello", 1L), request).data().get("answer"));
        assertEquals("diag", aiController.diagnosis(new AiDiagnosisRequest("q", 1L, "data:image/png;base64,abc", "a.png"), request).data().get("answer"));
        assertEquals("ok", aiController.rebuildKnowledge(request).data().get("status"));
        assertEquals(5L, aiController.submitCameraSnapshot(new CameraSnapshotRequest(1L, 2L, "", "abc", "manual"), request).data().get("snapshotId"));
        assertEquals(5L, aiController.cameraSnapshots(1L, request).data().get(0).get("id"));
        assertEquals(9L, aiController.suggestions(request).data().get(0).get("id"));
        assertTrue(aiController.downlinkSuggestion(9L, new AiSuggestionDownlinkRequest("note"), request).code() == 0);
        assertTrue(aiController.discardSuggestion(9L, new AiSuggestionDownlinkRequest("note"), request).code() == 0);
        assertTrue(aiController.directDownlinkSuggestion(new AiDirectDownlinkRequest(1L, "title", "content", "LOW", ""), request).code() == 0);

        HuaweiIotIngestionService ingestionService = mock(HuaweiIotIngestionService.class);
        HuaweiIotPullService pullService = mock(HuaweiIotPullService.class);
        HuaweiIotController tokenController = new HuaweiIotController(ingestionService, pullService, "secret", "default-device");
        MockHttpServletRequest tokenRequest = new MockHttpServletRequest();
        tokenRequest.addHeader("X-Huawei-Iot-Token", "secret");
        when(ingestionService.ingest(any())).thenReturn(Map.of("greenhouse_id", 1L));
        when(pullService.pullOnce("default-device")).thenReturn(Map.of("device_id", "default-device"));
        assertEquals(1L, tokenController.telemetry(new ObjectMapper().readTree("{\"data\":{}}"), tokenRequest).data().get("greenhouse_id"));
        assertEquals("default-device", tokenController.pull(" ", tokenRequest).data().get("device_id"));

        MockHttpServletRequest badToken = new MockHttpServletRequest();
        assertEquals(401, assertThrows(BusinessException.class,
                () -> tokenController.pull("device-1", badToken)).getCode());

        HuaweiIotController openController = new HuaweiIotController(ingestionService, pullService, "", "default-device");
        when(pullService.pullOnce("device-1")).thenReturn(Map.of("device_id", "device-1"));
        assertEquals("device-1", openController.pull(" device-1 ", new MockHttpServletRequest()).data().get("device_id"));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(ApiSecurityInterceptor.CURRENT_USER_ATTRIBUTE, new CurrentUser(7L, "farmer", "FARMER"));
        return request;
    }
}
