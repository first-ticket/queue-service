package com.firstticket.queueservice.infrastructure.jwt;

import com.firstticket.queueservice.config.JwtProperties;
import com.firstticket.queueservice.domain.QueueToken;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * 입장 토큰 (JWT) 발급기.
 *
 * <p>대기열에서 admit 된 사용자에게 발급되는 JWT.
 * 예매 서비스가 stateless 하게 검증한다 (queue-service 별도 호출 X).
 */
@Component
public class EntryTokenIssuer {

    private static final String ISSUER = "queue-service";
    private static final String CLAIM_PROGRAM_ID = "programId";

    private final SecretKey secretKey;
    private final Duration ttl;

    public EntryTokenIssuer(JwtProperties properties) {
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.ttl = properties.entryTokenTtl();
    }

    /**
     * 입장 토큰을 발급한다.
     *
     * @param token admit 된 대기 토큰
     * @return JWT 형식의 입장 토큰
     */
    public String issue(QueueToken token) {
        Instant now = Instant.now();
        Instant expiration = now.plus(ttl);

        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .issuer(ISSUER)
            .subject(token.getUserId().asString())
            .claim(CLAIM_PROGRAM_ID, token.getProgramId().asString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(secretKey)
            .compact();
    }
}
