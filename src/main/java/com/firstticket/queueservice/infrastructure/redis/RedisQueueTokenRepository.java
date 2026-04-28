package com.firstticket.queueservice.infrastructure.redis;

import com.firstticket.queueservice.config.QueueProperties;
import com.firstticket.queueservice.domain.QueueToken;
import com.firstticket.queueservice.domain.QueueTokenRepository;
import com.firstticket.queueservice.domain.TokenStatus;
import com.firstticket.queueservice.domain.exception.DuplicateTokenException;
import com.firstticket.queueservice.domain.vo.IssuedAt;
import com.firstticket.queueservice.domain.vo.ProgramId;
import com.firstticket.queueservice.domain.vo.QueueTokenId;
import com.firstticket.queueservice.domain.vo.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 대기 토큰의 Redis 기반 영속성 구현체.
 *
 * <p>3가지 자료구조를 조합하여 사용한다:
 * <ul>
 *   <li>Sorted Set ({@code queue:program:{programId}}) — 대기 순번 관리. score = 진입 시각(epoch milli)</li>
 *   <li>Hash ({@code queue:token:{tokenId}}) — 토큰 메타 데이터 저장</li>
 *   <li>String ({@code queue:user:{userId}:program:{programId}}) — 역인덱스 (도메인 키 → 토큰 ID)</li>
 * </ul>
 *
 * <p>저장 시 역인덱스(setIfAbsent)와 트랜잭션(MULTI/EXEC)으로 일관성을 보장한다.
 * 자세한 흐름은 {@link #enqueue(QueueToken)} 참고.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisQueueTokenRepository implements QueueTokenRepository {

    // ===== 키 prefix 상수 =====
    private static final String QUEUE_KEY_PREFIX = "queue:";
    private static final String PROGRAM_KEY_PREFIX = "program:";
    private static final String TOKEN_KEY_PREFIX = "token:";
    private static final String USER_KEY_PREFIX = "user:";

    // ===== Hash 필드 이름 상수 =====
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_PROGRAM_ID = "programId";
    private static final String FIELD_ISSUED_AT = "issuedAt";
    private static final String FIELD_STATUS = "status";

    private final StringRedisTemplate redisTemplate;
    private final QueueProperties properties;

    /**
     * Redis 기반 enqueue 구현.
     *
     * <p>2단계 처리:
     * <ol>
     *   <li>역인덱스(setIfAbsent)를 락처럼 사용하여 중복 진입 방지</li>
     *   <li>Sorted Set + Hash 를 MULTI/EXEC 트랜잭션으로 저장</li>
     * </ol>
     *
     * <p>한계: 1단계 성공 후 2단계 실패 시 orphan 역인덱스가 남을 수 있다.
     * TTL 로 자동 정리되지만, 진짜 원자성을 위해 향후 Lua Script 도입 예정.
     *
     * <p>Sorted Set 의 멤버는 키 단위 TTL 불가하므로,
     * {@link #delete(QueueToken)} 호출 시 명시적 ZREM 으로 정리한다.
     */
    @Override
    public void enqueue(QueueToken token) {

        // 키 생성
        String programKey = programKey(token.getProgramId());
        String tokenKey = tokenKey(token.getId());
        String userProgramKey = userProgramKey(token.getUserId(), token.getProgramId());

        String tokenIdStr = token.getId().asString();

        // Sorted Set의 score로 사용할 진입 시각 (epoch milli)
        long issuedAtEpochMilli = token.getIssuedAt().toEpochMilli();

        Duration ttl = properties.waitingTtl();

        // 1단계: 역인덱스를 락처럼 사용하여 중복 진입 방지
        Boolean acquired = redisTemplate.opsForValue()
            .setIfAbsent(userProgramKey, tokenIdStr, ttl);

        if (!Boolean.TRUE.equals(acquired)) {
            throw new DuplicateTokenException();
        }

        // 2단계: 나머지 키를 트랜잭션으로 저장
        Map<String, String> tokenFields = Map.of(
            FIELD_USER_ID, token.getUserId().asString(),
            FIELD_PROGRAM_ID, token.getProgramId().asString(),
            FIELD_ISSUED_AT, String.valueOf(issuedAtEpochMilli),
            FIELD_STATUS, token.getStatus().name()
        );

        // 트랜잭션으로 Sorted Set + Hash 저장 + Hash TTL 설정
        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @SuppressWarnings({"unchecked"})
            @Override
            public List<Object> execute(RedisOperations operations) {
                operations.multi();

                // 1. Sorted Set: 대기열에 추가 (score = 진입 시각)
                operations.opsForZSet().add(programKey, tokenIdStr, issuedAtEpochMilli);

                // 2. Hash: 토큰 메타 데이터 저장
                operations.opsForHash().putAll(tokenKey, tokenFields);

                // 3. Hash 키에 TTL 설정 (Sorted Set은 컬렉션이라 별도 정리 필요)
                operations.expire(tokenKey, ttl);

                return operations.exec();
            }
        });
    }

    /**
     * Redis 기반 findById 구현.
     *
     * <p>Hash 전체 조회 후 도메인 객체로 복원한다.
     * 토큰이 없으면 빈 Map이 반환되며 (null 아님), Optional.empty()로 처리한다.
     */
    @Override
    public Optional<QueueToken> findById(QueueTokenId id) {
        String tokenKey = tokenKey(id);

        // 타입 명시한 Map 으로 받기 위해 hashOps 변수 사용
        HashOperations<String, String, String> hashOps = redisTemplate.opsForHash();
        // Redis 에서 Hash 전체 조회
        Map<String, String> entries = hashOps.entries(tokenKey);

        // 토큰 없으면 빈 Map 이 옴 (null 아님)
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        // 깨진 레코드 자동 정리 향후 도입
        try {
            // Hash 데이터로 QueueToken 객체 만들어서 반환
            return Optional.of(toQueueToken(id, entries));
        } catch (Exception e) {
            log.warn("깨진 Hash 레코드 발견. tokenId={}", id.asString(), e);
            return Optional.empty();
        }
    }

    /**
     * Redis Hash 데이터로부터 QueueToken 도메인 객체를 복원한다.
     */
    private QueueToken toQueueToken(QueueTokenId id, Map<String, String> entries) {
        UserId userId = UserId.fromString(entries.get(FIELD_USER_ID));
        ProgramId programId = ProgramId.fromString(entries.get(FIELD_PROGRAM_ID));
        IssuedAt issuedAt = IssuedAt.fromEpochMilli(Long.parseLong(entries.get(FIELD_ISSUED_AT)));
        TokenStatus status = TokenStatus.valueOf(entries.get(FIELD_STATUS));

        return QueueToken.restore(id, userId, programId, issuedAt, status);
    }

    /**
     * Redis 기반 findByUserIdAndProgramId 구현.
     *
     * <p>2단계 조회:
     * <ol>
     *   <li>역인덱스로 tokenId 조회</li>
     *   <li>tokenId로 토큰 전체 조회 ({@link #findById} 재사용)</li>
     * </ol>
     */
    @Override
    public Optional<QueueToken> findByUserIdAndProgramId(UserId userId, ProgramId programId) {
        String userProgramKey = userProgramKey(userId, programId);

        // 1단계: 역인덱스로 tokenId 조회
        String tokenIdStr = redisTemplate.opsForValue().get(userProgramKey);

        if (tokenIdStr == null) {
            return Optional.empty();
        }

        // 2단계: tokenId로 토큰 전체 조회 (findById 재사용)
        return findById(QueueTokenId.fromString(tokenIdStr));
    }

    /**
     * Redis 기반 delete 구현.
     *
     * <p>3개 키를 트랜잭션으로 묶어 삭제한다:
     * <ol>
     *   <li>Sorted Set 에서 토큰 멤버 제거 (ZREM)</li>
     *   <li>Hash 키 삭제</li>
     *   <li>역인덱스 키 삭제</li>
     * </ol>
     *
     * <p>이미 만료/삭제된 토큰에 대해서도 안전하게 호출 가능 (멱등).
     */
    @Override
    public void delete(QueueToken token) {
        String programKey = programKey(token.getProgramId());
        String tokenKey = tokenKey(token.getId());
        String userProgramKey = userProgramKey(token.getUserId(), token.getProgramId());
        String tokenIdStr = token.getId().asString();

        // 역인덱스 값과 토큰 ID가 일치할 때만 삭제
        // (다른 토큰이 차지한 경우는 그대로 둠 — race condition 방어)
        String current = redisTemplate.opsForValue().get(userProgramKey);
        if (tokenIdStr.equals(current)) {
            redisTemplate.delete(userProgramKey);
        }

        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @SuppressWarnings({"unchecked"})
            @Override
            public List<Object> execute(RedisOperations operations) {
                operations.multi();

                // 1. Sorted Set 에서 멤버 제거
                operations.opsForZSet().remove(programKey, tokenIdStr);

                // 2. Hash 키 삭제
                operations.delete(tokenKey);

                return operations.exec();
            }
        });
    }

    /**
     * Redis 기반 findRank 구현.
     *
     * <p>2단계 조회:
     * <ol>
     *   <li>역인덱스로 tokenId 조회</li>
     *   <li>Sorted Set의 ZRANK로 순번 조회</li>
     * </ol>
     *
     * <p>Redis ZRANK는 0-based이므로 사용자에게 보여줄 1-based로 변환한다.
     */
    @Override
    public Optional<Long> findRank(UserId userId, ProgramId programId) {
        String userProgramKey = userProgramKey(userId, programId);

        // 1단계: 역인덱스로 tokenId 조회
        String tokenIdStr = redisTemplate.opsForValue().get(userProgramKey);

        if (tokenIdStr == null) {
            return Optional.empty();
        }

        // 2단계: Sorted Set에서 순번 조회 (0-based)
        String programKey = programKey(programId);
        Long rank = redisTemplate.opsForZSet().rank(programKey, tokenIdStr);

        if (rank == null) {
            return Optional.empty();
        }

        // 3단계: 1-based로 변환하여 반환
        return Optional.of(rank + 1);
    }

    /**
     * Redis 기반 findAdmissionCandidates 구현.
     *
     * <p>2단계 조회:
     * <ol>
     *   <li>Sorted Set ZRANGE로 앞에서 batchSize개의 tokenId 추출</li>
     *   <li>각 tokenId로 토큰 전체 조회 (N번 Hash 조회)</li>
     * </ol>
     *
     * <p>Hash가 만료되어 토큰을 복원할 수 없는 orphan 케이스는 자연 필터링된다.
     * 향후 N번 조회를 단일 Lua Script로 통합 검토.
     */
    @Override
    public List<QueueToken> findAdmissionCandidates(ProgramId programId, int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize는 1 이상이어야 합니다: " + batchSize);
        }

        String programKey = programKey(programId);

        // 1단계: Sorted Set에서 앞에서부터 batchSize명의 tokenId 조회
        Set<String> tokenIds = redisTemplate.opsForZSet().range(programKey, 0, batchSize - 1);

        if (tokenIds == null || tokenIds.isEmpty()) {
            return List.of();
        }

        // 2단계: 각 tokenId로 토큰 전체 조회
        return tokenIds.stream()
            // String → Optional<QueueToken>
            .map(tokenIdStr -> findById(QueueTokenId.fromString(tokenIdStr)))
            // 토큰 있는 것만
            .filter(Optional::isPresent)
            // Optional<QueueToken> → QueueToken
            .map(Optional::get)
            .toList();
    }

    // ===== 키 생성 헬퍼 =====

    private String programKey(ProgramId programId) {
        return QUEUE_KEY_PREFIX + PROGRAM_KEY_PREFIX + programId.asString();
    }

    private String tokenKey(QueueTokenId tokenId) {
        return QUEUE_KEY_PREFIX + TOKEN_KEY_PREFIX + tokenId.asString();
    }

    private String userProgramKey(UserId userId, ProgramId programId) {
        return QUEUE_KEY_PREFIX + USER_KEY_PREFIX + userId.asString()
            + ":" + PROGRAM_KEY_PREFIX + programId.asString();
    }
}
