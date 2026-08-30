package com.morel.greenhouse.interfaces.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.morel.greenhouse.application.service.HuaweiIotIngestionService;
import com.morel.greenhouse.application.service.HuaweiIotPullService;
import com.morel.greenhouse.shared.api.ApiResult;
import com.morel.greenhouse.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/iot/huawei")
public class HuaweiIotController {
    private final HuaweiIotIngestionService ingestionService;
    private final HuaweiIotPullService pullService;
    private final String webhookToken;
    private final String defaultDeviceId;

    public HuaweiIotController(
            HuaweiIotIngestionService ingestionService,
            HuaweiIotPullService pullService,
            @Value("${greenhouse.iot.huawei.webhook-token:}") String webhookToken,
            @Value("${greenhouse.iot.huawei.default-device-id:}") String defaultDeviceId
    ) {
        this.ingestionService = ingestionService;
        this.pullService = pullService;
        this.webhookToken = webhookToken;
        this.defaultDeviceId = defaultDeviceId;
    }

    @PostMapping("/telemetry")
    public ApiResult<Map<String, Object>> telemetry(@RequestBody JsonNode payload, HttpServletRequest request) {
        verifyToken(request);
        return ApiResult.ok(ingestionService.ingest(payload));
    }

    @PostMapping("/pull")
    public ApiResult<Map<String, Object>> pull(
            @RequestParam(required = false) String deviceId,
            HttpServletRequest request
    ) {
        verifyToken(request);
        String targetDeviceId = deviceId == null || deviceId.isBlank() ? defaultDeviceId : deviceId.trim();
        return ApiResult.ok(pullService.pullOnce(targetDeviceId));
    }

    private void verifyToken(HttpServletRequest request) {
        if (webhookToken == null || webhookToken.isBlank()) {
            return;
        }
        String provided = request.getHeader("X-Huawei-Iot-Token");
        if (!webhookToken.equals(provided)) {
            throw new BusinessException(401, "华为云数据转发密钥不正确");
        }
    }
}
