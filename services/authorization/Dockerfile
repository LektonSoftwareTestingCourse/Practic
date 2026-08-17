FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY ./services/pom.xml ./pom.xml

COPY ./services/common/pom.xml ./common/pom.xml
COPY ./services/authorization/pom.xml ./authorization/pom.xml

COPY ./services/common/src ./common/src
COPY ./services/authorization/src ./authorization/src

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -P authorization -pl authorization -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/authorization/target/authorization-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=40s \
    CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
