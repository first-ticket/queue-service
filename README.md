# queue-service

First Ticket 프로젝트의 대기열 서비스.  
티켓 오픈 시 트래픽 폭증을 제어하기 위한 대기열 시스템을 제공한다.

---

## 📌 핵심 기능

티켓 예매가 열리면 사용자들이 동시에 몰리면서 예매 시스템이 부담이 간다.
queue-service 는 사용자를 대기열에 줄 세우고 일정 인원씩만 입장을 승인하여 트래픽을 제어한다.

### 주요 흐름

1. 사용자가 큐 진입 (대기 토큰 발급)
2. 클라이언트가 폴링으로 자기 순번 조회
3. 스케줄러가 일정 주기로 큐 앞에서 batchSize 명을 입장 승인
4. 입장 승인된 사용자에게 JWT 입장 토큰 발급
5. 사용자는 입장 토큰을 가지고 예매 서비스로 진입

---

## 🛠 기술 스택

| 항목 | 기술 |
| --- | --- |
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.5.13 |
| Spring Cloud | 2025.0.2 |
| 빌드 도구 | Gradle |
| 저장소 | Redis |
| JWT | jjwt 0.12.6 |
| 서비스 디스커버리 | Eureka |
| 설정 관리 | Spring Cloud Config |
| API 문서 | Spring REST Docs (AsciiDoc) |

---

## 📁 패키지 구조

DDD + 헥사고날 아키텍처 기반.

```text
com.firstticket.queueservice
├── domain              # 도메인 모델, VO, Enum, Repository 인터페이스
├── application         # 유스케이스 (Service), Command / Query / Result DTO
├── infrastructure      # Redis Repository, JWT 발급기, 스케줄러
└── presentation        # REST Controller, Response DTO
```

---

## 🌐 API 엔드포인트

| Method | Path | 설명 |
| --- | --- | --- |
| POST | `/api/v1/queues/programs/{programId}` | 대기열 진입 (대기 토큰 발급) |
| GET | `/api/v1/queues/programs/{programId}` | 대기 / 입장 상태 조회 (폴링) |
| DELETE | `/api/v1/queues/programs/{programId}` | 대기 취소 |

상세한 요청 / 응답 예시는 REST Docs 참조: `http://localhost:8085/docs/index.html`

---

## 🌐 포트

| 환경 | 포트 |
| --- | --- |
| local | 8085 |
| prod | 8080 (컨테이너 내부) |

---

## 🚀 로컬 실행

### 사전 조건

다음 인프라가 먼저 실행되어 있어야 한다.

1. **infra 레포의 docker-compose** — Redis
2. **config-server** — 설정 관리 서버
3. **eureka-server** — 서비스 디스커버리

### 환경변수 설정

queue-service 실행 시 다음 환경변수가 필요하다.

```bash
CONFIG_SERVER_USERNAME=
CONFIG_SERVER_PASSWORD=
GITHUB_USER=
GITHUB_TOKEN=
```

`.env.example` 파일을 참고하여 `.env` 를 작성한다.

### 외부 설정

JWT 비밀키 / 대기 토큰 TTL / 입장 승인 batchSize 등 운영 정책은 config-repo 의
`queue-service.yml` 에서 관리된다.

---

## 🔍 헬스체크

```bash
curl http://localhost:8085/actuator/health
# → {"status":"UP"}
```

---

## 🔗 관련 레포

- [infra](https://github.com/first-ticket/infra) — 도커 인프라
- [config-server](https://github.com/first-ticket/config-server) — 설정 관리 서버
- [config-repo](https://github.com/first-ticket/config-repo) — 설정 저장소
- [eureka-server](https://github.com/first-ticket/eureka-server) — 서비스 디스커버리
