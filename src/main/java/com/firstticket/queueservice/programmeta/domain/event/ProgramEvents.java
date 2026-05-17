package com.firstticket.queueservice.programmeta.domain.event;

import com.firstticket.queueservice.programmeta.domain.vo.ProgramId;

/**
 * Program 도메인 이벤트 발행 인터페이스.
 *
 * <p>구현체는 Spring 의 ApplicationEventPublisher 를 위임 사용하며,
 * 인프라 계층 (infrastructure/event) 에 위치한다.</p>
 */
public interface ProgramEvents {
    /**
     * Program 취소 이벤트 발행.
     * queuetoken Aggregate 의 EventListener 가 수신해 토큰을 정리한다.
     */
    void publishProgramCancelled(ProgramId programId);
}
