package com.morel.greenhouse.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.DeviceShadowData;
import com.huaweicloud.sdk.iotda.v5.model.DeviceShadowProperties;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowRequest;
import com.huaweicloud.sdk.iotda.v5.model.ShowDeviceShadowResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class HuaweiIotPullServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HuaweiIotIngestionService ingestionService = mock(HuaweiIotIngestionService.class);

    @Test
    void scheduledAndManualPullValidateConfiguration() {
        HuaweiIotPullService disabled = service(false, "", "", "", "", "");
        disabled.pullScheduled();
        verifyNoInteractions(ingestionService);

        HuaweiIotPullService incomplete = service(true, "ak", "", "pid", "endpoint", "d1");
        incomplete.pullScheduled();
        assertThatThrownBy(() -> incomplete.pullOnce("d1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void pullOnceUsesClientIngestsPropertiesAndHandlesNoProperties() throws Exception {
        HuaweiIotPullService service = service(false, "ak", "sk", "pid", "endpoint", "d1");
        IoTDAClient client = mock(IoTDAClient.class);
        setClient(service, client);
        when(ingestionService.ingest(any())).thenReturn(Map.of("greenhouse_id", 1L));
        when(client.showDeviceShadow(any(ShowDeviceShadowRequest.class))).thenReturn(response("t1", Map.of("temp", 22)));

        Map<String, Object> result = service.pullOnce("d1");

        assertThat(result).containsEntry("greenhouse_id", 1L);
        verify(ingestionService).ingest(any());

        Map<String, Object> duplicate = service.pullOnce("d1");
        assertThat(duplicate).containsEntry("status", "NO_PROPERTIES");

        when(client.showDeviceShadow(any(ShowDeviceShadowRequest.class))).thenReturn(new ShowDeviceShadowResponse());
        Map<String, Object> noProperties = service.pullOnce("d2");
        assertThat(noProperties).containsEntry("status", "NO_PROPERTIES");

        when(client.showDeviceShadow(any(ShowDeviceShadowRequest.class))).thenThrow(new RuntimeException("boom"));
        Map<String, Object> failed = service.pullOnce("d3");
        assertThat(failed).containsEntry("status", "FAILED").containsEntry("message", "boom");
    }

    @Test
    void scheduledPullLoopsConfiguredDevicesAndSkipsBlankEntries() throws Exception {
        HuaweiIotPullService service = service(true, "ak", "sk", "pid", "endpoint", "d1, ,d2");
        IoTDAClient client = mock(IoTDAClient.class);
        setClient(service, client);
        when(ingestionService.ingest(any())).thenReturn(Map.of("greenhouse_id", 1L));
        when(client.showDeviceShadow(any(ShowDeviceShadowRequest.class)))
                .thenReturn(response("t1", Map.of("temp", 22)))
                .thenReturn(response("t2", Map.of("temp", 23)));

        service.pullScheduled();

        verify(client, org.mockito.Mockito.times(2)).showDeviceShadow(any(ShowDeviceShadowRequest.class));
    }

    @Test
    void propertyTextParsingCoversJsonSdkStyleAndBlankValues() throws Exception {
        HuaweiIotPullService service = service(false, "ak", "sk", "pid", "endpoint", "d1");

        JsonNode json = invoke(service, "toPropertiesNode", new Class<?>[]{Object.class}, "{\"temp\":25}");
        assertThat(json.get("temp").asInt()).isEqualTo(25);

        JsonNode sdkStyle = invoke(service, "toPropertiesNode", new Class<?>[]{Object.class}, """
                temp: 23.5,
                status: "ON",
                quoted: 'YES'
                empty:
                invalid
                """);
        assertThat(sdkStyle.get("temp").asDouble()).isEqualTo(23.5);
        assertThat(sdkStyle.get("status").asText()).isEqualTo("ON");
        assertThat(sdkStyle.get("quoted").asText()).isEqualTo("YES");

        ObjectNode mapNode = invoke(service, "toPropertiesNode", new Class<?>[]{Object.class}, Map.of("humidity", 60));
        assertThat(mapNode.get("humidity").asInt()).isEqualTo(60);

        JsonNode blank = invoke(service, "toPropertiesNode", new Class<?>[]{Object.class}, " ");
        assertThat(blank).isNull();

        JsonNode noValidLines = invoke(service, "parseSdkStyleProperties", new Class<?>[]{String.class}, "invalid\n: value\nkey:");
        assertThat(noValidLines).isNull();
    }

    @Test
    void sdkShadowExtractionSkipsNullReportedAndNullProperties() throws Exception {
        HuaweiIotPullService service = service(false, "ak", "sk", "pid", "endpoint", "d1");
        ShowDeviceShadowResponse response = new ShowDeviceShadowResponse().withShadow(java.util.List.of(
                new DeviceShadowData(),
                new DeviceShadowData().withReported(new DeviceShadowProperties()),
                new DeviceShadowData().withReported(new DeviceShadowProperties().withEventTime("").withProperties(Map.of("temp", 24)))
        ));

        JsonNode result = invoke(service, "extractLatestProperties",
                new Class<?>[]{ShowDeviceShadowResponse.class, String.class}, response, "d1");

        assertThat(result.get("temp").asInt()).isEqualTo(24);
    }

    @Test
    void jsonShadowExtractionReturnsLatestAndSuppressesDuplicateEventTime() throws Exception {
        HuaweiIotPullService service = service(false, "ak", "sk", "pid", "endpoint", "d1");
        JsonNode root = objectMapper.readTree("""
                {
                  "shadow": [
                    {"reported": {"event_time": "t1", "properties": {"temp": 20}}},
                    {"reported": {"event_time": "t2", "properties": {"temp": 21}}}
                  ]
                }
                """);

        JsonNode first = invoke(service, "extractLatestProperties", new Class<?>[]{JsonNode.class, String.class}, root, "d1");
        assertThat(first.get("temp").asInt()).isEqualTo(21);

        JsonNode duplicate = invoke(service, "extractLatestProperties", new Class<?>[]{JsonNode.class, String.class}, root, "d1");
        assertThat(duplicate).isNull();

        JsonNode fallback = objectMapper.readTree("""
                {"reported": {"event_time": "t3", "properties": {"soil": 55}}}
                """);
        JsonNode fallbackResult = invoke(service, "extractLatestProperties", new Class<?>[]{JsonNode.class, String.class}, fallback, "d1");
        assertThat(fallbackResult.get("soil").asInt()).isEqualTo(55);

        JsonNode deviceShadow = objectMapper.readTree("""
                {"device_shadow": [{"reported": {"event_time": "", "properties": {"light": 10}}}]}
                """);
        JsonNode deviceShadowResult = invoke(service, "extractLatestProperties", new Class<?>[]{JsonNode.class, String.class}, deviceShadow, "d1");
        assertThat(deviceShadowResult.get("light").asInt()).isEqualTo(10);

        JsonNode missingProperties = objectMapper.readTree("""
                {"shadow": [{"reported": {"event_time": "t4"}}]}
                """);
        JsonNode missingResult = invoke(service, "extractLatestProperties", new Class<?>[]{JsonNode.class, String.class}, missingProperties, "d1");
        assertThat(missingResult.isMissingNode()).isTrue();
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(HuaweiIotPullService target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = HuaweiIotPullService.class.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(target, args);
    }

    private HuaweiIotPullService service(boolean enabled, String ak, String sk, String projectId, String endpoint, String deviceIds) {
        return new HuaweiIotPullService(objectMapper, ingestionService, enabled, ak, sk, projectId, "cn-north-4", endpoint, deviceIds, true);
    }

    private ShowDeviceShadowResponse response(String eventTime, Object properties) {
        DeviceShadowProperties reported = new DeviceShadowProperties()
                .withEventTime(eventTime)
                .withProperties(properties);
        DeviceShadowData data = new DeviceShadowData().withReported(reported);
        return new ShowDeviceShadowResponse().withShadow(java.util.List.of(data));
    }

    private void setClient(HuaweiIotPullService service, IoTDAClient client) throws Exception {
        Field field = HuaweiIotPullService.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(service, client);
    }
}
