package com.example.yangdujun;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class MineDeepseek extends AppCompatActivity {

    private static final String TAG = "MineDeepseek";
    private Button bt_opencloseSpeech, btn_send_text;
    private OkHttpClient client;
    private String APIkey = BuildConfig.DEEPSEEK_API_KEY;
    private TextView tv_sendspeeddata, getTv_sendspeeddata, tv_history_record;
    private EditText et_text_input;
    private String speekstring = "";
    private String chatHistory = "";

    // 录音相关变量
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private AudioRecord audioRecord;
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private File audioFile;
    private boolean isRecording = false;
    private Thread recordingThread;
    private static final String AUDIO_FILE_NAME = "user_audio.pcm";
    private static final long MIN_RECORD_DURATION = 500;
    private long recordStartTime = 0;

    // 华为云配置
    private String huaweiProjectId = BuildConfig.HUAWEI_PROJECT_ID;
    private String huaweiRegion = BuildConfig.HUAWEI_REGION;
    private String huaweiToken = "";

    // 华为云账号配置
    private static final String HUAWEI_CLOUD_USER = BuildConfig.HUAWEI_CLOUD_USER;
    private static final String HUAWEI_CLOUD_PWD = BuildConfig.HUAWEI_CLOUD_PWD;
    private static final String HUAWEI_CLOUD_DOMAIN = BuildConfig.HUAWEI_CLOUD_DOMAIN;
    private static final String HUAWEI_CLOUD_PROJECT_NAME = BuildConfig.HUAWEI_CLOUD_PROJECT_NAME;

    // Token缓存相关
    private SharedPreferences sp;
    private static final long TOKEN_EXPIRE_TIME = 24 * 60 * 60 * 1000 - 5 * 60 * 1000;

    // 历史记录相关
    private static final String CHAT_HISTORY_KEY = "chat_history";
    private static final int MAX_HISTORY_LENGTH = 5000;

    // WebSocket相关
    private WebSocket currentWebSocket;
    private ExecutorService audioSendExecutor; // 音频发送线程池
    private boolean isWebSocketConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deepseek);

        // 初始化SharedPreferences用于Token缓存
        sp = getSharedPreferences("huawei_token", Context.MODE_PRIVATE);

        // 初始化线程池（单线程，避免并发发送）
        audioSendExecutor = Executors.newSingleThreadExecutor();

        // 初始化OkHttpClient - 增加WebSocket超时配置
        client = new OkHttpClient().newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .pingInterval(10, TimeUnit.SECONDS) // 增加心跳包，保持连接
                .retryOnConnectionFailure(true)
                .build();

        // 初始化录音文件
        audioFile = new File(getFilesDir(), AUDIO_FILE_NAME);

        // 初始化UI控件
        initViews();

        // 加载历史对话记录
        loadChatHistory();

        // 申请权限
        requestPermissionsIfNeeded();
    }

    private void initViews() {
        bt_opencloseSpeech = findViewById(R.id.bt_opencloseSpeech);
        btn_send_text = findViewById(R.id.btn_send_text);
        tv_sendspeeddata = findViewById(R.id.textSpeechTO);
        getTv_sendspeeddata = findViewById(R.id.textSpeechFrom);
        tv_history_record = findViewById(R.id.tv_history_record);
        et_text_input = findViewById(R.id.et_text_input);

        // 返回按钮
        ImageView iv_back = findViewById(R.id.iv_back);
        iv_back.setOnClickListener(v -> {
            startActivity(new Intent(MineDeepseek.this, MinePage.class));
            finish();
        });

        // 录音按钮触摸事件
        bt_opencloseSpeech.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        handleRecordStart();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        handleRecordStop();
                        return true;
                }
                return false;
            }
        });

        // 发送按钮
        btn_send_text.setOnClickListener(v -> {
            String inputText = et_text_input.getText().toString().trim();
            if (TextUtils.isEmpty(inputText)) {
                Toast.makeText(MineDeepseek.this, "请输入提问内容", Toast.LENGTH_SHORT).show();
                return;
            }
            hideKeyboard();
            tv_sendspeeddata.setText(inputText);
            sendTextToDeepSeek(inputText);
            et_text_input.setText("");
        });
    }

    private void handleRecordStart() {
        if (ContextCompat.checkSelfPermission(MineDeepseek.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MineDeepseek.this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.VIBRATE},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            startRecording();
        }
    }

    private void handleRecordStop() {
        stopRecording();
        long recordDuration = System.currentTimeMillis() - recordStartTime;
        if (recordDuration < MIN_RECORD_DURATION) {
            runOnUiThread(() -> {
                Toast.makeText(this, "录音时长过短（至少0.5秒），请重新录制", Toast.LENGTH_SHORT).show();
                resetButtonState();
            });
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            processAudio();
        } else {
            runOnUiThread(() -> {
                Toast.makeText(this, "系统版本过低，暂不支持语音识别功能", Toast.LENGTH_LONG).show();
                resetButtonState();
            });
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null) {
            imm.hideSoftInputFromWindow(et_text_input.getWindowToken(), 0);
        }
    }

    private void requestPermissionsIfNeeded() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.VIBRATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.INTERNET);
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    // =============== 核心修复：优化WebSocket语音识别 ===============
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void processAudio() {
        if (!audioFile.exists() || audioFile.length() < 1024) {
            runOnUiThread(() -> {
                Toast.makeText(this, "录音文件无效或过短", Toast.LENGTH_SHORT).show();
                resetButtonState();
            });
            return;
        }

        long audioSize = audioFile.length();
        long audioDurationMs = (audioSize * 1000) / (SAMPLE_RATE * 2);
        Log.d(TAG, "音频文件大小：" + audioSize + "字节，时长：" + audioDurationMs + "毫秒");

        if (audioDurationMs > 30 * 1000) {
            runOnUiThread(() -> {
                Toast.makeText(this, "录音时长超过30秒，请录制30秒以内", Toast.LENGTH_LONG).show();
                resetButtonState();
            });
            return;
        }

        runOnUiThread(() -> {
            bt_opencloseSpeech.setText("处理中...");
            bt_opencloseSpeech.setEnabled(false);
            bt_opencloseSpeech.setBackgroundResource(R.drawable.shape_button_red);
        });

        new Thread(() -> {
            try {
                // 关闭之前的WebSocket连接
                if (currentWebSocket != null) {
                    currentWebSocket.close(1000, "重新连接");
                }

                // 获取华为云Token
                huaweiToken = getValidToken();
                if (huaweiToken == null || huaweiToken.isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(MineDeepseek.this, "获取华为云Token失败", Toast.LENGTH_SHORT).show();
                        resetButtonState();
                    });
                    return;
                }

                // 调用WebSocket语音识别
                connectAndRecognize();

            } catch (final Exception e) {
                Log.e(TAG, "处理音频错误: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MineDeepseek.this, "语音处理失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
                    resetButtonState();
                });
            }
        }).start();
    }

    /**
     * 修复后的WebSocket连接和识别逻辑
     */
    private void connectAndRecognize() {
        String url = "wss://sis-ext." + huaweiRegion + ".myhuaweicloud.cn/v1/" + huaweiProjectId + "/asr/short-audio";

        // 重新构建WebSocket客户端，增加超时配置
        OkHttpClient wsClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .pingInterval(5, TimeUnit.SECONDS) // 5秒心跳包
                .retryOnConnectionFailure(true)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-Auth-Token", huaweiToken)
                .addHeader("Connection", "keep-alive") // 保持连接
                .addHeader("Accept-Encoding", "gzip, deflate")
                .build();

        // 重置连接状态
        isWebSocketConnected = false;

        currentWebSocket = wsClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket连接打开，响应码: " + response.code());
                isWebSocketConnected = true;

                // 延迟发送开始指令（避免连接未稳定）
                audioSendExecutor.execute(() -> {
                    try {
                        Thread.sleep(500); // 等待500ms确保连接稳定

                        // 发送开始指令
                        JSONObject startCmd = new JSONObject();
                        startCmd.put("command", "START");

                        JSONObject config = new JSONObject();
                        config.put("audio_format", "pcm16k16bit");
                        config.put("property", "chinese_16k_general");
                        config.put("need_punctuation", true); // 明确要求标点
                        startCmd.put("config", config);

                        Log.d(TAG, "发送开始指令: " + startCmd.toJSONString());
                        webSocket.send(startCmd.toJSONString());

                        // 等待指令发送完成
                        Thread.sleep(300);

                        // 读取并分块发送音频数据（优化分包策略）
                        sendAudioData(webSocket);

                    } catch (InterruptedException e) {
                        Log.e(TAG, "发送准备中断: " + e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                });
            }

            /**
             * 优化音频发送逻辑 - 解决Broken pipe问题
             */
            private void sendAudioData(WebSocket webSocket) {
                if (!isWebSocketConnected) {
                    Log.e(TAG, "WebSocket未连接，停止发送音频");
                    return;
                }

                byte[] audioData = readAudioFile();
                if (audioData == null || audioData.length == 0) {
                    Log.e(TAG, "音频数据为空，发送结束指令");
                    sendEndCommand(webSocket);
                    return;
                }

                Log.d(TAG, "开始发送音频数据，总长度: " + audioData.length + "字节");

                // 优化分包大小（减小分包，降低发送压力）
                int byteLen = 1024; // 从4000改为1024
                int nowIndex = 0;

                while (nowIndex < audioData.length && isWebSocketConnected) {
                    try {
                        int nextIndex = nowIndex + byteLen;
                        if (nextIndex > audioData.length) {
                            nextIndex = audioData.length;
                        }

                        byte[] sendData = new byte[nextIndex - nowIndex];
                        System.arraycopy(audioData, nowIndex, sendData, 0, sendData.length);

                        // 发送二进制数据
                        boolean sendSuccess = webSocket.send(ByteString.of(sendData));
                        if (!sendSuccess) {
                            Log.e(TAG, "音频数据发送失败，索引: " + nowIndex);
                            break;
                        }

                        Log.d(TAG, "发送音频块: " + nowIndex + "-" + nextIndex + " 字节");

                        nowIndex += byteLen;

                        // 增加发送间隔（关键修复：从50ms改为100ms）
                        Thread.sleep(100);

                    } catch (InterruptedException e) {
                        Log.e(TAG, "音频发送中断: " + e.getMessage());
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // 所有数据发送完成，延迟发送结束指令
                try {
                    Thread.sleep(500); // 等待最后一包数据发送完成
                    sendEndCommand(webSocket);
                } catch (InterruptedException e) {
                    Log.e(TAG, "等待结束指令发送中断: " + e.getMessage());
                }
            }

            /**
             * 发送结束指令
             */
            private void sendEndCommand(WebSocket webSocket) {
                if (!isWebSocketConnected) {
                    Log.e(TAG, "WebSocket已断开，无法发送结束指令");
                    return;
                }

                JSONObject endCmd = new JSONObject();
                endCmd.put("command", "END");
                endCmd.put("cancel", false);
                Log.d(TAG, "发送结束指令: " + endCmd.toJSONString());
                webSocket.send(endCmd.toJSONString());
            }

            /**
             * 解析识别结果
             */
            private String parseRecognizedText(String jsonData) {
                try {
                    Log.d(TAG, "开始解析JSON数据: " + jsonData);
                    JSONObject response = JSON.parseObject(jsonData);

                    if (response == null) {
                        Log.e(TAG, "JSON解析结果为空");
                        return "";
                    }

                    // 兼容不同的返回格式
                    if (response.containsKey("result") && !response.getJSONObject("result").isEmpty()) {
                        // 格式1：直接返回result
                        JSONObject resultObj = response.getJSONObject("result");
                        // 修复：先判断key是否存在，再获取值
                        return resultObj.containsKey("text") ? resultObj.getString("text").trim() : "";
                    } else if (response.containsKey("segments")) {
                        // 格式2：segments数组
                        JSONArray segments = response.getJSONArray("segments");
                        if (segments != null && segments.size() > 0) {
                            JSONObject segment = segments.getJSONObject(0);
                            if (segment != null && segment.containsKey("result")) {
                                JSONObject resultObj = segment.getJSONObject("result");
                                // 修复：先判断key是否存在，再获取值
                                return resultObj.containsKey("text") ? resultObj.getString("text").trim() : "";
                            }
                        }
                    }

                    Log.e(TAG, "无法解析识别结果，原始数据: " + jsonData);
                    return "";
                } catch (Exception e) {
                    Log.e(TAG, "解析语音识别结果出错: " + e.getMessage(), e);
                    return "";
                }
            }


            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "接收到文本消息: " + text);

                JSONObject msgData = JSON.parseObject(text);
                if (msgData == null) {
                    Log.e(TAG, "消息解析为空");
                    return;
                }

                // 处理错误响应
                if (msgData.containsKey("error_code")) {
                    String errorCode = msgData.getString("error_code");
                    String errorMsg = msgData.containsKey("error_msg") ? msgData.getString("error_msg") : "未知错误";
                    Log.e(TAG, "语音识别错误: " + errorCode + " - " + errorMsg);
                    runOnUiThread(() -> {
                        Toast.makeText(MineDeepseek.this, "识别失败: " + errorMsg, Toast.LENGTH_LONG).show();
                        resetButtonState();
                    });
                    webSocket.close(1001, "识别错误");
                    isWebSocketConnected = false;
                    return;
                }

                if (msgData.containsKey("resp_type")) {
                    String respType = msgData.getString("resp_type");

                    if ("RESULT".equals(respType)) {
                        String recognizedText = parseRecognizedText(text);
                        Log.d(TAG, "识别结果: " + recognizedText);
                        if (!recognizedText.isEmpty()) {
                            // 在UI线程更新界面并发送到DeepSeek
                            runOnUiThread(() -> {
                                tv_sendspeeddata.setText(recognizedText);
                                sendTextToDeepSeek(recognizedText);
                            });
                            // 延迟关闭连接
                            audioSendExecutor.execute(() -> {
                                try {
                                    Thread.sleep(1000);
                                    webSocket.close(1000, "识别完成");
                                    isWebSocketConnected = false;
                                } catch (InterruptedException e) {
                                    Log.e(TAG, "关闭连接延迟中断", e);
                                }
                            });
                        }
                    } else if ("FINISHED".equals(respType)) {
                        Log.d(TAG, "识别完成，无结果返回");
                        runOnUiThread(() -> {
                            Toast.makeText(MineDeepseek.this, "未识别到有效语音", Toast.LENGTH_SHORT).show();
                            resetButtonState();
                        });
                        webSocket.close(1000, "识别完成");
                        isWebSocketConnected = false;
                    }
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                Log.d(TAG, "接收到二进制消息，长度: " + bytes.size());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket正在关闭: " + code + " - " + reason);
                isWebSocketConnected = false;
                webSocket.close(code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket已关闭: " + code + " - " + reason);
                isWebSocketConnected = false;

                // 非正常关闭且未完成识别
                if (code != 1000 && !tv_sendspeeddata.getText().toString().isEmpty()) {
                    runOnUiThread(() -> {
                        Toast.makeText(MineDeepseek.this, "语音识别连接异常关闭: " + reason, Toast.LENGTH_LONG).show();
                        resetButtonState();
                    });
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket错误: " + t.getMessage(), t);
                isWebSocketConnected = false;

                String errorMsg = t.getMessage();
                if (response != null) {
                    errorMsg += " (响应码: " + response.code() + ")";
                    try {
                        if (response.body() != null) {
                            errorMsg += " 错误信息: " + response.body().string();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "读取错误响应失败", e);
                    }
                }

                String finalErrorMsg = errorMsg;
                runOnUiThread(() -> {
                    Toast.makeText(MineDeepseek.this, "语音识别连接失败: " + finalErrorMsg, Toast.LENGTH_LONG).show();
                    resetButtonState();
                });
            }
        });
    }

    // =============== Token相关方法 ===============
    /**
     * 获取有效的Token
     */
    private String getValidToken() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Log.e(TAG, "❌ 系统版本低于API 19，无法获取Token");
            return null;
        }

        // 1. 优先从SP读取缓存Token
        String cachedToken = sp.getString("huawei_token", "");
        long cachedTime = sp.getLong("token_time", 0);

        // 2. 检查Token是否有效（未过期）
        if (!TextUtils.isEmpty(cachedToken) && System.currentTimeMillis() - cachedTime < TOKEN_EXPIRE_TIME) {
            Log.d(TAG, "♻️ 使用缓存的Token");
            return cachedToken;
        }

        // 3. 同步锁防止重复请求Token
        synchronized (this) {
            try {
                // 双重检查
                cachedToken = sp.getString("huawei_token", "");
                cachedTime = sp.getLong("token_time", 0);
                if (!TextUtils.isEmpty(cachedToken) && System.currentTimeMillis() - cachedTime < TOKEN_EXPIRE_TIME) {
                    return cachedToken;
                }

                // 4. 获取新Token
                String newToken = getHuaweiCloudToken();
                if (!TextUtils.isEmpty(newToken)) {
                    // 5. 保存Token到SP
                    sp.edit()
                            .putString("huawei_token", newToken)
                            .putLong("token_time", System.currentTimeMillis())
                            .apply();
                    Log.d(TAG, "✅ 获取新Token成功，长度: " + newToken.length());
                    return newToken;
                }
            } catch (Exception e) {
                Log.e(TAG, "获取Token失败: " + e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * 获取华为云Token
     */
    private String getHuaweiCloudToken() {
        OkHttpClient tokenClient = new OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        // 华为云鉴权请求体
        String requestBodyStr = String.format(
                "{\n" +
                        "    \"auth\": {\n" +
                        "        \"identity\": {\n" +
                        "            \"methods\": [\"password\"],\n" +
                        "            \"password\": {\n" +
                        "                \"user\": {\n" +
                        "                    \"name\": \"%s\",\n" +
                        "                    \"password\": \"%s\",\n" +
                        "                    \"domain\": {\"name\": \"%s\"}\n" +
                        "                }\n" +
                        "            }\n" +
                        "        },\n" +
                        "        \"scope\": {\n" +
                        "            \"project\": {\"name\": \"%s\"}\n" +
                        "        }\n" +
                        "    }\n" +
                        "}",
                HUAWEI_CLOUD_USER, HUAWEI_CLOUD_PWD, HUAWEI_CLOUD_DOMAIN, HUAWEI_CLOUD_PROJECT_NAME
        );

        RequestBody body = RequestBody.create(mediaType, requestBodyStr);
        Request request = new Request.Builder()
                .url("https://iam.cn-north-4.myhuaweicloud.com/v3/auth/tokens")
                .post(body)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build();

        try {
            Response response = tokenClient.newCall(request).execute();
            Log.d(TAG, "Token请求响应码: " + response.code());

            if (response.isSuccessful()) {
                String token = response.header("X-Subject-Token");
                if (token != null && !token.isEmpty()) {
                    Log.d(TAG, "✅ Token获取成功");
                    return token;
                } else {
                    Log.e(TAG, "❌ Token响应头为空");
                }
            } else {
                // 读取错误信息
                if (response.body() != null) {
                    String errorBody = response.body().string();
                    Log.e(TAG, "❌ Token获取失败: " + errorBody);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Token请求异常: " + e.getMessage(), e);
        }

        return null;
    }

    // =============== 录音相关方法 ===============
    private void startRecording() {
        isRecording = true;
        recordStartTime = System.currentTimeMillis();

        runOnUiThread(() -> {
            bt_opencloseSpeech.setText("录音中...");
            bt_opencloseSpeech.setBackgroundResource(R.drawable.shape_button_red);
        });

        // 震动反馈
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) == PackageManager.PERMISSION_GRANTED) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(100);
            }
        }

        // 计算缓冲区大小
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        if (bufferSize <= 0) {
            Log.e(TAG, "缓冲区大小计算失败");
            runOnUiThread(() -> {
                Toast.makeText(this, "录音设备不支持", Toast.LENGTH_SHORT).show();
                resetButtonState();
            });
            return;
        }

        // 确保缓冲区大小是2的倍数
        if (bufferSize % 2 != 0) {
            bufferSize += 1;
        }

        Log.d(TAG, "🎙️ 录音参数 - 采样率: " + SAMPLE_RATE + ", 缓冲区: " + bufferSize);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            runOnUiThread(() -> {
                Toast.makeText(this, "录音权限未授予", Toast.LENGTH_SHORT).show();
                resetButtonState();
            });
            return;
        }

        // 删除旧文件
        if (audioFile.exists()) {
            audioFile.delete();
        }

        // 初始化AudioRecord
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.startRecording();

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[2048];
                try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                    while (isRecording) {
                        int read = audioRecord.read(buffer, 0, buffer.length);
                        if (read > 0) {
                            fos.write(buffer, 0, read);
                        } else if (read == AudioRecord.ERROR_INVALID_OPERATION ||
                                read == AudioRecord.ERROR_BAD_VALUE) {
                            Log.e(TAG, "录音读取错误: " + read);
                            break;
                        }
                    }
                    fos.flush();
                } catch (IOException e) {
                    Log.e(TAG, "录音错误: " + e.getMessage(), e);
                }
            });
            recordingThread.start();

        } else {
            Log.e(TAG, "音频录制初始化失败，状态码: " + audioRecord.getState());
            runOnUiThread(() -> {
                Toast.makeText(this, "录音初始化失败", Toast.LENGTH_SHORT).show();
                resetButtonState();
            });
        }
    }

    private void stopRecording() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
            } catch (Exception e) {
                Log.e(TAG, "停止录音错误", e);
            }
            audioRecord.release();
            audioRecord = null;
        }
        if (recordingThread != null && recordingThread.isAlive()) {
            try {
                recordingThread.join(2000);
            } catch (InterruptedException e) {
                Log.e(TAG, "等待录音线程结束错误: " + e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 读取音频文件
     */
    private byte[] readAudioFile() {
        if (!audioFile.exists()) {
            Log.e(TAG, "音频文件不存在: " + audioFile.getAbsolutePath());
            return null;
        }

        if (audioFile.length() < 1024) {
            Log.e(TAG, "音频文件过小: " + audioFile.length() + "字节");
            return null;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (FileInputStream fis = new FileInputStream(audioFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            byte[] data = bos.toByteArray();
            Log.d(TAG, "读取音频数据完成，长度: " + data.length + "字节");

            if (data.length == 0) {
                Log.e(TAG, "音频数据为空");
                return null;
            }
            return data;
        } catch (IOException e) {
            Log.e(TAG, "读取音频文件错误: " + e.getMessage(), e);
            return null;
        }
    }

    // =============== DeepSeek相关方法 ===============
    private void sendTextToDeepSeek(String text) {
        String escapedText = text.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\n" +
                "        \"model\": \"deepseek-chat\",\n" +
                "        \"messages\": [\n" +
                "          {\"role\": \"system\", \"content\": \"你是一个贴心的农业小助手，回答有关羊肚菌的问题，回答简洁易懂，字数控制在200字以内\"},\n" +
                "          {\"role\": \"user\", \"content\": \"" + escapedText + "\"}\n" +
                "        ],\n" +
                "        \"stream\": false\n" +
                "      }");

        Request request = new Request.Builder()
                .url("https://api.deepseek.com/v1/chat/completions")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + APIkey)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "DeepSeek API调用失败: " + e.toString(), e);
                runOnUiThread(() -> {
                    Toast.makeText(MineDeepseek.this, "DeepSeek API调用失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButtonState();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    String resultContent = ""; // 声明为局部变量
                    if (response.isSuccessful() && responseBody != null) {
                        JSONObject object = JSON.parseObject(responseBody.string());
                        if (object != null && object.containsKey("choices")) {
                            JSONObject choices1 = object.getJSONArray("choices").getJSONObject(0);
                            JSONObject message = choices1.getJSONObject("message");
                            // 赋值给局部变量
                            resultContent = message.containsKey("content") ? message.getString("content") : "";
                        }
                    }
                    // 修复：将变量声明为final
                    final String finalSpeekstring = resultContent;
                    final String finalUserText = text;
                    runOnUiThread(() -> {
                        getTv_sendspeeddata.setText(finalSpeekstring);
                        addChatToHistory(finalUserText, finalSpeekstring);
                        resetButtonState();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "处理DeepSeek响应错误: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(MineDeepseek.this, "处理响应时出错：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetButtonState();
                    });
                }
            }
        });
    }

    private void addChatToHistory(String userText, String aiText) {
        chatHistory += "我：" + userText + "\n\n";
        chatHistory += "智能助手：" + aiText + "\n\n————————————————————\n\n";
        
        // 保存到SharedPreferences
        saveChatHistory(chatHistory);
        
        tv_history_record.setText(chatHistory);
        tv_history_record.post(() -> tv_history_record.scrollTo(0, tv_history_record.getBottom()));
    }

    private void loadChatHistory() {
        chatHistory = sp.getString(CHAT_HISTORY_KEY, "");
        if (chatHistory == null || chatHistory.isEmpty()) {
            chatHistory = "暂无历史记录";
        }
        tv_history_record.setText(chatHistory);
    }

    private void saveChatHistory(String history) {
        // 限制历史记录长度，避免存储过大
        if (history.length() > MAX_HISTORY_LENGTH) {
            history = history.substring(history.length() - MAX_HISTORY_LENGTH);
        }
        sp.edit().putString(CHAT_HISTORY_KEY, history).apply();
    }

    private void resetButtonState() {
        runOnUiThread(() -> {
            bt_opencloseSpeech.setText("按住说话");
            bt_opencloseSpeech.setBackgroundResource(R.drawable.shape_button_green);
            bt_opencloseSpeech.setEnabled(true);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 关闭WebSocket连接
        if (currentWebSocket != null) {
            currentWebSocket.close(1000, "页面销毁");
        }

        // 关闭线程池
        if (audioSendExecutor != null) {
            audioSendExecutor.shutdown();
            try {
                if (!audioSendExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    audioSendExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                audioSendExecutor.shutdownNow();
            }
        }

        if (isRecording) {
            stopRecording();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startRecording();
            } else {
                Toast.makeText(this, "需要录音和震动权限才能正常使用语音功能", Toast.LENGTH_SHORT).show();
                resetButtonState();
            }
        }
    }
}
