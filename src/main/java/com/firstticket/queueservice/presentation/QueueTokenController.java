package com.firstticket.queueservice.presentation;

import com.firstticket.common.response.ApiResponse;
import com.firstticket.common.web.AuthContext;
import com.firstticket.queueservice.application.QueueTokenService;
import com.firstticket.queueservice.application.dto.CancelQueueTokenCommand;
import com.firstticket.queueservice.application.dto.GetQueueTokenQuery;
import com.firstticket.queueservice.application.dto.IssueQueueTokenCommand;
import com.firstticket.queueservice.application.dto.QueueTokenResult;
import com.firstticket.queueservice.presentation.dto.QueueTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * 대기열 진입 / 조회 / 취소를 처리하는 REST 컨트롤러.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/queues/programs/{programId}")
public class QueueTokenController {

    private final QueueTokenService queueTokenService;

    /**
     * 대기열 진입.
     *
     * @return 201 Created
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QueueTokenResponse>> issueToken(
        @PathVariable UUID programId
    ) {
        IssueQueueTokenCommand command = IssueQueueTokenCommand.of(
            AuthContext.getUserId(),
            programId
        );
        QueueTokenResult result = queueTokenService.issueToken(command);
        return ApiResponse.success(
            QueueSuccessCode.QUEUE_TOKEN_ISSUED,
            QueueTokenResponse.from(result)
        );
    }

    /**
     * 대기 정보 조회 (폴링용).
     *
     * @return 200 OK
     */
    @GetMapping
    public ResponseEntity<ApiResponse<QueueTokenResponse>> getToken(
        @PathVariable UUID programId
    ) {
        GetQueueTokenQuery query = GetQueueTokenQuery.of(AuthContext.getUserId(), programId);
        QueueTokenResult result = queueTokenService.getToken(query);
        return ApiResponse.success(
            QueueSuccessCode.QUEUE_TOKEN_FOUND,
            QueueTokenResponse.from(result));
    }

    /**
     * 대기 취소.
     *
     * @return 204 No Content
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> cancelToken(
        @PathVariable UUID programId
    ) {
        CancelQueueTokenCommand command = CancelQueueTokenCommand.of(
            AuthContext.getUserId(),
            programId
        );
        queueTokenService.cancelToken(command);
        return ApiResponse.success(QueueSuccessCode.QUEUE_TOKEN_CANCELLED);
    }
}
