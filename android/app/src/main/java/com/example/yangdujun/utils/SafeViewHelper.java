package com.example.yangdujun.utils;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

/**
 * 安全视图操作工具类
 * 用于简化空指针检查和Activity生命周期检查
 */
public class SafeViewHelper {

    /**
     * 安全设置TextView文本
     * @param textView TextView对象
     * @param text 要设置的文本
     */
    public static void safeSetText(TextView textView, String text) {
        if (textView != null) {
            textView.setText(text);
        }
    }

    /**
     * 安全设置TextView文本（带默认值）
     * @param textView TextView对象
     * @param text 要设置的文本
     * @param defaultText 默认文本
     */
    public static void safeSetText(TextView textView, String text, String defaultText) {
        if (textView != null) {
            textView.setText(text != null ? text : defaultText);
        }
    }

    /**
     * 安全设置View的点击监听器
     * @param view View对象
     * @param listener 点击监听器
     */
    public static void safeSetOnClickListener(View view, View.OnClickListener listener) {
        if (view != null && listener != null) {
            view.setOnClickListener(listener);
        }
    }

    /**
     * 检查Activity是否有效（未销毁）
     * @param activity Activity对象
     * @return 是否有效
     */
    public static boolean isActivityValid(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    /**
     * 安全执行UI操作（带Activity有效性检查）
     * @param activity Activity对象
     * @param action 要执行的操作
     */
    public static void safeRunOnUiThread(Activity activity, Runnable action) {
        if (isActivityValid(activity) && action != null) {
            activity.runOnUiThread(action);
        }
    }
}
