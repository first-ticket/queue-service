// k6 의 HTTP 모듈 - HTTP 요청 보내기
import http from 'k6/http';
// k6 의 check 함수 - 응답 검증 (테스트 단언)
import { check } from 'k6';
// UUID 생성 라이브러리 (k6 외부 모듈, jslib.k6.io 에서 자동 다운로드)
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

// k6 의 부하 설정
export const options = {
    // 가상 사용자 수 (동시 스레드)
    vus: 10,

    // // 총 반복 횟수 (10 사용자가 나눠서 100 번 호출)
    // iterations: 10000,

    duration: '15s',
};


// 부하 대상 서버
const BASE_URL = 'http://localhost:8085';

// 테스트할 프로그램 ID (고정값으로 모든 사용자가 같은 프로그램에 진입)
const PROGRAM_ID = '550e8400-e29b-41d4-a716-446655440000';

// k6 의 메인 함수 - 각 가상 사용자가 반복 실행하는 코드
export default function () {
    // 각 호출마다 새 사용자 ID 생성 (다른 user 동시 진입 시나리오)
    const userId = uuidv4();

    // POST /api/v1/queues/programs/{programId} - 대기열 진입 API 호출
    const res = http.post(
        `${BASE_URL}/api/v1/queues/programs/${PROGRAM_ID}`,
        null,                                       // request body 없음
        {
            headers: {
                'Authorization': 'Bearer dummy',        // JWT 인증 헤더 (더미 값)
                'X-User-Id': userId,                    // 사용자 ID 헤더
            },
        }
    );

    // 응답 검증 - 201 Created 인지 확인
    // 실패 시 k6 의 'checks' 메트릭에 기록 (단 테스트 자체 중단 X)
    check(res, {
        'status is 201': (r) => r.status === 201,
    });
}
