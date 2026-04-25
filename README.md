# queue-service

First Ticket 프로젝트의 대기열 서비스.  
티켓 오픈 시 트래픽 폭증을 제어하기 위한 대기열 시스템을 제공한다.

---

## 🛠 기술 스택

| 항목 | 기술 |
| --- | --- |
| 언어 | Java 21 |
| 프레임워크 | Spring Boot 3.5.13 |
| Spring Cloud | 2025.0.2 |
| 빌드 도구 | Gradle |
| 저장소 | Redis |
| 서비스 디스커버리 | Eureka |
| 설정 관리 | Spring Cloud Config |

---


## 🚀 로컬 실행

### 사전 조건

다음 인프라가 먼저 실행되어 있어야 한다.

1. **infra 레포의 docker-compose** — Redis, PostgreSQL, Keycloak 등
2. **config-server** — 설정 관리 서버
3. **eureka-server** — 서비스 디스커버리

### 실행 순서

```bash
# 1. infra 기동
cd ../infra
docker-compose -f docker/docker-compose.yml --env-file .env up -d

# 2. config-server 기동
cd ../config-server
./gradlew bootRun

# 3. eureka-server 기동
cd ../eureka-server
./gradlew bootRun

# 4. queue-service 기동
cd ../queue-service
./gradlew bootRun
```

또는 IntelliJ에서 각 서비스를 순서대로 실행한다.

### 환경변수 설정

queue-service 실행 시 다음 환경변수가 필요하다.

```bash
CONFIG_SERVER_USERNAME=admin
CONFIG_SERVER_PASSWORD=admin1234
SPRING_PROFILES_ACTIVE=local        # 선택, 기본값: local
CONFIG_SERVER_URL=http://localhost:8888   # 선택, 기본값
```

`.env.example` 파일을 참고하여 `.env` 또는 IntelliJ Run Configuration에 설정한다.

---

## 🌐 포트

| 환경 | 포트 |
| --- | --- |
| local | 8085 |
| prod | 8080 (컨테이너 내부) |

---

## 🔍 헬스체크

```bash
curl http://localhost:8085/actuator/health
# → {"status":"UP"}
```

---

## 📁 패키지 구조
```text
com.firstticket.queueservice
├── domain          # 도메인 모델, VO, Enum
├── application     # 유스케이스, 서비스
├── infrastructure  # Redis, 외부 연동
└── interfaces      # REST Controller, DTO
```

---

## 🔗 관련 레포

- [infra](https://github.com/first-ticket/infra) — 도커 인프라
- [config-server](https://github.com/first-ticket/config-server) — 설정 관리 서버
- [config-repo](https://github.com/first-ticket/config-repo) — 설정 저장소
- [eureka-server](https://github.com/first-ticket/eureka-server) — 서비스 디스커버리
