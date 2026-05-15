package com.firstticket.queueservice.programmeta.domain;

import com.firstticket.queueservice.programmeta.domain.vo.ProgramId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Program 의 메타 정보 (Aggregate Root).
 * program-service 의 이벤트로 갱신되는 캐시 / 읽기 모델.
 *
 * <p>원본은 program-service 가 소유하므로 queue-service 는 이 객체를
 * 영구 저장하지 않으며, 필요 시 program 토픽의 처음부터 재구독으로 복구한다.</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProgramMeta {

    private ProgramId programId;
    private LocalDateTime openAt;
    private LocalDateTime closeAt;
    private ProgramStatus status;

    /**
     * 새 ProgramMeta 생성. 일반적으로 program.created 이벤트 처리 시 호출.
     *
     * @param programId UUID 형태의 program 식별자
     * @param openAt    예매 오픈 시각 (생성 시점엔 null 가능)
     * @param closeAt   예매 종료 시각 (생성 시점엔 null 가능)
     * @param status    program-service 가 전달한 상태 문자열 ("DRAFT" / "CANCELLED")
     */
    public static ProgramMeta of(ProgramId programId, LocalDateTime openAt,
                                 LocalDateTime closeAt, ProgramStatus status) {
        return new ProgramMeta(
            programId,
            openAt,
            closeAt,
            status
        );
    }

    /**
     * 스케줄 갱신. program.time.updated 이벤트 처리 시 호출.
     */
    public void updateTime(LocalDateTime newOpenAt, LocalDateTime newCloseAt) {
        this.openAt = newOpenAt;
        this.closeAt = newCloseAt;
    }

    /**
     * 프로그램 취소 처리. program.cancelled 이벤트 처리 시 호출.
     */
    public void cancel() {
        this.status = ProgramStatus.CANCELLED;
    }

    /**
     * 현재 시각 기준 활성 여부.
     * CANCELLED 가 아니고, 스케줄이 설정됐고, 현재 시각이 openAt ~ closeAt 사이일 때 true.
     */
    public boolean isActiveAt(LocalDateTime now) {
        if (status == ProgramStatus.CANCELLED) return false;
        if (openAt == null || closeAt == null) return false;
        return !openAt.isAfter(now) && !closeAt.isBefore(now);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgramMeta that)) return false;
        return programId.equals(that.programId);
    }

    @Override
    public int hashCode() {
        return programId.hashCode();
    }
}
