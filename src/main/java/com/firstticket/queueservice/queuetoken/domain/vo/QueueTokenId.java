package com.firstticket.queueservice.queuetoken.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * 대기 토큰 ID를 표현하는 VO.
 */
public record QueueTokenId(UUID id) {

    public QueueTokenId {
        Objects.requireNonNull(id, "QueueTokenId는 null일 수 없습니다");
    }

    public static QueueTokenId of() {
        return new QueueTokenId(UUID.randomUUID());
    }

    public static QueueTokenId fromString(String id) {
        return new QueueTokenId(UUID.fromString(id));
    }

    public String asString() {
        return id.toString();
    }
}
