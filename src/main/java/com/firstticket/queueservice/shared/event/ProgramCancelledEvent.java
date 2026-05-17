package com.firstticket.queueservice.shared.event;

import java.util.UUID;

/**
 * Program 이 취소되었을 때 발행되는 도메인 이벤트.
 * programmeta Aggregate 가 발행하고 queuetoken Aggregate 가 수신한다.
 */
public record ProgramCancelledEvent(UUID programId) {}
