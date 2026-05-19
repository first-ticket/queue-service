package com.firstticket.queueservice.queuetoken.infrastructure.redis;

import com.firstticket.queueservice.queuetoken.config.QueueProperties;
import com.firstticket.queueservice.queuetoken.domain.QueueToken;
import com.firstticket.queueservice.queuetoken.domain.QueueTokenRepository;
import com.firstticket.queueservice.queuetoken.domain.TokenStatus;
import com.firstticket.queueservice.queuetoken.domain.exception.DuplicateTokenException;
import com.firstticket.queueservice.queuetoken.domain.vo.IssuedAt;
import com.firstticket.queueservice.queuetoken.domain.vo.ProgramId;
import com.firstticket.queueservice.queuetoken.domain.vo.QueueTokenId;
import com.firstticket.queueservice.queuetoken.domain.vo.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 대기 토큰의 Redis 기반 영속성 구현체.
 *
 * <p>3가지 자료구조를 조합하여 사용한다:
 * <ul>
 *   <li>Sorted Set ({@code queue:program:{programId}}) — 대기 순번 관리.
 *       score = {@code epoch_milli * 1000000 + sequence} (tie-breaker)</li>
 *   <li>Hash ({@code queue:token:{tokenId}}) — 토큰 메타 데이터 저장</li>
 *   <li>String ({@code queue:user:{userId}:program:{programId}}) — 역인덱스 (도메인 키 → 토큰 ID)</li>
 *   <li>String ({@code queue:seq:{programId}}) — INCR 시퀀스 (score tie-breaker 용)</li>
 * </ul>
 *
 * <p>enqueue / delete 는 Lua 스크립트로 원자 처리한다.</p>
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
    private static final String SEQ_KEY_PREFIX = "queue:seq:";

    // ===== Hash 필드 이름 상수 =====
    private static final String FIELD_USER_ID = "userId";
    private static final String FIELD_PROGRAM_ID = "programId";
    private static final String FIELD_ISSUED_AT = "issuedAt";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ENTRY_TOKEN = "entryToken";

    private final StringRedisTemplate redisTemplate;
    private final QueueProperties properties;
    private final DefaultRedisScript<Long> enqueueScript;
    private final DefaultRedisScript<Long> deleteScript;
    private final DefaultRedisScript<Long> deleteAllByProgramScript;

    /**
     * Redis 기반 enqueue 구현 (Lua 스크립트로 원자 처리).
     *
     * <p>Lua 스크립트 안에서 한 트랜잭션으로:
     * <ol>
     *   <li>역인덱스 SETNX 로 중복 진입 방지</li>
     *   <li>INCR 시퀀스 + epoch_milli 로 tie-breaker score 생성</li>
     *   <li>Sorted Set 추가 (ZADD)</li>
     *   <li>Hash 메타 저장 (HSET) + TTL (EXPIRE)</li>
     * </ol>
     */
    @Override
    public void enqueue(QueueToken token) {
        String programKey = programKey(token.getProgramId());
        String tokenKey = tokenKey(token.getId());
        String userProgramKey = userProgramKey(token.getUserId(), token.getProgramId());
        String seqKey = seqKey(token.getProgramId());

        String tokenIdStr = token.getId().asString();
        long issuedAtEpochMilli = token.getIssuedAt().toEpochMilli();
        long ttlSeconds = properties.waitingTtl().getSeconds();

        Long result = redisTemplate.execute(
            enqueueScript,
            List.of(userProgramKey, programKey, tokenKey, seqKey),
            tokenIdStr,
            token.getUserId().asString(),
            token.getProgramId().asString(),
            String.valueOf(issuedAtEpochMilli),
            token.getStatus().name(),
            String.valueOf(ttlSeconds)
        );

        if (result == null || result == 0L) {
            throw new DuplicateTokenException();
        }
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
        String entryToken = entries.get(FIELD_ENTRY_TOKEN);     // null 가능 (WAITING 상태)

        return QueueToken.restore(id, userId, programId, issuedAt, status, entryToken);
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
     * Redis 기반 delete 구현 (Lua 스크립트로 원자 처리).
     *
     * <p>Lua 스크립트 안에서 한 트랜잭션으로:
     * <ol>
     *   <li>역인덱스 compare-and-delete (다른 토큰 차지 시 보존)</li>
     *   <li>Sorted Set 멤버 제거 (ZREM)</li>
     *   <li>Hash 키 삭제 (DEL)</li>
     * </ol>
     *
     * <p>이미 만료 / 삭제된 토큰에 대해서도 안전하게 호출 가능 (멱등).</p>
     */
    @Override
    public void delete(QueueToken token) {
        String programKey = programKey(token.getProgramId());
        String tokenKey = tokenKey(token.getId());
        String userProgramKey = userProgramKey(token.getUserId(), token.getProgramId());
        String tokenIdStr = token.getId().asString();

        redisTemplate.execute(
            deleteScript,
            List.of(userProgramKey, programKey, tokenKey),
            tokenIdStr
        );
    }

    /**
     * Redis 기반 findPosition 구현.
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
    public Optional<Long> findPosition(UserId userId, ProgramId programId) {
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
     * <p>Hash 가 만료된 orphan 케이스는 findById 측에서 빈 Map 으로 자연 필터링된다.</p>
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

    /**
     * Redis 기반 admit 구현.
     *
     * <p>2가지 작업을 트랜잭션으로 처리한다:
     * <ol>
     *   <li>Sorted Set 에서 토큰 멤버 제거 (큐에서 빠짐 → position 조회 X)</li>
     *   <li>Hash 의 status / entryToken 업데이트</li>
     * </ol>
     *
     * <p>역인덱스는 유지 — 사용자가 GET 으로 자기 토큰 조회 가능 (status: ADMITTED 응답).</p>
     */
    public void admit(QueueToken token) {
        String programKey = programKey(token.getProgramId());
        String tokenKey = tokenKey(token.getId());
        String tokenIdStr = token.getId().asString();

        Map<String, String> updates = Map.of(
            FIELD_STATUS, token.getStatus().name(),
            FIELD_ENTRY_TOKEN, token.getEntryToken()
        );

        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @SuppressWarnings({"unchecked"})
            @Override
            public List<Object> execute(RedisOperations operations) {
                operations.multi();
                // 1. Sorted Set 에서 멤버 제거 (큐에서 빠짐)
                operations.opsForZSet().remove(programKey, tokenIdStr);
                // 2. Hash 의 status / entryToken 업데이트
                operations.opsForHash().putAll(tokenKey, updates);
                return operations.exec();
            }
        });
    }

    /**
     * Redis 기반 deleteAllByProgramId 구현 (Lua 스크립트로 원자 처리).
     *
     * <p>Lua 스크립트 안에서:
     * <ol>
     *   <li>SCAN 으로 모든 token Hash 키 조회</li>
     *   <li>programId 일치 토큰만 compare-and-delete (역인덱스 + Sorted Set + Hash)</li>
     *   <li>Sorted Set 자체 + seqKey 일괄 삭제</li>
     * </ol>
     *
     * <p>역인덱스 삭제는 compare-and-delete 로 TOCTOU 방어:
     * 동시에 새 토큰이 같은 사용자로 enqueue 되어도 새 토큰의 역인덱스는 보존.
     */
    @Override
    public void deleteAllByProgramId(ProgramId programId) {
        String programKey = programKey(programId);
        String programIdStr = programId.asString();
        String seqKey = seqKey(programId);

        Long processedCount = redisTemplate.execute(
            deleteAllByProgramScript,
            List.of(programKey, seqKey),
            programIdStr,
            QUEUE_KEY_PREFIX + TOKEN_KEY_PREFIX,        // tokenKeyPrefix
            QUEUE_KEY_PREFIX + USER_KEY_PREFIX,          // userProgramKeyPrefix
            ":" + PROGRAM_KEY_PREFIX                     // programKeyInfix
        );

        log.info("프로그램 토큰 삭제 완료. programId={}, 삭제 키 수={}",
            programIdStr, processedCount);
    }

    private Set<String> scanKeys(String pattern) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> result = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    result.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return result;
        });
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

    private String seqKey(ProgramId programId) {
        return SEQ_KEY_PREFIX + programId.asString();
    }
}
