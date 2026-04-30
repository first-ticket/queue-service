package com.firstticket.queueservice.presentation.dto;

import com.firstticket.queueservice.application.dto.QueueTokenResult;

import java.time.LocalDateTime;

public record QueueTokenResponse(
    String tokenId,
    String status,
    LocalDateTime issuedAt,
    Long position
) {
    public static QueueTokenResponse from(QueueTokenResult result) {
        return new QueueTokenResponse(
            result.tokenId().asString(),
            result.status().name(),
            result.issuedAt().value(),
            result.position()
        );
    }
}
