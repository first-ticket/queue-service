package com.firstticket.queueservice.domain.vo;

import java.util.Objects;
import java.util.UUID;

/**
 * 프로그램(공연) ID를 표현하는 VO.
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
