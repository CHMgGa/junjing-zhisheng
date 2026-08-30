package com.example.yangdujun;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.utils.GreenhouseHomeStorage;
import com.example.yangdujun.utils.HistoricalDataStorageUtils;
import com.example.yangdujun.utils.ManagePageStorage;
import com.github.AAChartModel.AAChartCore.AAChartCreator.AAChartModel;
import com.github.AAChartModel.AAChartCore.AAChartCreator.AAChartView;
import com.github.AAChartModel.AAChartCore.AAChartCreator.AASeriesElement;
import com.github.AAChartModel.AAChartCore.AAChartEnum.AAChartType;

import java.util.ArrayList;
import java.util.List;

public class HistoryAnalysisActivity extends AppCompatActivity {

    private ImageView btn_back;
    private Button btn_7d, btn_14d;
    private AAChartView chart_temp, chart_hum, chart_light, chart_gas, chart_ph;
    private Spinner spinnerGreenhouse;
    private ManagePageStorage managePageStorage;
    private GreenhouseHomeStorage greenhouseHomeStorage;
    private String currentGreenhouseId = "default";
    private List<String> greenhouseList = new ArrayList<>();
    private List<String> greenhouseIdList = new ArrayList<>();
    private int currentDayCount = 7;
    
    // 控制是否显示伪造数据
    private boolean useFakeData = true; // true: 显示伪造数据, false: 显示真实数据

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_analysis);

        managePageStorage = new ManagePageStorage();
        greenhouseHomeStorage = new GreenhouseHomeStorage(this);
        
        initViews();
        initChartStyle();
        initGreenhouseSpinner();
        initBtnClickListener();
        
        // 获取传递过来的大棚ID
        Intent intent = getIntent();
        if (intent != null) {
            currentGreenhouseId = intent.getStringExtra("greenhouseId");
        }
        
        // 如果没有传递大棚ID，使用默认值
        if (currentGreenhouseId == null) {
            currentGreenhouseId = "default";
        }
        
        loadChartData(currentDayCount);
    }

    private void initViews() {
        btn_back = findViewById(R.id.btn_back);
        btn_7d = findViewById(R.id.btn_7d);
        btn_14d = findViewById(R.id.btn_14d);
        spinnerGreenhouse = findViewById(R.id.spinner_greenhouse);

        chart_temp = findViewById(R.id.chart_temp);
        chart_hum = findViewById(R.id.chart_hum);
        chart_light = findViewById(R.id.chart_light);
        chart_gas = findViewById(R.id.chart_gas);
        chart_ph = findViewById(R.id.chart_ph);
    }

    private void initGreenhouseSpinner() {
        // 从GreenhouseHomeStorage获取大棚列表
        greenhouseList = greenhouseHomeStorage.getGreenhouseList();
        greenhouseIdList = greenhouseHomeStorage.getGreenhouseIdList();
        
        // 如果没有大棚列表，添加默认大棚
        if (greenhouseList.isEmpty()) {
            greenhouseList.add("默认大棚");
            greenhouseIdList.add("1");
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, greenhouseList);
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerGreenhouse.setAdapter(adapter);
        
        // 设置默认选中的大棚
        if (currentGreenhouseId != null && !currentGreenhouseId.isEmpty()) {
            int position = greenhouseIdList.indexOf(currentGreenhouseId);
            if (position >= 0) {
                spinnerGreenhouse.setSelection(position);
            }
        }
        
        spinnerGreenhouse.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position >= 0 && position < greenhouseIdList.size()) {
                    currentGreenhouseId = greenhouseIdList.get(position);
                    loadChartData(currentDayCount);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // 不做任何操作
            }
        });
    }

    private void initChartStyle() {
        // AAChartCore 图表配置在数据加载时进行
    }

    private void initBtnClickListener() {
        if (btn_back != null) {
            btn_back.setOnClickListener(v -> {
                startActivity(new Intent(HistoryAnalysisActivity.this, ManagePage.class));
                finish();
            });
        }

        if (btn_7d != null) {
            btn_7d.setOnClickListener(v -> {
                setBtnSelectStatus(true);
                currentDayCount = 7;
                loadChartData(currentDayCount);
            });
        }

        if (btn_14d != null) {
            btn_14d.setOnClickListener(v -> {
                setBtnSelectStatus(false);
                currentDayCount = 14;
                loadChartData(currentDayCount);
            });
        }
    }

    private void setBtnSelectStatus(boolean is7d) {
        if (is7d) {
            if (btn_7d != null) {
                ViewCompat.setBackgroundTintList(btn_7d,
                        ResourcesCompat.getColorStateList(getResources(), R.color.green_main, getTheme()));
                btn_7d.setTextColor(Color.parseColor("#FFFFFF"));
            }

            if (btn_14d != null) {
                ViewCompat.setBackgroundTintList(btn_14d,
                        ResourcesCompat.getColorStateList(getResources(), R.color.green_light, getTheme()));
                btn_14d.setTextColor(Color.parseColor("#1B320E"));
            }
        } else {
            if (btn_14d != null) {
                ViewCompat.setBackgroundTintList(btn_14d,
                        ResourcesCompat.getColorStateList(getResources(), R.color.green_main, getTheme()));
                btn_14d.setTextColor(Color.parseColor("#FFFFFF"));
            }

            if (btn_7d != null) {
                ViewCompat.setBackgroundTintList(btn_7d,
                        ResourcesCompat.getColorStateList(getResources(), R.color.green_light, getTheme()));
                btn_7d.setTextColor(Color.parseColor("#1B320E"));
            }
        }
    }

    private void loadChartData(int dayCount) {
        if (useFakeData) {
            // 使用伪造数据
            loadFakeData(dayCount);
        } else {
            // 使用真实数据
            // 从HistoricalDataStorageUtils获取历史数据
            JSONArray dailyData = HistoricalDataStorageUtils.getDailyAverageData(this, currentGreenhouseId, dayCount);
            
            if (dailyData != null && dailyData.size() > 0) {
                android.util.Log.d("HistoryAnalysis", "加载历史数据: 大棚ID=" + currentGreenhouseId + ", 天数=" + dayCount + ", 数据条数=" + dailyData.size());
                android.util.Log.d("HistoryAnalysis", "数据内容: " + dailyData.toJSONString());
                
                loadTempData(dailyData);
                loadHumData(dailyData);
                loadLightData(dailyData);
                loadGasData(dailyData);
                loadPHData(dailyData);
            } else {
                // 没有数据时，清空图表
                loadTempData(new JSONArray());
                loadHumData(new JSONArray());
                loadLightData(new JSONArray());
                loadGasData(new JSONArray());
                loadPHData(new JSONArray());
            }
        }
    }
    
    private void loadFakeData(int dayCount) {
        // 生成指定天数的伪造数据
        JSONArray fakeData = generateFakeData(dayCount);
        loadTempData(fakeData);
        loadHumData(fakeData);
        loadLightData(fakeData);
        loadGasData(fakeData);
        loadPHData(fakeData);
    }
    
    private JSONArray generateFakeData(int dayCount) {
        JSONArray fakeData = new JSONArray();
        long now = System.currentTimeMillis();
        
        for (int i = dayCount - 1; i >= 0; i--) {
            long time = now - i * 24 * 3600000; // 从dayCount-1天前到现在
            JSONObject data = new JSONObject();
            
            // 生成伪造数据
            data.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(time)));
            data.put("temperature", 18 + Math.random() * 8); // 18-26度
            data.put("humidity", 60 + Math.random() * 20); // 60-80%
            data.put("soilTemperature", 15 + Math.random() * 5); // 15-20度
            data.put("soilHumidity", 40 + Math.random() * 30); // 40-70%
            data.put("co2", 400 + Math.random() * 200); // 400-600ppm
            data.put("o2", 20 + Math.random() * 1); // 20-21%
            data.put("lightIntensity", 500 + Math.random() * 1000); // 500-1500lux
            data.put("ph", 6 + Math.random() * 1); // 6-7
            
            fakeData.add(data);
        }
        
        return fakeData;
    }

    private void loadTempData(JSONArray dailyData) {
        List<String> categories = new ArrayList<>();
        List<Number> tempData = new ArrayList<>();
        List<Number> humData = new ArrayList<>();
        
        for (int i = 0; i < dailyData.size(); i++) {
            JSONObject item = dailyData.getJSONObject(i);
            try {
                // 检查数据是否存在
                if (item.containsKey("temperature") && item.containsKey("humidity")) {
                    float temp = item.getFloatValue("temperature");
                    float hum = item.getFloatValue("humidity");
                    // 只添加有真实数据的点
                    if (!Float.isNaN(temp) && !Float.isNaN(hum)) {
                        String date = item.getString("date");
                        categories.add(date.substring(5)); // 只显示月-日
                        tempData.add(temp);
                        humData.add(hum);
                    }
                }
            } catch (Exception e) {
                // 如果数据解析失败，跳过该数据点
            }
        }
        
        AAChartModel chartModel = new AAChartModel()
                .chartType(AAChartType.Area)
                .title("")
                .subtitle("")
                .backgroundColor("#F2F9F2")
                .categories(categories.toArray(new String[0]))
                .yAxisTitle("")
                .series(new AASeriesElement[]{
                        new AASeriesElement()
                                .name("温度 (℃)")
                                .data(tempData.toArray(new Number[0]))
                                .color("#FF5722")
                                .lineWidth(2),
                        new AASeriesElement()
                                .name("湿度 (%)")
                                .data(humData.toArray(new Number[0]))
                                .color("#2196F3")
                                .lineWidth(2)
                });
        
        chart_temp.aa_drawChartWithChartModel(chartModel);
    }

    private void loadHumData(JSONArray dailyData) {
        List<String> categories = new ArrayList<>();
        List<Number> soilTempData = new ArrayList<>();
        List<Number> soilHumData = new ArrayList<>();
        
        for (int i = 0; i < dailyData.size(); i++) {
            JSONObject item = dailyData.getJSONObject(i);
            try {
                // 检查数据是否存在
                if (item.containsKey("soilTemperature") && item.containsKey("soilHumidity")) {
                    float soilTemp = item.getFloatValue("soilTemperature");
                    float soilHum = item.getFloatValue("soilHumidity");
                    // 只添加有真实数据的点
                    if (!Float.isNaN(soilTemp) && !Float.isNaN(soilHum)) {
                        String date = item.getString("date");
                        categories.add(date.substring(5)); // 只显示月-日
                        soilTempData.add(soilTemp);
                        soilHumData.add(soilHum);
                    }
                }
            } catch (Exception e) {
                // 如果数据解析失败，跳过该数据点
            }
        }
        
        AAChartModel chartModel = new AAChartModel()
                .chartType(AAChartType.Column)
                .title("")
                .subtitle("")
                .backgroundColor("#F2F9F2")
                .categories(categories.toArray(new String[0]))
                .yAxisTitle("")
                .series(new AASeriesElement[]{
                        new AASeriesElement()
                                .name("土壤温度 (℃)")
                                .data(soilTempData.toArray(new Number[0]))
                                .color("#FF5722"),
                        new AASeriesElement()
                                .name("土壤湿度 (%)")
                                .data(soilHumData.toArray(new Number[0]))
                                .color("#2196F3")
                });
        
        chart_hum.aa_drawChartWithChartModel(chartModel);
    }

    private void loadLightData(JSONArray dailyData) {
        List<String> categories = new ArrayList<>();
        List<Number> data = new ArrayList<>();
        
        for (int i = 0; i < dailyData.size(); i++) {
            JSONObject item = dailyData.getJSONObject(i);
            try {
                // 检查数据是否存在
                if (item.containsKey("lightIntensity")) {
                    float light = item.getFloatValue("lightIntensity");
                    // 只添加有真实数据的点
                    if (!Float.isNaN(light)) {
                        String date = item.getString("date");
                        categories.add(date.substring(5)); // 只显示月-日
                        data.add(light);
                    }
                }
            } catch (Exception e) {
                // 如果数据解析失败，跳过该数据点
            }
        }
        
        AAChartModel chartModel = new AAChartModel()
                .chartType(AAChartType.Spline)
                .title("")
                .subtitle("")
                .backgroundColor("#F2F9F2")
                .categories(categories.toArray(new String[0]))
                .yAxisTitle("光照强度 (lux)")
                .series(new AASeriesElement[]{
                        new AASeriesElement()
                                .name("光照强度")
                                .data(data.toArray(new Number[0]))
                                .color("#FFC107")
                                .lineWidth(2)
                });
        
        chart_light.aa_drawChartWithChartModel(chartModel);
    }

    private void loadGasData(JSONArray dailyData) {
        List<String> categories = new ArrayList<>();
        List<Number> co2Data = new ArrayList<>();
        
        for (int i = 0; i < dailyData.size(); i++) {
            JSONObject item = dailyData.getJSONObject(i);
            try {
                // 检查数据是否存在
                if (item.containsKey("co2")) {
                    float co2 = item.getFloatValue("co2");
                    // 只添加有真实数据的点
                    if (!Float.isNaN(co2)) {
                        String date = item.getString("date");
                        categories.add(date.substring(5)); // 只显示月-日
                        co2Data.add(co2);
                    }
                }
            } catch (Exception e) {
                // 如果数据解析失败，跳过该数据点
            }
        }
        
        AAChartModel chartModel = new AAChartModel()
                .chartType(AAChartType.Area)
                .title("")
                .subtitle("")
                .backgroundColor("#F2F9F2")
                .categories(categories.toArray(new String[0]))
                .yAxisTitle("浓度")
                .series(new AASeriesElement[]{
                        new AASeriesElement()
                                .name("二氧化碳 (ppm)")
                                .data(co2Data.toArray(new Number[0]))
                                .color("#9C27B0")
                });
        
        chart_gas.aa_drawChartWithChartModel(chartModel);
    }

    private void loadPHData(JSONArray dailyData) {
        List<String> categories = new ArrayList<>();
        List<Number> data = new ArrayList<>();
        
        for (int i = 0; i < dailyData.size(); i++) {
            JSONObject item = dailyData.getJSONObject(i);
            try {
                // 检查数据是否存在
                if (item.containsKey("ph")) {
                    float ph = item.getFloatValue("ph");
                    // 只添加有真实数据的点
                    if (!Float.isNaN(ph)) {
                        String date = item.getString("date");
                        categories.add(date.substring(5)); // 只显示月-日
                        data.add(ph);
                    }
                }
            } catch (Exception e) {
                // 如果数据解析失败，跳过该数据点
            }
        }
        
        AAChartModel chartModel = new AAChartModel()
                .chartType(AAChartType.Spline)
                .title("")
                .subtitle("")
                .backgroundColor("#F2F9F2")
                .categories(categories.toArray(new String[0]))
                .yAxisTitle("PH值")
                .series(new AASeriesElement[]{
                        new AASeriesElement()
                                .name("PH值")
                                .data(data.toArray(new Number[0]))
                                .color("#F44336")
                                .lineWidth(2)
                });
        
        chart_ph.aa_drawChartWithChartModel(chartModel);
    }
}

