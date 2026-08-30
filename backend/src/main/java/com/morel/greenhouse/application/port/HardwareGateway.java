package com.morel.greenhouse.application.port;

import com.morel.greenhouse.application.dto.DeviceCommandRequest;

public interface HardwareGateway {
    void dispatchDeviceCommand(DeviceCommandRequest request);
}
