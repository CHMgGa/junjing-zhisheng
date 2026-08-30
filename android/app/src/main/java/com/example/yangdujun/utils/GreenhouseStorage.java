package com.example.yangdujun.utils;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.Greenhouse;

import java.util.ArrayList;
import java.util.List;

public class GreenhouseStorage {
    private static final String TAG = "GreenhouseStorage";
    private static final String FILE_NAME = "greenhouses.json";
    private StorageManager storageManager;

    public GreenhouseStorage(Context context) {
        storageManager = StorageManager.getInstance(context);
    }

    public GreenhouseStorage() {
    }

    private StorageManager getStorageManager(Context context) {
        if (storageManager == null) {
            storageManager = StorageManager.getInstance(context);
        }
        return storageManager;
    }

    public void saveGreenhouseList(List<Greenhouse> greenhouseList) {
        try {
            JSONArray greenhouses = new JSONArray();
            for (Greenhouse greenhouse : greenhouseList) {
                // 跳过默认大棚，不保存到文件
                if (!"1".equals(greenhouse.id)) {
                    JSONObject greenhouseObj = new JSONObject();
                    greenhouseObj.put("id", greenhouse.id);
                    greenhouseObj.put("name", greenhouse.name);
                    greenhouseObj.put("distance", greenhouse.distance);
                    greenhouses.add(greenhouseObj);
                }
            }
            JSONObject root = new JSONObject();
            root.put("greenhouses", greenhouses);
            storageManager.saveJsonObject(FILE_NAME, root);
            Log.d(TAG, "Saved greenhouse list, size: " + greenhouseList.size());
        } catch (Exception e) {
            Log.e(TAG, "Failed to save greenhouse list", e);
        }
    }

    public List<Greenhouse> getGreenhouseList() {
        List<Greenhouse> greenhouseList = new ArrayList<>();
        try {
            JSONObject root = storageManager.readJsonObject(FILE_NAME);
            if (root != null) {
                JSONArray greenhouses = root.getJSONArray("greenhouses");
                if (greenhouses != null) {
                    for (int i = 0; i < greenhouses.size(); i++) {
                        JSONObject greenhouseObj = greenhouses.getJSONObject(i);
                        String id = greenhouseObj.getString("id");
                        String name = greenhouseObj.getString("name");
                        String distance = greenhouseObj.getString("distance");
                        // 跳过默认大棚，避免重复
                        if (!"1".equals(id)) {
                            greenhouseList.add(new Greenhouse(id, name, distance));
                        }
                    }
                    Log.d(TAG, "Loaded greenhouse list, size: " + greenhouseList.size());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load greenhouse list", e);
        }
        
        // 始终在列表开头添加默认大棚
        greenhouseList.add(0, new Greenhouse("1", "默认大棚", "0km"));
        Log.d(TAG, "Added default greenhouse at position 0");
        
        return greenhouseList;
    }

    public void addGreenhouse(Greenhouse greenhouse) {
        try {
            List<Greenhouse> currentList = getGreenhouseList();
            currentList.add(greenhouse);
            saveGreenhouseList(currentList);
            Log.d(TAG, "Added greenhouse: " + greenhouse.name);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add greenhouse", e);
        }
    }

    public void removeGreenhouse(String greenhouseId) {
        try {
            List<Greenhouse> currentList = getGreenhouseList();
            for (int i = 0; i < currentList.size(); i++) {
                if (currentList.get(i).id.equals(greenhouseId)) {
                    currentList.remove(i);
                    break;
                }
            }
            saveGreenhouseList(currentList);
            Log.d(TAG, "Removed greenhouse: " + greenhouseId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove greenhouse", e);
        }
    }

    public void clearGreenhouseList() {
        try {
            storageManager.delete(FILE_NAME);
            Log.d(TAG, "Cleared greenhouse list");
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear greenhouse list", e);
        }
    }
}

