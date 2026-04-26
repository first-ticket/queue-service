package com.firstticket.queueservice.domain;

import com.firstticket.queueservice.domain.exception.InvalidTokenStateException;
import com.firstticket.queueservice.domain.vo.IssuedAt;
import com.firstticket.queueservice.domain.vo.ProgramId;
import com.firstticket.queueservice.domain.vo.QueueTokenId;
import com.firstticket.queueservice.domain.vo.UserId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 1. 대기 토큰은 발급된 시점에 WAITING 상태로 시작한다
 * 2. WAITING 상태에서만 입장 승인(ADMITTED), 취소(CANCELLED), 만료(EXPIRED)로 전이 가능하다
 * 3. ADMITTED, CANCELLED, EXPIRED 는 최종 상태로 더 이상 전이 불가
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QueueToken {
    private final QueueTokenId id;
    private final UserId userId;
    private final ProgramId programId;
    private final IssuedAt issuedAt;
    private TokenStatus status;

    /**
     * 새로운 대기 토큰을 발급한다.
     */
    public static QueueToken issue(UserId userId, ProgramId programId) {
        Objects.requireNonNull(userId, "UserId는 필수입니다");
        Objects.requireNonNull(programId, "ProgramId는 필수입니다");
        return new QueueToken(
            com.firstticket.queueservice.domain.vo.QueueTokenId.of(),
            userId,
            programId,
            IssuedAt.now(),
            TokenStatus.WAITING);
    }

    /**
     * Redis 등 외부 저장소에서 토큰을 복원한다.
     */
    public static QueueToken restore(
        QueueTokenId id,
        UserId userId,
        ProgramId programId,
        IssuedAt issuedAt,
        TokenStatus status
    ) {
        Objects.requireNonNull(id, "QueueTokenId는 필수입니다");
        Objects.requireNonNull(userId, "UserId는 필수입니다");
        Objects.requireNonNull(programId, "ProgramId는 필수입니다");
        Objects.requireNonNull(issuedAt, "IssuedAt은 필수입니다");
        Objects.requireNonNull(status, "TokenStatus는 필수입니다");
        return new QueueToken(id, userId, programId, issuedAt, status);
    }

    /**
     * 입장을 승인한다 (WAITING -> ADMITTED)
     */
    public void admit() {
        ensureWaiting();
        this.status = TokenStatus.ADMITTED;
    }

    /**
     * 사용자가 대기를 취소한다 (WAITING -> CANCELLED)
     */
    public void cancel() {
        ensureWaiting();
        this.status = TokenStatus.CANCELLED;
    }

    /**
     * 시간 만료로 토큰을 폐기한다 (WAITING -> EXPIRED)
     */
    public void expire() {
        ensureWaiting();
        this.status = TokenStatus.EXPIRED;
    }


    private void ensureWaiting() {
        if (status.isTerminal()) {
            throw new InvalidTokenStateException();
        }
    }
}
