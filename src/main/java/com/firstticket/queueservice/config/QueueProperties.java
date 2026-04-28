package com.firstticket.queueservice.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * queue-service의 운영 정책 설정.
 * config-repo의 queue-service.yml 에서 외부 주입된다.
 */
@Validated
@ConfigurationProperties(prefix = "queue.token")
public record QueueProperties(

    /**
     * 대기 토큰의 TTL
     * 이 시간 내 입장 승인 못 받으면 자동 만료
     */
    @NotNull
    Duration waitingTtl,

    /**
     * 한 번의 배치 스케줄러 실행 시 입장 승인할 인원 수
     */
    @Min(1)
    int admissionBatchSize

) {
}
