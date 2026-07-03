# 빌드 스테이지 (Gradle 및 JDK 25 환경)
FROM gradle:jdk25 AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test -x installGitHooks --no-daemon

# 실행 스테이지 (런타임용 25 환경)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java" , "-jar" , "app.jar" ]
