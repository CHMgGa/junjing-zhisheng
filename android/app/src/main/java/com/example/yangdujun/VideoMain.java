package com.example.yangdujun;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class VideoMain extends AppCompatActivity {
    private static final String TAG = "VideoMain";
    private static final int REQUEST_CODE_PERMISSIONS = 100;

    // RTSP地址配置 - 你的播放源
    private final String input = "rtsp://112.126.28.203:8554/mystream";
    // 本地摄像头地址（备用）
    // private String localInput = "rtsp://admin:123456@192.168.3.215:554/stream1";

    // UI组件
    private MyVideoView surfaceView;
    private Button btnPlay, btnPause, btnBack, btnStop, btn_back_page; // 【新增】返回上一页按钮
    private TextView tvStatus;

    // 状态控制 - 只管理UI状态，播放状态交给MyVideoView内部维护
    private boolean isPlaying = false;
    private boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.videomain);

        initViews();
        checkAndRequestPermissions();
    }

    private void initViews() {
        // 初始化自定义的MyVideoView - 核心：不再设置任何Surface回调，交给MyVideoView内部处理
        surfaceView = findViewById(R.id.myVV);

        // 初始化按钮
        btnPlay = findViewById(R.id.btn_play);
        btnPause = findViewById(R.id.btn_pause);
        btnBack = findViewById(R.id.btn_back);
        btnStop = findViewById(R.id.button);
        tvStatus = findViewById(R.id.tv_status);
        // 【新增】绑定返回上一页按钮
        btn_back_page = findViewById(R.id.btn_back_page);

        // 设置按钮点击事件 - 所有事件都只调用MyVideoView的方法，无自己的线程和JNI调用
        if (btnPlay != null) btnPlay.setOnClickListener(v -> playVideo());
        if (btnPause != null) btnPause.setOnClickListener(v -> pauseVideo());
        if (btnBack != null) btnBack.setOnClickListener(v -> backToStart());
        if (btnStop != null) btnStop.setOnClickListener(v -> stopVideo());
        // 【新增】返回按钮点击事件
        if (btn_back_page != null) btn_back_page.setOnClickListener(v -> goBackPrePage());

        // 初始化按钮状态
        updateButtonStates();
        updateStatus("就绪");
    }

    // ===================== 【核心修改：完整版 适配Android6-14 权限检查+申请方法 【彻底解决权限不足】 =====================
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // ========== 必加：RTSP是网络视频流，必须申请网络权限 【你的代码漏了这个】 ==========
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.INTERNET);
        }

        // ========== 核心适配：Android 13(API33) 及以上版本 ==========
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android13+ 用【视频专属权限】替代存储权限，官方推荐，必申请
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
        } else {
            // Android6.0 - 12 版本，沿用原来的存储权限即可
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    REQUEST_CODE_PERMISSIONS);
        } else {
            Log.d(TAG, "所有必要权限已授予【包含视频+网络】");
            updateStatus("权限就绪，可播放");
        }
    }

    /**
     * ✅ 播放视频 - 核心优化：同步修改权限检查逻辑，适配Android13+
     */
    private void playVideo() {
        // 检查权限+状态 【同步修改权限判断逻辑，适配Android13+】
        boolean hasVideoPermission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasVideoPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        } else {
            hasVideoPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        // 同时检查网络权限
        boolean hasNetPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;

        if (!hasVideoPermission || !hasNetPermission) {
            showToast("请先授予【视频+网络】权限");
            checkAndRequestPermissions();
            return;
        }

        if (isPlaying && !isPaused) {
            showToast("视频已在播放中");
            return;
        }

        if (isPaused) {
            // 暂停恢复：重新调用播放即可
            isPaused = false;
            surfaceView.startPlay(input);
            updateStatus("继续播放");
            showToast("继续播放");
            updateButtonStates();
            return;
        }

        // 全新播放：调用MyVideoView的封装方法
        isPlaying = true;
        isPaused = false;
        surfaceView.startPlay(input);
        updateStatus("正在播放RTSP流");
        showToast("开始播放");
        updateButtonStates();
    }

    /**
     * ✅ 暂停视频 - 真实有效，暂停后按钮状态正确
     */
    private void pauseVideo() {
        if (!isPlaying) {
            showToast("视频未播放，无法暂停");
            return;
        }

        if (isPaused) {
            showToast("视频已暂停");
            return;
        }

        // 调用MyVideoView的停止方法，标记暂停状态
        surfaceView.stopPlay();
        isPaused = true;
        updateStatus("已暂停");
        showToast("视频已暂停");
        updateButtonStates();
    }

    /**
     * ✅ 停止视频 - 彻底停止，重置所有状态
     */
    private void stopVideo() {
        if (!isPlaying) {
            showToast("视频未播放");
            return;
        }

        // 调用MyVideoView的停止方法，释放所有资源
        surfaceView.stopPlay();
        isPlaying = false;
        isPaused = false;
        updateStatus("播放已停止");
        showToast("视频已停止");
        updateButtonStates();
    }

    /**
     * ✅ 返回重新播放 - 逻辑完整，无卡顿
     */
    private void backToStart() {
        if (!isPlaying) {
            showToast("视频未播放");
            return;
        }

        // 先停止，延迟500ms重新播放，避免Surface未就绪
        surfaceView.stopPlay();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            surfaceView.startPlay(input);
            isPaused = false;
            updateStatus("重新播放");
            showToast("重新播放视频");
            updateButtonStates();
        }, 500);
    }

    /**
     * 更新按钮状态 - UI交互核心，状态精准
     */
    private void updateButtonStates() {
        if (btnPlay != null) {
            btnPlay.setEnabled(!isPlaying || isPaused);
            btnPlay.setText(isPaused ? "继续" : "播放");
        }
        if (btnPause != null) btnPause.setEnabled(isPlaying && !isPaused);
        if (btnBack != null) btnBack.setEnabled(isPlaying);
        if (btnStop != null) btnStop.setEnabled(isPlaying);
    }

    /**
     * 更新播放状态文本
     */
    private void updateStatus(final String status) {
        if (isFinishing() || isDestroyed()) return;
        runOnUiThread(() -> {
            if (tvStatus != null) {
                tvStatus.setText("状态：" + status);
            }
        });
    }

    /**
     * 统一Toast提示，避免重复代码
     */
    private void showToast(final String message) {
        if (isFinishing() || isDestroyed()) return;
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                Toast.makeText(VideoMain.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===================== 【同步修复：权限回调方法 适配Android13+权限】 =====================
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // ========== 修复核心1：判断权限数组是否为空，防止数组越界 ==========
            if (grantResults == null || grantResults.length == 0) {
                showToast("权限申请被取消！部分功能受限");
                updateStatus("权限不足");
                return;
            }

            boolean hasDenied = false;
            boolean neverAskAgain = false;

            // ========== 修复核心2：遍历所有申请的权限，完整判断 ==========
            for (int i = 0; i < permissions.length; i++) {
                String perm = permissions[i];
                int result = grantResults[i];
                if (result != PackageManager.PERMISSION_GRANTED) {
                    hasDenied = true;
                    // ========== 修复核心3：判断用户是否点击【拒绝且不再询问】 ==========
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, perm)) {
                        neverAskAgain = true;
                    }
                }
            }

            // ========== 精准的权限判断逻辑 ==========
            if (!hasDenied) {
                // ✅ 权限授权成功
                showToast("权限申请成功【视频+网络】，可正常播放");
                updateStatus("权限就绪");
            } else {
                if (neverAskAgain) {
                    // ✅ 用户点了拒绝+不再询问 → 引导去设置页手动开启
                    showToast("权限被拒绝且不再询问！请前往【设置-应用权限】手动开启【视频+网络】权限");
                    updateStatus("权限不足");
                    // 跳转APP设置页（可选，非常友好，用户不用自己找）
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                } else {
                    // ✅ 用户只是单纯拒绝，未勾选不再询问
                    showToast("权限不足【视频+网络】！无法播放视频");
                    updateStatus("权限不足");
                }
            }
        }
    }

    // ==================== Activity生命周期管理 - 完美适配，无内存泄漏 ====================
    @Override
    protected void onPause() {
        super.onPause();
        // 切后台自动暂停，体验更佳
        if (isPlaying && !isPaused) {
            pauseVideo();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 页面销毁，释放所有视频资源，核心调用
        if (surfaceView != null) {
            surfaceView.release();
        }
        isPlaying = false;
        isPaused = false;
    }

    // ==================== 【新增】返回上一页核心逻辑 - 必须释放视频流资源 ====================
    private void goBackPrePage() {
        // 1. 如果视频正在播放/暂停，先停止播放 + 释放所有资源 【重中之重，必须加】
        if (isPlaying || isPaused) {
            if (surfaceView != null) {
                surfaceView.stopPlay();  // 停止RTSP视频流拉取和解码
                surfaceView.release();   // 释放Surface、解码器、网络连接等所有底层资源
            }
            isPlaying = false;       // 重置播放状态
            isPaused = false;        // 重置暂停状态
            updateStatus("已释放资源，准备返回");
            showToast("已停止监控，返回上一页");
        }
        // 2. 标准返回上一个页面，不会关闭APP，只是回到跳转过来的页面
        Intent intent = new Intent(VideoMain.this, GreenhouseHomePage.class);
        startActivity(intent);
        finish();
    }
}

