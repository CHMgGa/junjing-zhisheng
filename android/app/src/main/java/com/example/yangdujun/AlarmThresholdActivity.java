package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.utils.ThresholdStorageUtils;
import com.example.yangdujun.utils.ManagePageStorage;
import com.example.yangdujun.utils.GreenhouseHomeStorage;

import java.util.ArrayList;
import java.util.List;

public class AlarmThresholdActivity extends AppCompatActivity {
    // 温度阈值控件
    private TextView tvTempMin, tvTempMax;
    // 湿度阈值控件
    private TextView tvHumMin, tvHumMax;
    // 二氧化碳阈值控件
    private TextView tvCo2Min, tvCo2Max;
    // 光照阈值控件
    private TextView tvLightMin, tvLightMax;
    // 空气湿度阈值控件
    private TextView tvAirHumMin, tvAirHumMax;
    // 土壤温度阈值控件
    private TextView tvSoilTempMin, tvSoilTempMax;
    // 氧气浓度阈值控件

    // PH值阈值控件
    private TextView tvPhMin, tvPhMax;
    private String greenhouseId; // 当前大棚ID
    private List<String> greenhouseList = new ArrayList<>();
    private List<String> greenhouseIdList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.alarm_threshold);
        initView();
        // 初始化存储工具
        new ManagePageStorage();
        GreenhouseHomeStorage greenhouseHomeStorage = new GreenhouseHomeStorage(this);
        
        // 从存储中初始化大棚列表
        greenhouseList = greenhouseHomeStorage.getGreenhouseList();
        greenhouseIdList = greenhouseHomeStorage.getGreenhouseIdList();
        
        // 初始化大棚选择Spinner
        initGreenhouseSpinner();
        
        // 接收传递过来的大棚ID
        Intent intent = getIntent();
        if (intent != null) {
            greenhouseId = intent.getStringExtra("greenhouseId");
        }
        
        // 设置默认选中的大棚
        if (greenhouseId != null && !greenhouseId.isEmpty()) {
            Spinner spinner = findViewById(R.id.spinner_greenhouse);
            int position = greenhouseIdList.indexOf(greenhouseId);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
        
        loadSavedThresholds();
        initBtnClick();
    }

    private void initGreenhouseSpinner() {
        Spinner spinner = findViewById(R.id.spinner_greenhouse);
        if (spinner != null) {
            // 确保greenhouseList不为空
            if (greenhouseList == null) {
                greenhouseList = new ArrayList<>();
            }
            if (greenhouseList.isEmpty()) {
                greenhouseList.add("请选择大棚");
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    R.layout.custom_spinner_item, greenhouseList);
            adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position > 0 && position < greenhouseIdList.size() && greenhouseIdList != null) {
                        greenhouseId = greenhouseIdList.get(position);
                        // 重新加载该大棚的阈值
                        loadSavedThresholds();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private void initView() {
        // 返回按钮
        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                // 跳回ManagePage
                Intent intent = new Intent(AlarmThresholdActivity.this, ManagePage.class);
                if (greenhouseId != null) {
                    intent.putExtra("greenhouseId", greenhouseId);
                }
                startActivity(intent);
                finish();
            });
        }

        // 保存按钮
        Button btnSave = findViewById(R.id.btn_save);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveThresholds());
        }

        // 空气温度
        tvTempMin = findViewById(R.id.tv_temp_min);
        tvTempMax = findViewById(R.id.tv_temp_max);
        // 土壤湿度
        tvHumMin = findViewById(R.id.tv_hum_min);
        tvHumMax = findViewById(R.id.tv_hum_max);
        // 二氧化碳浓度
        tvCo2Min = findViewById(R.id.tv_co2_min);
        tvCo2Max = findViewById(R.id.tv_co2_max);
        // 光照强度
        tvLightMin = findViewById(R.id.tv_light_min);
        tvLightMax = findViewById(R.id.tv_light_max);
        // 空气湿度
        tvAirHumMin = findViewById(R.id.tv_air_hum_min);
        tvAirHumMax = findViewById(R.id.tv_air_hum_max);
        // 土壤温度
        tvSoilTempMin = findViewById(R.id.tv_soil_temp_min);
        tvSoilTempMax = findViewById(R.id.tv_soil_temp_max);

        // PH值
        tvPhMin = findViewById(R.id.tv_ph_min);
        tvPhMax = findViewById(R.id.tv_ph_max);
    }

    private void loadSavedThresholds() {
        if (greenhouseId == null || greenhouseId.isEmpty()) {
            return;
        }
        
        JSONObject thresholds = ThresholdStorageUtils.getThresholdsForGreenhouse(this, greenhouseId);
        if (thresholds == null) {
            thresholds = new JSONObject();
        }
        
        try {
            // 加载空气温度告警阈值
            if (tvTempMin != null) tvTempMin.setText(String.valueOf(thresholds.getIntValue("tempMin")));
            if (tvTempMax != null) tvTempMax.setText(String.valueOf(thresholds.getIntValue("tempMax")));
            // 加载土壤湿度告警阈值
            if (tvHumMin != null) tvHumMin.setText(String.valueOf(thresholds.getIntValue("humMin")));
            if (tvHumMax != null) tvHumMax.setText(String.valueOf(thresholds.getIntValue("humMax")));
            // 加载二氧化碳浓度告警阈值
            if (tvCo2Min != null) tvCo2Min.setText(String.valueOf(thresholds.getIntValue("co2Min")));
            if (tvCo2Max != null) tvCo2Max.setText(String.valueOf(thresholds.getIntValue("co2Max")));
            // 加载光照强度告警阈值
            if (tvLightMin != null) tvLightMin.setText(String.valueOf(thresholds.getIntValue("lightMin")));
            if (tvLightMax != null) tvLightMax.setText(String.valueOf(thresholds.getIntValue("lightMax")));
            // 加载空气湿度告警阈值
            if (tvAirHumMin != null) tvAirHumMin.setText(String.valueOf(thresholds.getIntValue("airHumMin")));
            if (tvAirHumMax != null) tvAirHumMax.setText(String.valueOf(thresholds.getIntValue("airHumMax")));
            // 加载土壤温度告警阈值
            if (tvSoilTempMin != null) tvSoilTempMin.setText(String.valueOf(thresholds.getIntValue("soilTempMin")));
            if (tvSoilTempMax != null) tvSoilTempMax.setText(String.valueOf(thresholds.getIntValue("soilTempMax")));
            // 加载PH值告警阈值
            if (tvPhMin != null) tvPhMin.setText(String.valueOf(thresholds.getIntValue("phMin")));
            if (tvPhMax != null) tvPhMax.setText(String.valueOf(thresholds.getIntValue("phMax")));
        } catch (Exception e) {
            e.printStackTrace();
            // 如果发生异常，设置默认值
            setDefaultThresholds();
        }
    }
    
    private void setDefaultThresholds() {
        // 设置默认阈值
        if (tvTempMin != null) tvTempMin.setText("0");
        if (tvTempMax != null) tvTempMax.setText("35");
        if (tvHumMin != null) tvHumMin.setText("0");
        if (tvHumMax != null) tvHumMax.setText("100");
        if (tvCo2Min != null) tvCo2Min.setText("400");
        if (tvCo2Max != null) tvCo2Max.setText("2000");
        if (tvLightMin != null) tvLightMin.setText("0");
        if (tvLightMax != null) tvLightMax.setText("50000");
        if (tvAirHumMin != null) tvAirHumMin.setText("0");
        if (tvAirHumMax != null) tvAirHumMax.setText("100");
        if (tvSoilTempMin != null) tvSoilTempMin.setText("0");
        if (tvSoilTempMax != null) tvSoilTempMax.setText("40");

        if (tvPhMin != null) tvPhMin.setText("5");
        if (tvPhMax != null) tvPhMax.setText("9");
    }

    private void saveThresholds() {
        if (greenhouseId == null || greenhouseId.isEmpty()) {
            Toast.makeText(this, "请选择大棚", Toast.LENGTH_SHORT).show();
            return;
        }
        
        JSONObject thresholds = new JSONObject();
        
        try {
            // 保存空气温度告警阈值
            if (tvTempMin != null && tvTempMin.getText() != null) thresholds.put("tempMin", Integer.parseInt(tvTempMin.getText().toString()));
            if (tvTempMax != null && tvTempMax.getText() != null) thresholds.put("tempMax", Integer.parseInt(tvTempMax.getText().toString()));
            // 保存土壤湿度告警阈值
            if (tvHumMin != null && tvHumMin.getText() != null) thresholds.put("humMin", Integer.parseInt(tvHumMin.getText().toString()));
            if (tvHumMax != null && tvHumMax.getText() != null) thresholds.put("humMax", Integer.parseInt(tvHumMax.getText().toString()));
            // 保存二氧化碳浓度告警阈值
            if (tvCo2Min != null && tvCo2Min.getText() != null) thresholds.put("co2Min", Integer.parseInt(tvCo2Min.getText().toString()));
            if (tvCo2Max != null && tvCo2Max.getText() != null) thresholds.put("co2Max", Integer.parseInt(tvCo2Max.getText().toString()));
            // 保存光照强度告警阈值
            if (tvLightMin != null && tvLightMin.getText() != null) thresholds.put("lightMin", Integer.parseInt(tvLightMin.getText().toString()));
            if (tvLightMax != null && tvLightMax.getText() != null) thresholds.put("lightMax", Integer.parseInt(tvLightMax.getText().toString()));
            // 保存空气湿度告警阈值
            if (tvAirHumMin != null && tvAirHumMin.getText() != null) thresholds.put("airHumMin", Integer.parseInt(tvAirHumMin.getText().toString()));
            if (tvAirHumMax != null && tvAirHumMax.getText() != null) thresholds.put("airHumMax", Integer.parseInt(tvAirHumMax.getText().toString()));
            // 保存土壤温度告警阈值
            if (tvSoilTempMin != null && tvSoilTempMin.getText() != null) thresholds.put("soilTempMin", Integer.parseInt(tvSoilTempMin.getText().toString()));
            if (tvSoilTempMax != null && tvSoilTempMax.getText() != null) thresholds.put("soilTempMax", Integer.parseInt(tvSoilTempMax.getText().toString()));
            // 保存氧气浓度告警阈值
               // 保存PH值告警阈值
            if (tvPhMin != null && tvPhMin.getText() != null) thresholds.put("phMin", Integer.parseInt(tvPhMin.getText().toString()));
            if (tvPhMax != null && tvPhMax.getText() != null) thresholds.put("phMax", Integer.parseInt(tvPhMax.getText().toString()));
            
            // 保存到存储（使用ThresholdStorageUtils，按大棚ID保存）
            ThresholdStorageUtils.saveThresholdsForGreenhouse(this, greenhouseId, thresholds);
            Toast.makeText(this, "阈值保存成功", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            e.printStackTrace();
            Toast.makeText(this, "数据格式错误，请检查输入", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }

    private void initBtnClick() {
        // 温度最低温增减
        View btnTempMinSub = findViewById(R.id.btn_temp_min_sub);
        if (btnTempMinSub != null) {
            btnTempMinSub.setOnClickListener(v -> changeValue(tvTempMin, false));
        }
        View btnTempMinAdd = findViewById(R.id.btn_temp_min_add);
        if (btnTempMinAdd != null) {
            btnTempMinAdd.setOnClickListener(v -> changeValue(tvTempMin, true));
        }
        // 温度最高温增减
        View btnTempMaxSub = findViewById(R.id.btn_temp_max_sub);
        if (btnTempMaxSub != null) {
            btnTempMaxSub.setOnClickListener(v -> changeValue(tvTempMax, false));
        }
        View btnTempMaxAdd = findViewById(R.id.btn_temp_max_add);
        if (btnTempMaxAdd != null) {
            btnTempMaxAdd.setOnClickListener(v -> changeValue(tvTempMax, true));
        }

        // 湿度最小值增减
        View btnHumMinSub = findViewById(R.id.btn_hum_min_sub);
        if (btnHumMinSub != null) {
            btnHumMinSub.setOnClickListener(v -> changeValue(tvHumMin, false));
        }
        View btnHumMinAdd = findViewById(R.id.btn_hum_min_add);
        if (btnHumMinAdd != null) {
            btnHumMinAdd.setOnClickListener(v -> changeValue(tvHumMin, true));
        }
        // 湿度最大值增减
        View btnHumMaxSub = findViewById(R.id.btn_hum_max_sub);
        if (btnHumMaxSub != null) {
            btnHumMaxSub.setOnClickListener(v -> changeValue(tvHumMax, false));
        }
        View btnHumMaxAdd = findViewById(R.id.btn_hum_max_add);
        if (btnHumMaxAdd != null) {
            btnHumMaxAdd.setOnClickListener(v -> changeValue(tvHumMax, true));
        }

        // 二氧化碳最小值增减
        View btnCo2MinSub = findViewById(R.id.btn_co2_min_sub);
        if (btnCo2MinSub != null) {
            btnCo2MinSub.setOnClickListener(v -> changeValue(tvCo2Min, false));
        }
        View btnCo2MinAdd = findViewById(R.id.btn_co2_min_add);
        if (btnCo2MinAdd != null) {
            btnCo2MinAdd.setOnClickListener(v -> changeValue(tvCo2Min, true));
        }
        // 二氧化碳最大值增减
        View btnCo2MaxSub = findViewById(R.id.btn_co2_max_sub);
        if (btnCo2MaxSub != null) {
            btnCo2MaxSub.setOnClickListener(v -> changeValue(tvCo2Max, false));
        }
        View btnCo2MaxAdd = findViewById(R.id.btn_co2_max_add);
        if (btnCo2MaxAdd != null) {
            btnCo2MaxAdd.setOnClickListener(v -> changeValue(tvCo2Max, true));
        }

        // 光照最小值增减
        View btnLightMinSub = findViewById(R.id.btn_light_min_sub);
        if (btnLightMinSub != null) {
            btnLightMinSub.setOnClickListener(v -> changeValue(tvLightMin, false));
        }
        View btnLightMinAdd = findViewById(R.id.btn_light_min_add);
        if (btnLightMinAdd != null) {
            btnLightMinAdd.setOnClickListener(v -> changeValue(tvLightMin, true));
        }
        // 光照最大值增减
        View btnLightMaxSub = findViewById(R.id.btn_light_max_sub);
        if (btnLightMaxSub != null) {
            btnLightMaxSub.setOnClickListener(v -> changeValue(tvLightMax, false));
        }
        View btnLightMaxAdd = findViewById(R.id.btn_light_max_add);
        if (btnLightMaxAdd != null) {
            btnLightMaxAdd.setOnClickListener(v -> changeValue(tvLightMax, true));
        }

        // 空气湿度最小值增减
        View btnAirHumMinSub = findViewById(R.id.btn_air_hum_min_sub);
        if (btnAirHumMinSub != null) {
            btnAirHumMinSub.setOnClickListener(v -> changeValue(tvAirHumMin, false));
        }
        View btnAirHumMinAdd = findViewById(R.id.btn_air_hum_min_add);
        if (btnAirHumMinAdd != null) {
            btnAirHumMinAdd.setOnClickListener(v -> changeValue(tvAirHumMin, true));
        }
        // 空气湿度最大值增减
        View btnAirHumMaxSub = findViewById(R.id.btn_air_hum_max_sub);
        if (btnAirHumMaxSub != null) {
            btnAirHumMaxSub.setOnClickListener(v -> changeValue(tvAirHumMax, false));
        }
        View btnAirHumMaxAdd = findViewById(R.id.btn_air_hum_max_add);
        if (btnAirHumMaxAdd != null) {
            btnAirHumMaxAdd.setOnClickListener(v -> changeValue(tvAirHumMax, true));
        }

        // 土壤温度最小值增减
        View btnSoilTempMinSub = findViewById(R.id.btn_soil_temp_min_sub);
        if (btnSoilTempMinSub != null) {
            btnSoilTempMinSub.setOnClickListener(v -> changeValue(tvSoilTempMin, false));
        }
        View btnSoilTempMinAdd = findViewById(R.id.btn_soil_temp_min_add);
        if (btnSoilTempMinAdd != null) {
            btnSoilTempMinAdd.setOnClickListener(v -> changeValue(tvSoilTempMin, true));
        }
        // 土壤温度最大值增减
        View btnSoilTempMaxSub = findViewById(R.id.btn_soil_temp_max_sub);
        if (btnSoilTempMaxSub != null) {
            btnSoilTempMaxSub.setOnClickListener(v -> changeValue(tvSoilTempMax, false));
        }
        View btnSoilTempMaxAdd = findViewById(R.id.btn_soil_temp_max_add);
        if (btnSoilTempMaxAdd != null) {
            btnSoilTempMaxAdd.setOnClickListener(v -> changeValue(tvSoilTempMax, true));
        }


        // PH值最小值增减
        View btnPhMinSub = findViewById(R.id.btn_ph_min_sub);
        if (btnPhMinSub != null) {
            btnPhMinSub.setOnClickListener(v -> changeValue(tvPhMin, false));
        }
        View btnPhMinAdd = findViewById(R.id.btn_ph_min_add);
        if (btnPhMinAdd != null) {
            btnPhMinAdd.setOnClickListener(v -> changeValue(tvPhMin, true));
        }
        // PH值最大值增减
        View btnPhMaxSub = findViewById(R.id.btn_ph_max_sub);
        if (btnPhMaxSub != null) {
            btnPhMaxSub.setOnClickListener(v -> changeValue(tvPhMax, false));
        }
        View btnPhMaxAdd = findViewById(R.id.btn_ph_max_add);
        if (btnPhMaxAdd != null) {
            btnPhMaxAdd.setOnClickListener(v -> changeValue(tvPhMax, true));
        }
    }

    // 通用数值增减方法
    private void changeValue(TextView tv, boolean isAdd) {
        if (tv == null || tv.getText() == null) {
            return;
        }
        try {
            int current = Integer.parseInt(tv.getText().toString());
            if (isAdd) {
                tv.setText(String.valueOf(current + 1));
            } else {
                if (current > 0) { // 防止数值为负
                    tv.setText(String.valueOf(current - 1));
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
}

