# Build the jar with the project's own wrapper so the image matches a local build.
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

# Copy the build definition first: dependency resolution is then cached and only
# redone when the build files actually change, not on every source edit.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies --quiet || true

COPY src src
RUN ./gradlew --no-daemon bootJar --quiet

FROM eclipse-temurin:25-jre-alpine AS runtime

# Run unprivileged: nothing in this app needs root.
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER spring

EXPOSE 8080

# Actuator reports DOWN when the database is unreachable, so this tracks whether
# the app can actually serve traffic rather than merely whether the JVM is up.
# start-period covers JVM boot and the per-tenant Flyway migrations.
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
