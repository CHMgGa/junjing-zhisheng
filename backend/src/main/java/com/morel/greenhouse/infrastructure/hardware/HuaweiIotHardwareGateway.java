package com.morel.greenhouse.infrastructure.hardware;

import com.huaweicloud.sdk.core.auth.BasicCredentials;
import com.huaweicloud.sdk.core.auth.ICredential;
import com.huaweicloud.sdk.core.exception.ConnectionException;
import com.huaweicloud.sdk.core.exception.RequestTimeoutException;
import com.huaweicloud.sdk.core.exception.ServiceResponseException;
import com.huaweicloud.sdk.core.region.Region;
import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.CreateCommandRequest;
import com.morel.greenhouse.application.dto.DeviceCommandRequest;
import com.morel.greenhouse.application.port.HardwareGateway;
import com.morel.greenhouse.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Primary
@Component
@ConditionalOnProperty(name = "greenhouse.iot.huawei.command-enabled", havingValue = "true")
public class HuaweiIotHardwareGateway implements HardwareGateway {
    private static final Logger log = LoggerFactory.getLogger(HuaweiIotHardwareGateway.class);

    private final JdbcTemplate jdbcTemplate;
    private final String ak;
    private final String sk;
    private final String projectId;
    private final String regionId;
    private final String endpoint;
    private final String defaultDeviceId;
    private final String serviceId;
    private final String instanceId;
    private final boolean derivedAuth;

    private IoTDAClient client;

    public HuaweiIotHardwareGateway(
            JdbcTemplate jdbcTemplate,
            @Value("${greenhouse.iot.huawei.ak:}") String ak,
            @Value("${greenhouse.iot.huawei.sk:}") String sk,
            @Value("${greenhouse.iot.huawei.project-id:}") String projectId,
            @Value("${greenhouse.iot.huawei.region-id:cn-north-4}") String regionId,
            @Value("${greenhouse.iot.huawei.endpoint:}") String endpoint,
            @Value("${greenhouse.iot.huawei.default-device-id:}") String defaultDeviceId,
            @Value("${greenhouse.iot.huawei.command-service-id:}") String serviceId,
            @Value("${greenhouse.iot.huawei.instance-id:}") String instanceId,
            @Value("${greenhouse.iot.huawei.derived-auth:true}") boolean derivedAuth
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.ak = ak;
        this.sk = sk;
        this.projectId = projectId;
        this.regionId = regionId;
        this.endpoint = endpoint;
        this.defaultDeviceId = defaultDeviceId;
        this.serviceId = serviceId;
        this.instanceId = instanceId;
        this.derivedAuth = derivedAuth;
    }

    @Override
    public void dispatchDeviceCommand(DeviceCommandRequest request) {
        if (!configured()) {
            throw new BusinessException(500, "Huawei IoTDA command configuration is incomplete");
        }
        DeviceInfo device = deviceInfo(request.deviceId());
        HuaweiCommand command = resolveCommand(request, device);
        try {
            com.huaweicloud.sdk.iotda.v5.model.DeviceCommandRequest body =
                    new com.huaweicloud.sdk.iotda.v5.model.DeviceCommandRequest()
                            .withServiceId(serviceId)
                            .withCommandName(command.name())
                            .withParas(Map.of(command.param(), command.value()));
            CreateCommandRequest commandRequest = new CreateCommandRequest()
                    .withDeviceId(device.iotDeviceId())
                    .withBody(body);
            if (!blank(instanceId)) {
                commandRequest.withInstanceId(instanceId);
            }
            client().createCommand(commandRequest);
            log.info("Huawei IoTDA command sent: localDeviceId={}, iotDeviceId={}, command={}, param={}, value={}",
                    request.deviceId(), device.iotDeviceId(), command.name(), command.param(), command.value());
        } catch (ConnectionException | RequestTimeoutException exception) {
            throw new BusinessException(504, "Huawei IoTDA command dispatch timed out");
        } catch (ServiceResponseException exception) {
            log.warn("Huawei IoTDA command failed: status={}, code={}, message={}",
                    exception.getHttpStatusCode(), exception.getErrorCode(), exception.getErrorMsg());
            throw new BusinessException(502, "Huawei IoTDA command failed: " + exception.getErrorMsg());
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Huawei IoTDA command failed: {}", exception.getMessage(), exception);
            throw new BusinessException(502, "Huawei IoTDA command failed: " + exception.getMessage());
        }
    }

