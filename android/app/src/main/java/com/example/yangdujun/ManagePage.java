package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.utils.AlarmRecordStorageUtils;

public class ManagePage extends AppCompatActivity {
    private LinearLayout layoutAlarmList;
    private String currentGreenhouseId = "default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage);

        // 获取传递过来的大棚ID
        Intent intent = getIntent();
        if (intent != null) {
            currentGreenhouseId = intent.getStringExtra("greenhouseId");
        }
        
        // 如果没有传递大棚ID，使用默认值
        if (currentGreenhouseId == null) {
            currentGreenhouseId = "default";
        }

        // 获取告警列表容器
        layoutAlarmList = findViewById(R.id.layout_alarm_list);
        if (layoutAlarmList == null) {
            Toast.makeText(this, "页面加载失败，请重试", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 绑定6个按钮并设置点击事件
        bindButtons();
        BottomNavHelper.initBottomNav(this, R.id.ll_nav_manage);
        
        // 加载最新告警记录
        loadLatestAlarmRecords();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 重新加载告警记录，确保显示最新数据
        loadLatestAlarmRecords();
    }
    
    // 加载最新的5条告警记录
    private void loadLatestAlarmRecords() {
        // 检查layoutAlarmList是否为空
        if (layoutAlarmList == null) {
            return;
        }
        
        // 清空现有告警列表
        layoutAlarmList.removeAllViews();
        
        // 获取所有告警记录
        JSONArray allAlarms = AlarmRecordStorageUtils.getAlarmRecords(this);
        
        // 检查allAlarms是否为空
        if (allAlarms == null) {
            View emptyView = LayoutInflater.from(this).inflate(R.layout.item_empty_alarm, null, false);
            layoutAlarmList.addView(emptyView);
            return;
        }
        
        // 限制显示数量为5条
        int displayCount = Math.min(allAlarms.size(), 5);
        
        // 显示最新的告警记录（按时间倒序，即数组的前5条）
        for (int i = 0; i < displayCount; i++) {
            JSONObject alarmObj = allAlarms.getJSONObject(i);
            if (alarmObj != null) {
                addAlarmItem(alarmObj);
            }
        }
        
        // 如果没有告警记录，显示空状态
        if (allAlarms.isEmpty()) {
            View emptyView = LayoutInflater.from(this).inflate(R.layout.item_empty_alarm, null, false);
            layoutAlarmList.addView(emptyView);
        }
    }
    
    // 添加告警条目
    private void addAlarmItem(JSONObject alarmObj) {
        if (alarmObj == null) {
            return;
        }
        
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_alarm_record_manage, null, false);
        if (itemView == null) {
            return;
        }
        
        // 绑定控件
        TextView tvGreenhouse = itemView.findViewById(R.id.tv_greenhouse);
        TextView tvDevice = itemView.findViewById(R.id.tv_device);
        TextView tvContent = itemView.findViewById(R.id.tv_alarm_content);
        TextView tvTime = itemView.findViewById(R.id.tv_alarm_time);
        TextView tvStatus = itemView.findViewById(R.id.tv_status);
        
        // 赋值数据
        String greenhouse = alarmObj.getString("greenhouse");
        String device = alarmObj.getString("device");
        String content = alarmObj.getString("content");
        String time = alarmObj.getString("time");
        boolean isHandled = alarmObj.getBooleanValue("isHandled");
        
        if (tvGreenhouse != null) tvGreenhouse.setText(greenhouse);
        if (tvDevice != null) tvDevice.setText(device);
        if (tvContent != null) tvContent.setText(content);
        if (tvTime != null) tvTime.setText(time);
        
        // 设置状态样式
        if (tvStatus != null) {
            if (isHandled) {
                tvStatus.setText("已处理");
                tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvStatus.setText("未处理");
                tvStatus.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }
        
        // 点击跳转到告警记录页面
        itemView.setOnClickListener(v -> {
            Intent intent = new Intent(ManagePage.this, AlarmRecordActivity.class);
            intent.putExtra("greenhouseId", currentGreenhouseId);
            startActivity(intent);
        });
        
        // 添加到容器
        layoutAlarmList.addView(itemView);
    }

    private void bindButtons() {
        // 1. 设备状态管理
        View btnDeviceStatus = findViewById(R.id.btn_device_status);
        if (btnDeviceStatus != null) {
            btnDeviceStatus.setOnClickListener(v -> {
                Intent intent = new Intent(ManagePage.this, DeviceManageActivity.class);
                intent.putExtra("greenhouseId", currentGreenhouseId);
                startActivity(intent);
                showToast("进入设备状态管理");
            });
        }

        // 2. 告警阈值设置
        View btnAlarmConfig = findViewById(R.id.btn_alarm_config);
        if (btnAlarmConfig != null) {
            btnAlarmConfig.setOnClickListener(v -> {
                Intent intent = new Intent(ManagePage.this, AlarmThresholdActivity.class);
                intent.putExtra("greenhouseId", currentGreenhouseId);
                startActivity(intent);
                showToast("进入告警阈值设置");
            });
        }

        // 3. 历史数据分析
        View btnHistoryAnalysis = findViewById(R.id.btn_history_analysis);
        if (btnHistoryAnalysis != null) {
            btnHistoryAnalysis.setOnClickListener(v -> {
                Intent intent = new Intent(ManagePage.this, HistoryAnalysisActivity.class);
                intent.putExtra("greenhouseId", currentGreenhouseId);
                startActivity(intent);
                showToast("进入历史数据分析");
            });
        }

        // 4. 实时数据分析
        View btnRealTimeAnalysis = findViewById(R.id.btn_real_time_analysis);
        if (btnRealTimeAnalysis != null) {
            btnRealTimeAnalysis.setOnClickListener(v -> {
                Intent intent = new Intent(ManagePage.this, RealTimeAnalysisActivity.class);
                intent.putExtra("greenhouseId", currentGreenhouseId);
                startActivity(intent);
                showToast("进入实时数据分析");
            });
        }

        // 5. 设备调控
        View btnDeviceMonitor = findViewById(R.id.btn_device_monitor);
        if (btnDeviceMonitor != null) {
            btnDeviceMonitor.setOnClickListener(v -> {
                Intent intent = new Intent(ManagePage.this, DeviceMonitorActivity.class);
                intent.putExtra("greenhouseId", currentGreenhouseId);
                startActivity(intent);
                showToast("进入设备调控");
            });
        }

        // 6. 告警记录
        View btnAlarmRecord = findViewById(R.id.btn_alarm_record);
        if (btnAlarmRecord != null) {
            btnAlarmRecord.setOnClickListener(v -> {
                Intent intent = new Intent(ManagePage.this, AlarmRecordActivity.class);
                intent.putExtra("greenhouseId", currentGreenhouseId);
                startActivity(intent);
                showToast("进入告警记录");
            });
        }

    }

    // 统一Toast提示
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}


