package com.example.yangdujun.utils;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.UUID;

public class LocalGreenhouseManager {
    private static final String TAG = "LocalGreenhouseManager";
    private static final String GREENHOUSES_FILE = "local_greenhouses.json";
    private final StorageManager storageManager;

    public LocalGreenhouseManager(Context context) {
        this.storageManager = StorageManager.getInstance(context);
        initializeGreenhousesFile();
    }

    // 初始化大棚文件
    private void initializeGreenhousesFile() {
        if (!storageManager.exists(GREENHOUSES_FILE)) {
            JSONObject root = new JSONObject();
            root.put("greenhouses", new JSONArray());
            storageManager.saveJsonObject(GREENHOUSES_FILE, root);
            Log.d(TAG, "Initialized greenhouses file");
            // 添加默认大棚
            addDefaultGreenhouse();
        }
    }

    // 添加默认大棚
    private void addDefaultGreenhouse() {
        JSONObject defaultGreenhouse = new JSONObject();
        defaultGreenhouse.put("id", "default");
        defaultGreenhouse.put("name", "默认大棚");
        defaultGreenhouse.put("location", "本地测试");
        defaultGreenhouse.put("area", "100");
        defaultGreenhouse.put("createdAt", System.currentTimeMillis());

        try {
            JSONObject greenhousesData = storageManager.readJsonObject(GREENHOUSES_FILE);
            JSONArray greenhouses = greenhousesData.getJSONArray("greenhouses");
            greenhouses.add(defaultGreenhouse);
            greenhousesData.put("greenhouses", greenhouses);
            storageManager.saveJsonObject(GREENHOUSES_FILE, greenhousesData);
            Log.d(TAG, "Added default greenhouse");
        } catch (Exception e) {
            Log.e(TAG, "Failed to add default greenhouse", e);
        }
    }

    // 获取大棚列表
    public JSONArray getGreenhouseList() {
        try {
            JSONObject greenhousesData = storageManager.readJsonObject(GREENHOUSES_FILE);
            JSONArray greenhouses = greenhousesData.getJSONArray("greenhouses");
            Log.d(TAG, "Retrieved greenhouse list, count: " + greenhouses.size());
            return greenhouses;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get greenhouse list", e);
            return new JSONArray();
        }
    }

    // 获取大棚详情
    public JSONObject getGreenhouseDetail(String greenhouseId) {
        try {
            JSONObject greenhousesData = storageManager.readJsonObject(GREENHOUSES_FILE);
            JSONArray greenhouses = greenhousesData.getJSONArray("greenhouses");

            for (int i = 0; i < greenhouses.size(); i++) {
                JSONObject greenhouse = greenhouses.getJSONObject(i);
                if (greenhouse.getString("id").equals(greenhouseId)) {
                    Log.d(TAG, "Retrieved greenhouse detail: " + greenhouseId);
                    return greenhouse;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get greenhouse detail", e);
        }
        return null;
    }

    // 创建大棚
    public JSONObject createGreenhouse(String name, String location, String area) {
        try {
            JSONObject newGreenhouse = new JSONObject();
            newGreenhouse.put("id", UUID.randomUUID().toString());
            newGreenhouse.put("name", name);
            newGreenhouse.put("location", location);
            newGreenhouse.put("area", area);
            newGreenhouse.put("createdAt", System.currentTimeMillis());

            JSONObject greenhousesData = storageManager.readJsonObject(GREENHOUSES_FILE);
            JSONArray greenhouses = greenhousesData.getJSONArray("greenhouses");
            greenhouses.add(newGreenhouse);
            greenhousesData.put("greenhouses", greenhouses);
            storageManager.saveJsonObject(GREENHOUSES_FILE, greenhousesData);

            Log.d(TAG, "Created greenhouse: " + name);
            return newGreenhouse;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create greenhouse", e);
            return null;
        }
    }

    // 更新大棚
    public boolean updateGreenhouse(String greenhouseId, String name, String location, String area) {
        try {
            JSONObject greenhousesData = storageManager.readJsonObject(GREENHOUSES_FILE);
            JSONArray greenhouses = greenhousesData.getJSONArray("greenhouses");

            for (int i = 0; i < greenhouses.size(); i++) {
                JSONObject greenhouse = greenhouses.getJSONObject(i);
                if (greenhouse.getString("id").equals(greenhouseId)) {
                    greenhouse.put("name", name);
                    greenhouse.put("location", location);
                    greenhouse.put("area", area);
                    greenhouse.put("updatedAt", System.currentTimeMillis());

                    greenhousesData.put("greenhouses", greenhouses);
                    storageManager.saveJsonObject(GREENHOUSES_FILE, greenhousesData);

                    Log.d(TAG, "Updated greenhouse: " + greenhouseId);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update greenhouse", e);
        }
        return false;
    }

    // 删除大棚
    public boolean deleteGreenhouse(String greenhouseId) {
        // 不允许删除默认大棚
        if ("1".equals(greenhouseId)) {
            Log.d(TAG, "Cannot delete default greenhouse");
            return false;
        }

        try {
            JSONObject greenhousesData = storageManager.readJsonObject(GREENHOUSES_FILE);
            JSONArray greenhouses = greenhousesData.getJSONArray("greenhouses");

            for (int i = 0; i < greenhouses.size(); i++) {
                JSONObject greenhouse = greenhouses.getJSONObject(i);
                if (greenhouse.getString("id").equals(greenhouseId)) {
                    greenhouses.remove(i);
                    greenhousesData.put("greenhouses", greenhouses);
                    storageManager.saveJsonObject(GREENHOUSES_FILE, greenhousesData);

                    Log.d(TAG, "Deleted greenhouse: " + greenhouseId);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete greenhouse", e);
        }
        return false;
    }

    // 检查大棚是否存在
    public boolean exists(String greenhouseId) {
        try {
            JSONObject greenhousesData = storageManager.readJsonObject(GREENHOUSES_FILE);
            JSONArray greenhouses = greenhousesData.getJSONArray("greenhouses");

            for (int i = 0; i < greenhouses.size(); i++) {
                JSONObject greenhouse = greenhouses.getJSONObject(i);
                if (greenhouse.getString("id").equals(greenhouseId)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to check greenhouse existence", e);
        }
        return false;
    }

    // 获取大棚数量
    public int getGreenhouseCount() {
        try {
            JSONObject greenhousesData = storageManager.readJsonObject(GREENHOUSES_FILE);
            JSONArray greenhouses = greenhousesData.getJSONArray("greenhouses");
            return greenhouses.size();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get greenhouse count", e);
            return 0;
        }
    }

    // 清空所有大棚（保留默认大棚）
    public void clearAllGreenhouses() {
        try {
            JSONObject root = new JSONObject();
            JSONArray greenhouses = new JSONArray();
            
            // 重新添加默认大棚
            JSONObject defaultGreenhouse = new JSONObject();
            defaultGreenhouse.put("id", "default");
            defaultGreenhouse.put("name", "默认大棚");
            defaultGreenhouse.put("location", "本地测试");
            defaultGreenhouse.put("area", "100");
            defaultGreenhouse.put("createdAt", System.currentTimeMillis());
            greenhouses.add(defaultGreenhouse);
            
            root.put("greenhouses", greenhouses);
            storageManager.saveJsonObject(GREENHOUSES_FILE, root);
            Log.d(TAG, "Cleared all greenhouses,保留默认大棚");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear greenhouses", e);
        }
    }
}


