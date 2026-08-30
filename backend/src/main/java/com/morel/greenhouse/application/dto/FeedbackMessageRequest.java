package com.morel.greenhouse.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FeedbackMessageRequest(
        Long conversationId,
        Long adminUserId,
        Long receiverUserId,
        @NotBlank String content,
        String messageType,
        String imageUrl
) {
}
