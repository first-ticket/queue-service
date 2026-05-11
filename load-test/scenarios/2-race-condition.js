import http from 'k6/http';
import { check } from 'k6';

export const options = {
    // 100 VUs 가 동시에 같은 user / program 으로 호출
    vus: 100,
    iterations: 100,
};

const BASE_URL = 'http://localhost:8085';
const PROGRAM_ID = '550e8400-e29b-41d4-a716-446655440000';
const SAME_USER_ID = '11111111-1111-1111-1111-111111111111';  // ← 고정!

export default function () {
    const res = http.post(
        `${BASE_URL}/api/v1/queues/programs/${PROGRAM_ID}`,
        null,
        {
            headers: {
                'Authorization': 'Bearer dummy',
                'X-User-Id': SAME_USER_ID,
            },
        }
    );

    check(res, {
        'is 201 or 409': (r) => r.status === 201 || r.status === 409,
    });
}
