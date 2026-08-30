package com.example.yangdujun;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MyVideoView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private String playUrl = "";
    private boolean isPlaying = false;
    private VideoUtils videoUtils = new VideoUtils();

    // 视频原始宽高 (记录摄像头视频的真实宽高，用于比例适配)
    private int videoWidth = 0;
    private int videoHeight = 0;

    public MyVideoView(Context context) {
        super(context);
        initView();
    }

    public MyVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MyVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        mHolder = getHolder();
        mHolder.addCallback(this);
        setZOrderOnTop(false);
    }

    // ========== 你原有播放逻辑 完全保留 一行未改 ==========
    public void startPlay(String url) {
        if (isPlaying && playUrl.equals(url)) {
            android.util.Log.d("MyVideoView", "当前已在播放相同视频，无需重复调用");
            return;
        }
        this.playUrl = url;
        this.isPlaying = true;
        android.util.Log.d("MyVideoView", "------>>调用native方法，路径：" + url);
        android.util.Log.d("MyVideoView", "Surface状态：" + this.getHolder().getSurface());
        videoUtils.videoplay(url, this.getHolder().getSurface());
    }

    public void stopPlay() {
        this.isPlaying = false;
        videoUtils.videostop();
    }

    public void release() {
        stopPlay();
        this.playUrl = "";
        this.videoWidth = 0;
        this.videoHeight = 0;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Surface尺寸变化时，重新计算缩放比例，保证画面不变形
        if (videoWidth >0 && videoHeight>0){
            setSurfaceSize(videoWidth, videoHeight);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPlay();
    }

    // ========== 核心新增：设置视频真实宽高 (给native层调用，适配精度更高) ==========
    public void setVideoSize(int videoW, int videoH) {
        this.videoWidth = videoW;
        this.videoHeight = videoH;
        if (videoW >0 && videoH>0){
            setSurfaceSize(videoW, videoH);
        }
    }

    // ========== 核心修复：SurfaceView官方推荐 等比例适配核心方法【无崩溃、不变形、居中显示】 ==========
    private void setSurfaceSize(int videoW, int videoH) {
        if (videoW <=0 || videoH <=0) return;
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // 计算视频原始宽高比 和 控件宽高比
        float videoRatio = (float) videoW / videoH;
        float viewRatio = (float) viewWidth / viewHeight;

        int surfaceW, surfaceH;
        // 核心逻辑：等比例缩放，保证画面不变形
        if (videoRatio > viewRatio) {
            // 视频更宽，按控件宽度适配，高度等比例缩小
            surfaceW = viewWidth;
            surfaceH = (int) (viewWidth / videoRatio);
        } else {
            // 视频更高，按控件高度适配，宽度等比例缩小
            surfaceH = viewHeight;
            surfaceW = (int) (viewHeight * videoRatio);
        }
        // 给Surface设置等比例的尺寸，这是SurfaceView适配的安全写法，不会崩溃！
        mHolder.setFixedSize(surfaceW, surfaceH);
    }

    // ========== 核心重写：计算控件显示尺寸 适配所有设备【手机/平板】 ==========
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        // 有视频尺寸时，按比例计算控件高度，无视频尺寸时用布局默认值
        if (videoWidth > 0 && videoHeight > 0) {
            float videoRatio = (float) videoWidth / videoHeight;
            int finalHeight = (int) (viewWidth / videoRatio);
            // 防止画面过高，限制最大高度，兼容你的布局
            finalHeight = Math.min(finalHeight, dip2px(getContext(), 300));
            setMeasuredDimension(viewWidth, finalHeight);
        } else {
            // 未获取视频尺寸时，使用布局的默认高度，不变形
            setMeasuredDimension(viewWidth, Math.min(viewHeight, dip2px(getContext(), 250)));
        }
    }

    // ========== 工具方法：dp转px 适配所有设备分辨率 ==========
    private int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}

