package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.utils.GreenhouseHomeStorage;
import com.example.yangdujun.utils.RealtimeDataStorageUtils;
import com.github.AAChartModel.AAChartCore.AAChartCreator.AAChartModel;
import com.github.AAChartModel.AAChartCore.AAChartCreator.AAChartView;
import com.github.AAChartModel.AAChartCore.AAChartCreator.AASeriesElement;
import com.github.AAChartModel.AAChartCore.AAChartEnum.AAChartType;

import java.util.ArrayList;
import java.util.List;

public class RealTimeAnalysisActivity extends AppCompatActivity {

    private ImageView btn_back;
    private AAChartView chart_air_temp_hum, chart_soil_temp_hum, chart_co2_o2, chart_light, chart_soil_ph;
    private Spinner spinnerGreenhouse;
    private final int HOUR_COUNT = 24;
    private GreenhouseHomeStorage greenhouseHomeStorage;
    private String currentGreenhouseId = "default";
    private List<String> greenhouseList = new ArrayList<>();
    private List<String> greenhouseIdList = new ArrayList<>();
    
    // 控制是否显示伪造数据
    private boolean useFakeData = true; // true: 显示伪造数据, false: 显示真实数据

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_time_analysis);

        greenhouseHomeStorage = new GreenhouseHomeStorage(this);
        
        initView();
        initChartBaseStyle();
        initGreenhouseSpinner();
        
        // 获取传递过来的大棚ID
        Intent intent = getIntent();
        if (intent != null) {
            currentGreenhouseId = intent.getStringExtra("greenhouseId");
        }
        
        // 如果没有传递大棚ID，使用默认值
        if (currentGreenhouseId == null) {
            currentGreenhouseId = "default";
        }
        
        loadAllChartData();
        initBackButton();
    }

    private void initView() {
        btn_back = findViewById(R.id.btn_back);
        spinnerGreenhouse = findViewById(R.id.spinner_greenhouse);
        chart_air_temp_hum = findViewById(R.id.chart_air_temp_hum);
        chart_soil_temp_hum = findViewById(R.id.chart_soil_temp_hum);
        chart_co2_o2 = findViewById(R.id.chart_co2_o2);
        chart_light = findViewById(R.id.chart_light);
        chart_soil_ph = findViewById(R.id.chart_soil_ph);
    }

    private void initGreenhouseSpinner() {
        if (spinnerGreenhouse != null) {
            // 从GreenhouseHomeStorage获取大棚列表
            if (greenhouseHomeStorage != null) {
                greenhouseList = greenhouseHomeStorage.getGreenhouseList();
                greenhouseIdList = greenhouseHomeStorage.getGreenhouseIdList();
                
                // 确保列表不为null
                if (greenhouseList == null) {
                    greenhouseList = new ArrayList<>();
                }
                if (greenhouseIdList == null) {
                    greenhouseIdList = new ArrayList<>();
                }
                
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
                        if (position >= 0 && position < greenhouseIdList.size() && greenhouseIdList != null) {
                            currentGreenhouseId = greenhouseIdList.get(position);
                            loadAllChartData();
                        }
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {
                        // 不做任何操作
                    }
                });
            }
        }
    }

    private void initChartBaseStyle() {
        // AAChartCore 图表配置在数据加载时进行
    }

    private void loadAllChartData() {
        if (useFakeData) {
            // 使用伪造数据
            loadFakeData();
        } else {
            // 使用真实数据
            // 从RealtimeDataStorageUtils获取24小时实时数据
            JSONArray hourlyData = RealtimeDataStorageUtils.getHourlyAverageData(this, currentGreenhouseId);
            
            if (hourlyData != null && hourlyData.size() > 0) {
                android.util.Log.d("RealTimeAnalysis", "加载实时数据: 大棚ID=" + currentGreenhouseId + ", 数据条数=" + hourlyData.size());
                android.util.Log.d("RealTimeAnalysis", "数据内容: " + hourlyData.toJSONString());
                
                loadAirTempHumData(hourlyData);
                loadSoilTempHumData(hourlyData);
                loadCo2AndO2Data(hourlyData);
                loadLightData(hourlyData);
                loadSoilPHData(hourlyData);
            } else {
                android.util.Log.d("RealTimeAnalysis", "未获取到实时数据: 大棚ID=" + currentGreenhouseId);
                // 没有数据时，清空图表
                loadAirTempHumData(new JSONArray());
                loadSoilTempHumData(new JSONArray());
                loadCo2AndO2Data(new JSONArray());
                loadLightData(new JSONArray());
                loadSoilPHData(new JSONArray());
            }
        }
    }
    
    private void loadFakeData() {
        // 生成24小时的伪造数据
        JSONArray fakeData = generateFakeData();
        loadAirTempHumData(fakeData);
        loadSoilTempHumData(fakeData);
        loadCo2AndO2Data(fakeData);
        loadLightData(fakeData);
        loadSoilPHData(fakeData);
    }
    
    private JSONArray generateFakeData() {
        JSONArray fakeData = new JSONArray();
        long now = System.currentTimeMillis();
        
        for (int i = 23; i >= 0; i--) {
            long time = now - i * 3600000; // 从23小时前到现在
            JSONObject data = new JSONObject();
            
            // 生成伪造数据
            data.put("datetime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(time)));
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

    private void loadAirTempHumData(JSONArray hourlyData) {
        if (chart_air_temp_hum != null && hourlyData != null) {
            List<String> categories = new ArrayList<>();
            List<Number> tempData = new ArrayList<>();
            List<Number> humData = new ArrayList<>();
            
            for (int i = 0; i < hourlyData.size(); i++) {
                JSONObject item = hourlyData.getJSONObject(i);
                try {
                    // 检查数据是否存在
                    if (item.containsKey("temperature") && item.containsKey("humidity")) {
                        float temp = item.getFloatValue("temperature");
                        float hum = item.getFloatValue("humidity");
                        // 只添加有真实数据的点
                        if (!Float.isNaN(temp) && !Float.isNaN(hum)) {
                            String datetime = item.getString("datetime");
                            categories.add(datetime.substring(11, 13) + "时"); // 只显示小时
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
            
            chart_air_temp_hum.aa_drawChartWithChartModel(chartModel);
        }
    }

    private void loadSoilTempHumData(JSONArray hourlyData) {
        if (chart_soil_temp_hum != null && hourlyData != null) {
            List<String> categories = new ArrayList<>();
            List<Number> soilTempData = new ArrayList<>();
            List<Number> soilHumData = new ArrayList<>();
            
            for (int i = 0; i < hourlyData.size(); i++) {
                JSONObject item = hourlyData.getJSONObject(i);
                try {
                    // 检查数据是否存在
                    if (item.containsKey("soilTemperature") && item.containsKey("soilHumidity")) {
                        float soilTemp = item.getFloatValue("soilTemperature");
                        float soilHum = item.getFloatValue("soilHumidity");
                        // 只添加有真实数据的点
                        if (!Float.isNaN(soilTemp) && !Float.isNaN(soilHum)) {
                            String datetime = item.getString("datetime");
                            categories.add(datetime.substring(11, 13) + "时"); // 只显示小时
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
            
            chart_soil_temp_hum.aa_drawChartWithChartModel(chartModel);
        }
    }

    private void loadCo2AndO2Data(JSONArray hourlyData) {
        if (chart_co2_o2 != null && hourlyData != null) {
            List<String> categories = new ArrayList<>();
            List<Number> co2Data = new ArrayList<>();
            
            for (int i = 0; i < hourlyData.size(); i++) {
                JSONObject item = hourlyData.getJSONObject(i);
                try {
                    // 检查数据是否存在
                    if (item.containsKey("co2")) {
                        float co2 = item.getFloatValue("co2");
                        // 只添加有真实数据的点
                        if (!Float.isNaN(co2)) {
                            String datetime = item.getString("datetime");
                            categories.add(datetime.substring(11, 13) + "时"); // 只显示小时
                            co2Data.add(co2);
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
                                    .name("二氧化碳 (ppm)")
                                    .data(co2Data.toArray(new Number[0]))
                                    .color("#9C27B0")
                    });
            
            chart_co2_o2.aa_drawChartWithChartModel(chartModel);
        }
    }

    private void loadLightData(JSONArray hourlyData) {
        if (chart_light != null && hourlyData != null) {
            List<String> categories = new ArrayList<>();
            List<Number> data = new ArrayList<>();
            
            for (int i = 0; i < hourlyData.size(); i++) {
                JSONObject item = hourlyData.getJSONObject(i);
                try {
                    // 检查数据是否存在
                    if (item.containsKey("lightIntensity")) {
                        float light = item.getFloatValue("lightIntensity");
                        // 只添加有真实数据的点
                        if (!Float.isNaN(light)) {
                            String datetime = item.getString("datetime");
                            categories.add(datetime.substring(11, 13) + "时"); // 只显示小时
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
    }

    private void loadSoilPHData(JSONArray hourlyData) {
        if (chart_soil_ph != null && hourlyData != null) {
            List<String> categories = new ArrayList<>();
            List<Number> data = new ArrayList<>();
            
            for (int i = 0; i < hourlyData.size(); i++) {
                JSONObject item = hourlyData.getJSONObject(i);
                try {
                    // 检查数据是否存在
                    if (item.containsKey("ph")) {
                        float ph = item.getFloatValue("ph");
                        // 只添加有真实数据的点
                        if (!Float.isNaN(ph)) {
                            String datetime = item.getString("datetime");
                            categories.add(datetime.substring(11, 13) + "时"); // 只显示小时
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
                    .yAxisTitle("土壤pH值")
                    .series(new AASeriesElement[]{
                            new AASeriesElement()
                                    .name("土壤pH值")
                                    .data(data.toArray(new Number[0]))
                                    .color("#F44336")
                                    .lineWidth(2)
                    });
            
            chart_soil_ph.aa_drawChartWithChartModel(chartModel);
        }
    }

    private void initBackButton() {
        if (btn_back != null) {
            btn_back.setOnClickListener(v -> {
                startActivity(new Intent(RealTimeAnalysisActivity.this, ManagePage.class));
                finish();
            });
        }
    }
}


