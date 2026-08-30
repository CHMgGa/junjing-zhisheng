package com.example.yangdujun.utils;

import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.model.User;

import java.util.ArrayList;
import java.util.List;

public class JsonFileStorage {
    private static final String TAG = "JsonFileStorage";
    private JSONObject root;
    private JSONArray users;

    public JsonFileStorage() {
        // 初始化内存中的JSON对象
        root = new JSONObject();
        users = new JSONArray();
        root.put("users", users);
    }

    // 保存用户信息
    public void saveUser(User user) {
        try {
            // 检查是否已存在该用户
            boolean exists = false;
            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("phone").equals(user.getPhone())) {
                    // 更新用户信息
                    userObj.put("id", user.getId());
                    userObj.put("token", user.getToken());
                    userObj.put("nickname", user.getNickname());
                    userObj.put("avatar", user.getAvatar());
                    exists = true;
                    break;
                }
            }

            // 如果不存在，添加新用户
            if (!exists) {
                JSONObject userObj = new JSONObject();
                userObj.put("id", user.getId());
                userObj.put("phone", user.getPhone());
                userObj.put("token", user.getToken());
                userObj.put("nickname", user.getNickname());
                userObj.put("avatar", user.getAvatar());
                users.add(userObj);
            }
        } catch (Exception e) {
            Log.e(TAG, "保存用户信息失败", e);
        }
    }

    // 保存用户账号密码
    public void saveUserCredentials(String phone, String password) {
        try {
            // 检查是否已存在该用户
            boolean exists = false;
            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("phone").equals(phone)) {
                    // 更新密码
                    userObj.put("password", password);
                    exists = true;
                    break;
                }
            }

            // 如果不存在，添加新用户
            if (!exists) {
                JSONObject userObj = new JSONObject();
                userObj.put("phone", phone);
                userObj.put("password", password);
                users.add(userObj);
            }
        } catch (Exception e) {
            Log.e(TAG, "保存用户凭证失败", e);
        }
    }

    // 获取用户信息
    public User getUser(String phone) {
        try {
            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("phone").equals(phone)) {
                    User user = new User();
                    user.setId(userObj.getString("id"));
                    user.setPhone(userObj.getString("phone"));
                    user.setToken(userObj.getString("token"));
                    user.setNickname(userObj.getString("nickname"));
                    user.setAvatar(userObj.getString("avatar"));
                    return user;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取用户信息失败", e);
        }
        return null;
    }

    // 获取所有用户
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        try {
            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                User user = new User();
                user.setId(userObj.getString("id"));
                user.setPhone(userObj.getString("phone"));
                user.setToken(userObj.getString("token"));
                user.setNickname(userObj.getString("nickname"));
                user.setAvatar(userObj.getString("avatar"));
                userList.add(user);
            }
        } catch (Exception e) {
            Log.e(TAG, "获取所有用户失败", e);
        }
        return userList;
    }

    // 检查账号密码是否匹配
    public boolean checkCredentials(String phone, String password) {
        try {
            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("phone").equals(phone)) {
                    String storedPassword = userObj.getString("password");
                    return storedPassword.equals(password);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "验证账号密码失败", e);
        }
        return false;
    }

    // 获取用户密码
    public String getPassword(String phone) {
        try {
            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("phone").equals(phone)) {
                    return userObj.getString("password");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取用户密码失败", e);
        }
        return "";
    }

    // 修改密码
    public boolean changePassword(String phone, String oldPassword, String newPassword) {
        try {
            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("phone").equals(phone)) {
                    String storedPassword = userObj.getString("password");
                    if (storedPassword.equals(oldPassword)) {
                        // 更新密码
                        userObj.put("password", newPassword);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "修改密码失败", e);
        }
        return false;
    }

    // 删除用户
    public boolean deleteUser(String phone) {
        try {
            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("phone").equals(phone)) {
                    users.remove(i);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "删除用户失败", e);
        }
        return false;
    }
}


