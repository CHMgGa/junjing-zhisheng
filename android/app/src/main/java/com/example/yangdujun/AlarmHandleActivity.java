package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.yangdujun.model.User;
import com.example.yangdujun.utils.AlarmRecordStorageUtils;
import com.example.yangdujun.utils.UserStorage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AlarmHandleActivity extends AppCompatActivity {

    private Button btn_back, btn_confirm;
    private TextView tvAlarmPos, tvAlarmStrategy, tvAlarmTime, tvAlarmDetail, tvSubmitTime;
    private Spinner spinnerGreenhouse, spinnerDevice, spinnerOperate;
    private String greenhouseId; // 当前大棚ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_handle);
        initViews();
        // 接收传递过来的大棚ID
        Intent intent = getIntent();
        if (intent != null) {
            greenhouseId = intent.getStringExtra("greenhouseId");
        }
        initSpinners();
        initData();
        initListeners();
    }

    private void initViews() {
        btn_back = findViewById(R.id.btn_back);
        btn_confirm = findViewById(R.id.btn_confirm);
        tvAlarmPos = findViewById(R.id.tv_alarm_pos);
        tvAlarmStrategy = findViewById(R.id.tv_alarm_strategy);
        tvAlarmTime = findViewById(R.id.tv_alarm_time);
        tvAlarmDetail = findViewById(R.id.tv_alarm_detail);
        tvSubmitTime = findViewById(R.id.tv_submit_time);
        spinnerGreenhouse = findViewById(R.id.spinner_greenhouse);
        spinnerDevice = findViewById(R.id.spinner_device);
        spinnerOperate = findViewById(R.id.spinner_operate);
    }

    // 初始化下拉选择器
    private void initSpinners() {
        // 大棚选择
        if (spinnerGreenhouse != null) {
            ArrayAdapter<String> greenhouseAdapter = new ArrayAdapter<>(this,
                    R.layout.custom_spinner_item, new String[]{"1#大棚", "2#大棚", "3#大棚", "4#大棚"});
            greenhouseAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
            spinnerGreenhouse.setAdapter(greenhouseAdapter);
            // 获取告警数据
            AlarmRecordActivity.Alarm alarm = (AlarmRecordActivity.Alarm) getIntent().getSerializableExtra("alarm_data");
            if (alarm != null) {
                String alarmGreenhouse = alarm.greenhouse;
                int position = greenhouseAdapter.getPosition(alarmGreenhouse);
                if (position != -1) {
                    spinnerGreenhouse.setSelection(position);
                }
            } else {
                spinnerGreenhouse.setSelection(0);
            }
        }

        // 设备选择
        if (spinnerDevice != null) {
            ArrayAdapter<String> deviceAdapter = new ArrayAdapter<>(this,
                    R.layout.custom_spinner_item, new String[]{"风机", "加湿器", "遮阳帘", "通风口"});
            deviceAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
            spinnerDevice.setAdapter(deviceAdapter);
            spinnerDevice.setSelection(0);
        }

        // 操作选择
        if (spinnerOperate != null) {
            ArrayAdapter<String> operateAdapter = new ArrayAdapter<>(this,
                    R.layout.custom_spinner_item, new String[]{"开启", "关闭", "调节"});
            operateAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
            spinnerOperate.setAdapter(operateAdapter);
            spinnerOperate.setSelection(0);
        }
    }

    private void initData() {
        // 获取告警数据
        AlarmRecordActivity.Alarm alarm = (AlarmRecordActivity.Alarm) getIntent().getSerializableExtra("alarm_data");
        if (alarm != null) {
            if (tvAlarmPos != null) {
                tvAlarmPos.setText(alarm.greenhouse + alarm.device);
            }
            if (tvAlarmStrategy != null) {
                tvAlarmStrategy.setText(alarm.content.replace("告警策略：", ""));
            }
            if (tvAlarmTime != null) {
                tvAlarmTime.setText(alarm.time);
            }
            // 告警详情（示例）
            if (tvAlarmDetail != null) {
                tvAlarmDetail.setText("当前状态" + alarm.greenhouse + "温度已连续2小时超过25℃");
            }
        }
        // 设置当前提交时间
        if (tvSubmitTime != null) {
            tvSubmitTime.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date()));
        }
    }

    private void initListeners() {
        // 返回AlarmRecordActivity
        if (btn_back != null) {
            btn_back.setOnClickListener(v -> {
                Intent intent = new Intent(AlarmHandleActivity.this, AlarmRecordActivity.class);
                if (greenhouseId != null) {
                    intent.putExtra("greenhouseId", greenhouseId);
                }
                startActivity(intent);
                finish();
            });
        }

        // 确认提交
        if (btn_confirm != null) {
            btn_confirm.setOnClickListener(v -> {
                // 获取告警数据
                AlarmRecordActivity.Alarm alarm = (AlarmRecordActivity.Alarm) getIntent().getSerializableExtra("alarm_data");
                if (alarm != null) {
                    // 获取当前用户信息
                    UserStorage userStorage = new UserStorage();
                    User user = userStorage.getUser();
                    String handlerId = user != null ? user.getId() : "unknown";
                    String handlerName = user != null ? user.getNickname() : "未知用户";
                    
                    // 更新告警状态为已处理，并记录处理人信息
                    AlarmRecordStorageUtils.updateAlarmStatus(this, alarm.alarmId, true, handlerId, handlerName);
                }
                Toast.makeText(this, "操作提交成功！", Toast.LENGTH_SHORT).show();
                // 提交后返回告警记录页
                Intent intent = new Intent(AlarmHandleActivity.this, AlarmRecordActivity.class);
                if (greenhouseId != null) {
                    intent.putExtra("greenhouseId", greenhouseId);
                }
                startActivity(intent);
                finish();
            });
        }
    }
}

