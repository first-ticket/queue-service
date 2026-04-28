package com.firstticket.queueservice.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * 사용자 ID를 표현하는 VO.
 */
public record UserId(UUID id) {

    public UserId {
        Objects.requireNonNull(id, "UserId는 null일 수 없습니다");
    }

    public static UserId of(UUID id) {
        return new UserId(id);
    }

    public static UserId fromString(String id) {
        return new UserId(UUID.fromString(id));
    }

    public String asString() {
        return id.toString();
    }
}
