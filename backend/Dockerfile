FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml pom.xml
COPY shared-lib/pom.xml shared-lib/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY identity-service/pom.xml identity-service/pom.xml
COPY organization-service/pom.xml organization-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml

RUN mvn dependency:go-offline -B || true

COPY . ./

ARG SERVICE_NAME
RUN mvn -pl ${SERVICE_NAME} -am package -DskipTests -B

FROM eclipse-temurin:26-jre
WORKDIR /app
ARG SERVICE_NAME
COPY --from=build /app/${SERVICE_NAME}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
