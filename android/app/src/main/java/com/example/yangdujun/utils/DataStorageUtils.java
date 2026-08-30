package com.example.yangdujun.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataStorageUtils {

    private static final String TAG = "DataStorageUtils";
    private static final String DATA_FILE_NAME = "greenhouse_data.json";
    private static final String DATA_ARRAY_KEY = "data";
    private static final int MAX_DATA_ENTRIES = 1000; // 最多存储1000条数据

    // 获取数据存储文件路径
    private static File getDataFile(Context context) {
        // 使用应用的内部存储
        File filesDir = context.getFilesDir();
        return new File(filesDir, DATA_FILE_NAME);
    }

    // 存储大棚数据到JSON文件
    public static synchronized void saveGreenhouseData(Context context, 
                                                     String airTemp, 
                                                     String airHumi, 
                                                     String light, 
                                                     int fanGear, 
                                                     String lightState, 
                                                     String boardState, 
                                                     String soilTemp, 
                                                     String soilHumi, 
                                                     String co2, 
                                                     String o2, 
                                                     String ph) {
        try {
            File dataFile = getDataFile(context);
            JSONArray dataArray;

            // 如果文件存在，读取现有数据
            if (dataFile.exists()) {
                try (FileReader reader = new FileReader(dataFile)) {
                    char[] buffer = new char[(int) dataFile.length()];
                    reader.read(buffer);
                    String jsonContent = new String(buffer);
                    JSONObject jsonObject = JSON.parseObject(jsonContent);
                    dataArray = jsonObject.getJSONArray(DATA_ARRAY_KEY);
                    // 检查dataArray是否为null
                    if (dataArray == null) {
                        dataArray = new JSONArray();
                    }
                } catch (Exception e) {
                    // 如果读取失败，创建新的数组
                    dataArray = new JSONArray();
                }
            } else {
                // 文件不存在，创建新的数组
                dataArray = new JSONArray();
            }

            // 创建新的数据对象
            JSONObject dataObject = new JSONObject();
            dataObject.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            dataObject.put("airTemp", airTemp);
            dataObject.put("airHumi", airHumi);
            dataObject.put("light", light);
            dataObject.put("fanGear", fanGear);
            dataObject.put("lightState", lightState);
            dataObject.put("boardState", boardState);
            dataObject.put("soilTemp", soilTemp);
            dataObject.put("soilHumi", soilHumi);
            dataObject.put("co2", co2);
            dataObject.put("o2", o2);
            dataObject.put("ph", ph);

            // 添加新数据到数组开头
            dataArray.add(0, dataObject);

            // 限制数据量，只保留最新的MAX_DATA_ENTRIES条记录
        if (dataArray.size() > MAX_DATA_ENTRIES) {
            JSONArray newArray = new JSONArray();
            for (int i = 0; i < MAX_DATA_ENTRIES && i < dataArray.size(); i++) {
                newArray.add(dataArray.get(i));
            }
            dataArray = newArray;
        }

            // 创建最终的JSON对象
            JSONObject finalObject = new JSONObject();
            finalObject.put(DATA_ARRAY_KEY, dataArray);

            // 写入文件
            try (FileWriter writer = new FileWriter(dataFile)) {
                writer.write(finalObject.toJSONString());
                writer.flush();
            }

            Log.d(TAG, "大棚数据存储成功");

        } catch (IOException e) {
            Log.e(TAG, "存储大棚数据失败: " + e.getMessage());
        }
    }

    // 从JSON文件读取大棚数据
    public static JSONArray getGreenhouseData(Context context) {
        try {
            File dataFile = getDataFile(context);
            if (dataFile.exists()) {
                try (FileReader reader = new FileReader(dataFile)) {
                    char[] buffer = new char[(int) dataFile.length()];
                    reader.read(buffer);
                    String jsonContent = new String(buffer);
                    JSONObject jsonObject = JSON.parseObject(jsonContent);
                    return jsonObject.getJSONArray(DATA_ARRAY_KEY);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "读取大棚数据失败: " + e.getMessage());
        }
        return new JSONArray();
    }

    // 清空数据文件
    public static void clearData(Context context) {
        try {
            File dataFile = getDataFile(context);
            if (dataFile.exists()) {
                dataFile.delete();
            }
            Log.d(TAG, "数据文件已清空");
        } catch (Exception e) {
            Log.e(TAG, "清空数据文件失败: " + e.getMessage());
        }
    }

    // 获取数据文件大小
    public static long getDataFileSize(Context context) {
        File dataFile = getDataFile(context);
        if (dataFile.exists()) {
            return dataFile.length();
        }
        return 0;
    }

    // 获取数据条目数
    public static int getDataCount(Context context) {
        JSONArray dataArray = getGreenhouseData(context);
        return dataArray != null ? dataArray.size() : 0;
    }
}


