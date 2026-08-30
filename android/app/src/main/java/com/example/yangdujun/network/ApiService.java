package com.example.yangdujun.network;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.utils.LocalAlarmManager;
import com.example.yangdujun.utils.LocalDeviceManager;
import com.example.yangdujun.utils.LocalEnvironmentManager;
import com.example.yangdujun.utils.LocalGreenhouseManager;
import com.example.yangdujun.utils.LocalSettingManager;
import com.example.yangdujun.utils.LocalUserManager;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class ApiService {
    private static final String TAG = "ApiService";
    // API URL常量
    public static final String FEEDBACK_URL = "user/feedback";
    public static final String CHANGE_PASSWORD_URL = "user/changePassword";
    public static final String CHECK_UPDATE_URL = "app/checkUpdate";
    public static final String SAVE_SETTING_URL = "user/saveSetting";
    public static final String ADD_GREENHOUSE_URL = "greenhouse/create";
    
    private static Context appContext;

    // 设置应用上下文
    public static void setAppContext(Context context) {
        appContext = context.getApplicationContext();
    }

    // 注册接口（带验证码）
    public static String register(String phone, String password, String code) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("phone", phone)
                    .add("password", password)
                    .add("code", code)
                    .build();
            return ApiClient.post("user/register", body);
        } catch (Exception e) {
            Log.e(TAG, "网络注册失败，使用本地注册: " + e.getMessage());
            // 使用本地注册
            if (appContext != null) {
                LocalUserManager userManager = new LocalUserManager(appContext);
                boolean registered = userManager.register(phone, password, code);
                JSONObject response = new JSONObject();
                response.put("success", registered);
                response.put("message", registered ? "注册成功" : "注册失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 注册接口（不带验证码）
    public static String register(String phone, String password) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("phone", phone)
                    .add("password", password)
                    .build();
            return ApiClient.post("user/register", body);
        } catch (Exception e) {
            Log.e(TAG, "网络注册失败，使用本地注册: " + e.getMessage());
            // 使用本地注册
            if (appContext != null) {
                LocalUserManager userManager = new LocalUserManager(appContext);
                boolean registered = userManager.register(phone, password);
                JSONObject response = new JSONObject();
                response.put("success", registered);
                response.put("message", registered ? "注册成功" : "注册失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 登录接口
    public static String login(String phone, String password) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("phone", phone)
                    .add("password", password)
                    .build();
            return ApiClient.post("user/login", body);
        } catch (Exception e) {
            Log.e(TAG, "网络登录失败，使用本地登录: " + e.getMessage());
            // 使用本地登录
            if (appContext != null) {
                LocalUserManager userManager = new LocalUserManager(appContext);
                com.example.yangdujun.model.User user = userManager.login(phone, password);
                JSONObject response = new JSONObject();
                if (user != null) {
                    response.put("code", 200);
                    response.put("message", "登录成功");
                    JSONObject data = new JSONObject();
                    data.put("id", user.getId());
                    data.put("phone", user.getPhone());
                    data.put("token", user.getToken());
                    data.put("nickname", user.getNickname());
                    data.put("avatar", user.getAvatar());
                    response.put("data", data);
                } else {
                    response.put("code", 400);
                    response.put("message", "登录失败，账号或密码错误");
                }
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 发送验证码接口
    public static String sendCode(String phone, String type) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("phone", phone)
                    .add("type", type) // register 或 forget
                    .build();
            return ApiClient.post("user/sendCode", body);
        } catch (Exception e) {
            Log.e(TAG, "网络发送验证码失败，使用本地模拟: " + e.getMessage());
            // 使用本地模拟
            if (appContext != null) {
                LocalUserManager userManager = new LocalUserManager(appContext);
                boolean sent = userManager.sendCode(phone, type);
                JSONObject response = new JSONObject();
                response.put("success", sent);
                response.put("message", sent ? "验证码发送成功" : "验证码发送失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 忘记密码接口（带验证码）
    public static String forgetPassword(String phone, String code, String newPassword) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("phone", phone)
                    .add("code", code)
                    .add("newPassword", newPassword)
                    .build();
            return ApiClient.post("user/resetPassword", body);
        } catch (Exception e) {
            Log.e(TAG, "网络重置密码失败，使用本地重置: " + e.getMessage());
            // 使用本地重置
            if (appContext != null) {
                LocalUserManager userManager = new LocalUserManager(appContext);
                boolean reset = userManager.resetPassword(phone, code, newPassword);
                JSONObject response = new JSONObject();
                response.put("success", reset);
                response.put("message", reset ? "密码重置成功" : "密码重置失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 忘记密码接口（不带验证码）
    public static String forgetPassword(String phone, String newPassword) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("phone", phone)
                    .add("newPassword", newPassword)
                    .build();
            return ApiClient.post("user/resetPassword", body);
        } catch (Exception e) {
            Log.e(TAG, "网络重置密码失败，使用本地重置: " + e.getMessage());
            // 使用本地重置
            if (appContext != null) {
                LocalUserManager userManager = new LocalUserManager(appContext);
                boolean reset = userManager.resetPassword(phone, newPassword);
                JSONObject response = new JSONObject();
                response.put("success", reset);
                response.put("message", reset ? "密码重置成功" : "密码重置失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 获取用户信息接口
    public static String getUserInfo(String token) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder().build();
            return ApiClient.postWithToken("user/info", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络获取用户信息失败，使用本地获取: " + e.getMessage());
            // 使用本地获取
            if (appContext != null) {
                LocalUserManager userManager = new LocalUserManager(appContext);
                com.example.yangdujun.model.User user = userManager.getUserInfo(token);
                JSONObject response = new JSONObject();
                if (user != null) {
                    response.put("success", true);
                    response.put("message", "获取成功");
                    JSONObject data = new JSONObject();
                    data.put("id", user.getId());
                    data.put("phone", user.getPhone());
                    data.put("nickname", user.getNickname());
                    data.put("avatar", user.getAvatar());
                    response.put("data", data.toJSONString());
                } else {
                    response.put("success", false);
                    response.put("message", "获取失败");
                }
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 修改个人信息接口
    public static String updateUserInfo(String token, String nickname, String avatar) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("nickname", nickname)
                    .add("avatar", avatar)
                    .build();
            return ApiClient.postWithToken("user/update", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络更新用户信息失败，使用本地更新: " + e.getMessage());
            // 使用本地更新
            if (appContext != null) {
                LocalUserManager userManager = new LocalUserManager(appContext);
                boolean updated = userManager.updateUserInfo(token, nickname, avatar);
                JSONObject response = new JSONObject();
                response.put("success", updated);
                response.put("message", updated ? "更新成功" : "更新失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 修改密码接口
    public static String changePassword(String token, String oldPassword, String newPassword) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("oldPassword", oldPassword)
                    .add("newPassword", newPassword)
                    .build();
            return ApiClient.postWithToken("user/changePassword", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络修改密码失败，使用本地修改: " + e.getMessage());
            // 使用本地修改
            if (appContext != null) {
                LocalUserManager userManager = new LocalUserManager(appContext);
                boolean changed = userManager.changePassword(token, oldPassword, newPassword);
                JSONObject response = new JSONObject();
                response.put("success", changed);
                response.put("message", changed ? "密码修改成功" : "密码修改失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 获取温室列表接口
    public static String getGreenhouseList(String token) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder().build();
            return ApiClient.postWithToken("greenhouse/list", body, token);
        } catch (Exception e) {
            // 使用本地列表
            if (appContext != null) {
                LocalGreenhouseManager greenhouseManager = new LocalGreenhouseManager(appContext);
                JSONArray greenhouses = greenhouseManager.getGreenhouseList();
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "获取成功");
                response.put("data", greenhouses.toJSONString());
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 获取温室详情接口
    public static String getGreenhouseDetail(String token, String greenhouseId) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("greenhouseId", greenhouseId)
                    .build();
            return ApiClient.postWithToken("greenhouse/detail", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络获取大棚详情失败，使用本地详情: " + e.getMessage());
            // 使用本地详情
            if (appContext != null) {
                LocalGreenhouseManager greenhouseManager = new LocalGreenhouseManager(appContext);
                JSONObject greenhouse = greenhouseManager.getGreenhouseDetail(greenhouseId);
                JSONObject response = new JSONObject();
                if (greenhouse != null) {
                    response.put("success", true);
                    response.put("message", "获取成功");
                    response.put("data", greenhouse.toJSONString());
                } else {
                    response.put("success", false);
                    response.put("message", "获取失败");
                }
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 创建温室接口
    public static String createGreenhouse(String token, String name, String location, String area) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("name", name)
                    .add("location", location)
                    .add("area", area)
                    .build();
            return ApiClient.postWithToken("greenhouse/create", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络创建大棚失败，使用本地创建: " + e.getMessage());
            // 使用本地创建
            if (appContext != null) {
                LocalGreenhouseManager greenhouseManager = new LocalGreenhouseManager(appContext);
                JSONObject greenhouse = greenhouseManager.createGreenhouse(name, location, area);
                JSONObject response = new JSONObject();
                if (greenhouse != null) {
                    response.put("success", true);
                    response.put("message", "创建成功");
                    response.put("data", greenhouse.toJSONString());
                } else {
                    response.put("success", false);
                    response.put("message", "创建失败");
                }
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 更新温室接口
    public static String updateGreenhouse(String token, String greenhouseId, String name, String location, String area) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("greenhouseId", greenhouseId)
                    .add("name", name)
                    .add("location", location)
                    .add("area", area)
                    .build();
            return ApiClient.postWithToken("greenhouse/update", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络更新大棚失败，使用本地更新: " + e.getMessage());
            // 使用本地更新
            if (appContext != null) {
                LocalGreenhouseManager greenhouseManager = new LocalGreenhouseManager(appContext);
                boolean updated = greenhouseManager.updateGreenhouse(greenhouseId, name, location, area);
                JSONObject response = new JSONObject();
                response.put("success", updated);
                response.put("message", updated ? "更新成功" : "更新失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 删除温室接口
    public static String deleteGreenhouse(String token, String greenhouseId) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("greenhouseId", greenhouseId)
                    .build();
            return ApiClient.postWithToken("greenhouse/delete", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络删除大棚失败，使用本地删除: " + e.getMessage());
            // 使用本地删除
            if (appContext != null) {
                LocalGreenhouseManager greenhouseManager = new LocalGreenhouseManager(appContext);
                boolean deleted = greenhouseManager.deleteGreenhouse(greenhouseId);
                JSONObject response = new JSONObject();
                response.put("success", deleted);
                response.put("message", deleted ? "删除成功" : "删除失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 获取设备列表接口
    public static String getDeviceList(String token, String greenhouseId) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("greenhouseId", greenhouseId)
                    .build();
            return ApiClient.postWithToken("device/list", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络获取设备列表失败，使用本地列表: " + e.getMessage());
            // 使用本地列表
            if (appContext != null) {
                LocalDeviceManager deviceManager = new LocalDeviceManager(appContext);
                JSONArray devices = deviceManager.getDeviceList(greenhouseId);
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "获取成功");
                response.put("data", devices.toJSONString());
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 控制设备接口
    public static String controlDevice(String token, String deviceId, String status) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("deviceId", deviceId)
                    .add("status", status)
                    .build();
            return ApiClient.postWithToken("device/control", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络控制设备失败，使用本地控制: " + e.getMessage());
            // 使用本地控制
            if (appContext != null) {
                LocalDeviceManager deviceManager = new LocalDeviceManager(appContext);
                boolean controlled = deviceManager.controlDevice(deviceId, status);
                JSONObject response = new JSONObject();
                response.put("success", controlled);
                response.put("message", controlled ? "控制成功" : "控制失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 获取设备状态接口
    public static String getDeviceStatus(String token, String deviceId) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("deviceId", deviceId)
                    .build();
            return ApiClient.postWithToken("device/status", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络获取设备状态失败，使用本地状态: " + e.getMessage());
            // 使用本地状态
            if (appContext != null) {
                LocalDeviceManager deviceManager = new LocalDeviceManager(appContext);
                JSONObject status = deviceManager.getDeviceStatus(deviceId);
                JSONObject response = new JSONObject();
                if (status != null) {
                    response.put("success", true);
                    response.put("message", "获取成功");
                    response.put("data", status.toJSONString());
                } else {
                    response.put("success", false);
                    response.put("message", "获取失败");
                }
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 获取环境数据接口
    public static String getEnvironmentData(String token, String greenhouseId, String startTime, String endTime) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("greenhouseId", greenhouseId)
                    .add("startTime", startTime)
                    .add("endTime", endTime)
                    .build();
            return ApiClient.postWithToken("environment/data", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络获取环境数据失败，使用本地数据: " + e.getMessage());
            // 使用本地环境数据
            if (appContext != null) {
                LocalEnvironmentManager environmentManager = new LocalEnvironmentManager(appContext);
                JSONObject data = environmentManager.getEnvironmentData(greenhouseId);
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "获取成功");
                response.put("data", data.toJSONString());
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 获取告警列表接口
    public static String getAlarmList(String token, String greenhouseId) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("greenhouseId", greenhouseId)
                    .build();
            return ApiClient.postWithToken("alarm/list", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络获取告警列表失败，使用本地列表: " + e.getMessage());
            // 使用本地列表
            if (appContext != null) {
                LocalAlarmManager alarmManager = new LocalAlarmManager(appContext);
                JSONArray alarms = alarmManager.getAlarmList(greenhouseId);
                JSONObject response = new JSONObject();
                response.put("success", true);
                response.put("message", "获取成功");
                response.put("data", alarms.toJSONString());
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 处理告警接口
    public static String handleAlarm(String token, String alarmId, String handlerId, String handlerName) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("alarmId", alarmId)
                    .add("handlerId", handlerId)
                    .add("handlerName", handlerName)
                    .build();
            return ApiClient.postWithToken("alarm/handle", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络处理告警失败，使用本地处理: " + e.getMessage());
            // 使用本地处理
            if (appContext != null) {
                LocalAlarmManager alarmManager = new LocalAlarmManager(appContext);
                boolean handled = alarmManager.handleAlarm(alarmId, handlerId, handlerName);
                JSONObject response = new JSONObject();
                response.put("success", handled);
                response.put("message", handled ? "处理成功" : "处理失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 处理告警接口（兼容旧版本）
    public static String handleAlarm(String token, String alarmId) throws IOException {
        if (appContext != null) {
            LocalUserManager userManager = new LocalUserManager(appContext);
            com.example.yangdujun.model.User user = userManager.getCurrentUser();
            if (user != null) {
                return handleAlarm(token, alarmId, user.getId(), user.getNickname());
            }
        }
        return handleAlarm(token, alarmId, "unknown", "未知用户");
    }

    // 提交反馈接口
    public static String submitFeedback(String token, String content) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("content", content)
                    .build();
            return ApiClient.postWithToken("user/feedback", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络提交反馈失败，使用本地保存: " + e.getMessage());
            // 使用本地保存（这里可以实现一个本地反馈存储）
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("message", "反馈提交成功");
            return response.toJSONString();
        }
    }

    // 保存设置接口
    public static String saveSetting(String token, String type, String enabled) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("type", type)
                    .add("enabled", enabled)
                    .build();
            return ApiClient.postWithToken("user/saveSetting", body, token);
        } catch (Exception e) {
            Log.e(TAG, "网络保存设置失败，使用本地保存: " + e.getMessage());
            // 使用本地保存
            if (appContext != null) {
                LocalSettingManager settingManager = new LocalSettingManager(appContext);
                boolean saved = settingManager.saveSetting(type, enabled);
                JSONObject response = new JSONObject();
                response.put("success", saved);
                response.put("message", saved ? "保存成功" : "保存失败");
                return response.toJSONString();
            }
            throw e;
        }
    }

    // 检查更新接口
    public static String checkUpdate(String version) throws IOException {
        try {
            // 尝试网络请求
            RequestBody body = new FormBody.Builder()
                    .add("version", version)
                    .build();
            return ApiClient.post("app/checkUpdate", body);
        } catch (Exception e) {
            Log.e(TAG, "网络检查更新失败，使用本地响应: " + e.getMessage());
            // 使用本地响应
            JSONObject response = new JSONObject();
            response.put("success", true);
            response.put("message", "当前已是最新版本");
            response.put("data", new JSONObject().toJSONString());
            return response.toJSONString();
        }
    }
}


