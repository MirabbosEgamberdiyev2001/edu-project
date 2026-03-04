# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Cache Maven dependencies as a separate layer (speeds up rebuilds)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Build the application (tests are skipped — run them in CI before docker build)
COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Security: run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Uploads directory
RUN mkdir -p /app/uploads && chown spring:spring /app/uploads

COPY --from=builder /build/target/*.jar app.jar

USER spring

EXPOSE 8082

# Container-aware JVM flags:
# -XX:+UseContainerSupport     — respects Docker CPU/mem limits
# -XX:MaxRAMPercentage=75.0    — use 75% of container RAM for heap
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
