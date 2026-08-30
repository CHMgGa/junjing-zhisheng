package com.morel.greenhouse.infrastructure.hardware;

import com.morel.greenhouse.application.dto.DeviceCommandRequest;
import org.junit.jupiter.api.Test;

class MockHardwareGatewayTest {

    @Test
    void dispatchDeviceCommandLogsWithoutThrowing() {
        new MockHardwareGateway().dispatchDeviceCommand(new DeviceCommandRequest(1L, "START", "ON"));
    }
}
