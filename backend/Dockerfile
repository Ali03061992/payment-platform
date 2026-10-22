FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY shared-lib/pom.xml shared-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml
COPY organization-service/pom.xml organization-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

# Cache BuildKit ~/.m2 : les dépendances survivent aux rebuilds (VM 2 CPU).
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B || true

COPY . ./

ARG SERVICE_NAME
RUN --mount=type=cache,target=/root/.m2 mvn -pl ${SERVICE_NAME} -am package -DskipTests -B

FROM eclipse-temurin:26-jre
WORKDIR /app
RUN groupadd --system app \
    && useradd --system --gid app --home-dir /app --shell /usr/sbin/nologin app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app
ARG SERVICE_NAME
COPY --chown=app:app --from=build /app/${SERVICE_NAME}/target/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
