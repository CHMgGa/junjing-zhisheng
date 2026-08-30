package com.example.yangdujun.utils;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Random;
import java.util.UUID;

public class LocalAlarmManager {
    private static final String TAG = "LocalAlarmManager";
    private static final String ALARMS_FILE = "local_alarms.json";
    private final StorageManager storageManager;
    private final Random random;

    public LocalAlarmManager(Context context) {
        this.storageManager = StorageManager.getInstance(context);
        this.random = new Random();
        initializeAlarmsFile();
    }

    // 初始化告警文件
    private void initializeAlarmsFile() {
        if (!storageManager.exists(ALARMS_FILE)) {
            JSONObject root = new JSONObject();
            root.put("alarms", new JSONArray());
            storageManager.saveJsonObject(ALARMS_FILE, root);
            Log.d(TAG, "Initialized alarms file");
            // 生成一些默认告警
            generateDefaultAlarms();
        }
    }

    // 生成默认告警
    private void generateDefaultAlarms() {
        String[] alarmTypes = {
            "温度异常", "湿度异常", "光照异常", "CO2浓度异常",
            "土壤湿度异常", "土壤温度异常", "pH值异常", "氧气浓度异常"
        };

        try {
            JSONObject alarmsData = storageManager.readJsonObject(ALARMS_FILE);
            JSONArray alarms = alarmsData.getJSONArray("alarms");

            for (int i = 0; i < 3; i++) {
                JSONObject alarm = generateAlarm("default", alarmTypes[random.nextInt(alarmTypes.length)]);
                alarms.add(alarm);
            }

            alarmsData.put("alarms", alarms);
            storageManager.saveJsonObject(ALARMS_FILE, alarmsData);
            Log.d(TAG, "Generated default alarms");
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate default alarms", e);
        }
    }

    // 生成告警
    public JSONObject generateAlarm(String greenhouseId, String type) {
        JSONObject alarm = new JSONObject();
        alarm.put("id", "alarm_" + UUID.randomUUID().toString().substring(0, 8));
        alarm.put("greenhouseId", greenhouseId);
        alarm.put("type", type);
        alarm.put("message", type + "告警，请及时处理");
        alarm.put("status", "unhandled");
        alarm.put("severity", random.nextInt(3) + 1); // 1-3级
        alarm.put("timestamp", System.currentTimeMillis());
        alarm.put("handledAt", 0);
        return alarm;
    }

    // 获取告警列表
    public JSONArray getAlarmList(String greenhouseId) {
        try {
            JSONObject alarmsData = storageManager.readJsonObject(ALARMS_FILE);
            JSONArray allAlarms = alarmsData.getJSONArray("alarms");
            JSONArray filteredAlarms = new JSONArray();

            for (int i = 0; i < allAlarms.size(); i++) {
                JSONObject alarm = allAlarms.getJSONObject(i);
                if (alarm.getString("greenhouseId").equals(greenhouseId)) {
                    filteredAlarms.add(alarm);
                }
            }

            Log.d(TAG, "Retrieved alarm list for greenhouse " + greenhouseId + ", count: " + filteredAlarms.size());
            return filteredAlarms;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get alarm list", e);
            return new JSONArray();
        }
    }

