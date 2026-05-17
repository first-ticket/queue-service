package com.firstticket.queueservice.queuetoken.domain.vo;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * 대기 토큰의 발급 시각을 표현하는 VO.
 *
 * <p>모든 시간은 UTC 기준으로 다룬다.
 */
public record IssuedAt(LocalDateTime value) {
    public IssuedAt {
        Objects.requireNonNull(value, "IssuedAt은 null일 수 없습니다");
    }

    /**
     * 현재 시각으로 IssuedAt을 생성한다 (UTC 기준).
     */
    public static IssuedAt now() {
        return new IssuedAt(LocalDateTime.now(ZoneOffset.UTC));
    }

    /**
     * epoch milli로부터 IssuedAt을 복원한다 (UTC 기준).
     */
    public static IssuedAt fromEpochMilli(long epochMilli) {
        return new IssuedAt(LocalDateTime.ofInstant(
                Instant.ofEpochMilli(epochMilli),
                ZoneOffset.UTC
        ));
    }

    /**
     * Redis Sorted Set score 에 사용할 epoch milli 변환.
     */
    public long toEpochMilli() {
        return value.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
