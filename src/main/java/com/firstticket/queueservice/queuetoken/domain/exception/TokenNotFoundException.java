package com.firstticket.queueservice.queuetoken.domain.exception;

import com.firstticket.common.exception.BusinessException;

/**
 * 요청한 토큰을 찾을 수 없을 때 발생하는 예외.
 *
 * <p>다음 두 케이스에서 동일하게 던진다:
 * <ul>
 *   <li>토큰 자체가 존재하지 않음</li>
 *   <li>토큰은 존재하나 본인 소유가 아님</li>
 * </ul>
 *
 * <p>두 케이스를 구분하지 않는 이유는 "리소스 존재 여부" 정보 누출 방지 (보안).
 * 공격자가 tokenId 추측 공격으로 토큰 존재 여부를 알아낼 수 없게 한다.
 */
public class TokenNotFoundException extends BusinessException {

    public TokenNotFoundException() {
        super(QueueErrorCode.TOKEN_NOT_FOUND);
    }
}
