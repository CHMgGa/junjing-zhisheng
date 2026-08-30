package com.example.yangdujun.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class ConfigStorage {
    private static final String PREF_NAME = "app_config";
    private static final String KEY_FEATURE_CONFIG = "feature_config";

    private SharedPreferences preferences;

    public ConfigStorage(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 获取功能配置
    public JSONObject getFeatureConfig() {
        String configStr = preferences.getString(KEY_FEATURE_CONFIG, "");
        if (configStr.isEmpty()) {
            // 如果配置不存在，返回默认配置
            return getDefaultFeatureConfig();
        }
        try {
            return JSON.parseObject(configStr);
        } catch (Exception e) {
            // 如果解析失败，返回默认配置
            return getDefaultFeatureConfig();
        }
    }

    // 保存功能配置
    public void saveFeatureConfig(JSONObject config) {
        preferences.edit().putString(KEY_FEATURE_CONFIG, config.toJSONString()).apply();
    }

    // 获取默认功能配置
    private JSONObject getDefaultFeatureConfig() {
        JSONObject config = new JSONObject();

        // 我的大棚配置
        JSONObject greenhouseConfig = new JSONObject();
        greenhouseConfig.put("enabled", true);
        greenhouseConfig.put("title", "我的大棚");
        greenhouseConfig.put("description", "查看和管理您的大棚");
        config.put("greenhouse", greenhouseConfig);

        // 问题反馈配置
        JSONObject feedbackConfig = new JSONObject();
        feedbackConfig.put("enabled", true);
        feedbackConfig.put("title", "问题反馈");
        feedbackConfig.put("description", "提交您的问题和建议");
        config.put("feedback", feedbackConfig);

        // 设置配置
        JSONObject settingsConfig = new JSONObject();
        settingsConfig.put("enabled", true);
        settingsConfig.put("title", "设置");
        settingsConfig.put("description", "应用设置和偏好");
        config.put("settings", settingsConfig);

        // AI对话配置
        JSONObject aiConfig = new JSONObject();
        aiConfig.put("enabled", true);
        aiConfig.put("title", "AI对话");
        aiConfig.put("description", "智能农业助手");
        config.put("ai", aiConfig);

        return config;
    }

    // 更新单个功能配置
    public void updateFeatureConfig(String featureKey, JSONObject featureConfig) {
        JSONObject config = getFeatureConfig();
        config.put(featureKey, featureConfig);
        saveFeatureConfig(config);
    }

    // 检查功能是否启用
    public boolean isFeatureEnabled(String featureKey) {
        JSONObject config = getFeatureConfig();
        JSONObject featureConfig = config.getJSONObject(featureKey);
        return featureConfig != null && featureConfig.getBooleanValue("enabled");
    }

    // 获取功能标题
    public String getFeatureTitle(String featureKey) {
        JSONObject config = getFeatureConfig();
        JSONObject featureConfig = config.getJSONObject(featureKey);
        return featureConfig != null ? featureConfig.getString("title") : "";
    }

    // 获取功能描述
    public String getFeatureDescription(String featureKey) {
        JSONObject config = getFeatureConfig();
        JSONObject featureConfig = config.getJSONObject(featureKey);
        return featureConfig != null ? featureConfig.getString("description") : "";
    }

    // 清除所有配置
    public void clearConfig() {
        preferences.edit().clear().apply();
    }
}

