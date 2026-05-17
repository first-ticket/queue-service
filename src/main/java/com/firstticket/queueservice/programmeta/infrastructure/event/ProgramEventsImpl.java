package com.firstticket.queueservice.programmeta.infrastructure.event;

import com.firstticket.queueservice.programmeta.domain.event.ProgramEvents;
import com.firstticket.queueservice.programmeta.domain.vo.ProgramId;
import com.firstticket.queueservice.shared.event.ProgramCancelledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * ProgramEvents 의 Spring 구현체.
 * 도메인이 Spring 에 직접 의존하지 않도록 인프라 계층에서 위임만 수행한다.
 */
@Component
@RequiredArgsConstructor
public class ProgramEventsImpl implements ProgramEvents {

    private final ApplicationEventPublisher applicationEventPublisher;
    @Override
    public void publishProgramCancelled(ProgramId programId) {
        applicationEventPublisher.publishEvent(
                new ProgramCancelledEvent(programId.id())
        );
    }
}
