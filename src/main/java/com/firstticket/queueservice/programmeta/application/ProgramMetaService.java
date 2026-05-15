package com.firstticket.queueservice.programmeta.application;

import com.firstticket.queueservice.programmeta.application.dto.CancelProgramCommand;
import com.firstticket.queueservice.programmeta.application.dto.CreateProgramMetaCommand;
import com.firstticket.queueservice.programmeta.application.dto.UpdateProgramTimeCommand;
import com.firstticket.queueservice.programmeta.domain.ProgramMeta;
import com.firstticket.queueservice.programmeta.domain.ProgramMetaRepository;
import com.firstticket.queueservice.programmeta.domain.event.ProgramEvents;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ProgramMeta 도메인의 Command 처리 서비스.
 * Kafka Consumer 가 전달한 Command 를 받아 Aggregate 의 상태를 변경하고,
 * 필요 시 도메인 이벤트를 발행한다.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ProgramMetaService {
    private final ProgramMetaRepository programMetaRepository;
    private final ProgramEvents programEvents;

    /**
     * program.created 처리.
     * ProgramMeta 새로 생성하여 저장.
     */
    public void handleCreated(CreateProgramMetaCommand command) {
        log.info("Program created. programId={}, status={}", command.programId(), command.status());

        ProgramMeta programMeta = ProgramMeta.of(
            command.programId(),
            command.openAt(),
            command.closeAt(),
            command.status()
        );
        programMetaRepository.save(programMeta);
    }

    /**
     * program.time.updated 처리.
     * 기존 ProgramMeta 의 openAt / closeAt 갱신.
     * Meta 가 존재하지 않으면 경고 로그만 남긴다 (이벤트 순서가 어긋난 경우 대비).
     */
    public void handleTimeUpdated(UpdateProgramTimeCommand command) {
        log.info("Program time updated. programId={}, openAt={}, closeAt={}",
            command.programId(), command.openAt(), command.closeAt());

        programMetaRepository.findById(command.programId())
            .ifPresentOrElse(
                programMeta -> {
                    programMeta.updateTime(command.openAt(), command.closeAt());
                    programMetaRepository.save(programMeta);
                },
                () -> log.warn("ProgramMeta 없음 (time updated). programId={}", command.programId())
            );
    }

    /**
     * program.cancelled 처리.
     * 1. ProgramMeta 의 status 를 CANCELLED 로 갱신.
     * 2. ProgramCancelledEvent 발행 (queuetoken Aggregate 가 토큰 정리).
     */
    public void handleCancelled(CancelProgramCommand command) {
        log.info("Program cancelled. programId={}", command.programId());

        programMetaRepository.findById(command.programId())
            .ifPresentOrElse(
                programMeta -> {
                    programMeta.cancel();
                    programMetaRepository.save(programMeta);
                },
                () -> log.warn("ProgramMeta 없음 (cancelled). programId={}", command.programId())
            );

        // queuetoken Aggregate 에 이벤트 발급
        programEvents.publishProgramCancelled(command.programId());
    }

}
