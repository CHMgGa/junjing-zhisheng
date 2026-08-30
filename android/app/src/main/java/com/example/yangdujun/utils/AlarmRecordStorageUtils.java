package com.example.yangdujun.utils;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 告警记录存储工具类
 * 用于管理和持久化存储告警记录
 */
public class AlarmRecordStorageUtils {
    private static final String TAG = "AlarmRecordStorageUtils";
    private static final String ALARM_FILE_NAME = "alarm_records.json";
    private static final int MAX_ALARM_RECORDS = 500; // 最大存储告警记录数
    
    /**
     * 获取告警文件路径
     */
    private static String getAlarmFilePath(Context context) {
        File dataDir = context.getFilesDir();
        return dataDir.getAbsolutePath() + File.separator + ALARM_FILE_NAME;
    }
    
    /**
     * 保存告警记录
     */
    public static void saveAlarmRecord(Context context, JSONObject alarmRecord) {
        AsyncTaskUtils.executeAsync(() -> {
            try {
                JSONArray alarmArray = getAlarmRecordsSync(context);
                
                // 添加新告警记录到开头
                alarmArray.add(0, alarmRecord);
                
                // 限制记录数量
                if (alarmArray.size() > MAX_ALARM_RECORDS) {
                    JSONArray newAlarmArray = new JSONArray();
                    for (int i = 0; i < MAX_ALARM_RECORDS; i++) {
                        newAlarmArray.add(alarmArray.get(i));
                    }
                    alarmArray = newAlarmArray;
                }
                
                // 保存到文件
                String filePath = getAlarmFilePath(context);
                FileOutputStream fos = new FileOutputStream(filePath);
                String jsonString = JSON.toJSONString(alarmArray, true);
                fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
                fos.close();
                
                Log.d(TAG, "告警记录保存成功");
            } catch (IOException e) {
                Log.e(TAG, "告警记录保存失败: " + e.getMessage());
                e.printStackTrace();
            }
        }, null);
    }

    /**
     * 同步获取所有告警记录（用于后台线程）
     */
    private static JSONArray getAlarmRecordsSync(Context context) {
        try {
            String filePath = getAlarmFilePath(context);
            File file = new File(filePath);
            if (!file.exists()) {
                Log.d(TAG, "告警记录文件不存在，返回空数组");
                return new JSONArray();
            }
            
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            fis.close();
            
            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            return JSON.parseArray(jsonString);
        } catch (IOException e) {
            Log.e(TAG, "告警记录读取失败: " + e.getMessage());
            e.printStackTrace();
            return new JSONArray();
        }
    }
    
    /**
     * 获取所有告警记录（同步，用于后台线程）
     */
    public static JSONArray getAlarmRecords(Context context) {
        return getAlarmRecordsSync(context);
    }

    /**
     * 异步获取所有告警记录
     */
    public static void getAlarmRecordsAsync(Context context, AsyncTaskUtils.Callback<JSONArray> callback) {
        AsyncTaskUtils.executeAsync(() -> getAlarmRecordsSync(context), callback);
    }
    
    /**
     * 获取未处理的告警记录
     */
    public static JSONArray getUnhandledAlarms(Context context) {
        JSONArray allAlarms = getAlarmRecords(context);
        JSONArray unhandledAlarms = new JSONArray();
        
        for (int i = 0; i < allAlarms.size(); i++) {
            JSONObject alarm = allAlarms.getJSONObject(i);
            if (!alarm.getBooleanValue("isHandled")) {
                unhandledAlarms.add(alarm);
            }
        }
        
        return unhandledAlarms;
    }
    
    /**
     * 获取已处理的告警记录
     */
    public static JSONArray getHandledAlarms(Context context) {
        JSONArray allAlarms = getAlarmRecords(context);
        JSONArray handledAlarms = new JSONArray();
        
        for (int i = 0; i < allAlarms.size(); i++) {
            JSONObject alarm = allAlarms.getJSONObject(i);
            if (alarm.getBooleanValue("isHandled")) {
                handledAlarms.add(alarm);
            }
        }
        
        return handledAlarms;
    }
    
    /**
     * 更新告警处理状态
     */
    public static void updateAlarmStatus(Context context, String alarmId, boolean isHandled, String handlerId, String handlerName) {
        AsyncTaskUtils.executeAsync(() -> {
            try {
                JSONArray alarmArray = getAlarmRecordsSync(context);
                boolean updated = false;
                
                for (int i = 0; i < alarmArray.size(); i++) {
                    JSONObject alarm = alarmArray.getJSONObject(i);
                    if (alarm.getString("alarmId").equals(alarmId)) {
                        alarm.put("isHandled", isHandled);
                        alarm.put("handleTime", System.currentTimeMillis());
                        alarm.put("handlerId", handlerId);
                        alarm.put("handlerName", handlerName);
                        updated = true;
                        break;
                    }
                }
                
                if (updated) {
                    // 保存更新后的告警记录
                    String filePath = getAlarmFilePath(context);
                    FileOutputStream fos = new FileOutputStream(filePath);
                    String jsonString = JSON.toJSONString(alarmArray, true);
                    fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
                    fos.close();
                    Log.d(TAG, "告警状态更新成功，处理人: " + handlerName);
                }
            } catch (IOException e) {
                Log.e(TAG, "告警状态更新失败: " + e.getMessage());
                e.printStackTrace();
            }
        }, null);
    }

    /**
     * 更新告警处理状态（兼容旧版本）
     */
    public static void updateAlarmStatus(Context context, String alarmId, boolean isHandled) {
        updateAlarmStatus(context, alarmId, isHandled, "unknown", "未知用户");
    }
    
    /**
     * 清空告警记录
     */
    public static void clearAlarmRecords(Context context) {
        try {
            String filePath = getAlarmFilePath(context);
            FileOutputStream fos = new FileOutputStream(filePath);
            fos.write("[]".getBytes(StandardCharsets.UTF_8));
            fos.close();
            Log.d(TAG, "告警记录已清空");
        } catch (IOException e) {
            Log.e(TAG, "清空告警记录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取告警记录数量
     */
    public static int getAlarmRecordCount(Context context) {
        return getAlarmRecords(context).size();
    }
    
    /**
     * 检查是否已存在相同的未处理告警（避免重复生成）
     */
    public static boolean hasUnhandledAlarm(Context context, String greenhouseId, String device, String content) {
        JSONArray allAlarms = getAlarmRecords(context);
        long currentTime = System.currentTimeMillis();
        long timeThreshold = 5 * 60 * 1000; // 5分钟内
        
        for (int i = 0; i < allAlarms.size(); i++) {
            JSONObject alarm = allAlarms.getJSONObject(i);
            if (!alarm.getBooleanValue("isHandled") &&
                alarm.getString("greenhouse").equals(greenhouseId) &&
                alarm.getString("device").equals(device) &&
                alarm.getString("content").equals(content)) {
                long alarmTime = alarm.getLongValue("timestamp");
                if (currentTime - alarmTime < timeThreshold) {
                    return true;
                }
            }
        }
        return false;
    }
}


