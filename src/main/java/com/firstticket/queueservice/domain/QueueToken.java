package com.firstticket.queueservice.domain;

import com.firstticket.queueservice.domain.exception.InvalidTokenStateException;
import com.firstticket.queueservice.domain.exception.TokenNotFoundException;
import com.firstticket.queueservice.domain.vo.IssuedAt;
import com.firstticket.queueservice.domain.vo.ProgramId;
import com.firstticket.queueservice.domain.vo.QueueTokenId;
import com.firstticket.queueservice.domain.vo.UserId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * 대기 토큰 애그리거트 루트.
 *
 * <p>한 사용자의 한 프로그램에 대한 대기 상태를 표현한다.
 * 발급(WAITING) 후 입장 승인(ADMITTED), 취소(CANCELLED), 만료(EXPIRED) 중 하나로 전이된다.
 *
 * <p>상태 전이 규칙:
 * <ul>
 *   <li>WAITING → ADMITTED, CANCELLED, EXPIRED</li>
 *   <li>나머지 상태에서는 전이 불가 (최종 상태)</li>
 * </ul>
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
            QueueTokenId.of(),
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

    /**
     * 토큰의 소유자가 주어진 사용자인지 검증한다.
     *
     * <p>본인 소유가 아니면 {@link TokenNotFoundException} 을 던진다.
     * "권한 없음" 대신 "토큰 없음" 으로 통일하여, 토큰의 존재 여부 정보 누출을 방지한다.
     */
    public void verifyOwner(UserId userId) {
        Objects.requireNonNull(userId, "UserId는 필수입니다");
        if (this.userId.equals(userId)) {
            throw new TokenNotFoundException();
        }
    }

    /**
     * 현재 상태가 WAITING이 아니면 예외를 던진다.
     */
    private void ensureWaiting() {
        if (status.isTerminal()) {
            throw new InvalidTokenStateException();
        }
    }
}
