FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY contracts/pom.xml contracts/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY notification-service/src notification-service/src

RUN mvn --batch-mode --no-transfer-progress \
    -pl notification-service -am \
    -DskipTests \
    package

FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring

WORKDIR /app

COPY --from=build --chown=spring:spring \
    /workspace/notification-service/target/notification-service-0.1.0-SNAPSHOT.jar \
    /app/app.jar

USER spring:spring

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

