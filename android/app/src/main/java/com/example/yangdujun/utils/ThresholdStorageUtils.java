package com.example.yangdujun.utils;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 告警阈值存储工具类
 * 用于管理和持久化存储告警阈值设置
 */
public class ThresholdStorageUtils {
    private static final String TAG = "ThresholdStorageUtils";
    private static final String THRESHOLD_FILE_NAME = "alarm_thresholds.json";
    
    // 默认阈值设置
    private static final int DEFAULT_TEMP_MIN = 15;
    private static final int DEFAULT_TEMP_MAX = 30;
    private static final int DEFAULT_HUM_MIN = 40;
    private static final int DEFAULT_HUM_MAX = 80;
    private static final int DEFAULT_CO2_MIN = 300;
    private static final int DEFAULT_CO2_MAX = 1500;
    private static final int DEFAULT_LIGHT_MIN = 1000;
    private static final int DEFAULT_LIGHT_MAX = 10000;
    private static final int DEFAULT_SOIL_TEMP_MIN = 15;
    private static final int DEFAULT_SOIL_TEMP_MAX = 30;
    private static final int DEFAULT_SOIL_HUM_MIN = 40;
    private static final int DEFAULT_SOIL_HUM_MAX = 80;
    private static final int DEFAULT_PH_MIN = 5;
    private static final int DEFAULT_PH_MAX = 7;
    private static final int DEFAULT_O2_MIN = 18;
    private static final int DEFAULT_O2_MAX = 25;
    
    /**
     * 获取阈值文件路径
     */
    private static String getThresholdFilePath(Context context) {
        File dataDir = context.getFilesDir();
        return dataDir.getAbsolutePath() + File.separator + THRESHOLD_FILE_NAME;
    }
    
