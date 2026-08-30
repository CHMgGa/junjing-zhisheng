package com.example.yangdujun;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.model.LatLng;
import com.example.yangdujun.utils.GreenhouseHomeStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddGreenhouseActivity extends AppCompatActivity {

    private static final String TAG = "AddGreenhouse";

    // ====== IoTDA Shadow 接口 ======
    private static final String HUAWEI_GPS_URL = BuildConfig.HUAWEI_GPS_URL;

    // ====== IAM 换 Token 接口 ======
    private static final String IAM_TOKEN_URL = BuildConfig.HUAWEI_IAM_ENDPOINT;

    private static final String HWC_DOMAIN_NAME  = BuildConfig.HUAWEI_CLOUD_DOMAIN;
    private static final String HWC_USERNAME     = BuildConfig.HUAWEI_CLOUD_USER;
    private static final String HWC_PASSWORD     = BuildConfig.HUAWEI_CLOUD_PWD;
    private static final String HWC_PROJECT_NAME = BuildConfig.HUAWEI_CLOUD_PROJECT_NAME;

    private String iotdaToken = "";
    private long tokenExpireAtMs = 0;
    private final OkHttpClient client = new OkHttpClient();

    private EditText etName, etLoc;
    private Button btnOk, btnBack, btnGetGps, btnReset;
    private TextView tvLon, tvLat;

    private double curLat, curLon;

    private volatile boolean isUiAlive = false;

    private GreenhouseHomeStorage greenhouseHomeStorage;

    // ====== 地图（按 TabMap 思路：MapView + BaiduMap + Marker） ======
    private MapView mMapView;
    private BaiduMap mBaiduMap;

    private enum MarkerType { HUAWEI_CLOUD_LOCATION, DEVICE_LOCATION, PRECISE_RED }

    private Marker huaweiMarker;   // 蓝
    private Marker deviceMarker;   // 绿
    private Marker redMarker;      // 红

    private static final double DEFAULT_LAT = 39.9042; // 北京
    private static final double DEFAULT_LON = 116.4074;
    private static final float DEFAULT_ZOOM = 18f;

    private static final double POSITION_CHANGE_THRESHOLD = 0.00001;

    // ====== 本机定位（按 TabMap：LocationManager） ======
    private static final int REQ_LOCATION = 1001;
    private LocationManager locationManager;
    private double lastDeviceLon = Double.NaN;
    private double lastDeviceLat = Double.NaN;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_greenhouse);
        isUiAlive = true;

        // 返回按钮
        View ivBack = findViewById(R.id.iv_back);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        // views
        etName = findViewById(R.id.etName);
        etLoc = findViewById(R.id.etLoc);
        btnOk = findViewById(R.id.btnOk);
        btnBack = findViewById(R.id.btnBack);
        btnGetGps = findViewById(R.id.btnGetGps);
        btnReset = findViewById(R.id.btn_reset_location);

        tvLon = findViewById(R.id.longit);
        tvLat = findViewById(R.id.lati);

        if (etName == null || etLoc == null || btnOk == null || btnBack == null || btnGetGps == null || btnReset == null) {
            Toast.makeText(this, "页面布局缺少必要控件，无法打开", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnOk != null) {
            btnOk.setOnClickListener(v -> {
                String name = etName.getText().toString().trim();
                String loc = etLoc.getText().toString().trim();
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(loc)) {
                    Toast.makeText(this, "请填写大棚名称和位置", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                greenhouseHomeStorage = new GreenhouseHomeStorage(this);
                if (greenhouseHomeStorage != null) {
                    String greenhouseId = String.valueOf(System.currentTimeMillis());
                    
                    // 从GreenhouseHomeStorage获取当前大棚列表
                    List<String> greenhouseNames = greenhouseHomeStorage.getGreenhouseList();
                    List<String> greenhouseIds = greenhouseHomeStorage.getGreenhouseIdList();
                    
                    // 确保列表不为空，且包含默认大棚
                    if (greenhouseNames == null || greenhouseNames.size() < 2) {
                        greenhouseNames = new ArrayList<>();
                        greenhouseNames.add("请选择大棚");
                        greenhouseNames.add("默认大棚");
                        greenhouseIds = new ArrayList<>();
                        greenhouseIds.add("");
                        greenhouseIds.add("1");
                    }
                    
                    // 添加新大棚
                    greenhouseNames.add(name);
                    greenhouseIds.add(greenhouseId);
                    
                    // 保存更新后的列表
                    greenhouseHomeStorage.saveGreenhouseList(greenhouseNames, greenhouseIds);
                    
                    Toast.makeText(this, "添加成功", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }

        // ====== 初始化地图（完全按 TabMap：直接 MapView.getMap） ======
        initMap();
        initMapTouchIntercept();

        // 回到当前位置：优先云端点，没有就用本机点，再没有就用默认点
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> resetToCurrentLocation());
        }

        // 获取华为云坐标：先 token 再 shadow
        if (btnGetGps != null) {
            btnGetGps.setOnClickListener(v -> ensureTokenThenFetchShadow());
        }

        // ====== 启动本机定位（按 TabMap） ======
        checkLocationPermissionsAndStart();

        // 打开就拉一次云端坐标
        ensureTokenThenFetchShadow();
    }

    // ===================== 地图初始化 & Marker =====================

    private void initMap() {
        mMapView = findViewById(R.id.bmapView);
        if (mMapView == null) return;

        mBaiduMap = mMapView.getMap();
        LatLng defaultPoint = new LatLng(DEFAULT_LAT, DEFAULT_LON);

        MapStatusUpdate update = MapStatusUpdateFactory.newLatLngZoom(defaultPoint, DEFAULT_ZOOM);
        mBaiduMap.setMapStatus(update);

        // 初始三个点（也可以等第一次数据来了再创建）
        huaweiMarker = addMarker(defaultPoint, MarkerType.HUAWEI_CLOUD_LOCATION);
        deviceMarker = addMarker(defaultPoint, MarkerType.DEVICE_LOCATION);
        redMarker = addMarker(defaultPoint, MarkerType.PRECISE_RED);
    }

    private void initMapTouchIntercept() {
        if (mMapView == null) return;

        mMapView.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getX() - downX);
                        float dy = Math.abs(event.getY() - downY);
                        // 与 TabMap 同思路：拦截明显的水平滑动，避免和 ViewPager 冲突
                        if (dx > dy && dx > 10) return true;
                        break;
                }
                return false;
            }
        });
    }

    private Marker addMarker(LatLng point, MarkerType type) {
        int color;
        int zIndex;
        switch (type) {
            case HUAWEI_CLOUD_LOCATION:
                color = 0xFF2F80ED; // 蓝
                zIndex = 12;
                break;
            case DEVICE_LOCATION:
                color = 0xFF27AE60; // 绿
                zIndex = 12;
                break;
            case PRECISE_RED:
            default:
                color = 0xFFE74C3C; // 红
                zIndex = 11;
                break;
        }

        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(createCircleIcon(color));
        return (Marker) mBaiduMap.addOverlay(new MarkerOptions()
                .position(point)
                .icon(icon)
                .zIndex(zIndex));
    }

    private Bitmap createCircleIcon(int color) {
        int size = 50;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);
        return bitmap;
    }

    private boolean shouldUpdate(double newLon, double newLat, LatLng oldPos) {
        if (oldPos == null) return true;
        return Math.abs(newLon - oldPos.longitude) > POSITION_CHANGE_THRESHOLD
                || Math.abs(newLat - oldPos.latitude) > POSITION_CHANGE_THRESHOLD;
    }

    private void updateHuaweiCloudLocation(double lon, double lat) {
        if (mBaiduMap == null || huaweiMarker == null) return;

        LatLng oldPos = huaweiMarker.getPosition();
        if (!shouldUpdate(lon, lat, oldPos)) return;

        LatLng p = new LatLng(lat, lon);
        huaweiMarker.setPosition(p);

        if (tvLon != null) tvLon.setText(String.format(Locale.US, "%.6f", lon));
        if (tvLat != null) tvLat.setText(String.format(Locale.US, "%.6f", lat));
    }

    private void updateDeviceLocation(double lon, double lat) {
        lastDeviceLon = lon;
        lastDeviceLat = lat;

        if (deviceMarker != null) {
            LatLng oldPos = deviceMarker.getPosition();
            if (shouldUpdate(lon, lat, oldPos)) {
                deviceMarker.setPosition(new LatLng(lat, lon));
            }
        }
        if (redMarker != null) {
            LatLng oldPos = redMarker.getPosition();
            if (shouldUpdate(lon, lat, oldPos)) {
                redMarker.setPosition(new LatLng(lat, lon));
            }
        }
    }

    private void resetToCurrentLocation() {
        if (mBaiduMap == null) return;

        LatLng target;
        if (huaweiMarker != null) {
            target = huaweiMarker.getPosition();
        } else if (!Double.isNaN(lastDeviceLat) && !Double.isNaN(lastDeviceLon)) {
            target = new LatLng(lastDeviceLat, lastDeviceLon);
        } else {
            target = new LatLng(DEFAULT_LAT, DEFAULT_LON);
        }
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(target, 15f));
    }

    // ===================== 本机定位（LocationManager） =====================

    private void checkLocationPermissionsAndStart() {
        boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION);
        } else {
            startDeviceLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    private void startDeviceLocationUpdates() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) return;

        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
        }
        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, locationListener);
        }

        Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (last == null) last = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (last != null) updateDeviceLocation(last.getLongitude(), last.getLatitude());
    }

    private void stopDeviceLocationUpdates() {
        if (locationManager == null) return;

        boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) return; // 没权限就别调用 removeUpdates

        locationManager.removeUpdates(locationListener);
    }


    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            updateDeviceLocation(location.getLongitude(), location.getLatitude());
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            boolean granted = true;
            for (int r : grantResults) granted = granted && (r == PackageManager.PERMISSION_GRANTED);
            if (granted) startDeviceLocationUpdates();
        }
    }

    // ===================== IAM Token 获取逻辑（保留原逻辑） =====================

    private void ensureTokenThenFetchShadow() {
        long now = System.currentTimeMillis();
        if (!TextUtils.isEmpty(iotdaToken) && now < tokenExpireAtMs - 60_000) {
            fetchLonLatFromHuaweiCloud();
            return;
        }

        fetchIotdaToken(new TokenCallback() {
            @Override
            public void onTokenOk(String token, long expireAtMs) {
                iotdaToken = token;
                tokenExpireAtMs = expireAtMs;
                fetchLonLatFromHuaweiCloud();
            }

            @Override
            public void onFail(String msg) {
                Log.e(TAG, "获取Token失败: " + msg);
                safeUi(() -> Toast.makeText(AddGreenhouseActivity.this, "获取Token失败: " + msg, Toast.LENGTH_LONG).show());
            }
        });
    }

    private interface TokenCallback {
        void onTokenOk(String token, long expireAtMs);
        void onFail(String msg);
    }

    private void fetchIotdaToken(TokenCallback cb) {
        try {
            JSONObject root = new JSONObject();
            JSONObject auth = new JSONObject();

            JSONObject identity = new JSONObject();
            JSONArray methods = new JSONArray();
            methods.add("password");
            identity.put("methods", methods);

            JSONObject password = new JSONObject();
            JSONObject user = new JSONObject();
            user.put("name", HWC_USERNAME);
            user.put("password", HWC_PASSWORD);

            JSONObject domain = new JSONObject();
            domain.put("name", HWC_DOMAIN_NAME);
            user.put("domain", domain);

            JSONObject userWrap = new JSONObject();
            userWrap.put("user", user);

            password.put("user", userWrap.getJSONObject("user"));
            identity.put("password", password);

            JSONObject scope = new JSONObject();
            JSONObject project = new JSONObject();
            project.put("name", HWC_PROJECT_NAME);
            scope.put("project", project);

            auth.put("identity", identity);
            auth.put("scope", scope);
            root.put("auth", auth);

            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    root.toJSONString()
                        );

            Request req = new Request.Builder()
                    .url(IAM_TOKEN_URL)
                    .post(body)
                    .build();

            client.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    cb.onFail(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful()) {
                            String err = response.body() != null ? response.body().string() : "";
                            cb.onFail("HTTP " + response.code() + " " + err);
                            return;
                        }

                        String token = response.header("X-Subject-Token", "");
                        if (TextUtils.isEmpty(token)) {
                            cb.onFail("响应头缺少 X-Subject-Token");
                            return;
                        }

                        long expireAt = System.currentTimeMillis() + 60 * 60_000L;
                        cb.onTokenOk(token, expireAt);

                    } catch (Exception ex) {
                        cb.onFail(ex.getMessage());
                    } finally {
                        response.close();
                    }
                }
            });

        } catch (Exception e) {
            cb.onFail(e.getMessage());
        }
    }

    // ===================== IoTDA Shadow 拉取 + 解析（保留原逻辑） =====================

    private void fetchLonLatFromHuaweiCloud() {
        try {
            Request.Builder builder = new Request.Builder()
                    .url(HUAWEI_GPS_URL)
                    .get();

            if (!TextUtils.isEmpty(iotdaToken)) {
                builder.addHeader("X-Auth-Token", iotdaToken);
            }

            Request request = builder.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "IoTDA shadow 请求失败: " + e.getMessage(), e);
                    safeUi(() -> Toast.makeText(AddGreenhouseActivity.this, "获取失败", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        if (!response.isSuccessful() || response.body() == null) {
                            Log.e(TAG, "响应失败: code=" + response.code());
                            safeUi(() -> Toast.makeText(AddGreenhouseActivity.this,
                                    "响应失败: " + response.code(), Toast.LENGTH_SHORT).show());
                            return;
                        }

                        String body = response.body().string();
                        Log.d(TAG, "IoTDA shadow 返回: " + body);

                        parseLonLatFromHuawei(body);

                    } catch (Exception e) {
                        Log.e(TAG, "处理响应异常: " + e.getMessage(), e);
                        safeUi(() -> Toast.makeText(AddGreenhouseActivity.this,
                                "解析异常: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } finally {
                        response.close();
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "fetchLonLatFromHuaweiCloud异常: " + e.getMessage(), e);
            safeUi(() -> Toast.makeText(this, "网络请求异常", Toast.LENGTH_SHORT).show());
        }
    }

    private void parseLonLatFromHuawei(String responseBody) {
        try {
            JSONObject obj = JSON.parseObject(responseBody);
            if (obj == null) throw new RuntimeException("JSON为空");

            JSONArray shadow = obj.getJSONArray("shadow");
            if (shadow == null || shadow.isEmpty()) throw new RuntimeException("shadow为空");

            JSONObject props = null;
            for (int i = 0; i < shadow.size(); i++) {
                JSONObject item = shadow.getJSONObject(i);
                if (item == null) continue;
                JSONObject reported = item.getJSONObject("reported");
                if (reported == null) continue;
                JSONObject p = reported.getJSONObject("properties");
                if (p != null) { props = p; break; }
            }
            if (props == null) throw new RuntimeException("properties未找到");

            String lonStr = pickFirst(props, "longitude", "Longitude", "lng", "Lon", "LON");
            String latStr = pickFirst(props, "latitude", "Latitude", "lat", "Lat", "LAT");

            if (TextUtils.isEmpty(lonStr) || TextUtils.isEmpty(latStr)) {
                throw new RuntimeException("经纬度字段缺失: lon=" + lonStr + ", lat=" + latStr);
            }

            double lon = Double.parseDouble(cleanNumber(lonStr));
            double lat = Double.parseDouble(cleanNumber(latStr));

            curLon = lon;
            curLat = lat;

            safeUi(() -> {
                etLoc.setText(String.format(Locale.US, "%.6f,%.6f", curLat, curLon));
                updateHuaweiCloudLocation(curLon, curLat);
                // 同时把地图居中过去（你也可以只按“回到当前位置”按钮再居中）
                if (mBaiduMap != null) {
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(new LatLng(curLat, curLon), 18f));
                }
                Toast.makeText(this, "已显示华为云坐标", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            Log.e(TAG, "解析经纬度失败: " + e.getMessage(), e);
            safeUi(() -> Toast.makeText(this, "解析失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private String pickFirst(JSONObject obj, String... keys) {
        for (String k : keys) {
            if (obj.containsKey(k)) {
                String v = obj.getString(k);
                if (!TextUtils.isEmpty(v)) return v;
            }
        }
        return null;
    }

    private String cleanNumber(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("[^0-9.\\-]", "");
    }

    // ===================== UI 安全更新 =====================

    private void safeUi(Runnable r) {
        if (!isUiAlive) return;
        runOnUiThread(() -> {
            if (!isUiAlive || isFinishing() || isDestroyed()) return;
            try { r.run(); } catch (Exception e) {
                Log.e(TAG, "safeUi异常: " + e.getMessage(), e);
            }
        });
    }

    // ===================== MapView 生命周期（按 TabMap：直接转发） =====================

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) mMapView.onResume();
    }

    @Override
    protected void onPause() {
        if (mMapView != null) mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        isUiAlive = false;
        stopDeviceLocationUpdates();
        if (mMapView != null) mMapView.onDestroy();
        super.onDestroy();
    }
}

