package com.example.yangdujun.utils;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class ManagePageStorage {
    private static final String TAG = "ManagePageStorage";
    private static final String DEVICE_STATUS_FILE = "device_status.json";
    private static final String ALARM_THRESHOLDS_FILE = "alarm_thresholds.json";
    private static final String HISTORY_ANALYSIS_FILE = "history_analysis.json";
    private static final String REAL_TIME_ANALYSIS_FILE = "real_time_analysis.json";
    private static final String DEVICE_MONITOR_FILE = "device_monitor.json";
    private static final String ALARM_RECORDS_FILE = "alarm_records.json";
    
    // 内存存储数据
    private JSONObject deviceStatus;
    private JSONObject alarmThresholds;
    private JSONObject historyAnalysis;
    private JSONObject realTimeAnalysis;
    private JSONObject deviceMonitor;
    private JSONArray alarmRecords;
    private StorageManager storageManager;

    public ManagePageStorage(Context context) {
        // 初始化存储管理器
        storageManager = StorageManager.getInstance(context);
        // 初始化内存存储
        deviceStatus = loadDeviceStatus();
        alarmThresholds = loadAlarmThresholds();
        historyAnalysis = loadHistoryAnalysis();
        realTimeAnalysis = loadRealTimeAnalysis();
        deviceMonitor = loadDeviceMonitor();
        alarmRecords = loadAlarmRecords();
    }

    public ManagePageStorage() {
        // 无参构造函数，仅用于内存存储
        deviceStatus = new JSONObject();
        alarmThresholds = new JSONObject();
        historyAnalysis = new JSONObject();
        realTimeAnalysis = new JSONObject();
        deviceMonitor = new JSONObject();
        alarmRecords = new JSONArray();
    }

    // 设备状态管理相关
    public void saveDeviceStatus(JSONObject deviceStatus) {
        try {
            this.deviceStatus = deviceStatus;
            // 持久化存储
            if (storageManager != null) {
                storageManager.saveJsonObject(DEVICE_STATUS_FILE, deviceStatus);
            }
            Log.d(TAG, "设备状态保存成功");
        } catch (Exception e) {
            Log.e(TAG, "保存设备状态失败: " + e.getMessage());
        }
    }

    public JSONObject getDeviceStatus() {
        try {
            return deviceStatus;
        } catch (Exception e) {
            Log.e(TAG, "获取设备状态失败: " + e.getMessage());
        }
        return new JSONObject();
    }

    private JSONObject loadDeviceStatus() {
        if (storageManager != null) {
            JSONObject data = storageManager.readJsonObject(DEVICE_STATUS_FILE);
            if (data != null) {
                return data;
            }
        }
        return new JSONObject();
    }

    // 告警阈值设置相关
    public void saveAlarmThresholds(JSONObject thresholds) {
        try {
            this.alarmThresholds = thresholds;
            // 持久化存储
            if (storageManager != null) {
                storageManager.saveJsonObject(ALARM_THRESHOLDS_FILE, thresholds);
            }
            Log.d(TAG, "告警阈值保存成功");
        } catch (Exception e) {
            Log.e(TAG, "保存告警阈值失败: " + e.getMessage());
        }
    }

    public JSONObject getAlarmThresholds() {
        try {
            return alarmThresholds;
        } catch (Exception e) {
            Log.e(TAG, "获取告警阈值失败: " + e.getMessage());
        }
        return new JSONObject();
    }

    private JSONObject loadAlarmThresholds() {
        if (storageManager != null) {
            JSONObject data = storageManager.readJsonObject(ALARM_THRESHOLDS_FILE);
            if (data != null) {
                return data;
            }
        }
        return new JSONObject();
    }

    // 历史数据分析相关
    public void saveHistoryAnalysis(JSONObject analysisData) {
        try {
            this.historyAnalysis = analysisData;
            // 持久化存储
            if (storageManager != null) {
                storageManager.saveJsonObject(HISTORY_ANALYSIS_FILE, analysisData);
            }
            Log.d(TAG, "历史数据分析保存成功");
        } catch (Exception e) {
            Log.e(TAG, "保存历史数据分析失败: " + e.getMessage());
        }
    }

    public JSONObject getHistoryAnalysis() {
        try {
            return historyAnalysis;
        } catch (Exception e) {
            Log.e(TAG, "获取历史数据分析失败: " + e.getMessage());
        }
        return new JSONObject();
    }

    private JSONObject loadHistoryAnalysis() {
        if (storageManager != null) {
            JSONObject data = storageManager.readJsonObject(HISTORY_ANALYSIS_FILE);
            if (data != null) {
                return data;
            }
        }
        return new JSONObject();
    }

    // 实时数据分析相关
    public void saveRealTimeAnalysis(JSONObject analysisData) {
        try {
            this.realTimeAnalysis = analysisData;
            // 持久化存储
            if (storageManager != null) {
                storageManager.saveJsonObject(REAL_TIME_ANALYSIS_FILE, analysisData);
            }
            Log.d(TAG, "实时数据分析保存成功");
        } catch (Exception e) {
            Log.e(TAG, "保存实时数据分析失败: " + e.getMessage());
        }
    }

    public JSONObject getRealTimeAnalysis() {
        try {
            return realTimeAnalysis;
        } catch (Exception e) {
            Log.e(TAG, "获取实时数据分析失败: " + e.getMessage());
        }
        return new JSONObject();
    }

    private JSONObject loadRealTimeAnalysis() {
        if (storageManager != null) {
            JSONObject data = storageManager.readJsonObject(REAL_TIME_ANALYSIS_FILE);
            if (data != null) {
                return data;
            }
        }
        return new JSONObject();
    }

    // 设备调控相关
    public void saveDeviceMonitor(JSONObject monitorData) {
        try {
            this.deviceMonitor = monitorData;
            // 持久化存储
            if (storageManager != null) {
                storageManager.saveJsonObject(DEVICE_MONITOR_FILE, monitorData);
            }
            Log.d(TAG, "设备调控数据保存成功");
        } catch (Exception e) {
            Log.e(TAG, "保存设备调控数据失败: " + e.getMessage());
        }
    }

    public JSONObject getDeviceMonitor() {
        try {
            return deviceMonitor;
        } catch (Exception e) {
            Log.e(TAG, "获取设备调控数据失败: " + e.getMessage());
        }
        return new JSONObject();
    }

    private JSONObject loadDeviceMonitor() {
        if (storageManager != null) {
            JSONObject data = storageManager.readJsonObject(DEVICE_MONITOR_FILE);
            if (data != null) {
                return data;
            }
        }
        return new JSONObject();
    }

    // 告警记录相关
    public void saveAlarmRecords(JSONArray alarmRecords) {
        try {
            this.alarmRecords = alarmRecords;
            // 持久化存储
            if (storageManager != null) {
                storageManager.saveJsonArray(ALARM_RECORDS_FILE, alarmRecords);
            }
            Log.d(TAG, "告警记录保存成功");
        } catch (Exception e) {
            Log.e(TAG, "保存告警记录失败: " + e.getMessage());
        }
    }

    public JSONArray getAlarmRecords() {
        try {
            return alarmRecords;
        } catch (Exception e) {
            Log.e(TAG, "获取告警记录失败: " + e.getMessage());
        }
        return new JSONArray();
    }

    private JSONArray loadAlarmRecords() {
        if (storageManager != null) {
            JSONArray data = storageManager.readJsonArray(ALARM_RECORDS_FILE);
            if (data != null) {
                return data;
            }
        }
        return new JSONArray();
    }

    // 清空所有数据
    public void clearAllData() {
        try {
            deviceStatus = new JSONObject();
            alarmThresholds = new JSONObject();
            historyAnalysis = new JSONObject();
            realTimeAnalysis = new JSONObject();
            deviceMonitor = new JSONObject();
            alarmRecords = new JSONArray();
            Log.d(TAG, "所有数据清除成功");
        } catch (Exception e) {
            Log.e(TAG, "清除数据失败: " + e.getMessage());
        }
    }
}


