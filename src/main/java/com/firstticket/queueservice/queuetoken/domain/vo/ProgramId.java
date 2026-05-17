package com.firstticket.queueservice.queuetoken.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * Program 을 식별하는 Value Object.
 * 원본은 program-service 가 소유하며, queue-service 는 동일한 UUID 를 참조한다.
 */
public record ProgramId(UUID id) {

    public ProgramId {
        Objects.requireNonNull(id, "ProgramId는 null일 수 없습니다");
    }

    public static ProgramId of(UUID id) {
        return new ProgramId(id);
    }

    public static ProgramId fromString(String id) {
        return new ProgramId(UUID.fromString(id));
    }

    public String asString() {
        return id.toString();
    }
}
