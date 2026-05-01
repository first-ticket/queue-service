package com.firstticket.queueservice.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.firstticket.queueservice.application.dto.QueueTokenResult;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QueueTokenResponse(
    String tokenId,
    String status,
    LocalDateTime issuedAt,
    Long position,
    String entryToken
) {
    public static QueueTokenResponse from(QueueTokenResult result) {
        return new QueueTokenResponse(
            result.tokenId().asString(),
            result.status().name(),
            result.issuedAt().value(),
            result.position(),
            result.entryToken()
        );
    }
}
