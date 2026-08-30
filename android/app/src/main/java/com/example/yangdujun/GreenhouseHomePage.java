package com.example.yangdujun;

import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.model.ApiResponse;
import com.example.yangdujun.network.ApiService;
import com.example.yangdujun.utils.AlarmRecordStorageUtils;
import com.example.yangdujun.utils.GreenhouseHomeStorage;
import com.example.yangdujun.utils.GreenhouseStorage;
import com.example.yangdujun.utils.HistoricalDataStorageUtils;
import com.example.yangdujun.utils.JsonUtils;
import com.example.yangdujun.utils.ManagePageStorage;
import com.example.yangdujun.utils.RealtimeDataStorageUtils;
import com.example.yangdujun.utils.ThresholdStorageUtils;
import com.example.yangdujun.utils.UserStorage;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GreenhouseHomePage extends AppCompatActivity {

    private static final String TAG = "GreenhouseHomePage";
    private static final String DEEPSEEK_API_KEY = BuildConfig.DEEPSEEK_API_KEY;
    private static final String DEEPSEEK_CHAT_URL = "https://api.deepseek.com/v1/chat/completions";

    // 网络请求客户端
    private OkHttpClient client;
    // 华为云鉴权令牌
    private String AToken;
    // 用户token
    private String userToken;
    // 11项传感数据文本控件
    private TextView tv_tempdata, tv_humity, tv_Lumi, tv_Fengd, tv_LightSt, tv_DangGuangBan,
            tv_Soil_Humi, tv_Soil_Temp, tv_CO2, tv_pH;
    private TextView tv_Bump, tv_AIWarning, tv_medicine; // 新增：tv_medicine控件
    // 控制控件
    private ToggleButton tbDangGuangBan, tbLight, onHealthy, onBump; // 新增：两个水泵/药泵按钮
    private Button btnUpFengD, btnDownFengD, btnGetAiSuggestion;
    private boolean isRequestingAiSuggestion = false;
    private BottomSheetDialog aiLoadingDialog;
    // 内容区域容器
    private LinearLayout layoutContent;
    // 定时器对象
    private Timer mDataTimer;       // 原有接口数据定时器（5秒）
    private Timer mDevicePullTimer; // 华为云设备数据拉取定时器（1秒）
    // 大棚列表
    private List<String> greenhouseList = new ArrayList<>();
    private List<String> greenhouseIdList = new ArrayList<>();
    // 存储工具
    private GreenhouseHomeStorage greenhouseHomeStorage;
    private ManagePageStorage managePageStorage;
    // 防重复点击标记
    private long lastCommandTime = 0;
    private static final long COMMAND_INTERVAL = 2000;
    // 震动器
    private Vibrator vibrator;

    // ========== 新增：生长阶段相关定义 ==========
    // 生长阶段名称映射（对应State数值）
    private static final Map<Integer, String> STAGE_NAME_MAP = new HashMap<>();
    // 生长阶段图片资源映射（激活/未激活）
    private static final Map<Integer, Integer> STAGE_ACTIVE_IMG_MAP = new HashMap<>();
    private static final Map<Integer, Integer> STAGE_INACTIVE_IMG_MAP = new HashMap<>();

    static {
        // 初始化阶段名称
        STAGE_NAME_MAP.put(1, "催菇诱导期");
        STAGE_NAME_MAP.put(2, "原基形成期");
        STAGE_NAME_MAP.put(3, "子实体生长期");
        STAGE_NAME_MAP.put(4, "成熟期");

        // 初始化激活态图片（深色）
        STAGE_ACTIVE_IMG_MAP.put(1, R.drawable.deep_image_1);
        STAGE_ACTIVE_IMG_MAP.put(2, R.drawable.deep_image_2);
        STAGE_ACTIVE_IMG_MAP.put(3, R.drawable.deep_image_3);
        STAGE_ACTIVE_IMG_MAP.put(4, R.drawable.deep_image_4);

        // 初始化未激活态图片（浅色）
        STAGE_INACTIVE_IMG_MAP.put(1, R.drawable.light_image_1);
        STAGE_INACTIVE_IMG_MAP.put(2, R.drawable.light_image_2);
        STAGE_INACTIVE_IMG_MAP.put(3, R.drawable.light_image_3);
        STAGE_INACTIVE_IMG_MAP.put(4, R.drawable.light_image_4);
    }

    // ========== 新增：生长阶段控件 ==========
    private ImageView iv_stage_1, iv_stage_2, iv_stage_3, iv_stage_4;
    private TextView tv_growth_progress;

    // 华为云配置
    private static final String DEVICE_ID = BuildConfig.HUAWEI_DEVICE_ID;
    private static final String PROJECT_ID = BuildConfig.HUAWEI_PROJECT_ID;
    private static final String SERVICE_ID = "smartlan";
    // 华为云命令名称&参数名（与产品模型完全一致）
    private static final String CMD_FAN_GEAR = "Fengdegree";
    private static final String CMD_LIGHT = "Light";
    private static final String CMD_BOARD = "Board";
    private static final String CMD_BUMP = "BUMP"; // 新增：水泵命令名称
    private static final String PARAM_FAN = "fengdegree";
    private static final String PARAM_LIGHT = "light";
    private static final String PARAM_BOARD = "board";
    private static final String PARAM_BUMP = "bump"; // 新增：水泵参数名
    // 华为云状态值
    private static final String STATUS_ON = "ON";
    private static final String STATUS_OFF = "OFF";

    // 华为云Endpoint
    private static final String IOTDA_ENDPOINT = BuildConfig.HUAWEI_IOTDA_ENDPOINT;
    private static final String IAM_ENDPOINT = BuildConfig.HUAWEI_IAM_ENDPOINT;

    // 华为云账号配置
    private static final String HUAWEI_CLOUD_USER = BuildConfig.HUAWEI_CLOUD_USER;
    private static final String HUAWEI_CLOUD_PWD = BuildConfig.HUAWEI_CLOUD_PWD;
    private static final String HUAWEI_CLOUD_DOMAIN = BuildConfig.HUAWEI_CLOUD_DOMAIN;
    private static final String HUAWEI_CLOUD_PROJECT_NAME = BuildConfig.HUAWEI_CLOUD_PROJECT_NAME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.greenhouse_homepage);

        // 初始化震动器
        vibrator = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        // 初始化内容区域容器
        layoutContent = findViewById(R.id.layout_content);
        layoutContent.setVisibility(View.GONE);

        // 初始化底部导航
        BottomNavHelper.initBottomNav(this, R.id.ll_nav_greenhouse);

        // 获取用户token
        UserStorage userStorage = new UserStorage();
        userToken = userStorage.getToken();
        if (userToken == null || userToken.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        // 初始化存储工具
        greenhouseHomeStorage = new GreenhouseHomeStorage(this);
        managePageStorage = new ManagePageStorage(this);

        // ========== 绑定控件 ==========
        // 传感器数据显示控件
        tv_tempdata = findViewById(R.id.tv_tempdata);
        tv_humity = findViewById(R.id.tv_humity);
        tv_Lumi = findViewById(R.id.tv_Lumi);
        tv_Fengd = findViewById(R.id.tv_Fengd);
        tv_LightSt = findViewById(R.id.LightSt);
        tv_DangGuangBan = findViewById(R.id.DangGuangBan);
        tv_Soil_Humi = findViewById(R.id.tv_Soil_Humi);
        tv_Soil_Temp = findViewById(R.id.tv_Soil_Temp);
        tv_CO2 = findViewById(R.id.tv_CO2);
        tv_pH = findViewById(R.id.tv_pH);
        tv_Bump = findViewById(R.id.tv_Bump);       // 水泵状态文本
        tv_medicine = findViewById(R.id.tv_medicine); // 药泵状态文本
        tv_AIWarning = findViewById(R.id.tv_AIWarning); // 健康状态文本

        // ========== 新增：绑定生长阶段控件 ==========
        iv_stage_1 = findViewById(R.id.iv_stage_1);
        iv_stage_2 = findViewById(R.id.iv_stage_2);
        iv_stage_3 = findViewById(R.id.iv_stage_3);
        iv_stage_4 = findViewById(R.id.iv_stage_4);
        tv_growth_progress = findViewById(R.id.tv_growth_progress);

        // 控制控件
        tbDangGuangBan = findViewById(R.id.onDangFengBan);
        tbLight = findViewById(R.id.onLight);
        onHealthy = findViewById(R.id.onHealthy); // 药泵按钮
        onBump = findViewById(R.id.onBump);       // 水泵按钮
        btnUpFengD = findViewById(R.id.upFengD);
        btnDownFengD = findViewById(R.id.downFengD);
        btnGetAiSuggestion = findViewById(R.id.btn_get_ai_suggestion);

        // 视频按钮点击事件
        Button video1 = findViewById(R.id.video1);
        video1.setOnClickListener(v -> {
            Intent intent = new Intent(GreenhouseHomePage.this, VideoMain.class);
            startActivity(intent);
            finish();
        });
        Button video2 = findViewById(R.id.video2);
        video2.setOnClickListener(v -> {
            Intent intent = new Intent(GreenhouseHomePage.this, VideoMain.class);
            startActivity(intent);
            finish();
        });
        Button video3 = findViewById(R.id.video3);
        video3.setOnClickListener(v -> {
            Intent intent = new Intent(GreenhouseHomePage.this, VideoMain.class);
            startActivity(intent);
            finish();
        });
        Button video4 = findViewById(R.id.video4);
        video4.setOnClickListener(v -> {
            Intent intent = new Intent(GreenhouseHomePage.this, VideoMain.class);
            startActivity(intent);
            finish();
        });
        Button video5 = findViewById(R.id.video5);
        video5.setOnClickListener(v -> {
            Intent intent = new Intent(GreenhouseHomePage.this, VideoMain.class);
            startActivity(intent);
            finish();
        });

        // ========== 新增：药泵按钮点击事件 ==========
        onHealthy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!buttonView.isPressed()) return; // 防止非用户操作触发

            // 修正：按钮开（isChecked=true）显示ON，按钮关显示OFF
            String status = isChecked ? STATUS_ON : STATUS_OFF;
            tv_medicine.setText(status);

            // 2. 发送BUMP命令到华为云
            sendCommandToHuaweiIoT("BUMP", status, () -> {
                runOnUiThread(() -> {
                    Toast.makeText(GreenhouseHomePage.this,
                            isChecked ? "药泵已开启" : "药泵已关闭",
                            Toast.LENGTH_SHORT).show();
                });
            });
        });

