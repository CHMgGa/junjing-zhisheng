package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import com.example.yangdujun.model.ApiResponse;
import com.example.yangdujun.network.ApiService;
import com.example.yangdujun.utils.JsonUtils;
import com.example.yangdujun.utils.UserStorage;

public class NewPassWord extends AppCompatActivity {
    // 控件声明 - 和布局ID一一对应，命名规范和注册页一致
    private EditText et_phone, et_new_pwd;
    private ImageView iv_pwd_eye;
    private ImageButton btn_back;
    private Button btn_confirm;

    // 密码显示隐藏标记
    private boolean isPwdShow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newpassword); // 对应你的忘记密码布局文件名

        // 绑定所有控件
        bindViews();
        // 返回按钮 点击事件 - 返回上一页(登录页)

        btn_back.setOnClickListener(v -> jumpToLogin());
        // 密码显示/隐藏 点击事件 - 和注册页逻辑完全一样
        iv_pwd_eye.setOnClickListener(v -> {
            isPwdShow = !isPwdShow;
            if (isPwdShow) {
                // 显示密码 正确写法 兼容所有安卓版本 不闪退
                et_new_pwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                iv_pwd_eye.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                // 隐藏密码
                et_new_pwd.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                iv_pwd_eye.setImageResource(android.R.drawable.ic_menu_view);
            }
            // 光标定位到密码末尾，体验优化
            et_new_pwd.setSelection(et_new_pwd.getText().length());
        });

        // 确认修改密码 按钮点击事件 - 完整业务逻辑
        btn_confirm.setOnClickListener(v -> {
            String phone = et_phone.getText().toString().trim();
            String newPwd = et_new_pwd.getText().toString().trim();

            // 完整表单校验
            if (TextUtils.isEmpty(phone) || phone.length() != 11) {
                showToast("请输入正确的手机号码");
                return;
            }
            if (TextUtils.isEmpty(newPwd) || newPwd.length() < 8) {
                showToast("密码长度不能少于8位");
                return;
            }
            if (isAllLetterOrDigit(newPwd)) {
                showToast("密码不能全是字母或数字，请重新设置");
                return;
            }

            // 显示加载状态
            showToast("重置密码中...");

            // 使用本地JSON存储进行密码重置
            new Thread(() -> {
                try {
                    // 调用API重置密码（会使用本地存储进行处理）
                    String response = ApiService.forgetPassword(phone, newPwd);
                    ApiResponse<?> apiResponse = JsonUtils.fromJson(response, ApiResponse.class);

                    runOnUiThread(() -> {
                        if (apiResponse != null && apiResponse.isSuccess()) {
                            // 更新本地存储的密码
                            UserStorage userStorage = new UserStorage();
                            userStorage.saveUserCredentials(phone, newPwd);
                            
                            showToast("密码修改成功！即将返回登录页");
                            // 跳转到登录页，关闭当前忘记密码页面
                            Intent intent = new Intent(NewPassWord.this, Login.class);
                            startActivity(intent);
                            finish();
                        } else {
                            String errorMsg = apiResponse != null ? apiResponse.getMessage() : "重置密码失败，请稍后重试";
                            showToast(errorMsg);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        showToast("重置密码失败，请稍后重试");
                    });
                }
            }).start();
        });
    }

    /**
     * 绑定所有控件 统一管理 无重复绑定 避免闪退
     */
    private void bindViews() {
        et_phone = findViewById(R.id.et_phone);
        et_new_pwd = findViewById(R.id.et_new_pwd);
        iv_pwd_eye = findViewById(R.id.iv_pwd_eye);
        btn_back = findViewById(R.id.btn_back);
        btn_confirm = findViewById(R.id.btn_confirm);
    }

    /**
     * 校验密码规则：不能全是字母 或 全是数字
     * 对应布局里的密码提示文字
     */
    private boolean isAllLetterOrDigit(String str) {
        boolean isAllLetter = true;
        boolean isAllDigit = true;
        for (char c : str.toCharArray()) {
            if (!Character.isLetter(c)) {
                isAllLetter = false;
            }
            if (!Character.isDigit(c)) {
                isAllDigit = false;
            }
        }
        return isAllLetter || isAllDigit;
    }

    /**
     * 统一Toast提示方法 避免重复代码
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 生命周期管理
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void jumpToLogin() {
        Intent intent = new Intent(NewPassWord.this, Login.class);
        startActivity(intent);
        finish(); // 关闭当前注册页，返回登录页后无法回退，体验更好
    }
}

