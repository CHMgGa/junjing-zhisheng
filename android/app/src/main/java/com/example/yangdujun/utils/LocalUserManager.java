package com.example.yangdujun.utils;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.model.User;

import java.util.UUID;

public class LocalUserManager {
    private static final String TAG = "LocalUserManager";
    private static final String USERS_FILE = "local_users.json";
    private static final String CURRENT_USER_FILE = "current_user.json";
    private static final int MIN_PASSWORD_LENGTH = 8;
    private final StorageManager storageManager;

    public LocalUserManager(Context context) {
        this.storageManager = StorageManager.getInstance(context);
        initializeUsersFile();
    }

    // 验证密码长度
    private boolean isValidPassword(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }

    // 初始化用户文件
    private void initializeUsersFile() {
        if (!storageManager.exists(USERS_FILE)) {
            JSONObject root = new JSONObject();
            root.put("users", new JSONArray());
            storageManager.saveJsonObject(USERS_FILE, root);
            Log.d(TAG, "Initialized users file");
        }
    }

    // 注册用户（带验证码）
    public boolean register(String phone, String password, String code) {
        if (!isValidPassword(password)) {
            Log.d(TAG, "Password too short, minimum " + MIN_PASSWORD_LENGTH + " characters required");
            return false;
        }
        return register(phone, password);
    }

    // 注册用户（不带验证码）
    public boolean register(String phone, String password) {
        if (!isValidPassword(password)) {
            Log.d(TAG, "Password too short, minimum " + MIN_PASSWORD_LENGTH + " characters required");
            return false;
        }
        try {
            JSONObject usersData = storageManager.readJsonObject(USERS_FILE);
            JSONArray users = usersData.getJSONArray("users");

            // 检查手机号是否已存在
            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("phone").equals(phone)) {
                    Log.d(TAG, "Phone already registered: " + phone);
                    return false;
                }
            }

            // 创建新用户
            JSONObject newUser = new JSONObject();
            newUser.put("id", UUID.randomUUID().toString());
            newUser.put("phone", phone);
            newUser.put("password", password);
            newUser.put("token", generateToken());
            newUser.put("nickname", "用户" + phone.substring(phone.length() - 4));
            newUser.put("avatar", "");

            users.add(newUser);
            usersData.put("users", users);
            storageManager.saveJsonObject(USERS_FILE, usersData);

