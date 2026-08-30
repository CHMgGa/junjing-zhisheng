#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <unistd.h>
#include <libavfilter/avfilter.h>
#include <libavutil/opt.h>
#include <libavfilter/buffersrc.h>
#include <libavfilter/buffersink.h>
//编码
#include "include/libavcodec/avcodec.h"
//封装格式处理
#include "include/libavformat/avformat.h"
//像素处理
#include "include/libswscale/swscale.h"
#include "include/libavutil/avutil.h"
#include "include/libavutil/frame.h"
#include "include/libavutil/pixdesc.h"
#include "include/libavutil/imgutils.h"

#define LOGI(FORMAT,...) __android_log_print(ANDROID_LOG_INFO,"heiko",FORMAT,__VA_ARGS__);
#define LOGE(FORMAT,...) __android_log_print(ANDROID_LOG_ERROR,"heiko",FORMAT,__VA_ARGS__);

JNIEXPORT jstring JNICALL
Java_com_example_yangdujun_VideoUtils_ffmpegInfo(JNIEnv *env, jclass type) {
    char info[10000] = { 0 };
    sprintf(info, "%s\n", avcodec_configuration());
//    sprintf(info, "%s\n","aaa");
    return (*env)->NewStringUTF(env, info);
}
JNIEXPORT jstring JNICALL
Java_com_example_yangdujun_VideoUtils_ffmpeg(JNIEnv *env, jclass type) {
    char info[10000] = { 0 };
//    sprintf(info, "%s\n", avcodec_configuration());
    sprintf(info, "%s\n","bbb");
    return (*env)->NewStringUTF(env, info);
}
JNIEXPORT void JNICALL
Java_com_example_yangdujun_VideoUtils_decode(JNIEnv *env, jclass type, jstring input_jstr,
                                          jstring output_jstr) {
    const char *input_cstr = (*env)->GetStringUTFChars(env, input_jstr, NULL);
    const char *output_cstr = (*env)->GetStringUTFChars(env, output_jstr, NULL);

    //1.注册所有组件
    av_register_all();

//封装格式上下文，统领全局的结构体，保存了视频文件封装格式的相关信息
    AVFormatContext *pFormatCtx = avformat_alloc_context();
    if (avformat_open_input(&pFormatCtx, input_cstr, NULL, NULL) != 0) {
        LOGE("无法打开视频文件:%s", input_cstr);
        return;
    }
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE("%s", "无法获取视频文件信息")
        return;
    }

//获取视频流的索引位置
//遍历所有类型的流 (音频流、视频流、字幕流)，找到视频流
    int v_stream_idx = -1;
//number of streams
    for (int i = 0; i < pFormatCtx->nb_streams; ++i) {
        //流的类型
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            v_stream_idx = i;
            break;
        }
    }

    if (v_stream_idx == -1) {
        LOGE("%s", "找不到视频流\n");
        return;
    }

//只有知道视频的编码方式，才能够根据编码方式去找到解码器
//获取视频流中的编码上下文
    AVCodecContext *pCodecCtx = pFormatCtx->streams[v_stream_idx]->codec;
    AVCodec *pCodec = avcodec_find_decoder(pCodecCtx->codec_id);

    if (pCodec == NULL) {
        //这里可以临时下载一个解码器
        LOGE("%s", "找不到解码器\n");
        return;
    }
    if (avcodec_open2(pCodecCtx, pCodec, NULL < 0)) {
        LOGE("%s", "解码器无法打开\n");
        return;
    }

//准备读取
//AVPacket用于存储一帧一帧的压缩数据 (H264)
//缓冲区，开辟空间
    AVPacket *packet = (AVPacket *) av_malloc(sizeof(AVPacket));

//AVFrame用于存储解码后的像素数据(YUV)
//内存分配
    AVFrame *pFrame = av_frame_alloc();
//YUV420
    AVFrame *pFrameYUV = av_frame_alloc();
//只有指定了AVFrame的像素格式、画面大小才能真正分配内存
//缓冲区分配内存
    uint8_t *out_buffer = (uint8_t *) av_malloc(
            avpicture_get_size(AV_PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height));

//初始化缓冲区
    avpicture_fill((AVPicture *) pFrameYUV, out_buffer, AV_PIX_FMT_YUV420P, pCodecCtx->width,
                   pCodecCtx->height);

