package com.firstticket.queueservice.queuetoken.presentation;

import com.firstticket.common.exception.BusinessException;
import com.firstticket.common.exception.GlobalExceptionHandler;
import com.firstticket.common.response.CommonErrorCode;
import com.firstticket.common.web.AuthContext;
import com.firstticket.queueservice.queuetoken.application.QueueTokenService;
import com.firstticket.queueservice.queuetoken.application.dto.QueueTokenResult;
import com.firstticket.queueservice.queuetoken.domain.QueueToken;
import com.firstticket.queueservice.queuetoken.domain.exception.DuplicateTokenException;
import com.firstticket.queueservice.queuetoken.domain.exception.InvalidTokenStateException;
import com.firstticket.queueservice.queuetoken.domain.exception.TokenNotFoundException;
import com.firstticket.queueservice.queuetoken.domain.vo.ProgramId;
import com.firstticket.queueservice.queuetoken.domain.vo.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueueTokenController.class)
@AutoConfigureRestDocs
@ActiveProfiles("test")
@Import(GlobalExceptionHandler.class)
class QueueTokenControllerTest {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String DUMMY_BEARER_TOKEN = "Bearer dummy-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueueTokenService queueTokenService;

    @MockitoBean
    private PollingIntervalPolicy pollingIntervalPolicy;  // ← 추가

    // ===== 성공 케이스 =====

    @Test
    @DisplayName("대기열 진입 성공")
    void 대기열_진입_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        QueueToken token = QueueToken.issue(UserId.of(userId), ProgramId.of(programId));
        QueueTokenResult result = QueueTokenResult.of(token, 1L);

        when(queueTokenService.issueToken(any())).thenReturn(result);
        when(pollingIntervalPolicy.nextRetryAfterMs(1L)).thenReturn(1000);  // ← 추가

