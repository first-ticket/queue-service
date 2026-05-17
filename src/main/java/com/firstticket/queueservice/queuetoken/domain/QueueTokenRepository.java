package com.firstticket.queueservice.queuetoken.domain;

import com.firstticket.queueservice.queuetoken.domain.exception.DuplicateTokenException;
import com.firstticket.queueservice.queuetoken.domain.vo.ProgramId;
import com.firstticket.queueservice.queuetoken.domain.vo.QueueTokenId;
import com.firstticket.queueservice.queuetoken.domain.vo.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 대기 토큰의 영속성 저장소.
 *
 * <p>도메인 규칙: 한 사용자는 한 프로그램에 활성 토큰을 하나만 가질 수 있다.
 * 중복 진입 시도는 인프라 구현체가 거부한다.
 */
public interface QueueTokenRepository {

    /**
     * 신규 대기 토큰을 등록한다.
     *
     * @throws DuplicateTokenException 같은 사용자가 이미 같은 프로그램에 토큰 보유 시
     */
    void enqueue(QueueToken token);

    /**
     * 토큰 ID로 대기 토큰을 조회한다.
     */
    Optional<QueueToken> findById(QueueTokenId id);

    /**
     * 사용자가 특정 프로그램에 보유한 대기 토큰을 조회한다.
     * 동일 사용자는 한 프로그램에 하나의 활성 토큰만 가질 수 있다.
     */
    Optional<QueueToken> findByUserIdAndProgramId(UserId userId, ProgramId programId);

    /**
     * 대기 토큰을 삭제한다.
     */
    void delete(QueueToken token);

    /**
     * 사용자가 특정 프로그램의 대기 순번을 조회한다.
     * 토큰이 없거나 WAITING 상태가 아니면 Optional.empty()
     */
    Optional<Long> findPosition(UserId userId, ProgramId programId);

    /**
     * 특정 프로그램의 다음 입장 승인 대상자들을 조회한다 (앞에서 batchSize 명).
     */
    List<QueueToken> findAdmissionCandidates(ProgramId programId, int batchSize);

    /**
     * 입장 승인된 토큰의 상태를 업데이트한다.
     */
    void admit(QueueToken token);

    /**
     * 현재 큐가 존재하는 모든 프로그램 ID 를 조회한다.
     */
    List<ProgramId> findActiveProgramIds();

    /**
     * 특정 프로그램의 모든 대기 / 입장 토큰을 삭제한다.
     * Program 이 취소되었을 때 호출.
     */
    void deleteAllByProgramId(ProgramId programId);
}
