package com.firstticket.queueservice.queuetoken.application;

import com.firstticket.queueservice.queuetoken.application.dto.CancelQueueTokenCommand;
import com.firstticket.queueservice.queuetoken.application.dto.GetQueueTokenQuery;
import com.firstticket.queueservice.queuetoken.application.dto.IssueQueueTokenCommand;
import com.firstticket.queueservice.queuetoken.application.dto.QueueTokenResult;
import com.firstticket.queueservice.queuetoken.domain.QueueToken;
import com.firstticket.queueservice.queuetoken.domain.QueueTokenRepository;
import com.firstticket.queueservice.queuetoken.domain.exception.DuplicateTokenException;
import com.firstticket.queueservice.queuetoken.domain.exception.InvalidTokenStateException;
import com.firstticket.queueservice.queuetoken.domain.exception.TokenNotFoundException;
import com.firstticket.queueservice.queuetoken.domain.vo.ProgramId;
import com.firstticket.queueservice.queuetoken.domain.vo.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 대기열 진입 / 조회 / 취소를 처리하는 서비스.
 */
@Service
@RequiredArgsConstructor
public class QueueTokenService {

    private final QueueTokenRepository queueTokenRepository;

    /**
     * 대기열에 진입한다.
     *
     * <p>대기 토큰을 발급한다.
     * 같은 사용자 + 프로그램으로 토큰이 이미 있으면 기존 토큰을 폐기한 뒤 새로 발급한다.
     *
     * @return 발급된 토큰과 현재 순번
     * @throws DuplicateTokenException 동시 요청으로 race 발생 시 (드물게)
     */
    public QueueTokenResult issueToken(IssueQueueTokenCommand command) {
        UserId userId = command.userId();
        ProgramId programId = command.programId();

        // 같은 user+program 토큰이 있으면 폐기 후 새로 발급
        queueTokenRepository.findByUserIdAndProgramId(userId, programId)
            .ifPresent(queueTokenRepository::delete);

        QueueToken token = QueueToken.issue(userId, programId);
        // race condition: 동시 요청 시 DuplicateTokenException 가능. v0.2.0 Lua 통합으로 해결.
        queueTokenRepository.enqueue(token);

        Long position = queueTokenRepository.findPosition(userId, programId).orElse(null);
        return QueueTokenResult.of(token, position);
    }

    /**
     * 사용자의 대기 정보를 조회한다.
     *
     * <p>주로 폴링용으로 호출된다. 토큰 정보와 현재 순번을 반환한다.
     *
     * @return 토큰 정보 + 현재 순번 (ADMITTED 등 큐에서 빠진 상태면 position = null)
     * @throws TokenNotFoundException 해당 사용자의 토큰이 없을 때
     */
    public QueueTokenResult getToken(GetQueueTokenQuery query) {
        UserId userId = query.userId();
        ProgramId programId = query.programId();

        QueueToken token = queueTokenRepository.findByUserIdAndProgramId(userId, programId)
            .orElseThrow(TokenNotFoundException::new);

        Long position = queueTokenRepository.findPosition(userId, programId).orElse(null);
        return QueueTokenResult.of(token, position);
    }

    /**
     * 사용자가 대기를 취소한다.
     *
     * <p>대기 토큰을 CANCELLED 상태로 바꾼 뒤 저장소에서 제거한다.
     *
     * @throws TokenNotFoundException 해당 사용자의 토큰이 없을 때
     * @throws InvalidTokenStateException WAITING 이 아닌 상태에서 취소 시도 시 (예: ADMITTED)
     */
    public void cancelToken(CancelQueueTokenCommand command) {
        UserId userId = command.userId();
        ProgramId programId = command.programId();

        QueueToken token = queueTokenRepository.findByUserIdAndProgramId(userId, programId)
            .orElseThrow(TokenNotFoundException::new);

        // 도메인 상태 검증 + 전이 (WAITING → CANCELLED)
        token.cancel();

        queueTokenRepository.delete(token);
    }
}
