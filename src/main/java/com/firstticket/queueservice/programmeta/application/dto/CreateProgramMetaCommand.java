package com.firstticket.queueservice.programmeta.application.dto;

import com.firstticket.queueservice.programmeta.domain.ProgramStatus;
import com.firstticket.queueservice.programmeta.domain.vo.ProgramId;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * program.created 이벤트 처리용 Command.
 * Kafka Consumer 가 외부 Payload 를 변환하여 application 에 전달한다.
 *
 * <p>openAt / closeAt 은 생성 시점엔 스케줄 미정이라 null 가능.</p>
 */
public record CreateProgramMetaCommand(
    ProgramId programId,
    LocalDateTime openAt,
    LocalDateTime closeAt,
    ProgramStatus status
) {
    public static CreateProgramMetaCommand of(
        UUID programId,
        LocalDateTime openAt,
        LocalDateTime closeAt,
        String status
    ) {
        return new CreateProgramMetaCommand(
            ProgramId.of(programId),
            openAt,
            closeAt,
            ProgramStatus.parse(status)
        );
    }
}
