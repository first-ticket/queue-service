package com.firstticket.queueservice.queuetoken.application.dto;

import com.firstticket.queueservice.queuetoken.domain.QueueToken;
import com.firstticket.queueservice.queuetoken.domain.TokenStatus;
import com.firstticket.queueservice.queuetoken.domain.vo.IssuedAt;
import com.firstticket.queueservice.queuetoken.domain.vo.QueueTokenId;

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
