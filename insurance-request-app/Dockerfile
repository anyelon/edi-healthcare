# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy Gradle wrapper and config first (layer caching)
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./

# Copy module build files
COPY common/build.gradle common/build.gradle
COPY claims-app/build.gradle claims-app/build.gradle
COPY insurance-request-app/build.gradle insurance-request-app/build.gradle
COPY insurance-response-app/build.gradle insurance-response-app/build.gradle

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY common/src common/src
COPY insurance-request-app/src insurance-request-app/src

# Build the boot JAR
RUN ./gradlew :insurance-request-app:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/insurance-request-app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
