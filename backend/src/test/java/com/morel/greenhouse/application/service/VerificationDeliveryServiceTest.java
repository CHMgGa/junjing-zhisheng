package com.morel.greenhouse.application.service;

import com.morel.greenhouse.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VerificationDeliveryServiceTest {

    @Test
    void deliverEmailRejectsInvalidReceiver() {
        VerificationDeliveryService service = new VerificationDeliveryService(emptyProvider(), "", 1, 0);

        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.deliverEmail(null, "REGISTER", "123456", 5)).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.deliverEmail("not-email", "REGISTER", "123456", 5)).getCode());
    }

    @Test
    void deliverEmailFallsBackToDevModeWhenMailIsNotConfigured() {
        VerificationDeliveryService service = new VerificationDeliveryService(emptyProvider(), "", 1, 0);

        VerificationDeliveryService.DeliveryResult result = service.deliverEmail("a@example.com", "REGISTER", "123456", 5);

        assertTrue(result.success());
        assertTrue(result.devMode());
        assertEquals(0, result.retryCount());
    }

    @Test
    void deliverEmailFallsBackWhenSenderAddressIsNullOrBlank() {
        JavaMailSender mailSender = mock(JavaMailSender.class);

        assertTrue(new VerificationDeliveryService(provider(mailSender), null, 1, 0)
                .deliverEmail("a@example.com", "REGISTER", "123456", 5).devMode());
        assertTrue(new VerificationDeliveryService(provider(mailSender), " ", 1, 0)
                .deliverEmail("a@example.com", "REGISTER", "123456", 5).devMode());
    }

    @Test
    void deliverEmailSendsMessageWhenMailSenderIsAvailable() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        VerificationDeliveryService service = new VerificationDeliveryService(provider(mailSender), "noreply@example.com", 1, 0);

        VerificationDeliveryService.DeliveryResult result = service.deliverEmail("a@example.com", "RESET_PASSWORD", "123456", 5);

        assertTrue(result.success());
        assertFalse(result.devMode());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void deliverEmailCoversRegisterAndDefaultSceneText() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        VerificationDeliveryService service = new VerificationDeliveryService(provider(mailSender), "noreply@example.com", 0, 0);

        assertTrue(service.deliverEmail("a@example.com", "REGISTER", "123456", 5).success());
        assertTrue(service.deliverEmail("a@example.com", null, "123456", 5).success());
        assertTrue(service.deliverEmail("a@example.com", "UNKNOWN", "123456", 5).success());
    }

    @Test
    void deliverEmailReturnsFailureAfterRetries() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        VerificationDeliveryService service = new VerificationDeliveryService(provider(mailSender), "noreply@example.com", 1, 0);

        VerificationDeliveryService.DeliveryResult result = service.deliverEmail("a@example.com", "EMAIL_LOGIN", "123456", 5);

        assertFalse(result.success());
        assertFalse(result.devMode());
        assertEquals(1, result.retryCount());
        assertEquals("smtp down", result.errorMessage());
    }

    @Test
    void deliveryResultFailedUsesEmptyStringForNullErrorMessage() {
        VerificationDeliveryService.DeliveryResult result = VerificationDeliveryService.DeliveryResult.failed("failed", 1, null);

        assertEquals("", result.errorMessage());
    }

    @Test
    void deliverEmailWaitsBeforeRetryWhenBackoffIsEnabled() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        VerificationDeliveryService service = new VerificationDeliveryService(provider(mailSender), "noreply@example.com", 1, 1);

        VerificationDeliveryService.DeliveryResult result = service.deliverEmail("a@example.com", "EMAIL_LOGIN", "123456", 5);

        assertFalse(result.success());
    }

    @Test
    void deliverEmailKeepsInterruptedFlagWhenBackoffIsInterrupted() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        VerificationDeliveryService service = new VerificationDeliveryService(provider(mailSender), "noreply@example.com", 1, 1);

        Thread.currentThread().interrupt();
        VerificationDeliveryService.DeliveryResult result = service.deliverEmail("a@example.com", "EMAIL_LOGIN", "123456", 5);

        assertFalse(result.success());
        assertTrue(Thread.interrupted());
    }

    private ObjectProvider<JavaMailSender> emptyProvider() {
        return provider(null);
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<JavaMailSender> provider(JavaMailSender mailSender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);
        return provider;
    }
}
