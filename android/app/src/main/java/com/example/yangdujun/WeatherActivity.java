package com.example.yangdujun;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.AAChartModel.AAChartCore.AAChartCreator.AAChartModel;
import com.github.AAChartModel.AAChartCore.AAChartCreator.AAChartView;
import com.github.AAChartModel.AAChartCore.AAChartCreator.AASeriesElement;
import com.github.AAChartModel.AAChartCore.AAChartEnum.AAChartType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeatherActivity extends AppCompatActivity {

    private Button btn24h, btn7days;
    private TextView tvMainTemp, tvWeatherDesc;
    private ImageView btnBack;
    private AAChartView chartTemperature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        btnBack = findViewById(R.id.btn_back);
        btn24h = findViewById(R.id.btn_24h);
        btn7days = findViewById(R.id.btn_7days);
        tvMainTemp = findViewById(R.id.tv_main_temp);
        tvWeatherDesc = findViewById(R.id.tv_weather_desc);
        chartTemperature = findViewById(R.id.chart_temperature);

        btnBack.setOnClickListener(v -> finish());

        // 初始化显示 24 小时数据
        toggleTab(true);
        load24hData();

        btn24h.setOnClickListener(v -> {
            toggleTab(true);
            load24hData();
        });

        btn7days.setOnClickListener(v -> {
            toggleTab(false);
            load7DayData();
        });
    }

    private void toggleTab(boolean is24hSelected) {
        if (is24hSelected) {
            btn24h.setBackgroundTintList(getResources().getColorStateList(R.color.green_primary));
            btn24h.setTextColor(Color.WHITE);
            btn7days.setBackgroundTintList(getResources().getColorStateList(R.color.green_light_bg));
            btn7days.setTextColor(Color.parseColor("#6BA878"));
        } else {
            btn7days.setBackgroundTintList(getResources().getColorStateList(R.color.green_primary));
            btn7days.setTextColor(Color.WHITE);
            btn24h.setBackgroundTintList(getResources().getColorStateList(R.color.green_light_bg));
            btn24h.setTextColor(Color.parseColor("#6BA878"));
        }
    }

    private void load24hData() {
        tvMainTemp.setText("24°C");
        tvWeatherDesc.setText("多云 · 适宜生长");

        List<String> categories = new ArrayList<>();
        List<Number> temperatureData = new ArrayList<>();

        Random random = new Random();
        int baseTemp = 24;

        for (int i = 0; i < 24; i++) {
            int hour = (int) ((System.currentTimeMillis() / 3600000 + i) % 24);
            categories.add(hour + "时");
            
            // 模拟温度变化：白天高，夜晚低
            int tempVariation = (int) (Math.sin((hour - 6) * Math.PI / 12) * 5);
            int temp = baseTemp + tempVariation + random.nextInt(3) - 1;
            temperatureData.add(temp);
        }

        AAChartModel chartModel = new AAChartModel()
                .chartType(AAChartType.Line)
                .title("")
                .subtitle("")
                .backgroundColor("#F2F9F2")
                .categories(categories.toArray(new String[0]))
                .yAxisTitle("温度 (℃)")
                .series(new AASeriesElement[]{
                        new AASeriesElement()
                                .name("温度")
                                .data(temperatureData.toArray(new Number[0]))
                                .color("#FF5722")
                                .lineWidth(2)
                });

        chartTemperature.aa_drawChartWithChartModel(chartModel);
    }

    private void load7DayData() {
        tvMainTemp.setText("26°C");
        tvWeatherDesc.setText("晴朗 · 适宜生长");

        List<String> categories = new ArrayList<>();
        List<Number> temperatureData = new ArrayList<>();

        String[] weekDays = {"今天", "明天", "周三", "周四", "周五", "周六", "周日"};
        Random random = new Random();
        int baseTemp = 26;

        for (int i = 0; i < 7; i++) {
            categories.add(weekDays[i]);
            int temp = baseTemp + random.nextInt(6) - 3;
            temperatureData.add(temp);
        }

        AAChartModel chartModel = new AAChartModel()
                .chartType(AAChartType.Line)
                .title("")
                .subtitle("")
                .backgroundColor("#F2F9F2")
                .categories(categories.toArray(new String[0]))
                .yAxisTitle("温度 (℃)")
                .series(new AASeriesElement[]{
                        new AASeriesElement()
                                .name("温度")
                                .data(temperatureData.toArray(new Number[0]))
                                .color("#FF5722")
                                .lineWidth(2)
                });

        chartTemperature.aa_drawChartWithChartModel(chartModel);
    }
}
