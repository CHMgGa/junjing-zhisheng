package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import com.example.yangdujun.model.ApiResponse;
import com.example.yangdujun.model.User;
import com.example.yangdujun.network.ApiService;
import com.example.yangdujun.utils.JsonUtils;
import com.example.yangdujun.utils.UserStorage;

public class Reg_Page extends AppCompatActivity {
    // 控件声明
    private EditText et_phone, et_password, et_pwd_confirm;
    private ImageView iv_pwd_eye;
    private Button btn_login_register;
    // 两个返回按钮【新增声明，统一管理】
    private ImageButton btn_return1;
    private TextView btn_return2;
    // 用户协议勾选框
    private android.widget.CheckBox cb_agreement;

    // 密码显示隐藏标记
    private boolean isPwdShow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reg_page);

        // 1. 绑定所有控件【统一绑定，只执行一次】
        bindViews();

        // 2. 密码显示/隐藏 点击事件【修复InputType闪退问题】
        iv_pwd_eye.setOnClickListener(v -> {
            isPwdShow = !isPwdShow;
            if (isPwdShow) {
                // 显示密码 - 正确写法：必须拼接TYPE_CLASS_TEXT
                et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                et_pwd_confirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                iv_pwd_eye.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            } else {
                // 隐藏密码 - 原有写法正确，保留
                et_password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                et_pwd_confirm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                iv_pwd_eye.setImageResource(android.R.drawable.ic_menu_view);
            }
            // 光标定位到末尾
            et_password.setSelection(et_password.getText().length());
            et_pwd_confirm.setSelection(et_pwd_confirm.getText().length());
        });

        // 3. 注册按钮点击事件
        btn_login_register.setOnClickListener(v -> {
            String phone = et_phone.getText().toString().trim();
            String password = et_password.getText().toString().trim();
            String pwdConfirm = et_pwd_confirm.getText().toString().trim();

            // 完整校验
            if (TextUtils.isEmpty(phone) || phone.length() != 11) {
                Toast.makeText(this, "请输入正确的手机号码", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 8) {
                Toast.makeText(this, "密码长度不能少于8位", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isPasswordStrong(password)) {
                Toast.makeText(this, "密码必须包含字母和数字", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!password.equals(pwdConfirm)) {
                Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cb_agreement != null && !cb_agreement.isChecked()) {
                Toast.makeText(this, "请阅读并同意用户协议和隐私政策", Toast.LENGTH_SHORT).show();
                return;
            }

            // 显示加载状态
            Toast.makeText(this, "注册中...", Toast.LENGTH_SHORT).show();

            // 网络请求注册（移除验证码参数）
            new Thread(() -> {
                try {
                    String response = ApiService.register(phone, password);
                    ApiResponse<User> apiResponse = JsonUtils.fromJson(response, ApiResponse.class);

                    runOnUiThread(() -> {
                        if (apiResponse != null && apiResponse.isSuccess()) {
                            // 保存用户账号密码到本地存储
                            UserStorage userStorage = new UserStorage();
                            userStorage.saveUserCredentials(phone, password);
                            
                            Toast.makeText(this, "注册成功！即将跳转到登录页", Toast.LENGTH_SHORT).show();
                            // 延迟跳转，让用户看到成功提示
                            new android.os.Handler().postDelayed(() -> {
                                jumpToLogin();
                            }, 1500);
                        } else {
                            String errorMsg = apiResponse != null ? apiResponse.getMessage() : "注册失败，请稍后重试";
                            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "网络错误，请检查网络连接", Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();
        });

        // 6. 返回登录页按钮点击事件【核心修复：只绑定一次，无重复，绝对不闪退】
        btn_return1.setOnClickListener(v -> jumpToLogin());
        btn_return2.setOnClickListener(v -> jumpToLogin());
    }

    // 绑定所有控件【统一管理，精简无冗余】
    private void bindViews() {
        // 输入框
        et_phone = findViewById(R.id.et_phone);
        et_password = findViewById(R.id.et_password);
        et_pwd_confirm = findViewById(R.id.et_pwd_confirm);
        // 小控件
        iv_pwd_eye = findViewById(R.id.iv_pwd_eye);
        // 按钮
        btn_login_register = findViewById(R.id.btn_login_register);
        // 返回按钮【关键：统一声明绑定，解决重复问题】
        btn_return1 = findViewById(R.id.return_1);
        btn_return2 = findViewById(R.id.return_2);
        // 用户协议勾选框
        cb_agreement = findViewById(R.id.cb_agreement);
    }

    // 跳转登录页方法【抽成公共方法，精简代码，避免重复写Intent】
    private void jumpToLogin() {
        Intent intent = new Intent(Reg_Page.this, Login.class);
        startActivity(intent);
        finish(); // 关闭当前注册页，返回登录页后无法回退，体验更好
    }

    // 密码强度检查方法
    private boolean isPasswordStrong(String password) {
        boolean hasLetter = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
            
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        
        return false;
    }

    // 销毁页面时的清理工作
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}

