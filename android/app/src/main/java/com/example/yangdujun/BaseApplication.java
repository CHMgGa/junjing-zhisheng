package com.example.yangdujun;

import android.app.Application;
import com.baidu.mapapi.SDKInitializer;
import com.example.yangdujun.network.ApiService;

public class BaseApplication extends Application {
    private static BaseApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        
        SDKInitializer.initialize(getApplicationContext());
        
        initNetwork();
        ApiService.setAppContext(this);
    }

    private void initNetwork() {
    }

    public static BaseApplication getInstance() {
        return instance;
    }
}


