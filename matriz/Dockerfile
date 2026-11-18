# The Matriz API. Two stages: Maven builds the jar, and only the jar goes to a small Java runtime image.
# No credentials here: the database and the demo password are given when the container runs (see compose.yaml).

# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# The dependencies first, so that they stay in the cache until pom.xml changes
COPY pom.xml .
# Maven Central fails now and then: docker/mvn-retry.sh runs Maven again when it does
COPY --chmod=755 docker/mvn-retry.sh /usr/local/bin/mvn-retry
RUN --mount=type=cache,target=/root/.m2 mvn-retry dependency:go-offline
COPY src ./src
# The tests run in the CI (they need a database), not while the image is built
RUN --mount=type=cache,target=/root/.m2 mvn-retry -DskipTests package && cp target/*.jar /build/matriz.jar

# ---- run ----
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home matriz
WORKDIR /app
COPY --from=build /build/matriz.jar /app/matriz.jar
USER matriz
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=12 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1
# Needs DB_URL, DB_USER, DB_PASSWORD (and, for the demo accounts, DEMO_PASSWORD) in the environment
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/matriz.jar"]