//用于转码 (缩放) 的参数，转之前的宽高，转之后的宽高，格式等
    struct SwsContext *sws_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height,
                                                pCodecCtx->pix_fmt,
                                                pCodecCtx->width, pCodecCtx->height,
                                                AV_PIX_FMT_YUV420P, SWS_BICUBIC, NULL, NULL, NULL);
    int got_picture, ret;
    FILE *fp_yuv = fopen(output_cstr, "wb+");
    int frame_count = 0;

//6.一帧一帧读取压缩数据
    while (av_read_frame(pFormatCtx, packet) >= 0) {
        //只要视频压缩数据 (根据流的索引位置判断)
        if(packet->stream_index == v_stream_idx){
            //7.解码一帧视频压缩数据，得到视频像素数据
            ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
            if(ret < 0){
                LOGE("%s","解码错误");
                return;
            }

            //为0说明解码完成，非0正在解码
            if(got_picture){
                //AVFrame转为像素格式YUV420，宽高
                //参数2、6:输入、输出数据
                //参数3、7:输入、输出画面一行的数据大小 AVFrame转换是一行一行转换的
                //参数4:输入数据第一列要转码的位置 从0开始
                //参数5:输入画面的高度
                sws_scale(sws_ctx, pFrame->data, pFrame->linesize, 0, pCodecCtx->height,
                          pFrameYUV->data, pFrameYUV->linesize);

                //输出到YUV文件
                //AVFrame像素帧写入文件
                //data解码后的图像像素数据 (音频采样数据)
                //Y 亮度 U 色度 (压缩了) 人对亮度更加敏感
                //U V 个数是Y的1/4
                int y_size = pCodecCtx->width * pCodecCtx->height;
                fwrite(pFrameYUV->data[0], 1, y_size, fp_yuv); //Y
                fwrite(pFrameYUV->data[1], 1, y_size / 4, fp_yuv); //U
                fwrite(pFrameYUV->data[2], 1, y_size / 4, fp_yuv); //V

                frame_count++;
                LOGI("解码第%d帧",frame_count);
            }
        }

        //释放资源
        av_free_packet(packet);
    }
    fclose(fp_yuv);

    (*env)->ReleaseStringUTFChars(env,input_jstr,input_cstr);
    (*env)->ReleaseStringUTFChars(env,output_jstr,output_cstr);

    av_frame_free(&pFrame);

    avcodec_close(pCodecCtx);

    avformat_free_context(pFormatCtx);

}
static AVFormatContext *pFormatCtx;
static AVCodecContext *vCodecCtx;
ANativeWindow* nativeWindow;


