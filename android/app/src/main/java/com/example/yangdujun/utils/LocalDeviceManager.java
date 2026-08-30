package com.example.yangdujun.utils;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.UUID;

public class LocalDeviceManager {
    private static final String TAG = "LocalDeviceManager";
    private static final String DEVICES_FILE = "local_devices.json";
    private final StorageManager storageManager;

    public LocalDeviceManager(Context context) {
        this.storageManager = StorageManager.getInstance(context);
        initializeDevicesFile();
    }

    // 初始化设备文件
    private void initializeDevicesFile() {
        if (!storageManager.exists(DEVICES_FILE)) {
            JSONObject root = new JSONObject();
            root.put("devices", new JSONArray());
            storageManager.saveJsonObject(DEVICES_FILE, root);
            Log.d(TAG, "Initialized devices file");
            // 为默认大棚添加默认设备
            addDefaultDevices();
        }
    }

    // 为默认大棚添加默认设备
    private void addDefaultDevices() {
        String[] deviceNames = {
            "温度传感器", "湿度传感器", "光照传感器", "CO2传感器",
            "土壤湿度传感器", "土壤温度传感器", "pH传感器", "氧气传感器",
            "风扇", "灯光", "挡风板"
        };

        String[] deviceTypes = {
            "sensor", "sensor", "sensor", "sensor",
            "sensor", "sensor", "sensor", "sensor",
            "actuator", "actuator", "actuator"
        };

        try {
            JSONObject devicesData = storageManager.readJsonObject(DEVICES_FILE);
            JSONArray devices = devicesData.getJSONArray("devices");

            for (int i = 0; i < deviceNames.length; i++) {
                JSONObject device = new JSONObject();
                device.put("id", "device_" + UUID.randomUUID().toString().substring(0, 8));
                device.put("greenhouseId", "1");
                device.put("name", deviceNames[i]);
                device.put("type", deviceTypes[i]);
                device.put("status", deviceTypes[i].equals("sensor") ? "online" : "off");
                device.put("value", deviceTypes[i].equals("sensor") ? "0" : "");
                device.put("createdAt", System.currentTimeMillis());
                devices.add(device);
            }

            devicesData.put("devices", devices);
            storageManager.saveJsonObject(DEVICES_FILE, devicesData);
            Log.d(TAG, "Added default devices for default greenhouse");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add default devices", e);
        }
    }

    // 获取大棚设备列表
    public JSONArray getDeviceList(String greenhouseId) {
        try {
            JSONObject devicesData = storageManager.readJsonObject(DEVICES_FILE);
            JSONArray devices = devicesData.getJSONArray("devices");
            JSONArray greenhouseDevices = new JSONArray();

            for (int i = 0; i < devices.size(); i++) {
                JSONObject device = devices.getJSONObject(i);
                if (device.getString("greenhouseId").equals(greenhouseId)) {
                    greenhouseDevices.add(device);
                }
            }

            Log.d(TAG, "Retrieved devices for greenhouse " + greenhouseId + ", count: " + greenhouseDevices.size());
            return greenhouseDevices;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get device list", e);
            return new JSONArray();
        }
    }

