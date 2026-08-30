package com.example.yangdujun.utils;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.Random;

public class LocalEnvironmentManager {
    private static final String TAG = "LocalEnvironmentManager";
    private static final String ENVIRONMENT_DATA_FILE = "local_environment_data.json";
    private static final String ENVIRONMENT_HISTORY_FILE = "local_environment_history.json";
    private final StorageManager storageManager;
    private final Random random;

    public LocalEnvironmentManager(Context context) {
        this.storageManager = StorageManager.getInstance(context);
        this.random = new Random();
        initializeDataFiles();
    }

    // 初始化数据文件
    private void initializeDataFiles() {
        if (!storageManager.exists(ENVIRONMENT_DATA_FILE)) {
            JSONObject root = new JSONObject();
            storageManager.saveJsonObject(ENVIRONMENT_DATA_FILE, root);
            Log.d(TAG, "Initialized environment data file");
        } else {
            // 迁移旧数据：将 "default" ID 转换为 "1"
            migrateOldData();
        }

        if (!storageManager.exists(ENVIRONMENT_HISTORY_FILE)) {
            JSONObject root = new JSONObject();
            storageManager.saveJsonObject(ENVIRONMENT_HISTORY_FILE, root);
            Log.d(TAG, "Initialized environment history file");
        } else {
            // 迁移历史数据中的 "default" ID
            migrateHistoryData();
        }

        // 为默认大棚生成初始环境数据
        generateInitialEnvironmentData();
    }

    // 迁移旧数据：将 "default" ID 转换为 "1"
    private void migrateOldData() {
        try {
            JSONObject allData = storageManager.readJsonObject(ENVIRONMENT_DATA_FILE);
            if (allData != null && allData.containsKey("default")) {
                JSONObject defaultData = allData.getJSONObject("default");
                allData.remove("default");
                allData.put("1", defaultData);
                storageManager.saveJsonObject(ENVIRONMENT_DATA_FILE, allData);
                Log.d(TAG, "迁移环境数据：将 default 转换为 1");
            }
        } catch (Exception e) {
            Log.e(TAG, "迁移旧数据失败: " + e.getMessage());
        }
    }

    // 迁移历史数据中的 "default" ID
    private void migrateHistoryData() {
        try {
            JSONObject allHistory = storageManager.readJsonObject(ENVIRONMENT_HISTORY_FILE);
            if (allHistory != null && allHistory.containsKey("default")) {
                JSONArray history = allHistory.getJSONArray("default");
                allHistory.remove("default");
                allHistory.put("1", history);
                storageManager.saveJsonObject(ENVIRONMENT_HISTORY_FILE, allHistory);
                Log.d(TAG, "迁移历史数据：将 default 转换为 1");
            }
        } catch (Exception e) {
            Log.e(TAG, "迁移历史数据失败: " + e.getMessage());
        }
    }

    // 为默认大棚生成初始环境数据
    private void generateInitialEnvironmentData() {
        JSONObject data = generateEnvironmentData();
        saveEnvironmentData("1", data);
        Log.d(TAG, "Generated initial environment data for default greenhouse");
    }

    // 生成模拟环境数据
    public JSONObject generateEnvironmentData() {
        JSONObject data = new JSONObject();
        
        // 生成随机数据，模拟真实环境
        data.put("temperature", String.format("%.1f", 20 + random.nextDouble() * 10)); // 20-30°C
        data.put("humidity", String.format("%.1f", 40 + random.nextDouble() * 40)); // 40-80%
        data.put("light", String.format("%.1f", 5000 + random.nextDouble() * 15000)); // 5000-20000 lux
        data.put("windSpeed", String.format("%.1f", 0 + random.nextDouble() * 10)); // 0-10 m/s
        data.put("lightStatus", random.nextBoolean() ? "ON" : "OFF");
        data.put("windBoardStatus", random.nextBoolean() ? "ON" : "OFF");
        data.put("soilHumidity", String.format("%.1f", 30 + random.nextDouble() * 50)); // 30-80%
        data.put("soilTemperature", String.format("%.1f", 18 + random.nextDouble() * 8)); // 18-26°C
        data.put("co2", String.format("%.1f", 300 + random.nextDouble() * 700)); // 300-1000 ppm
        data.put("ph", String.format("%.1f", 5.5 + random.nextDouble() * 2)); // 5.5-7.5
        data.put("o2", String.format("%.1f", 18 + random.nextDouble() * 4)); // 18-22%
        data.put("timestamp", System.currentTimeMillis());

        return data;
    }

    // 保存环境数据
    public void saveEnvironmentData(String greenhouseId, JSONObject data) {
        try {
            JSONObject allData = storageManager.readJsonObject(ENVIRONMENT_DATA_FILE);
            if (allData == null) {
                allData = new JSONObject();
            }
            allData.put(greenhouseId, data);
            storageManager.saveJsonObject(ENVIRONMENT_DATA_FILE, allData);
            Log.d(TAG, "Saved environment data for greenhouse: " + greenhouseId);

            // 同时保存到历史记录
            saveToHistory(greenhouseId, data);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save environment data", e);
        }
    }

