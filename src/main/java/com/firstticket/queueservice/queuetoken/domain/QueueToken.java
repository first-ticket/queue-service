package com.firstticket.queueservice.queuetoken.domain;

import com.firstticket.queueservice.queuetoken.domain.exception.InvalidTokenStateException;
import com.firstticket.queueservice.queuetoken.domain.vo.IssuedAt;
import com.firstticket.queueservice.queuetoken.domain.vo.ProgramId;
import com.firstticket.queueservice.queuetoken.domain.vo.QueueTokenId;
import com.firstticket.queueservice.queuetoken.domain.vo.UserId;
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
    private String entryToken;

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
            TokenStatus.WAITING,
            null    // entryToken: 발급 시점엔 없음, admit 시 부여
        );
    }

    /**
     * Redis 등 외부 저장소에서 토큰을 복원한다.
     */
    public static QueueToken restore(
        QueueTokenId id,
        UserId userId,
        ProgramId programId,
        IssuedAt issuedAt,
        TokenStatus status,
        String entryToken
    ) {
        Objects.requireNonNull(id, "QueueTokenId는 필수입니다");
        Objects.requireNonNull(userId, "UserId는 필수입니다");
        Objects.requireNonNull(programId, "ProgramId는 필수입니다");
        Objects.requireNonNull(issuedAt, "IssuedAt은 필수입니다");
        Objects.requireNonNull(status, "TokenStatus는 필수입니다");
        return new QueueToken(id, userId, programId, issuedAt, status, entryToken);
    }

    /**
     * 입장을 승인한다 (WAITING -> ADMITTED)
     */
    public void admit(String entryToken) {
        ensureWaiting();
        this.status = TokenStatus.ADMITTED;
        this.entryToken = entryToken;
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
     * 현재 상태가 WAITING이 아니면 예외를 던진다.
     */
    private void ensureWaiting() {
        if (status.isTerminal()) {
            throw new InvalidTokenStateException();
        }
    }
}
