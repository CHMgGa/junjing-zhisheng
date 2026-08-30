package com.morel.greenhouse.infrastructure.hardware;

import com.morel.greenhouse.application.dto.DeviceCommandRequest;
import com.morel.greenhouse.application.port.HardwareGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
public class MockHardwareGateway implements HardwareGateway {
    private static final Logger log = LoggerFactory.getLogger(MockHardwareGateway.class);

    @Override
    public void dispatchDeviceCommand(DeviceCommandRequest request) {
        log.info("Mock hardware command dispatched: deviceId={}, command={}, value={}",
                request.deviceId(), request.command(), request.value());
    }
}
