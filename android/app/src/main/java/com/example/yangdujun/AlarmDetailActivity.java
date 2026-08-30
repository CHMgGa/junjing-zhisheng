package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.utils.AlarmRecordStorageUtils;
import com.example.yangdujun.utils.AsyncTaskUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmDetailActivity extends AppCompatActivity {

    private Button btn_back;
    private TextView tvOrderId, tvAlarmPos, tvAlarmTime, tvHandler, tvPhone, tvContent, tvPublishTime, tvCompleteTime, tvOperate;
    private String greenhouseId; // 当前大棚ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_detail);
        initViews();
        // 接收传递过来的大棚ID
        Intent intent = getIntent();
        if (intent != null) {
            greenhouseId = intent.getStringExtra("greenhouseId");
        }
        initData();
        initBackListener();
    }

    private void initViews() {
        btn_back = findViewById(R.id.btn_back);
        tvOrderId = findViewById(R.id.tv_order_id);
        tvAlarmPos = findViewById(R.id.tv_alarm_pos);
        tvAlarmTime = findViewById(R.id.tv_alarm_time);
        tvHandler = findViewById(R.id.tv_handler);
        tvPhone = findViewById(R.id.tv_phone);
        tvContent = findViewById(R.id.tv_content);
        tvPublishTime = findViewById(R.id.tv_publish_time);
        tvCompleteTime = findViewById(R.id.tv_complete_time);
        tvOperate = findViewById(R.id.tv_operate);
    }

    private void initData() {
        // 获取告警数据
        AlarmRecordActivity.Alarm alarm = (AlarmRecordActivity.Alarm) getIntent().getSerializableExtra("alarm_data");
        if (alarm != null) {
            // 填充基础信息
            if (tvAlarmPos != null) {
                tvAlarmPos.setText(alarm.greenhouse + alarm.device);
            }
            if (tvContent != null) {
                tvContent.setText(alarm.content);
            }
            if (tvAlarmTime != null) {
                tvAlarmTime.setText(alarm.time);
            }
            if (tvOrderId != null) {
                tvOrderId.setText(alarm.alarmId);
            }
            
            // 格式化发布时间
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.CHINA);
            if (tvPublishTime != null) {
                tvPublishTime.setText(sdf.format(new Date(alarm.timestamp)));
            }
            
            // 从存储中获取完整的告警信息（包括处理人、处理时间等）
            AlarmRecordStorageUtils.getAlarmRecordsAsync(this, new AsyncTaskUtils.Callback<com.alibaba.fastjson.JSONArray>() {
                @Override
                public void onComplete(com.alibaba.fastjson.JSONArray alarmRecords) {
                    if (alarmRecords != null) {
                        for (int i = 0; i < alarmRecords.size(); i++) {
                            JSONObject alarmObj = alarmRecords.getJSONObject(i);
                            if (alarmObj != null && alarmObj.getString("alarmId").equals(alarm.alarmId)) {
                                // 填充处理人信息
                                String handlerName = alarmObj.getString("handlerName");
                                if (handlerName == null || handlerName.isEmpty()) {
                                    handlerName = "未知用户";
                                }
                                if (tvHandler != null) {
                                    tvHandler.setText(handlerName);
                                }
                                
                                // 电话号码（暂时显示默认值，实际项目可从用户信息获取）
                                if (tvPhone != null) {
                                    tvPhone.setText("暂无");
                                }
                                
                                // 处理完成时间
                                long handleTime = alarmObj.getLongValue("handleTime");
                                if (tvCompleteTime != null) {
                                    if (handleTime > 0) {
                                        tvCompleteTime.setText(sdf.format(new Date(handleTime)));
                                    } else {
                                        tvCompleteTime.setText("未知");
                                    }
                                }
                                
                                // 处理操作（根据告警内容生成）
                                String operateText = generateOperateText(alarm.greenhouse, alarm.device, alarm.content);
                                if (tvOperate != null) {
                                    tvOperate.setText(operateText);
                                }
                                
                                break;
                            }
                        }
                    }
                }
                
                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
    
    /**
     * 生成处理操作描述文本
     */
    private String generateOperateText(String greenhouse, String device, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("已处理").append(greenhouse).append(device).append("的");
        
        if (content.contains("温度")) {
            sb.append("温度异常");
        } else if (content.contains("湿度")) {
            sb.append("湿度异常");
        } else if (content.contains("光照")) {
            sb.append("光照异常");
        } else if (content.contains("二氧化碳")) {
            sb.append("二氧化碳浓度异常");
        } else if (content.contains("氧气")) {
            sb.append("氧气浓度异常");
        } else if (content.contains("PH")) {
            sb.append("PH值异常");
        } else {
            sb.append("设备异常");
        }
        
        sb.append("告警，已通过APP进行相应操作");
        
        return sb.toString();
    }

    // 返回AlarmRecordActivity
    private void initBackListener() {
        if (btn_back != null) {
            btn_back.setOnClickListener(v -> {
                Intent intent = new Intent(AlarmDetailActivity.this, AlarmRecordActivity.class);
                if (greenhouseId != null) {
                    intent.putExtra("greenhouseId", greenhouseId);
                }
                startActivity(intent);
                finish();
            });
        }
    }
}


