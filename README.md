# queue-service

First Ticket 프로젝트의 대기열 서비스.  
티켓 오픈 시 트래픽 폭증을 제어하기 위한 대기열 시스템을 제공한다.

---

## 📌 핵심 기능

티켓 예매가 열리면 트래픽이 순간적으로 폭증하여 예매 시스템에 과부하가 발생한다.  
queue-service 는 사용자를 대기열에 줄 세우고 일정 인원씩만 입장을 승인하여 트래픽을 제어한다.

### 주요 흐름

1. 사용자가 큐 진입 → 대기 토큰 발급 + 순번 부여 + Redis Hash 에 토큰 정보 저장
2. 클라이언트가 폴링으로 자기 순번 조회 (서버가 권장 폴링 간격을 동적으로 응답)
3. `AdmissionScheduler`가 앞 순번 `batchSize` 명을 입장 승인
4. 입장 승인된 사용자에게 JWT 입장 토큰 발급
5. 사용자는 입장 토큰을 가지고 예매 서비스로 진입

### 외부 이벤트 연동

program-service 의 이벤트를 수신하여 ProgramMeta (Read Model) 를 유지한다:

| 이벤트 | 처리 |
| --- | --- |
| `program.created` | 대기열 활성 정보 저장 |
| `program.time.updated` | 활성 시간 갱신 |
| `program.cancelled` | 대기열 정리 |

---

## 🛠 기술 스택

| 항목 | 기술 |
| --- | --- |
| 저장소 | Redis (대기 토큰, ProgramMeta) + PostgreSQL (Kafka Inbox) |
| 메시지 브로커 | Apache Kafka |
| JWT | jjwt 0.12.6 |
| 부하 테스트 | nGrinder, K6 |

공통 기술 스택은 [공통 README](https://github.com/first-ticket/.github/blob/main/profile/README.md) 참고.

---

## 📁 패키지 구조

DDD + 헥사고날 아키텍처 기반

```text
com.firstticket.queueservice
├── queuetoken/                  # QueueToken Aggregate (대기 토큰 관리)
│   ├── domain/                  # 도메인 모델, VO, Enum, Exception, Repository 인터페이스
│   ├── application/             # 대기 토큰 관리 서비스, Command/Query/Result DTO
│   ├── config/                  # Aggregate 전용 설정
│   ├── infrastructure/          # Redis Repository, Lua Script, JWT 발급기, 스케줄러
│   └── presentation/            # REST Controller, Response DTO, Polling 정책
├── programmeta/                 # ProgramMeta Aggregate (외부 이벤트 Read Model)
│   ├── domain/                  # 도메인 모델, VO, Enum, Exception, Repository 인터페이스, 도메인 이벤트 포트
│   ├── application/             # 이벤트 처리 서비스, Command Dto
│   └── infrastructure/          # Kafka Consumer, Redis Repository, 도메인 이벤트 어댑터
├── shared/                      # Aggregate 간 공유 자원
└── config/                      # 전역 설정
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

1. **infra 레포의 docker-compose** — Redis, kafka, PostgreSQL, Zipkin
2. **eureka-server** — 서비스 디스커버리
3. **config-server** — 설정 관리 서버
4. **program-service** — Kafka 이벤트 발행

### 환경변수 설정

```bash
CONFIG_SERVER_USERNAME=
CONFIG_SERVER_PASSWORD=
GITHUB_USER=
GITHUB_TOKEN=
```

`.env.example` 파일을 참고하여 `.env` 를 작성한다.

### 외부 설정

설정은 [config-repo](https://github.com/first-ticket/config-repo) 에서 관리한다.

**공통 (`application.yml`)**
- Redis / PostgreSQL / Kafka 연결 정보
- Zipkin, Prometheus
- Kafka Producer / Consumer 기본값 (idempotence, retries 등)

**queue-service 전용 (`queue-service.yml`)**
- 큐 정책: `queue.token.waiting-ttl`, `queue.token.admission-batch-size`
- JWT: `queue.jwt.secret`, `queue.jwt.entry-token-ttl`
- Kafka 토픽 매핑: `kafka.topics.*`
- Inbox 활성화: `messaging.inbox.enabled`
- 스키마: `queue_schema` (JPA + Flyway)

**환경별 (`queue-service-{profile}.yml`)**
- 포트, JWT TTL 등 환경 의존 설정

---

## 🔍 헬스체크

```bash
curl http://localhost:8085/actuator/health
# → {"status":"UP"}
```
