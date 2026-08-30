package com.example.yangdujun.utils;

import com.alibaba.fastjson.JSONObject;

public class SettingsStorage {
    private JSONObject userSettings;
    private String appVersion;

    public SettingsStorage() {
        // 初始化默认设置
        userSettings = getDefaultUserSettings();
        appVersion = "1.0.0";
    }

    // 获取用户设置
    public JSONObject getUserSettings() {
        return userSettings;
    }

    // 保存用户设置
    public void saveUserSettings(JSONObject settings) {
        this.userSettings = settings;
    }

    // 获取默认用户设置
    private JSONObject getDefaultUserSettings() {
        JSONObject settings = new JSONObject();

        // 声音设置
        JSONObject soundSettings = new JSONObject();
        soundSettings.put("push", true);
        soundSettings.put("alarm", true);
        settings.put("sound", soundSettings);

        // 其他设置
        settings.put("notifications", true);
        settings.put("autoUpdate", false);

        return settings;
    }

    // 获取应用版本
    public String getAppVersion() {
        return appVersion;
    }

    // 保存应用版本
    public void saveAppVersion(String version) {
        this.appVersion = version;
    }

    // 获取单个设置项
    public <T> T getSetting(String key, T defaultValue) {
        if (userSettings.containsKey(key)) {
            try {
                return (T) userSettings.get(key);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    // 保存单个设置项
    public void saveSetting(String key, Object value) {
        userSettings.put(key, value);
    }

    // 获取声音设置
    public boolean getSoundSetting(String type) {
        JSONObject soundSettings = userSettings.getJSONObject("sound");
        if (soundSettings != null && soundSettings.containsKey(type)) {
            return soundSettings.getBooleanValue(type);
        }
        return true; // 默认开启
    }

    // 保存声音设置
    public void saveSoundSetting(String type, boolean isEnabled) {
        JSONObject soundSettings = userSettings.getJSONObject("sound");
        if (soundSettings == null) {
            soundSettings = new JSONObject();
            userSettings.put("sound", soundSettings);
        }
        soundSettings.put(type, isEnabled);
    }

    // 清空所有设置
    public void clearSettings() {
        userSettings = getDefaultUserSettings();
    }
}

