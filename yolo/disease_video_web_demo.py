# -*- coding: utf-8 -*-
"""Small web demo: upload a video, run disease YOLO, report AIWarning once."""

import json
import os
import ssl
import time
import uuid
from pathlib import Path

import cv2
import torch
from flask import Flask, Response, redirect, render_template_string, request, send_from_directory, url_for

BASE_DIR = Path(__file__).resolve().parent
UPLOAD_DIR = BASE_DIR / "video_demo_uploads"
OUTPUT_DIR = BASE_DIR / "video_demo_outputs"
MODEL_PATH = BASE_DIR / "disease_best.pt"

CONF_THRESHOLD = 0.5
SAMPLE_EVERY_N_FRAMES = 10
MAX_SAMPLED_FRAMES = 120
DISEASE_NAMES = ["cobweb", "healthy", "pest", "slug", "white_mold"]

HUAWEI_DEVICE_ID = os.getenv("HUAWEI_IOT_DEVICE_ID", "your-device-id")
HUAWEI_USERNAME = os.getenv("HUAWEI_IOT_USERNAME", HUAWEI_DEVICE_ID)
HUAWEI_PASSWORD = os.getenv("HUAWEI_IOT_PASSWORD", "your-password")
HUAWEI_CLIENT_ID = os.getenv("HUAWEI_IOT_CLIENT_ID", "your-client-id")
HUAWEI_HOST = os.getenv("HUAWEI_IOT_HOST", "your-iotda-host")
HUAWEI_SERVICE_ID = os.getenv("HUAWEI_IOT_SERVICE_ID", "your-service-id")
HUAWEI_PROPERTY_ID = os.getenv("HUAWEI_IOT_PROPERTY_ID", "AIWarning")


def _patch_torch_load_for_old_yolo_weights():
    original_load = torch.load

    def patched_load(*args, **kwargs):
        kwargs.setdefault("weights_only", False)
        return original_load(*args, **kwargs)

    torch.load = patched_load


os.environ.setdefault("YOLO_CONFIG_DIR", str(BASE_DIR / ".ultralytics"))
_patch_torch_load_for_old_yolo_weights()
from ultralytics import YOLO  # noqa: E402

app = Flask(__name__)
UPLOAD_DIR.mkdir(exist_ok=True)
OUTPUT_DIR.mkdir(exist_ok=True)
model = YOLO(str(MODEL_PATH))

HTML = """
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <title>羊肚菌病虫害 YOLO 视频识别</title>
  <style>
    body { font-family: Arial, "Microsoft YaHei", sans-serif; margin: 32px; background: #f6f7f9; color: #20242a; }
    main { max-width: 980px; margin: 0 auto; }
    h1 { font-size: 26px; margin-bottom: 18px; }
    .panel { background: #fff; border: 1px solid #dde1e6; border-radius: 8px; padding: 20px; margin-bottom: 18px; }
    input[type=file] { margin-right: 12px; }
    button { padding: 8px 14px; border: 0; border-radius: 6px; background: #1677ff; color: white; cursor: pointer; }
    img.preview { width: 100%; max-height: 720px; background: #111; border-radius: 8px; object-fit: contain; }
    .status { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
    .item { padding: 12px; border-radius: 6px; background: #f4f7fb; }
    .label { color: #5f6b7a; font-size: 13px; margin-bottom: 6px; }
    .value { font-size: 20px; font-weight: 700; }
    .ok { color: #087443; }
    .bad { color: #b42318; }
    .hint { color: #5f6b7a; line-height: 1.7; }
  </style>
</head>
<body>
<main>
  <h1>羊肚菌病虫害 YOLO 视频识别</h1>
  <section class="panel">
    <form method="post" action="/analyze" enctype="multipart/form-data">
      <input type="file" name="video" accept="video/*" required>
      <button type="submit">开始识别并上传</button>
    </form>
    <p class="hint">识别完成后只上报一次华为云 AIWarning。未识别到病虫害时，最终状态按 healthy 上报。</p>
  </section>
  {% if result %}
  <section class="panel status">
    <div class="item"><div class="label">最终 AIWarning</div><div class="value">{{ result.final_label }}</div></div>
    <div class="item"><div class="label">最高置信度</div><div class="value">{{ "%.2f"|format(result.confidence * 100) }}%</div></div>
    <div class="item"><div class="label">抽样识别帧数</div><div class="value">{{ result.sampled_frames }}</div></div>
    <div class="item"><div class="label">华为云上传</div><div class="value {{ 'ok' if result.cloud_reported else 'bad' }}">{{ "已上传" if result.cloud_reported else "上传失败" }}</div></div>
  </section>
  <section class="panel">
    {% if result.preview_url %}
    <p class="hint">识别截图预览：这是最高置信度那一帧，可以直接看到 YOLO 框。</p>
    <img class="preview" src="{{ result.preview_url }}" alt="YOLO识别截图">
    {% else %}
    <p class="hint">没有检测到目标框，最终按 healthy 上报。</p>
    {% endif %}
  </section>
  {% endif %}
</main>
</body>
</html>
"""


