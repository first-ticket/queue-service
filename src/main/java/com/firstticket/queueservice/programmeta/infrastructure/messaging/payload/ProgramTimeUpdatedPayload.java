package com.firstticket.queueservice.programmeta.infrastructure.messaging.payload;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * program.time.updated 토픽 페이로드.
 * 스케줄 등록 / 변경 시 발행.
 */
public record ProgramTimeUpdatedPayload(
    UUID programId,
    LocalDateTime openAt,
    LocalDateTime closeAt
) {
}
