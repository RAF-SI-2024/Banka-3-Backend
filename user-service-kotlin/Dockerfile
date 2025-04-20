FROM maven:3.9.6 AS builder
WORKDIR /app

COPY pom.xml app/pom.xml
COPY user-service-kotlin/src app/service/src
COPY user-service-kotlin/pom.xml app/service/pom.xml

RUN --mount=type=cache,target=/root/.m2 mvn -DfinalName=app -f app/service/pom.xml -Dmaven.test.skip clean package

FROM openjdk:17
WORKDIR /app
COPY --from=builder /app .

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app/service/target/user-service-kotlin-0.0.1.jar"]