    // 控制设备
    public boolean controlDevice(String deviceId, String status) {
        try {
            JSONObject devicesData = storageManager.readJsonObject(DEVICES_FILE);
            JSONArray devices = devicesData.getJSONArray("devices");

            for (int i = 0; i < devices.size(); i++) {
                JSONObject device = devices.getJSONObject(i);
                if (device.getString("id").equals(deviceId)) {
                    device.put("status", status);
                    device.put("updatedAt", System.currentTimeMillis());

                    devicesData.put("devices", devices);
                    storageManager.saveJsonObject(DEVICES_FILE, devicesData);

                    Log.d(TAG, "Controlled device " + deviceId + " to status: " + status);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to control device", e);
        }
        return false;
    }

    // 获取设备状态
    public JSONObject getDeviceStatus(String deviceId) {
        try {
            JSONObject devicesData = storageManager.readJsonObject(DEVICES_FILE);
            JSONArray devices = devicesData.getJSONArray("devices");

            for (int i = 0; i < devices.size(); i++) {
                JSONObject device = devices.getJSONObject(i);
                if (device.getString("id").equals(deviceId)) {
                    JSONObject statusInfo = new JSONObject();
                    statusInfo.put("deviceId", deviceId);
                    statusInfo.put("status", device.getString("status"));
                    statusInfo.put("value", device.getString("value"));
                    Log.d(TAG, "Retrieved status for device " + deviceId + ": " + device.getString("status"));
                    return statusInfo;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get device status", e);
        }
        return null;
    }

    // 添加设备
    public JSONObject addDevice(String greenhouseId, String name, String type) {
        try {
            JSONObject newDevice = new JSONObject();
            newDevice.put("id", "device_" + UUID.randomUUID().toString().substring(0, 8));
            newDevice.put("greenhouseId", greenhouseId);
            newDevice.put("name", name);
            newDevice.put("type", type);
            newDevice.put("status", type.equals("sensor") ? "online" : "off");
            newDevice.put("value", type.equals("sensor") ? "0" : "");
            newDevice.put("createdAt", System.currentTimeMillis());

            JSONObject devicesData = storageManager.readJsonObject(DEVICES_FILE);
            JSONArray devices = devicesData.getJSONArray("devices");
            devices.add(newDevice);
            devicesData.put("devices", devices);
            storageManager.saveJsonObject(DEVICES_FILE, devicesData);

            Log.d(TAG, "Added device " + name + " to greenhouse " + greenhouseId);
            return newDevice;
        } catch (Exception e) {
            Log.e(TAG, "Failed to add device", e);
            return null;
        }
    }

    // 删除设备
    public boolean deleteDevice(String deviceId) {
        try {
            JSONObject devicesData = storageManager.readJsonObject(DEVICES_FILE);
            JSONArray devices = devicesData.getJSONArray("devices");

            for (int i = 0; i < devices.size(); i++) {
                JSONObject device = devices.getJSONObject(i);
                if (device.getString("id").equals(deviceId)) {
                    devices.remove(i);
                    devicesData.put("devices", devices);
                    storageManager.saveJsonObject(DEVICES_FILE, devicesData);

                    Log.d(TAG, "Deleted device " + deviceId);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete device", e);
        }
        return false;
    }

    // 更新设备信息
    public boolean updateDevice(String deviceId, String name, String type) {
        try {
            JSONObject devicesData = storageManager.readJsonObject(DEVICES_FILE);
            JSONArray devices = devicesData.getJSONArray("devices");

            for (int i = 0; i < devices.size(); i++) {
                JSONObject device = devices.getJSONObject(i);
                if (device.getString("id").equals(deviceId)) {
                    device.put("name", name);
                    device.put("type", type);
                    device.put("updatedAt", System.currentTimeMillis());

                    devicesData.put("devices", devices);
                    storageManager.saveJsonObject(DEVICES_FILE, devicesData);

                    Log.d(TAG, "Updated device " + deviceId);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update device", e);
        }
        return false;
    }

    // 更新传感器值
    public boolean updateSensorValue(String deviceId, String value) {
        try {
            JSONObject devicesData = storageManager.readJsonObject(DEVICES_FILE);
            JSONArray devices = devicesData.getJSONArray("devices");

            for (int i = 0; i < devices.size(); i++) {
                JSONObject device = devices.getJSONObject(i);
                if (device.getString("id").equals(deviceId) && "sensor".equals(device.getString("type"))) {
                    device.put("value", value);
                    device.put("updatedAt", System.currentTimeMillis());

                    devicesData.put("devices", devices);
                    storageManager.saveJsonObject(DEVICES_FILE, devicesData);

                    Log.d(TAG, "Updated sensor value for device " + deviceId + ": " + value);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update sensor value", e);
        }
        return false;
    }

    // 获取所有设备
    public JSONArray getAllDevices() {
        try {
            JSONObject devicesData = storageManager.readJsonObject(DEVICES_FILE);
            JSONArray devices = devicesData.getJSONArray("devices");
            Log.d(TAG, "Retrieved all devices, count: " + devices.size());
            return devices;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get all devices", e);
            return new JSONArray();
        }
    }

    // 清空大棚设备
    public boolean clearGreenhouseDevices(String greenhouseId) {
        try {
            JSONObject devicesData = storageManager.readJsonObject(DEVICES_FILE);
            JSONArray devices = devicesData.getJSONArray("devices");
            JSONArray remainingDevices = new JSONArray();

            for (int i = 0; i < devices.size(); i++) {
                JSONObject device = devices.getJSONObject(i);
                if (!device.getString("greenhouseId").equals(greenhouseId)) {
                    remainingDevices.add(device);
                }
            }

            devicesData.put("devices", remainingDevices);
            storageManager.saveJsonObject(DEVICES_FILE, devicesData);

            Log.d(TAG, "Cleared devices for greenhouse " + greenhouseId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear greenhouse devices", e);
            return false;
        }
    }
}


