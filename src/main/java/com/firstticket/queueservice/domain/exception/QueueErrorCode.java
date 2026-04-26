package com.firstticket.queueservice.domain.exception;

import com.firstticket.common.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QueueErrorCode implements ErrorCode {
    INVALID_TOKEN_STATE(HttpStatus.BAD_REQUEST, "대기 토큰 상태 전이 규칙을 위반했습니다");

    private final HttpStatus status;
    private final String message;
}
