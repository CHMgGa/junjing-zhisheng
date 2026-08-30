package com.morel.greenhouse.application.service;

import com.morel.greenhouse.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class VerificationDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(VerificationDeliveryService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String emailFrom;
    private final int maxRetry;
    private final long retryBackoffMs;

    public VerificationDeliveryService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${greenhouse.verification.email-from:}") String emailFrom,
            @Value("${greenhouse.verification.mail-max-retry:2}") int maxRetry,
            @Value("${greenhouse.verification.mail-retry-backoff-ms:800}") long retryBackoffMs
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.emailFrom = emailFrom;
        this.maxRetry = Math.max(maxRetry, 0);
        this.retryBackoffMs = Math.max(retryBackoffMs, 0);
    }

    public DeliveryResult deliverEmail(String receiver, String scene, String code, int ttlMinutes) {
        if (receiver == null || !receiver.contains("@")) {
            throw new BusinessException(400, "验证码接收方必须是邮箱地址");
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || emailFrom == null || emailFrom.isBlank()) {
            log.warn("Email verification is not configured, fallback to development code. receiver={}, scene={}", receiver, scene);
            return DeliveryResult.dev("邮箱服务未配置，已生成开发验证码");
        }

        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetry; attempt++) {
            try {
                mailSender.send(buildMessage(receiver, scene, code, ttlMinutes));
                log.info("Email verification code sent: receiver={}, scene={}, attempt={}", receiver, scene, attempt + 1);
                return DeliveryResult.sent("验证码已发送至邮箱", attempt);
            } catch (Exception exception) {
                lastException = exception;
                log.warn("Email verification send attempt failed: receiver={}, scene={}, attempt={}/{}",
                        receiver, scene, attempt + 1, maxRetry + 1, exception);
                backoff(attempt);
            }
        }
        String error = lastException == null ? "unknown error" : lastException.getMessage();
        return DeliveryResult.failed("邮件验证码发送失败，请检查 SMTP 配置", maxRetry, error);
    }

    private SimpleMailMessage buildMessage(String receiver, String scene, String code, int ttlMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailFrom);
        message.setTo(receiver);
        message.setSubject("智慧羊肚菌大棚系统验证码");
        message.setText("""
                您正在进行%s操作。

                验证码：%s
                有效期：%d 分钟

                若非本人操作，请忽略本邮件。为保障账号安全，请不要向他人泄露验证码。
                """.formatted(sceneText(scene), code, ttlMinutes));
        return message;
    }

    private String sceneText(String scene) {
        return switch (scene == null ? "" : scene) {
            case "REGISTER" -> "账号注册";
            case "RESET_PASSWORD" -> "密码重置";
            case "EMAIL_LOGIN" -> "邮箱登录";
            default -> "身份验证";
        };
    }

    private void backoff(int attempt) {
        if (attempt >= maxRetry || retryBackoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMs * (attempt + 1));
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    public record DeliveryResult(
            String message,
            boolean devMode,
            boolean success,
            int retryCount,
            String errorMessage
    ) {
        static DeliveryResult sent(String message, int retryCount) {
            return new DeliveryResult(message, false, true, retryCount, "");
        }

        static DeliveryResult dev(String message) {
            return new DeliveryResult(message, true, true, 0, "");
        }

        static DeliveryResult failed(String message, int retryCount, String errorMessage) {
            return new DeliveryResult(message, false, false, retryCount, errorMessage == null ? "" : errorMessage);
        }
    }
}
