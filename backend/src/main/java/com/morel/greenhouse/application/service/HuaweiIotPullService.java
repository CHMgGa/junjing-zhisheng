package com.morel.greenhouse.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.core.region.Region;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.DeviceShadowData;
import com.huaweicloud.sdk.iotda.v5.model.DeviceShadowProperties;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowRequest;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class HuaweiIotPullService {
    private static final Logger log = LoggerFactory.getLogger(HuaweiIotPullService.class);

    private final ObjectMapper objectMapper;
    private final HuaweiIotIngestionService ingestionService;
    private final boolean enabled;
    private final String ak;
    private final String sk;
    private final String projectId;
    private final String regionId;
    private final String endpoint;
    private final String deviceIds;
    private final boolean derivedAuth;
    private final Map<String, String> lastEventTimes = new HashMap<>();

    private IoTDAClient client;

    public HuaweiIotPullService(
            ObjectMapper objectMapper,
            HuaweiIotIngestionService ingestionService,
            @Value("${greenhouse.iot.huawei.pull-enabled:false}") boolean enabled,
            @Value("${greenhouse.iot.huawei.ak:}") String ak,
            @Value("${greenhouse.iot.huawei.sk:}") String sk,
            @Value("${greenhouse.iot.huawei.project-id:}") String projectId,
            @Value("${greenhouse.iot.huawei.region-id:cn-north-4}") String regionId,
            @Value("${greenhouse.iot.huawei.endpoint:}") String endpoint,
            @Value("${greenhouse.iot.huawei.pull-device-ids:}") String deviceIds,
            @Value("${greenhouse.iot.huawei.derived-auth:true}") boolean derivedAuth
    ) {
        this.objectMapper = objectMapper;
        this.ingestionService = ingestionService;
        this.enabled = enabled;
        this.ak = ak;
        this.sk = sk;
        this.projectId = projectId;
        this.regionId = regionId;
        this.endpoint = endpoint;
        this.deviceIds = deviceIds;
        this.derivedAuth = derivedAuth;
    }

    @Scheduled(fixedDelayString = "${greenhouse.iot.huawei.pull-fixed-delay-ms:60000}")
    public void pullScheduled() {
        if (!enabled) {
            return;
        }
        if (!configured()) {
            log.warn("Huawei IoT pull is enabled but AK/SK/projectId/endpoint/deviceIds are not fully configured");
            return;
        }
        for (String deviceId : deviceIds.split(",")) {
            String normalized = deviceId.trim();
            if (!normalized.isBlank()) {
                pullDevice(normalized);
            }
        }
    }

    public Map<String, Object> pullOnce(String deviceId) {
        if (!configured()) {
            throw new IllegalStateException("Huawei IoT pull configuration is incomplete");
        }
        return pullDevice(deviceId);
    }

    private Map<String, Object> pullDevice(String deviceId) {
        try {
            ShowDeviceShadowRequest request = new ShowDeviceShadowRequest().withDeviceId(deviceId);
            ShowDeviceShadowResponse response = client().showDeviceShadow(request);
            JsonNode properties = extractLatestProperties(response, deviceId);
            if (properties == null || properties.isMissingNode() || properties.isNull()) {
                log.warn("Huawei IoT shadow has no reported properties for device {}", deviceId);
                return Map.of("device_id", deviceId, "status", "NO_PROPERTIES");
            }
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("device_id", deviceId);
            payload.set("data", properties);
            Map<String, Object> result = ingestionService.ingest(payload);
            log.info("Huawei IoT shadow pulled and ingested: deviceId={}, greenhouseId={}", deviceId, result.get("greenhouse_id"));
            return result;
        } catch (ConnectionException | RequestTimeoutException exception) {
            log.warn("Huawei IoT pull network error: deviceId={}, message={}", deviceId, exception.getMessage());
            return Map.of("device_id", deviceId, "status", "NETWORK_ERROR", "message", exception.getMessage());
        } catch (ServiceResponseException exception) {
            log.warn("Huawei IoT pull service error: deviceId={}, status={}, code={}, message={}",
                    deviceId, exception.getHttpStatusCode(), exception.getErrorCode(), exception.getErrorMsg());
            return Map.of("device_id", deviceId, "status", "SERVICE_ERROR", "code", exception.getErrorCode(), "message", exception.getErrorMsg());
        } catch (Exception exception) {
            log.warn("Huawei IoT pull failed: deviceId={}, message={}", deviceId, exception.getMessage(), exception);
            return Map.of("device_id", deviceId, "status", "FAILED", "message", exception.getMessage());
        }
    }

    private JsonNode extractLatestProperties(ShowDeviceShadowResponse response, String deviceId) {
        JsonNode latestProperties = null;
        String latestEventTime = null;
        if (response.getShadow() != null) {
            for (DeviceShadowData item : response.getShadow()) {
                DeviceShadowProperties reported = item.getReported();
                if (reported == null || reported.getProperties() == null) {
                    continue;
                }
                JsonNode properties = toPropertiesNode(reported.getProperties());
                if (properties == null || properties.isMissingNode() || properties.isNull()) {
                    continue;
                }
                latestProperties = properties;
                latestEventTime = reported.getEventTime();
            }
        }
        if (latestEventTime != null && !latestEventTime.isBlank() && latestEventTime.equals(lastEventTimes.get(deviceId))) {
            return null;
        }
        if (latestEventTime != null && !latestEventTime.isBlank()) {
            lastEventTimes.put(deviceId, latestEventTime);
        }
        return latestProperties;
    }

    private JsonNode toPropertiesNode(Object properties) {
        if (properties instanceof String text) {
            if (text.isBlank()) {
                return null;
            }
            try {
                return objectMapper.readTree(text);
            } catch (Exception ignored) {
                return parseSdkStyleProperties(text);
            }
        }
        return objectMapper.valueToTree(properties);
    }

    private JsonNode parseSdkStyleProperties(String text) {
        ObjectNode node = objectMapper.createObjectNode();
        String[] lines = text.split("\\R");
        for (String line : lines) {
            String trimmed = line.trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();
            if (key.isBlank() || value.isBlank()) {
                continue;
            }
            if (value.endsWith(",")) {
                value = value.substring(0, value.length() - 1).trim();
            }
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            try {
                double number = Double.parseDouble(value);
                node.put(key, number);
            } catch (NumberFormatException ignored) {
                node.put(key, value);
            }
        }
        return node.size() == 0 ? null : node;
    }

    private JsonNode extractLatestProperties(JsonNode root, String deviceId) {
        JsonNode shadow = root.path("shadow");
        if (!shadow.isArray()) {
            shadow = root.path("device_shadow");
        }
        JsonNode latestProperties = null;
        String latestEventTime = null;
        if (shadow.isArray()) {
            for (JsonNode item : shadow) {
                JsonNode reported = item.path("reported");
                JsonNode properties = reported.path("properties");
                if (properties.isMissingNode() || properties.isNull()) {
                    continue;
                }
                String eventTime = reported.path("event_time").asText("");
                latestProperties = properties;
                latestEventTime = eventTime;
            }
        }
        if (latestProperties == null) {
            latestProperties = root.at("/reported/properties");
            latestEventTime = root.at("/reported/event_time").asText("");
        }
        if (latestEventTime != null && !latestEventTime.isBlank() && latestEventTime.equals(lastEventTimes.get(deviceId))) {
            return null;
        }
        if (latestEventTime != null && !latestEventTime.isBlank()) {
            lastEventTimes.put(deviceId, latestEventTime);
        }
        return latestProperties;
    }

    private IoTDAClient client() {
        if (client != null) {
            return client;
        }
        BasicCredentials credentials = new BasicCredentials()
                .withAk(ak)
                .withSk(sk)
                .withProjectId(projectId);
        ICredential auth = derivedAuth
                ? credentials.withDerivedPredicate(BasicCredentials.DEFAULT_DERIVED_PREDICATE)
                : credentials;
        client = IoTDAClient.newBuilder()
                .withCredential(auth)
                .withRegion(new Region(regionId, endpoint))
                .build();
        return client;
    }

    private boolean configured() {
        return !blank(ak) && !blank(sk) && !blank(projectId) && !blank(endpoint) && !blank(deviceIds);
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
