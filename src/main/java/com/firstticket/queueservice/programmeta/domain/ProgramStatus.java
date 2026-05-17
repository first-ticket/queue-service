package com.firstticket.queueservice.programmeta.domain;

import java.util.Locale;
import java.util.Objects;

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
    CANCELLED;

    /**
     * 문자열을 ProgramStatus 로 변환.
     * 외부 (Kafka 페이로드 등) 에서 받은 문자열의 포맷 편차를 정정한다
     * (공백 / 대소문자 정규화 후 변환).
     *
     * @throws NullPointerException     value 가 null 일 때
     * @throws IllegalArgumentException 지원하지 않는 status 일 때
     */
    public static ProgramStatus parse(String value) {
        Objects.requireNonNull(value, "status는 null일 수 없습니다");
        try {
            return ProgramStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 program status: " + value, e);
        }
    }
}
