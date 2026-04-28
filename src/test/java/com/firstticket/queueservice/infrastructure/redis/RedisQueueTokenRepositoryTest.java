package com.firstticket.queueservice.infrastructure.redis;

import com.firstticket.queueservice.domain.QueueToken;
import com.firstticket.queueservice.domain.TokenStatus;
import com.firstticket.queueservice.domain.exception.DuplicateTokenException;
import com.firstticket.queueservice.domain.vo.ProgramId;
import com.firstticket.queueservice.domain.vo.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * RedisQueueTokenRepository 통합 테스트.
 *
 * <p>Testcontainers로 실제 Redis를 띄워 테스트한다.
 * 매 테스트 후 flushAll로 데이터를 초기화하여 격리를 보장한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RedisQueueTokenRepositoryTest {

    @Container
    @ServiceConnection(name = "redis")
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @Autowired
    private RedisQueueTokenRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @AfterEach
    void cleanUp() {
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Nested
    @DisplayName("enqueue")
    class Enqueue {

        @Test
        @DisplayName("새 토큰을 등록하면 조회할 수 있다")
        void 새_토큰_등록() {
            QueueToken token = newToken();

            repository.enqueue(token);

            Optional<QueueToken> found = repository.findById(token.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getStatus()).isEqualTo(TokenStatus.WAITING);
            assertThat(found.get().getUserId()).isEqualTo(token.getUserId());
            assertThat(found.get().getProgramId()).isEqualTo(token.getProgramId());
        }

        @Test
        @DisplayName("같은 사용자가 같은 프로그램에 중복 진입 시 DuplicateTokenException 발생")
        void 중복_진입_예외() {
            UserId userId = UserId.of(UUID.randomUUID());
            ProgramId programId = ProgramId.of(UUID.randomUUID());

            QueueToken first = QueueToken.issue(userId, programId);
            repository.enqueue(first);

            QueueToken second = QueueToken.issue(userId, programId);
            assertThatThrownBy(() -> repository.enqueue(second))
                .isInstanceOf(DuplicateTokenException.class);
        }

        @Test
        @DisplayName("같은 사용자라도 다른 프로그램이면 진입 가능")
        void 다른_프로그램_진입() {
            UserId userId = UserId.of(UUID.randomUUID());

            QueueToken first = QueueToken.issue(userId, ProgramId.of(UUID.randomUUID()));
            QueueToken second = QueueToken.issue(userId, ProgramId.of(UUID.randomUUID()));

            repository.enqueue(first);
            repository.enqueue(second);

            assertThat(repository.findById(first.getId())).isPresent();
            assertThat(repository.findById(second.getId())).isPresent();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("저장된 토큰의 모든 필드가 정확히 복원된다")
        void 토큰_필드_복원() {
            QueueToken token = newToken();
            repository.enqueue(token);

            QueueToken found = repository.findById(token.getId()).orElseThrow();

            assertThat(found.getId()).isEqualTo(token.getId());
            assertThat(found.getUserId()).isEqualTo(token.getUserId());
            assertThat(found.getProgramId()).isEqualTo(token.getProgramId());
            assertThat(found.getStatus()).isEqualTo(token.getStatus());
        }

        @Test
        @DisplayName("저장 안 된 ID는 빈 Optional 반환")
        void 미저장_ID() {
            Optional<QueueToken> found = repository.findById(
                com.firstticket.queueservice.domain.vo.QueueTokenId.of()
            );

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndProgramId")
    class FindByUserIdAndProgramId {

        @Test
        @DisplayName("저장된 사용자 토큰을 조회할 수 있다")
        void 저장된_토큰_조회() {
            UserId userId = UserId.of(UUID.randomUUID());
            ProgramId programId = ProgramId.of(UUID.randomUUID());
            QueueToken token = QueueToken.issue(userId, programId);
            repository.enqueue(token);

            Optional<QueueToken> found = repository.findByUserIdAndProgramId(userId, programId);

            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(token.getId());
        }

        @Test
        @DisplayName("저장 안 된 사용자는 빈 Optional 반환")
        void 미저장_사용자() {
            Optional<QueueToken> found = repository.findByUserIdAndProgramId(
                UserId.of(UUID.randomUUID()),
                ProgramId.of(UUID.randomUUID())
            );

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findRank")
    class FindRank {

        @Test
        @DisplayName("첫 번째 진입자의 순번은 1이다")
        void 첫_번째_진입자() {
            ProgramId programId = ProgramId.of(UUID.randomUUID());
            UserId userId = UserId.of(UUID.randomUUID());

            QueueToken token = QueueToken.issue(userId, programId);
            repository.enqueue(token);

            Optional<Long> rank = repository.findRank(userId, programId);

            assertThat(rank).isPresent();
            assertThat(rank.get()).isEqualTo(1L);
        }

        @Test
        @DisplayName("두 번째 진입자의 순번은 2이다")
        void 두_번째_진입자() throws InterruptedException {
            ProgramId programId = ProgramId.of(UUID.randomUUID());

            QueueToken first = QueueToken.issue(UserId.of(UUID.randomUUID()), programId);
            repository.enqueue(first);

            // epoch milli 차이 보장 (Sorted Set score 충돌 방지)
            Thread.sleep(2);

            UserId secondUser = UserId.of(UUID.randomUUID());
            QueueToken second = QueueToken.issue(secondUser, programId);
            repository.enqueue(second);

            Optional<Long> rank = repository.findRank(secondUser, programId);

            assertThat(rank).isPresent();
            assertThat(rank.get()).isEqualTo(2L);
        }

        @Test
        @DisplayName("미진입 사용자는 빈 Optional 반환")
        void 미진입_사용자() {
            Optional<Long> rank = repository.findRank(
                UserId.of(UUID.randomUUID()),
                ProgramId.of(UUID.randomUUID())
            );

            assertThat(rank).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("삭제하면 모든 키에서 조회되지 않는다")
        void 삭제_후_조회_불가() {
            QueueToken token = newToken();
            repository.enqueue(token);

            repository.delete(token);

            assertThat(repository.findById(token.getId())).isEmpty();
            assertThat(repository.findByUserIdAndProgramId(
                token.getUserId(), token.getProgramId())).isEmpty();
            assertThat(repository.findRank(
                token.getUserId(), token.getProgramId())).isEmpty();
        }

        @Test
        @DisplayName("삭제 후 같은 사용자가 재진입 가능 (DuplicateTokenException X)")
        void 삭제_후_재진입() {
            UserId userId = UserId.of(UUID.randomUUID());
            ProgramId programId = ProgramId.of(UUID.randomUUID());

            QueueToken first = QueueToken.issue(userId, programId);
            repository.enqueue(first);
            repository.delete(first);

            QueueToken second = QueueToken.issue(userId, programId);
            repository.enqueue(second);   // 예외 발생 안 해야 함

            assertThat(repository.findByUserIdAndProgramId(userId, programId))
                .isPresent();
        }

        @Test
        @DisplayName("이미 없는 토큰을 삭제해도 예외 없음 (멱등)")
        void 멱등성() {
            QueueToken token = newToken();

            // enqueue 없이 바로 delete
            repository.delete(token);   // 예외 없어야 함
        }
    }

    @Nested
    @DisplayName("findAdmissionCandidates")
    class FindAdmissionCandidates {

        @Test
        @DisplayName("앞에서 batchSize명을 진입 시각 순으로 반환")
        void 앞_N명_조회() throws InterruptedException {
            ProgramId programId = ProgramId.of(UUID.randomUUID());

            QueueToken first = QueueToken.issue(UserId.of(UUID.randomUUID()), programId);
            repository.enqueue(first);
            Thread.sleep(2);

            QueueToken second = QueueToken.issue(UserId.of(UUID.randomUUID()), programId);
            repository.enqueue(second);
            Thread.sleep(2);

            QueueToken third = QueueToken.issue(UserId.of(UUID.randomUUID()), programId);
            repository.enqueue(third);

            List<QueueToken> candidates = repository.findAdmissionCandidates(programId, 2);

            assertThat(candidates).hasSize(2);
            assertThat(candidates.get(0).getId()).isEqualTo(first.getId());
            assertThat(candidates.get(1).getId()).isEqualTo(second.getId());
        }

        @Test
        @DisplayName("토큰이 없으면 빈 리스트 반환")
        void 빈_프로그램() {
            List<QueueToken> candidates = repository.findAdmissionCandidates(
                ProgramId.of(UUID.randomUUID()), 10);

            assertThat(candidates).isEmpty();
        }

        @Test
        @DisplayName("batchSize가 실제 인원보다 크면 있는 만큼 반환")
        void 적은_인원() {
            ProgramId programId = ProgramId.of(UUID.randomUUID());

            QueueToken token = QueueToken.issue(UserId.of(UUID.randomUUID()), programId);
            repository.enqueue(token);

            List<QueueToken> candidates = repository.findAdmissionCandidates(programId, 100);

            assertThat(candidates).hasSize(1);
        }
    }

    private QueueToken newToken() {
        return QueueToken.issue(
            UserId.of(UUID.randomUUID()),
            ProgramId.of(UUID.randomUUID())
        );
    }
}
