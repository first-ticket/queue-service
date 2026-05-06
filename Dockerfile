# syntax=docker/dockerfile:1.7

# ===== Stage 1: Build =====
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# 빌드 설정 파일만 먼저 복사 (의존성 레이어 캐시 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew

# 의존성만 먼저 다운로드 (소스 변경 시에도 이 레이어는 캐시됨)
RUN ./gradlew bootJar -x test --no-daemon || true

# 소스 복사 후 빌드
COPY src src
RUN ./gradlew clean bootJar --no-daemon -x test

# 빌드된 jar에서 layer 추출
RUN java -Djarmode=layertools -jar build/libs/*.jar extract


# ===== Stage 2: Runtime =====
FROM eclipse-temurin:21-jre-jammy

# 비-root 유저 생성
RUN useradd -ms /bin/bash spring

WORKDIR /app

# layered jar 복사 (변경 빈도 낮은 순서대로)
COPY --from=builder --chown=spring:spring /app/dependencies/ ./
COPY --from=builder --chown=spring:spring /app/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /app/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /app/application/ ./

USER spring

EXPOSE 8085

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
