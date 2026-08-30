package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.example.yangdujun.model.User;
import com.example.yangdujun.utils.LocalUserManager;
import com.example.yangdujun.utils.SettingsStorage;
import com.example.yangdujun.utils.UserStorage;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private String userToken;
    private UserStorage userStorage;
    private SettingsStorage settingsStorage;
    private String greenhouseId; // 当前大棚ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView ivBack = findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> {
                // 返回时传递大棚ID
                Intent intent = new Intent();
                if (greenhouseId != null) {
                    intent.putExtra("greenhouseId", greenhouseId);
                }
                setResult(RESULT_OK, intent);
                finish();
            });
        }

        // 接收传递过来的大棚ID
        Intent intent = getIntent();
        if (intent != null) {
            greenhouseId = intent.getStringExtra("greenhouseId");
        }

        // 初始化存储工具
        userStorage = new UserStorage();
        settingsStorage = new SettingsStorage();
        userToken = userStorage.getToken();

        // 获取当前用户信息
        User user = userStorage.getUser();

        // 加载用户头像
        ImageView ivAvatar = findViewById(R.id.ivAvatar);
        if (ivAvatar != null) {
            if (user != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                try {
                    android.net.Uri avatarUri = android.net.Uri.parse(user.getAvatar());
                    ivAvatar.setImageURI(avatarUri);
                } catch (Exception e) {
                    ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
                }
            } else {
                ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
            }
        }

        // 用户名
        View itemUsername = findViewById(R.id.itemUsername);
        if (itemUsername != null) {
            TextView tvLeft = itemUsername.findViewById(R.id.tvLeft);
            if (tvLeft != null) {
                tvLeft.setText("用户名");
            }
            String username = user != null && user.getNickname() != null ? user.getNickname() : "未设置";
            TextView tvRight = itemUsername.findViewById(R.id.tvRight);
            if (tvRight != null) {
                tvRight.setText(username);
            }
        }

        // 手机号
        View itemPhone = findViewById(R.id.itemPhone);
        if (itemPhone != null) {
            TextView tvLeft = itemPhone.findViewById(R.id.tvLeft);
            if (tvLeft != null) {
                tvLeft.setText("手机号");
            }
            String phone = user != null && user.getPhone() != null ? user.getPhone() : "未设置";
            TextView tvRight = itemPhone.findViewById(R.id.tvRight);
            if (tvRight != null) {
                tvRight.setText(phone);
            }
        }

        // 密码
        View itemPassword = findViewById(R.id.itemPassword);
        if (itemPassword != null) {
            TextView tvLeft = itemPassword.findViewById(R.id.tvLeft);
            if (tvLeft != null) {
                tvLeft.setText("密码");
            }
            TextView tvRight = itemPassword.findViewById(R.id.tvRight);
            if (tvRight != null) {
                tvRight.setText("******");
            }
            itemPassword.setOnClickListener(v -> showChangePasswordDialog());
        }

        // 推送声音
        View itemPush = findViewById(R.id.itemPushSound);
        if (itemPush != null) {
            TextView tvLeft = itemPush.findViewById(R.id.tvLeft);
            if (tvLeft != null) {
                tvLeft.setText("推送声音");
            }
            SwitchCompat swPush = itemPush.findViewById(R.id.sw);
            if (swPush != null) {
                swPush.setChecked(settingsStorage.getSoundSetting("push"));
                swPush.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    saveSoundSetting("push", isChecked);
                });
            }
        }

        // 告警声音
        View itemAlarm = findViewById(R.id.itemAlarmSound);
        if (itemAlarm != null) {
            TextView tvLeft = itemAlarm.findViewById(R.id.tvLeft);
            if (tvLeft != null) {
                tvLeft.setText("告警声音");
            }
            SwitchCompat swAlarm = itemAlarm.findViewById(R.id.sw);
            if (swAlarm != null) {
                swAlarm.setChecked(settingsStorage.getSoundSetting("alarm"));
                swAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    saveSoundSetting("alarm", isChecked);
                });
            }
        }

        // 检查更新
        View itemUpdate = findViewById(R.id.itemCheckUpdate);
        if (itemUpdate != null) {
            TextView tvLeft = itemUpdate.findViewById(R.id.tvLeft);
            if (tvLeft != null) {
                tvLeft.setText("检查更新");
            }
            TextView tvRight = itemUpdate.findViewById(R.id.tvRight);
            if (tvRight != null) {
                tvRight.setText("已是最新版本");
            }
            itemUpdate.setOnClickListener(v -> checkUpdate());
        }
    }

    // 显示修改密码对话框
    private void showChangePasswordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("修改密码")
                .setView(dialogView)
                .setPositiveButton("确定", null) // 稍后设置点击事件
                .setNegativeButton("取消", (dialogInterface, i) -> dialogInterface.dismiss())
                .create();

        dialog.show();

        // 为确定按钮设置点击事件
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String oldPassword = etOldPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "请输入所有密码字段", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 8) {
                Toast.makeText(this, "新密码长度不能少于8位", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "两次输入的新密码不一致", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(oldPassword, newPassword, dialog);
        });
    }

    // 修改密码
    private void changePassword(String oldPassword, String newPassword, AlertDialog dialog) {
        // 使用LocalUserManager修改密码
        LocalUserManager localUserManager = new LocalUserManager(this);
        boolean success = localUserManager.changePassword(userToken, oldPassword, newPassword);
        
        if (success) {
            Toast.makeText(SettingsActivity.this, "密码修改成功", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        } else {
            Toast.makeText(SettingsActivity.this, "密码修改失败，请检查旧密码是否正确", Toast.LENGTH_SHORT).show();
        }
    }

    // 检查更新
    private void checkUpdate() {
        // 本地检查更新（模拟）
        Toast.makeText(SettingsActivity.this, "已是最新版本", Toast.LENGTH_SHORT).show();
    }

    // 显示更新对话框
    private void showUpdateDialog(String version, String description) {
        new AlertDialog.Builder(this)
                .setTitle("发现新版本 " + version)
                .setMessage(description)
                .setPositiveButton("立即更新", (dialogInterface, i) -> {
                    // 这里可以跳转到应用商店或者下载页面
                    Toast.makeText(this, "跳转到更新页面", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("暂不更新", (dialogInterface, i) -> dialogInterface.dismiss())
                .show();
    }

    // 保存声音设置
    private void saveSoundSetting(String type, boolean isEnabled) {
        // 保存到本地存储
        settingsStorage.saveSoundSetting(type, isEnabled);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 重新加载用户信息，确保从编辑页面返回时能更新显示
        User user = userStorage.getUser();
        if (user != null) {
            // 更新用户名显示
            View itemUsername = findViewById(R.id.itemUsername);
            if (itemUsername != null) {
                TextView tvRight = itemUsername.findViewById(R.id.tvRight);
                if (tvRight != null) {
                    tvRight.setText(user.getNickname());
                }
            }
            
            // 更新手机号显示
            View itemPhone = findViewById(R.id.itemPhone);
            if (itemPhone != null) {
                TextView tvRight = itemPhone.findViewById(R.id.tvRight);
                if (tvRight != null) {
                    tvRight.setText(user.getPhone());
                }
            }
            
            // 更新头像显示
            ImageView ivAvatar = findViewById(R.id.ivAvatar);
            if (ivAvatar != null) {
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    try {
                        android.net.Uri avatarUri = android.net.Uri.parse(user.getAvatar());
                        ivAvatar.setImageURI(avatarUri);
                    } catch (Exception e) {
                        ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
                    }
                } else {
                    ivAvatar.setImageResource(R.mipmap.ic_launcher_round);
                }
            }
        }
    }
}


