# hardware — STM32 智能环境监测上云核心源码（2026 大赛）

基于 **STM32F103ZE（正点原子精英版）+ ESP8266 WiFi + 华为云 IoT** 的智能教室/大棚环境监测与控制系统核心源码，配合本仓库 `backend`、`web`、`digital-twin` 使用。

> 📌 本目录**只保留核心业务源码**（主程序 + 外设驱动），不含 Keil 工程骨架
> （启动文件、CMSIS 内核、ST 标准外设库、FATFS、字库、内存管理等模板文件）。
> 完整可编译的 Keil 工程保存在团队本地，如需要完整工程请联系团队维护者。

## 功能概览

- **传感器采集**：温度/湿度（DHT11）、光照（光敏）、土壤湿度、pH 值、碰撞/称重（HX711）
- **执行控制**：教室灯（LED）、风扇电机（PWM 多档调速）、挡光板舵机、水泵（土壤湿度低于 30% 自动灌溉）
- **云端联动（华为云 IoT，MQTT over TCP 1883）**：
  - 每 3 秒上报 JSON 属性（温度、湿度、光照、风档、灯、AI 病虫害、挡光板、pH、水泵、土壤湿度、生长周期）
  - 接收下行命令：`Fengdegree`（风扇档位）、`Light`（灯开关）、`Board`（挡光板）、`AIWarning`（AI 病虫害结果，触发舵机 + 语音播报）、`State`（生长周期 → 语音播报）
- **语音播报**：通过串口 2 发送 9 字节帧（`AA 55 0A/0B ... 55 AA`）驱动语音板
- **本地显示**：TFTLCD（FSMC 驱动）实时显示全部监测数据

## 目录结构

```
hardware/
├── USER/               # 主程序 main.c / main.h（业务逻辑、MQTT 对接、命令解析）
└── HARDWARE/           # 外设驱动（均为 Keil 工程实际编译使用的模块）
    ├── mqtt/           # MQTT 客户端（华为云对接核心）
    ├── wifi/           # ESP8266 WiFi 驱动
    ├── uart2/          # 语音播报板串口（9 字节控制帧）
    ├── uart3/          # ESP8266 数据通道串口
    ├── dht11/ lsens/ adc/ soilhumi/ ph/ bump/ weight(HX711)   # 传感器
    ├── Duoji(Servo)/ Motor/ PWM/ TIMER3/                      # 执行控制
    ├── LCD/ LED/ KEY/ BEEP/                                   # 人机交互
    └── SDIO/ SPI/ W25QXX/                                     # 存储
```

## 连接华为云前的配置

> ⚠️ **安全提醒**：本仓库为公开仓库，所有敏感凭据已脱敏为占位符。请自行替换后使用，**切勿**把真实密钥提交到仓库。

| 文件 | 占位符 | 替换为 |
|---|---|---|
| `USER/main.c` | `your_wifi_name` / `your_wifi_password` | 你的 WiFi 账号密码 |
| `USER/main.c` | `your_broker_address` | 华为云 IoT 接入地址（公网 IP） |
| `USER/main.c` | `your_device_id` / `your_device_username` / `your_device_secret` | 华为云 IoT 设备注册信息 |
| `HARDWARE/wifi/wifi.h` | `your_wifi_name` / `your_wifi_password` / `your_broker_address` | 同上（备用宏定义方式） |

设备侧需与华为云产品模型一致：产品服务 ID 为 `smartlan`，属性包括 `Temp`、`Humi`、`Lumi`、`Fengd`、`LightSt`、`AIWarning`、`DangGuangBan`、`pH`、`Bump`、`Soil_Humi`、`State`。

## 硬件接线（概要）

- USART1 (PA9/PA10)：调试串口 115200
- USART2：语音播报板
- USART3：ESP8266 WiFi 模块（MQTT 数据通道）
- LCD：FSMC（NE4 片选 / A10 复位）
- DHT11、光敏、土壤湿度、pH 模块：见 `HARDWARE/` 各驱动头文件