static AVPacket *vPacket;
static AVFrame *vFrame, *pFrameRGBA;
static AVStream *o_video_stream;
static AVStream *i_video_stream;
static AVFormatContext *o_fmt_ctx;
JNIEXPORT void JNICALL
Java_com_example_yangdujun_VideoUtils_videoplay(JNIEnv *env, jobject instance, jstring path_, jobject surface) {
    // 记录结果
//    const char *filename = "mm.mp4";
    int result;
    int bStop = 0;
    // R1 Java String -> C String
    const char *path = (*env)->GetStringUTFChars(env, path_, 0);
    LOGE("path:%s", path);
//    const char *output_cstr = (*env)->GetStringUTFChars(env, output_jstr, NULL);
//    LOGE("outpath1:%s", outpath_);
//    const char *outpath = (*env)->GetStringUTFChars(env, outpath_, NULL);
//    LOGE("outpath2:%s", outpath);
    // 注册 FFmpeg 组件
    av_register_all();
    avcodec_register_all();
    avformat_network_init();
    // R2 初始化 AVFormatContext 上下文
    AVFormatContext *format_context = avformat_alloc_context();
    // 打开视频文件

    AVDictionary *opts = NULL;
// 核心修改：将udp改为tcp，彻底切换到TCP传输  程国辉 2026年1月16日
    av_dict_set(&opts, "rtsp_transport", "tcp", 0);
    av_dict_set(&opts, "stimeout", "5000000", 0);    // 超时5秒（适配5G网络延迟）
    av_dict_set(&opts, "buffer_size", "1048576", 0); // 增大接收缓冲区（1MB，应对5G带宽波动）
    av_dict_set(&opts, "max_delay", "200000", 0);    // 增大最大延迟（200ms，应对5G抖动）

// 后续打开RTSP流的代码不变
    result = avformat_open_input(&format_context, path, NULL, &opts);
    av_dict_free(&opts);  // 释放配置字典

    //result = avformat_open_input(&format_context, path, NULL, NULL);
    LOGE("result :%d\r\n", result);
    if (result < 0) {
        LOGE("Player Error :%s", " Can not open video file");
        return;
    }


    // 查找视频文件的流信息
    result = avformat_find_stream_info(format_context, NULL);
    if (result < 0) {
        LOGE("Player Error:%s", "Can not find video file stream info");
        return;
    }
    // 查找视频编码器
    int video_stream_index = -1;
    for (int i = 0; i < format_context->nb_streams; i++) {
        // 匹配视频流
        if (format_context->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_index = i;
//            i_video_stream = format_context->streams[i];
            break;
        }
    }

    // 没找到视频流
    if (video_stream_index == -1) {
        LOGE("Player Error:%s", " Can not find video stream");
        return;
    }


    // 初始化视频编码器上下文
    AVCodecContext *video_codec_context = avcodec_alloc_context3(NULL);
    avcodec_parameters_to_context(video_codec_context,
                                  format_context->streams[video_stream_index]->codecpar);
    // 初始化视频编码器
    AVCodec *video_codec = avcodec_find_decoder(video_codec_context->codec_id);
    if (video_codec == NULL) {
        LOGE("Player Error :%s", " Can not find video codec");
        return;
    }
    // R3 打开视频解码器
    result = avcodec_open2(video_codec_context, video_codec, NULL);
    if (result < 0) {
        LOGE("Player Error :%s", " Can not find video stream");
        return;
    }

    // 获取视频的宽高
    int videoWidth = video_codec_context->width;
    int videoHeight = video_codec_context->height;

    if(videoWidth==0||videoHeight==0) return;

    LOGE("HEI=%d,wei=%d\r\n",videoHeight,videoWidth);
    // R4 初始化 Native Window 用于播放视频
    ANativeWindow *native_window = ANativeWindow_fromSurface(env, surface);
    if (native_window == NULL) {
        LOGE("Player Error:%s", "Can not create native window");
        return;
    }
    // 通过设置宽高限制缓冲区中的像素数量，而非屏幕的物理显示尺寸。
    // 如果缓冲区与物理屏幕的显示尺寸不相符，则实际显示可能会是拉伸，或者被压缩的图像
    result = ANativeWindow_setBuffersGeometry(native_window, videoWidth, videoHeight,
                                              WINDOW_FORMAT_RGBA_8888);
    if (result < 0) {
        LOGE("Player Error:%s", " Can not set native window buffer");
        ANativeWindow_release(native_window);
        return;
    }
    // 定义绘图缓冲区
    ANativeWindow_Buffer window_buffer;
    // 声明数据容器 有3个
    // R5 解码前数据容器 Packet 编码数据
    AVPacket *packet = av_packet_alloc();
    // R6 解码后数据容器 Frame 像素数据 不能直接播放像素数据 还要转换
    AVFrame *frame = av_frame_alloc();
    // R7 转换后数据容器 这里面的数据可以用于播放
    AVFrame *rgba_frame = av_frame_alloc();
    // 数据格式转换准备
    // 输出 Buffer
    int buffer_size = av_image_get_buffer_size(AV_PIX_FMT_RGBA, videoWidth, videoHeight, 1);
    // R8 申请 Buffer 内存
    uint8_t *out_buffer = (uint8_t *) av_malloc(buffer_size * sizeof(uint8_t));
    av_image_fill_arrays(rgba_frame->data, rgba_frame->linesize, out_buffer, AV_PIX_FMT_RGBA,
                         videoWidth, videoHeight, 1);
    // R9 数据格式转换上下文
    struct SwsContext *data_convert_context = sws_getContext(
            videoWidth, videoHeight, video_codec_context->pix_fmt,
            videoWidth, videoHeight, AV_PIX_FMT_RGBA,
            SWS_BICUBIC, NULL, NULL, NULL);

    // 开始读取帧


    while (av_read_frame(format_context, packet) >= 0) {

        // 匹配视频流
        if (packet->stream_index == video_stream_index) {
            // 解码
            result = avcodec_send_packet(video_codec_context, packet);
            if (result < 0 && result != AVERROR(EAGAIN) && result != AVERROR_EOF) {

                LOGE("Player Error:%s", " codec step 1 fail");
                return;
            }
            if (result = avcodec_receive_frame(video_codec_context, frame) != 0) continue;
//            result = avcodec_receive_frame(video_codec_context, frame);
//                if (result < 0 && result != AVERROR_EOF) {
//                    LOGE("Player Error:%s", " codec step 2 fail");
//                    LOGI("失败%d", result);
//                    return;
//                }
            // 数据格式转换
            result = sws_scale(
                    data_convert_context,
                    (const uint8_t *const *) frame->data, frame->linesize,
                    0, videoHeight,
                    rgba_frame->data, rgba_frame->linesize);
            if (result <= 0) {
                LOGE("Player Error:%s", " data convert fail");
                return;
            }
            // 播放
            result = ANativeWindow_lock(native_window, &window_buffer, NULL);
            if (result < 0) {
                LOGE("Player Error :%s", " Can not lock native window");
            } else {
                // 将图像绘制到界面上
                // 注意 : 这里 rgba_frame 一行的像素和 window_buffer 一行的像素长度可能不一致
                // 需要转换好 否则可能花屏
                LOGE("rgba_frame line size :%d\r\n",rgba_frame->linesize[0]);

                LOGE("window_buffer line size :%d\r\n",window_buffer.stride);
                LOGE("videoHeight=%d\r\n",videoHeight);
                uint8_t *bits = (uint8_t *) window_buffer.bits;
                for (int h = 0; h < videoHeight; h++) {
                    memcpy(bits + h * window_buffer.stride * 4,
                           out_buffer + h * rgba_frame->linesize[0],
                           rgba_frame->linesize[0]);
                }
                ANativeWindow_unlockAndPost(native_window);
            }
        }
    }

//            av_write_trailer(format_context);
    // 释放 packet 引用
    av_packet_unref(packet);

}
static AVFormatContext *i_fmt_ctx;
static AVStream *i_video_stream;

