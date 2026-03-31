# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy Gradle wrapper and config first (layer caching)
COPY gradle/ gradle/
COPY gradlew settings.gradle build.gradle ./

# Copy module build files
COPY backend/common/build.gradle backend/common/build.gradle
COPY backend/claims-app/build.gradle backend/claims-app/build.gradle
COPY backend/insurance-request-app/build.gradle backend/insurance-request-app/build.gradle
COPY backend/insurance-response-app/build.gradle backend/insurance-response-app/build.gradle
COPY backend/prior-auth-app/build.gradle backend/prior-auth-app/build.gradle

# Download dependencies (cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY backend/common/src backend/common/src
COPY backend/prior-auth-app/src backend/prior-auth-app/src

# Build the boot JAR
RUN ./gradlew :prior-auth-app:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /workspace/backend/prior-auth-app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
