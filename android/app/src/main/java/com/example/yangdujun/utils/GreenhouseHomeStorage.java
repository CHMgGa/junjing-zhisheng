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

public class GreenhouseHomeStorage {
    private static final String TAG = "GreenhouseHomeStorage";
    private static final String STORAGE_FILE_NAME = "greenhouse_home_data.json";
    
    private Context context;
    private JSONObject greenhouseListData;
    private JSONObject environmentData;
    private JSONObject lastSelectedData;
    private JSONObject hourlyHistoryData;
    private JSONObject dailyHistoryData;

    public GreenhouseHomeStorage() {
        this(null);
    }

    public GreenhouseHomeStorage(Context context) {
        this.context = context;
        
        if (context != null) {
            loadDataFromFile();
        } else {
            initializeDefaultData();
        }
    }

    private void initializeDefaultData() {
        greenhouseListData = new JSONObject();
        environmentData = new JSONObject();
        lastSelectedData = new JSONObject();
        hourlyHistoryData = new JSONObject();
        dailyHistoryData = new JSONObject();
        
        List<String> defaultList = new ArrayList<>();
        defaultList.add("请选择大棚");
        defaultList.add("默认大棚");
        List<String> defaultIdList = new ArrayList<>();
        defaultIdList.add("");
        defaultIdList.add("1");
        greenhouseListData.put("greenhouseList", defaultList);
        greenhouseListData.put("greenhouseIdList", defaultIdList);
        lastSelectedData.put("lastSelectedIndex", 0);
    }

