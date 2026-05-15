package com.firstticket.queueservice.programmeta.infrastructure.messaging.payload;

import java.util.UUID;

/**
 * program.cancelled 토픽 페이로드.
 * 프로그램 취소 시 발행. queue-service 가 활성 토큰 모두 정리.
 */
public record ProgramCancelledPayload(
    UUID programId,
    String status
) {
}
