package com.example.yangdujun;

public class DeviceBean {
    private String type;    // 设备类型
    private String name;    // 设备名称
    private boolean status; // 设备状态
    private String deviceId; // 设备ID

    public DeviceBean(String type, String name, boolean status) {
        this.type = type;
        this.name = name;
        this.status = status;
    }

    public DeviceBean(String type, String name, boolean status, String deviceId) {
        this.type = type;
        this.name = name;
        this.status = status;
        this.deviceId = deviceId;
    }

    // getter方法
    public String getType() { return type; }
    public String getName() { return name; }
    public boolean isStatus() { return status; }
    public String getDeviceId() { return deviceId; }

    // setter方法
    public void setStatus(boolean status) { this.status = status; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}

