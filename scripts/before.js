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

// 각 VU 마다 고유 userId 유지
const userIdsByVU = {};

export default function () {
    // 이 VU 의 userId (없으면 새로 만들고 enqueue)
    if (!userIdsByVU[__VU]) {
        userIdsByVU[__VU] = uuidv4();

        // enqueue (한 번만)
        http.post(
            `${BASE_URL}/api/v1/queues/programs/${PROGRAM_ID}`,
            null,
            { headers: { 'X-User-Id': userIdsByVU[__VU] }, tags: { name: 'enqueue' } }
        );
    }

    const userId = userIdsByVU[__VU];

    // Polling
    const res = http.get(
        `${BASE_URL}/api/v1/queues/programs/${PROGRAM_ID}`,
        { headers: { 'X-User-Id': userId }, tags: { name: 'polling' } }
    );

    check(res, { 'polling 200': (r) => r.status === 200 });

    sleep(3);  // Before: 3초 고정
}
