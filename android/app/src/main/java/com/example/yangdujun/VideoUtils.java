package com.example.yangdujun;

import android.view.Surface;

public class VideoUtils {
    private static boolean isLibLoaded = false;

    static {
        try {
            // 顺序不能乱
            System.loadLibrary("avutil-55");
            System.loadLibrary("swresample-2");
            System.loadLibrary("avcodec-57");
            System.loadLibrary("avformat-57");
            System.loadLibrary("swscale-4");
            System.loadLibrary("postproc-54");
            System.loadLibrary("avfilter-6");
            System.loadLibrary("avdevice-57");
            System.loadLibrary("native-lib");
            isLibLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            isLibLoaded = false;
        }
    }

    // 增加库加载状态判断
    public static boolean isLibLoaded() {
        return isLibLoaded;
    }

    public native String ffmpegInfo();
    public native void decode(String input,String output);
    public native void videoplay(String videoPath,Surface surface);
    public native void videostop();
    public native static void videosave(String videoPath,String outPath);
    public native static void videosavetest(String input,String output);
}

