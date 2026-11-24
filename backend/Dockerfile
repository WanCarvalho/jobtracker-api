# Versões fixadas. O Maven vem do Maven Wrapper (.mvn/wrapper), não da imagem base.
FROM eclipse-temurin:21.0.13_9-jdk-jammy AS build
WORKDIR /build
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline
COPY src ./src
# Testes PostgreSQL (Testcontainers) precisam de Docker, que não existe dentro deste build.
# A imagem executa os testes H2; os testes PostgreSQL rodam no pipeline e localmente.
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipITs -Dspring.profiles.active=default -Dspring.profiles.default=default verify

FROM eclipse-temurin:21.0.13_9-jre-jammy AS runtime
RUN groupadd --system app && useradd --system --gid app app \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build --chown=app:app /build/target/jobtracker-api-0.1.0.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
