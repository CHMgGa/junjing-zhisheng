# YOLO 病虫害识别模块

本模块为“菌境智生：羊肚菌智慧种植调控系统”的病虫害识别功能提供支持，主要用于对羊肚菌图像或视频中的异常目标进行检测，并将识别结果作为系统告警和辅助诊断依据。

## 功能说明

- 支持上传羊肚菌种植现场视频进行抽帧识别。
- 基于训练后的 YOLO 模型识别病虫害及健康状态。
- 输出最高置信度识别结果和对应截图。
- 支持将识别结果上报为系统中的 `AIWarning` 字段，用于后续告警展示和联动处理。

## 文件说明

- `disease_video_web_demo.py`：YOLO 病虫害识别 Web 演示程序。
- `disease_best.pt`：训练后的 YOLO 病虫害识别模型权重文件。

## 识别类别

| 类别 | 含义 |
| --- | --- |
| `cobweb` | 蛛网病 |
| `healthy` | 健康 |
| `pest` | 虫害 |
| `slug` | 蛞蝓 |
| `white_mold` | 白霉病 |

## 运行环境

建议使用 Python 3.9 及以上版本。运行前需要安装以下依赖：

```bash
pip install flask opencv-python torch ultralytics paho-mqtt
```

## 启动方式

在当前目录下执行：

```bash
python disease_video_web_demo.py
```

启动后在浏览器打开：

```text
http://127.0.0.1:5050
```

进入页面后上传视频，系统会自动抽帧识别，并展示识别结果截图、最终类别、最高置信度和云端上报状态。

## 配置说明

程序支持通过环境变量配置华为云 IoTDA 设备接入信息，避免在代码中直接写入真实密钥。

常用环境变量包括：

```text
HUAWEI_IOT_DEVICE_ID
HUAWEI_IOT_USERNAME
HUAWEI_IOT_PASSWORD
HUAWEI_IOT_CLIENT_ID
HUAWEI_IOT_HOST
HUAWEI_IOT_PORT
HUAWEI_IOT_SERVICE_ID
HUAWEI_IOT_PROPERTY_ID
```

上传公开仓库前，请勿在代码中保留真实设备密码、云平台密钥或其他敏感信息。

## 说明

本仓库保留病虫害识别演示代码和训练后的模型权重文件。训练数据集包含大量图片和标注文件，未随仓库上传，可根据比赛材料要求单独提供。