    /**
     * 保存告警阈值
     */
    public static void saveThresholds(Context context, JSONObject thresholds) {
        try {
            String filePath = getThresholdFilePath(context);
            FileOutputStream fos = new FileOutputStream(filePath);
            String jsonString = JSON.toJSONString(thresholds, true);
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.close();
            Log.d(TAG, "阈值保存成功");
        } catch (IOException e) {
            Log.e(TAG, "阈值保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 读取告警阈值
     */
    public static JSONObject getThresholds(Context context) {
        try {
            String filePath = getThresholdFilePath(context);
            File file = new File(filePath);
            if (!file.exists()) {
                Log.d(TAG, "阈值文件不存在，返回默认值");
                return getDefaultThresholds();
            }
            
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            fis.close();
            
            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            return JSON.parseObject(jsonString);
        } catch (IOException e) {
            Log.e(TAG, "阈值读取失败: " + e.getMessage());
            e.printStackTrace();
            return getDefaultThresholds();
        }
    }
    
    /**
     * 获取默认阈值
     */
    private static JSONObject getDefaultThresholds() {
        JSONObject thresholds = new JSONObject();
        
        // 空气温湿度
        thresholds.put("tempMin", DEFAULT_TEMP_MIN);
        thresholds.put("tempMax", DEFAULT_TEMP_MAX);
        thresholds.put("airHumMin", DEFAULT_HUM_MIN);
        thresholds.put("airHumMax", DEFAULT_HUM_MAX);
        
        // 土壤温湿度
        thresholds.put("soilTempMin", DEFAULT_SOIL_TEMP_MIN);
        thresholds.put("soilTempMax", DEFAULT_SOIL_TEMP_MAX);
        thresholds.put("humMin", DEFAULT_SOIL_HUM_MIN);
        thresholds.put("humMax", DEFAULT_SOIL_HUM_MAX);
        
        // 二氧化碳和氧气
        thresholds.put("co2Min", DEFAULT_CO2_MIN);
        thresholds.put("co2Max", DEFAULT_CO2_MAX);
        thresholds.put("o2Min", DEFAULT_O2_MIN);
        thresholds.put("o2Max", DEFAULT_O2_MAX);
        
        // 光照强度
        thresholds.put("lightMin", DEFAULT_LIGHT_MIN);
        thresholds.put("lightMax", DEFAULT_LIGHT_MAX);
        
        // pH值
        thresholds.put("phMin", DEFAULT_PH_MIN);
        thresholds.put("phMax", DEFAULT_PH_MAX);
        
        return thresholds;
    }
    
    /**
     * 保存单个阈值
     */
    public static void saveSingleThreshold(Context context, String key, int value) {
        JSONObject thresholds = getThresholds(context);
        thresholds.put(key, value);
        saveThresholds(context, thresholds);
    }
    
    /**
     * 获取单个阈值
     */
    public static int getSingleThreshold(Context context, String key) {
        JSONObject thresholds = getThresholds(context);
        return thresholds.getIntValue(key);
    }
    
    /**
     * 重置为默认阈值
     */
    public static void resetToDefault(Context context) {
        saveThresholds(context, getDefaultThresholds());
        Log.d(TAG, "阈值已重置为默认值");
    }
    
    /**
     * 保存大棚的告警阈值
     */
    public static void saveThresholdsForGreenhouse(Context context, String greenhouseId, JSONObject thresholds) {
        try {
            String filePath = getThresholdFilePath(context);
            File file = new File(filePath);
            
            JSONObject allThresholds;
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[(int) file.length()];
                fis.read(buffer);
                fis.close();
                String jsonString = new String(buffer, StandardCharsets.UTF_8);
                allThresholds = JSON.parseObject(jsonString);
            } else {
                allThresholds = new JSONObject();
            }
            
            // 保存该大棚的阈值
            allThresholds.put(greenhouseId, thresholds);
            
            // 写入文件
            FileOutputStream fos = new FileOutputStream(filePath);
            String jsonString = JSON.toJSONString(allThresholds, true);
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.close();
            Log.d(TAG, "大棚 " + greenhouseId + " 的阈值保存成功");
        } catch (IOException e) {
            Log.e(TAG, "大棚阈值保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取大棚的告警阈值
     */
    public static JSONObject getThresholdsForGreenhouse(Context context, String greenhouseId) {
        try {
            String filePath = getThresholdFilePath(context);
            File file = new File(filePath);
            if (!file.exists()) {
                Log.d(TAG, "阈值文件不存在，返回默认值");
                return getDefaultThresholds();
            }
            
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            fis.close();
            
            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONObject allThresholds = JSON.parseObject(jsonString);
            
            // 获取该大棚的阈值
            if (allThresholds.containsKey(greenhouseId)) {
                return allThresholds.getJSONObject(greenhouseId);
            } else {
                Log.d(TAG, "大棚 " + greenhouseId + " 的阈值不存在，返回默认值");
                return getDefaultThresholds();
            }
        } catch (IOException e) {
            Log.e(TAG, "大棚阈值读取失败: " + e.getMessage());
            e.printStackTrace();
            return getDefaultThresholds();
        }
    }
    
    /**
     * 检查数据是否超出阈值
     */
    public static String checkThresholds(Context context, String greenhouseId, JSONObject environmentData) {
        JSONObject thresholds = getThresholdsForGreenhouse(context, greenhouseId);
        StringBuilder alarmMessages = new StringBuilder();
        
        // 检查空气温度
        float temp = environmentData.getFloatValue("temperature");
        int tempMin = thresholds.getIntValue("tempMin");
        int tempMax = thresholds.getIntValue("tempMax");
        if (temp < tempMin) {
            alarmMessages.append("空气温度过低：").append(temp).append("℃（阈值：").append(tempMin).append("℃）\n");
        } else if (temp > tempMax) {
            alarmMessages.append("空气温度过高：").append(temp).append("℃（阈值：").append(tempMax).append("℃）\n");
        }
        
        // 检查空气湿度
        float airHum = environmentData.getFloatValue("airHumidity");
        int airHumMin = thresholds.getIntValue("airHumMin");
        int airHumMax = thresholds.getIntValue("airHumMax");
        if (airHum < airHumMin) {
            alarmMessages.append("空气湿度过低：").append(airHum).append("%（阈值：").append(airHumMin).append("%）\n");
        } else if (airHum > airHumMax) {
            alarmMessages.append("空气湿度过高：").append(airHum).append("%（阈值：").append(airHumMax).append("%）\n");
        }
        
        // 检查土壤湿度
        float soilHum = environmentData.getFloatValue("soilHumidity");
        int soilHumMin = thresholds.getIntValue("humMin");
        int soilHumMax = thresholds.getIntValue("humMax");
        if (soilHum < soilHumMin) {
            alarmMessages.append("土壤湿度过低：").append(soilHum).append("%（阈值：").append(soilHumMin).append("%）\n");
        } else if (soilHum > soilHumMax) {
            alarmMessages.append("土壤湿度过高：").append(soilHum).append("%（阈值：").append(soilHumMax).append("%）\n");
        }
        
        // 检查土壤温度
        float soilTemp = environmentData.getFloatValue("soilTemperature");
        int soilTempMin = thresholds.getIntValue("soilTempMin");
        int soilTempMax = thresholds.getIntValue("soilTempMax");
        if (soilTemp < soilTempMin) {
            alarmMessages.append("土壤温度过低：").append(soilTemp).append("℃（阈值：").append(soilTempMin).append("℃）\n");
        } else if (soilTemp > soilTempMax) {
            alarmMessages.append("土壤温度过高：").append(soilTemp).append("℃（阈值：").append(soilTempMax).append("℃）\n");
        }
        
        // 检查氧气
        float o2 = environmentData.getFloatValue("o2");
        int o2Min = thresholds.getIntValue("o2Min");
        int o2Max = thresholds.getIntValue("o2Max");
        if (o2 < o2Min) {
            alarmMessages.append("氧气浓度过低：").append(o2).append("%（阈值：").append(o2Min).append("%）\n");
        } else if (o2 > o2Max) {
            alarmMessages.append("氧气浓度过高：").append(o2).append("%（阈值：").append(o2Max).append("%）\n");
        }
        
        // 检查pH值
        float ph = environmentData.getFloatValue("ph");
        int phMin = thresholds.getIntValue("phMin");
        int phMax = thresholds.getIntValue("phMax");
        if (ph < phMin) {
            alarmMessages.append("pH值过低：").append(ph).append("（阈值：").append(phMin).append("）\n");
        } else if (ph > phMax) {
            alarmMessages.append("pH值过高：").append(ph).append("（阈值：").append(phMax).append("）\n");
        }
        
        return alarmMessages.length() > 0 ? alarmMessages.toString() : null;
    }
}


