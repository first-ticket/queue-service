package com.firstticket.queueservice.infrastructure.scheduler;

import com.firstticket.queueservice.config.QueueProperties;
import com.firstticket.queueservice.domain.QueueToken;
import com.firstticket.queueservice.domain.QueueTokenRepository;
import com.firstticket.queueservice.domain.vo.ProgramId;
import com.firstticket.queueservice.domain.vo.QueueTokenId;
import com.firstticket.queueservice.infrastructure.jwt.EntryTokenIssuer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 대기열 입장 승인 스케줄러.
 *
 * <p>주기적으로 활성 프로그램의 큐 앞에서 batchSize 명을 admit 한다.
 * admit 시점에 JWT 입장 토큰을 발급하고, QueueToken 의 상태를 ADMITTED 로 전이시킨다.
 *
 * <p>흐름:
 * <ol>
 *   <li>Redis SCAN 으로 활성 프로그램 (큐가 존재하는 프로그램) 발견</li>
 *   <li>각 프로그램의 큐 앞에서 batchSize 명 조회 (Sorted Set ZRANGE)</li>
 *   <li>각 토큰에 대해 JWT 입장 토큰 발급 + 도메인 상태 전이 + Redis 영속성</li>
 * </ol>
 *
 * <p>MVP 단계 한계:
 * <ul>
 *   <li>단일 인스턴스 가정 — 여러 인스턴스에서 동시 실행 시 race 가능 (v0.2.0 별도 이슈)</li>
 *   <li>활성 프로그램 발견을 Redis SCAN 에 의존 — program-service 통합 후 변경</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdmissionScheduler {

    private final QueueTokenRepository queueTokenRepository;
    private final EntryTokenIssuer entryTokenIssuer;
    private final QueueProperties queueProperties;

    /**
     * 매 5 초마다 활성 프로그램의 큐 앞 batchSize 명을 admit.
     *
     * <p>fixedRate 5000ms = 이전 실행이 끝나든 안 끝나든 5 초마다 시작.
     */
    @Scheduled(fixedRate = 5000)
    public void admit() {
        List<ProgramId> activePrograms = queueTokenRepository.findActiveProgramIds();

        if (activePrograms.isEmpty()) {
            return;
        }

        log.debug("[AdmissionScheduler] 활성 프로그램 {} 개 발견", activePrograms.size());

        for (ProgramId programId : activePrograms) {
            admitProgram(programId);
        }
    }

    /**
     * 특정 프로그램의 큐 앞 batchSize 명을 admit.
     *
     * <p>한 토큰의 admit 실패가 다른 토큰 처리에 영향을 주지 않도록 개별 try-catch 처리.
     */
    private void admitProgram(ProgramId programId) {
        int batchSize = queueProperties.admissionBatchSize();
        List<QueueToken> candidates = queueTokenRepository.findAdmissionCandidates(programId, batchSize);

        if (candidates.isEmpty()) {
            return;
        }

        int successCount = 0;
        for (QueueToken queueToken : candidates) {
            try {
                String entryToken = entryTokenIssuer.issue(queueToken);
                queueToken.admit(entryToken);
                queueTokenRepository.admit(queueToken);
                successCount++;
            } catch (Exception e) {
                log.error("[AdmissionScheduler] admit 실패 - tokenId={}, programId={}",
                    queueToken.getId().asString(), programId.asString(), e);
            }
        }

        if (successCount < candidates.size()) {
            log.warn("[AdmissionScheduler] 부분 실패 - programId={}, success={}/{}",
                programId.asString(), successCount, candidates.size());
        }
    }
}
