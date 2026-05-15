package com.firstticket.queueservice.queuetoken.infrastructure.event;

import com.firstticket.queueservice.queuetoken.domain.QueueTokenRepository;
import com.firstticket.queueservice.queuetoken.domain.vo.ProgramId;
import com.firstticket.queueservice.shared.event.ProgramCancelledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 도메인 이벤트를 수신하여 queuetoken Aggregate 의 상태를 정리.
 * Program 이 취소되면 해당 프로그램의 모든 대기 / 입장 토큰을 삭제한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProgramCancelledEventListener {

    private final QueueTokenRepository queueTokenRepository;

    @EventListener
    public void onProgramCancelled(ProgramCancelledEvent event) {
        log.info("ProgramCancelledEvent 수신. programId={}", event.programId());

        // shared 이벤트의 UUID 를 queuetoken 의 ProgramId VO 로 변환
        ProgramId programId = ProgramId.of(event.programId());

        queueTokenRepository.deleteAllByProgramId(programId);
    }
}