    // 获取环境数据
    public JSONObject getEnvironmentData(String greenhouseId) {
        try {
            JSONObject allData = storageManager.readJsonObject(ENVIRONMENT_DATA_FILE);
            if (allData != null && allData.containsKey(greenhouseId)) {
                JSONObject data = allData.getJSONObject(greenhouseId);
                Log.d(TAG, "Retrieved environment data for greenhouse: " + greenhouseId);
                return data;
            } else {
                // 如果没有数据，生成新数据
                JSONObject newData = generateEnvironmentData();
                saveEnvironmentData(greenhouseId, newData);
                return newData;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get environment data", e);
            // 发生错误时生成新数据
            return generateEnvironmentData();
        }
    }

    // 获取历史环境数据
    public JSONArray getEnvironmentHistory(String greenhouseId, long startTime, long endTime) {
        try {
            JSONObject allHistory = storageManager.readJsonObject(ENVIRONMENT_HISTORY_FILE);
            if (allHistory != null && allHistory.containsKey(greenhouseId)) {
                JSONArray history = allHistory.getJSONArray(greenhouseId);
                JSONArray filteredHistory = new JSONArray();

                // 过滤时间范围内的数据
                for (int i = 0; i < history.size(); i++) {
                    JSONObject data = history.getJSONObject(i);
                    long timestamp = data.getLong("timestamp");
                    if (timestamp >= startTime && timestamp <= endTime) {
                        filteredHistory.add(data);
                    }
                }

                Log.d(TAG, "Retrieved environment history for greenhouse " + greenhouseId + ", count: " + filteredHistory.size());
                return filteredHistory;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get environment history", e);
        }
        return new JSONArray();
    }

    // 保存到历史记录
    private void saveToHistory(String greenhouseId, JSONObject data) {
        try {
            JSONObject allHistory = storageManager.readJsonObject(ENVIRONMENT_HISTORY_FILE);
            if (allHistory == null) {
                allHistory = new JSONObject();
            }

            JSONArray history;
            if (allHistory.containsKey(greenhouseId)) {
                history = allHistory.getJSONArray(greenhouseId);
            } else {
                history = new JSONArray();
            }

            // 添加新数据
            history.add(data);

            // 限制历史记录数量，保留最近1000条
            if (history.size() > 1000) {
                JSONArray newHistory = new JSONArray();
                for (int i = history.size() - 1000; i < history.size(); i++) {
                    newHistory.add(history.get(i));
                }
                history = newHistory;
            }

            allHistory.put(greenhouseId, history);
            storageManager.saveJsonObject(ENVIRONMENT_HISTORY_FILE, allHistory);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save to history", e);
        }
    }

    // 获取统计数据
    public JSONObject getEnvironmentStats(String greenhouseId, long startTime, long endTime) {
        JSONArray history = getEnvironmentHistory(greenhouseId, startTime, endTime);
        if (history.isEmpty()) {
            return generateEmptyStats();
        }

        JSONObject stats = new JSONObject();
        double tempSum = 0, humiditySum = 0, lightSum = 0;
        double minTemp = Double.MAX_VALUE, maxTemp = Double.MIN_VALUE;
        double minHumidity = Double.MAX_VALUE, maxHumidity = Double.MIN_VALUE;

        for (int i = 0; i < history.size(); i++) {
            JSONObject data = history.getJSONObject(i);
            
            double temp = Double.parseDouble(data.getString("temperature"));
            double humidity = Double.parseDouble(data.getString("humidity"));
            double light = Double.parseDouble(data.getString("light"));

            tempSum += temp;
            humiditySum += humidity;
            lightSum += light;

            minTemp = Math.min(minTemp, temp);
            maxTemp = Math.max(maxTemp, temp);
            minHumidity = Math.min(minHumidity, humidity);
            maxHumidity = Math.max(maxHumidity, humidity);
        }

        int count = history.size();
        stats.put("averageTemperature", String.format("%.1f", tempSum / count));
        stats.put("averageHumidity", String.format("%.1f", humiditySum / count));
        stats.put("averageLight", String.format("%.1f", lightSum / count));
        stats.put("minTemperature", String.format("%.1f", minTemp));
        stats.put("maxTemperature", String.format("%.1f", maxTemp));
        stats.put("minHumidity", String.format("%.1f", minHumidity));
        stats.put("maxHumidity", String.format("%.1f", maxHumidity));
        stats.put("dataPoints", count);

        Log.d(TAG, "Calculated environment stats for greenhouse: " + greenhouseId);
        return stats;
    }

    // 生成空统计数据
    private JSONObject generateEmptyStats() {
        JSONObject stats = new JSONObject();
        stats.put("averageTemperature", "0.0");
        stats.put("averageHumidity", "0.0");
        stats.put("averageLight", "0.0");
        stats.put("minTemperature", "0.0");
        stats.put("maxTemperature", "0.0");
        stats.put("minHumidity", "0.0");
        stats.put("maxHumidity", "0.0");
        stats.put("dataPoints", 0);
        return stats;
    }

    // 清空环境数据
    public void clearEnvironmentData(String greenhouseId) {
        try {
            JSONObject allData = storageManager.readJsonObject(ENVIRONMENT_DATA_FILE);
            if (allData != null) {
                allData.remove(greenhouseId);
                storageManager.saveJsonObject(ENVIRONMENT_DATA_FILE, allData);
            }

            JSONObject allHistory = storageManager.readJsonObject(ENVIRONMENT_HISTORY_FILE);
            if (allHistory != null) {
                allHistory.remove(greenhouseId);
                storageManager.saveJsonObject(ENVIRONMENT_HISTORY_FILE, allHistory);
            }

            Log.d(TAG, "Cleared environment data for greenhouse: " + greenhouseId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear environment data", e);
        }
    }


}


