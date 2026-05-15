package com.firstticket.queueservice.programmeta.infrastructure.messaging.payload;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * program.created 토픽 페이로드.
 * 프로그램 생성 시점에는 스케줄 미등록이므로 openAt/closeAt 은 null 가능.
 */
public record ProgramCreatedPayload(
    UUID programId,
    LocalDateTime openAt,
    LocalDateTime closeAt,
    String status
) {
}
