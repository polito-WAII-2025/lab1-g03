# BUILDER
FROM openjdk:22-jdk-slim AS builder

# Setup working dir
WORKDIR /app

# Setup gradle
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN apt-get update && apt-get install -y dos2unix
RUN dos2unix gradlew && chmod +x gradlew

RUN ./gradlew dependencies --no-daemon
COPY src ./src

RUN ./gradlew shadowJar --no-daemon

# RUNNER
FROM openjdk:22-jdk-slim

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

CMD ["java", "-jar", "app.jar"]
