package com.firstticket.queueservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 입장 토큰 설정.
 *
 * <p>도메인 정책 (TTL) 과 인프라 정책 (비밀키) 모두 포함.
 */
@ConfigurationProperties(prefix = "queue.jwt")
public record JwtProperties(
    String secret,
    Duration entryTokenTtl
) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("queue.jwt.secret must not be blank");
        }
        if (entryTokenTtl == null || entryTokenTtl.isZero() || entryTokenTtl.isNegative()) {
            throw new IllegalArgumentException("queue.jwt.entry-token-ttl must be positive");
        }
    }
}
