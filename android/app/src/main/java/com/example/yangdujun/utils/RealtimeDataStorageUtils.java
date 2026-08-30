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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 24小时实时数据存储工具类
 * 用于保存大棚实时数据到本地，按时间记录，自动清理超过24小时的数据
 */
public class RealtimeDataStorageUtils {
    private static final String TAG = "RealtimeDataStorageUtils";
    private static final String REALTIME_FILE_NAME = "realtime_data.json";
    private static final long MAX_HOURS = 24; // 最多保留24小时的数据

    /**
     * 获取实时数据文件路径
     */
    private static String getRealtimeFilePath(Context context) {
        File dataDir = context.getFilesDir();
        return dataDir.getAbsolutePath() + File.separator + REALTIME_FILE_NAME;
    }

    /**
     * 保存实时数据到24小时记录
     * @param context 上下文
     * @param greenhouseId 大棚ID
     * @param environmentData 环境数据
     */
    public static void saveRealtimeData(Context context, String greenhouseId, JSONObject environmentData) {
        AsyncTaskUtils.executeAsync(() -> {
            try {
                JSONObject realtimeData = getRealtimeDataSync(context);
                String currentTime = getCurrentDateTime();

                android.util.Log.d(TAG, "开始保存实时数据: 大棚ID=" + greenhouseId + ", 时间=" + currentTime);
                android.util.Log.d(TAG, "环境数据: " + environmentData.toJSONString());

                // 获取该大棚的实时数据
                JSONArray greenhouseData;
                if (realtimeData.containsKey(greenhouseId)) {
                    greenhouseData = realtimeData.getJSONArray(greenhouseId);
                    android.util.Log.d(TAG, "大棚已存在数据，当前数据条数: " + greenhouseData.size());
                } else {
                    greenhouseData = new JSONArray();
                    realtimeData.put(greenhouseId, greenhouseData);
                    android.util.Log.d(TAG, "大棚首次保存数据");
                }

                // 添加时间戳和数据
                JSONObject dataWithTime = new JSONObject();
                dataWithTime.put("timestamp", System.currentTimeMillis());
                dataWithTime.put("datetime", currentTime);
                dataWithTime.putAll(environmentData);
                
                // 添加到数组开头（最新的在前）
                greenhouseData.add(0, dataWithTime);

                // 清理超过24小时的数据
                cleanupOldData(greenhouseData);

                // 保存到文件
                saveRealtimeDataSync(context, realtimeData);

                android.util.Log.d(TAG, "实时数据保存成功: 大棚ID=" + greenhouseId + ", 时间=" + currentTime + ", 总数据条数=" + greenhouseData.size());
            } catch (Exception e) {
                android.util.Log.e(TAG, "实时数据保存失败: " + e.getMessage(), e);
            }
        }, null);
    }

