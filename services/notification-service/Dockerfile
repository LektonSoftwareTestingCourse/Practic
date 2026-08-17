FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY ./services/pom.xml ./pom.xml

COPY ./services/notification-service/pom.xml ./notification-service/pom.xml

COPY ./services/notification-service/src ./notification-service/src

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -P notification-service -pl notification-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/notification-service/target/notification-service-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=40s \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
