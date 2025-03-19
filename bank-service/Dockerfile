FROM maven:3.9.6 AS builder
WORKDIR /app

COPY pom.xml app/pom.xml
COPY bank-service/src app/service/src
COPY bank-service/pom.xml app/service/pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn -DfinalName=app -f app/service/pom.xml -Dmaven.test.skip clean package

FROM openjdk:17
WORKDIR /app
COPY --from=builder /app .

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app/service/target/bank-service-0.0.1-SNAPSHOT.jar"]