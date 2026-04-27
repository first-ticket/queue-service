package com.firstticket.queueservice.domain.vo;

import java.util.Objects;
import java.util.UUID;

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
}
