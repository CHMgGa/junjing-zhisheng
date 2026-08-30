package com.example.yangdujun;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.utils.AlarmRecordStorageUtils;
import com.example.yangdujun.utils.AsyncTaskUtils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AlarmRecordActivity extends AppCompatActivity {
    private TextView btn_back, btnUnhandled, btnHandled;
    private LinearLayout layoutAlarmList;
    private ProgressBar progressBar;
    private String greenhouseId; // 当前大棚ID

    // 告警数据模型
    public static class Alarm implements Serializable {
        String alarmId;
        String greenhouse;
        String device;
        String content;
        String time;
        boolean isHandled;
        long timestamp;

        public Alarm(String alarmId, String greenhouse, String device, String content, String time, boolean isHandled, long timestamp) {
            this.alarmId = alarmId;
            this.greenhouse = greenhouse;
            this.device = device;
            this.content = content;
            this.time = time;
            this.isHandled = isHandled;
            this.timestamp = timestamp;
        }
    }

    private ArrayList<Alarm> unhandledAlarms = new ArrayList<>();
    private ArrayList<Alarm> handledAlarms = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_record);
        initViews();
        // 接收传递过来的大棚ID
        Intent intent = getIntent();
        if (intent != null) {
            greenhouseId = intent.getStringExtra("greenhouseId");
        }
        // 初始化标签状态
        setTabStatus(true);
        btnUnhandled.setSelected(true);
        // 加载告警数据
        loadAlarmData();
        initTabClickListener();
        initBackClickListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 重新加载告警数据
        loadAlarmData();
    }

    private void initViews() {
        btn_back = findViewById(R.id.btn_back);
        btnUnhandled = findViewById(R.id.btn_unhandled);
        btnHandled = findViewById(R.id.btn_handled);
        layoutAlarmList = findViewById(R.id.layout_alarm_list);
        progressBar = findViewById(R.id.progress_bar);
        
        // 检查关键视图是否为空
        if (layoutAlarmList == null) {
            throw new IllegalStateException("layout_alarm_list not found in layout");
        }
    }

    // 加载告警数据
    private void loadAlarmData() {
        // 检查Activity是否有效
        if (isFinishing() || isDestroyed()) {
            return;
        }
        
        // 显示加载进度
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        // 清空现有数据
        unhandledAlarms.clear();
        handledAlarms.clear();
        
        // 异步加载告警数据
        AlarmRecordStorageUtils.getAlarmRecordsAsync(this, new AsyncTaskUtils.Callback<JSONArray>() {
            @Override
            public void onComplete(JSONArray alarmRecords) {
                // 检查Activity是否有效
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                
                // 检查alarmRecords是否为空
                if (alarmRecords == null) {
                    // 如果没有告警记录，添加一些默认数据
                    if (unhandledAlarms.isEmpty() && handledAlarms.isEmpty()) {
                        addDefaultAlarmData();
                    } else {
                        // 数据加载完成，更新UI
                        updateAlarmListUI();
                    }
                    return;
                }
                
                // 处理加载的数据
                for (int i = 0; i < alarmRecords.size(); i++) {
                    JSONObject alarmObj = alarmRecords.getJSONObject(i);
                    if (alarmObj == null) continue;
                    
                    boolean isHandled = alarmObj.getBooleanValue("isHandled");
                    Alarm alarm = new Alarm(
                            alarmObj.getString("alarmId"),
                            alarmObj.getString("greenhouse"),
                            alarmObj.getString("device"),
                            alarmObj.getString("content"),
                            alarmObj.getString("time"),
                            isHandled,
                            alarmObj.getLongValue("timestamp")
                    );
                    if (isHandled) {
                        handledAlarms.add(alarm);
                    } else {
                        unhandledAlarms.add(alarm);
                    }
                }
                
                // 如果没有告警记录，添加一些默认数据
                if (unhandledAlarms.isEmpty() && handledAlarms.isEmpty()) {
                    addDefaultAlarmData();
                } else {
                    // 数据加载完成，更新UI
                    updateAlarmListUI();
                }
            }
            
            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                // 检查Activity是否有效
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                // 加载失败，隐藏进度条
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
    
    // 更新告警列表UI
    private void updateAlarmListUI() {
        // 隐藏加载进度
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        
        // 根据当前选中的标签重新显示数据
        if (btnUnhandled.isSelected()) {
            showUnhandledAlarms();
        } else {
            showHandledAlarms();
        }
    }

    // 添加默认告警数据
    private void addDefaultAlarmData() {
        // 创建默认未处理告警
        JSONObject alarm1 = new JSONObject();
        alarm1.put("alarmId", "alarm_" + System.currentTimeMillis());
        alarm1.put("greenhouse", "1#大棚");
        alarm1.put("device", "温湿度检测仪");
        alarm1.put("content", "温度过高告警");
        alarm1.put("time", getCurrentTime());
        alarm1.put("isHandled", false);
        alarm1.put("timestamp", System.currentTimeMillis());
        AlarmRecordStorageUtils.saveAlarmRecord(this, alarm1);
        
        JSONObject alarm2 = new JSONObject();
        alarm2.put("alarmId", "alarm_" + (System.currentTimeMillis() + 1));
        alarm2.put("greenhouse", "2#大棚");
        alarm2.put("device", "光照强度检测仪");
        alarm2.put("content", "光强过强告警");
        alarm2.put("time", getCurrentTime());
        alarm2.put("isHandled", false);
        alarm2.put("timestamp", System.currentTimeMillis());
        AlarmRecordStorageUtils.saveAlarmRecord(this, alarm2);
        
        // 创建默认已处理告警
        JSONObject alarm3 = new JSONObject();
        alarm3.put("alarmId", "alarm_" + (System.currentTimeMillis() + 2));
        alarm3.put("greenhouse", "3#大棚");
        alarm3.put("device", "温度传感器");
        alarm3.put("content", "温度过高告警");
        alarm3.put("time", getCurrentTime());
        alarm3.put("isHandled", true);
        alarm3.put("timestamp", System.currentTimeMillis());
        AlarmRecordStorageUtils.saveAlarmRecord(this, alarm3);
        
        // 延迟一点时间后重新加载数据，确保数据已保存
        new android.os.Handler().postDelayed(() -> {
            loadAlarmData();
        }, 100);
    }

    // 获取当前时间
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    // 显示未处理告警
    private void showUnhandledAlarms() {
        layoutAlarmList.removeAllViews();
        for (Alarm alarm : unhandledAlarms) {
            addAlarmItem(alarm);
        }
    }

    // 显示已处理告警
    private void showHandledAlarms() {
        layoutAlarmList.removeAllViews();
        for (Alarm alarm : handledAlarms) {
            addAlarmItem(alarm);
        }
    }

    // 添加告警条目
    private void addAlarmItem(Alarm alarm) {
        // 正确的布局加载方式【修复点击事件绑定失败的核心】
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_alarm_record, null, false);

        // 绑定控件
        TextView tvGreenhouse = itemView.findViewById(R.id.tv_greenhouse);
        TextView tvDevice = itemView.findViewById(R.id.tv_device);
        TextView tvContent = itemView.findViewById(R.id.tv_alarm_content);
        TextView tvTime = itemView.findViewById(R.id.tv_alarm_time);
        TextView tvStatus = itemView.findViewById(R.id.tv_status);
        LinearLayout layoutAlarmItem = itemView.findViewById(R.id.layout_alarm_item);

        // 赋值数据
        tvGreenhouse.setText(alarm.greenhouse);
        tvDevice.setText(alarm.device);
        tvContent.setText(alarm.content);
        tvTime.setText(alarm.time);
        // 设置状态样式
        if (alarm.isHandled) {
            tvStatus.setText("已处理");
            tvStatus.setTextColor(Color.parseColor("#4CAF50")); // 深绿色字体
            tvStatus.setBackgroundColor(Color.parseColor("#E8F5E8")); // 浅绿色框
        } else {
            tvStatus.setText("未处理");
            tvStatus.setTextColor(Color.parseColor("#000000")); // 黑色字体
            tvStatus.setBackgroundColor(Color.parseColor("#FB1717")); // 红色框
        }

        // 条目点击事件【这里绝对能触发了！】
        layoutAlarmItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                if (alarm.isHandled) {
                    // 已处理 → 跳转到告警详情页
                    intent = new Intent(AlarmRecordActivity.this, AlarmDetailActivity.class);
                } else {
                    // 未处理 → 跳转到告警处理页
                    intent = new Intent(AlarmRecordActivity.this, AlarmHandleActivity.class);
                }
                intent.putExtra("alarm_data", alarm);
                if (greenhouseId != null) {
                    intent.putExtra("greenhouseId", greenhouseId);
                }
                startActivity(intent);
            }
        });

        // 最后把条目添加到列表容器
        layoutAlarmList.addView(itemView);
    }

    // 标签切换
    private void initTabClickListener() {
        btnUnhandled.setOnClickListener(v -> {
            setTabStatus(true);
            btnUnhandled.setSelected(true);
            btnHandled.setSelected(false);
            showUnhandledAlarms();
        });
        btnHandled.setOnClickListener(v -> {
            setTabStatus(false);
            btnHandled.setSelected(true);
            btnUnhandled.setSelected(false);
            showHandledAlarms();
        });
    }

    // 设置标签状态
    private void setTabStatus(boolean isUnhandled) {

        if (isUnhandled) {
            // 未处理标签（选中）：白色背景 + 深绿加粗字
            btnUnhandled.setBackgroundColor(Color.WHITE);
            btnUnhandled.setTextColor(Color.parseColor("#2E7D32"));
            btnUnhandled.setTypeface(null, android.graphics.Typeface.BOLD);
            // 已处理标签（未选中）：浅绿背景 + 灰色常规字
            btnHandled.setBackgroundColor(Color.parseColor("#D9E6D5"));
            btnHandled.setTextColor(Color.parseColor("#6E8569"));
            btnHandled.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            // 已处理标签（选中）：白色背景 + 深绿加粗字
            btnHandled.setBackgroundColor(Color.WHITE);
            btnHandled.setTextColor(Color.parseColor("#2E7D32"));
            btnHandled.setTypeface(null, android.graphics.Typeface.BOLD);
            // 未处理标签（未选中）：浅绿背景 + 灰色常规字
            btnUnhandled.setBackgroundColor(Color.parseColor("#D9E6D5"));
            btnUnhandled.setTextColor(Color.parseColor("#6E8569"));
            btnUnhandled.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    // 返回ManagePage
    private void initBackClickListener() {
        btn_back.setOnClickListener(v -> {
            Intent intent = new Intent(AlarmRecordActivity.this, ManagePage.class);
            if (greenhouseId != null) {
                intent.putExtra("greenhouseId", greenhouseId);
            }
            startActivity(intent);
            finish();
        });
    }
}


