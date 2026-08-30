package com.example.yangdujun;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import com.example.yangdujun.model.ApiResponse;
import com.example.yangdujun.model.User;
import com.example.yangdujun.network.ApiService;
import com.example.yangdujun.utils.JsonUtils;
import com.example.yangdujun.utils.UserStorage;

public class Login extends AppCompatActivity{
    private EditText etPhone, etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login2); // 对应登录页面的布局文件

        // 初始化输入框
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        
        // 检查输入框是否为null
        if (etPhone == null || etPassword == null) {
            Toast.makeText(this, "页面加载失败，请重试", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 找到登录按钮
        Button loginBtn = findViewById(R.id.btn_login);
        if (loginBtn != null) {
            // 设置点击事件
            loginBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    login();
                }
            });
        }
        
        Button RegBtn = findViewById(R.id.btn_reg1);
        if (RegBtn != null) {
            // 设置点击事件
            RegBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Login.this, Reg_Page.class);
                    startActivity(intent);
                    finish();
                }
            });
        }
        
        Button newpsw = findViewById(R.id.btn_newpsw1);
        if (newpsw != null) {
            // 设置点击事件
            newpsw.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Login.this, NewPassWord.class);
                    startActivity(intent);
                    finish();
                }
            });
        }
    }

    private void login() {
        // 检查输入框是否为null
        if (etPhone == null || etPassword == null) {
            Toast.makeText(this, "页面加载失败，请重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (phone.isEmpty()) {
            Toast.makeText(this, "请输入手机号", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示加载状态
        Toast.makeText(this, "登录中...", Toast.LENGTH_SHORT).show();

        // 使用本地JSON存储进行登录验证
        new Thread(() -> {
            try {
                // 调用API登录（会使用本地存储进行验证）
                String response = ApiService.login(phone, password);
                ApiResponse<User> apiResponse = JsonUtils.fromJson(response, ApiResponse.class);

                runOnUiThread(() -> {
                    // 检查Activity是否有效
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    
                    if (apiResponse != null && apiResponse.isSuccess()) {
                        // 保存用户信息到本地JSON存储
                        UserStorage userStorage = new UserStorage();
                        
                        // 处理FastJSON泛型解析问题
                        Object dataObj = apiResponse.getData();
                        if (dataObj instanceof com.alibaba.fastjson.JSONObject) {
                            com.alibaba.fastjson.JSONObject dataJson = (com.alibaba.fastjson.JSONObject) dataObj;
                            User user = new User();
                            user.setId(dataJson.getString("id"));
                            user.setPhone(dataJson.getString("phone"));
                            user.setToken(dataJson.getString("token"));
                            user.setNickname(dataJson.getString("nickname"));
                            user.setAvatar(dataJson.getString("avatar"));
                            userStorage.saveUser(user);
                        }
                        
                        // 同时保存账号密码到本地
                        userStorage.saveUserCredentials(phone, password);
                        
                        android.util.Log.d("LoginTest", "Login successful, preparing to navigate");
                        Toast.makeText(Login.this, "登录成功", Toast.LENGTH_SHORT).show();
                        
                        android.util.Log.d("LoginTest", "Creating intent for GreenhouseHomePage");
                        Intent intent = new Intent(Login.this, GreenhouseHomePage.class);
                        
                        android.util.Log.d("LoginTest", "Starting activity");
                        startActivity(intent);
                        
                        android.util.Log.d("LoginTest", "Finishing current activity");
                        finish();
                    } else {
                        String errorMsg = apiResponse != null ? apiResponse.getMessage() : "登录失败，请稍后重试";
                        Toast.makeText(Login.this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    // 检查Activity是否有效
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    Toast.makeText(Login.this, "登录失败，请稍后重试", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}


