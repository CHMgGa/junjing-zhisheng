package com.example.yangdujun.utils;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class LocalSettingManager {
    private static final String TAG = "LocalSettingManager";
    private static final String SETTINGS_FILE = "local_settings.json";
    private final StorageManager storageManager;

    public LocalSettingManager(Context context) {
        this.storageManager = StorageManager.getInstance(context);
        initializeSettingsFile();
    }

    // 初始化设置文件
    private void initializeSettingsFile() {
        if (!storageManager.exists(SETTINGS_FILE)) {
            JSONObject defaultSettings = getDefaultSettings();
            storageManager.saveJsonObject(SETTINGS_FILE, defaultSettings);
            Log.d(TAG, "Initialized settings file with default values");
        }
    }

    // 获取默认设置
    private JSONObject getDefaultSettings() {
        JSONObject settings = new JSONObject();
        
        // 通知设置
        settings.put("notificationEnabled", true);
        settings.put("alarmNotificationEnabled", true);
        settings.put("dataNotificationEnabled", false);
        
        // 数据刷新设置
        settings.put("dataRefreshInterval", 5); // 5秒
        settings.put("historyDataInterval", 60); // 60秒
        
        // 单位设置
        settings.put("temperatureUnit", "celsius"); // celsius 或 fahrenheit
        settings.put("humidityUnit", "percent"); // percent
        settings.put("lightUnit", "lux"); // lux
        
        // 告警阈值设置
        settings.put("temperatureMax", 35);
        settings.put("temperatureMin", 5);
        settings.put("humidityMax", 90);
        settings.put("humidityMin", 20);
        settings.put("co2Max", 2000);
        settings.put("phMax", 8.5);
        settings.put("phMin", 4.5);
        
        // 其他设置
        settings.put("autoControlEnabled", false);
        settings.put("language", "zh");
        settings.put("theme", "light");
        
        return settings;
    }

    // 保存设置
    public boolean saveSetting(String type, String enabled) {
        try {
            JSONObject settings = storageManager.readJsonObject(SETTINGS_FILE);
            if (settings == null) {
                settings = getDefaultSettings();
            }
            
            // 处理不同类型的设置
            switch (type) {
                case "notification":
                    settings.put("notificationEnabled", "true".equals(enabled));
                    break;
                case "alarmNotification":
                    settings.put("alarmNotificationEnabled", "true".equals(enabled));
                    break;
                case "dataNotification":
                    settings.put("dataNotificationEnabled", "true".equals(enabled));
                    break;
                case "autoControl":
                    settings.put("autoControlEnabled", "true".equals(enabled));
                    break;
                default:
                    Log.d(TAG, "Unknown setting type: " + type);
                    return false;
            }
            
            storageManager.saveJsonObject(SETTINGS_FILE, settings);
            Log.d(TAG, "Saved setting: " + type + " = " + enabled);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save setting", e);
            return false;
        }
    }

    // 保存设置（通用方法）
    public boolean saveSetting(String key, Object value) {
        try {
            JSONObject settings = storageManager.readJsonObject(SETTINGS_FILE);
            if (settings == null) {
                settings = getDefaultSettings();
            }
            
            settings.put(key, value);
            storageManager.saveJsonObject(SETTINGS_FILE, settings);
            Log.d(TAG, "Saved setting: " + key + " = " + value);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to save setting", e);
            return false;
        }
    }

    // 获取设置
    public Object getSetting(String key) {
        try {
            JSONObject settings = storageManager.readJsonObject(SETTINGS_FILE);
            if (settings != null && settings.containsKey(key)) {
                Object value = settings.get(key);
                Log.d(TAG, "Retrieved setting: " + key + " = " + value);
                return value;
            } else {
                // 获取默认值
                JSONObject defaultSettings = getDefaultSettings();
                Object defaultValue = defaultSettings.get(key);
                Log.d(TAG, "Setting not found, returning default: " + key + " = " + defaultValue);
                return defaultValue;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get setting", e);
            // 返回默认值
            JSONObject defaultSettings = getDefaultSettings();
            return defaultSettings.get(key);
        }
    }

    // 获取布尔类型设置
    public boolean getBooleanSetting(String key) {
        Object value = getSetting(key);
        if (value instanceof Boolean) {
            return (boolean) value;
        } else if (value instanceof String) {
            return "true".equals(value);
        }
        return false;
    }

    // 获取整数类型设置
    public int getIntSetting(String key) {
        Object value = getSetting(key);
        if (value instanceof Integer) {
            return (int) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        } else if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        return 0;
    }

    // 获取浮点数类型设置
    public double getDoubleSetting(String key) {
        Object value = getSetting(key);
        if (value instanceof Double) {
            return (double) value;
        } else if (value instanceof Integer) {
            return (int) value;
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    // 获取字符串类型设置
    public String getStringSetting(String key) {
        Object value = getSetting(key);
        if (value != null) {
            return value.toString();
        }
        return "";
    }

    // 重置所有设置到默认值
    public void resetAllSettings() {
        JSONObject defaultSettings = getDefaultSettings();
        storageManager.saveJsonObject(SETTINGS_FILE, defaultSettings);
        Log.d(TAG, "Reset all settings to default values");
    }

    // 清除所有设置
    public void clearAllSettings() {
        storageManager.delete(SETTINGS_FILE);
        Log.d(TAG, "Cleared all settings");
    }

    // 获取所有设置
    public JSONObject getAllSettings() {
        try {
            JSONObject settings = storageManager.readJsonObject(SETTINGS_FILE);
            if (settings == null) {
                settings = getDefaultSettings();
            }
            Log.d(TAG, "Retrieved all settings");
            return settings;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get all settings", e);
            return getDefaultSettings();
        }
    }

    // 检查设置是否存在
    public boolean hasSetting(String key) {
        try {
            JSONObject settings = storageManager.readJsonObject(SETTINGS_FILE);
            return settings != null && settings.containsKey(key);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check setting existence", e);
            return false;
        }
    }
}


