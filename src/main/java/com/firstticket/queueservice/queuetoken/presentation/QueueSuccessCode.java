package com.firstticket.queueservice.queuetoken.presentation;

import com.firstticket.common.response.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QueueSuccessCode implements SuccessCode {
    QUEUE_TOKEN_ISSUED(HttpStatus.CREATED, "대기 토큰이 발급되었습니다"),
    QUEUE_TOKEN_FOUND(HttpStatus.OK, "대기 토큰을 조회했습니다"),
    QUEUE_TOKEN_CANCELLED(HttpStatus.OK, "대기를 취소했습니다");

    private final HttpStatus status;
    private final String message;
}
