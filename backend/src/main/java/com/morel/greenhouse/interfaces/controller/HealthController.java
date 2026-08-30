package com.morel.greenhouse.interfaces.controller;

import com.morel.greenhouse.shared.api.ApiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final Environment environment;
    private final boolean huaweiPullEnabled;
    private final boolean huaweiCommandEnabled;

    public HealthController(
            Environment environment,
            @Value("${greenhouse.iot.huawei.pull-enabled:false}") boolean huaweiPullEnabled,
            @Value("${greenhouse.iot.huawei.command-enabled:false}") boolean huaweiCommandEnabled
    ) {
        this.environment = environment;
        this.huaweiPullEnabled = huaweiPullEnabled;
        this.huaweiCommandEnabled = huaweiCommandEnabled;
    }

    @GetMapping
    public ApiResult<Map<String, String>> health() {
        return ApiResult.ok(Map.of(
                "status", "UP",
                "profiles", String.join(",", environment.getActiveProfiles()),
                "huaweiPull", Boolean.toString(huaweiPullEnabled),
                "huaweiCommand", Boolean.toString(huaweiCommandEnabled)
        ));
    }
}
