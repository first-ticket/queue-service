package com.firstticket.queueservice.programmeta.infrastructure.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firstticket.queueservice.programmeta.domain.ProgramMeta;
import com.firstticket.queueservice.programmeta.domain.ProgramMetaRepository;
import com.firstticket.queueservice.programmeta.domain.ProgramStatus;
import com.firstticket.queueservice.programmeta.domain.vo.ProgramId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ProgramMetaRepository 의 Redis 구현체.
 *
 * <p>키 패턴: {@code queue:program:meta:{programId}}<br>
 * 값: ProgramMeta 의 필드를 담은 JSON 문자열.</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisProgramMetaRepository implements ProgramMetaRepository {

    private static final String KEY_PREFIX = "queue:program:meta:";
    private static final String KEY_PATTERN = KEY_PREFIX + "*";

    // JSON 필드 이름 상수
    private static final String FIELD_PROGRAM_ID = "programId";
    private static final String FIELD_OPEN_AT = "openAt";
    private static final String FIELD_CLOSE_AT = "closeAt";
    private static final String FIELD_STATUS = "status";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * ProgramMeta 저장 (overwrite).
     *
     * <p>이벤트 수신 시마다 호출되어 캐시를 갱신. 같은 programId 의 기존 값은 덮어쓴다.
     * openAt / closeAt 이 null 이면 빈 문자열로 저장한다 (null 직렬화 회피).</p>
     */
    @Override
    public void save(ProgramMeta programMeta) {
        try {
            Map<String, String> data = Map.of(
                FIELD_PROGRAM_ID, programMeta.getProgramId().asString(),
                FIELD_OPEN_AT, programMeta.getOpenAt() == null ? "" : programMeta.getOpenAt().toString(),
                FIELD_CLOSE_AT, programMeta.getCloseAt() == null ? "" : programMeta.getCloseAt().toString(),
                FIELD_STATUS, programMeta.getStatus().name()
            );
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(buildKey(programMeta.getProgramId()), json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("ProgramMeta 직렬화 실패", e);
        }
    }

    /**
     * programId 로 ProgramMeta 단건 조회.
     */
    @Override
    public Optional<ProgramMeta> findById(ProgramId programId) {
        String json = redisTemplate.opsForValue().get(buildKey(programId));
        if (json == null) return Optional.empty();
        return Optional.of(deserialize(json));
    }

    /**
     * 모든 ProgramMeta 조회.
     * SCAN 으로 키 목록을 가져온 후 각 키별 GET.
     */
    @Override
    public List<ProgramMeta> findAll() {
        ArrayList<ProgramMeta> result = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(
            ScanOptions.scanOptions().match(KEY_PATTERN).count(100).build()
        )) {
            while (cursor.hasNext()) {
                String json = redisTemplate.opsForValue().get(cursor.next());
                if (json != null) {
                    result.add(deserialize(json));
                }
            }
        }
        return result;
    }

    /**
     * programId 로 ProgramMeta 삭제.
     * 이미 없어도 안전 (Redis DEL 의 멱등성).
     */
    @Override
    public void deleteById(ProgramId programId) {
        redisTemplate.delete(buildKey(programId));
    }

    /**
     * 현재 시각 기준 활성 프로그램 ID 목록 조회.
     *
     * <p>findAll() 결과를 도메인의 {@link ProgramMeta#isActiveAt} 로 필터링.
     * 활성 = CANCELLED 아니고 스케줄 설정됐고 현재 시각이 범위 안.</p>
     *
     * <p>현재 모든 ProgramMeta 를 메모리로 가져와 필터링하는 본질이라,
     * 프로그램 수가 많아지면 비효율. 미래엔 별도 인덱스 키
     * (예: queue:program:active = Set of programIds) 도입 고려.</p>
     */
    @Override
    public List<ProgramId> findActiveProgramIds(LocalDateTime now) {
        return findAll().stream()
            .filter(programMeta -> programMeta.isActiveAt(now))
            .map(ProgramMeta::getProgramId)
            .toList();
    }

    // ===== 헬퍼 =====

    private String buildKey(ProgramId programId) {
        return KEY_PREFIX + programId.asString();
    }

    /**
     * JSON 문자열을 ProgramMeta 도메인 객체로 복원.
     * 빈 문자열은 null 로 변환 (openAt / closeAt 의 미정 상태 표현).
     */
    private ProgramMeta deserialize(String json) {
        try {
            Map<String, String> data = objectMapper.readValue(json, Map.class);
            return ProgramMeta.of(
                ProgramId.of(UUID.fromString(data.get(FIELD_PROGRAM_ID))),
                parseDateTime(data.get(FIELD_OPEN_AT)),
                parseDateTime(data.get(FIELD_CLOSE_AT)),
                ProgramStatus.valueOf(data.get(FIELD_STATUS))
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("ProgramMeta 역직렬화 실패", e);
        }
    }

    /**
     * 빈 문자열을 null 로, 그 외는 LocalDateTime 으로 변환.
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) return null;
        return LocalDateTime.parse(value);
    }
}
