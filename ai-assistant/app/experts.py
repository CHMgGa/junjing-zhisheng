from app.knowledge_base import knowledge_base
from app.ollama_client import ollama_client
from app.schemas import AiResult, ChatRequest, EnvironmentSnapshot, ReferenceChunk, VisionDiagnosisRequest
from app.settings import settings


def environment_text(environment: EnvironmentSnapshot | None) -> str:
    if not environment:
        return "未提供大棚实时环境数据。"

    air_temperature = environment.air_temperature if environment.air_temperature is not None else environment.temperature
    air_humidity = environment.air_humidity if environment.air_humidity is not None else environment.humidity
    soil_humidity = environment.soil_humidity if environment.soil_humidity is not None else environment.soil_moisture

    return (
        f"大棚：{environment.greenhouse_name or environment.greenhouse_id or '-'}；"
        f"空气温度：{air_temperature if air_temperature is not None else '-'}℃；"
        f"空气湿度：{air_humidity if air_humidity is not None else '-'}%；"
        f"土壤温度：{environment.soil_temperature if environment.soil_temperature is not None else '-'}℃；"
        f"土壤湿度：{soil_humidity if soil_humidity is not None else '-'}%；"
        f"pH值：{environment.ph_value if environment.ph_value is not None else '-'}；"
        f"CO2：{environment.co2_ppm if environment.co2_ppm is not None else '-'}ppm；"
        f"光照强度：{environment.light_lux if environment.light_lux is not None else '-'}lux。"
    )


def reference_text(references: list[ReferenceChunk]) -> str:
    if not references:
        return "无。"
    lines = []
    for index, ref in enumerate(references, start=1):
        lines.append(f"[{index}] {ref.source} 第{ref.page or '-'}页：{ref.content}")
    return "\n".join(lines)


def infer_risk_level(text: str) -> str:
    severe_words = ["严重", "高风险", "立即", "大量", "腐烂", "虫害", "病害", "白霉", "蛛网", "螨"]
    medium_words = ["偏高", "偏低", "建议", "注意", "风险", "异常", "复核", "检查"]
    if any(word in text for word in severe_words):
        return "HIGH"
    if any(word in text for word in medium_words):
        return "MEDIUM"
    return "LOW"


def extract_actions(text: str) -> list[str]:
    actions: list[str] = []
    keywords = ["建议", "应", "需要", "控制", "清理", "通风", "补湿", "降湿", "隔离", "复核", "拍照"]
    for line in text.splitlines():
        stripped = line.strip(" -0123456789.、")
        if stripped and any(keyword in stripped for keyword in keywords):
            actions.append(stripped)
    return actions[:6]


async def chat(request: ChatRequest) -> AiResult:
    references = knowledge_base.search(request.question, top_k=5)
    mode_instruction = (
        "知识库已命中相关片段。请优先依据知识库内容回答，可以简要说明参考了知识库信息。"
        if references
        else "知识库未命中相关片段。请直接基于大模型自身的通用农技知识回答，不要拒答，也不要向用户说明“知识库未命中”或“非知识库引用”。"
    )
    prompt = f"""
你是羊肚菌智慧大棚专业 AI 助手。请根据用户问题、实时环境数据和可用知识库内容回答。

当前回答模式：{mode_instruction}

要求：
1. 使用中文回答，直接、专业、可执行。
2. 知识库没有命中时，也要基于羊肚菌种植常识给出阶段划分、判断逻辑和操作建议。
3. 不确定的结论要提示需要人工复核，但不能只说“咨询专家”就结束。
4. 不要伪造“某文件/某标准第几条”等不存在的知识库引用。
5. 涉及环境调控时，结合空气温湿度、土壤温湿度、pH、CO2、光照强度七项数据说明。
6. 没有知识库依据时，不要在回答开头或结尾强调“未检索到知识库依据”。

用户问题：{request.question}

实时环境：
{environment_text(request.environment)}

知识库依据：
{reference_text(references)}
"""
    answer = await ollama_client.generate(settings.text_model, prompt, max_tokens=settings.text_max_tokens)
    return AiResult(
        answer=answer,
        risk_level=infer_risk_level(answer),
        actions=extract_actions(answer),
        references=references,
        expert_trace=["知识库检索专家", "环境数据分析专家", "Ollama文本问答专家", "建议生成专家"],
        raw={"answer_mode": "rag" if references else "ollama_fallback", "model": settings.text_model},
    )


