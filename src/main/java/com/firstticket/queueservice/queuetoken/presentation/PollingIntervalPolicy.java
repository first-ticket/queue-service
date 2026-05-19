package com.firstticket.queueservice.queuetoken.presentation;

import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 사용자 순번에 따른 폴링 간격 결정 정책.
 *
 * <p>Server-driven Adaptive Polling 의 핵심 컴포넌트.
 * 응답에 포함될 {@code retryAfterMs} 값을 계산한다.
 *
 * <h3>정책</h3>
 * <ul>
 *   <li>임박 (≤100명): 1초 (jitter 없음, 빠른 알림 우선)</li>
 *   <li>중간 (≤1000명): 5초 ± 300ms</li>
 *   <li>멀리 (≤10000명): 15초 ± 500ms</li>
 *   <li>매우 멀리 (그 이상): 30초 ± 4초</li>
 * </ul>
 *
 * <p>Jitter 적용으로 같은 구간 사용자들의 동시 요청 분산 (Thundering Herd 방지).
 */
@Component
public class PollingIntervalPolicy {

    private static final int IMMINENT_THRESHOLD = 100;
    private static final int NEAR_THRESHOLD = 1000;
    private static final int FAR_THRESHOLD = 10000;

    private static final int IMMINENT_INTERVAL_MS = 1000;
    private static final int NEAR_INTERVAL_MS = 5000;
    private static final int FAR_INTERVAL_MS = 15000;
    private static final int VERY_FAR_INTERVAL_MS = 30000;

    private static final int NEAR_JITTER_MS = 300;
    private static final int FAR_JITTER_MS = 500;
    private static final int VERY_FAR_JITTER_MS = 4000;

    /**
     * 사용자 순번에 따른 다음 폴링 간격을 계산한다.
     *
     * @param position 1-based 순번. {@code null} 이면 큐에서 빠진 상태 (ADMITTED 등).
     * @return 다음 폴링까지 대기 시간 (ms). {@code null} 이면 폴링 불필요.
     */
    public Integer nextRetryAfterMs(Long position) {
        if (position == null) {
            return null;
        }

        int interval;
        int jitter;

        if (position <= IMMINENT_THRESHOLD) {
            interval = IMMINENT_INTERVAL_MS;
            jitter = 0;
        } else if (position <= NEAR_THRESHOLD) {
            interval = NEAR_INTERVAL_MS;
            jitter = NEAR_JITTER_MS;
        } else if (position <= FAR_THRESHOLD) {
            interval = FAR_INTERVAL_MS;
            jitter = FAR_JITTER_MS;
        } else {
            interval = VERY_FAR_INTERVAL_MS;
            jitter = VERY_FAR_JITTER_MS;
        }

        int jitterValue = jitter > 0
            ? ThreadLocalRandom.current().nextInt(-jitter, jitter + 1)
            : 0;

        return interval + jitterValue;
    }

}
