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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {
    private static final String TAG = "StorageManager";
    private static StorageManager instance;
    private final File directory;
    private final ConcurrentHashMap<String, JSONObject> memoryCache;

    private StorageManager(Context context) {
        this.directory = context.getFilesDir();
        this.memoryCache = new ConcurrentHashMap<>();
        Log.d(TAG, "StorageManager initialized with directory: " + directory.getAbsolutePath());
    }

    public static synchronized StorageManager getInstance(Context context) {
        if (instance == null) {
            instance = new StorageManager(context.getApplicationContext());
        }
        return instance;
    }

    // 通用保存方法
    public synchronized void save(String fileName, String content) {
        File file = new File(directory, fileName);
        try {
            if (!file.exists()) {
                file.createNewFile();
                Log.d(TAG, "Created new file: " + fileName);
            }
            writeFile(file, content);
            // 更新内存缓存
            try {
                JSONObject jsonObject = JSON.parseObject(content);
                memoryCache.put(fileName, jsonObject);
            } catch (Exception e) {
                // 非JSON内容不缓存
                Log.d(TAG, "Content is not JSON, skipping cache: " + fileName);
            }
            Log.d(TAG, "Saved successfully: " + fileName);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save file: " + fileName, e);
        }
    }

    // 通用读取方法
    public synchronized String read(String fileName) {
        // 先从内存缓存读取
        if (memoryCache.containsKey(fileName)) {
            JSONObject cachedData = memoryCache.get(fileName);
            Log.d(TAG, "Reading from cache: " + fileName);
            return cachedData.toJSONString();
        }

        File file = new File(directory, fileName);
        if (!file.exists()) {
            Log.d(TAG, "File not found: " + fileName);
            return null;
        }

        String content = readFile(file);
        // 更新内存缓存
        if (content != null) {
            try {
                JSONObject jsonObject = JSON.parseObject(content);
                memoryCache.put(fileName, jsonObject);
            } catch (Exception e) {
                // 非JSON内容不缓存
                Log.d(TAG, "Content is not JSON, skipping cache: " + fileName);
            }
        }
        return content;
    }

    // 通用删除方法
    public synchronized void delete(String fileName) {
        File file = new File(directory, fileName);
        if (file.exists()) {
            if (file.delete()) {
                memoryCache.remove(fileName);
                Log.d(TAG, "Deleted successfully: " + fileName);
            } else {
                Log.e(TAG, "Failed to delete file: " + fileName);
            }
        }
    }

    // 检查文件是否存在
    public boolean exists(String fileName) {
        File file = new File(directory, fileName);
        return file.exists();
    }

    // 获取文件大小
    public long getFileSize(String fileName) {
        File file = new File(directory, fileName);
        if (file.exists()) {
            return file.length();
        }
        return 0;
    }

    // 清空内存缓存
    public void clearCache() {
        memoryCache.clear();
        Log.d(TAG, "Memory cache cleared");
    }

    // 写入文件
    private void writeFile(File file, String content) {
        try (OutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes("UTF-8"));
            outputStream.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to write file", e);
        }
    }

    // 读取文件
    private String readFile(File file) {
        StringBuilder content = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                content.append(new String(buffer, 0, length, "UTF-8"));
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to read file", e);
        }
        return content.toString();
    }

    // 保存JSON对象
    public void saveJsonObject(String fileName, JSONObject jsonObject) {
        save(fileName, jsonObject.toJSONString());
    }

    // 读取JSON对象
    public JSONObject readJsonObject(String fileName) {
        String content = read(fileName);
        if (content != null) {
            try {
                return JSON.parseObject(content);
            } catch (Exception e) {
                Log.e(TAG, "Failed to parse JSON", e);
            }
        }
        return null;
    }

    // 保存JSON数组
    public void saveJsonArray(String fileName, JSONArray jsonArray) {
        JSONObject wrapper = new JSONObject();
        wrapper.put("data", jsonArray);
        save(fileName, wrapper.toJSONString());
    }

    // 读取JSON数组
    public JSONArray readJsonArray(String fileName) {
        String content = read(fileName);
        if (content != null) {
            try {
                // 尝试直接解析为JSONArray
                return JSON.parseArray(content);
            } catch (Exception e) {
                // 如果失败，尝试解析为JSONObject，然后获取data字段
                try {
                    JSONObject jsonObject = JSON.parseObject(content);
                    if (jsonObject != null) {
                        return jsonObject.getJSONArray("data");
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Failed to parse JSON array", ex);
                }
            }
        }
        return null;
    }

    // 批量保存
    public void saveBatch(String[] fileNames, String[] contents) {
        if (fileNames.length != contents.length) {
            Log.e(TAG, "File names and contents length mismatch");
            return;
        }

        for (int i = 0; i < fileNames.length; i++) {
            save(fileNames[i], contents[i]);
        }
    }

    // 批量读取
    public String[] readBatch(String[] fileNames) {
        String[] contents = new String[fileNames.length];
        for (int i = 0; i < fileNames.length; i++) {
            contents[i] = read(fileNames[i]);
        }
        return contents;
    }
}


