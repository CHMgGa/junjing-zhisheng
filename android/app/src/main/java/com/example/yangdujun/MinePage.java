package com.example.yangdujun;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import com.example.yangdujun.model.User;
import com.example.yangdujun.utils.ConfigStorage;
import com.example.yangdujun.utils.UserStorage;
import com.example.yangdujun.utils.GreenhouseStorage;
import com.example.yangdujun.utils.FeedbackStorage;
import com.example.yangdujun.utils.SettingsStorage;

public class MinePage extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.minepage);
        // 初始化底部导航（当前选中"我的"）
        BottomNavHelper.initBottomNav(this, R.id.ll_nav_mine);

        // 初始化用户存储
        UserStorage userStorage = new UserStorage();

        // 检查登录状态
        if (!userStorage.isLoggedIn()) {
            // 未登录状态，显示登录提示并跳转到登录页面
            TextView tvName = findViewById(R.id.tvName);
            if (tvName != null) tvName.setText("未登录");

            // 添加点击头像或用户名跳转到登录页面
            View ivAvatar = findViewById(R.id.ivAvatar);
            if (ivAvatar != null) {
                ivAvatar.setOnClickListener(v -> {
                    Intent intent = new Intent(MinePage.this, LoginActivities.class);
                    startActivity(intent);
                });
            }

            View tvNameView = findViewById(R.id.tvName);
            if (tvNameView != null) {
                tvNameView.setOnClickListener(v -> {
                    Intent intent = new Intent(MinePage.this, LoginActivities.class);
                    startActivity(intent);
                });
            }
        } else {
            // 已登录状态，显示用户信息
            User currentUser = userStorage.getUser();

            // 更新UI显示
            TextView tvName = findViewById(R.id.tvName);
            ImageView ivAvatar = findViewById(R.id.ivAvatar);

            if (currentUser != null) {
                // 显示用户名
                if (tvName != null) tvName.setText(currentUser.getNickname());
                // 这里可以根据用户头像URL加载头像，目前使用默认头像

                // 添加个人信息编辑功能
                View.OnClickListener editInfoListener = v -> {
                    Intent intent = new Intent(MinePage.this, EditProfileActivity.class);
                    startActivity(intent);
                };

                if (ivAvatar != null) ivAvatar.setOnClickListener(editInfoListener);
                if (tvName != null) tvName.setOnClickListener(editInfoListener);
            }
        }

        View itemGreenhouse = findViewById(R.id.itemGreenhouse);
        View itemFeedback = findViewById(R.id.itemFeedback);
        View itemLogout = findViewById(R.id.itemLogout);
        View itemSettings = findViewById(R.id.itemSettings);
        View itemWeather = findViewById(R.id.itemWeather);
        View deepseek = findViewById(R.id.Deepseek);

        // 初始化配置存储
        ConfigStorage configStorage = new ConfigStorage(this);
        JSONObject featureConfig = configStorage.getFeatureConfig();

        // 我的大棚
        if (configStorage.isFeatureEnabled("greenhouse") && itemGreenhouse != null) {
            itemGreenhouse.setVisibility(View.VISIBLE);
            itemGreenhouse.setOnClickListener(v -> {
                String title = configStorage.getFeatureTitle("greenhouse");
                Toast.makeText(MinePage.this, "点击了" + title, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MinePage.this, GreenhouseListActivity.class));
            });
        } else if (itemGreenhouse != null) {
            itemGreenhouse.setVisibility(View.GONE);
        }

        // 问题反馈
        if (configStorage.isFeatureEnabled("feedback") && itemFeedback != null) {
            itemFeedback.setVisibility(View.VISIBLE);
            itemFeedback.setOnClickListener(v -> {
                String title = configStorage.getFeatureTitle("feedback");
                Toast.makeText(MinePage.this, "点击了" + title, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MinePage.this, FeedbackActivity.class));
            });
        } else if (itemFeedback != null) {
            itemFeedback.setVisibility(View.GONE);
        }

        // 设置
        if (configStorage.isFeatureEnabled("settings") && itemSettings != null) {
            itemSettings.setVisibility(View.VISIBLE);
            itemSettings.setOnClickListener(v -> {
                String title = configStorage.getFeatureTitle("settings");
                Toast.makeText(MinePage.this, "点击了" + title, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MinePage.this, SettingsActivity.class);
                // 传递大棚ID（如果有）
                String greenhouseId = getIntent().getStringExtra("greenhouseId");
                if (greenhouseId != null) {
                    intent.putExtra("greenhouseId", greenhouseId);
                }
                startActivityForResult(intent, 1);
            });
        } else if (itemSettings != null) {
            itemSettings.setVisibility(View.GONE);
        }

        // 智能天气
        if (itemWeather != null) {
            itemWeather.setVisibility(View.VISIBLE);
            itemWeather.setOnClickListener(v -> {
                Toast.makeText(MinePage.this, "点击了智能天气", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MinePage.this, WeatherActivity.class));
            });
        }

        // AI对话
        if (configStorage.isFeatureEnabled("ai") && deepseek != null) {
            deepseek.setVisibility(View.VISIBLE);
            deepseek.setOnClickListener(v -> {
                String title = configStorage.getFeatureTitle("ai");
                Toast.makeText(MinePage.this, "点击了" + title, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MinePage.this, MineDeepseek.class));
            });
        } else if (deepseek != null) {
            deepseek.setVisibility(View.GONE);
        }

        if (itemLogout != null) {
            itemLogout.setOnClickListener(v -> showLogoutDialog());
        }
    }

    private void showLogoutDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_logout, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(v)
                .create();

        TextView tvLogout = v.findViewById(R.id.tvLogout);
        TextView tvBack = v.findViewById(R.id.tvBack);

        if (tvBack != null) {
            tvBack.setOnClickListener(x -> dialog.dismiss());
        }

        if (tvLogout != null) {
            tvLogout.setOnClickListener(x -> {
                dialog.dismiss();
                // 清除所有相关存储数据
                UserStorage userStorage = new UserStorage();
                userStorage.clearUser();

                GreenhouseStorage greenhouseStorage = new GreenhouseStorage();
                greenhouseStorage.clearGreenhouseList();

                FeedbackStorage feedbackStorage = new FeedbackStorage();
                feedbackStorage.clearFeedbacks();

                SettingsStorage settingsStorage = new SettingsStorage();
                settingsStorage.clearSettings();

                // 跳转到登录页面
                Intent intent = new Intent(MinePage.this, LoginActivities.class);
                startActivity(intent);
                finish();
                Toast.makeText(MinePage.this, "已退出登录", Toast.LENGTH_SHORT).show();
            });
        }

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // 从设置页面返回，更新大棚ID
            String greenhouseId = data.getStringExtra("greenhouseId");
            if (greenhouseId != null) {
                // 保持大棚ID一致性
                getIntent().putExtra("greenhouseId", greenhouseId);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 重新加载用户信息，确保从编辑页面返回时能更新显示
        UserStorage userStorage = new UserStorage();
        TextView tvName = findViewById(R.id.tvName);
        ImageView ivAvatar = findViewById(R.id.ivAvatar);

        if (userStorage.isLoggedIn()) {
            User currentUser = userStorage.getUser();
            if (currentUser != null) {
                tvName.setText(currentUser.getNickname());

                // 加载用户头像
                if (currentUser.getAvatar() != null && !currentUser.getAvatar().isEmpty()) {
                    try {
                        android.net.Uri avatarUri = android.net.Uri.parse(currentUser.getAvatar());
                        ivAvatar.setImageURI(avatarUri);
                    } catch (Exception e) {
                        // 头像加载失败，使用默认头像
                        ivAvatar.setImageResource(R.drawable.image_1);
                    }
                } else {
                    // 用户没有头像，使用默认头像
                    ivAvatar.setImageResource(R.drawable.image_1);
                }

                // 添加个人信息编辑功能
                View.OnClickListener editInfoListener = v -> {
                    Intent intent = new Intent(MinePage.this, EditProfileActivity.class);
                    startActivity(intent);
                };
                ivAvatar.setOnClickListener(editInfoListener);
                tvName.setOnClickListener(editInfoListener);
            }
        } else {
            tvName.setText("未登录");
            ivAvatar.setImageResource(R.drawable.image_1);
            // 添加点击头像或用户名跳转到登录页面
            ivAvatar.setOnClickListener(v -> {
                Intent intent = new Intent(MinePage.this, LoginActivities.class);
                startActivity(intent);
            });
            tvName.setOnClickListener(v -> {
                Intent intent = new Intent(MinePage.this, LoginActivities.class);
                startActivity(intent);
            });
        }

        // 重新加载功能配置
        updateFeatureVisibility();
    }

    // 更新功能可见性
    private void updateFeatureVisibility() {
        ConfigStorage configStorage = new ConfigStorage(this);

        View itemGreenhouse = findViewById(R.id.itemGreenhouse);
        View itemFeedback = findViewById(R.id.itemFeedback);
        View itemSettings = findViewById(R.id.itemSettings);
        View itemWeather = findViewById(R.id.itemWeather);
        View deepseek = findViewById(R.id.Deepseek);

        // 我的大棚
        if (configStorage.isFeatureEnabled("greenhouse")) {
            itemGreenhouse.setVisibility(View.VISIBLE);
        } else {
            itemGreenhouse.setVisibility(View.GONE);
        }

        // 问题反馈
        if (configStorage.isFeatureEnabled("feedback")) {
            itemFeedback.setVisibility(View.VISIBLE);
        } else {
            itemFeedback.setVisibility(View.GONE);
        }

        // 设置
        if (configStorage.isFeatureEnabled("settings")) {
            itemSettings.setVisibility(View.VISIBLE);
        } else {
            itemSettings.setVisibility(View.GONE);
        }

        // 智能天气
        if (itemWeather != null) {
            itemWeather.setVisibility(View.VISIBLE);
        }

        // AI对话
        if (configStorage.isFeatureEnabled("ai")) {
            deepseek.setVisibility(View.VISIBLE);
        } else {
            deepseek.setVisibility(View.GONE);
        }
    }
}
