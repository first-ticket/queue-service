package com.firstticket.queueservice.domain.vo;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public record IssuedAt(LocalDateTime time) {
    public IssuedAt {
        Objects.requireNonNull(time, "IssuedAt은 null일 수 없습니다");
    }

    public static IssuedAt now() {
        return new IssuedAt(LocalDateTime.now(ZoneOffset.UTC));
    }

}
