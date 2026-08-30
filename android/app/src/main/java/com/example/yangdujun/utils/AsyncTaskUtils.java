package com.example.yangdujun.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步任务工具类
 * 用于在后台线程执行耗时操作，如文件IO
 */
public class AsyncTaskUtils {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 执行异步任务
     * @param task 后台任务
     * @param callback 主线程回调
     * @param <T> 任务返回类型
     */
    public static <T> void executeAsync(Runnable task, Runnable callback) {
        executor.execute(() -> {
            try {
                task.run();
                if (callback != null) {
                    handler.post(callback);
                }
            } catch (Exception e) {
                android.util.Log.e("AsyncTaskUtils", "异步任务执行失败", e);
                e.printStackTrace();
            }
        });
    }

    /**
     * 执行有返回值的异步任务
     * @param task 后台任务
     * @param callback 主线程回调
     * @param <T> 任务返回类型
     */
    public static <T> void executeAsync(Callable<T> task, Callback<T> callback) {
        executor.execute(() -> {
            try {
                T result = task.call();
                if (callback != null) {
                    handler.post(() -> callback.onComplete(result));
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) {
                    handler.post(() -> callback.onError(e));
                }
            }
        });
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        executor.shutdown();
    }

    /**
     * 带返回值的任务接口
     * @param <T> 返回类型
     */
    public interface Callable<T> {
        T call() throws Exception;
    }

    /**
     * 回调接口
     * @param <T> 返回类型
     */
    public interface Callback<T> {
        void onComplete(T result);
        void onError(Exception e);
    }
}