    /**
     * 获取实时数据（同步）
     */
    private static JSONObject getRealtimeDataSync(Context context) {
        try {
            String filePath = getRealtimeFilePath(context);
            File file = new File(filePath);
            if (!file.exists()) {
                return new JSONObject();
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            fis.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONObject result = JSON.parseObject(jsonString);
            if (result == null) {
                return new JSONObject();
            }
            return result;
        } catch (IOException e) {
            Log.e(TAG, "实时数据读取失败: " + e.getMessage(), e);
            return new JSONObject();
        }
    }

    /**
     * 保存实时数据到文件（同步）
     */
    private static void saveRealtimeDataSync(Context context, JSONObject realtimeData) {
        try {
            String filePath = getRealtimeFilePath(context);
            FileOutputStream fos = new FileOutputStream(filePath);
            String jsonString = JSON.toJSONString(realtimeData, true);
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "实时数据保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理超过24小时的数据
     */
    private static void cleanupOldData(JSONArray greenhouseData) {
        try {
            long currentTime = System.currentTimeMillis();
            long maxTime = currentTime - (MAX_HOURS * 60 * 60 * 1000); // 24小时前的时间戳

            List<Integer> indicesToRemove = new ArrayList<>();
            for (int i = 0; i < greenhouseData.size(); i++) {
                JSONObject data = greenhouseData.getJSONObject(i);
                long timestamp = data.getLongValue("timestamp");
                if (timestamp < maxTime) {
                    indicesToRemove.add(i);
                }
            }

            // 从后往前删除，避免索引变化
            for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
                greenhouseData.remove(indicesToRemove.get(i).intValue());
                Log.d(TAG, "删除过期数据: " + indicesToRemove.get(i));
            }
        } catch (Exception e) {
            Log.e(TAG, "清理过期数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取指定大棚最近24小时的数据
     * @param context 上下文
     * @param greenhouseId 大棚ID
     * @return 最近24小时的数据列表
     */
    public static JSONArray getRealtimeData(Context context, String greenhouseId) {
        JSONObject realtimeData = getRealtimeDataSync(context);
        
        if (realtimeData == null || !realtimeData.containsKey(greenhouseId)) {
            return new JSONArray();
        }

        return realtimeData.getJSONArray(greenhouseId);
    }

    /**
     * 获取指定大棚最近24小时的数据（按小时平均）
     * @param context 上下文
     * @param greenhouseId 大棚ID
     * @return 按小时平均的数据列表
     */
    public static JSONArray getHourlyAverageData(Context context, String greenhouseId) {
        JSONArray allData = getRealtimeData(context, greenhouseId);
        JSONArray result = new JSONArray();

        android.util.Log.d(TAG, "获取实时数据: 大棚ID=" + greenhouseId + ", 原始数据条数=" + allData.size());

        if (allData.isEmpty()) {
            android.util.Log.d(TAG, "没有实时数据，返回空数组");
            return result;
        }

        // 按小时分组
        SimpleDateFormat hourFormat = new SimpleDateFormat("yyyy-MM-dd HH", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        
        // 从最近一小时开始，往前获取24小时的数据
        for (int i = 0; i < 24; i++) {
            calendar.setTime(new Date());
            calendar.add(Calendar.HOUR_OF_DAY, -i);
            String hourKey = hourFormat.format(calendar.getTime());
            
            List<JSONObject> hourDataList = new ArrayList<>();
            for (int j = 0; j < allData.size(); j++) {
                JSONObject data = allData.getJSONObject(j);
                String datetime = data.getString("datetime");
                if (datetime != null && datetime.startsWith(hourKey)) {
                    hourDataList.add(data);
                }
            }
            
            // 计算小时平均值
            if (!hourDataList.isEmpty()) {
                JSONObject avgData = calculateHourlyAverage(hourDataList);
                avgData.put("datetime", hourKey + ":00");
                avgData.put("hour", 24 - i); // 用于图表显示的X轴
                result.add(avgData);
                android.util.Log.d(TAG, "小时 " + hourKey + " 的数据条数: " + hourDataList.size());
            }
        }

        // 反转数组，使时间从早到晚
        JSONArray reversedResult = new JSONArray();
        for (int i = result.size() - 1; i >= 0; i--) {
            reversedResult.add(result.get(i));
        }

        android.util.Log.d(TAG, "返回的按小时平均数据条数: " + reversedResult.size());
        return reversedResult;
    }

    /**
     * 计算一小时内的数据平均值
     */
    private static JSONObject calculateHourlyAverage(List<JSONObject> hourDataList) {
        JSONObject avgData = new JSONObject();
        int count = hourDataList.size();
        double tempSum = 0, humSum = 0, lightSum = 0, windSum = 0;
        double soilHumSum = 0, soilTempSum = 0, co2Sum = 0, phSum = 0, o2Sum = 0;

        for (JSONObject data : hourDataList) {
            try {
                tempSum += safeParseDouble(data.getString("temperature"));
                humSum += safeParseDouble(data.getString("humidity"));
                lightSum += safeParseDouble(data.getString("light"));
                windSum += safeParseDouble(data.getString("windSpeed"));
                soilHumSum += safeParseDouble(data.getString("soilHumidity"));
                soilTempSum += safeParseDouble(data.getString("soilTemperature"));
                co2Sum += safeParseDouble(data.getString("co2"));
                phSum += safeParseDouble(data.getString("ph"));
                o2Sum += safeParseDouble(data.getString("o2"));
            } catch (Exception e) {
                // 跳过无效数据
            }
        }

        if (count > 0) {
            avgData.put("temperature", tempSum / count);
            avgData.put("humidity", humSum / count);
            avgData.put("light", lightSum / count);
            avgData.put("windSpeed", windSum / count);
            avgData.put("soilHumidity", soilHumSum / count);
            avgData.put("soilTemperature", soilTempSum / count);
            avgData.put("co2", co2Sum / count);
            avgData.put("ph", phSum / count);
            avgData.put("o2", o2Sum / count);
        }

        return avgData;
    }

    /**
     * 安全解析double值
     */
    private static double safeParseDouble(String value) {
        try {
            if (value == null || value.isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 获取当前日期时间（yyyy-MM-dd HH:mm:ss）
     */
    private static String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 获取所有大棚ID列表
     */
    public static List<String> getGreenhouseIdList(Context context) {
        JSONObject realtimeData = getRealtimeDataSync(context);
        List<String> greenhouseIds = new ArrayList<>();
        for (String key : realtimeData.keySet()) {
            greenhouseIds.add(key);
        }
        return greenhouseIds;
    }
}

