package com.firstticket.queueservice.queuetoken.domain.exception;

import com.firstticket.common.exception.BusinessException;

/**
 * 입장하려는 프로그램이 ProgramMeta 캐시에 존재하지 않을 때.
 *
 * <p>가능한 본질:
 * <ul>
 *   <li>program.created 이벤트 도착 전</li>
 *   <li>존재하지 않는 programId 로 입장 시도</li>
 * </ul>
 */
public class ProgramNotFoundException extends BusinessException {

    public ProgramNotFoundException() {
        super(QueueErrorCode.PROGRAM_NOT_FOUND);
    }
}
