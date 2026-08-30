package com.morel.greenhouse.infrastructure.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.morel.greenhouse.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class AiServiceClient {
    private static final Logger log = LoggerFactory.getLogger(AiServiceClient.class);
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final String serviceUrl;
    private final ObjectMapper objectMapper;

    public AiServiceClient(@Value("${greenhouse.ai.service-url}") String serviceUrl, ObjectMapper objectMapper) {
        this.serviceUrl = serviceUrl.endsWith("/") ? serviceUrl.substring(0, serviceUrl.length() - 1) : serviceUrl;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> chat(Map<String, Object> payload) {
        return post("/api/chat", payload);
    }

    public Map<String, Object> visionDiagnosis(Map<String, Object> payload) {
        return post("/api/vision-diagnosis", payload);
    }

    public Map<String, Object> rebuildIndex() {
        return post("/api/index/rebuild", Map.of());
    }

    private Map<String, Object> post(String path, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            log.debug("Sending local AI request, path={}, bodyLength={}", path, json.length());

            URL url = URI.create(serviceUrl + path).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(300_000);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }

            int status = connection.getResponseCode();
            String responseBody = readResponseBody(connection, status);
            if (status < 200 || status >= 300) {
                log.warn("Local AI service returned error, path={}, status={}, body={}", path, status, responseBody);
                throw new BusinessException(502, "本地 AI 服务返回异常，请查看 ai-service 日志");
            }
            return objectMapper.readValue(responseBody, MAP_TYPE);
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.warn("Local AI service JSON or IO error, path={}", path, ex);
            throw new BusinessException(502, "本地 AI 服务数据解析失败，请确认 AI 服务响应格式");
        } catch (RuntimeException ex) {
            log.warn("Local AI service request failed, path={}", path, ex);
            throw new BusinessException(502, "本地 AI 服务暂不可用，请确认 ai-service 和 Ollama 已启动");
        }
    }

    private String readResponseBody(HttpURLConnection connection, int status) throws IOException {
        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (stream == null) {
            return "";
        }
        try (InputStream input = stream) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
