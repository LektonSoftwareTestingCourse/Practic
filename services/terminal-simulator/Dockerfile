FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY ./services/pom.xml ./pom.xml

COPY ./services/common/pom.xml ./common/pom.xml
COPY ./services/terminal-simulator/pom.xml ./terminal-simulator/pom.xml

COPY ./services/common/src ./common/src
COPY ./services/terminal-simulator/src ./terminal-simulator/src

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -P terminal-simulator -pl terminal-simulator -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/terminal-simulator/target/terminal-simulator-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=40s \
    CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
