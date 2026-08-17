FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY ./services/pom.xml ./pom.xml

COPY ./services/common/pom.xml ./common/pom.xml
COPY ./services/transaction-logger/pom.xml ./transaction-logger/pom.xml

COPY ./services/common/src ./common/src
COPY ./services/transaction-logger/src ./transaction-logger/src

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -P transaction-logger -pl transaction-logger -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/transaction-logger/target/transaction-logger-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=40s \
    CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