    private void loadDataFromFile() {
        try {
            String filePath = getStorageFilePath();
            File file = new File(filePath);
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[(int) file.length()];
                fis.read(buffer);
                fis.close();
                
                String jsonString = new String(buffer, StandardCharsets.UTF_8);
                JSONObject allData = JSON.parseObject(jsonString);
                
                if (allData != null) {
                    greenhouseListData = allData.getJSONObject("greenhouseListData");
                    environmentData = allData.getJSONObject("environmentData");
                    lastSelectedData = allData.getJSONObject("lastSelectedData");
                    hourlyHistoryData = allData.getJSONObject("hourlyHistoryData");
                    dailyHistoryData = allData.getJSONObject("dailyHistoryData");
                    
                    if (greenhouseListData == null) greenhouseListData = new JSONObject();
                    if (environmentData == null) environmentData = new JSONObject();
                    if (lastSelectedData == null) lastSelectedData = new JSONObject();
                    if (hourlyHistoryData == null) hourlyHistoryData = new JSONObject();
                    if (dailyHistoryData == null) dailyHistoryData = new JSONObject();
                    
                    // 确保greenhouseListData中有必要的键
                    ensureGreenhouseListData();
                    
                    // 迁移旧数据：将 "default" ID 转换为 "1"
                    migrateOldData();
                    
                    Log.d(TAG, "从文件加载数据成功");
                } else {
                    initializeDefaultData();
                }
            } else {
                initializeDefaultData();
            }
        } catch (Exception e) {
            Log.e(TAG, "从文件加载数据失败: " + e.getMessage());
            initializeDefaultData();
        }
    }
    
    // 确保greenhouseListData中有必要的键
    private void ensureGreenhouseListData() {
        try {
            JSONArray listArray = greenhouseListData.getJSONArray("greenhouseList");
            JSONArray idArray = greenhouseListData.getJSONArray("greenhouseIdList");
            
            // 检查键是否存在或数组为空
            if (!greenhouseListData.containsKey("greenhouseList") || listArray == null || listArray.size() < 2) {
                List<String> defaultList = new ArrayList<>();
                defaultList.add("请选择大棚");
                defaultList.add("默认大棚");
                greenhouseListData.put("greenhouseList", defaultList);
                Log.d(TAG, "初始化默认大棚列表");
            }
            
            if (!greenhouseListData.containsKey("greenhouseIdList") || idArray == null || idArray.size() < 2) {
                List<String> defaultIdList = new ArrayList<>();
                defaultIdList.add("");
                defaultIdList.add("1");
                greenhouseListData.put("greenhouseIdList", defaultIdList);
                Log.d(TAG, "初始化默认大棚ID列表");
            }
            
            // 保存确保后的数据
            saveDataToFile();
        } catch (Exception e) {
            Log.e(TAG, "确保大棚列表数据失败: " + e.getMessage());
            // 异常时强制初始化默认数据
            List<String> defaultList = new ArrayList<>();
            defaultList.add("请选择大棚");
            defaultList.add("默认大棚");
            greenhouseListData.put("greenhouseList", defaultList);
            
            List<String> defaultIdList = new ArrayList<>();
            defaultIdList.add("");
            defaultIdList.add("1");
            greenhouseListData.put("greenhouseIdList", defaultIdList);
            
            saveDataToFile();
        }
    }

    // 迁移旧数据：将 "default" ID 转换为 "1"
    private void migrateOldData() {
        try {
            JSONArray idArray = greenhouseListData.getJSONArray("greenhouseIdList");
            if (idArray != null) {
                for (int i = 0; i < idArray.size(); i++) {
                    String id = idArray.getString(i);
                    if ("default".equals(id)) {
                        idArray.set(i, "1");
                        Log.d(TAG, "迁移旧数据：将 default 转换为 1");
                    }
                }
            }
            
            // 迁移环境数据中的 "default" ID
            if (environmentData.containsKey("default")) {
                JSONObject defaultData = environmentData.getJSONObject("default");
                environmentData.remove("default");
                environmentData.put("1", defaultData);
                Log.d(TAG, "迁移环境数据：将 default 转换为 1");
            }
            
            // 迁移传感器数据中的 "default" ID
            if (environmentData.containsKey("default_sensor")) {
                JSONObject defaultSensorData = environmentData.getJSONObject("default_sensor");
                environmentData.remove("default_sensor");
                environmentData.put("1_sensor", defaultSensorData);
                Log.d(TAG, "迁移传感器数据：将 default 转换为 1");
            }
            
            // 保存迁移后的数据
            saveDataToFile();
        } catch (Exception e) {
            Log.e(TAG, "迁移旧数据失败: " + e.getMessage());
        }
    }

    private void saveDataToFile() {
        if (context == null) {
            return;
        }
        
        try {
            String filePath = getStorageFilePath();
            JSONObject allData = new JSONObject();
            allData.put("greenhouseListData", greenhouseListData);
            allData.put("environmentData", environmentData);
            allData.put("lastSelectedData", lastSelectedData);
            allData.put("hourlyHistoryData", hourlyHistoryData);
            allData.put("dailyHistoryData", dailyHistoryData);
            
            FileOutputStream fos = new FileOutputStream(filePath);
            String jsonString = JSON.toJSONString(allData, true);
            fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
            fos.close();
            
            Log.d(TAG, "数据保存到文件成功");
        } catch (IOException e) {
            Log.e(TAG, "数据保存到文件失败: " + e.getMessage());
        }
    }

    private String getStorageFilePath() {
        File dataDir = context.getFilesDir();
        return dataDir.getAbsolutePath() + File.separator + STORAGE_FILE_NAME;
    }

    // 保存大棚列表
    public void saveGreenhouseList(List<String> greenhouseList, List<String> greenhouseIdList) {
        try {
            greenhouseListData.put("greenhouseList", greenhouseList);
            greenhouseListData.put("greenhouseIdList", greenhouseIdList);
            saveDataToFile();
            Log.d(TAG, "大棚列表保存成功");
        } catch (Exception e) {
            Log.e(TAG, "保存大棚列表失败: " + e.getMessage());
        }
    }

    // 获取大棚列表
    public List<String> getGreenhouseList() {
        try {
            JSONArray listArray = greenhouseListData.getJSONArray("greenhouseList");
            if (listArray != null) {
                List<String> list = new ArrayList<>();
                for (int i = 0; i < listArray.size(); i++) {
                    list.add(listArray.getString(i));
                }
                return list;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取大棚列表失败: " + e.getMessage());
        }
        // 返回默认大棚列表
        List<String> defaultList = new ArrayList<>();
        defaultList.add("请选择大棚");
        defaultList.add("默认大棚");
        return defaultList;
    }

    // 获取大棚ID列表
    public List<String> getGreenhouseIdList() {
        try {
            JSONArray idArray = greenhouseListData.getJSONArray("greenhouseIdList");
            if (idArray != null) {
                List<String> list = new ArrayList<>();
                for (int i = 0; i < idArray.size(); i++) {
                    list.add(idArray.getString(i));
                }
                return list;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取大棚ID列表失败: " + e.getMessage());
        }
        // 返回默认大棚ID列表
        List<String> defaultList = new ArrayList<>();
        defaultList.add("");
        defaultList.add("1");
        return defaultList;
    }

    // 保存环境数据
    public void saveEnvironmentData(String greenhouseId, JSONObject data) {
        try {
            environmentData.put(greenhouseId, data);
            saveDataToFile();
            Log.d(TAG, "环境数据保存成功: " + greenhouseId);
        } catch (Exception e) {
            Log.e(TAG, "保存环境数据失败: " + e.getMessage());
        }
    }

    // 获取环境数据
    public JSONObject getEnvironmentData(String greenhouseId) {
        try {
            return environmentData.getJSONObject(greenhouseId);
        } catch (Exception e) {
            Log.e(TAG, "获取环境数据失败: " + e.getMessage());
        }
        return null;
    }

    // 保存传感器实时数据
    public void saveSensorData(String greenhouseId, JSONObject sensorData) {
        try {
            environmentData.put(greenhouseId + "_sensor", sensorData);
            saveDataToFile();
            Log.d(TAG, "传感器数据保存成功: " + greenhouseId);
        } catch (Exception e) {
            Log.e(TAG, "保存传感器数据失败: " + e.getMessage());
        }
    }

    // 获取传感器实时数据
    public JSONObject getSensorData(String greenhouseId) {
        try {
            return environmentData.getJSONObject(greenhouseId + "_sensor");
        } catch (Exception e) {
            Log.e(TAG, "获取传感器数据失败: " + e.getMessage());
        }
        return null;
    }

    // 保存上次选中的大棚索引
    public void saveLastSelectedIndex(int index) {
        try {
            lastSelectedData.put("lastSelectedIndex", index);
            saveDataToFile();
            Log.d(TAG, "上次选中大棚索引保存成功: " + index);
        } catch (Exception e) {
            Log.e(TAG, "保存上次选中大棚索引失败: " + e.getMessage());
        }
    }

    // 获取上次选中的大棚索引
    public int getLastSelectedIndex() {
        try {
            return lastSelectedData.getIntValue("lastSelectedIndex");
        } catch (Exception e) {
            Log.e(TAG, "获取上次选中大棚索引失败: " + e.getMessage());
        }
        // 默认返回0（请选择大棚）
        return 0;
    }

    // 保存每小时的历史数据（用于实时数据分析）
    public void saveHourlyHistoryData(String greenhouseId, JSONArray hourlyData) {
        try {
            hourlyHistoryData.put(greenhouseId, hourlyData);
            saveDataToFile();
            Log.d(TAG, "每小时历史数据保存成功: " + greenhouseId);
        } catch (Exception e) {
            Log.e(TAG, "保存每小时历史数据失败: " + e.getMessage());
        }
    }

    // 获取每小时的历史数据（用于实时数据分析）
    public JSONArray getHourlyHistoryData(String greenhouseId) {
        try {
            JSONArray hourlyData = hourlyHistoryData.getJSONArray(greenhouseId);
            if (hourlyData == null) {
                // 如果没有数据，返回空数组
                return new JSONArray();
            }
            return hourlyData;
        } catch (Exception e) {
            Log.e(TAG, "获取每小时历史数据失败: " + e.getMessage());
            return new JSONArray();
        }
    }

    // 保存每天的历史数据（用于历史数据分析）
    public void saveDailyHistoryData(String greenhouseId, JSONArray dailyData) {
        try {
            dailyHistoryData.put(greenhouseId, dailyData);
            saveDataToFile();
            Log.d(TAG, "每天历史数据保存成功: " + greenhouseId);
        } catch (Exception e) {
            Log.e(TAG, "保存每天历史数据失败: " + e.getMessage());
        }
    }

    // 获取每天的历史数据（用于历史数据分析）
    public JSONArray getDailyHistoryData(String greenhouseId) {
        try {
            JSONArray dailyData = dailyHistoryData.getJSONArray(greenhouseId);
            if (dailyData == null) {
                // 如果没有数据，返回空数组
                return new JSONArray();
            }
            return dailyData;
        } catch (Exception e) {
            Log.e(TAG, "获取每天历史数据失败: " + e.getMessage());
            return new JSONArray();
        }
    }

    // 清除所有数据
    public void clearAllData() {
        try {
            // 重置所有数据
            greenhouseListData = new JSONObject();
            environmentData = new JSONObject();
            lastSelectedData = new JSONObject();
            hourlyHistoryData = new JSONObject();
            dailyHistoryData = new JSONObject();
            
            // 设置默认值
            List<String> defaultList = new ArrayList<>();
            defaultList.add("请选择大棚");
            defaultList.add("默认大棚");
            List<String> defaultIdList = new ArrayList<>();
            defaultIdList.add("");
            defaultIdList.add("1");
            greenhouseListData.put("greenhouseList", defaultList);
            greenhouseListData.put("greenhouseIdList", defaultIdList);
            lastSelectedData.put("lastSelectedIndex", 0);
            
            Log.d(TAG, "所有数据清除成功");
        } catch (Exception e) {
            Log.e(TAG, "清除数据失败: " + e.getMessage());
        }
    }
}


