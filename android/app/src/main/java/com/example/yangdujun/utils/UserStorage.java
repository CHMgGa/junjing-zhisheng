package com.example.yangdujun.utils;

import android.content.Context;
import android.app.Application;
import com.example.yangdujun.model.User;

public class UserStorage {
    private boolean loginStatus;
    private String currentPhone;
    private LocalUserManager localUserManager;

    public UserStorage() {
        loginStatus = false;
        currentPhone = "";
        // 获取应用上下文
        Context context = getApplicationContext();
        if (context != null) {
            localUserManager = new LocalUserManager(context);
            // 初始化登录状态
            User currentUser = localUserManager.getCurrentUser();
            if (currentUser != null) {
                loginStatus = true;
                currentPhone = currentUser.getPhone();
            }
        }
    }

    public UserStorage(JsonFileStorage jsonFileStorage) {
        this();
    }

    // 获取应用上下文
    private Context getApplicationContext() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Object application = activityThreadClass.getMethod("getApplication").invoke(activityThread);
            return (Context) application;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 保存用户信息
    public void saveUser(User user) {
        if (localUserManager != null) {
            // 直接更新当前用户信息到文件
            localUserManager.updateUserInfo(user.getToken(), user.getNickname(), user.getAvatar());
            loginStatus = true;
            currentPhone = user.getPhone();
        }
    }

    // 保存用户账号密码（用于本地登录验证）
    public void saveUserCredentials(String phone, String password) {
        if (localUserManager != null) {
            // 保存到本地文件系统
            localUserManager.login(phone, password);
            currentPhone = phone;
        }
    }

    // 获取密码
    public String getPassword() {
        if (currentPhone.isEmpty()) {
            return "";
        }
        // 从本地文件系统获取
        return "";
    }

    // 获取用户信息
    public User getUser() {
        if (!loginStatus) {
            return null;
        }

        if (currentPhone.isEmpty()) {
            return null;
        }

        // 从本地文件系统获取
        if (localUserManager != null) {
            return localUserManager.getCurrentUser();
        }
        return null;
    }

    // 检查是否已登录
    public boolean isLoggedIn() {
        if (localUserManager != null) {
            User currentUser = localUserManager.getCurrentUser();
            return currentUser != null;
        }
        return loginStatus;
    }

    // 清除用户信息
    public void clearUser() {
        if (localUserManager != null) {
            localUserManager.logout();
        }
        loginStatus = false;
        currentPhone = "";
    }

    // 获取Token
    public String getToken() {
        if (localUserManager != null) {
            User user = localUserManager.getCurrentUser();
            return user != null ? user.getToken() : "";
        }
        User user = getUser();
        return user != null ? user.getToken() : "";
    }

    // 获取手机号
    public String getPhone() {
        if (localUserManager != null) {
            User user = localUserManager.getCurrentUser();
            return user != null ? user.getPhone() : "";
        }
        return currentPhone;
    }

    // 检查账号密码是否匹配
    public boolean checkCredentials(String phone, String password) {
        if (localUserManager != null) {
            // 使用本地用户管理器验证
            User user = localUserManager.login(phone, password);
            return user != null;
        }
        return false;
    }
}


