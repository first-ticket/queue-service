package com.firstticket.queueservice.queuetoken.domain;

import com.firstticket.queueservice.queuetoken.domain.exception.InvalidTokenStateException;
import com.firstticket.queueservice.queuetoken.domain.exception.QueueErrorCode;
import com.firstticket.queueservice.queuetoken.domain.vo.ProgramId;
import com.firstticket.queueservice.queuetoken.domain.vo.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueueTokenTest {

    private static final String DUMMY_ENTRY_TOKEN = "dummy-entry-token";

    private final UserId userId = UserId.of(UUID.randomUUID());
    private final ProgramId programId = ProgramId.of(UUID.randomUUID());

    @Nested
    @DisplayName("발급(issue)")
    class Issue {

        @Test
        @DisplayName("발급된 토큰의 초기 상태는 WAITING이다")
        void 발급_시_WAITING_상태() {
            QueueToken token = QueueToken.issue(userId, programId);

            assertThat(token.getStatus()).isEqualTo(TokenStatus.WAITING);
        }

        @Test
        @DisplayName("발급 시 entryToken은 null 이다")
        void 발급_시_entryToken_null() {
            QueueToken token = QueueToken.issue(userId, programId);

            assertThat(token.getEntryToken()).isNull();
        }

        @Test
        @DisplayName("발급 시 ID가 자동 생성된다")
        void 발급_시_ID_자동_생성() {
            QueueToken token1 = QueueToken.issue(userId, programId);
            QueueToken token2 = QueueToken.issue(userId, programId);

            assertThat(token1.getId()).isNotEqualTo(token2.getId());
        }

        @Test
        @DisplayName("발급 시 IssuedAt이 현재 시각으로 설정된다")
        void 발급_시_IssuedAt_설정() {
            QueueToken token = QueueToken.issue(userId, programId);

            assertThat(token.getIssuedAt()).isNotNull();
        }

        @Test
        @DisplayName("UserId가 null이면 예외가 발생한다")
        void userId_null_예외() {
            assertThatThrownBy(() -> QueueToken.issue(null, programId))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("ProgramId가 null이면 예외가 발생한다")
        void programId_null_예외() {
            assertThatThrownBy(() -> QueueToken.issue(userId, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("입장 승인(admit)")
    class Admit {

        @Test
        @DisplayName("WAITING 상태에서 admit 호출 시 ADMITTED로 전이된다")
        void waiting에서_admit_성공() {
            QueueToken token = QueueToken.issue(userId, programId);

            token.admit(DUMMY_ENTRY_TOKEN);

            assertThat(token.getStatus()).isEqualTo(TokenStatus.ADMITTED);
        }

        @Test
        @DisplayName("admit 시 entryToken이 저장된다")
        void admit_시_entryToken_저장() {
            QueueToken token = QueueToken.issue(userId, programId);

            token.admit(DUMMY_ENTRY_TOKEN);

            assertThat(token.getEntryToken()).isEqualTo(DUMMY_ENTRY_TOKEN);
        }

        @Test
        @DisplayName("이미 ADMITTED 상태에서 admit 시 예외가 발생한다")
        void admitted에서_admit_시_예외() {
            QueueToken token = QueueToken.issue(userId, programId);
            token.admit(DUMMY_ENTRY_TOKEN);

            assertThatThrownBy(() -> token.admit(DUMMY_ENTRY_TOKEN))
                    .isInstanceOf(InvalidTokenStateException.class)
                    .extracting("errorCode")
                    .isEqualTo(QueueErrorCode.INVALID_TOKEN_STATE);
        }

        @Test
        @DisplayName("CANCELLED 상태에서 admit 시 예외가 발생한다")
        void cancelled에서_admit_시_예외() {
            QueueToken token = QueueToken.issue(userId, programId);
            token.cancel();

            assertThatThrownBy(() -> token.admit(DUMMY_ENTRY_TOKEN))
                    .isInstanceOf(InvalidTokenStateException.class);
        }

        @Test
        @DisplayName("EXPIRED 상태에서 admit 시 예외가 발생한다")
        void expired에서_admit_시_예외() {
            QueueToken token = QueueToken.issue(userId, programId);
            token.expire();

            assertThatThrownBy(() -> token.admit(DUMMY_ENTRY_TOKEN))
                    .isInstanceOf(InvalidTokenStateException.class);
        }
    }

    @Nested
    @DisplayName("취소(cancel)")
    class Cancel {

        @Test
        @DisplayName("WAITING 상태에서 cancel 호출 시 CANCELLED로 전이된다")
        void waiting에서_cancel_성공() {
            QueueToken token = QueueToken.issue(userId, programId);

            token.cancel();

            assertThat(token.getStatus()).isEqualTo(TokenStatus.CANCELLED);
        }

        @Test
        @DisplayName("ADMITTED 상태에서 cancel 시 예외가 발생한다")
        void admitted에서_cancel_시_예외() {
            QueueToken token = QueueToken.issue(userId, programId);
            token.admit(DUMMY_ENTRY_TOKEN);

            assertThatThrownBy(token::cancel)
                    .isInstanceOf(InvalidTokenStateException.class);
        }
    }

    @Nested
    @DisplayName("만료(expire)")
    class Expire {

        @Test
        @DisplayName("WAITING 상태에서 expire 호출 시 EXPIRED로 전이된다")
        void waiting에서_expire_성공() {
            QueueToken token = QueueToken.issue(userId, programId);

            token.expire();

            assertThat(token.getStatus()).isEqualTo(TokenStatus.EXPIRED);
        }

        @Test
        @DisplayName("CANCELLED 상태에서 expire 시 예외가 발생한다")
        void cancelled에서_expire_시_예외() {
            QueueToken token = QueueToken.issue(userId, programId);
            token.cancel();

            assertThatThrownBy(token::expire)
                    .isInstanceOf(InvalidTokenStateException.class);
        }
    }

    @Nested
    @DisplayName("복원(restore)")
    class Restore {

        @Test
        @DisplayName("ADMITTED 토큰을 entryToken과 함께 복원한다")
        void ADMITTED_토큰_복원() {
            QueueToken issued = QueueToken.issue(userId, programId);
            issued.admit(DUMMY_ENTRY_TOKEN);

            QueueToken restored = QueueToken.restore(
                    issued.getId(),
                    issued.getUserId(),
                    issued.getProgramId(),
                    issued.getIssuedAt(),
                    issued.getStatus(),
                    issued.getEntryToken()
            );

            assertThat(restored.getId()).isEqualTo(issued.getId());
            assertThat(restored.getUserId()).isEqualTo(issued.getUserId());
            assertThat(restored.getProgramId()).isEqualTo(issued.getProgramId());
            assertThat(restored.getIssuedAt()).isEqualTo(issued.getIssuedAt());
            assertThat(restored.getStatus()).isEqualTo(TokenStatus.ADMITTED);
            assertThat(restored.getEntryToken()).isEqualTo(DUMMY_ENTRY_TOKEN);
        }

        @Test
        @DisplayName("WAITING 토큰은 entryToken null 로 복원된다")
        void WAITING_토큰_복원() {
            QueueToken issued = QueueToken.issue(userId, programId);

            QueueToken restored = QueueToken.restore(
                    issued.getId(),
                    issued.getUserId(),
                    issued.getProgramId(),
                    issued.getIssuedAt(),
                    issued.getStatus(),
                    null
            );

            assertThat(restored.getStatus()).isEqualTo(TokenStatus.WAITING);
            assertThat(restored.getEntryToken()).isNull();
        }
    }
}
