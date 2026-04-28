package com.firstticket.queueservice.domain;

/**
 * 대기 토큰의 상태.
 *
 * <p>상태 전이 규칙:
 * <pre>
 *   WAITING → ADMITTED  (입장 승인)
 *   WAITING → CANCELLED (사용자 취소)
 *   WAITING → EXPIRED   (시간 만료)
 *   ADMITTED, CANCELLED, EXPIRED → (전이 불가, 최종 상태)
 * </pre>
 */
public enum TokenStatus {

    /** 대기 중 */
    WAITING,

    /** 입장 승인됨 */
    ADMITTED,

    /** 입장 승인됨 */
    CANCELLED,

    /** 시간 만료 */
    EXPIRED;

    /**
     * 최종 상태인지 (더 이상 전이 불가) 여부를 반환한다.
     */
    public boolean isTerminal() {
        return this != WAITING;
    }
}
