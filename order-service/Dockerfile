FROM maven:3.9.11-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY contracts/pom.xml contracts/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY contracts/src contracts/src
COPY order-service/src order-service/src

RUN mvn --batch-mode --no-transfer-progress \
    -pl order-service -am \
    -DskipTests \
    package

FROM eclipse-temurin:17-jre-jammy

RUN groupadd --system spring \
    && useradd --system --gid spring --home-dir /app spring

WORKDIR /app

COPY --from=build --chown=spring:spring \
    /workspace/order-service/target/order-service-0.1.0-SNAPSHOT.jar \
    /app/app.jar

USER spring:spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

