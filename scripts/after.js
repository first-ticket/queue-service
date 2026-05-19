import http from 'k6/http';
import { sleep, check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
    stages: [
        { duration: '30s', target: 3000 },
        { duration: '2m30s', target: 3000 },
        { duration: '10s', target: 0 },
    ],
};

const PROGRAM_ID = '550e8400-e29b-41d4-a716-446655440000';
const BASE_URL = 'http://localhost:8085';

const userIdsByVU = {};

export default function () {
    if (!userIdsByVU[__VU]) {
        userIdsByVU[__VU] = uuidv4();

        http.post(
            `${BASE_URL}/api/v1/queues/programs/${PROGRAM_ID}`,
            null,
            { headers: { 'X-User-Id': userIdsByVU[__VU] }, tags: { name: 'enqueue' } }
        );
    }

    const userId = userIdsByVU[__VU];

    const res = http.get(
        `${BASE_URL}/api/v1/queues/programs/${PROGRAM_ID}`,
        { headers: { 'X-User-Id': userId }, tags: { name: 'polling' } }
    );

    check(res, { 'polling 200': (r) => r.status === 200 });

    // After: retryAfterMs 따라 동적 sleep
    let sleepSec = 3;
    if (res.status === 200) {
        try {
            const body = JSON.parse(res.body);
            const retryAfterMs = body.data?.retryAfterMs;
            if (retryAfterMs != null) {
                sleepSec = retryAfterMs / 1000;
            }
        } catch (e) {
            // 파싱 실패 시 기본값
        }
    }

    sleep(sleepSec);
}
