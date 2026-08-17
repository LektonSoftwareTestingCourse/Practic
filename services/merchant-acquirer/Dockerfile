FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY ./services/pom.xml ./pom.xml

COPY ./services/common/pom.xml ./common/pom.xml
COPY ./services/merchant-acquirer/pom.xml ./merchant-acquirer/pom.xml

COPY ./services/common/src ./common/src
COPY ./services/merchant-acquirer/src ./merchant-acquirer/src

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -P merchant-acquirer -pl merchant-acquirer -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/merchant-acquirer/target/merchant-acquirer-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=40s \
    CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
