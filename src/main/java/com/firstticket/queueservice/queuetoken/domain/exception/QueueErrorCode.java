package com.firstticket.queueservice.queuetoken.domain.exception;

import com.firstticket.common.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QueueErrorCode implements ErrorCode {
    INVALID_TOKEN_STATE(HttpStatus.BAD_REQUEST, "대기 토큰 상태 전이 규칙을 위반했습니다"),
    DUPLICATE_TOKEN(HttpStatus.CONFLICT, "이미 대기 중인 토큰이 있습니다"),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "토큰을 찾을 수 없습니다");

    private final HttpStatus status;
    private final String message;
}
