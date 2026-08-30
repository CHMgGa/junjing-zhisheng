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
 * 历史数据存储工具类
 * 用于保存大棚实时数据到本地，按日期记录，自动清理超过14天的数据
 */
public class HistoricalDataStorageUtils {
    private static final String TAG = "HistoricalDataStorageUtils";
    private static final String HISTORY_FILE_NAME = "historical_data.json";
    private static final int MAX_DAYS = 14; // 最多保留14天的数据

    /**
     * 获取历史数据文件路径
     */
    private static String getHistoryFilePath(Context context) {
        File dataDir = context.getFilesDir();
        return dataDir.getAbsolutePath() + File.separator + HISTORY_FILE_NAME;
    }

    /**
     * 保存实时数据到历史记录
     * @param context 上下文
     * @param greenhouseId 大棚ID
     * @param environmentData 环境数据
     */
    public static void saveRealtimeData(Context context, String greenhouseId, JSONObject environmentData) {
        android.util.Log.d(TAG, "开始异步保存历史数据: 大棚ID=" + greenhouseId);
        AsyncTaskUtils.executeAsync(() -> {
            try {
                android.util.Log.d(TAG, "异步任务开始执行: 大棚ID=" + greenhouseId);
                JSONObject historyData = getHistoryDataSync(context);
                String currentDate = getCurrentDate();

                // 获取该大棚的历史数据
                JSONObject greenhouseData;
                if (historyData.containsKey(greenhouseId)) {
                    greenhouseData = historyData.getJSONObject(greenhouseId);
                    if (greenhouseData == null) {
                        greenhouseData = new JSONObject();
                        historyData.put(greenhouseId, greenhouseData);
                    }
                } else {
                    greenhouseData = new JSONObject();
                    historyData.put(greenhouseId, greenhouseData);
                }

                // 获取当天的数据
                JSONObject dayData;
                if (greenhouseData.containsKey(currentDate)) {
                    dayData = greenhouseData.getJSONObject(currentDate);
                    if (dayData == null) {
                        dayData = new JSONObject();
                        greenhouseData.put(currentDate, dayData);
                    }
                } else {
                    dayData = new JSONObject();
                    greenhouseData.put(currentDate, dayData);
                }

                // 添加时间戳和数据
                String currentTime = getCurrentTime();
                dayData.put(currentTime, environmentData);

                // 清理超过14天的数据
                cleanupOldData(greenhouseData);

                // 保存到文件
                saveHistoryDataSync(context, historyData);

                android.util.Log.d(TAG, "实时数据保存成功: 大棚ID=" + greenhouseId + ", 日期=" + currentDate);
            } catch (Exception e) {
                android.util.Log.e(TAG, "实时数据保存失败: " + e.getMessage(), e);
            }
        }, null);
    }

    /**
     * 获取指定大棚的历史数据（同步）
     */
    private static JSONObject getHistoryDataSync(Context context) {
        try {
            String filePath = getHistoryFilePath(context);
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
            Log.e(TAG, "历史数据读取失败: " + e.getMessage(), e);
            return new JSONObject();
        }
    }

    /**
     * 保存历史数据到文件（同步）
     */
    private static void saveHistoryDataSync(Context context, JSONObject historyData) {
        try {
            String filePath = getHistoryFilePath(context);
            FileOutputStream fos = new FileOutputStream(filePath);
            String jsonString = JSON.toJSONString(historyData, true);
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "历史数据保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理超过14天的数据
     */
    private static void cleanupOldData(JSONObject greenhouseData) {
        try {
            List<String> datesToRemove = new ArrayList<>();
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

            for (String dateKey : greenhouseData.keySet()) {
                Date date = sdf.parse(dateKey);
                if (date != null) {
                    calendar.setTime(date);
                    calendar.add(Calendar.DAY_OF_MONTH, MAX_DAYS);
                    if (calendar.getTime().before(new Date())) {
                        datesToRemove.add(dateKey);
                    }
                }
            }

            // 删除过期数据
            for (String dateKey : datesToRemove) {
                greenhouseData.remove(dateKey);
                Log.d(TAG, "删除过期数据: " + dateKey);
            }
        } catch (Exception e) {
            Log.e(TAG, "清理过期数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取指定大棚最近N天的数据（按天平均）
     * @param context 上下文
     * @param greenhouseId 大棚ID
     * @param days 天数（7或14）
     * @return 按天平均的数据列表
     */
    public static JSONArray getDailyAverageData(Context context, String greenhouseId, int days) {
        JSONObject historyData = getHistoryDataSync(context);
        JSONArray result = new JSONArray();

        if (!historyData.containsKey(greenhouseId)) {
            return result;
        }

        JSONObject greenhouseData = historyData.getJSONObject(greenhouseId);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        // 从最近一天开始，往前获取days天的数据
        for (int i = days - 1; i >= 0; i--) {
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -i);
            String dateKey = sdf.format(calendar.getTime());

            if (greenhouseData.containsKey(dateKey)) {
                JSONObject dayData = greenhouseData.getJSONObject(dateKey);
                JSONObject avgData = calculateDailyAverage(dayData);
                avgData.put("date", dateKey);
                avgData.put("day", days - i); // 用于图表显示的X轴
                result.add(avgData);
            }
        }

        return result;
    }

    /**
     * 计算一天内所有数据的平均值
     */
    private static JSONObject calculateDailyAverage(JSONObject dayData) {
        JSONObject avgData = new JSONObject();
        int count = 0;
        double tempSum = 0, humSum = 0, lightSum = 0, windSum = 0;
        double soilHumSum = 0, soilTempSum = 0, co2Sum = 0, phSum = 0, o2Sum = 0;

        for (String timeKey : dayData.keySet()) {
            JSONObject data = dayData.getJSONObject(timeKey);
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
                count++;
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
     * 获取当前日期（yyyy-MM-dd）
     */
    private static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 获取当前时间（HH:mm:ss）
     */
    private static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 获取所有大棚ID列表
     */
    public static List<String> getGreenhouseIdList(Context context) {
        JSONObject historyData = getHistoryDataSync(context);
        List<String> greenhouseIds = new ArrayList<>();
        for (String key : historyData.keySet()) {
            greenhouseIds.add(key);
        }
        return greenhouseIds;
    }
}