    private DeviceInfo deviceInfo(Long deviceId) {
        return jdbcTemplate.query("""
                SELECT d.id, d.name, d.category, d.serial_no, dt.type_code
                FROM greenhouse_device d
                LEFT JOIN device_type dt ON dt.id = d.device_type_id
                WHERE d.id = ? AND d.deleted = FALSE
                """, rs -> {
            if (!rs.next()) {
                throw new BusinessException(404, "Device not found");
            }
            String serialNo = rs.getString("serial_no");
            String iotDeviceId = blank(serialNo) ? defaultDeviceId : serialNo.trim();
            if (blank(iotDeviceId)) {
                throw new BusinessException(400, "Device is not bound to Huawei IoTDA device_id");
            }
            return new DeviceInfo(
                    rs.getLong("id"),
                    text(rs.getString("name")),
                    text(rs.getString("category")),
                    text(rs.getString("type_code")),
                    iotDeviceId
            );
        }, deviceId);
    }

    private HuaweiCommand resolveCommand(DeviceCommandRequest request, DeviceInfo device) {
        String rawCommand = text(request.command());
        String upper = rawCommand.toUpperCase(Locale.ROOT);
        String normalizedValue = text(request.value());
        return switch (upper) {
            case "LIGHT" -> new HuaweiCommand("Light", "light", onOffValue(normalizedValue, "ON"));
            case "FENGDEGREE", "FAN", "FENGD" -> new HuaweiCommand("Fengdegree", "fengdegree", numericValue(normalizedValue, 5));
            case "BOARD" -> new HuaweiCommand("Board", "board", onOffValue(normalizedValue, "ON"));
            case "AIWARNING" -> new HuaweiCommand("AIWarning", "aiwarning", blank(normalizedValue) ? "normal" : normalizedValue);
            case "STATE" -> new HuaweiCommand("State", "state", onOffValue(normalizedValue, "ON"));
            case "BUMP", "PUMP" -> new HuaweiCommand("BUMP", "bump", onOffValue(normalizedValue, "ON"));
            case "START", "ON", "OPEN" -> startCommand(device, normalizedValue);
            case "STOP", "OFF", "CLOSE" -> stopCommand(device);
            case "MAINTENANCE" -> new HuaweiCommand("State", "state", "MAINTENANCE");
            default -> throw new BusinessException(400, "Unsupported device command: " + request.command());
        };
    }

    private HuaweiCommand startCommand(DeviceInfo device, String value) {
        if ("VENTILATION_FAN".equalsIgnoreCase(device.typeCode()) || containsAny(device, "fan", "feng", "风机", "通风")) {
            return new HuaweiCommand("Fengdegree", "fengdegree", numericValue(value, 5));
        }
        if ("IRRIGATION_PUMP".equalsIgnoreCase(device.typeCode()) || containsAny(device, "pump", "bump", "水泵", "灌溉")) {
            return new HuaweiCommand("BUMP", "bump", "ON");
        }
        if (containsAny(device, "light", "lamp", "灯", "补光")) {
            return new HuaweiCommand("Light", "light", "ON");
        }
        if (containsAny(device, "board")) {
            return new HuaweiCommand("Board", "board", "ON");
        }
        return new HuaweiCommand("State", "state", "ON");
    }

    private HuaweiCommand stopCommand(DeviceInfo device) {
        if ("VENTILATION_FAN".equalsIgnoreCase(device.typeCode()) || containsAny(device, "fan", "feng", "风机", "通风")) {
            return new HuaweiCommand("Fengdegree", "fengdegree", 0);
        }
        if ("IRRIGATION_PUMP".equalsIgnoreCase(device.typeCode()) || containsAny(device, "pump", "bump", "水泵", "灌溉")) {
            return new HuaweiCommand("BUMP", "bump", "OFF");
        }
        if (containsAny(device, "light", "lamp", "灯", "补光")) {
            return new HuaweiCommand("Light", "light", "OFF");
        }
        if (containsAny(device, "board")) {
            return new HuaweiCommand("Board", "board", "OFF");
        }
        return new HuaweiCommand("State", "state", "OFF");
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

    private Object numericValue(String value, int defaultValue) {
        if (blank(value)) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(0, Math.min(9, parsed));
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private String onOffValue(String value, String defaultValue) {
        if (blank(value)) {
            return defaultValue;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        return switch (upper) {
            case "START", "OPEN", "TRUE", "1" -> "ON";
            case "STOP", "CLOSE", "FALSE", "0" -> "OFF";
            default -> value;
        };
    }

    private boolean containsAny(DeviceInfo device, String... keys) {
        String haystack = (device.name() + " " + device.category() + " " + device.typeCode()).toLowerCase(Locale.ROOT);
        for (String key : keys) {
            if (haystack.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean configured() {
        return !blank(ak) && !blank(sk) && !blank(projectId) && !blank(endpoint) && !blank(serviceId);
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private record DeviceInfo(Long id, String name, String category, String typeCode, String iotDeviceId) {
    }

    private record HuaweiCommand(String name, String param, Object value) {
    }
}
