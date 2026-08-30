package com.example.yangdujun;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.yangdujun.utils.UserStorage;

public class LoginActivities extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        // 检查登录状态，如果已登录直接进入大棚页面
        UserStorage userStorage = new UserStorage();
        if (userStorage.isLoggedIn()) {
            // 已登录，直接进入大棚页面（首页）
            Intent intent = new Intent(LoginActivities.this, GreenhouseHomePage.class);
            startActivity(intent);
            finish();
            return;
        }
        
        Button loginBtn = findViewById(R.id.btn_login1);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                intent = new Intent(LoginActivities.this, Login.class);
                startActivity(intent);
                finish();
            }
        });
        Button RegBtn = findViewById(R.id.reg);
        RegBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                intent = new Intent(LoginActivities.this,Reg_Page.class);
                startActivity(intent);
                finish();
            }
        });
    }

}


