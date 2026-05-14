package com.firstticket.queueservice.queuetoken.domain.exception;

import com.firstticket.common.exception.BusinessException;

/**
 * 같은 사용자 + 같은 프로그램 조합으로 토큰을 중복 발급하려 할 때 발생하는 예외.
 *
 * <p>정상 흐름에 발생하지 않으며, 동시 요청으로 인한 race condition 시 Repository 의 setIfAbsent 에 의해 노출된다.
 *
 * <p>해결: 클라이언트 재시도. 근본 해결은 v0.2.0 의 Lua Script 통합 예정.
 */
public class DuplicateTokenException extends BusinessException {
    public DuplicateTokenException() {
        super(QueueErrorCode.DUPLICATE_TOKEN);
    }
}
