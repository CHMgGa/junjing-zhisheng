package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.utils.GreenhouseHomeStorage;
import com.example.yangdujun.utils.ManagePageStorage;
import com.example.yangdujun.utils.UserStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeviceMonitorActivity extends AppCompatActivity {
    private static final String TAG = "DeviceMonitorActivity";
    
    // 华为云配置
    private static final String DEVICE_ID = BuildConfig.HUAWEI_DEVICE_ID;
    private static final String PROJECT_ID = BuildConfig.HUAWEI_PROJECT_ID;
    private static final String SERVICE_ID = "smartlan";
    private static final String CMD_FAN_GEAR = "Fengdegree";
    private static final String CMD_LIGHT = "Light";
    private static final String CMD_BOARD = "Board";
    private static final String PARAM_FAN = "fengdegree";
    private static final String PARAM_LIGHT = "light";
    private static final String PARAM_BOARD = "board";
    private static final String CMD_BUMP = "BUMP";
    private static final String PARAM_BUMP = "bump";
    private static final String STATUS_ON = "ON";
    private static final String STATUS_OFF = "OFF";
    private static final String IOTDA_ENDPOINT = BuildConfig.HUAWEI_IOTDA_ENDPOINT;
    private static final String IAM_ENDPOINT = BuildConfig.HUAWEI_IAM_ENDPOINT;
    private static final String HUAWEI_CLOUD_USER = BuildConfig.HUAWEI_CLOUD_USER;
    private static final String HUAWEI_CLOUD_PWD = BuildConfig.HUAWEI_CLOUD_PWD;
    private static final String HUAWEI_CLOUD_DOMAIN = BuildConfig.HUAWEI_CLOUD_DOMAIN;
    private static final String HUAWEI_CLOUD_PROJECT_NAME = BuildConfig.HUAWEI_CLOUD_PROJECT_NAME;
    
    // 命令下发间隔（毫秒）
    private static final long COMMAND_INTERVAL = 2000;
    private long lastCommandTime = 0;
    
    private OkHttpClient client;
    private String AToken;
    
    private Spinner spinnerGreenhouse, spinnerDevice, spinnerOperate;
    private TextView tvStatus;
    private Button btnExecute, btnBack;
    
    private UserStorage userStorage;
    private ManagePageStorage managePageStorage;
    private GreenhouseHomeStorage greenhouseHomeStorage;
    private List<String> greenhouseNames = new ArrayList<>();
    private List<String> greenhouseIds = new ArrayList<>();
    private List<String> deviceNames = new ArrayList<>();
    private List<String> deviceIds = new ArrayList<>();
    private String currentGreenhouseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_monitor);
        
        client = new OkHttpClient().newBuilder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        
        initViews();
        userStorage = new UserStorage();
        managePageStorage = new ManagePageStorage(this);
        greenhouseHomeStorage = new GreenhouseHomeStorage(this);
        
        // 获取华为云Token
        getHuaweiCloudToken();
        
        initSpinners();
        initListener();
    }

    private void initViews() {
        spinnerGreenhouse = findViewById(R.id.spinner_greenhouse);
        spinnerDevice = findViewById(R.id.spinner_device);
        spinnerOperate = findViewById(R.id.spinner_operate);
        tvStatus = findViewById(R.id.tv_status);
        btnExecute = findViewById(R.id.btn_execute);
        btnBack = findViewById(R.id.btn_back);
    }

    // 初始化下拉选择器
    private void initSpinners() {
        // 从GreenhouseHomeStorage获取大棚列表
        greenhouseNames = greenhouseHomeStorage.getGreenhouseList();
        greenhouseIds = greenhouseHomeStorage.getGreenhouseIdList();
        
        // 确保至少有默认大棚
        if (greenhouseNames.isEmpty()) {
            greenhouseNames.add("默认大棚");
            greenhouseIds.add("1");
        }
        
        // 更新大棚下拉框
        updateGreenhouseSpinner();
        
        // 初始化设备列表为空，等待选择大棚后加载
        deviceNames.add("请先选择大棚");
        deviceIds.add("");
        updateDeviceSpinner();
        
        // 初始化操作选择为空，等待选择设备后加载
        List<String> operateOptions = new ArrayList<>();
        operateOptions.add("请先选择设备");
        ArrayAdapter<String> operateAdapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, operateOptions);
        operateAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerOperate.setAdapter(operateAdapter);
    }

    // 更新大棚下拉框
    private void updateGreenhouseSpinner() {
        if (spinnerGreenhouse == null) {
            return;
        }
        
        ArrayAdapter<String> greenhouseAdapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, greenhouseNames);
        greenhouseAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerGreenhouse.setAdapter(greenhouseAdapter);
        
        // 设置默认选中的大棚
        if (currentGreenhouseId != null && !currentGreenhouseId.isEmpty()) {
            int position = greenhouseIds.indexOf(currentGreenhouseId);
            if (position >= 0) {
                spinnerGreenhouse.setSelection(position);
            }
        }
        
        // 监听大棚选择变化
        spinnerGreenhouse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < greenhouseIds.size()) {
                    currentGreenhouseId = greenhouseIds.get(position);
                    // 加载该大棚的设备列表
                    loadDeviceListForGreenhouse(currentGreenhouseId);
                } else {
                    currentGreenhouseId = "";
                    // 清空设备列表
                    deviceNames.clear();
                    deviceIds.clear();
                    deviceNames.add("请先选择大棚");
                    deviceIds.add("");
                    updateDeviceSpinner();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // 加载大棚的设备列表
    private void loadDeviceListForGreenhouse(String greenhouseId) {
        // 立即拉取华为云数据
        pullHuaweiCloudData(greenhouseId);
        
        // 从GreenhouseHomeStorage获取环境数据，包含设备状态
        JSONObject environmentData = greenhouseHomeStorage.getEnvironmentData(greenhouseId);
        
        // 无论是否有环境数据，都添加五个设备（水泵和药泵状态由大棚页面按钮控制）
        deviceNames.clear();
        deviceIds.clear();
        
        // 添加五个设备
        deviceNames.add("灯");
        deviceIds.add("light_1");
        
        deviceNames.add("挡光板");
        deviceIds.add("board_1");
        
        deviceNames.add("风机");
        deviceIds.add("fan_1");
        
        deviceNames.add("水泵");
        deviceIds.add("water_pump_1");
        
        deviceNames.add("药泵");
        deviceIds.add("medicine_pump_1");
        
        updateDeviceSpinner();
        
        // 默认选择第一个设备
        if (deviceNames.size() > 0) {
            spinnerDevice.setSelection(0);
        }
        
        // 更新设备状态显示
        if (environmentData != null) {
            updateDeviceStatusDisplay(environmentData);
        } else {
            // 如果没有环境数据，显示默认状态
            JSONObject defaultData = new JSONObject();
            defaultData.put("lightStatus", "关");
            defaultData.put("windBoardStatus", "关");
            defaultData.put("windSpeed", "0");
            updateDeviceStatusDisplay(defaultData);
        }
    }

    // 更新设备状态显示
    private void updateDeviceStatusDisplay(JSONObject environmentData) {
        if (environmentData != null && tvStatus != null) {
            // 从环境数据中获取设备状态
            String lightStatus = environmentData.getString("lightStatus");
            String windBoardStatus = environmentData.getString("windBoardStatus");
            String windSpeed = environmentData.getString("windSpeed");
            
            // 处理可能的空值
            if (lightStatus == null) lightStatus = "关";
            if (windBoardStatus == null) windBoardStatus = "关";
            if (windSpeed == null) windSpeed = "0";
            
            StringBuilder statusText = new StringBuilder();
            statusText.append("设备状态：\n");
            statusText.append("灯：").append(lightStatus).append("\n");
            statusText.append("挡光板：").append(windBoardStatus).append("\n");
            statusText.append("风机：").append(windSpeed).append("档\n");
            
            // 添加水泵和药泵状态（从环境数据中获取，由大棚页面按钮控制）
            String waterPumpStatus = environmentData.getString("waterPumpStatus");
            String medicinePumpStatus = environmentData.getString("medicinePumpStatus");
            if (waterPumpStatus == null) waterPumpStatus = "关";
            if (medicinePumpStatus == null) medicinePumpStatus = "关";
            
            statusText.append("水泵：").append(waterPumpStatus).append("\n");
            statusText.append("药泵：").append(medicinePumpStatus);
            
            tvStatus.setText(statusText.toString());
        }
    }

    // 根据设备类型更新操作选项
    private void updateOperateOptions(String deviceId, String deviceName) {
        if (spinnerOperate == null) {
            return;
        }
        
        List<String> operateOptions = new ArrayList<>();
        int defaultPosition = 0;
        
        if ("fan_1".equals(deviceId)) {
            // 风机：0~9档
            for (int i = 0; i <= 9; i++) {
                operateOptions.add(i + "档");
            }
            // 获取当前风机档位
            JSONObject environmentData = greenhouseHomeStorage.getEnvironmentData(currentGreenhouseId);
            if (environmentData != null) {
                String windSpeed = environmentData.getString("windSpeed");
                if (windSpeed != null) {
                    try {
                        int gear = Integer.parseInt(windSpeed);
                        if (gear >= 0 && gear <= 9) {
                            defaultPosition = gear;
                        }
                    } catch (NumberFormatException e) {
                        // 忽略异常，使用默认值
                    }
                }
            }
        } else if ("light_1".equals(deviceId)) {
            // 灯：开启/关闭
            operateOptions.add("开启");
            operateOptions.add("关闭");
            // 获取当前灯状态
            JSONObject environmentData = greenhouseHomeStorage.getEnvironmentData(currentGreenhouseId);
            if (environmentData != null) {
                String lightStatus = environmentData.getString("lightStatus");
                if ("开".equals(lightStatus)) {
                    defaultPosition = 0;
                } else {
                    defaultPosition = 1;
                }
            }
        } else if ("board_1".equals(deviceId)) {
            // 挡光板：开启/关闭
            operateOptions.add("开启");
            operateOptions.add("关闭");
            // 获取当前挡光板状态
            JSONObject environmentData = greenhouseHomeStorage.getEnvironmentData(currentGreenhouseId);
            if (environmentData != null) {
                String windBoardStatus = environmentData.getString("windBoardStatus");
                if ("开".equals(windBoardStatus)) {
                    defaultPosition = 0;
                } else {
                    defaultPosition = 1;
                }
            }
        } else if ("water_pump_1".equals(deviceId)) {
            // 水泵：开启/关闭
            operateOptions.add("开启");
            operateOptions.add("关闭");
            // 获取当前水泵状态（从环境数据中获取，由大棚页面按钮控制）
            JSONObject environmentData = greenhouseHomeStorage.getEnvironmentData(currentGreenhouseId);
            if (environmentData != null) {
                String waterPumpStatus = environmentData.getString("waterPumpStatus");
                if ("开".equals(waterPumpStatus)) {
                    defaultPosition = 0;
                } else {
                    defaultPosition = 1;
                }
            }
        } else if ("medicine_pump_1".equals(deviceId)) {
            // 药泵：开启/关闭
            operateOptions.add("开启");
            operateOptions.add("关闭");
            // 获取当前药泵状态（从环境数据中获取，由大棚页面按钮控制）
            JSONObject environmentData = greenhouseHomeStorage.getEnvironmentData(currentGreenhouseId);
            if (environmentData != null) {
                String medicinePumpStatus = environmentData.getString("medicinePumpStatus");
                if ("开".equals(medicinePumpStatus)) {
                    defaultPosition = 0;
                } else {
                    defaultPosition = 1;
                }
            }
        } else {
            operateOptions.add("未知操作");
        }
        
        ArrayAdapter<String> operateAdapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, operateOptions);
        operateAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerOperate.setAdapter(operateAdapter);
        
        // 设置默认选择的操作选项
        if (defaultPosition < operateOptions.size()) {
            spinnerOperate.setSelection(defaultPosition);
        }
    }

    private void updateDeviceSpinner() {
        if (spinnerDevice == null) {
            return;
        }
        
        ArrayAdapter<String> deviceAdapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, deviceNames);
        deviceAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerDevice.setAdapter(deviceAdapter);
        
        // 监听设备选择变化
        spinnerDevice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < deviceIds.size() && !deviceIds.get(position).isEmpty()) {
                    String deviceId = deviceIds.get(position);
                    String deviceName = deviceNames.get(position);
                    // 根据设备类型更新操作选项
                    updateOperateOptions(deviceId, deviceName);
                } else {
                    // 清空操作选项
                    if (spinnerOperate != null) {
                        List<String> operateOptions = new ArrayList<>();
                        operateOptions.add("请先选择设备");
                        ArrayAdapter<String> operateAdapter = new ArrayAdapter<>(DeviceMonitorActivity.this,
                                R.layout.custom_spinner_item, operateOptions);
                        operateAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
                        spinnerOperate.setAdapter(operateAdapter);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // 执行按钮+返回按钮 点击事件
    private void initListener() {
        // 返回按钮点击事件
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(DeviceMonitorActivity.this, ManagePage.class);
                startActivity(intent);
                finish();
            });
        }

        // 执行按钮点击事件
        if (btnExecute != null) {
            btnExecute.setOnClickListener(v -> {
                if (spinnerGreenhouse == null || spinnerDevice == null || spinnerOperate == null) {
                    Toast.makeText(DeviceMonitorActivity.this, "页面加载失败，请重试", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int greenhousePosition = spinnerGreenhouse.getSelectedItemPosition();
                int devicePosition = spinnerDevice.getSelectedItemPosition();
                Object operateObj = spinnerOperate.getSelectedItem();
                if (operateObj == null) {
                    Toast.makeText(DeviceMonitorActivity.this, "请选择操作", Toast.LENGTH_SHORT).show();
                    return;
                }
                String operate = operateObj.toString();

                if (greenhousePosition < 0 || greenhousePosition >= greenhouseIds.size() || 
                    greenhouseIds.get(greenhousePosition).isEmpty()) {
                    Toast.makeText(DeviceMonitorActivity.this, "请选择有效的大棚", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (devicePosition < 0 || devicePosition >= deviceIds.size() || 
                    deviceIds.get(devicePosition).isEmpty()) {
                    Toast.makeText(DeviceMonitorActivity.this, "请选择有效的设备", Toast.LENGTH_SHORT).show();
                    return;
                }

                String greenhouseId = greenhouseIds.get(greenhousePosition);
                String deviceId = deviceIds.get(devicePosition);
                String deviceName = deviceNames.get(devicePosition);

                // 控制设备
                controlDevice(greenhouseId, deviceId, deviceName, operate);
            });
        }
    }

    private void controlDevice(String greenhouseId, String deviceId, String deviceName, String operate) {
        // 根据设备类型构建命令值
        Object commandValue = null;
        
        if ("light_1".equals(deviceId)) {
            // 灯：开启/关闭
            commandValue = "开启".equals(operate) ? "ON" : "OFF";
        } else if ("board_1".equals(deviceId)) {
            // 挡光板：开启/关闭
            commandValue = "开启".equals(operate) ? "ON" : "OFF";
        } else if ("fan_1".equals(deviceId)) {
            // 风机：0~9档
            int gear = 0;
            try {
                gear = Integer.parseInt(operate.replace("档", ""));
            } catch (NumberFormatException e) {
                gear = 0;
            }
            commandValue = gear;
        } else if ("water_pump_1".equals(deviceId)) {
            // 水泵：开启/关闭（使用BUMP命令，与大棚页面一致）
            commandValue = "开启".equals(operate) ? "ON" : "OFF";
        } else if ("medicine_pump_1".equals(deviceId)) {
            // 药泵：开启/关闭（使用BUMP命令，与大棚页面一致）
            commandValue = "开启".equals(operate) ? "ON" : "OFF";
        }
        
        // 下发华为云命令
        sendCommandToHuaweiIoT(deviceId, commandValue, () -> {
            // 命令下发成功后的回调
            // 不需要手动更新状态，依赖pullHuaweiCloudData自动更新
        });
        
        // 提示操作成功
        Toast.makeText(this, "操作成功：" + deviceName + " " + operate, Toast.LENGTH_SHORT).show();
    }

    /**
     * 下发命令到华为云（与GreenhouseHomePage保持一致）
     */
    private void sendCommandToHuaweiIoT(String deviceType, Object commandValue, Runnable successCallback) {
        // 1. 防重复点击检查
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCommandTime < COMMAND_INTERVAL) {
            runOnUiThread(() -> Toast.makeText(this, "操作太频繁，请稍后再试", Toast.LENGTH_SHORT).show());
            return;
        }
        lastCommandTime = currentTime;

        // 2. 检查Token
        if (TextUtils.isEmpty(AToken)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "华为云Token为空，正在重新获取", Toast.LENGTH_SHORT).show();
                getHuaweiCloudToken();
            });
            return;
        }

        // 3. 构建命令参数（精准匹配华为云）
        String commandName;
        String paramKey;
        switch (deviceType) {
            case "light_1":
                commandName = CMD_LIGHT;
                paramKey = PARAM_LIGHT;
                break;
            case "board_1":
                commandName = CMD_BOARD;
                paramKey = PARAM_BOARD;
                break;
            case "fan_1":
                commandName = CMD_FAN_GEAR;
                paramKey = PARAM_FAN;
                // 确保风扇档位在0-9范围内
                commandValue = Math.max(0, Math.min(9, (Integer) commandValue));
                break;
            case "water_pump_1":
            case "medicine_pump_1":
                commandName = CMD_BUMP;
                paramKey = PARAM_BUMP;
                break;
            default:
                runOnUiThread(() -> Toast.makeText(this, "不支持的设备类型", Toast.LENGTH_SHORT).show());
                return;
        }

        // 4. 构建华为云标准命令格式
        JSONObject commandJson = new JSONObject();
        commandJson.put("service_id", SERVICE_ID);
        commandJson.put("command_name", commandName);
        commandJson.put("expire_time", 30);

        JSONObject paras = new JSONObject();
        paras.put(paramKey, commandValue);
        commandJson.put("paras", paras);

        // 5. 发送命令请求
        MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json;charset=UTF-8");
        RequestBody body = RequestBody.create(JSON_MEDIA_TYPE, commandJson.toString());

        String commandUrl = IOTDA_ENDPOINT + "/v5/iot/" + PROJECT_ID + "/devices/" + DEVICE_ID + "/commands";
        Request request = new Request.Builder()
                .url(commandUrl)
                .post(body)
                .addHeader("Content-Type", "application/json;charset=UTF-8")
                .addHeader("X-Auth-Token", AToken)
                .build();

        Log.d(TAG, "下发命令：" + commandJson.toString());
        Log.d(TAG, "命令URL：" + commandUrl);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "命令下发失败：" + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(DeviceMonitorActivity.this, "命令下发失败：网络异常", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        Log.d(TAG, "命令下发成功，响应：" + responseBody);

                        try {
                            JSONObject responseJson = com.alibaba.fastjson.JSON.parseObject(responseBody);
                            JSONObject responseObj = responseJson.getJSONObject("response");
                            if (responseObj != null) {
                                int resultCode = responseObj.getIntValue("result_code");
                                String result = responseObj.getJSONObject("paras").getString("result");

                                if (resultCode == 0 && "success".equalsIgnoreCase(result)) {
                                    runOnUiThread(successCallback);
                                    // 命令下发成功后，立即拉取设备影子数据更新状态
                                    if (currentGreenhouseId != null && !currentGreenhouseId.isEmpty()) {
                                        pullHuaweiCloudData(currentGreenhouseId);
                                    }
                                } else {
                                    runOnUiThread(() -> {
                                        Toast.makeText(DeviceMonitorActivity.this,
                                                "设备执行命令失败：" + result + "（错误码：" + resultCode + "）",
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }
                            } else {
                                runOnUiThread(successCallback);
                                // 命令下发成功后，立即拉取设备影子数据更新状态
                                if (currentGreenhouseId != null && !currentGreenhouseId.isEmpty()) {
                                    pullHuaweiCloudData(currentGreenhouseId);
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "解析响应体失败，按成功处理", e);
                            runOnUiThread(successCallback);
                            // 命令下发成功后，立即拉取设备影子数据更新状态
                            if (currentGreenhouseId != null && !currentGreenhouseId.isEmpty()) {
                                pullHuaweiCloudData(currentGreenhouseId);
                            }
                        }
                    } else {
                        Log.e(TAG, "命令下发失败，响应码：" + response.code());
                        runOnUiThread(() -> {
                            String toastMsg;
                            if (response.code() == 403) {
                                toastMsg = "下发失败：权限不足，请检查IoTDA订阅和用户权限";
                            } else if (response.code() == 404) {
                                toastMsg = "下发失败：设备ID或项目ID错误";
                            } else if (response.code() == 401) {
                                toastMsg = "下发失败：Token过期，正在重新获取";
                                getHuaweiCloudToken();
                            } else {
                                toastMsg = "命令下发失败";
                            }
                            Toast.makeText(DeviceMonitorActivity.this, toastMsg, Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析响应失败：" + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(DeviceMonitorActivity.this, "命令下发失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // 同步设备状态到管理页面存储
    private void syncDeviceStatusToManagePage(String greenhouseId, String lightStatus, String windBoardStatus, String windSpeed) {
        try {
            // 获取管理页面的设备状态
            JSONObject deviceStatus = managePageStorage.getDeviceStatus();
            if (deviceStatus == null || !deviceStatus.containsKey("devices")) {
                // 如果没有设备数据，创建默认设备列表
                JSONArray deviceArray = new JSONArray();
                
                // 添加灯设备
                JSONObject lightDevice = new JSONObject();
                lightDevice.put("type", "控制设备");
                lightDevice.put("name", "灯");
                lightDevice.put("status", "开".equals(lightStatus));
                lightDevice.put("deviceId", "light_1");
                deviceArray.add(lightDevice);
                
                // 添加挡光板设备
                JSONObject boardDevice = new JSONObject();
                boardDevice.put("type", "控制设备");
                boardDevice.put("name", "挡光板");
                boardDevice.put("status", "开".equals(windBoardStatus));
                boardDevice.put("deviceId", "board_1");
                deviceArray.add(boardDevice);
                
                // 添加风机设备
                JSONObject fanDevice = new JSONObject();
                fanDevice.put("type", "控制设备");
                fanDevice.put("name", "风机");
                fanDevice.put("status", !"0".equals(windSpeed));
                fanDevice.put("deviceId", "fan_1");
                deviceArray.add(fanDevice);
                
                // 添加水泵设备（状态由大棚页面按钮控制）
                JSONObject waterPumpDevice = new JSONObject();
                waterPumpDevice.put("type", "控制设备");
                waterPumpDevice.put("name", "水泵");
                waterPumpDevice.put("status", false);
                waterPumpDevice.put("deviceId", "water_pump_1");
                deviceArray.add(waterPumpDevice);
                
                // 添加药泵设备（状态由大棚页面按钮控制）
                JSONObject medicinePumpDevice = new JSONObject();
                medicinePumpDevice.put("type", "控制设备");
                medicinePumpDevice.put("name", "药泵");
                medicinePumpDevice.put("status", false);
                medicinePumpDevice.put("deviceId", "medicine_pump_1");
                deviceArray.add(medicinePumpDevice);
                
                deviceStatus = new JSONObject();
                deviceStatus.put("greenhouseId", greenhouseId);
                deviceStatus.put("devices", deviceArray);
            } else {
                // 更新现有设备状态
                JSONArray deviceArray = deviceStatus.getJSONArray("devices");
                for (int i = 0; i < deviceArray.size(); i++) {
                    JSONObject device = deviceArray.getJSONObject(i);
                    String deviceName = device.getString("name");
                    if ("灯".equals(deviceName)) {
                        device.put("status", "开".equals(lightStatus));
                    } else if ("挡光板".equals(deviceName)) {
                        device.put("status", "开".equals(windBoardStatus));
                    } else if ("风机".equals(deviceName)) {
                        device.put("status", !"0".equals(windSpeed));
                    }
                    // 水泵和药泵状态由大棚页面按钮控制，不在这里更新
                }
            }
            
            // 保存到管理页面存储
            managePageStorage.saveDeviceStatus(deviceStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取华为云Token（与GreenhouseHomePage保持一致）
     */
    private void getHuaweiCloudToken() {
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");

        RequestBody body = RequestBody.create(mediaType, "{ \n" +
                "    \"auth\": { \n" +
                "        \"identity\": { \n" +
                "            \"methods\": [ \n" +
                "                \"password\" \n" +
                "            ], \n" +
                "            \"password\": { \n" +
                "                \"user\": { \n" +
                "                    \"name\": \"" + HUAWEI_CLOUD_USER + "\", \n" +
                "                    \"password\": \"" + HUAWEI_CLOUD_PWD + "\", \n" +
                "                    \"domain\": { \n" +
                "                        \"name\": \"" + HUAWEI_CLOUD_DOMAIN + "\" \n" +
                "                    } \n" +
                "                } \n" +
                "            } \n" +
                "        }, \n" +
                "        \"scope\": { \n" +
                "            \"project\": { \n" +
                "                \"name\": \"" + HUAWEI_CLOUD_PROJECT_NAME + "\" \n" +
                "            } \n" +
                "        } \n" +
                "    } \n" +
                "}");

        Request request = new Request.Builder()
                .url(IAM_ENDPOINT)
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "鉴权失败：" + e.getMessage());
                runOnUiThread(() -> Toast.makeText(DeviceMonitorActivity.this,
                        "华为云鉴权失败：" + e.getMessage(), Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    AToken = response.header("X-Subject-Token");
                    Log.d(TAG, "获取Token成功：" + AToken);
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * 拉取华为云设备影子数据（与GreenhouseHomePage保持一致）
     */
    private void pullHuaweiCloudData(String greenhouseId) {
        if (TextUtils.isEmpty(AToken)) {
            Log.w(TAG, "Token为空，跳过华为云数据拉取");
            return;
        }

        String url = IOTDA_ENDPOINT + "/v5/iot/" + PROJECT_ID + "/devices/" + DEVICE_ID + "/shadow";
        Request dataRequest = new Request.Builder()
                .url(url)
                .get()
                .addHeader("Content-Type", "application/json")
                .addHeader("X-Auth-Token", AToken)
                .build();

        client.newCall(dataRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "华为云数据拉取失败：" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "华为云设备影子数据：" + responseBody);

                        JSONObject object = com.alibaba.fastjson.JSON.parseObject(responseBody);
                        boolean isDataValid = false;

                        String lightState = STATUS_OFF;
                        String boardState = STATUS_OFF;
                        int fanGear = 0;
                        String airTemp = "0";

                        if (object != null) {
                            JSONObject properties = object.getJSONObject("properties");
                            if (properties != null) {
                                isDataValid = true;
                                airTemp = processSensorData(properties.getString("Temp"));

                                if (properties.containsKey("fengdegree")) {
                                    fanGear = properties.getInteger("fengdegree") == null ? 0 : properties.getInteger("fengdegree");
                                }

                                lightState = properties.getString("light") == null ? STATUS_OFF : properties.getString("light");
                                boardState = properties.getString("board") == null ? STATUS_OFF : properties.getString("board");
                            } else if (object.containsKey("shadow")) {
                                com.alibaba.fastjson.JSONArray shadows = object.getJSONArray("shadow");
                                if (shadows != null && shadows.size() > 0) {
                                    JSONObject service = shadows.getJSONObject(0);
                                    JSONObject reportedObj = service.getJSONObject("reported");
                                    if (reportedObj != null) {
                                        JSONObject propertiesObj = reportedObj.getJSONObject("properties");
                                        if (propertiesObj != null) {
                                            isDataValid = true;
                                            airTemp = processSensorData(propertiesObj.getString("Temp"));

                                            if (propertiesObj.containsKey("fengdegree")) {
                                                fanGear = propertiesObj.getInteger("fengdegree") == null ? 0 : propertiesObj.getInteger("fengdegree");
                                            }

                                            lightState = propertiesObj.getString("light") == null ? STATUS_OFF : propertiesObj.getString("light");
                                            boardState = propertiesObj.getString("board") == null ? STATUS_OFF : propertiesObj.getString("board");
                                        }
                                    }
                                }
                            }
                        }

                        if (isDataValid) {
                            String finalLightState = lightState;
                            String finalBoardState = boardState;
                            int finalFanGear = fanGear;
                            String finalAirTemp = airTemp;
                            runOnUiThread(() -> {
                                JSONObject environmentData = new JSONObject();
                                environmentData.put("lightStatus", STATUS_ON.equals(finalLightState) ? "开" : "关");
                                environmentData.put("windBoardStatus", STATUS_ON.equals(finalBoardState) ? "开" : "关");
                                environmentData.put("windSpeed", String.valueOf(finalFanGear));
                                environmentData.put("temperature", finalAirTemp);
                                
                                // 水泵和药泵状态从环境数据中获取（由大棚页面按钮控制）
                                try {
                                    JSONObject existingData = greenhouseHomeStorage.getEnvironmentData(greenhouseId);
                                    if (existingData != null) {
                                        String waterPumpStatus = existingData.getString("waterPumpStatus");
                                        String medicinePumpStatus = existingData.getString("medicinePumpStatus");
                                        environmentData.put("waterPumpStatus", waterPumpStatus != null ? waterPumpStatus : "关");
                                        environmentData.put("medicinePumpStatus", medicinePumpStatus != null ? medicinePumpStatus : "关");
                                    } else {
                                        environmentData.put("waterPumpStatus", "关");
                                        environmentData.put("medicinePumpStatus", "关");
                                    }
                                } catch (Exception e) {
                                    environmentData.put("waterPumpStatus", "关");
                                    environmentData.put("medicinePumpStatus", "关");
                                }
                                
                                greenhouseHomeStorage.saveEnvironmentData(greenhouseId, environmentData);
                                updateDeviceStatusDisplay(environmentData);
                            });
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析华为云数据失败", e);
                    } finally {
                        response.body().close();
                    }
                }
            }
        });
    }

    private String processSensorData(String data) {
        if (data == null) {
            return "0";
        }
        String trimmedData = data.trim();
        return trimmedData.isEmpty() ? "0" : trimmedData;
    }
}


