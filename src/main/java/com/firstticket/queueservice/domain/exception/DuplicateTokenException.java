package com.firstticket.queueservice.domain.exception;

import com.firstticket.common.exception.BusinessException;

public class DuplicateTokenException extends BusinessException {
    public DuplicateTokenException() {
        super(QueueErrorCode.DUPLICATE_TOKEN);
    }
}
