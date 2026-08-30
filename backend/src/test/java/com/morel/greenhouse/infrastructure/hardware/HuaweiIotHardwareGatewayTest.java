package com.morel.greenhouse.infrastructure.hardware;

import com.huaweicloud.sdk.iotda.v5.IoTDAClient;
import com.huaweicloud.sdk.iotda.v5.model.CreateCommandRequest;
import com.morel.greenhouse.application.dto.DeviceCommandRequest;
import com.morel.greenhouse.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HuaweiIotHardwareGatewayTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    @Test
    void dispatchRejectsIncompleteConfigurationBeforeNetworkAccess() {
        HuaweiIotHardwareGateway gateway = new HuaweiIotHardwareGateway(
                jdbcTemplate, "", "sk", "pid", "cn-north-4", "endpoint", "iot-1", "srv", "", true);

        assertThatThrownBy(() -> gateway.dispatchDeviceCommand(new DeviceCommandRequest(1L, "START", "ON")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("incomplete");
    }

    @Test
    void dispatchLoadsDeviceAndSendsCommandThroughMockedClient() throws Exception {
        HuaweiIotHardwareGateway gateway = gateway();
        IoTDAClient client = mock(IoTDAClient.class);
        setClient(gateway, client);
        when(jdbcTemplate.query(any(String.class), any(ResultSetExtractor.class), any()))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<?> extractor = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.next()).thenReturn(true);
                    when(rs.getLong("id")).thenReturn(1L);
                    when(rs.getString("serial_no")).thenReturn(" iot-device ");
                    when(rs.getString("name")).thenReturn("Fan");
                    when(rs.getString("category")).thenReturn("vent");
                    when(rs.getString("type_code")).thenReturn("VENTILATION_FAN");
                    return extractor.extractData(rs);
                });

        gateway.dispatchDeviceCommand(new DeviceCommandRequest(1L, "START", "6"));

        verify(client).createCommand(any(CreateCommandRequest.class));
    }

    @Test
    void dispatchUsesDefaultDeviceIdAndAllowsBlankInstanceId() throws Exception {
        HuaweiIotHardwareGateway gateway = new HuaweiIotHardwareGateway(
                jdbcTemplate, "ak", "sk", "pid", "cn-north-4", "endpoint", "default-iot", "srv", "", true);
        IoTDAClient client = mock(IoTDAClient.class);
        setClient(gateway, client);
        stubDeviceRow(true, " ", "Light", "lamp", "");

        gateway.dispatchDeviceCommand(new DeviceCommandRequest(1L, "START", ""));

        verify(client).createCommand(any(CreateCommandRequest.class));
    }

    @Test
    void dispatchReportsMissingDeviceUnboundDeviceAndClientFailure() throws Exception {
        HuaweiIotHardwareGateway missing = gateway();
        stubNoDevice();
        assertThatThrownBy(() -> missing.dispatchDeviceCommand(new DeviceCommandRequest(1L, "START", "")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Device not found");

        HuaweiIotHardwareGateway unbound = new HuaweiIotHardwareGateway(
                jdbcTemplate, "ak", "sk", "pid", "cn-north-4", "endpoint", "", "srv", "", true);
        stubDeviceRow(true, " ", "Device", "", "");
        assertThatThrownBy(() -> unbound.dispatchDeviceCommand(new DeviceCommandRequest(1L, "START", "")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not bound");

        HuaweiIotHardwareGateway failed = gateway();
        IoTDAClient client = mock(IoTDAClient.class);
        setClient(failed, client);
        stubDeviceRow(true, "iot-device", "Fan", "", "VENTILATION_FAN");
        when(client.createCommand(any(CreateCommandRequest.class))).thenThrow(new RuntimeException("sdk"));
        assertThatThrownBy(() -> failed.dispatchDeviceCommand(new DeviceCommandRequest(1L, "START", "")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sdk");
    }

    @Test
    void resolveExplicitCommandsAndValueNormalization() throws Exception {
        HuaweiIotHardwareGateway gateway = gateway();
        Object generic = device("Generic", "state", "", "iot-1");

        assertCommand(invokeResolve(gateway, "LIGHT", "open", generic), "Light", "light", "ON");
        assertCommand(invokeResolve(gateway, "LIGHT", "0", generic), "Light", "light", "OFF");
        assertCommand(invokeResolve(gateway, "FAN", "7", generic), "Fengdegree", "fengdegree", 7);
        assertCommand(invokeResolve(gateway, "FAN", "fast", generic), "Fengdegree", "fengdegree", 5);
        assertCommand(invokeResolve(gateway, "FENGD", "", generic), "Fengdegree", "fengdegree", 5);
        assertCommand(invokeResolve(gateway, "BOARD", "true", generic), "Board", "board", "ON");
        assertCommand(invokeResolve(gateway, "AIWARNING", "", generic), "AIWarning", "aiwarning", "normal");
        assertCommand(invokeResolve(gateway, "STATE", "false", generic), "State", "state", "OFF");
        assertCommand(invokeResolve(gateway, "PUMP", "close", generic), "BUMP", "bump", "OFF");
        assertCommand(invokeResolve(gateway, "AIWARNING", "alarm", generic), "AIWarning", "aiwarning", "alarm");
        assertCommand(invokeResolve(gateway, "MAINTENANCE", "", generic), "State", "state", "MAINTENANCE");
        assertThatThrownBy(() -> invokeResolve(gateway, "BAD", "", generic)).hasCauseInstanceOf(BusinessException.class);
    }

    @Test
    void startAndStopInferCommandFromDeviceTypeAndNames() throws Exception {
        HuaweiIotHardwareGateway gateway = gateway();

        assertCommand(invokeResolve(gateway, "START", "3", device("Fan 1", "", "VENTILATION_FAN", "iot")), "Fengdegree", "fengdegree", 3);
        assertCommand(invokeResolve(gateway, "STOP", "", device("Fan 1", "", "VENTILATION_FAN", "iot")), "Fengdegree", "fengdegree", 0);
        assertCommand(invokeResolve(gateway, "ON", "", device("Pump", "", "IRRIGATION_PUMP", "iot")), "BUMP", "bump", "ON");
        assertCommand(invokeResolve(gateway, "OFF", "", device("Pump", "", "IRRIGATION_PUMP", "iot")), "BUMP", "bump", "OFF");
        assertCommand(invokeResolve(gateway, "OPEN", "", device("Top light", "", "", "iot")), "Light", "light", "ON");
        assertCommand(invokeResolve(gateway, "CLOSE", "", device("Light", "", "", "iot")), "Light", "light", "OFF");
        assertCommand(invokeResolve(gateway, "START", "", device("shade board", "", "", "iot")), "Board", "board", "ON");
        assertCommand(invokeResolve(gateway, "STOP", "", device("board", "", "", "iot")), "Board", "board", "OFF");
        assertCommand(invokeResolve(gateway, "START", "", device("unknown", "", "", "iot")), "State", "state", "ON");
        assertCommand(invokeResolve(gateway, "STOP", "", device("unknown", "", "", "iot")), "State", "state", "OFF");
        assertCommand(invokeResolve(gateway, "START", "", device("feng box", "", "", "iot")), "Fengdegree", "fengdegree", 5);
        assertCommand(invokeResolve(gateway, "STOP", "", device("bump box", "", "", "iot")), "BUMP", "bump", "OFF");
    }

    @Test
    void helperMethodsCoverBlankTextAndContainsAny() throws Exception {
        HuaweiIotHardwareGateway gateway = gateway();
        assertThat((String) invoke(gateway, "text", new Class<?>[]{String.class}, " x ")).isEqualTo("x");
        assertThat((String) invoke(gateway, "text", new Class<?>[]{String.class}, new Object[]{null})).isEqualTo("");
        assertThat((Boolean) invoke(gateway, "blank", new Class<?>[]{String.class}, " ")).isTrue();
        assertThat((Boolean) invoke(gateway, "configured", new Class<?>[]{})).isTrue();
        assertThat((Boolean) invoke(gateway, "containsAny", new Class<?>[]{deviceClass(), String[].class},
                device("Vent", "fan", "", "iot"), new String[]{"fan"})).isTrue();
    }

    private HuaweiIotHardwareGateway gateway() {
        return new HuaweiIotHardwareGateway(jdbcTemplate, "ak", "sk", "pid", "cn-north-4", "endpoint", "iot-1", "srv", "inst", true);
    }

    private void stubDeviceRow(boolean present, String serialNo, String name, String category, String typeCode) {
        when(jdbcTemplate.query(any(String.class), any(ResultSetExtractor.class), any()))
                .thenAnswer(invocation -> {
                    ResultSetExtractor<?> extractor = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.next()).thenReturn(present);
                    when(rs.getLong("id")).thenReturn(1L);
                    when(rs.getString("serial_no")).thenReturn(serialNo);
                    when(rs.getString("name")).thenReturn(name);
                    when(rs.getString("category")).thenReturn(category);
                    when(rs.getString("type_code")).thenReturn(typeCode);
                    return extractor.extractData(rs);
                });
    }

    private void stubNoDevice() {
        stubDeviceRow(false, null, null, null, null);
    }

    private void setClient(HuaweiIotHardwareGateway gateway, IoTDAClient client) throws Exception {
        Field field = HuaweiIotHardwareGateway.class.getDeclaredField("client");
        field.setAccessible(true);
        field.set(gateway, client);
    }

    private Object invokeResolve(HuaweiIotHardwareGateway gateway, String command, String value, Object device) throws Exception {
        return invoke(gateway, "resolveCommand", new Class<?>[]{DeviceCommandRequest.class, deviceClass()},
                new DeviceCommandRequest(1L, command, value), device);
    }

    private Object device(String name, String category, String typeCode, String iotDeviceId) throws Exception {
        Constructor<?> constructor = deviceClass().getDeclaredConstructor(Long.class, String.class, String.class, String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(1L, name, category, typeCode, iotDeviceId);
    }

    private Class<?> deviceClass() throws ClassNotFoundException {
        return Class.forName("com.morel.greenhouse.infrastructure.hardware.HuaweiIotHardwareGateway$DeviceInfo");
    }

    private void assertCommand(Object command, String name, String param, Object value) throws Exception {
        assertThat((String) invoke(command, "name", new Class<?>[]{})).isEqualTo(name);
        assertThat((String) invoke(command, "param", new Class<?>[]{})).isEqualTo(param);
        assertThat((Object) invoke(command, "value", new Class<?>[]{})).isEqualTo(value);
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(Object target, String name, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return (T) method.invoke(target, args);
    }
}