    // 处理告警
    public boolean handleAlarm(String alarmId, String handlerId, String handlerName) {
        try {
            JSONObject alarmsData = storageManager.readJsonObject(ALARMS_FILE);
            JSONArray alarms = alarmsData.getJSONArray("alarms");

            for (int i = 0; i < alarms.size(); i++) {
                JSONObject alarm = alarms.getJSONObject(i);
                if (alarm.getString("id").equals(alarmId)) {
                    alarm.put("status", "handled");
                    alarm.put("handledAt", System.currentTimeMillis());
                    alarm.put("handlerId", handlerId);
                    alarm.put("handlerName", handlerName);

                    alarmsData.put("alarms", alarms);
                    storageManager.saveJsonObject(ALARMS_FILE, alarmsData);

                    Log.d(TAG, "Handled alarm: " + alarmId + " by " + handlerName);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to handle alarm", e);
        }
        return false;
    }

    // 处理告警（兼容旧版本）
    public boolean handleAlarm(String alarmId) {
        return handleAlarm(alarmId, "unknown", "未知用户");
    }

    // 添加告警
    public JSONObject addAlarm(String greenhouseId, String type, String message, int severity) {
        try {
            JSONObject alarm = new JSONObject();
            alarm.put("id", "alarm_" + UUID.randomUUID().toString().substring(0, 8));
            alarm.put("greenhouseId", greenhouseId);
            alarm.put("type", type);
            alarm.put("message", message);
            alarm.put("status", "unhandled");
            alarm.put("severity", severity);
            alarm.put("timestamp", System.currentTimeMillis());
            alarm.put("handledAt", 0);

            JSONObject alarmsData = storageManager.readJsonObject(ALARMS_FILE);
            JSONArray alarms = alarmsData.getJSONArray("alarms");
            alarms.add(alarm);
            alarmsData.put("alarms", alarms);
            storageManager.saveJsonObject(ALARMS_FILE, alarmsData);

            Log.d(TAG, "Added alarm: " + type + " for greenhouse: " + greenhouseId);
            return alarm;
        } catch (Exception e) {
            Log.e(TAG, "Failed to add alarm", e);
            return null;
        }
    }

    // 删除告警
    public boolean deleteAlarm(String alarmId) {
        try {
            JSONObject alarmsData = storageManager.readJsonObject(ALARMS_FILE);
            JSONArray alarms = alarmsData.getJSONArray("alarms");

            for (int i = 0; i < alarms.size(); i++) {
                JSONObject alarm = alarms.getJSONObject(i);
                if (alarm.getString("id").equals(alarmId)) {
                    alarms.remove(i);
                    alarmsData.put("alarms", alarms);
                    storageManager.saveJsonObject(ALARMS_FILE, alarmsData);

                    Log.d(TAG, "Deleted alarm: " + alarmId);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete alarm", e);
        }
        return false;
    }

    // 获取告警详情
    public JSONObject getAlarmDetail(String alarmId) {
        try {
            JSONObject alarmsData = storageManager.readJsonObject(ALARMS_FILE);
            JSONArray alarms = alarmsData.getJSONArray("alarms");

            for (int i = 0; i < alarms.size(); i++) {
                JSONObject alarm = alarms.getJSONObject(i);
                if (alarm.getString("id").equals(alarmId)) {
                    Log.d(TAG, "Retrieved alarm detail: " + alarmId);
                    return alarm;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get alarm detail", e);
        }
        return null;
    }

    // 获取未处理告警数量
    public int getUnhandledAlarmCount(String greenhouseId) {
        try {
            JSONObject alarmsData = storageManager.readJsonObject(ALARMS_FILE);
            JSONArray alarms = alarmsData.getJSONArray("alarms");
            int count = 0;

            for (int i = 0; i < alarms.size(); i++) {
                JSONObject alarm = alarms.getJSONObject(i);
                if (alarm.getString("greenhouseId").equals(greenhouseId) && 
                    "unhandled".equals(alarm.getString("status"))) {
                    count++;
                }
            }

            Log.d(TAG, "Unhandled alarm count for greenhouse " + greenhouseId + ": " + count);
            return count;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get unhandled alarm count", e);
            return 0;
        }
    }

    // 检查环境数据并生成告警
    public JSONArray checkEnvironmentDataAndGenerateAlarms(String greenhouseId, JSONObject environmentData) {
        JSONArray generatedAlarms = new JSONArray();

        try {
            // 检查温度
            double temperature = Double.parseDouble(environmentData.getString("temperature"));
            if (temperature > 35) {
                JSONObject alarm = addAlarm(greenhouseId, "温度异常", "温度过高: " + temperature + "°C", 3);
                if (alarm != null) generatedAlarms.add(alarm);
            } else if (temperature < 5) {
                JSONObject alarm = addAlarm(greenhouseId, "温度异常", "温度过低: " + temperature + "°C", 2);
                if (alarm != null) generatedAlarms.add(alarm);
            }

            // 检查湿度
            double humidity = Double.parseDouble(environmentData.getString("humidity"));
            if (humidity > 90) {
                JSONObject alarm = addAlarm(greenhouseId, "湿度异常", "湿度过高: " + humidity + "%", 2);
                if (alarm != null) generatedAlarms.add(alarm);
            } else if (humidity < 20) {
                JSONObject alarm = addAlarm(greenhouseId, "湿度异常", "湿度过低: " + humidity + "%", 2);
                if (alarm != null) generatedAlarms.add(alarm);
            }

            // 检查CO2
            double co2 = Double.parseDouble(environmentData.getString("co2"));
            if (co2 > 2000) {
                JSONObject alarm = addAlarm(greenhouseId, "CO2浓度异常", "CO2浓度过高: " + co2 + "ppm", 3);
                if (alarm != null) generatedAlarms.add(alarm);
            }

            // 检查pH值
            double ph = Double.parseDouble(environmentData.getString("ph"));
            if (ph > 8.5) {
                JSONObject alarm = addAlarm(greenhouseId, "pH值异常", "pH值过高: " + ph, 2);
                if (alarm != null) generatedAlarms.add(alarm);
            } else if (ph < 4.5) {
                JSONObject alarm = addAlarm(greenhouseId, "pH值异常", "pH值过低: " + ph, 2);
                if (alarm != null) generatedAlarms.add(alarm);
            }

            Log.d(TAG, "Generated " + generatedAlarms.size() + " alarms for greenhouse " + greenhouseId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to check environment data and generate alarms", e);
        }

        return generatedAlarms;
    }

    // 清空大棚告警
    public boolean clearGreenhouseAlarms(String greenhouseId) {
        try {
            JSONObject alarmsData = storageManager.readJsonObject(ALARMS_FILE);
            JSONArray alarms = alarmsData.getJSONArray("alarms");
            JSONArray remainingAlarms = new JSONArray();

            for (int i = 0; i < alarms.size(); i++) {
                JSONObject alarm = alarms.getJSONObject(i);
                if (!alarm.getString("greenhouseId").equals(greenhouseId)) {
                    remainingAlarms.add(alarm);
                }
            }

            alarmsData.put("alarms", remainingAlarms);
            storageManager.saveJsonObject(ALARMS_FILE, alarmsData);

            Log.d(TAG, "Cleared alarms for greenhouse " + greenhouseId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear greenhouse alarms", e);
            return false;
        }
    }

    // 获取所有告警
    public JSONArray getAllAlarms() {
        try {
            JSONObject alarmsData = storageManager.readJsonObject(ALARMS_FILE);
            JSONArray alarms = alarmsData.getJSONArray("alarms");
            Log.d(TAG, "Retrieved all alarms, count: " + alarms.size());
            return alarms;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get all alarms", e);
            return new JSONArray();
        }
    }
}