            Log.d(TAG, "User registered successfully: " + phone);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to register user", e);
            return false;
        }
    }

    // 用户登录
    public User login(String phone, String password) {
        try {
            JSONObject usersData = storageManager.readJsonObject(USERS_FILE);
            JSONArray users = usersData.getJSONArray("users");

            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("phone").equals(phone) && 
                    userObj.getString("password").equals(password)) {

                    // 更新用户token
                    String newToken = generateToken();
                    userObj.put("token", newToken);
                    usersData.put("users", users);
                    storageManager.saveJsonObject(USERS_FILE, usersData);

                    // 保存当前登录用户
                    storageManager.saveJsonObject(CURRENT_USER_FILE, userObj);

                    // 转换为User对象
                    User user = new User();
                    user.setId(userObj.getString("id"));
                    user.setPhone(userObj.getString("phone"));
                    user.setToken(userObj.getString("token"));
                    user.setNickname(userObj.getString("nickname"));
                    user.setAvatar(userObj.getString("avatar"));

                    Log.d(TAG, "User logged in successfully: " + phone);
                    return user;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to login user", e);
        }
        return null;
    }

    // 获取当前登录用户
    public User getCurrentUser() {
        try {
            JSONObject userObj = storageManager.readJsonObject(CURRENT_USER_FILE);
            if (userObj != null) {
                User user = new User();
                user.setId(userObj.getString("id"));
                user.setPhone(userObj.getString("phone"));
                user.setToken(userObj.getString("token"));
                user.setNickname(userObj.getString("nickname"));
                user.setAvatar(userObj.getString("avatar"));
                return user;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get current user", e);
        }
        return null;
    }

    // 获取用户信息
    public User getUserInfo(String token) {
        try {
            JSONObject usersData = storageManager.readJsonObject(USERS_FILE);
            JSONArray users = usersData.getJSONArray("users");

            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("token").equals(token)) {
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
            Log.e(TAG, "Failed to get user info", e);
        }
        return null;
    }

    // 更新用户信息
    public boolean updateUserInfo(String token, String nickname, String avatar) {
        try {
            JSONObject usersData = storageManager.readJsonObject(USERS_FILE);
            JSONArray users = usersData.getJSONArray("users");

            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("token").equals(token)) {
                    userObj.put("nickname", nickname);
                    userObj.put("avatar", avatar);
                    usersData.put("users", users);
                    storageManager.saveJsonObject(USERS_FILE, usersData);

                    // 更新当前用户信息
                    if (isCurrentUser(token)) {
                        storageManager.saveJsonObject(CURRENT_USER_FILE, userObj);
                    }

                    Log.d(TAG, "User info updated successfully");
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update user info", e);
        }
        return false;
    }

    // 修改密码
    public boolean changePassword(String token, String oldPassword, String newPassword) {
        if (!isValidPassword(newPassword)) {
            Log.d(TAG, "New password too short, minimum " + MIN_PASSWORD_LENGTH + " characters required");
            return false;
        }
        try {
            JSONObject usersData = storageManager.readJsonObject(USERS_FILE);
            JSONArray users = usersData.getJSONArray("users");

            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("token").equals(token) && 
                    userObj.getString("password").equals(oldPassword)) {

                    userObj.put("password", newPassword);
                    usersData.put("users", users);
                    storageManager.saveJsonObject(USERS_FILE, usersData);

                    Log.d(TAG, "Password changed successfully");
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to change password", e);
        }
        return false;
    }

    // 忘记密码（通过手机号重置，带验证码）
    public boolean resetPassword(String phone, String code, String newPassword) {
        if (!isValidPassword(newPassword)) {
            Log.d(TAG, "New password too short, minimum " + MIN_PASSWORD_LENGTH + " characters required");
            return false;
        }
        return resetPassword(phone, newPassword);
    }

    // 忘记密码（通过手机号重置，不带验证码）
    public boolean resetPassword(String phone, String newPassword) {
        if (!isValidPassword(newPassword)) {
            Log.d(TAG, "New password too short, minimum " + MIN_PASSWORD_LENGTH + " characters required");
            return false;
        }
        try {
            JSONObject usersData = storageManager.readJsonObject(USERS_FILE);
            JSONArray users = usersData.getJSONArray("users");

            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("phone").equals(phone)) {
                    userObj.put("password", newPassword);
                    usersData.put("users", users);
                    storageManager.saveJsonObject(USERS_FILE, usersData);

                    Log.d(TAG, "Password reset successfully for: " + phone);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to reset password", e);
        }
        return false;
    }

    // 用户注销
    public void logout() {
        storageManager.delete(CURRENT_USER_FILE);
        Log.d(TAG, "User logged out");
    }

    // 检查token是否有效
    public boolean isValidToken(String token) {
        try {
            JSONObject usersData = storageManager.readJsonObject(USERS_FILE);
            JSONArray users = usersData.getJSONArray("users");

            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                if (userObj.getString("token").equals(token)) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to validate token", e);
        }
        return false;
    }

    // 发送验证码（本地模拟）
    public boolean sendCode(String phone, String type) {
        // 本地模拟发送验证码，实际开发中可以集成短信SDK
        Log.d(TAG, "Simulating sending code to: " + phone + " for type: " + type);
        // 这里可以返回true，表示验证码发送成功
        return true;
    }

    // 检查是否是当前登录用户
    private boolean isCurrentUser(String token) {
        try {
            JSONObject currentUser = storageManager.readJsonObject(CURRENT_USER_FILE);
            return currentUser != null && currentUser.getString("token").equals(token);
        } catch (Exception e) {
            return false;
        }
    }

    // 生成token
    private String generateToken() {
        return "local_" + UUID.randomUUID().toString().replace("-", "");
    }


}


