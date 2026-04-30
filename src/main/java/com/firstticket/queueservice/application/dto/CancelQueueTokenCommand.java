package com.firstticket.queueservice.application.dto;

import com.firstticket.queueservice.domain.vo.ProgramId;
import com.firstticket.queueservice.domain.vo.UserId;

import java.util.UUID;

public record CancelQueueTokenCommand(
    UserId userId,
    ProgramId programId
) {
    public static CancelQueueTokenCommand of(UUID userId, UUID programId) {
        return new CancelQueueTokenCommand(
            UserId.of(userId),
            ProgramId.of(programId)
        );
    }
}
