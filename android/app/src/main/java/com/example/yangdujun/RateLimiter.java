package com.example.yangdujun;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    private BlockingQueue<Boolean> tokenBucket;
    private ExecutorService executor;
    private final Handler handler;
    private static RateLimiter instance;

    private RateLimiter() {
        // 初始化令牌桶和线程池
        initTokenBucket();
        initExecutor();

        handler = new Handler(Looper.getMainLooper());
        startTokenRefillTask();
    }

    // 单例模式
    public static synchronized RateLimiter getInstance() {
        if (instance == null) {
            instance = new RateLimiter();
        }
        return instance;
    }

    // 初始化令牌桶（容量10，对应10次/秒）
    private void initTokenBucket() {
        tokenBucket = new ArrayBlockingQueue<>(10);
        // 初始化时填充令牌
        try {
            for (int i = 0; i < 10; i++) {
                tokenBucket.put(true);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 初始化线程池（改为可重启）
    private void initExecutor() {
        if (executor != null && !executor.isShutdown() && !executor.isTerminated()) {
            return;
        }
        // 创建核心线程数1、最大线程数1的线程池，避免多线程问题
        executor = new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(100), // 任务队列容量100，避免瞬时请求过多
                new ThreadPoolExecutor.DiscardOldestPolicy() // 队列满时丢弃最旧任务，而非直接抛异常
        );
    }

    // 启动令牌补充任务（每秒补充10个）
    private void startTokenRefillTask() {
        handler.removeCallbacks(tokenRefillRunnable);
        handler.postDelayed(tokenRefillRunnable, 1000);
    }

    // 令牌补充任务
    private final Runnable tokenRefillRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                tokenBucket.clear();
                for (int i = 0; i < 10; i++) {
                    tokenBucket.put(true);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 循环执行
            handler.postDelayed(this, 1000);
        }
    };

    // 获取令牌（非阻塞，超时则返回false）
    private boolean tryAcquire() {
        try {
            // 3秒超时，避免一直阻塞
            return tokenBucket.poll(3, TimeUnit.SECONDS) != null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // 执行限流后的任务（核心修复：先检查线程池状态）
    public void execute(Runnable task) {
        // 1. 检查并重建线程池
        initExecutor();

        // 2. 非空校验
        if (task == null || executor.isShutdown() || executor.isTerminated()) {
            return;
        }

        // 3. 执行任务
        executor.execute(() -> {
            // 获取令牌（超时则放弃执行）
            if (tryAcquire()) {
                task.run();
            } else {
                // 令牌获取超时，可打印日志
                android.util.Log.w("RateLimiter", "获取令牌超时，请求被限流");
            }
        });
    }

    // 安全释放资源（仅在应用退出时调用，而非Activity销毁）
    public void release() {
        handler.removeCallbacks(tokenRefillRunnable);
        if (executor != null) {
            executor.shutdown();
            try {
                // 等待60秒，让未完成的任务执行完毕
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
            executor = null;
        }
        tokenBucket = null;
    }
}

