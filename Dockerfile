# syntax=docker/dockerfile:1.7
# Multi-stage Dockerfile for Spring Boot application

# ---- Build Stage ----
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

# Copy build configuration first (leverages Docker layer caching)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Pre-fetch dependencies (won't fail build if something is unresolved yet)
RUN gradle --no-daemon dependencies || true

# Copy application source
COPY src ./src

# Build the bootable jar (skip tests for container build speed; run tests in CI)
RUN gradle --no-daemon bootJar -x test

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# Add a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# JVM optimization flags for container environments
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

# Copy the built jar from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Expose the application port (matches server.port default override logic)
EXPOSE 3000

# Health check using the Spring Boot actuator endpoint
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:3000/actuator/health || exit 1

# Switch to non-root user
USER spring

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