async def vision_diagnosis(request: VisionDiagnosisRequest) -> AiResult:
    has_environment = request.environment is not None
    vision_prompt = f"""
请作为羊肚菌图像识别专家，只基于图片可见信息回答用户问题。

用户问题：{request.question or "这个羊肚菌正常吗？"}

要求：
1. 先直接回答“看起来正常 / 不太正常 / 仅凭图片无法确定”，不要绕到大棚环境。
2. 只描述图片中真实可见的证据，例如菌盖、菌柄、颜色、形态、表面霉斑、腐烂、水渍、虫咬等；看不清或图片没有显示的内容要说“图片中未见明显迹象”，不要猜测。
3. 如果没有明显异常，不要为了给建议而强行判定中高风险。
4. 输出要简短，控制在 5 条以内。
5. 输出格式：
- 判断：
- 图片依据：
- 风险等级：
- 建议：
- 需要复核：
"""
    vision_answer = await ollama_client.generate(settings.vision_model, vision_prompt, [request.image_base64], max_tokens=settings.vision_max_tokens)
    query = f"{request.question or ''}\n{vision_answer}"
    references = knowledge_base.search(query, top_k=5)
    if not references and not has_environment:
        return AiResult(
            answer=vision_answer,
            diagnosis=vision_answer,
            risk_level=infer_risk_level(vision_answer),
            actions=extract_actions(vision_answer),
            references=[],
            expert_trace=["图像识别专家"],
            raw={"answer_mode": "vision_only", "model": settings.vision_model},
        )
    mode_instruction = (
        "知识库已命中相关片段，请结合图像识别结果和知识库依据输出，可说明参考了知识库信息。"
        if references
        else "知识库未命中相关片段，请基于图像模型结果和通用农技知识输出，不要向用户说明“知识库未命中”或“非知识库引用”。"
    )
    final_prompt = f"""
你是羊肚菌多专家建议生成专家。请以图像识别结果为主，必要时再结合实时环境和知识库依据，输出最终诊断。

当前回答模式：{mode_instruction}

重要规则：
1. 用户问图片是否正常时，先回答图片中的羊肚菌是否看起来正常。
2. 不要把环境数据当作图片异常的主要证据；只有用户选择了大棚且环境明显异常时，才作为辅助提醒。
3. 图片没有明显腐烂、霉斑、虫害时，不要推断“潜在腐烂风险较高”。
4. 没有知识库依据时，不要写“知识库未命中”“非知识库原文引用”等说明。

图像识别结果：
{vision_answer}

实时环境（仅作辅助，不是图片判断的主要依据）：
{environment_text(request.environment) if has_environment else "未关联大棚环境数据。"}

知识库依据：
{reference_text(references)}

输出格式：
- 诊断结论：
- 风险等级：
- 主要依据：
- 建议操作：
- 需要人工复核：
"""
    answer = await ollama_client.generate(settings.text_model, final_prompt, max_tokens=settings.text_max_tokens)
    return AiResult(
        answer=answer,
        diagnosis=vision_answer,
        risk_level=infer_risk_level(answer + vision_answer),
        actions=extract_actions(answer),
        references=references,
        expert_trace=["图像识别专家", "知识库检索专家", "环境数据分析专家", "Ollama建议生成专家"],
        raw={"vision": vision_answer, "answer_mode": "rag" if references else "ollama_fallback", "model": settings.text_model},
    )
