package com.firstticket.queueservice.domain;

/**
 * 대기 토큰의 상태
 *
 * 상태 전이 규칙:
 *   WAITING → ADMITTED  (입장 승인)
 *   WAITING → CANCELLED (사용자 취소)
 *   WAITING → EXPIRED   (시간 만료)
 *   ADMITTED, CANCELLED, EXPIRED → (전이 불가, 최종 상태)
 */
public enum TokenStatus {

    // 대기 중
    WAITING,

    // 입장 승인됨
    ADMITTED,

    // 사용자 취소
    CANCELLED,

    // 시간 만료
    EXPIRED;

    // 전이 불가 여부 반환
    public boolean isTerminal() {
        return this != WAITING;
    }
}
