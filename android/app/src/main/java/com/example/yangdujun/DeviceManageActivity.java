package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.model.ApiResponse;
import com.example.yangdujun.network.ApiService;
import com.example.yangdujun.utils.JsonUtils;
import com.example.yangdujun.utils.UserStorage;
import com.example.yangdujun.utils.ManagePageStorage;
import com.example.yangdujun.utils.GreenhouseHomeStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DeviceManageActivity extends AppCompatActivity {
    private TextView tabCollect, tabControl, tabMonitor, tabDisplay;
    private RecyclerView rvDeviceList;
    private DeviceAdapter adapter;
    private final List<DeviceBean> deviceList = new ArrayList<>();
    private String userToken;
    private String greenhouseId; // 从Intent获取的大棚ID
    private ManagePageStorage managePageStorage;
    private GreenhouseHomeStorage greenhouseHomeStorage;
    private List<String> greenhouseList = new ArrayList<>();
    private List<String> greenhouseIdList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_manage);

        // 获取用户token
        UserStorage userStorage = new UserStorage();
        userToken = userStorage.getToken();
        if (userToken == null || userToken.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }

        // 获取从Intent传递的大棚ID
        greenhouseId = getIntent().getStringExtra("greenhouseId");
        if (greenhouseId == null) {
            greenhouseId = "1"; // 默认大棚ID
        }

        // 初始化存储工具
        managePageStorage = new ManagePageStorage(this);
        greenhouseHomeStorage = new GreenhouseHomeStorage(this);

        // 修复1：增加非空保护，避免返回null导致后续调用异常
        List<String> tempGreenhouseList = greenhouseHomeStorage.getGreenhouseList();
        greenhouseList = tempGreenhouseList != null ? tempGreenhouseList : new ArrayList<>();

        List<String> tempGreenhouseIdList = greenhouseHomeStorage.getGreenhouseIdList();
        greenhouseIdList = tempGreenhouseIdList != null ? tempGreenhouseIdList : new ArrayList<>();

        initView();
        initGreenhouseSpinner();
        initTabClick();
        // 初始化返回按钮点击事件
        initBackBtn();
        // 默认选中采集设备Tab
        switchTab("采集设备");
        // 获取设备列表
        getDeviceList();
    }

    private void initGreenhouseSpinner() {
        Spinner spinner = findViewById(R.id.spinner_greenhouse);
        if (spinner == null) {
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, greenhouseList);
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        // 设置Spinner的字体颜色为黑色
        spinner.setAdapter(adapter);

        // 修复2：增加greenhouseIdList非空判断，避免indexOf空指针
        if (greenhouseId != null && !greenhouseId.isEmpty() && !greenhouseIdList.isEmpty()) {
            int position = greenhouseIdList.indexOf(greenhouseId);
            if (position >= 0 && position < greenhouseList.size()) { // 修复：增加索引范围判断
                spinner.setSelection(position);
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 修复3：增加greenhouseIdList非空+索引有效判断
                if (greenhouseIdList != null && !greenhouseIdList.isEmpty()
                        && position >= 0 && position < greenhouseIdList.size()) {
                    greenhouseId = greenhouseIdList.get(position);
                    // 无论选择哪个大棚都加载设备列表
                    getDeviceList();
                } else {
                    // 清空设备列表
                    deviceList.clear();
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                    // 隐藏所有标签页的内容
                    if (rvDeviceList != null) {
                        rvDeviceList.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 清空设备列表
                deviceList.clear();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
                // 隐藏所有标签页的内容
                if (rvDeviceList != null) {
                    rvDeviceList.setVisibility(View.GONE);
                }
            }
        });
    }

    private void initView() {
        tabCollect = findViewById(R.id.tab_collect);
        tabControl = findViewById(R.id.tab_control);
        tabMonitor = findViewById(R.id.tab_monitor);
        tabDisplay = findViewById(R.id.tab_display);
        rvDeviceList = findViewById(R.id.rv_device_list);
        rvDeviceList.setLayoutManager(new LinearLayoutManager(this));
    }

    // ========== 新增核心代码：返回按钮功能 ==========
    private void initBackBtn() {
        // 绑定布局中的返回按钮id
        ImageView btn_back = findViewById(R.id.btn_back);
        if (btn_back != null) {
            btn_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 返回ManagePage大棚管理页面
                    Intent intent = new Intent(DeviceManageActivity.this, ManagePage.class);
                    if (greenhouseId != null) {
                        intent.putExtra("greenhouseId", greenhouseId);
                    }
                    startActivity(intent);
                    finish(); // 销毁当前页面，避免返回后重复叠加页面
                }
            });
        }
    }

    // 获取设备列表
    private void getDeviceList() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 先从存储中读取设备列表
                    JSONObject deviceStatus = managePageStorage.getDeviceStatus();
                    // 修复4：增加deviceStatus非空+包含devices字段的双重判断
                    if (deviceStatus != null && deviceStatus.containsKey("devices")) {
                        JSONArray deviceArray = deviceStatus.getJSONArray("devices");
                        // 修复5：增加deviceArray非空判断
                        if (deviceArray != null) {
                            final List<DeviceBean> devices = new ArrayList<>();
                            for (int i = 0; i < deviceArray.size(); i++) {
                                JSONObject deviceObj = deviceArray.getJSONObject(i);
                                // 修复6：字段取值增加非空默认值，避免空指针
                                String type = deviceObj.getString("type") == null ? "" : deviceObj.getString("type");
                                String name = deviceObj.getString("name") == null ? "" : deviceObj.getString("name");
                                boolean status = deviceObj.getBooleanValue("status"); // 使用getBooleanValue避免null
                                String deviceId = deviceObj.getString("deviceId") == null ? "" : deviceObj.getString("deviceId");
                                devices.add(new DeviceBean(type, name, status, deviceId));
                            }

                            runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            deviceList.clear();
                                            deviceList.addAll(devices);
                                            adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceStatusChangeListener() {
                                                @Override
                                                public void onStatusChange(DeviceBean device, boolean status) {
                                                    // 控制设备状态
                                                    controlDevice(device, status);
                                                }
                                            });
                                            rvDeviceList.setAdapter(adapter);
                                            // 显示设备列表
                                            rvDeviceList.setVisibility(View.VISIBLE);
                                            // 默认筛选采集设备
                                            if (adapter != null) {
                                                adapter.filter("采集设备");
                                            }
                                        }
                                    });
                        } else {
                            // deviceArray为空，走默认列表逻辑
                            createDefaultDeviceList();
                        }
                    } else {
                        // 如果存储中没有设备数据，从网络获取
                        try {
                            String response = ApiService.getDeviceList(userToken, greenhouseId);
                            // 修复7：增加response非空判断
                            if (response != null && !response.isEmpty()) {
                                ApiResponse<String> apiResponse = JsonUtils.fromJson(response, ApiResponse.class);
                                // 修复8：增加apiResponse非空判断
                                if (apiResponse != null && apiResponse.isSuccess()) {
                                    String data = apiResponse.getData();
                                    // 修复9：增加data非空判断
                                    if (data != null && !data.isEmpty()) {
                                        JSONArray deviceArray = JSON.parseArray(data);
                                        if (deviceArray != null) {
                                            final List<DeviceBean> devices = new ArrayList<>();
                                            for (int i = 0; i < deviceArray.size(); i++) {
                                                JSONObject deviceObj = deviceArray.getJSONObject(i);
                                                String type = deviceObj.getString("type") == null ? "" : deviceObj.getString("type");
                                                String name = deviceObj.getString("name") == null ? "" : deviceObj.getString("name");
                                                boolean status = deviceObj.getBooleanValue("status");
                                                String deviceId = deviceObj.getString("deviceId") == null ? "" : deviceObj.getString("deviceId");
                                                devices.add(new DeviceBean(type, name, status, deviceId));
                                            }

                                            // 保存设备列表到存储
                                            JSONObject newDeviceStatus = new JSONObject();
                                            newDeviceStatus.put("greenhouseId", greenhouseId);
                                            newDeviceStatus.put("devices", deviceArray);
                                            managePageStorage.saveDeviceStatus(newDeviceStatus);

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    deviceList.clear();
                                                    deviceList.addAll(devices);
                                                    adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceStatusChangeListener() {
                                                        @Override
                                                        public void onStatusChange(DeviceBean device, boolean status) {
                                                            // 控制设备状态
                                                            controlDevice(device, status);
                                                        }
                                                    });
                                                    rvDeviceList.setAdapter(adapter);
                                                    // 默认筛选采集设备
                                                    if (adapter != null) {
                                                        adapter.filter("采集设备");
                                                    }
                                                }
                                            });
                                        } else {
                                            createDefaultDeviceList();
                                        }
                                    } else {
                                        createDefaultDeviceList();
                                    }
                                } else {
                                    // 如果网络获取失败，创建默认设备列表
                                    createDefaultDeviceList();
                                }
                            } else {
                                createDefaultDeviceList();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            // 网络异常，创建默认设备列表
                            createDefaultDeviceList();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 异常，创建默认设备列表
                    createDefaultDeviceList();
                }
            }
        }).start();
    }

    // 创建默认设备列表（与大棚页面保持一致）
    private void createDefaultDeviceList() {
        final List<DeviceBean> devices = new ArrayList<>();

        // 控制设备（3个）：灯、挡光板、风机
        devices.add(new DeviceBean("控制设备", "灯", false, "light_1"));
        devices.add(new DeviceBean("控制设备", "挡光板", false, "board_1"));
        devices.add(new DeviceBean("控制设备", "风机", false, "fan_1"));

        // 采集设备（8个）
        devices.add(new DeviceBean("采集设备", "空气温度传感器", true, "sensor_temp_1"));
        devices.add(new DeviceBean("采集设备", "空气湿度传感器", true, "sensor_hum_1"));
        devices.add(new DeviceBean("采集设备", "土壤温度传感器", true, "sensor_soil_temp_1"));
        devices.add(new DeviceBean("采集设备", "土壤湿度传感器", true, "sensor_soil_hum_1"));
        devices.add(new DeviceBean("采集设备", "二氧化碳传感器", true, "sensor_co2_1"));
        devices.add(new DeviceBean("采集设备", "氧气传感器", true, "sensor_o2_1"));
        devices.add(new DeviceBean("采集设备", "PH值传感器", true, "sensor_ph_1"));
        devices.add(new DeviceBean("采集设备", "光照强度传感器", true, "sensor_light_1"));

        // 监控设备（1个）
        devices.add(new DeviceBean("监控设备", "摄像头", true, "camera_1"));

        // 显示设备（1个）
        devices.add(new DeviceBean("显示设备", "显示屏", true, "display_1"));

        // 保存到存储
        JSONArray deviceArray = new JSONArray();
        for (DeviceBean device : devices) {
            JSONObject deviceObj = new JSONObject();
            deviceObj.put("type", device.getType());
            deviceObj.put("name", device.getName());
            deviceObj.put("status", device.isStatus());
            deviceObj.put("deviceId", device.getDeviceId());
            deviceArray.add(deviceObj);
        }
        JSONObject deviceStatus = new JSONObject();
        deviceStatus.put("greenhouseId", greenhouseId);
        deviceStatus.put("devices", deviceArray);
        managePageStorage.saveDeviceStatus(deviceStatus);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceList.clear();
                deviceList.addAll(devices);
                adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceStatusChangeListener() {
                    @Override
                    public void onStatusChange(DeviceBean device, boolean status) {
                        // 控制设备状态
                        controlDevice(device, status);
                    }
                });
                rvDeviceList.setAdapter(adapter);
                // 显示设备列表
                rvDeviceList.setVisibility(View.VISIBLE);
                // 默认筛选采集设备
                if (adapter != null) {
                    adapter.filter("采集设备");
                }
            }
        });
    }

    // 获取设备列表并筛选
    private void getDeviceListAndFilter(String filterType) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 先从存储中读取设备列表
                    JSONObject deviceStatus = managePageStorage.getDeviceStatus();
                    // 修复10：增加deviceStatus非空判断
                    if (deviceStatus != null && deviceStatus.containsKey("devices")) {
                        JSONArray deviceArray = deviceStatus.getJSONArray("devices");
                        if (deviceArray != null) {
                            final List<DeviceBean> devices = new ArrayList<>();
                            for (int i = 0; i < deviceArray.size(); i++) {
                                JSONObject deviceObj = deviceArray.getJSONObject(i);
                                String type = deviceObj.getString("type") == null ? "" : deviceObj.getString("type");
                                String name = deviceObj.getString("name") == null ? "" : deviceObj.getString("name");
                                boolean status = deviceObj.getBooleanValue("status");
                                String deviceId = deviceObj.getString("deviceId") == null ? "" : deviceObj.getString("deviceId");
                                devices.add(new DeviceBean(type, name, status, deviceId));
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    deviceList.clear();
                                    deviceList.addAll(devices);
                                    adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceStatusChangeListener() {
                                        @Override
                                        public void onStatusChange(DeviceBean device, boolean status) {
                                            // 控制设备状态
                                            controlDevice(device, status);
                                        }
                                    });
                                    rvDeviceList.setAdapter(adapter);
                                    // 显示设备列表
                                    rvDeviceList.setVisibility(View.VISIBLE);
                                    // 筛选设备
                                    if (adapter != null) {
                                        adapter.filter(filterType);
                                    }
                                }
                            });
                        } else {
                            createDefaultDeviceListAndFilter(filterType);
                        }
                    } else {
                        // 如果存储中没有设备数据，从网络获取
                        try {
                            String response = ApiService.getDeviceList(userToken, greenhouseId);
                            if (response != null && !response.isEmpty()) {
                                ApiResponse<String> apiResponse = JsonUtils.fromJson(response, ApiResponse.class);
                                // 修复11：增加apiResponse非空判断
                                if (apiResponse != null && apiResponse.isSuccess()) {
                                    String data = apiResponse.getData();
                                    if (data != null && !data.isEmpty()) {
                                        JSONArray deviceArray = JSON.parseArray(data);
                                        if (deviceArray != null) {
                                            final List<DeviceBean> devices = new ArrayList<>();
                                            for (int i = 0; i < deviceArray.size(); i++) {
                                                JSONObject deviceObj = deviceArray.getJSONObject(i);
                                                String type = deviceObj.getString("type") == null ? "" : deviceObj.getString("type");
                                                String name = deviceObj.getString("name") == null ? "" : deviceObj.getString("name");
                                                boolean status = deviceObj.getBooleanValue("status");
                                                String deviceId = deviceObj.getString("deviceId") == null ? "" : deviceObj.getString("deviceId");
                                                devices.add(new DeviceBean(type, name, status, deviceId));
                                            }

                                            // 保存设备列表到存储
                                            JSONObject newDeviceStatus = new JSONObject();
                                            newDeviceStatus.put("greenhouseId", greenhouseId);
                                            newDeviceStatus.put("devices", deviceArray);
                                            managePageStorage.saveDeviceStatus(newDeviceStatus);

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    deviceList.clear();
                                                    deviceList.addAll(devices);
                                                    adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceStatusChangeListener() {
                                                        @Override
                                                        public void onStatusChange(DeviceBean device, boolean status) {
                                                            // 控制设备状态
                                                            controlDevice(device, status);
                                                        }
                                                    });
                                                    rvDeviceList.setAdapter(adapter);
                                                    // 筛选设备
                                                    if (adapter != null) {
                                                        adapter.filter(filterType);
                                                    }
                                                }
                                            });
                                        } else {
                                            createDefaultDeviceListAndFilter(filterType);
                                        }
                                    } else {
                                        createDefaultDeviceListAndFilter(filterType);
                                    }
                                } else {
                                    // 如果网络获取失败，创建默认设备列表
                                    createDefaultDeviceListAndFilter(filterType);
                                }
                            } else {
                                createDefaultDeviceListAndFilter(filterType);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            // 网络异常，创建默认设备列表
                            createDefaultDeviceListAndFilter(filterType);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 异常，创建默认设备列表
                    createDefaultDeviceListAndFilter(filterType);
                }
            }
        }).start();
    }

    // 创建默认设备列表并筛选
    private void createDefaultDeviceListAndFilter(String filterType) {
        final List<DeviceBean> devices = new ArrayList<>();

        // 控制设备（3个）：灯、挡光板、风机
        devices.add(new DeviceBean("控制设备", "灯", false, "light_1"));
        devices.add(new DeviceBean("控制设备", "挡光板", false, "board_1"));
        devices.add(new DeviceBean("控制设备", "风机", false, "fan_1"));

        // 采集设备（8个）
        devices.add(new DeviceBean("采集设备", "空气温度传感器", true, "sensor_temp_1"));
        devices.add(new DeviceBean("采集设备", "空气湿度传感器", true, "sensor_hum_1"));
        devices.add(new DeviceBean("采集设备", "土壤温度传感器", true, "sensor_soil_temp_1"));
        devices.add(new DeviceBean("采集设备", "土壤湿度传感器", true, "sensor_soil_hum_1"));
        devices.add(new DeviceBean("采集设备", "二氧化碳传感器", true, "sensor_co2_1"));
        devices.add(new DeviceBean("采集设备", "氧气传感器", true, "sensor_o2_1"));
        devices.add(new DeviceBean("采集设备", "PH值传感器", true, "sensor_ph_1"));
        devices.add(new DeviceBean("采集设备", "光照强度传感器", true, "sensor_light_1"));

        // 监控设备（1个）
        devices.add(new DeviceBean("监控设备", "摄像头", true, "camera_1"));

        // 显示设备（1个）
        devices.add(new DeviceBean("显示设备", "显示屏", true, "display_1"));

        // 保存到存储
        JSONArray deviceArray = new JSONArray();
        for (DeviceBean device : devices) {
            JSONObject deviceObj = new JSONObject();
            deviceObj.put("type", device.getType());
            deviceObj.put("name", device.getName());
            deviceObj.put("status", device.isStatus());
            deviceObj.put("deviceId", device.getDeviceId());
            deviceArray.add(deviceObj);
        }
        JSONObject deviceStatus = new JSONObject();
        deviceStatus.put("greenhouseId", greenhouseId);
        deviceStatus.put("devices", deviceArray);
        managePageStorage.saveDeviceStatus(deviceStatus);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceList.clear();
                deviceList.addAll(devices);
                adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceStatusChangeListener() {
                    @Override
                    public void onStatusChange(DeviceBean device, boolean status) {
                        // 控制设备状态
                        controlDevice(device, status);
                    }
                });
                rvDeviceList.setAdapter(adapter);
                // 显示设备列表
                rvDeviceList.setVisibility(View.VISIBLE);
                // 筛选设备
                if (adapter != null) {
                    adapter.filter(filterType);
                }
            }
        });
    }

    // 从存储中加载设备列表
    private void loadDeviceListFromStorage() {
        JSONObject deviceStatus = managePageStorage.getDeviceStatus();
        // 修复12：增加deviceStatus非空判断
        if (deviceStatus != null && deviceStatus.containsKey("devices")) {
            JSONArray deviceArray = deviceStatus.getJSONArray("devices");
            if (deviceArray != null) {
                final List<DeviceBean> devices = new ArrayList<>();
                for (int i = 0; i < deviceArray.size(); i++) {
                    JSONObject deviceObj = deviceArray.getJSONObject(i);
                    String type = deviceObj.getString("type") == null ? "" : deviceObj.getString("type");
                    String name = deviceObj.getString("name") == null ? "" : deviceObj.getString("name");
                    boolean status = deviceObj.getBooleanValue("status");
                    String deviceId = deviceObj.getString("deviceId") == null ? "" : deviceObj.getString("deviceId");
                    devices.add(new DeviceBean(type, name, status, deviceId));
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deviceList.clear();
                        deviceList.addAll(devices);
                        adapter = new DeviceAdapter(deviceList, new DeviceAdapter.OnDeviceStatusChangeListener() {
                            @Override
                            public void onStatusChange(DeviceBean device, boolean status) {
                                // 控制设备状态
                                controlDevice(device, status);
                            }
                        });
                        rvDeviceList.setAdapter(adapter);
                        // 显示设备列表
                        rvDeviceList.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    }

    // 控制设备
    private void controlDevice(DeviceBean device, boolean status) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 从device对象中获取deviceId
                    String deviceId = device.getDeviceId();
                    // 修复13：增加deviceId非空判断
                    if (deviceId == null || deviceId.isEmpty()) {
                        return;
                    }

                    String statusStr = status ? "1" : "0";
                    String response = ApiService.controlDevice(userToken, deviceId, statusStr);
                    // 修复14：增加response非空判断
                    if (response != null && !response.isEmpty()) {
                        ApiResponse<String> apiResponse = JsonUtils.fromJson(response, ApiResponse.class);
                        // 修复15：增加apiResponse非空判断
                        if (apiResponse != null && apiResponse.isSuccess()) {
                            // 更新本地存储中的设备状态
                            updateDeviceStatusInStorage(deviceId, status);

                            // 同步到GreenhouseHomeStorage，确保多页面数据一致性
                            syncDeviceStatusToGreenhouseHomeStorage(deviceId, status);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DeviceManageActivity.this, "设备控制成功", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // 设备控制失败，不显示提示
                                    // 恢复设备状态
                                    device.setStatus(!status);
                                    if (adapter != null) { // 修复16：增加adapter非空判断
                                        adapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                    } else {
                        // 接口返回为空，不显示提示
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 网络异常，不显示提示
                            // 恢复设备状态
                            device.setStatus(!status);
                            if (adapter != null) { // 修复17：增加adapter非空判断
                                adapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    // 同步设备状态到GreenhouseHomeStorage
    private void syncDeviceStatusToGreenhouseHomeStorage(String deviceId, boolean status) {
        try {
            // 获取当前大棚的环境数据
            JSONObject environmentData = greenhouseHomeStorage.getEnvironmentData(greenhouseId);
            // 修复18：增加environmentData非空判断，为空则初始化
            if (environmentData == null) {
                environmentData = new JSONObject();
            }

            // 根据设备类型更新状态
            if ("light_1".equals(deviceId)) {
                // 灯：开启/关闭
                environmentData.put("lightStatus", status ? "开" : "关");
            } else if ("board_1".equals(deviceId)) {
                // 挡光板：开启/关闭
                environmentData.put("windBoardStatus", status ? "开" : "关");
            } else if ("fan_1".equals(deviceId)) {
                // 风机：开启/关闭（默认为0档）
                environmentData.put("windSpeed", status ? "1" : "0");
            }

            // 保存更新后的环境数据
            greenhouseHomeStorage.saveEnvironmentData(greenhouseId, environmentData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 更新存储中的设备状态
    private void updateDeviceStatusInStorage(String deviceId, boolean status) {
        JSONObject deviceStatus = managePageStorage.getDeviceStatus();
        // 修复19：增加deviceStatus非空判断
        if (deviceStatus != null && deviceStatus.containsKey("devices")) {
            JSONArray deviceArray = deviceStatus.getJSONArray("devices");
            // 修复20：增加deviceArray非空判断
            if (deviceArray != null) {
                for (int i = 0; i < deviceArray.size(); i++) {
                    JSONObject deviceObj = deviceArray.getJSONObject(i);
                    // 修复21：增加deviceObj和deviceId非空判断
                    if (deviceObj != null && deviceId != null
                            && deviceId.equals(deviceObj.getString("deviceId"))) {
                        deviceObj.put("status", status);
                        break;
                    }
                }
                managePageStorage.saveDeviceStatus(deviceStatus);
            }
        }
    }

    // 标签切换逻辑
    private void initTabClick() {
        if (tabCollect != null) {
            tabCollect.setOnClickListener(v -> switchTab("采集设备"));
        }
        if (tabControl != null) {
            tabControl.setOnClickListener(v -> switchTab("控制设备"));
        }
        if (tabMonitor != null) {
            tabMonitor.setOnClickListener(v -> switchTab("监控设备"));
        }
        if (tabDisplay != null) {
            tabDisplay.setOnClickListener(v -> switchTab("显示设备"));
        }
    }

    // 切换标签并筛选设备
    private void switchTab(String type) {
        // 当前选中的标签类型

        // 重置标签样式
        resetTabStyle();
        // 设置选中标签样式
        switch (type) {
            case "采集设备":
                if (tabCollect != null) {
                    tabCollect.setBackgroundResource(R.drawable.tab_selected_background);
                    tabCollect.setTextColor(getResources().getColor(R.color.green_dark));
                    tabCollect.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
            case "控制设备":
                if (tabControl != null) {
                    tabControl.setBackgroundResource(R.drawable.tab_selected_background);
                    tabControl.setTextColor(getResources().getColor(R.color.green_dark));
                    tabControl.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
            case "监控设备":
                if (tabMonitor != null) {
                    tabMonitor.setBackgroundResource(R.drawable.tab_selected_background);
                    tabMonitor.setTextColor(getResources().getColor(R.color.green_dark));
                    tabMonitor.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
            case "显示设备":
                if (tabDisplay != null) {
                    tabDisplay.setBackgroundResource(R.drawable.tab_selected_background);
                    tabDisplay.setTextColor(getResources().getColor(R.color.green_dark));
                    tabDisplay.setTypeface(null, android.graphics.Typeface.BOLD);
                }
                break;
        }
        // 筛选设备列表，检查adapter是否为null
        if (adapter != null) {
            adapter.filter(type);
        } else {
            // 如果adapter为null，先获取设备列表，然后筛选
            getDeviceListAndFilter(type);
        }
    }

    // 重置标签样式
    private void resetTabStyle() {
        if (tabCollect != null) {
            tabCollect.setBackgroundColor(getResources().getColor(R.color.green_light));
            tabCollect.setTextColor(getResources().getColor(R.color.gray));
            tabCollect.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (tabControl != null) {
            tabControl.setBackgroundColor(getResources().getColor(R.color.green_light));
            tabControl.setTextColor(getResources().getColor(R.color.gray));
            tabControl.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (tabMonitor != null) {
            tabMonitor.setBackgroundColor(getResources().getColor(R.color.green_light));
            tabMonitor.setTextColor(getResources().getColor(R.color.gray));
            tabMonitor.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        if (tabDisplay != null) {
            tabDisplay.setBackgroundColor(getResources().getColor(R.color.green_light));
            tabDisplay.setTextColor(getResources().getColor(R.color.gray));
            tabDisplay.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }
}