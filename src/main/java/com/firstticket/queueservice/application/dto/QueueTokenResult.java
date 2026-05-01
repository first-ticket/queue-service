package com.firstticket.queueservice.application.dto;

import com.firstticket.queueservice.domain.QueueToken;
import com.firstticket.queueservice.domain.TokenStatus;
import com.firstticket.queueservice.domain.vo.IssuedAt;
import com.firstticket.queueservice.domain.vo.QueueTokenId;

public record QueueTokenResult(
    QueueTokenId tokenId,
    TokenStatus status,
    IssuedAt issuedAt,
    Long position,
    String entryToken
) {
    public static QueueTokenResult of(QueueToken token, Long position) {
        return new QueueTokenResult(
            token.getId(),
            token.getStatus(),
            token.getIssuedAt(),
            position,
            token.getEntryToken()
        );
    }
}
