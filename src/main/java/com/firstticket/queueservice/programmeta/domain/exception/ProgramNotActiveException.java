package com.firstticket.queueservice.programmeta.domain.exception;

import com.firstticket.common.exception.BusinessException;
import com.firstticket.queueservice.queuetoken.domain.exception.QueueErrorCode;

/**
 * 현재 시점이 프로그램의 입장 가능 시간이 아닐 때.
 *
 * <p>본질:
 * <ul>
 *   <li>openAt 전</li>
 *   <li>closeAt 후</li>
 *   <li>CANCELLED 상태</li>
 * </ul>
 */
public class ProgramNotActiveException extends BusinessException {

    public ProgramNotActiveException() {
        super(QueueErrorCode.PROGRAM_NOT_ACTIVE);
    }
}
