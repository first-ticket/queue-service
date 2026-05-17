package com.firstticket.queueservice.programmeta.application.dto;

import com.firstticket.queueservice.programmeta.domain.ProgramStatus;
import com.firstticket.queueservice.programmeta.domain.vo.ProgramId;

import java.util.UUID;

/**
 * program.cancelled 이벤트 처리용 Command.
 */
public record CancelProgramCommand(
    ProgramId programId,
    ProgramStatus status
) {
    public static CancelProgramCommand of(UUID programId, String status) {
        return new CancelProgramCommand(
            ProgramId.of(programId),
            ProgramStatus.parse(status)
        );
    }
}
