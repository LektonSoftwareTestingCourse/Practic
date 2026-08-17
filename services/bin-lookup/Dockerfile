FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY ./services/pom.xml ./pom.xml

COPY ./services/bin-lookup/pom.xml ./bin-lookup/pom.xml

COPY ./services/bin-lookup/src ./bin-lookup/src

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -P bin-lookup -pl bin-lookup -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/bin-lookup/target/bin-lookup-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=30s \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
