package com.firstticket.queueservice.programmeta.domain;

import com.firstticket.queueservice.programmeta.domain.vo.ProgramId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ProgramMeta 영속화 인터페이스.
 * 구현체는 인프라 계층 (infrastructure/persistence) 에 위치한다.
 */
public interface ProgramMetaRepository {

    /**
     * ProgramMeta 저장 (overwrite).
     */
    void save(ProgramMeta programMeta);

    Optional<ProgramMeta> findById(ProgramId programId);

    List<ProgramMeta> findAll();

    void deleteById(ProgramId programId);

    /**
     * 현재 시각 기준 활성 (CANCELLED 아니고 openAt ~ closeAt 사이) 인
     * 모든 program 의 ID 목록을 반환한다.
     * AdmissionScheduler 등에서 활성 프로그램 순회 시 사용.
     */
    List<ProgramId> findActiveProgramIds(LocalDateTime now);
}