def _reason_code_value(reason_code):
    return getattr(reason_code, "value", reason_code)


def _reason_ok(reason_code):
    value = _reason_code_value(reason_code)
    return value == 0 or str(reason_code).lower() == "success"


def report_aiwarning_once(label_text):
    try:
        from paho.mqtt import client as mqtt_client
        from paho.mqtt.client import CallbackAPIVersion
    except Exception as exc:
        print(f"[Huawei MQTT] paho-mqtt unavailable: {exc}")
        return False

    topic = f"$oc/devices/{HUAWEI_DEVICE_ID}/sys/properties/report"
    payload = {"services": [{"service_id": HUAWEI_SERVICE_ID, "properties": {HUAWEI_PROPERTY_ID: str(label_text)}}]}
    payload_text = json.dumps(payload, ensure_ascii=False)
    connected = {"ok": False, "code": None}

    def on_connect(client, userdata, flags, reason_code, properties=None):
        connected["code"] = _reason_code_value(reason_code)
        connected["ok"] = _reason_ok(reason_code)
        print(f"[Huawei MQTT] connect code={reason_code}, client_id={HUAWEI_CLIENT_ID}")

    client = None
    try:
        client = mqtt_client.Client(CallbackAPIVersion.VERSION2, HUAWEI_CLIENT_ID)
        client.username_pw_set(HUAWEI_USERNAME, HUAWEI_PASSWORD)
        client.tls_set(cert_reqs=ssl.CERT_REQUIRED)
        client.on_connect = on_connect
        client.connect(HUAWEI_HOST, HUAWEI_PORT, keepalive=60)
        client.loop_start()
        time.sleep(2)
        if not connected["ok"]:
            print(f"[Huawei MQTT] connect failed, code={connected['code']}")
            return False

        result = client.publish(topic, payload_text, qos=1)
        result.wait_for_publish(timeout=5)
        ok = result.rc == mqtt_client.MQTT_ERR_SUCCESS and result.is_published()
        print(f"[Huawei MQTT] reported={ok}, topic={topic}, payload={payload_text}")
        return ok
    except Exception as exc:
        print(f"[Huawei MQTT] error: {exc}")
        return False
    finally:
        if client is not None:
            try:
                client.loop_stop()
                client.disconnect()
            except Exception:
                pass


def choose_final_label(best_label):
    return best_label or "healthy"


def analyze_video(input_path, output_path):
    cap = cv2.VideoCapture(str(input_path))
    if not cap.isOpened():
        raise RuntimeError("视频无法打开，请检查文件格式")

    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
    frame_index = 0
    sampled_frames = 0
    best_label = None
    best_conf = 0.0
    best_preview_frame = None

    while True:
        ok, frame = cap.read()
        if not ok:
            break
        should_detect = frame_index % SAMPLE_EVERY_N_FRAMES == 0 and sampled_frames < MAX_SAMPLED_FRAMES
        if should_detect:
            sampled_frames += 1
            results = model.predict(source=frame, conf=CONF_THRESHOLD, verbose=False)[0]
            if len(results.boxes) > 0:
                plotted = results.plot()
                for box in results.boxes:
                    class_id = int(box.cls)
                    conf = float(box.conf)
                    if class_id < len(DISEASE_NAMES) and conf > best_conf:
                        best_label = DISEASE_NAMES[class_id]
                        best_conf = conf
                        best_preview_frame = plotted.copy()
        frame_index += 1

    cap.release()
    preview_url = None
    if best_preview_frame is not None:
        preview_path = output_path.with_suffix(".jpg")
        cv2.imwrite(str(preview_path), best_preview_frame)
        preview_url = url_for("output_file", filename=preview_path.name)
    return {"total_frames": total_frames, "sampled_frames": sampled_frames, "final_label": choose_final_label(best_label), "confidence": best_conf, "preview_url": preview_url}


@app.get("/")
def index():
    return render_template_string(HTML, result=None)


@app.post("/analyze")
def analyze():
    file = request.files.get("video")
    if not file or not file.filename:
        return redirect(url_for("index"))
    suffix = Path(file.filename).suffix.lower() or ".mp4"
    job_id = uuid.uuid4().hex
    input_path = UPLOAD_DIR / f"{job_id}{suffix}"
    output_path = OUTPUT_DIR / f"{job_id}_annotated.jpg"
    file.save(input_path)
    result = analyze_video(input_path, output_path)
    result["cloud_reported"] = report_aiwarning_once(result["final_label"])
    return render_template_string(HTML, result=result)


@app.get("/outputs/<path:filename>")
def output_file(filename):
    return send_from_directory(OUTPUT_DIR, filename)


@app.get("/health")
def health():
    return Response("ok", mimetype="text/plain")


if __name__ == "__main__":
    print("Open http://127.0.0.1:5050 in your browser")
    app.run(host="127.0.0.1", port=5050, debug=False)