        try (MockedStatic<AuthContext> mocked = mockStatic(AuthContext.class)) {
            mocked.when(AuthContext::getUserId).thenReturn(userId);

            mockMvc.perform(post("/api/v1/queues/programs/{programId}", programId)
                    .header(AUTHORIZATION_HEADER, DUMMY_BEARER_TOKEN))
                .andExpect(status().isCreated())
                .andDo(document("queue-token-issue",
                    preprocessRequest(
                        prettyPrint(),
                        modifyHeaders().remove("Content-Type")
                    ),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("programId").description("프로그램 ID")
                    ),
                    requestHeaders(
                        headerWithName("Authorization")
                            .description("Bearer access token (Keycloak 발급)")
                    ),
                    responseFields(
                        fieldWithPath("success").description("요청 성공 여부"),
                        fieldWithPath("code").description("응답 코드"),
                        fieldWithPath("message").description("응답 메시지"),
                        fieldWithPath("timestamp").description("응답 시각"),
                        fieldWithPath("data.tokenId").description("발급된 토큰 ID"),
                        fieldWithPath("data.status").description("토큰 상태 (WAITING)"),
                        fieldWithPath("data.issuedAt").description("발급 시각"),
                        fieldWithPath("data.position").description("현재 순번"),
                        fieldWithPath("data.retryAfterMs").description("다음 폴링까지 권장 대기 시간 (ms). 순번에 따라 차등 적용").optional()  // ← 추가
                    )
                ));
        }
    }

    @Test
    @DisplayName("대기 정보 조회 성공")
    void 대기_정보_조회_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        QueueToken token = QueueToken.issue(UserId.of(userId), ProgramId.of(programId));
        QueueTokenResult result = QueueTokenResult.of(token, 50L);

        when(queueTokenService.getToken(any())).thenReturn(result);
        when(pollingIntervalPolicy.nextRetryAfterMs(50L)).thenReturn(1000);  // ← 추가

        try (MockedStatic<AuthContext> mocked = mockStatic(AuthContext.class)) {
            mocked.when(AuthContext::getUserId).thenReturn(userId);

            mockMvc.perform(get("/api/v1/queues/programs/{programId}", programId)
                    .header(AUTHORIZATION_HEADER, DUMMY_BEARER_TOKEN))
                .andExpect(status().isOk())
                .andDo(document("queue-token-get",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("programId").description("프로그램 ID")
                    ),
                    requestHeaders(
                        headerWithName("Authorization")
                            .description("Bearer access token (Keycloak 발급)")
                    ),
                    responseFields(
                        fieldWithPath("success").description("요청 성공 여부"),
                        fieldWithPath("code").description("응답 코드"),
                        fieldWithPath("message").description("응답 메시지"),
                        fieldWithPath("timestamp").description("응답 시각"),
                        fieldWithPath("data.tokenId").description("토큰 ID"),
                        fieldWithPath("data.status").description("토큰 상태 (WAITING / ADMITTED / EXPIRED)"),
                        fieldWithPath("data.issuedAt").description("발급 시각"),
                        fieldWithPath("data.position").description("현재 순번. ADMITTED 등 큐에서 빠진 상태면 null").optional(),
                        fieldWithPath("data.retryAfterMs").description("다음 폴링까지 권장 대기 시간 (ms). ADMITTED 상태면 null").optional()  // ← 추가
                    )
                ));
        }
    }

    @Test
    @DisplayName("대기 취소 성공")
    void 대기_취소_성공() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        doNothing().when(queueTokenService).cancelToken(any());

        try (MockedStatic<AuthContext> mocked = mockStatic(AuthContext.class)) {
            mocked.when(AuthContext::getUserId).thenReturn(userId);

            mockMvc.perform(delete("/api/v1/queues/programs/{programId}", programId)
                    .header(AUTHORIZATION_HEADER, DUMMY_BEARER_TOKEN))
                .andExpect(status().isOk())
                .andDo(document("queue-token-cancel",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("programId").description("프로그램 ID")
                    ),
                    requestHeaders(
                        headerWithName("Authorization")
                            .description("Bearer access token (Keycloak 발급)")
                    ),
                    responseFields(
                        fieldWithPath("success").description("요청 성공 여부"),
                        fieldWithPath("code").description("응답 코드 (QUEUE_TOKEN_CANCELLED)"),
                        fieldWithPath("message").description("응답 메시지"),
                        fieldWithPath("timestamp").description("응답 시각")
                    )
                ));
        }
    }

    // ===== 에러 케이스 =====
    // (변경 없음 - 에러 응답엔 data 필드 없으니 그대로)

    @Test
    @DisplayName("인증 실패 시 401 Unauthorized")
    void 인증_실패_401() throws Exception {
        // given
        UUID programId = UUID.randomUUID();

        try (MockedStatic<AuthContext> mocked = mockStatic(AuthContext.class)) {
            mocked.when(AuthContext::getUserId)
                .thenThrow(new BusinessException(CommonErrorCode.UNAUTHORIZED));

            mockMvc.perform(post("/api/v1/queues/programs/{programId}", programId))
                .andExpect(status().isUnauthorized())
                .andDo(document("queue-token-unauthorized",
                    preprocessRequest(
                        prettyPrint(),
                        modifyHeaders().remove("Content-Type")
                    ),
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("success").description("요청 성공 여부 (false)"),
                        fieldWithPath("code").description("에러 코드 (UNAUTHORIZED)"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("timestamp").description("응답 시각")
                    )
                ));
        }
    }

    @Test
    @DisplayName("동시 진입 시 race — 409 Conflict")
    void 중복_진입_409() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        when(queueTokenService.issueToken(any())).thenThrow(new DuplicateTokenException());

        try (MockedStatic<AuthContext> mocked = mockStatic(AuthContext.class)) {
            mocked.when(AuthContext::getUserId).thenReturn(userId);

            mockMvc.perform(post("/api/v1/queues/programs/{programId}", programId)
                    .header(AUTHORIZATION_HEADER, DUMMY_BEARER_TOKEN))
                .andExpect(status().isConflict())
                .andDo(document("queue-token-duplicate",
                    preprocessRequest(
                        prettyPrint(),
                        modifyHeaders().remove("Content-Type")
                    ),
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("success").description("요청 성공 여부 (false)"),
                        fieldWithPath("code").description("에러 코드 (DUPLICATE_TOKEN)"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("timestamp").description("응답 시각")
                    )
                ));
        }
    }

    @Test
    @DisplayName("토큰 없음 — 조회 시 404")
    void 토큰_없음_조회_404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        when(queueTokenService.getToken(any())).thenThrow(new TokenNotFoundException());

        try (MockedStatic<AuthContext> mocked = mockStatic(AuthContext.class)) {
            mocked.when(AuthContext::getUserId).thenReturn(userId);

            mockMvc.perform(get("/api/v1/queues/programs/{programId}", programId)
                    .header(AUTHORIZATION_HEADER, DUMMY_BEARER_TOKEN))
                .andExpect(status().isNotFound())
                .andDo(document("queue-token-get-not-found",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("success").description("요청 성공 여부 (false)"),
                        fieldWithPath("code").description("에러 코드 (TOKEN_NOT_FOUND)"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("timestamp").description("응답 시각")
                    )
                ));
        }
    }

    @Test
    @DisplayName("토큰 없음 — 취소 시 404")
    void 토큰_없음_취소_404() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        doThrow(new TokenNotFoundException()).when(queueTokenService).cancelToken(any());

        try (MockedStatic<AuthContext> mocked = mockStatic(AuthContext.class)) {
            mocked.when(AuthContext::getUserId).thenReturn(userId);

            mockMvc.perform(delete("/api/v1/queues/programs/{programId}", programId)
                    .header(AUTHORIZATION_HEADER, DUMMY_BEARER_TOKEN))
                .andExpect(status().isNotFound())
                .andDo(document("queue-token-cancel-not-found",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("success").description("요청 성공 여부 (false)"),
                        fieldWithPath("code").description("에러 코드 (TOKEN_NOT_FOUND)"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("timestamp").description("응답 시각")
                    )
                ));
        }
    }

    @Test
    @DisplayName("WAITING 이 아닌 상태 취소 시도 — 400")
    void 취소_불가_상태_400() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();

        doThrow(new InvalidTokenStateException()).when(queueTokenService).cancelToken(any());

        try (MockedStatic<AuthContext> mocked = mockStatic(AuthContext.class)) {
            mocked.when(AuthContext::getUserId).thenReturn(userId);

            mockMvc.perform(delete("/api/v1/queues/programs/{programId}", programId)
                    .header(AUTHORIZATION_HEADER, DUMMY_BEARER_TOKEN))
                .andExpect(status().isBadRequest())
                .andDo(document("queue-token-cancel-invalid-state",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    responseFields(
                        fieldWithPath("success").description("요청 성공 여부 (false)"),
                        fieldWithPath("code").description("에러 코드 (INVALID_TOKEN_STATE)"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("timestamp").description("응답 시각")
                    )
                ));
        }
    }

    @Test
    @DisplayName("ADMITTED 상태 토큰 조회 시 entryToken 응답에 포함")
    void ADMITTED_조회_entryToken_포함() throws Exception {
        // given
        UUID userId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        QueueToken token = QueueToken.issue(UserId.of(userId), ProgramId.of(programId));
        String entryToken = "eyJhbGc.dummy.jwt";
        token.admit(entryToken);
        QueueTokenResult result = QueueTokenResult.of(token, null);   // position null

        when(queueTokenService.getToken(any())).thenReturn(result);
        when(pollingIntervalPolicy.nextRetryAfterMs(null)).thenReturn(null);  // ← 추가 (ADMITTED니까 null)

        try (MockedStatic<AuthContext> mocked = mockStatic(AuthContext.class)) {
            mocked.when(AuthContext::getUserId).thenReturn(userId);

            mockMvc.perform(get("/api/v1/queues/programs/{programId}", programId)
                    .header(AUTHORIZATION_HEADER, DUMMY_BEARER_TOKEN))
                .andExpect(status().isOk())
                .andDo(document("queue-token-get-admitted",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    pathParameters(
                        parameterWithName("programId").description("프로그램 ID")
                    ),
                    requestHeaders(
                        headerWithName("Authorization")
                            .description("Bearer access token (Keycloak 발급)")
                    ),
                    responseFields(
                        fieldWithPath("success").description("요청 성공 여부"),
                        fieldWithPath("code").description("응답 코드"),
                        fieldWithPath("message").description("응답 메시지"),
                        fieldWithPath("timestamp").description("응답 시각"),
                        fieldWithPath("data.tokenId").description("토큰 ID"),
                        fieldWithPath("data.status").description("토큰 상태 (ADMITTED)"),
                        fieldWithPath("data.issuedAt").description("발급 시각"),
                        fieldWithPath("data.entryToken").description("입장 토큰 (JWT) — ADMITTED 상태일 때만 포함")
                        // retryAfterMs 는 null 이라 응답에 없음 → 명세 안 함
                    )
                ));
        }
    }
}
