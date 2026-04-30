package com.firstticket.queueservice.application.dto;

import com.firstticket.queueservice.domain.vo.ProgramId;
import com.firstticket.queueservice.domain.vo.UserId;

import java.util.UUID;

public record GetQueueTokenQuery(
    UserId userId,
    ProgramId programId
) {
    public static GetQueueTokenQuery of(UUID userId, UUID programId) {
        return new GetQueueTokenQuery(
            UserId.of(userId),
            ProgramId.of(programId)
        );
    }
}
