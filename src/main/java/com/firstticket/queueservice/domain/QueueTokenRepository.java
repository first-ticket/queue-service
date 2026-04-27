package com.firstticket.queueservice.domain;

import com.firstticket.queueservice.domain.vo.ProgramId;
import com.firstticket.queueservice.domain.vo.QueueTokenId;
import com.firstticket.queueservice.domain.vo.UserId;

import java.util.List;
import java.util.Optional;

/**
 * 대기 토큰의 영속성 저장소 추상화
 * 도메인 계층에서 정의하는 인터페이스로, 인프라 구현(Redis 등)에 무관하다.
 */
public interface QueueTokenRepository {

    /**
     * 대기 토큰을 저장한다 (신규 또는 갱신).
     */
    QueueToken save(QueueToken token);

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
    Optional<Long> findRanking(QueueTokenId tokenId, ProgramId programId);

    /**
     * 특정 프로그램의 다음 입장 승인 대상자들을 조회한다 (앞에서 batchSize 명).
     */
    List<QueueToken> findAdmissionCandidates(ProgramId programId, int batchSize);

}
