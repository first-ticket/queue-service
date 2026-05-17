package com.firstticket.queueservice.queuetoken.application.dto;

import com.firstticket.queueservice.queuetoken.domain.vo.ProgramId;
import com.firstticket.queueservice.queuetoken.domain.vo.UserId;

import java.util.UUID;

public record IssueQueueTokenCommand(
    UserId userId,
    ProgramId programId
) {
    public static IssueQueueTokenCommand of(UUID userId, UUID programId) {
        return new IssueQueueTokenCommand(
            UserId.of(userId),
            ProgramId.of(programId)
        );
    }
}
