package com.firstticket.queueservice.programmeta.application.dto;

import com.firstticket.queueservice.programmeta.domain.vo.ProgramId;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * program.time.updated 이벤트 처리용 Command.
 * 스케줄 (openAt / closeAt) 등록 / 변경 시 사용.
 */
public record UpdateProgramTimeCommand(
    ProgramId programId,
    LocalDateTime openAt,
    LocalDateTime closeAt
) {
    public static UpdateProgramTimeCommand of(
        UUID programId,
        LocalDateTime openAt,
        LocalDateTime closeAt
    ) {
        return new UpdateProgramTimeCommand(
            ProgramId.of(programId),
            openAt,
            closeAt
        );
    }
}
