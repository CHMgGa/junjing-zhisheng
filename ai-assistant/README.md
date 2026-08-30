# 智慧小菇 AI 助手服务

本模块为“菌境智生：羊肚菌智慧种植调控系统”提供智能问答、环境数据解释、图片辅助诊断和种植建议生成能力。系统可结合羊肚菌种植知识库、实时环境数据和图像诊断结果，为农户和管理员提供更直观的风险判断与调控建议。

## 功能说明

- 支持羊肚菌种植知识问答，辅助用户了解种植管理、环境调控和异常处理方法。
- 支持结合大棚实时环境数据进行分析，包括空气温度、空气湿度、土壤湿度、pH 值、CO2 浓度、光照强度等指标。
- 支持图片辅助诊断，根据上传图片对羊肚菌长势、病害风险和异常特征进行分析。
- 支持基于知识库检索结果生成种植建议，提高回答内容与项目场景的匹配度。
- 支持为 Web 端或移动端提供独立 AI 接口，便于系统集成调用。

## 目录结构

```text
ai-assistant/
├─ app/                    AI 服务核心代码
├─ knowledge/              羊肚菌种植知识库材料
├─ scripts/                知识库导入脚本
├─ .env.example            环境变量示例配置
├─ requirements.txt        Python 依赖清单
└─ README.md               模块说明文档
```

## 文件说明

- `app/main.py`：AI 服务入口，提供健康检查、知识库重建、检索、问答和图片诊断接口。
- `app/experts.py`：组织问答提示词、环境数据解释、图片诊断和建议生成逻辑。
- `app/knowledge_base.py`：负责知识库读取、切分、索引构建和相似内容检索。
- `app/ollama_client.py`：封装本地大模型调用逻辑。
- `app/schemas.py`：定义问答请求、图片诊断请求、环境数据和返回结果结构。
- `knowledge/`：存放羊肚菌种植相关知识文本。
- `scripts/ingest_knowledge.py`：用于手动重建知识库索引。

## 模型准备

本模块默认使用 Ollama 管理本地模型。模型本体不提交到 GitHub，运行前请在本机安装 Ollama，并执行：

```powershell
ollama pull qwen3:4b-thinking
ollama pull minicpm-v:latest
```

默认模型：

```text
文本问答模型：qwen3:4b-thinking
视觉诊断模型：minicpm-v:latest
```

如需调整模型，可通过环境变量覆盖：

```powershell
$env:MOREL_AI_TEXT_MODEL="qwen3:4b-thinking"
$env:MOREL_AI_VISION_MODEL="minicpm-v:latest"
$env:MOREL_AI_OLLAMA_BASE_URL="http://localhost:11434"
```

## 运行环境

建议使用 Python 3.11 及以上版本。进入 `ai-assistant` 目录后执行：

```powershell
cd ai-assistant
python -m venv .venv
.\.venv\Scripts\pip install -r requirements.txt
.\.venv\Scripts\python -m app.main
```

服务默认启动地址：

```text
http://localhost:18080
```

健康检查地址：

```text
http://localhost:18080/health
```

## 知识库导入

系统默认读取 `ai-assistant/knowledge` 目录下的知识库文件。首次运行或更新知识库后，可执行：

```powershell
.\.venv\Scripts\python scripts\ingest_knowledge.py
```

也可以通过环境变量指定知识库目录：

```powershell
$env:MOREL_AI_KNOWLEDGE_DIR="D:\your\knowledge"
```

ChromaDB 索引默认保存在：

```text
ai-assistant/data/chroma
```

该目录为运行时生成内容，不建议上传到 GitHub。

## 接口说明

```text
GET  /health                 服务健康检查
POST /api/index/rebuild      重建知识库索引
POST /api/search             检索知识库相关内容
POST /api/chat               智慧小菇文本问答
POST /api/vision-diagnosis   图片辅助诊断
```

## 配置说明

可通过 `.env` 文件或系统环境变量进行配置。仓库中只保留 `.env.example` 示例文件，不上传真实 `.env` 文件。

常用配置项：

```text
MOREL_AI_OLLAMA_BASE_URL     Ollama 服务地址
MOREL_AI_TEXT_MODEL          文本问答模型名称
MOREL_AI_VISION_MODEL        视觉诊断模型名称
MOREL_AI_KNOWLEDGE_DIR       知识库目录
MOREL_AI_CHROMA_DIR          向量索引保存目录
MOREL_AI_UPLOAD_DIR          图片上传临时目录
```

## 上传注意事项

以下文件可以上传：

```text
app/
knowledge/
scripts/
requirements.txt
.env.example
README.md
```

以下文件不要上传：

```text
.env
.venv/
venv/
data/
__pycache__/
*.pyc
```

## 说明

本模块主要用于项目中的“智慧小菇”AI 助手功能，可为羊肚菌种植过程中的环境判断、异常解释、病害风险分析和调控建议提供辅助支持。实际使用时，AI 输出结果应作为辅助参考，关键种植决策仍需结合现场情况和人工复核。
