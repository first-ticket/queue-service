package com.firstticket.queueservice.domain.exception;

import com.firstticket.common.exception.BusinessException;
import com.firstticket.common.response.ErrorCode;

/**
 * 대기 토큰의 상태 전이 규칙을 위반했을 때 발생하는 예외
 *
 *  예: ADMITTED 상태인 토큰을 cancel() 시도
 */
public class InvalidTokenStateException extends BusinessException {

    public InvalidTokenStateException() {
        super(QueueErrorCode.INVALID_TOKEN_STATE);
    }
}