static AVFormatContext *o_fmt_ctx;
static AVStream *o_video_stream;

jboolean flag=JNI_TRUE;
JNIEXPORT void JNICALL
Java_com_example_yangdujun_VideoUtils_videosave(JNIEnv *env,jclass type, jobject inpath_,jstring outpath_) {
    const char *rtspUrl = (*env)->GetStringUTFChars(env, inpath_, 0);
    const char *filename = (*env)->GetStringUTFChars(env, outpath_, 0);
    avcodec_register_all();
    av_register_all();
    avformat_network_init();

    /* should set to NULL so that avformat_open_input() allocate a new one */
    i_fmt_ctx = NULL;
//    char rtspUrl[] = "rtsp://admin:12345@192.168.10.76:554";
//    const char *filename = "1.mp4";
    if (avformat_open_input(&i_fmt_ctx, rtspUrl, NULL, NULL)!=0)
    {
        fprintf(stderr, "could not open input file\n");
        return ;
    }

    if (avformat_find_stream_info(i_fmt_ctx, NULL)<0)
    {
        fprintf(stderr, "could not find stream info\n");
        return ;
    }

    //av_dump_format(i_fmt_ctx, 0, argv[1], 0);

    /* find first video stream */
    for (unsigned i=0; i<i_fmt_ctx->nb_streams; i++)
    {
        if (i_fmt_ctx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO)
        {
            i_video_stream = i_fmt_ctx->streams[i];
            break;
        }
    }
    if (i_video_stream == NULL)
    {
        fprintf(stderr, "didn't find any video stream\n");
        return ;
    }

    avformat_alloc_output_context2(&o_fmt_ctx, NULL, NULL, filename);

    /*
    * since all input files are supposed to be identical (framerate, dimension, color format, ...)
    * we can safely set output codec values from first input file
    */


    o_video_stream = avformat_new_stream(o_fmt_ctx, NULL);
    {
        AVCodecContext *avctx = o_video_stream->codec;
        avctx->codec_type = AVMEDIA_TYPE_VIDEO;
        /*此处,需指定编码后的H264数据的分辨率、帧率及码率*/
        avctx->codec_id = AV_CODEC_ID_H264;
        avctx->bit_rate = 2000000;
        avctx->width = 640;
        avctx->height = 480;
        avctx->time_base.num = 1;
        avctx->time_base.den = 30;

//        AVCodecContext *c;
//        c = o_video_stream->codec;
//        c->bit_rate = 400000;
//        c->codec_id = i_video_stream->codec->codec_id;
//        c->codec_type = i_video_stream->codec->codec_type;
//        c->time_base.num = i_video_stream->time_base.num;
//        c->time_base.den = i_video_stream->time_base.den;
//        fprintf(stderr, "time_base.num = %d time_base.den = %d\n", c->time_base.num, c->time_base.den);
//        c->width = i_video_stream->codec->width;
//        c->height = i_video_stream->codec->height;
//        c->pix_fmt = i_video_stream->codec->pix_fmt;
//        printf("%d %d %d", c->width, c->height, c->pix_fmt);
//        c->flags = i_video_stream->codec->flags;
//        c->flags |= CODEC_FLAG_GLOBAL_HEADER;
//        c->me_range = i_video_stream->codec->me_range;
//        c->max_qdiff = i_video_stream->codec->max_qdiff;
//
//        c->qmin = i_video_stream->codec->qmin;
//        c->qmax = i_video_stream->codec->qmax;
//
//        c->qcompress = i_video_stream->codec->qcompress;
    }
    int ret;
    av_dump_format(o_fmt_ctx, 0, filename, 1);
    if (!(o_fmt_ctx->flags & AVFMT_NOFILE))
    {
        ret = avio_open(&o_fmt_ctx->pb, filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("Could not open output file:%s", filename);
            return;
        }
        LOGE(":%s", "Open output file success!");
    }

//    avio_open(&o_fmt_ctx->pb, filename, AVIO_FLAG_WRITE);
    ret = avformat_write_header(o_fmt_ctx, NULL);
    if (ret < 0) {

        LOGE(":%s", " write mp4 file header failed!");
        return;
    }
    LOGE(":%s", " Write avi header success!");


//    avformat_write_header(o_fmt_ctx, NULL);

    int last_pts = 0;
    int last_dts = 0;

    int64_t pts, dts;
    int Fflag=1;
    while (flag)
    {
        AVPacket i_pkt;
        av_init_packet(&i_pkt);
        i_pkt.size = 0;
        i_pkt.data = NULL;
        if (av_read_frame(i_fmt_ctx, &i_pkt) <0 )
            break;
        /*
        * pts and dts should increase monotonically
        * pts should be >= dts
        */



        if(i_pkt.flags &AV_PKT_FLAG_KEY)

        {
            LOGE(":%s", " 为关键帧");
//            i_pkt.flags|= AV_PKT_FLAG_KEY;

        }else{

            LOGE(":%s", " 不为关键帧");
//            i_pkt.flags|= AV_PKT_FLAG_KEY;

        }

//    i_pkt.dts = i_pkt.pts = AV_NOPTS_VALUE;
//    i_pkt.size = size; /*帧大小*/
//
//    i_pkt.data = data; /*帧数据*/


        i_pkt.stream_index = 0;
        i_pkt.pts = av_rescale_q_rnd(i_pkt.pts, i_video_stream->time_base, o_video_stream->time_base, AV_ROUND_NEAR_INF);
        LOGE("i_pkt.pts:%s", i_pkt.pts);
        LOGE(" i_pkt.dts:%s", i_pkt.dts);
        i_pkt.dts = av_rescale_q_rnd(i_pkt.dts, i_video_stream->time_base, o_video_stream->time_base, AV_ROUND_NEAR_INF);
        i_pkt.duration = av_rescale_q(i_pkt.duration, i_video_stream->time_base, o_video_stream->time_base);
        i_pkt.flags|= AV_PKT_FLAG_KEY;
        pts = i_pkt.pts;
        i_pkt.pts += last_pts;
        dts = i_pkt.dts;
        i_pkt.dts += last_dts;
        //printf("%lld %lld\n", i_pkt.pts, i_pkt.dts);
        static int num = 1;
        LOGI("frame:%d", num++);
//        printf("frame %d\n", num++);
        av_interleaved_write_frame(o_fmt_ctx, &i_pkt);
        //av_free_packet(&i_pkt);
        //av_init_packet(&i_pkt);
//        sleep(1);

    }
    last_dts += dts;
    last_pts += pts;
    avformat_close_input(&i_fmt_ctx);

    av_write_trailer(o_fmt_ctx);

    avcodec_close(o_fmt_ctx->streams[0]->codec);
    av_freep(&o_fmt_ctx->streams[0]->codec);
    av_freep(&o_fmt_ctx->streams[0]);

    avio_close(o_fmt_ctx->pb);
    av_free(o_fmt_ctx);


}
JNIEXPORT void JNICALL
Java_com_example_yangdujun_VideoUtils_videostop(JNIEnv *env, jobject instance){

    flag=JNI_FALSE;
}



JNIEXPORT void JNICALL
Java_com_example_yangdujun_VideoUtils_videosavetest(JNIEnv *env,jclass type1, jobject inpath_,jstring outpath_){






}