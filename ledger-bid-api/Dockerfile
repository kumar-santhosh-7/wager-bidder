# syntax=docker/dockerfile:1
#
# Ledger Bid API image
# - Java 17, Spring Boot 3.4.4, Maven wrapper
# - JAR: target/ledger-bid-api-1.0.0.jar
# - Port 8090
# - MySQL is not in this image; use docker-compose.yml for local app + MySQL 8
#
# Build:
#   docker build -t wager-bidder-api .
#
# Run (prod):
#   docker run --rm -p 8090:8090 \
#     -e SPRING_PROFILES_ACTIVE=prod \
#     -e SPRING_DATASOURCE_URL=jdbc:mysql://db-host:3306/ledger_bid \
#     -e SPRING_DATASOURCE_USERNAME=ledgerbid \
#     -e SPRING_DATASOURCE_PASSWORD=secret \
#     -e LEDGERBID_ADMIN_USERNAME=admin \
#     -e LEDGERBID_ADMIN_PASSWORD=change-me \
#     wager-bidder-api

FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src/main ./src/main

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B clean package -DskipTests \
    && cp target/ledger-bid-api-1.0.0.jar /tmp/application.jar

FROM eclipse-temurin:17-jre-jammy AS runtime

RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring --no-create-home spring \
    && mkdir -p /application/uploads \
    && chown -R spring:spring /application

WORKDIR /application

COPY --from=build --chown=spring:spring /tmp/application.jar ./application.jar

USER spring

ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=UTC
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
ENV LEDGERBID_UPLOAD_DIR=/application/uploads

EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8090"]

ENTRYPOINT ["java", "-jar", "application.jar"]
