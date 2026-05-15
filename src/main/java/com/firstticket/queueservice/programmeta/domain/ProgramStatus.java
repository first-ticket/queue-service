package com.firstticket.queueservice.programmeta.domain;

/**
 * Program 의 생명주기 상태.
 * program-service 의 이벤트로 갱신되며, queue-service 는 이 상태를 캐시한다.
 *
 * 활성/비활성 (OPEN / CLOSED) 은 별도 상태로 관리하지 않고
 * openAt / closeAt 시간으로 동적 판단한다.
 */
public enum ProgramStatus {
    /**
     * 프로그램이 생성된 기본 상태.
     * 스케줄 (openAt / closeAt) 등록 / 변경과 무관하게 유지된다.
     * 시간 조건이 맞으면 토큰 발급 / 입장 허용.
     */
    DRAFT,

    /**
     * 프로그램이 취소된 상태.
     * 모든 대기 토큰을 정리하고 신규 토큰 발급을 거부한다.
     */
    CANCELLED
}