// ========== 新增：水泵按钮点击事件 ==========
        onBump.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!buttonView.isPressed()) return; // 防止非用户操作触发

            // 修正：按钮开（isChecked=true）显示ON，按钮关显示OFF
            String status = isChecked ? STATUS_ON : STATUS_OFF;
            tv_Bump.setText(status);

            // 2. 发送BUMP命令到华为云
            sendCommandToHuaweiIoT("BUMP", status, () -> {
                runOnUiThread(() -> {
                    Toast.makeText(GreenhouseHomePage.this,
                            isChecked ? "水泵已开启" : "水泵已关闭",
                            Toast.LENGTH_SHORT).show();
                });
            });
        });

        // ========== 设置控制控件点击事件 ==========
        // 挡光板开关
        tbDangGuangBan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!buttonView.isPressed()) return;
            String commandValue = isChecked ? STATUS_ON : STATUS_OFF;
            sendCommandToHuaweiIoT("DangGuangBan", commandValue, () -> {
                runOnUiThread(() -> {
                    tv_DangGuangBan.setText(commandValue);
                    Toast.makeText(GreenhouseHomePage.this,
                            isChecked ? "下发开挡光板命令" : "下发关挡光板命令",
                            Toast.LENGTH_SHORT).show();
                });
            });
        });

        // 灯泡开关
        tbLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!buttonView.isPressed()) return;
            String commandValue = isChecked ? STATUS_ON : STATUS_OFF;
            sendCommandToHuaweiIoT("LightSt", commandValue, () -> {
                runOnUiThread(() -> {
                    tv_LightSt.setText(commandValue);
                    Toast.makeText(GreenhouseHomePage.this,
                            isChecked ? "下发开大灯命令" : "下发关大灯命令",
                            Toast.LENGTH_SHORT).show();
                });
            });
        });

        // 调高风速（0-9档逻辑）
        btnUpFengD.setOnClickListener(v -> {
            try {
                int currentGear = TextUtils.isEmpty(tv_Fengd.getText().toString()) ? 0 : Integer.parseInt(tv_Fengd.getText().toString());
                int newGear = Math.min(currentGear + 1, 9);

                // 优化提示文案
                String tip = newGear == 0 ? "风扇已开启（1档）" : "调高风速至：" + newGear + "档";
                Toast.makeText(GreenhouseHomePage.this, tip, Toast.LENGTH_SHORT).show();

                sendCommandToHuaweiIoT("Fengd", newGear, () -> {
                    runOnUiThread(() -> tv_Fengd.setText(String.valueOf(newGear)));
                });
            } catch (NumberFormatException e) {
                Toast.makeText(this, "档位数据异常，默认设置为1档", Toast.LENGTH_SHORT).show();
                sendCommandToHuaweiIoT("Fengd", 1, () -> {
                    runOnUiThread(() -> tv_Fengd.setText("1"));
                });
            }
        });

        // 调低风速（0-9档逻辑）
        btnDownFengD.setOnClickListener(v -> {
            try {
                int currentGear = TextUtils.isEmpty(tv_Fengd.getText().toString()) ? 0 : Integer.parseInt(tv_Fengd.getText().toString());
                int newGear = Math.max(currentGear - 1, 0);

                // 优化提示文案
                String tip = newGear == 0 ? "风扇已关闭（0档）" : "调低风速至：" + newGear + "档";
                Toast.makeText(GreenhouseHomePage.this, tip, Toast.LENGTH_SHORT).show();

                sendCommandToHuaweiIoT("Fengd", newGear, () -> {
                    runOnUiThread(() -> tv_Fengd.setText(String.valueOf(newGear)));
                });
            } catch (NumberFormatException e) {
                Toast.makeText(this, "档位数据异常，默认关闭风扇（0档）", Toast.LENGTH_SHORT).show();
                sendCommandToHuaweiIoT("Fengd", 0, () -> {
                    runOnUiThread(() -> tv_Fengd.setText("0"));
                });
            }
        });

        btnGetAiSuggestion.setOnClickListener(v -> requestAiSuggestion());

        // ========== 大棚选择Spinner ==========
        Spinner spinner = findViewById(R.id.spinner_greenhouse);
        // 初始化大棚列表
        initGreenhouseList();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, greenhouseList);
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // 设置默认选中大棚
        int lastSelectedIndex = greenhouseHomeStorage.getLastSelectedIndex();
        int defaultIndex = 1;
        if (lastSelectedIndex > 0 && lastSelectedIndex < greenhouseList.size()) {
            defaultIndex = lastSelectedIndex;
        }

        // Spinner选择监听
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                greenhouseHomeStorage.saveLastSelectedIndex(position);

                if (position == 0) {
                    // 选择"请选择大棚"，清空显示
                    clearAllSensorData();
                    layoutContent.setVisibility(View.GONE);
                } else if (position > 0 && position < greenhouseIdList.size()) {
                    String greenhouseId = greenhouseIdList.get(position);
                    String greenhouseName = greenhouseList.get(position);
                    Toast.makeText(GreenhouseHomePage.this, "已选择：" + greenhouseName, Toast.LENGTH_SHORT).show();
                    layoutContent.setVisibility(View.VISIBLE);

                    // 1. 优先拉取华为云数据
                    if (!TextUtils.isEmpty(AToken)) {
                        // 使用新的拉取逻辑
                        pullDeviceShadowImmediately(greenhouseId);
                    } else {
                        // Token为空时直接置为默认值（0/OFF）
                        setSensorDefaultValue();
                        // 补充拉取原有接口数据
                        getEnvironmentData(greenhouseId);
                        // 从本地存储加载保底数据
                        loadSensorDataFromStorage(greenhouseId);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                layoutContent.setVisibility(View.GONE);
            }
        });

        // 设置默认选中（注意：setSelection不会触发onItemSelected，需要手动触发）
        spinner.setSelection(defaultIndex, false);
        greenhouseHomeStorage.saveLastSelectedIndex(defaultIndex);

        // 手动触发默认大棚的数据加载
        if (defaultIndex > 0 && defaultIndex < greenhouseIdList.size()) {
            String greenhouseId = greenhouseIdList.get(defaultIndex);
            String greenhouseName = greenhouseList.get(defaultIndex);
            layoutContent.setVisibility(View.VISIBLE);

            // 1. 优先拉取华为云数据
            if (!TextUtils.isEmpty(AToken)) {
                // 使用新的拉取逻辑
                pullDeviceShadowImmediately(greenhouseId);
            } else {
                // Token为空时直接置为默认值（0/OFF）
                setSensorDefaultValue();
                // 补充拉取原有接口数据
                getEnvironmentData(greenhouseId);
                // 从本地存储加载保底数据
                loadSensorDataFromStorage(greenhouseId);
            }
        }

        // ========== 初始化网络请求客户端 ==========
        client = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // ========== 华为云鉴权（替换为新代码逻辑） ==========
        getHuaweiCloudToken();

        // ========== 设置初始状态 ==========
        setSensorDefaultValue();

        // ========== 启动定时器 ==========
        startDataTimer(); // 原有接口数据定时器（5秒）
    }

    /**
     * 新增：根据State值更新生长阶段显示
     * @param stateValue State数值（1/2/3/4）
     */
    private void updateGrowthStageDisplay(int stateValue) {
        // 1. 校验State值范围（1-4）
        int currentStage = Math.max(1, Math.min(4, stateValue));

        // 2. 更新图片显示（当前及之前阶段为深色，之后为浅色）
        // 阶段1：催菇诱导期
        iv_stage_1.setImageResource(currentStage >= 1 ? STAGE_ACTIVE_IMG_MAP.get(1) : STAGE_INACTIVE_IMG_MAP.get(1));
        // 阶段2：原基形成期
        iv_stage_2.setImageResource(currentStage >= 2 ? STAGE_ACTIVE_IMG_MAP.get(2) : STAGE_INACTIVE_IMG_MAP.get(2));
        // 阶段3：子实体生长期
        iv_stage_3.setImageResource(currentStage >= 3 ? STAGE_ACTIVE_IMG_MAP.get(3) : STAGE_INACTIVE_IMG_MAP.get(3));
        // 阶段4：成熟期
        iv_stage_4.setImageResource(currentStage >= 4 ? STAGE_ACTIVE_IMG_MAP.get(4) : STAGE_INACTIVE_IMG_MAP.get(4));

        // 3. 更新进度文本
        String currentStageName = STAGE_NAME_MAP.getOrDefault(currentStage, "未知阶段");
        tv_growth_progress.setText("当前进度：正在" + currentStageName);

        Log.d(TAG, "生长阶段更新：State=" + stateValue + "，当前阶段=" + currentStageName);
    }

    /**
     * 新增：统一处理传感器数据，空值/空字符串转为"0"，非空则去空格
     */
    private String processSensorData(String data) {
        if (data == null) {
            return "0";
        }
        String trimmedData = data.trim();
        return trimmedData.isEmpty() ? "0" : trimmedData;
    }

    /**
     * 新增：安全转换Float，防止空值/非数字转换异常
     */
    private float safeParseFloat(String value) {
        try {
            String processedValue = processSensorData(value);
            return Float.parseFloat(processedValue);
        } catch (NumberFormatException e) {
            Log.w(TAG, "数据转换失败，使用默认值0：" + value, e);
            return 0.0f;
        }
    }

    /**
     * 替换：使用新代码的有效鉴权逻辑获取华为云Token
     */
    private void getHuaweiCloudToken() {
        MediaType mediaType = MediaType.parse("application/json");

        // 华为云鉴权请求体
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

        // 发起鉴权请求获取Token
        Request request = new Request.Builder()
                .url(IAM_ENDPOINT)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "鉴权失败："+e.getMessage());
                runOnUiThread(() -> Toast.makeText(GreenhouseHomePage.this,
                        "华为云鉴权失败：" + e.getMessage(), Toast.LENGTH_LONG).show());

                // 增加重试机制
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(() -> getHuaweiCloudToken());
                    }
                }, 5000);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 增加try-finally确保响应体关闭
                try {
                    AToken = response.header("X-Subject-Token");
                    Log.d(TAG, "获取Token成功：" + AToken);

                    // Token获取成功后，启动设备数据拉取
                    startDeviceDataPull();

                    // 立即拉取一次数据
                    Spinner spinner = findViewById(R.id.spinner_greenhouse);
                    int position = spinner.getSelectedItemPosition();
                    if (position > 0) {
                        String greenhouseId = greenhouseIdList.get(position);
                        pullDeviceShadowImmediately(greenhouseId);
                    }
                } finally {
                    // 必须关闭响应体，防止连接泄漏
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * 替换：重构华为云设备数据拉取逻辑（使用新代码核心逻辑）
     */
    private void startDeviceDataPull(){
        // 先停止旧的定时器，防止重复
        if (mDevicePullTimer != null) {
            mDevicePullTimer.cancel();
            mDevicePullTimer = null;
        }

        // 定时1秒拉取一次物联网设备数据
        mDevicePullTimer = new Timer();
        mDevicePullTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (TextUtils.isEmpty(AToken)) {
                    Log.w(TAG, "Token为空，跳过数据拉取");
                    return;
                }

                // 使用新代码的有效设备影子URL
                Request dataRequest = new Request.Builder()
                        .url(IOTDA_ENDPOINT + "/v5/iot/" + PROJECT_ID + "/devices/" + DEVICE_ID + "/shadow")
                        .method("GET", null)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("X-Auth-Token", AToken)
                        .build();

                client.newCall(dataRequest).enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "拉取传感器数据失败：" + e.getMessage());
                        // 拉取失败时，使用原有数据补充
                        Spinner spinner = findViewById(R.id.spinner_greenhouse);
                        int position = spinner.getSelectedItemPosition();
                        if (position > 0) {
                            String greenhouseId = greenhouseIdList.get(position);
                            getEnvironmentData(greenhouseId);
                        }
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful() && response.body() != null){
                            // 检查Activity是否已销毁
                            if (isFinishing() || isDestroyed()) {
                                response.body().close();
                                return;
                            }
                            
                            // 检查是否选择了大棚
                            Spinner spinner1 = findViewById(R.id.spinner_greenhouse);
                            if (spinner1 == null) {
                                Log.e(TAG, "spinner_greenhouse为null");
                                response.body().close();
                                return;
                            }
                            
                            int selectedPosition = spinner1.getSelectedItemPosition();
                            Log.d(TAG, "华为云设备影子响应: selectedPosition=" + selectedPosition + ", greenhouseIdList.size()=" + greenhouseIdList.size());
                            if (selectedPosition == 0) {
                                Log.d(TAG, "未选择大棚，跳过数据处理");
                                response.body().close();
                                return;
                            }
                            
                            // 检查greenhouseIdList是否为空且索引是否有效
                            if (greenhouseIdList == null || greenhouseIdList.isEmpty() || 
                                selectedPosition < 0 || selectedPosition >= greenhouseIdList.size()) {
                                Log.e(TAG, "greenhouseIdList为空或索引越界");
                                response.body().close();
                                return;
                            }

                            try {
                                // 1. 读取完整响应体并打印日志
                                String responseBody = response.body().string();
                                Log.d(TAG, "华为云设备影子完整数据：" + responseBody);

                                // 2. 解析JSON
                                JSONObject object = JSON.parseObject(responseBody);

                                // 3. 初始化默认值
                                String airTemp = "0";
                                String airHumi = "0";
                                String light = "0";
                                int fanGear = 0;
                                String lightState = STATUS_OFF; // 默认OFF
                                String boardState = STATUS_OFF; // 默认OFF
                                String soilTemp = "0";
                                String soilHumi = "0";
                                String co2 = "0";
                                String o2 = "0";
                                String ph = "0";

                                // ========== 新增：初始化State值 ==========
                                int stateValue = 0;
                                String bumpStatus = "未知";       // 水泵状态默认值
                                String aiWarning = "未知";      // AI预警默认值
                                // 4. 解析设备影子数据 - 兼容多种JSON结构
                                if (object != null) {
                                    // 方式1：直接从properties读取（最常见）
                                    JSONObject properties = object.getJSONObject("properties");
                                    if (properties != null) {
                                        Log.d(TAG, "从根节点properties读取数据");
                                        airTemp = processSensorData(properties.getString("Temp"));
                                        airHumi = processSensorData(properties.getString("Humi"));
                                        light = processSensorData(properties.getString("Lumi"));
                                        // 新增：解析水泵状态和AI预警
                                        bumpStatus = processSensorData(properties.getString("Bump"));
                                        aiWarning = processSensorData(properties.getString("AIWarning"));
                                        // 空值兜底
                                        bumpStatus = TextUtils.isEmpty(bumpStatus) ? "未知" : bumpStatus;
                                        aiWarning = TextUtils.isEmpty(aiWarning) ? "未知" : aiWarning;

                                        // 风扇档位 - 兼容多种字段名
                                        if (properties.containsKey("fengdegree")) {
                                            fanGear = properties.getInteger("fengdegree") == null ? 0 : properties.getInteger("fengdegree");
                                        } else if (properties.containsKey("Fengd")) {
                                            fanGear = properties.getInteger("Fengd") == null ? 0 : properties.getInteger("Fengd");
                                        } else if (properties.containsKey("fanGear")) {
                                            fanGear = properties.getInteger("fanGear") == null ? 0 : properties.getInteger("fanGear");
                                        }

                                        // 灯光状态 - 直接使用华为云原始值
                                        if (properties.containsKey("light")) {
                                            lightState = properties.getString("light");
                                        } else if (properties.containsKey("LightSt")) {
                                            lightState = properties.getString("LightSt");
                                        }
                                        // 空值兜底
                                        lightState = TextUtils.isEmpty(lightState) ? STATUS_OFF : lightState;

                                        // 挡光板状态 - 直接使用华为云原始值
                                        if (properties.containsKey("board")) {
                                            boardState = properties.getString("board");
                                        } else if (properties.containsKey("DangGuangBan")) {
                                            boardState = properties.getString("DangGuangBan");
                                        } else if (properties.containsKey("windBoardStatus")) {
                                            boardState = properties.getString("windBoardStatus");
                                        }
                                        // 空值兜底
                                        boardState = TextUtils.isEmpty(boardState) ? STATUS_OFF : boardState;

                                        soilTemp = processSensorData(properties.getString("Soil_Temp"));
                                        soilHumi = processSensorData(properties.getString("Soil_Humi"));
                                        co2 = processSensorData(properties.getString("CO2"));
                                        o2 = processSensorData(properties.getString("O2"));
                                        ph = processSensorData(properties.getString("pH"));

                                        // ========== 新增：解析State字段 ==========
                                        Object stateObj = properties.get("State");
                                        if (stateObj instanceof String) {
                                            try {
                                                stateValue = Integer.parseInt((String) stateObj);
                                            } catch (NumberFormatException e) {
                                                stateValue = 0;
                                            }
                                        } else if (stateObj instanceof Integer) {
                                            stateValue = (Integer) stateObj;
                                        }
                                    }
                                    // 方式2：从shadow数组读取
                                    else if (object.containsKey("shadow")) {
                                        Log.d(TAG, "从shadow数组读取数据");
                                        JSONArray shadows = object.getJSONArray("shadow");
                                        if (shadows != null && shadows.size()>0) {
                                            JSONObject service = shadows.getJSONObject(0);
                                            JSONObject reoportObj = service.getJSONObject("reported");
                                            if (reoportObj != null) {
                                                JSONObject propertiesObj = reoportObj.getJSONObject("properties");
                                                if (propertiesObj != null){
                                                    airTemp = processSensorData(propertiesObj.getString("Temp"));
                                                    airHumi = processSensorData(propertiesObj.getString("Humi"));
                                                    light = processSensorData(propertiesObj.getString("Lumi"));
                                                    bumpStatus = processSensorData(propertiesObj.getString("Bump"));
                                                    aiWarning = processSensorData(propertiesObj.getString("AIWarning"));
                                                    // 空值兜底
                                                    // 空值兜底 - 修正为"未知"
                                                    bumpStatus = TextUtils.isEmpty(bumpStatus) ? "未知" : bumpStatus;
                                                    aiWarning = TextUtils.isEmpty(aiWarning) ? "未知" : aiWarning;
                                                    // 风扇档位
                                                    if (propertiesObj.containsKey("fengdegree")) {
                                                        fanGear = propertiesObj.getInteger("fengdegree") == null ? 0 : propertiesObj.getInteger("fengdegree");
                                                    } else if (propertiesObj.containsKey("Fengd")) {
                                                        fanGear = propertiesObj.getInteger("Fengd") == null ? 0 : propertiesObj.getInteger("Fengd");
                                                    }

                                                    // 灯光状态
                                                    if (propertiesObj.containsKey("light")) {
                                                        lightState = propertiesObj.getString("light");
                                                    } else if (propertiesObj.containsKey("LightSt")) {
                                                        lightState = propertiesObj.getString("LightSt");
                                                    }
                                                    lightState = TextUtils.isEmpty(lightState) ? STATUS_OFF : lightState;

                                                    // 挡光板状态
                                                    if (propertiesObj.containsKey("board")) {
                                                        boardState = propertiesObj.getString("board");
                                                    } else if (propertiesObj.containsKey("DangGuangBan")) {
                                                        boardState = propertiesObj.getString("DangGuangBan");
                                                    }
                                                    boardState = TextUtils.isEmpty(boardState) ? STATUS_OFF : boardState;

                                                    soilTemp = processSensorData(propertiesObj.getString("Soil_Temp"));
                                                    soilHumi = processSensorData(propertiesObj.getString("Soil_Humi"));
                                                    co2 = processSensorData(propertiesObj.getString("CO2"));
                                                    o2 = processSensorData(propertiesObj.getString("O2"));
                                                    ph = processSensorData(propertiesObj.getString("pH"));

                                                    // ========== 新增：解析State字段 ==========
                                                    Object stateObj = propertiesObj.get("State");
                                                    if (stateObj instanceof String) {
                                                        try {
                                                            stateValue = Integer.parseInt((String) stateObj);
                                                        } catch (NumberFormatException e) {
                                                            stateValue = 0;
                                                        }
                                                    } else if (stateObj instanceof Integer) {
                                                        stateValue = (Integer) stateObj;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 打印关键状态日志
                                Log.d(TAG, "解析结果 - 灯光状态：" + lightState +
                                        " 挡光板状态：" + boardState +
                                        " 风扇档位：" + fanGear +
                                        " 生长阶段State：" + stateValue); // ========== 新增：打印State值 ==========

                                // 5. 保存到本地存储
                                JSONObject sensorData = new JSONObject();
                                sensorData.put("Temp", airTemp);
                                sensorData.put("Humi", airHumi);
                                sensorData.put("Lumi", light);
                                sensorData.put("Fengd", fanGear);
                                sensorData.put("LightSt", lightState);
                                sensorData.put("DangGuangBan", boardState);
                                sensorData.put("Soil_Temp", soilTemp);
                                sensorData.put("Soil_Humi", soilHumi);
                                sensorData.put("CO2", co2);
                                sensorData.put("O2", o2);
                                sensorData.put("pH", ph);
                                sensorData.put("State", stateValue); // ========== 新增：保存State值 ==========
                                sensorData.put("Bump", bumpStatus);       // 水泵状态
                                sensorData.put("AIWarning", aiWarning);   // AI病虫害预警
                                // 封装数据用于Lambda
                                final SensorDataHolder dataHolder = new SensorDataHolder();
                                dataHolder.airTemp = airTemp;
                                dataHolder.airHumi = airHumi;
                                dataHolder.light = light;
                                dataHolder.fanGear = fanGear;
                                dataHolder.lightState = lightState;
                                dataHolder.boardState = boardState;
                                dataHolder.soilTemp = soilTemp;
                                dataHolder.soilHumi = soilHumi;
                                dataHolder.co2 = co2;
                                dataHolder.o2 = o2;
                                dataHolder.ph = ph;
                                String currentGreenhouseId = greenhouseIdList.get(selectedPosition);
                                dataHolder.currentGreenhouseId = currentGreenhouseId;
                                dataHolder.stateValue = stateValue; // ========== 新增：State值存入Holder ==========
                                dataHolder.bumpStatus = bumpStatus;
                                dataHolder.aiWarning = aiWarning;
                                greenhouseHomeStorage.saveSensorData(currentGreenhouseId, sensorData);

                                // 保存到实时数据和历史数据
                                JSONObject environmentData = new JSONObject();
                                environmentData.put("temperature", airTemp);
                                environmentData.put("humidity", airHumi);
                                environmentData.put("light", light);
                                environmentData.put("windSpeed", String.valueOf(fanGear));
                                environmentData.put("lightStatus", lightState);
                                environmentData.put("windBoardStatus", boardState);
                                environmentData.put("soilHumidity", soilHumi);
                                environmentData.put("soilTemperature", soilTemp);
                                environmentData.put("co2", co2);
                                environmentData.put("ph", ph);
                                environmentData.put("o2", o2);
                                environmentData.put("growthCycle", stateValue); // ========== 新增：保存State值 ==========
                                environmentData.put("bumpStatus", bumpStatus);
                                environmentData.put("aiWarning", aiWarning);
                                HistoricalDataStorageUtils.saveRealtimeData(GreenhouseHomePage.this, currentGreenhouseId, environmentData);
                                RealtimeDataStorageUtils.saveRealtimeData(GreenhouseHomePage.this, currentGreenhouseId, environmentData);

                                // 6. 主线程更新UI
                                runOnUiThread(() -> {
                                    // 检查Activity是否已销毁
                                    if (isFinishing() || isDestroyed()) {
                                        return;
                                    }
                                    
                                    // 数据格式化，拼接单位
                                    if (tv_tempdata != null) tv_tempdata.setText(dataHolder.airTemp + " °C");
                                    if (tv_humity != null) tv_humity.setText(dataHolder.airHumi + "%");
                                    if (tv_Lumi != null) tv_Lumi.setText(dataHolder.light + " lux");
                                    if (tv_Fengd != null) tv_Fengd.setText(String.valueOf(dataHolder.fanGear));
                                    if (tv_LightSt != null) tv_LightSt.setText(dataHolder.lightState);
                                    if (tv_DangGuangBan != null) tv_DangGuangBan.setText(dataHolder.boardState);
                                    if (tv_Soil_Temp != null) tv_Soil_Temp.setText(dataHolder.soilTemp + " °C");
                                    if (tv_Soil_Humi != null) tv_Soil_Humi.setText(dataHolder.soilHumi + "%");
                                    if (tv_CO2 != null) tv_CO2.setText(dataHolder.co2 + " ppm");

                                    if (tv_pH != null) tv_pH.setText(dataHolder.ph);
                                    // 仅更新AIWarning（格式：健康状态：xxx），水泵/药泵状态不再从华为云更新
                                    if (tv_AIWarning != null) tv_AIWarning.setText("健康状态：" + dataHolder.aiWarning);
                                    // 同步ToggleButton开关状态
                                    if (tbLight != null) tbLight.setChecked(STATUS_ON.equals(dataHolder.lightState));
                                    if (tbDangGuangBan != null) tbDangGuangBan.setChecked(STATUS_ON.equals(dataHolder.boardState));

                                    // ========== 新增：更新生长阶段显示 ==========
                                    if (dataHolder.stateValue > 0) {
                                        updateGrowthStageDisplay(dataHolder.stateValue);
                                    } else {
                                        if (tv_growth_progress != null) tv_growth_progress.setText("当前进度：未开始");
                                        // 所有图片置为浅色
                                        if (iv_stage_1 != null) iv_stage_1.setImageResource(STAGE_INACTIVE_IMG_MAP.get(1));
                                        if (iv_stage_2 != null) iv_stage_2.setImageResource(STAGE_INACTIVE_IMG_MAP.get(2));
                                        if (iv_stage_3 != null) iv_stage_3.setImageResource(STAGE_INACTIVE_IMG_MAP.get(3));
                                        if (iv_stage_4 != null) iv_stage_4.setImageResource(STAGE_INACTIVE_IMG_MAP.get(4));
                                    }

                                    // 保存到历史数据
                                    saveToHistoryStorage(dataHolder.currentGreenhouseId, dataHolder.airTemp,
                                            dataHolder.airHumi, dataHolder.light, dataHolder.fanGear,
                                            dataHolder.lightState, dataHolder.boardState, dataHolder.soilTemp,
                                            dataHolder.soilHumi, dataHolder.co2, dataHolder.o2, dataHolder.ph,dataHolder.bumpStatus, dataHolder.aiWarning);
                                });

                            } catch (Exception e) {
                                Log.e(TAG, "解析华为云数据失败", e);
                                // 解析失败，使用原有数据补充
                                Spinner spinner = findViewById(R.id.spinner_greenhouse);
                                int position = spinner.getSelectedItemPosition();
                                if (position > 0) {
                                    String greenhouseId = greenhouseIdList.get(position);
                                    getEnvironmentData(greenhouseId);
                                }
                            } finally {
                                response.body().close();
                            }
                        } else {
                            if (response.body() != null) {
                                response.body().close();
                            }
                            // 响应失败，使用原有数据补充
                            Spinner spinner = findViewById(R.id.spinner_greenhouse);
                            int position = spinner.getSelectedItemPosition();
                            if (position > 0) {
                                String greenhouseId = greenhouseIdList.get(position);
                                getEnvironmentData(greenhouseId);
                            }
                        }
                    }
                });
            }
        }, 0, 1000);
    }

    /**
     * 替换：立即拉取华为云设备影子数据（使用新代码逻辑）
     */
    private void pullDeviceShadowImmediately(String greenhouseId) {
        if (TextUtils.isEmpty(AToken) || TextUtils.isEmpty(greenhouseId)) {
            Log.w(TAG, "Token或大棚ID为空，跳过华为云数据拉取");
            setSensorDefaultValue();
            return;
        }

        // 华为云设备影子请求URL
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
                setSensorDefaultValue();
                getEnvironmentData(greenhouseId);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "华为云设备影子数据：" + responseBody);

                        JSONObject object = JSON.parseObject(responseBody);
                        boolean isDataValid = false;

                        // 复用startDeviceDataPull中的解析逻辑
                        String airTemp = "0";
                        String airHumi = "0";
                        String light = "0";
                        int fanGear = 0;
                        String lightState = STATUS_OFF;
                        String boardState = STATUS_OFF;
                        String soilTemp = "0";
                        String soilHumi = "0";
                        String co2 = "0";
                        String o2 = "0";
                        String ph = "0";
                        String bumpStatus = "未知";
                        String aiWarning = "未知";
                        // ========== 新增：初始化State值 ==========
                        int stateValue = 0;

                        if (object != null) {
                            JSONObject properties = object.getJSONObject("properties");
                            if (properties != null) {
                                isDataValid = true;
                                airTemp = processSensorData(properties.getString("Temp"));
                                airHumi = processSensorData(properties.getString("Humi"));
                                light = processSensorData(properties.getString("Lumi"));
                                bumpStatus = processSensorData(properties.getString("Bump"));
                                aiWarning = processSensorData(properties.getString("AIWarning"));
                                bumpStatus = TextUtils.isEmpty(bumpStatus) ? "未知" : bumpStatus;
                                aiWarning = TextUtils.isEmpty(aiWarning) ? "未知" : aiWarning;

                                if (properties.containsKey("fengdegree")) {
                                    fanGear = properties.getInteger("fengdegree") == null ? 0 : properties.getInteger("fengdegree");
                                }

                                lightState = properties.getString("light") == null ? STATUS_OFF : properties.getString("light");
                                boardState = properties.getString("board") == null ? STATUS_OFF : properties.getString("board");

                                soilTemp = processSensorData(properties.getString("Soil_Temp"));
                                soilHumi = processSensorData(properties.getString("Soil_Humi"));
                                co2 = processSensorData(properties.getString("CO2"));
                                o2 = processSensorData(properties.getString("O2"));
                                ph = processSensorData(properties.getString("pH"));

                                // ========== 新增：解析State字段 ==========
                                Object stateObj = properties.get("State");
                                if (stateObj instanceof String) {
                                    try {
                                        stateValue = Integer.parseInt((String) stateObj);
                                    } catch (NumberFormatException e) {
                                        stateValue = 0;
                                    }
                                } else if (stateObj instanceof Integer) {
                                    stateValue = (Integer) stateObj;
                                }
                            } else if (object.containsKey("shadow")) {
                                JSONArray shadows = object.getJSONArray("shadow");
                                if (shadows != null && shadows.size()>0) {
                                    JSONObject service = shadows.getJSONObject(0);
                                    JSONObject reoportObj = service.getJSONObject("reported");
                                    if (reoportObj != null) {
                                        JSONObject propertiesObj = reoportObj.getJSONObject("properties");
                                        if (propertiesObj != null){
                                            isDataValid = true;
                                            airTemp = processSensorData(propertiesObj.getString("Temp"));
                                            airHumi = processSensorData(propertiesObj.getString("Humi"));
                                            light = processSensorData(propertiesObj.getString("Lumi"));

                                            if (propertiesObj.containsKey("fengdegree")) {
                                                fanGear = propertiesObj.getInteger("fengdegree") == null ? 0 : propertiesObj.getInteger("fengdegree");
                                            }

                                            lightState = propertiesObj.getString("light") == null ? STATUS_OFF : propertiesObj.getString("light");
                                            boardState = propertiesObj.getString("board") == null ? STATUS_OFF : propertiesObj.getString("board");
                                            bumpStatus = TextUtils.isEmpty(bumpStatus) ? "未知" : bumpStatus;
                                            aiWarning = TextUtils.isEmpty(aiWarning) ? "未知" : aiWarning;
                                            bumpStatus = TextUtils.isEmpty(bumpStatus) ? "OFF" : bumpStatus;
                                            aiWarning = TextUtils.isEmpty(aiWarning) ? "healthy" : aiWarning;
                                            soilTemp = processSensorData(propertiesObj.getString("Soil_Temp"));
                                            soilHumi = processSensorData(propertiesObj.getString("Soil_Humi"));
                                            co2 = processSensorData(propertiesObj.getString("CO2"));
                                            o2 = processSensorData(propertiesObj.getString("O2"));
                                            ph = processSensorData(propertiesObj.getString("pH"));

                                            // ========== 新增：解析State字段 ==========
                                            Object stateObj = propertiesObj.get("State");
                                            if (stateObj instanceof String) {
                                                try {
                                                    stateValue = Integer.parseInt((String) stateObj);
                                                } catch (NumberFormatException e) {
                                                    stateValue = 0;
                                                }
                                            } else if (stateObj instanceof Integer) {
                                                stateValue = (Integer) stateObj;
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isDataValid) {
                            // 更新UI
                            String finalAirTemp = airTemp;
                            String finalAirHumi = airHumi;
                            String finalLight = light;
                            int finalFanGear = fanGear;
                            String finalLightState = lightState;
                            String finalBoardState = boardState;
                            String finalSoilTemp = soilTemp;
                            String finalSoilHumi = soilHumi;
                            String finalCo = co2;
                            String finalO = o2;
                            String finalPh = ph;
                            int finalStateValue = stateValue; // ========== 新增：State值变量 ==========
                            String finalBumpStatus = bumpStatus;
                            String finalAiWarning = aiWarning;

                            runOnUiThread(() -> {
                                tv_tempdata.setText(finalAirTemp + " °C");
                                tv_humity.setText(finalAirHumi + "%");
                                tv_Lumi.setText(finalLight + " lux");
                                tv_Fengd.setText(String.valueOf(finalFanGear));
                                tv_LightSt.setText(finalLightState);
                                tv_DangGuangBan.setText(finalBoardState);
                                tv_Soil_Temp.setText(finalSoilTemp + " °C");
                                tv_Soil_Humi.setText(finalSoilHumi + "%");
                                tv_CO2.setText(finalCo + " ppm");

                                tv_pH.setText(finalPh);
                                // 仅更新AIWarning（格式：健康状态：xxx），水泵/药泵状态不再从华为云更新
                                if (tv_AIWarning != null) tv_AIWarning.setText("健康状态：" + finalAiWarning);
                                tbLight.setChecked(STATUS_ON.equals(finalLightState));
                                tbDangGuangBan.setChecked(STATUS_ON.equals(finalBoardState));

                                // ========== 新增：更新生长阶段显示 ==========
                                if (finalStateValue > 0) {
                                    updateGrowthStageDisplay(finalStateValue);
                                } else {
                                    tv_growth_progress.setText("当前进度：未开始");
                                    iv_stage_1.setImageResource(STAGE_INACTIVE_IMG_MAP.get(1));
                                    iv_stage_2.setImageResource(STAGE_INACTIVE_IMG_MAP.get(2));
                                    iv_stage_3.setImageResource(STAGE_INACTIVE_IMG_MAP.get(3));
                                    iv_stage_4.setImageResource(STAGE_INACTIVE_IMG_MAP.get(4));
                                }

                                // 保存数据
                                JSONObject sensorData = new JSONObject();
                                sensorData.put("Temp", finalAirTemp);
                                sensorData.put("Humi", finalAirHumi);
                                sensorData.put("Lumi", finalLight);
                                sensorData.put("Fengd", finalFanGear);
                                sensorData.put("LightSt", finalLightState);
                                sensorData.put("DangGuangBan", finalBoardState);
                                sensorData.put("Soil_Temp", finalSoilTemp);
                                sensorData.put("Soil_Humi", finalSoilHumi);
                                sensorData.put("CO2", finalCo);
                                sensorData.put("O2", finalO);
                                sensorData.put("pH", finalPh);
                                sensorData.put("State", finalStateValue); // ========== 新增：保存State值 ==========
                                sensorData.put("Bump", finalBumpStatus);       // 水泵状态
                                sensorData.put("AIWarning", finalAiWarning);   // AI病虫害预警
                                greenhouseHomeStorage.saveSensorData(greenhouseId, sensorData);

                                // 保存到实时数据和历史数据
                                JSONObject environmentData = new JSONObject();
                                environmentData.put("temperature", finalAirTemp);
                                environmentData.put("humidity", finalAirHumi);
                                environmentData.put("light", finalLight);
                                environmentData.put("windSpeed", String.valueOf(finalFanGear));
                                environmentData.put("lightStatus", finalLightState);
                                environmentData.put("windBoardStatus", finalBoardState);
                                environmentData.put("soilHumidity", finalSoilHumi);
                                environmentData.put("soilTemperature", finalSoilTemp);
                                environmentData.put("co2", finalCo);
                                environmentData.put("ph", finalPh);
                                environmentData.put("o2", finalO);
                                environmentData.put("growthCycle", finalStateValue); // ========== 新增：保存State值 ==========
                                environmentData.put("bumpStatus", finalBumpStatus);
                                environmentData.put("aiWarning", finalAiWarning);
                                HistoricalDataStorageUtils.saveRealtimeData(GreenhouseHomePage.this, greenhouseId, environmentData);
                                RealtimeDataStorageUtils.saveRealtimeData(GreenhouseHomePage.this, greenhouseId, environmentData);
                            });
                        } else {
                            setSensorDefaultValue();
                            getEnvironmentData(greenhouseId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "解析华为云数据失败", e);
                        setSensorDefaultValue();
                        getEnvironmentData(greenhouseId);
                    } finally {
                        response.body().close();
                    }
                } else {
                    Log.e(TAG, "华为云响应失败，码：" + response.code());
                    if (response.body() != null) {
                        response.body().close();
                    }
                    setSensorDefaultValue();
                    getEnvironmentData(greenhouseId);
                }
            }
        });
    }

    /**
     * 替换：下发命令到华为云（使用新代码的命令下发逻辑）
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

        // 3. 检查大棚选择
        Spinner spinner = findViewById(R.id.spinner_greenhouse);
        int selectedPosition = spinner.getSelectedItemPosition();
        if (selectedPosition == 0) {
            runOnUiThread(() -> Toast.makeText(this, "请先选择大棚", Toast.LENGTH_SHORT).show());
            return;
        }

        // 4. 构建命令参数
        String commandName;
        String paramKey;
        switch (deviceType) {
            case "LightSt":
                commandName = CMD_LIGHT;
                paramKey = PARAM_LIGHT;
                break;
            case "DangGuangBan":
                commandName = CMD_BOARD;
                paramKey = PARAM_BOARD;
                break;
            case "Fengd":
                commandName = CMD_FAN_GEAR;
                paramKey = PARAM_FAN;
                commandValue = Math.max(0, Math.min(9, (Integer) commandValue));
                break;
            case "BUMP": // 新增：处理水泵/药泵命令
                commandName = CMD_BUMP;
                paramKey = PARAM_BUMP;
                break;
            default:
                runOnUiThread(() -> Toast.makeText(this, "不支持的设备类型", Toast.LENGTH_SHORT).show());
                return;
        }

        // 5. 构建华为云标准命令格式
        JSONObject commandJson = new JSONObject();
        commandJson.put("service_id", SERVICE_ID);
        commandJson.put("command_name", commandName);
        commandJson.put("expire_time", 30);

        JSONObject paras = new JSONObject();
        paras.put(paramKey, commandValue);
        commandJson.put("paras", paras);

        // 6. 发送命令请求
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
                    Toast.makeText(GreenhouseHomePage.this, "命令下发失败：网络异常", Toast.LENGTH_SHORT).show();
                    resetControlState(deviceType);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        Log.d(TAG, "命令下发成功，响应：" + responseBody);

                        try {
                            JSONObject responseJson = JSON.parseObject(responseBody);
                            JSONObject responseObj = responseJson.getJSONObject("response");
                            if (responseObj != null) {
                                int resultCode = responseObj.getIntValue("result_code");
                                String result = responseObj.getJSONObject("paras").getString("result");

                                if (resultCode == 0 && "success".equalsIgnoreCase(result)) {
                                    runOnUiThread(successCallback);
                                    // 检查greenhouseIdList和索引有效性
                                    if (greenhouseIdList != null && selectedPosition >= 0 && selectedPosition < greenhouseIdList.size()) {
                                        String greenhouseId = greenhouseIdList.get(selectedPosition);
                                        pullDeviceShadowImmediately(greenhouseId);
                                    }
                                } else {
                                    runOnUiThread(() -> {
                                        Toast.makeText(GreenhouseHomePage.this,
                                                "设备执行命令失败：" + result + "（错误码：" + resultCode + "）",
                                                Toast.LENGTH_SHORT).show();
                                        resetControlState(deviceType);
                                    });
                                }
                            } else {
                                runOnUiThread(successCallback);
                                // 检查greenhouseIdList和索引有效性
                                if (greenhouseIdList != null && selectedPosition >= 0 && selectedPosition < greenhouseIdList.size()) {
                                    pullDeviceShadowImmediately(greenhouseIdList.get(selectedPosition));
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "解析响应体失败，按成功处理", e);
                            runOnUiThread(successCallback);
                            // 检查greenhouseIdList和索引有效性
                            if (greenhouseIdList != null && selectedPosition >= 0 && selectedPosition < greenhouseIdList.size()) {
                                pullDeviceShadowImmediately(greenhouseIdList.get(selectedPosition));
                            }
                        }
                    }else {
                        String errorMsg = "响应码：" + response.code() + "，信息：" + responseBody;
                        Log.e(TAG, "命令下发失败：" + errorMsg);
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
                                toastMsg = "下发失败：" + errorMsg;
                            }
                            Toast.makeText(GreenhouseHomePage.this, toastMsg, Toast.LENGTH_LONG).show();
                            resetControlState(deviceType);
                        });
                    }
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }

    /**
     * 新增：恢复控件状态（命令下发失败时）
     */
    private void resetControlState(String deviceType) {
        runOnUiThread(() -> {
            switch (deviceType) {
                case "DangGuangBan":
                    tbDangGuangBan.setChecked(!tbDangGuangBan.isChecked());
                    tv_DangGuangBan.setText(tbDangGuangBan.isChecked() ? STATUS_ON : STATUS_OFF);
                    break;
                case "LightSt":
                    tbLight.setChecked(!tbLight.isChecked());
                    tv_LightSt.setText(tbLight.isChecked() ? STATUS_ON : STATUS_OFF);
                    break;
                case "Fengd":
                    Toast.makeText(this, "风扇档位调整失败，已恢复原档位", Toast.LENGTH_SHORT).show();
                    break;
                case "BUMP": // 修正：恢复水泵/药泵按钮状态
                    // 恢复按钮状态并更新文本
                    if (onHealthy.isPressed()) {
                        onHealthy.setChecked(!onHealthy.isChecked());
                        tv_medicine.setText(onHealthy.isChecked() ? STATUS_ON : STATUS_OFF);
                    } else if (onBump.isPressed()) {
                        onBump.setChecked(!onBump.isChecked());
                        tv_Bump.setText(onBump.isChecked() ? STATUS_ON : STATUS_OFF);
                    }
                    break;
            }
        });
    }

    /**
     * 内部实体类：封装需要在Lambda中引用的变量
     */
    private static class SensorDataHolder {
        String airTemp;
        String airHumi;
        String light;
        int fanGear;
        String lightState;
        String boardState;
        String soilTemp;
        String soilHumi;
        String co2;
        String o2;
        String ph;
        String currentGreenhouseId;
        String bumpStatus;
        String aiWarning;
        int stateValue; // ========== 新增：State值字段 ==========
    }

    // ========== 原有方法保持不变 ==========
    private void setSensorDefaultValue() {
        runOnUiThread(() -> {
            // 传感器数值全部置0
            tv_tempdata.setText("0 °C");
            tv_humity.setText("0%");
            tv_Lumi.setText("0 lux");
            tv_Soil_Temp.setText("0 °C");
            tv_Soil_Humi.setText("0%");
            tv_CO2.setText("0 ppm");

            tv_pH.setText("0");
            // 设备状态置为默认值
            tv_Fengd.setText("0");
            tv_LightSt.setText(STATUS_OFF);
            tv_DangGuangBan.setText(STATUS_OFF);
            // 初始化水泵/药泵文本为OFF
            if (tv_Bump != null) tv_Bump.setText(STATUS_OFF);
            if (tv_medicine != null) tv_medicine.setText(STATUS_OFF);

            // 同步水泵/药泵按钮状态为未选中（OFF）
            if (onHealthy != null) onHealthy.setChecked(false);
            if (onBump != null) onBump.setChecked(false);
            // 初始化健康状态文本
            if (tv_AIWarning != null) tv_AIWarning.setText("健康状态：未知");
            // 同步ToggleButton状态
            tbLight.setChecked(false);
            tbDangGuangBan.setChecked(false);


            // ========== 新增：生长阶段默认值 ==========
            tv_growth_progress.setText("当前进度：未开始");
            if (iv_stage_1 != null) {
                iv_stage_1.setImageResource(STAGE_INACTIVE_IMG_MAP.get(1));
                iv_stage_2.setImageResource(STAGE_INACTIVE_IMG_MAP.get(2));
                iv_stage_3.setImageResource(STAGE_INACTIVE_IMG_MAP.get(3));
                iv_stage_4.setImageResource(STAGE_INACTIVE_IMG_MAP.get(4));
            }
        });
    }

    private void initGreenhouseList() {
        greenhouseList = greenhouseHomeStorage.getGreenhouseList();
        greenhouseIdList = greenhouseHomeStorage.getGreenhouseIdList();

        // 确保列表不为空且长度一致
        if (greenhouseList == null || greenhouseIdList == null || greenhouseList.size() != greenhouseIdList.size()) {
            greenhouseList = new ArrayList<>();
            greenhouseIdList = new ArrayList<>();
        }


        // 确保至少有一个默认大棚
        if (greenhouseList.isEmpty()) {
            greenhouseList.add("默认大棚");
            greenhouseIdList.add("1");
        }

        // 保存更新后的列表
        if (greenhouseList.size() >= 1) {
            greenhouseHomeStorage.saveGreenhouseList(greenhouseList, greenhouseIdList);
        }
    }

    private void clearAllSensorData() {
        runOnUiThread(() -> {
            tv_tempdata.setText("");
            tv_humity.setText("");
            tv_Lumi.setText("");
            tv_Fengd.setText("");
            tv_LightSt.setText("");
            tv_DangGuangBan.setText("");
            tv_Soil_Humi.setText("");
            tv_Soil_Temp.setText("");
            tv_CO2.setText("");
            tv_pH.setText("");

            if (tv_Bump != null) tv_Bump.setText("");
            if (tv_medicine != null) tv_medicine.setText("");
            if (tv_AIWarning != null) tv_AIWarning.setText("");

            // ========== 新增：清空生长阶段显示 ==========
            if (tv_growth_progress != null) {
                tv_growth_progress.setText("当前进度：未开始");
            }
            if (iv_stage_1 != null) {
                iv_stage_1.setImageResource(STAGE_INACTIVE_IMG_MAP.get(1));
                iv_stage_2.setImageResource(STAGE_INACTIVE_IMG_MAP.get(2));
                iv_stage_3.setImageResource(STAGE_INACTIVE_IMG_MAP.get(3));
                iv_stage_4.setImageResource(STAGE_INACTIVE_IMG_MAP.get(4));
            }
        });
    }

    private void startDataTimer() {
        if (mDataTimer != null) {
            mDataTimer.cancel();
            mDataTimer = null;
        }

        mDataTimer = new Timer();
        mDataTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    Spinner spinner = findViewById(R.id.spinner_greenhouse);
                    int position = spinner.getSelectedItemPosition();
                    if (position > 0 && position < greenhouseIdList.size()) {
                        String greenhouseId = greenhouseIdList.get(position);
                        getEnvironmentData(greenhouseId);
                    }
                });
            }
        }, 0, 5000);
    }

    private void getGreenhouseList() {
        new Thread(() -> {
            try {
                String response = ApiService.getGreenhouseList(userToken);
                ApiResponse<String> apiResponse = JsonUtils.fromJson(response, ApiResponse.class);
                if (apiResponse != null && apiResponse.isSuccess()) {
                    JSONArray greenhouseArray = JSON.parseArray(apiResponse.getData());
                    runOnUiThread(() -> {
                        List<String> newGreenhouseList = new ArrayList<>();
                        List<String> newGreenhouseIdList = new ArrayList<>();
                        newGreenhouseList.add("请选择大棚");
                        newGreenhouseIdList.add("");
                        newGreenhouseList.add("默认大棚");
                        newGreenhouseIdList.add("1");

                        if (greenhouseArray != null && !greenhouseArray.isEmpty()) {
                            for (int i = 0; i < greenhouseArray.size(); i++) {
                                JSONObject greenhouseObj = greenhouseArray.getJSONObject(i);
                                String id = greenhouseObj.getString("id");
                                String name = greenhouseObj.getString("name");
                                if (!"1".equals(id)) {
                                    newGreenhouseList.add(name);
                                    newGreenhouseIdList.add(id);
                                }
                            }
                        }
                        greenhouseHomeStorage.saveGreenhouseList(newGreenhouseList, newGreenhouseIdList);
                        greenhouseList = newGreenhouseList;
                        greenhouseIdList = newGreenhouseIdList;

                        Spinner spinner = findViewById(R.id.spinner_greenhouse);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(GreenhouseHomePage.this,
                                android.R.layout.simple_spinner_item, greenhouseList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adapter);

                        int lastIndex = greenhouseHomeStorage.getLastSelectedIndex();
                        int selectedIndex = (lastIndex > 0 && lastIndex < greenhouseList.size()) ? lastIndex : 1;
                        spinner.setSelection(selectedIndex);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(GreenhouseHomePage.this,
                            "获取大棚列表失败，显示存储数据", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(GreenhouseHomePage.this,
                        "网络异常", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void getEnvironmentData(String greenhouseId) {
        Log.d(TAG, "补充加载原有接口数据: " + greenhouseId);
        new Thread(() -> {
            try {
                long endTime = System.currentTimeMillis();
                long startTime = endTime - 3600000;
                String response = ApiService.getEnvironmentData(userToken, greenhouseId,
                        String.valueOf(startTime), String.valueOf(endTime));

                ApiResponse<String> apiResponse = JsonUtils.fromJson(response, ApiResponse.class);
                if (apiResponse != null && apiResponse.isSuccess()) {
                    JSONObject dataObj = JSON.parseObject(apiResponse.getData());
                    if (dataObj != null) {
                        // 仅当华为云无数据时，才更新UI
                        runOnUiThread(() -> {
                            // 检查华为云是否已有数据（如果是默认值则更新）
                            if (TextUtils.isEmpty(tv_tempdata.getText()) || "0 °C".equals(tv_tempdata.getText().toString())) {
                                tv_tempdata.setText(processSensorData(dataObj.getString("temperature")) + " °C");
                                tv_humity.setText(processSensorData(dataObj.getString("humidity")) + "%");
                                tv_Lumi.setText(processSensorData(dataObj.getString("light")) + " lux");
                                tv_Fengd.setText(processSensorData(dataObj.getString("windSpeed")));
                                tv_LightSt.setText(processSensorData(dataObj.getString("lightStatus")));
                                tv_DangGuangBan.setText(processSensorData(dataObj.getString("windBoardStatus")));
                                tv_Soil_Humi.setText(processSensorData(dataObj.getString("soilHumidity")) + "%");
                                tv_Soil_Temp.setText(processSensorData(dataObj.getString("soilTemperature")) + " °C");
                                tv_CO2.setText(processSensorData(dataObj.getString("co2")) + " ppm");
                                tv_pH.setText(processSensorData(dataObj.getString("ph")));

                                // 同步开关状态
                                tbLight.setChecked(STATUS_ON.equals(processSensorData(dataObj.getString("lightStatus"))));
                                tbDangGuangBan.setChecked(STATUS_ON.equals(processSensorData(dataObj.getString("windBoardStatus"))));
                            }
                        });

                        // 保存原有数据到本地
                        saveEnvironmentDataToStorage(greenhouseId, dataObj);
                        // 检查告警
                        checkAlarmThresholds(greenhouseId, dataObj);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "获取原有接口数据失败", e);
                // 加载本地存储数据补充
                loadEnvironmentDataFromStorage(greenhouseId);
            }
        }).start();
    }

    private void loadEnvironmentDataFromStorage(String greenhouseId) {
        JSONObject data = greenhouseHomeStorage.getEnvironmentData(greenhouseId);
        if (data != null) {
            runOnUiThread(() -> {
                // 仅当无实时数据或为默认值时才显示存储数据
                if (TextUtils.isEmpty(tv_tempdata.getText()) || "0 °C".equals(tv_tempdata.getText().toString())) {
                    tv_tempdata.setText(processSensorData(data.getString("temperature")) + " °C");
                    tv_humity.setText(processSensorData(data.getString("humidity")) + "%");
                    tv_Lumi.setText(processSensorData(data.getString("light")) + " lux");
                    tv_Fengd.setText(processSensorData(data.getString("windSpeed")));
                    tv_LightSt.setText(processSensorData(data.getString("lightStatus")));
                    tv_DangGuangBan.setText(processSensorData(data.getString("windBoardStatus")));
                    tv_Soil_Humi.setText(processSensorData(data.getString("soilHumidity")) + "%");
                    tv_Soil_Temp.setText(processSensorData(data.getString("soilTemperature")) + " °C");
                    tv_CO2.setText(processSensorData(data.getString("co2")) + " ppm");
                    tv_pH.setText(processSensorData(data.getString("ph")));

                    // 移除从存储加载水泵/药泵状态的逻辑，仅保留按钮控制
                }
            });
        }
    }

    private void loadSensorDataFromStorage(String greenhouseId) {
        JSONObject data = greenhouseHomeStorage.getSensorData(greenhouseId);
        if (data != null) {
            runOnUiThread(() -> {
                // 仅当无实时数据或为默认值时才显示存储数据
                if (TextUtils.isEmpty(tv_tempdata.getText()) || "0 °C".equals(tv_tempdata.getText().toString())) {
                    tv_tempdata.setText(processSensorData(data.getString("Temp")) + " °C");
                    tv_humity.setText(processSensorData(data.getString("Humi")) + "%");
                    tv_Lumi.setText(processSensorData(data.getString("Lumi")) + " lux");
                    tv_Fengd.setText(String.valueOf(data.getInteger("Fengd") == null ? 0 : data.getInteger("Fengd")));
                    tv_LightSt.setText(processSensorData(data.getString("LightSt")));
                    tv_DangGuangBan.setText(processSensorData(data.getString("DangGuangBan")));
                    tv_Soil_Temp.setText(processSensorData(data.getString("Soil_Temp")) + " °C");
                    tv_Soil_Humi.setText(processSensorData(data.getString("Soil_Humi")) + "%");
                    tv_CO2.setText(processSensorData(data.getString("CO2")) + " ppm");
                    tv_pH.setText(processSensorData(data.getString("pH")));

                    // 移除从存储加载水泵/药泵状态的逻辑，这些值仅由按钮控制

                    // AIWarning从存储加载并按格式显示
                    String aiWarning = processSensorData(data.getString("AIWarning"));
                    tv_AIWarning.setText("健康状态：" + (TextUtils.isEmpty(aiWarning) ? "未知" : aiWarning));

                    // ========== 新增：加载存储的State值 ==========
                    int stateValue = data.getInteger("State") == null ? 0 : data.getInteger("State");
                    if (stateValue > 0) {
                        updateGrowthStageDisplay(stateValue);
                    } else {
                        tv_growth_progress.setText("当前进度：未开始");
                        iv_stage_1.setImageResource(STAGE_INACTIVE_IMG_MAP.get(1));
                        iv_stage_2.setImageResource(STAGE_INACTIVE_IMG_MAP.get(2));
                        iv_stage_3.setImageResource(STAGE_INACTIVE_IMG_MAP.get(3));
                        iv_stage_4.setImageResource(STAGE_INACTIVE_IMG_MAP.get(4));
                    }
                }

                // 强制同步按钮状态到文本显示（初始OFF）
                if (onBump != null) {
                    onBump.setChecked(false);
                    tv_Bump.setText(STATUS_OFF);
                }
                if (onHealthy != null) {
                    onHealthy.setChecked(false);
                    tv_medicine.setText(STATUS_OFF);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消所有定时器，防止内存泄漏
        if (mDataTimer != null) {
            mDataTimer.cancel();
            mDataTimer = null;
        }
        if (mDevicePullTimer != null) {
            mDevicePullTimer.cancel();
            mDevicePullTimer = null;
        }
    }


    private void checkAlarmThresholds(String greenhouseId, JSONObject environmentData) {
        try {
            JSONObject thresholds = ThresholdStorageUtils.getThresholdsForGreenhouse(this, greenhouseId);
            if (thresholds == null) return;

            double airTemp = safeParseFloat(environmentData.getString("temperature"));
            double airHum = safeParseFloat(environmentData.getString("humidity"));
            double light = safeParseFloat(environmentData.getString("light"));
            double soilHum = safeParseFloat(environmentData.getString("soilHumidity"));
            double soilTemp = safeParseFloat(environmentData.getString("soilTemperature"));
            double co2 = safeParseFloat(environmentData.getString("co2"));
            double ph = safeParseFloat(environmentData.getString("ph"));
            double o2 = safeParseFloat(environmentData.getString("o2"));

            String greenhouseName = "默认大棚";
            Spinner spinner = findViewById(R.id.spinner_greenhouse);
            int position = spinner.getSelectedItemPosition();
            if (position > 0 && position < greenhouseList.size()) {
                greenhouseName = greenhouseList.get(position);
            }

            if (airTemp < thresholds.getIntValue("tempMin")) {
                generateAlarm(greenhouseId, greenhouseName, "温度传感器", "空气温度过低告警");
            } else if (airTemp > thresholds.getIntValue("tempMax")) {
                generateAlarm(greenhouseId, greenhouseName, "温度传感器", "空气温度过高告警");
            }

            if (airHum < thresholds.getIntValue("airHumMin")) {
                generateAlarm(greenhouseId, greenhouseName, "湿度传感器", "空气湿度过低告警");
            } else if (airHum > thresholds.getIntValue("airHumMax")) {
                generateAlarm(greenhouseId, greenhouseName, "湿度传感器", "空气湿度过高告警");
            }

            if (soilTemp < thresholds.getIntValue("soilTempMin")) {
                generateAlarm(greenhouseId, greenhouseName, "土壤温度传感器", "土壤温度过低告警");
            } else if (soilTemp > thresholds.getIntValue("soilTempMax")) {
                generateAlarm(greenhouseId, greenhouseName, "土壤温度传感器", "土壤温度过高告警");
            }

            if (soilHum < thresholds.getIntValue("humMin")) {
                generateAlarm(greenhouseId, greenhouseName, "土壤湿度传感器", "土壤湿度过低告警");
            } else if (soilHum > thresholds.getIntValue("humMax")) {
                generateAlarm(greenhouseId, greenhouseName, "土壤湿度传感器", "土壤湿度过高告警");
            }

            if (co2 < thresholds.getIntValue("co2Min")) {
                generateAlarm(greenhouseId, greenhouseName, "二氧化碳传感器", "二氧化碳浓度过低告警");
            } else if (co2 > thresholds.getIntValue("co2Max")) {
                generateAlarm(greenhouseId, greenhouseName, "二氧化碳传感器", "二氧化碳浓度过高告警");
            }

            if (o2 < thresholds.getIntValue("o2Min")) {
                generateAlarm(greenhouseId, greenhouseName, "氧气传感器", "氧气浓度过低告警");
            } else if (o2 > thresholds.getIntValue("o2Max")) {
                generateAlarm(greenhouseId, greenhouseName, "氧气传感器", "氧气浓度过高告警");
            }

            if (ph < thresholds.getIntValue("phMin")) {
                generateAlarm(greenhouseId, greenhouseName, "PH值传感器", "PH值过低告警");
            } else if (ph > thresholds.getIntValue("phMax")) {
                generateAlarm(greenhouseId, greenhouseName, "PH值传感器", "PH值过高告警");
            }

            if (light < thresholds.getIntValue("lightMin")) {
                generateAlarm(greenhouseId, greenhouseName, "光照传感器", "光照强度过低告警");
            } else if (light > thresholds.getIntValue("lightMax")) {
                generateAlarm(greenhouseId, greenhouseName, "光照传感器", "光照强度过高告警");
            }
        } catch (Exception e) {
            Log.e(TAG, "检查告警阈值失败", e);
        }
    }

    private void generateAlarm(String greenhouseId, String greenhouseName, String device, String content) {
        try {
            if (AlarmRecordStorageUtils.hasUnhandledAlarm(this, greenhouseId, device, content)) {
                return;
            }

            JSONObject alarmRecord = new JSONObject();
            alarmRecord.put("alarmId", "alarm_" + System.currentTimeMillis());
            alarmRecord.put("greenhouse", greenhouseName);
            alarmRecord.put("device", device);
            alarmRecord.put("content", content);
            alarmRecord.put("time", getCurrentTime());
            alarmRecord.put("isHandled", false);
            alarmRecord.put("timestamp", System.currentTimeMillis());

            AlarmRecordStorageUtils.saveAlarmRecord(this, alarmRecord);
            runOnUiThread(() -> Toast.makeText(GreenhouseHomePage.this, "告警: " + content, Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Log.e(TAG, "生成告警失败", e);
        }
    }

    private String getCurrentTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date());
    }

    private void syncDeviceStatusToManagePage(String greenhouseId, String lightStatus, String windBoardStatus, String windSpeed) {
        try {
            JSONObject deviceStatus = managePageStorage.getDeviceStatus();
            if (deviceStatus == null || !deviceStatus.containsKey("devices")) {
                deviceStatus = new JSONObject();
                JSONArray deviceArray = new JSONArray();

                JSONObject lightDevice = new JSONObject();
                lightDevice.put("type", "控制设备");
                lightDevice.put("name", "灯");
                lightDevice.put("status", STATUS_ON.equals(lightStatus));
                lightDevice.put("deviceId", "light_1");
                deviceArray.add(lightDevice);

                JSONObject boardDevice = new JSONObject();
                boardDevice.put("type", "控制设备");
                boardDevice.put("name", "挡光板");
                boardDevice.put("status", STATUS_ON.equals(windBoardStatus));
                boardDevice.put("deviceId", "board_1");
                deviceArray.add(boardDevice);

                JSONObject fanDevice = new JSONObject();
                fanDevice.put("type", "控制设备");
                fanDevice.put("name", "风机");
                fanDevice.put("status", !"0".equals(windSpeed));
                fanDevice.put("deviceId", "fan_1");
                deviceArray.add(fanDevice);

                JSONObject waterPumpDevice = new JSONObject();
                waterPumpDevice.put("type", "控制设备");
                waterPumpDevice.put("name", "水泵");
                waterPumpDevice.put("status", onBump != null && onBump.isChecked()); // 修改：按钮开=ON
                waterPumpDevice.put("deviceId", "water_pump_1");
                deviceArray.add(waterPumpDevice);

                JSONObject medicinePumpDevice = new JSONObject();
                medicinePumpDevice.put("type", "控制设备");
                medicinePumpDevice.put("name", "药泵");
                medicinePumpDevice.put("status", onHealthy != null && onHealthy.isChecked()); // 修改：按钮开=ON
                medicinePumpDevice.put("deviceId", "medicine_pump_1");
                deviceArray.add(medicinePumpDevice);

                deviceStatus.put("greenhouseId", greenhouseId);
                deviceStatus.put("devices", deviceArray);
            } else {
                JSONArray deviceArray = deviceStatus.getJSONArray("devices");
                for (int i = 0; i < deviceArray.size(); i++) {
                    JSONObject device = deviceArray.getJSONObject(i);
                    String deviceName = device.getString("name");
                    if ("灯".equals(deviceName)) {
                        device.put("status", STATUS_ON.equals(lightStatus));
                    } else if ("挡光板".equals(deviceName)) {
                        device.put("status", STATUS_ON.equals(windBoardStatus));
                    } else if ("风机".equals(deviceName)) {
                        device.put("status", !"0".equals(windSpeed));
                    } else if ("水泵".equals(deviceName)) {
                        // 水泵状态仅由按钮决定
                        device.put("status", onBump != null && !onBump.isChecked());
                    } else if ("药泵".equals(deviceName)) {
                        // 药泵状态仅由按钮决定
                        device.put("status", onHealthy != null && !onHealthy.isChecked());
                    }
                }
            }
            managePageStorage.saveDeviceStatus(deviceStatus);
        } catch (Exception e) {
            Log.e(TAG, "同步设备状态失败", e);
        }
    }

    public String getCurrentGreenhouseId() {
        Spinner spinner = findViewById(R.id.spinner_greenhouse);
        int position = spinner.getSelectedItemPosition();
        if (position > 0 && position < greenhouseIdList.size()) {
            return greenhouseIdList.get(position);
        }
        return "";
    }

    private void syncGreenhouseListFromStorage() {
        new Thread(() -> {
            try {
                GreenhouseStorage greenhouseStorage = new GreenhouseStorage(this);
                List<Greenhouse> list = greenhouseStorage.getGreenhouseList();
                if (list != null && !list.isEmpty()) {
                    List<String> newNames = new ArrayList<>();
                    List<String> newIds = new ArrayList<>();
                    newNames.add("请选择大棚");
                    newIds.add("");

                    for (Greenhouse gh : list) {
                        newNames.add(gh.name);
                        newIds.add(gh.id);
                    }
                    greenhouseHomeStorage.saveGreenhouseList(newNames, newIds);

                    runOnUiThread(() -> {
                        greenhouseList = newNames;
                        greenhouseIdList = newIds;
                        Spinner spinner = findViewById(R.id.spinner_greenhouse);
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(GreenhouseHomePage.this,
                                android.R.layout.simple_spinner_item, greenhouseList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinner.setAdapter(adapter);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "同步大棚列表失败", e);
            }
        }).start();
    }

    private void saveEnvironmentDataToStorage(String greenhouseId, JSONObject dataObj) {
        try {
            JSONObject environmentData = new JSONObject();
            environmentData.put("temperature", dataObj.getString("temperature"));
            environmentData.put("humidity", dataObj.getString("humidity"));
            environmentData.put("light", dataObj.getString("light"));
            environmentData.put("windSpeed", dataObj.getString("windSpeed"));
            environmentData.put("lightStatus", dataObj.getString("lightStatus"));
            environmentData.put("windBoardStatus", dataObj.getString("windBoardStatus"));
            environmentData.put("soilHumidity", dataObj.getString("soilHumidity"));
            environmentData.put("soilTemperature", dataObj.getString("soilTemperature"));
            environmentData.put("co2", dataObj.getString("co2"));
            environmentData.put("ph", dataObj.getString("ph"));
            environmentData.put("o2", dataObj.getString("o2"));
            // 移除水泵/药泵状态的存储，这些值仅由按钮控制

            greenhouseHomeStorage.saveEnvironmentData(greenhouseId, environmentData);
            HistoricalDataStorageUtils.saveRealtimeData(GreenhouseHomePage.this, greenhouseId, environmentData);
            RealtimeDataStorageUtils.saveRealtimeData(GreenhouseHomePage.this, greenhouseId, environmentData);
        } catch (Exception e) {
            Log.e(TAG, "保存环境数据失败", e);
        }
    }

    private void saveToHistoryStorage(String greenhouseId, String airTemp, String airHumi, String light,
                                      int fanGear, String lightState, String boardState, String soilTemp,
                                      String soilHumi, String co2, String o2, String ph, String bumpStatus, String aiWarning) {
        try {
            greenhouseId = TextUtils.isEmpty(greenhouseId) ? "1" : greenhouseId.trim();

            long currentTime = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            int currentDay = calendar.get(Calendar.DAY_OF_YEAR);

            // 小时数据
            JSONArray hourlyData = greenhouseHomeStorage.getHourlyHistoryData(greenhouseId);
            if (hourlyData == null) hourlyData = new JSONArray();

            JSONObject hourlyPoint = new JSONObject();
            hourlyPoint.put("timestamp", currentTime);
            hourlyPoint.put("hour", currentHour);
            hourlyPoint.put("airTemp", safeParseFloat(airTemp));
            hourlyPoint.put("airHumi", safeParseFloat(airHumi));
            hourlyPoint.put("light", safeParseFloat(light));
            hourlyPoint.put("fanGear", fanGear);
            hourlyPoint.put("lightState", lightState);
            hourlyPoint.put("boardState", boardState);
            hourlyPoint.put("soilTemp", safeParseFloat(soilTemp));
            hourlyPoint.put("soilHumi", safeParseFloat(soilHumi));
            hourlyPoint.put("co2", safeParseFloat(co2));
            hourlyPoint.put("o2", safeParseFloat(o2));
            hourlyPoint.put("ph", safeParseFloat(ph));
            // 水泵/药泵状态从按钮获取，不再使用传入的bumpStatus
            hourlyPoint.put("waterPumpStatus", onBump != null ? (onBump.isChecked() ? "OFF" : "ON") : "OFF");
            hourlyPoint.put("medicinePumpStatus", onHealthy != null ? (onHealthy.isChecked() ? "OFF" : "ON") : "OFF");
            hourlyPoint.put("aiWarning", aiWarning);
            hourlyData.add(hourlyPoint);
            if (hourlyData.size() > 24) {
                JSONArray newHourlyData = new JSONArray();
                for (int i = hourlyData.size() - 24; i < hourlyData.size(); i++) {
                    newHourlyData.add(hourlyData.getJSONObject(i));
                }
                hourlyData = newHourlyData;
            }
            greenhouseHomeStorage.saveHourlyHistoryData(greenhouseId, hourlyData);

            // 日数据
            JSONArray dailyData = greenhouseHomeStorage.getDailyHistoryData(greenhouseId);
            if (dailyData == null) dailyData = new JSONArray();

            JSONObject todayData = null;
            for (int i = 0; i < dailyData.size(); i++) {
                JSONObject dayData = dailyData.getJSONObject(i);
                if (dayData.getIntValue("day") == currentDay) {
                    todayData = dayData;
                    break;
                }
            }

            if (todayData == null) {
                todayData = new JSONObject();
                todayData.put("day", currentDay);
                todayData.put("airTempSum", 0.0f);
                todayData.put("airHumiSum", 0.0f);
                todayData.put("lightSum", 0.0f);
                todayData.put("soilTempSum", 0.0f);
                todayData.put("soilHumiSum", 0.0f);
                todayData.put("co2Sum", 0.0f);
                todayData.put("o2Sum", 0.0f);
                todayData.put("phSum", 0.0f);
                todayData.put("dataCount", 0);
                dailyData.add(todayData);
            }

            todayData.put("airTempSum", todayData.getFloatValue("airTempSum") + safeParseFloat(airTemp));
            todayData.put("airHumiSum", todayData.getFloatValue("airHumiSum") + safeParseFloat(airHumi));
            todayData.put("lightSum", todayData.getFloatValue("lightSum") + safeParseFloat(light));
            todayData.put("soilTempSum", todayData.getFloatValue("soilTempSum") + safeParseFloat(soilTemp));
            todayData.put("soilHumiSum", todayData.getFloatValue("soilHumiSum") + safeParseFloat(soilHumi));
            todayData.put("co2Sum", todayData.getFloatValue("co2Sum") + safeParseFloat(co2));
            todayData.put("o2Sum", todayData.getFloatValue("o2Sum") + safeParseFloat(o2));
            todayData.put("phSum", todayData.getFloatValue("phSum") + safeParseFloat(ph));
            todayData.put("dataCount", todayData.getIntValue("dataCount") + 1);

            int count = todayData.getIntValue("dataCount");
            if (count > 0) {
                todayData.put("airTemp", todayData.getFloatValue("airTempSum") / count);
                todayData.put("airHumi", todayData.getFloatValue("airHumiSum") / count);
                todayData.put("light", todayData.getFloatValue("lightSum") / count);
                todayData.put("soilTemp", todayData.getFloatValue("soilTempSum") / count);
                todayData.put("soilHumi", todayData.getFloatValue("soilHumiSum") / count);
                todayData.put("co2", todayData.getFloatValue("co2Sum") / count);
                todayData.put("o2", todayData.getFloatValue("o2Sum") / count);
                todayData.put("ph", todayData.getFloatValue("phSum") / count);
            }

            if (dailyData.size() > 14) {
                JSONArray newDailyData = new JSONArray();
                for (int i = dailyData.size() - 14; i < dailyData.size(); i++) {
                    newDailyData.add(dailyData.getJSONObject(i));
                }
                dailyData = newDailyData;
            }
            greenhouseHomeStorage.saveDailyHistoryData(greenhouseId, dailyData);

            // 保存到实时数据和历史数据存储工具类
            JSONObject environmentData = new JSONObject();
            environmentData.put("temperature", airTemp);
            environmentData.put("humidity", airHumi);
            environmentData.put("light", light);
            environmentData.put("windSpeed", String.valueOf(fanGear));
            environmentData.put("lightStatus", lightState);
            environmentData.put("windBoardStatus", boardState);
            environmentData.put("soilHumidity", soilHumi);
            environmentData.put("soilTemperature", soilTemp);
            environmentData.put("co2", co2);
            environmentData.put("ph", ph);
            environmentData.put("o2", o2);
            // 水泵/药泵状态从按钮获取
            environmentData.put("waterPumpStatus", onBump != null ? (onBump.isChecked() ? "OFF" : "ON") : "OFF");
            environmentData.put("medicinePumpStatus", onHealthy != null ? (onHealthy.isChecked() ? "OFF" : "ON") : "OFF");
            environmentData.put("aiWarning", aiWarning);
            HistoricalDataStorageUtils.saveRealtimeData(GreenhouseHomePage.this, greenhouseId, environmentData);
            RealtimeDataStorageUtils.saveRealtimeData(GreenhouseHomePage.this, greenhouseId, environmentData);

        } catch (Exception e) {
            Log.e(TAG, "保存历史数据失败", e);
        }
    }

    private void requestAiSuggestion() {
        if (isRequestingAiSuggestion) {
            Toast.makeText(this, "正在获取AI建议，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }

        Spinner spinner = findViewById(R.id.spinner_greenhouse);
        int position = spinner.getSelectedItemPosition();
        if (position <= 0 || position >= greenhouseIdList.size()) {
            Toast.makeText(this, "请先选择大棚", Toast.LENGTH_SHORT).show();
            return;
        }

        String greenhouseId = greenhouseIdList.get(position);
        String greenhouseName = greenhouseList.get(position);
        String growthStage = getCurrentGrowthStageName(greenhouseId);
        String userPrompt = buildAiUserPrompt(greenhouseId, greenhouseName, growthStage);

        JSONObject requestJson = new JSONObject();
        requestJson.put("model", "deepseek-chat");
        requestJson.put("temperature", 0.7);
        requestJson.put("stream", false);

        JSONArray messages = new JSONArray();
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一位专业的羊肚菌种植技术顾问。请只输出以下四段内容：\n"
                + "1) 风险等级：低/中/高（只写一个）；\n"
                + "2) 核心问题：最多3条；\n"
                + "3) 紧急操作：最多3条，按优先级；\n"
                + "4) 详细建议：最多4条。\n"
                + "要求：语言通俗、可执行、每条一句，禁止输出无关说明。");
        messages.add(systemMessage);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);
        requestJson.put("messages", messages);

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), requestJson.toJSONString());
        Request request = new Request.Builder()
                .url(DEEPSEEK_CHAT_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                .build();

        isRequestingAiSuggestion = true;
        btnGetAiSuggestion.setEnabled(false);
        btnGetAiSuggestion.setText("AI分析中...");
        showAiLoadingSheet();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    isRequestingAiSuggestion = false;
                    btnGetAiSuggestion.setEnabled(true);
                    btnGetAiSuggestion.setText("获取AI建议");
                    hideAiLoadingSheet();
                    Toast.makeText(GreenhouseHomePage.this, "获取AI建议失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String aiContent = "";
                try {
                    if (response.body() != null) {
                        String raw = response.body().string();
                        JSONObject responseJson = JSON.parseObject(raw);
                        if (responseJson != null && responseJson.containsKey("choices")) {
                            JSONArray choices = responseJson.getJSONArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                JSONObject choice = choices.getJSONObject(0);
                                JSONObject message = choice.getJSONObject("message");
                                if (message != null) {
                                    aiContent = message.getString("content");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析AI建议失败", e);
                } finally {
                    response.close();
                }

                final String finalAiContent = TextUtils.isEmpty(aiContent) ? "AI暂未返回有效建议，请稍后重试。" : aiContent;
                runOnUiThread(() -> {
                    isRequestingAiSuggestion = false;
                    btnGetAiSuggestion.setEnabled(true);
                    btnGetAiSuggestion.setText("获取AI建议");
                    hideAiLoadingSheet();
                    showAiSuggestionBottomSheet(finalAiContent);
                });
            }
        });
    }

    private String buildAiUserPrompt(String greenhouseId, String greenhouseName, String growthStage) {
        String airTemp = extractNumber(tv_tempdata.getText().toString());
        String airHumi = extractNumber(tv_humity.getText().toString());
        String soilTemp = extractNumber(tv_Soil_Temp.getText().toString());
        String soilHumi = extractNumber(tv_Soil_Humi.getText().toString());
        String light = extractNumber(tv_Lumi.getText().toString());
        String co2 = extractNumber(tv_CO2.getText().toString());
        String ph = extractNumber(tv_pH.getText().toString());

        String boardStatus = safeText(tv_DangGuangBan.getText().toString(), STATUS_OFF);
        String medicineStatus = safeText(tv_medicine.getText().toString(), STATUS_OFF);
        String bumpStatus = safeText(tv_Bump.getText().toString(), STATUS_OFF);
        String lightStatus = safeText(tv_LightSt.getText().toString(), STATUS_OFF);
        String fanGear = safeText(tv_Fengd.getText().toString(), "0");

        JSONArray hourlyData = RealtimeDataStorageUtils.getHourlyAverageData(this, greenhouseId);
        JSONArray dailyData = HistoricalDataStorageUtils.getDailyAverageData(this, greenhouseId, 7);

        String airTempTrend24h = formatRangeTrend(hourlyData, "temperature", "℃");
        String airHumiTrend24h = formatRangeTrend(hourlyData, "humidity", "%");
        String soilTempTrend24h = formatRangeTrend(hourlyData, "soilTemperature", "℃");
        String soilHumiTrend24h = formatRangeTrend(hourlyData, "soilHumidity", "%");
        String lightTrend24h = formatRangeTrend(hourlyData, "light", "lux");
        String co2Trend24h = formatRangeTrend(hourlyData, "co2", "ppm");

        String airTempTrend7d = formatRangeTrend(dailyData, "temperature", "℃");
        String airHumiTrend7d = formatRangeTrend(dailyData, "humidity", "%");
        String soilTempTrend7d = formatRangeTrend(dailyData, "soilTemperature", "℃");
        String soilHumiTrend7d = formatRangeTrend(dailyData, "soilHumidity", "%");
        String lightTrend7d = formatRangeTrend(dailyData, "light", "lux");
        String co2Trend7d = formatRangeTrend(dailyData, "co2", "ppm");

        return "以下是我的大棚数据：\n"
                + "大棚ID：" + greenhouseId + "\n"
                + "大棚名称：" + greenhouseName + "\n"
                + "当前生长阶段：" + growthStage + "\n"
                + "\n【实时环境数据】\n"
                + "空气温度：" + airTemp + "℃\n"
                + "空气湿度：" + airHumi + "%\n"
                + "土壤温度：" + soilTemp + "℃\n"
                + "土壤湿度：" + soilHumi + "%\n"
                + "光照强度：" + light + "lux\n"
                + "CO₂浓度：" + co2 + "ppm\n"
                + "土壤pH值：" + ph + "\n"
                + "\n【设备运行状态】\n"
                + "挡光板状态：" + boardStatus + "\n"
                + "药泵状态：" + medicineStatus + "\n"
                + "水泵状态：" + bumpStatus + "\n"
                + "灯泡状态：" + lightStatus + "\n"
                + "风机档位：" + fanGear + "档\n"
                + "\n【历史趋势】\n"
                + "近24小时空气温湿度波动：温度" + airTempTrend24h + "，湿度" + airHumiTrend24h + "\n"
                + "近24小时土壤温湿度波动：温度" + soilTempTrend24h + "，湿度" + soilHumiTrend24h + "\n"
                + "近24小时光照变化：" + lightTrend24h + "\n"
                + "近24小时CO₂变化：" + co2Trend24h + "\n"
                + "近7天空气温湿度波动：温度" + airTempTrend7d + "，湿度" + airHumiTrend7d + "\n"
                + "近7天土壤温湿度波动：温度" + soilTempTrend7d + "，湿度" + soilHumiTrend7d + "\n"
                + "近7天光照变化：" + lightTrend7d + "\n"
                + "近7天CO₂变化：" + co2Trend7d + "\n"
                + "\n请根据这些数据，给出我的大棚当前的种植建议，包括环境调节建议、设备调控建议、可能的病虫害风险预警及对应的防治措施，语言要通俗易懂，直接给出可执行的操作。";
    }

    private void showAiSuggestionBottomSheet(String aiText) {
        View contentView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_suggestion_sheet, null);
        BottomSheetDialog sheetDialog = new BottomSheetDialog(this);
        sheetDialog.setContentView(contentView);
        sheetDialog.setDismissWithAnimation(true);

        TextView tvRisk = contentView.findViewById(R.id.tv_risk_level);
        TextView tvCore = contentView.findViewById(R.id.tv_core_problem);
        TextView tvUrgent = contentView.findViewById(R.id.tv_urgent_action);
        TextView tvDetail = contentView.findViewById(R.id.tv_detail_advice);
        Button btnClose = contentView.findViewById(R.id.btn_close_ai_sheet);
        Button btnCopy = contentView.findViewById(R.id.btn_copy_ai_sheet);

        AiSuggestionModel suggestion = parseAiSuggestion(aiText);
        tvRisk.setText("风险等级：" + suggestion.riskLevel);
        tvCore.setText("核心问题：\n" + suggestion.coreProblem);
        tvUrgent.setText("紧急操作：\n" + suggestion.urgentAction);
        tvDetail.setText(buildStyledDetailText("详细建议：\n" + suggestion.detailAdvice));

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                String copyText = "风险等级：" + suggestion.riskLevel + "\n\n核心问题：\n"
                        + suggestion.coreProblem + "\n\n紧急操作：\n"
                        + suggestion.urgentAction + "\n\n详细建议：\n"
                        + suggestion.detailAdvice;
                ClipData clip = ClipData.newPlainText("AI智能建议", copyText);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(GreenhouseHomePage.this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        });
        btnClose.setOnClickListener(v -> sheetDialog.dismiss());

        sheetDialog.setOnShowListener(dialog -> {
            FrameLayout bottomSheet = sheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight((int) (screenHeight * 0.68f), true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setDraggable(true);
            }
        });
        sheetDialog.show();
    }

    private void showAiLoadingSheet() {
        if (aiLoadingDialog != null && aiLoadingDialog.isShowing()) {
            return;
        }
        View loadingView = LayoutInflater.from(this).inflate(R.layout.dialog_ai_loading_sheet, null);
        aiLoadingDialog = new BottomSheetDialog(this);
        aiLoadingDialog.setContentView(loadingView);
        aiLoadingDialog.setCancelable(false);
        aiLoadingDialog.setDismissWithAnimation(true);
        aiLoadingDialog.setOnShowListener(dialog -> {
            FrameLayout bottomSheet = aiLoadingDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                int screenHeight = getResources().getDisplayMetrics().heightPixels;
                behavior.setPeekHeight((int) (screenHeight * 0.3f), true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setDraggable(false);
            }
        });
        aiLoadingDialog.show();
    }

    private void hideAiLoadingSheet() {
        if (aiLoadingDialog != null && aiLoadingDialog.isShowing()) {
            aiLoadingDialog.dismiss();
        }
    }

    private SpannableString buildStyledDetailText(String detailText) {
        SpannableString spannable = new SpannableString(detailText);
        String lower = detailText.toLowerCase(Locale.ROOT);
        highlightKeyword(spannable, lower, "高", "#C62828");
        highlightKeyword(spannable, lower, "过高", "#C62828");
        highlightKeyword(spannable, lower, "过低", "#EF6C00");
        highlightKeyword(spannable, lower, "风险", "#C62828");
        highlightKeyword(spannable, lower, "预警", "#EF6C00");
        return spannable;
    }

    private void highlightKeyword(SpannableString spannable, String lowerContent, String keyword, String colorHex) {
        int start = lowerContent.indexOf(keyword.toLowerCase(Locale.ROOT));
        while (start >= 0) {
            int end = start + keyword.length();
            spannable.setSpan(new ForegroundColorSpan(Color.parseColor(colorHex)), start, end, 0);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
            start = lowerContent.indexOf(keyword.toLowerCase(Locale.ROOT), end);
        }
    }

    private AiSuggestionModel parseAiSuggestion(String aiText) {
        AiSuggestionModel model = new AiSuggestionModel();
        if (TextUtils.isEmpty(aiText)) {
            return model;
        }

        String[] lines = aiText.split("\\n");
        StringBuilder core = new StringBuilder();
        StringBuilder urgent = new StringBuilder();
        StringBuilder detail = new StringBuilder();
        int section = 0;
        for (String line : lines) {
            String text = line == null ? "" : line.trim();
            if (TextUtils.isEmpty(text)) continue;

            if (text.contains("风险等级")) {
                model.riskLevel = extractSectionValue(text, "风险等级");
                section = 0;
                continue;
            }
            if (text.contains("核心问题")) {
                section = 1;
                continue;
            }
            if (text.contains("紧急操作")) {
                section = 2;
                continue;
            }
            if (text.contains("详细建议")) {
                section = 3;
                continue;
            }

            if (section == 1 && core.length() < 180) {
                appendLimitedBullet(core, text);
            } else if (section == 2 && urgent.length() < 180) {
                appendLimitedBullet(urgent, text);
            } else if (section == 3 && detail.length() < 260) {
                appendLimitedBullet(detail, text);
            }
        }

        if (core.length() == 0) core.append("暂无明确核心问题，建议持续监测关键环境指标。");
        if (urgent.length() == 0) urgent.append("暂无紧急操作，先保持当前参数并持续观察。");
        if (detail.length() == 0) detail.append("建议结合实时数据每2小时复核一次温湿度与设备状态。");

        model.coreProblem = core.toString();
        model.urgentAction = urgent.toString();
        model.detailAdvice = detail.toString();
        return model;
    }

    private void appendLimitedBullet(StringBuilder builder, String line) {
        String normalized = line.replaceFirst("^[0-9]+[.、\\)]\\s*", "")
                .replaceFirst("^[-•]\\s*", "");
        if (builder.length() > 0) {
            builder.append("\n");
        }
        builder.append("• ").append(normalized);
    }

    private String extractSectionValue(String line, String sectionName) {
        String text = line.replace("：", ":");
        int idx = text.indexOf(":");
        if (idx >= 0 && idx < text.length() - 1) {
            return text.substring(idx + 1).trim();
        }
        return "中";
    }

    private static class AiSuggestionModel {
        String riskLevel = "中";
        String coreProblem = "";
        String urgentAction = "";
        String detailAdvice = "";
    }

    private String getCurrentGrowthStageName(String greenhouseId) {
        JSONObject sensorData = greenhouseHomeStorage.getSensorData(greenhouseId);
        int stateValue = 0;
        if (sensorData != null && sensorData.getInteger("State") != null) {
            stateValue = sensorData.getInteger("State");
        }
        if (stateValue <= 0) {
            return "未知阶段";
        }
        return STAGE_NAME_MAP.getOrDefault(stateValue, "未知阶段");
    }

    private String formatRangeTrend(JSONArray dataArray, String key, String unit) {
        if (dataArray == null || dataArray.isEmpty()) {
            return "暂无数据";
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        boolean hasValid = false;
        for (int i = 0; i < dataArray.size(); i++) {
            JSONObject obj = dataArray.getJSONObject(i);
            if (obj == null) continue;
            double value = safeParseTrendValue(obj.getString(key));
            min = Math.min(min, value);
            max = Math.max(max, value);
            hasValid = true;
        }

        if (!hasValid) {
            return "暂无数据";
        }

        return String.format(Locale.getDefault(), "%.1f-%.1f%s", min, max, unit);
    }

    private double safeParseTrendValue(String value) {
        try {
            if (TextUtils.isEmpty(value)) return 0.0;
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String extractNumber(String rawValue) {
        if (rawValue == null) return "0";
        String value = rawValue.trim();
        if (value.isEmpty()) return "0";
        return value.replaceAll("[^0-9.\\-]", "");
    }

    private String safeText(String value, String fallback) {
        if (TextUtils.isEmpty(value)) return fallback;
        return value.trim();
    }
}
