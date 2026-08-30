package com.example.yangdujun;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BottomNavHelper {
    // 统一颜色常量，方便维护
    public static final String SELECT_COLOR = "#29A867"; // 选中-绿色
    public static final String NORMAL_COLOR = "#666666"; // 未选中-灰色

    // 初始化底部导航：参数1=当前页面，参数2=当前选中的导航ID
    public static void initBottomNav(Activity activity, int selectedNavId) {
        // 绑定底部导航的三个按钮
        LinearLayout llGreenhouse = activity.findViewById(R.id.ll_nav_greenhouse);
        LinearLayout llManage = activity.findViewById(R.id.ll_nav_manage);
        LinearLayout llMine = activity.findViewById(R.id.ll_nav_mine);

        // 设置选中状态：高亮当前页面的导航按钮（绿色），其他灰色
        setNavSelected(llGreenhouse, selectedNavId == R.id.ll_nav_greenhouse);
        setNavSelected(llManage, selectedNavId == R.id.ll_nav_manage);
        setNavSelected(llMine, selectedNavId == R.id.ll_nav_mine);

        // ========== 导航按钮点击跳转逻辑 ==========
        // 点击【大棚】跳转到大棚页面
        if (llGreenhouse != null) {
            llGreenhouse.setOnClickListener(v -> {
                if (selectedNavId != R.id.ll_nav_greenhouse) {
                    activity.startActivity(new Intent(activity, GreenhouseHomePage.class));
                    activity.finish(); // 关闭当前页面，防止返回栈堆积
                }
            });
        }

        // 点击【管理】跳转到管理页面
        if (llManage != null) {
            llManage.setOnClickListener(v -> {
                if (selectedNavId != R.id.ll_nav_manage) {
                    Intent intent = new Intent(activity, ManagePage.class);
                    // 如果当前是大棚页面，传递当前选中的大棚ID
                    if (activity instanceof GreenhouseHomePage) {
                        GreenhouseHomePage greenhousePage = (GreenhouseHomePage) activity;
                        String greenhouseId = greenhousePage.getCurrentGreenhouseId();
                        if (!greenhouseId.isEmpty()) {
                            intent.putExtra("greenhouseId", greenhouseId);
                        }
                    }
                    activity.startActivity(intent);
                    activity.finish(); // 关闭当前页面，防止返回栈堆积
                }
            });
        }

        // 点击【我的】跳转到我的页面/登录页
        if (llMine != null) {
            llMine.setOnClickListener(v -> {
                if (selectedNavId != R.id.ll_nav_mine) {
                    activity.startActivity(new Intent(activity, MinePage.class));
                    activity.finish();
                }
            });
        }
    }

    // 封装：设置导航按钮的选中/未选中样式
    private static void setNavSelected(LinearLayout navItem, boolean isSelected) {
        // 添加空检查，防止导航栏不存在时崩溃
        if (navItem == null || navItem.getChildCount() < 2) {
            return;
        }

        TextView icon = (TextView) navItem.getChildAt(0);
        TextView text = (TextView) navItem.getChildAt(1);
        
        if (icon == null || text == null) {
            return;
        }
        
        if (isSelected) {
            icon.setTextColor(Color.parseColor(SELECT_COLOR));
            text.setTextColor(Color.parseColor(SELECT_COLOR));
            icon.setAlpha(1.0f); // 选中-不透明
        } else {
            icon.setTextColor(Color.parseColor(NORMAL_COLOR));
            text.setTextColor(Color.parseColor(NORMAL_COLOR));
            icon.setAlpha(0.6f); // 未选中-半透明，更美观
        }
    }
}

